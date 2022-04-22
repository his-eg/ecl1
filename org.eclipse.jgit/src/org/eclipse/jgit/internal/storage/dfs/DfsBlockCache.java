/*
 * Copyright (C) 2008-2011, Google Inc.
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org> and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.eclipse.jgit.internal.storage.dfs;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.stream.LongStream;

import org.eclipse.jgit.internal.JGitText;
import org.eclipse.jgit.internal.storage.pack.PackExt;

/**
 * Caches slices of a
 * {@link org.eclipse.jgit.internal.storage.dfs.BlockBasedFile} in memory for
 * faster read access.
 * <p>
 * The DfsBlockCache serves as a Java based "buffer cache", loading segments of
 * a BlockBasedFile into the JVM heap prior to use. As JGit often wants to do
 * reads of only tiny slices of a file, the DfsBlockCache tries to smooth out
 * these tiny reads into larger block-sized IO operations.
 * <p>
 * Whenever a cache miss occurs, loading is invoked by exactly one thread for
 * the given <code>(DfsStreamKey,position)</code> key tuple. This is ensured by
 * an array of locks, with the tuple hashed to a lock instance.
 * <p>
 * Its too expensive during object access to be accurate with a least recently
 * used (LRU) algorithm. Strictly ordering every read is a lot of overhead that
 * typically doesn't yield a corresponding benefit to the application. This
 * cache implements a clock replacement algorithm, giving each block at least
 * one chance to have been accessed during a sweep of the cache to save itself
 * from eviction. The number of swipe chances is configurable per pack
 * extension.
 * <p>
 * Entities created by the cache are held under hard references, preventing the
 * Java VM from clearing anything. Blocks are discarded by the replacement
 * algorithm when adding a new block would cause the cache to exceed its
 * configured maximum size.
 * <p>
 * The key tuple is passed through to methods as a pair of parameters rather
 * than as a single Object, thus reducing the transient memory allocations of
 * callers. It is more efficient to avoid the allocation, as we can't be 100%
 * sure that a JIT would be able to stack-allocate a key tuple.
 * <p>
 * The internal hash table does not expand at runtime, instead it is fixed in
 * size at cache creation time. The internal lock table used to gate load
 * invocations is also fixed in size.
 */
public final class DfsBlockCache {
	private static volatile DfsBlockCache cache;

	static {
		reconfigure(new DfsBlockCacheConfig());
	}

	/**
	 * Modify the configuration of the window cache.
	 * <p>
	 * The new configuration is applied immediately, and the existing cache is
	 * cleared.
	 *
	 * @param cfg
	 *            the new window cache configuration.
	 * @throws java.lang.IllegalArgumentException
	 *             the cache configuration contains one or more invalid
	 *             settings, usually too low of a limit.
	 */
	public static void reconfigure(DfsBlockCacheConfig cfg) {
		cache = new DfsBlockCache(cfg);
	}

	/**
	 * Get the currently active DfsBlockCache.
	 *
	 * @return the currently active DfsBlockCache.
	 */
	public static DfsBlockCache getInstance() {
		return cache;
	}

	/** Number of entries in {@link #table}. */
	private final int tableSize;

	/** Hash bucket directory; entries are chained below. */
	private final AtomicReferenceArray<HashEntry> table;

	/**
	 * Locks to prevent concurrent loads for same (PackFile,position) block. The
	 * number of locks is {@link DfsBlockCacheConfig#getConcurrencyLevel()} to
	 * cap the overall concurrent block loads.
	 */
	private final ReentrantLock[] loadLocks;

	/**
	 * A separate pool of locks per pack extension to prevent concurrent loads
	 * for same index or bitmap from PackFile.
	 */
	private final ReentrantLock[][] refLocks;

	/** Maximum number of bytes the cache should hold. */
	private final long maxBytes;

	/** Pack files smaller than this size can be copied through the cache. */
	private final long maxStreamThroughCache;

	/**
	 * Suggested block size to read from pack files in.
	 * <p>
	 * If a pack file does not have a native block size, this size will be used.
	 * <p>
	 * If a pack file has a native size, a whole multiple of the native size
	 * will be used until it matches this size.
	 * <p>
	 * The value for blockSize must be a power of 2.
	 */
	private final int blockSize;

	/** As {@link #blockSize} is a power of 2, bits to shift for a / blockSize. */
	private final int blockSizeShift;

	/**
	 * Number of times a block was found in the cache, per pack file extension.
	 */
	private final AtomicReference<AtomicLong[]> statHit;

	/**
	 * Number of times a block was not found, and had to be loaded, per pack
	 * file extension.
	 */
	private final AtomicReference<AtomicLong[]> statMiss;

	/**
	 * Number of blocks evicted due to cache being full, per pack file
	 * extension.
	 */
	private final AtomicReference<AtomicLong[]> statEvict;

	/**
	 * Number of bytes currently loaded in the cache, per pack file extension.
	 */
	private final AtomicReference<AtomicLong[]> liveBytes;

	/** Protects the clock and its related data. */
	private final ReentrantLock clockLock;

	/**
	 * A consumer of object reference lock wait time milliseconds.  May be used to build a metric.
	 */
	private final Consumer<Long> refLockWaitTime;

	/** Current position of the clock. */
	private Ref clockHand;

	/** Limits of cache hot count per pack file extension. */
	private final int[] cacheHotLimits = new int[PackExt.values().length];

	/** Consumer of loading and eviction events of indexes. */
	private final DfsBlockCacheConfig.IndexEventConsumer indexEventConsumer;

	/** Stores timestamps of the last eviction of indexes. */
	private final Map<EvictKey, Long> indexEvictionMap = new ConcurrentHashMap<>();

	@SuppressWarnings("unchecked")
	private DfsBlockCache(DfsBlockCacheConfig cfg) {
		tableSize = tableSize(cfg);
		if (tableSize < 1) {
			throw new IllegalArgumentException(JGitText.get().tSizeMustBeGreaterOrEqual1);
		}

		table = new AtomicReferenceArray<>(tableSize);
		int concurrencyLevel = cfg.getConcurrencyLevel();
		loadLocks = new ReentrantLock[concurrencyLevel];
		for (int i = 0; i < loadLocks.length; i++) {
			loadLocks[i] = new ReentrantLock(true /* fair */);
		}
		refLocks = new ReentrantLock[PackExt.values().length][concurrencyLevel];
		for (int i = 0; i < PackExt.values().length; i++) {
			for (int j = 0; j < concurrencyLevel; ++j) {
				refLocks[i][j] = new ReentrantLock(true /* fair */);
			}
		}

		maxBytes = cfg.getBlockLimit();
		maxStreamThroughCache = (long) (maxBytes * cfg.getStreamRatio());
		blockSize = cfg.getBlockSize();
		blockSizeShift = Integer.numberOfTrailingZeros(blockSize);

		clockLock = new ReentrantLock(true /* fair */);
		String none = ""; //$NON-NLS-1$
		clockHand = new Ref<>(
				DfsStreamKey.of(new DfsRepositoryDescription(none), none, null),
				-1, 0, null);
		clockHand.next = clockHand;

		statHit = new AtomicReference<>(newCounters());
		statMiss = new AtomicReference<>(newCounters());
		statEvict = new AtomicReference<>(newCounters());
		liveBytes = new AtomicReference<>(newCounters());

		refLockWaitTime = cfg.getRefLockWaitTimeConsumer();

		for (int i = 0; i < PackExt.values().length; ++i) {
			Integer limit = cfg.getCacheHotMap().get(PackExt.values()[i]);
			if (limit != null && limit.intValue() > 0) {
				cacheHotLimits[i] = limit.intValue();
			} else {
				cacheHotLimits[i] = DfsBlockCacheConfig.DEFAULT_CACHE_HOT_MAX;
			}
		}
		indexEventConsumer = cfg.getIndexEventConsumer();
	}

	boolean shouldCopyThroughCache(long length) {
		return length <= maxStreamThroughCache;
	}

	/**
	 * Get total number of bytes in the cache, per pack file extension.
	 *
	 * @return total number of bytes in the cache, per pack file extension.
	 */
	public long[] getCurrentSize() {
		return getStatVals(liveBytes);
	}

	/**
	 * Get 0..100, defining how full the cache is.
	 *
	 * @return 0..100, defining how full the cache is.
	 */
	public long getFillPercentage() {
		return LongStream.of(getCurrentSize()).sum() * 100 / maxBytes;
	}

	/**
	 * Get number of requests for items in the cache, per pack file extension.
	 *
	 * @return number of requests for items in the cache, per pack file
	 *         extension.
	 */
	public long[] getHitCount() {
		return getStatVals(statHit);
	}

	/**
	 * Get number of requests for items not in the cache, per pack file
	 * extension.
	 *
	 * @return number of requests for items not in the cache, per pack file
	 *         extension.
	 */
	public long[] getMissCount() {
		return getStatVals(statMiss);
	}

	/**
	 * Get total number of requests (hit + miss), per pack file extension.
	 *
	 * @return total number of requests (hit + miss), per pack file extension.
	 */
	public long[] getTotalRequestCount() {
		AtomicLong[] hit = statHit.get();
		AtomicLong[] miss = statMiss.get();
		long[] cnt = new long[Math.max(hit.length, miss.length)];
		for (int i = 0; i < hit.length; i++) {
			cnt[i] += hit[i].get();
		}
		for (int i = 0; i < miss.length; i++) {
			cnt[i] += miss[i].get();
		}
		return cnt;
	}

	/**
	 * Get hit ratios
	 *
	 * @return hit ratios
	 */
	public long[] getHitRatio() {
		AtomicLong[] hit = statHit.get();
		AtomicLong[] miss = statMiss.get();
		long[] ratio = new long[Math.max(hit.length, miss.length)];
		for (int i = 0; i < ratio.length; i++) {
			if (i >= hit.length) {
				ratio[i] = 0;
			} else if (i >= miss.length) {
				ratio[i] = 100;
			} else {
				long hitVal = hit[i].get();
				long missVal = miss[i].get();
				long total = hitVal + missVal;
				ratio[i] = total == 0 ? 0 : hitVal * 100 / total;
			}
		}
		return ratio;
	}

	/**
	 * Get number of evictions performed due to cache being full, per pack file
	 * extension.
	 *
	 * @return number of evictions performed due to cache being full, per pack
	 *         file extension.
	 */
	public long[] getEvictions() {
		return getStatVals(statEvict);
	}

	/**
	 * Quickly check if the cache contains block 0 of the given stream.
	 * <p>
	 * This can be useful for sophisticated pre-read algorithms to quickly
	 * determine if a file is likely already in cache, especially small
	 * reftables which may be smaller than a typical DFS block size.
	 *
	 * @param key
	 *            the file to check.
	 * @return true if block 0 (the first block) is in the cache.
	 */
	public boolean hasBlock0(DfsStreamKey key) {
		HashEntry e1 = table.get(slot(key, 0));
		DfsBlock v = scan(e1, key, 0);
		return v != null && v.contains(key, 0);
	}

	private int hash(int packHash, long off) {
		return packHash + (int) (off >>> blockSizeShift);
	}

	int getBlockSize() {
		return blockSize;
	}

	private static int tableSize(DfsBlockCacheConfig cfg) {
		final int wsz = cfg.getBlockSize();
		final long limit = cfg.getBlockLimit();
		if (wsz <= 0) {
			throw new IllegalArgumentException(JGitText.get().invalidWindowSize);
		}
		if (limit < wsz) {
			throw new IllegalArgumentException(JGitText.get().windowSizeMustBeLesserThanLimit);
		}
		return (int) Math.min(5 * (limit / wsz) / 2, Integer.MAX_VALUE);
	}

	/**
	 * Look up a cached object, creating and loading it if it doesn't exist.
	 *
	 * @param file
	 *            the pack that "contains" the cached object.
	 * @param position
	 *            offset within <code>pack</code> of the object.
	 * @param ctx
	 *            current thread's reader.
	 * @param fileChannel
	 *            supplier for channel to read {@code pack}.
	 * @return the object reference.
	 * @throws IOException
	 *             the reference was not in the cache and could not be loaded.
	 */
	DfsBlock getOrLoad(BlockBasedFile file, long position, DfsReader ctx,
			ReadableChannelSupplier fileChannel) throws IOException {
		final long requestedPosition = position;
		position = file.alignToBlock(position);

		DfsStreamKey key = file.key;
		int slot = slot(key, position);
		HashEntry e1 = table.get(slot);
		DfsBlock v = scan(e1, key, position);
		if (v != null && v.contains(key, requestedPosition)) {
			ctx.stats.blockCacheHit++;
			getStat(statHit, key).incrementAndGet();
			return v;
		}

		reserveSpace(blockSize, key);
		ReentrantLock regionLock = lockFor(key, position);
		regionLock.lock();
		try {
			HashEntry e2 = table.get(slot);
			if (e2 != e1) {
				v = scan(e2, key, position);
				if (v != null) {
					ctx.stats.blockCacheHit++;
					getStat(statHit, key).incrementAndGet();
					creditSpace(blockSize, key);
					return v;
				}
			}

			getStat(statMiss, key).incrementAndGet();
			boolean credit = true;
			try {
				v = file.readOneBlock(position, ctx, fileChannel.get());
				credit = false;
			} finally {
				if (credit) {
					creditSpace(blockSize, key);
				}
			}
			if (position != v.start) {
				// The file discovered its blockSize and adjusted.
				position = v.start;
				slot = slot(key, position);
				e2 = table.get(slot);
			}

			Ref<DfsBlock> ref = new Ref<>(key, position, v.size(), v);
			ref.markHotter();
			for (;;) {
				HashEntry n = new HashEntry(clean(e2), ref);
				if (table.compareAndSet(slot, e2, n)) {
					break;
				}
				e2 = table.get(slot);
			}
			addToClock(ref, blockSize - v.size());
		} finally {
			regionLock.unlock();
		}

		// If the block size changed from the default, it is possible the block
		// that was loaded is the wrong block for the requested position.
		if (v.contains(file.key, requestedPosition)) {
			return v;
		}
		return getOrLoad(file, requestedPosition, ctx, fileChannel);
	}

	@SuppressWarnings("unchecked")
	private void reserveSpace(long reserve, DfsStreamKey key) {
		clockLock.lock();
		try {
			long live = LongStream.of(getCurrentSize()).sum() + reserve;
			if (maxBytes < live) {
				Ref prev = clockHand;
				Ref hand = clockHand.next;
				do {
					if (hand.isHot()) {
						// Value was recently touched. Cache is still hot so
						// give it another chance, but cool it down a bit.
						hand.markColder();
						prev = hand;
						hand = hand.next;
						continue;
					} else if (prev == hand)
						break;

					// No recent access since last scan, kill
					// value and remove from clock.
					Ref dead = hand;
					hand = hand.next;
					prev.next = hand;
					dead.next = null;
					dead.value = null;
					live -= dead.size;
					getStat(liveBytes, dead.key).addAndGet(-dead.size);
					getStat(statEvict, dead.key).incrementAndGet();
					reportIndexEvicted(dead);
				} while (maxBytes < live);
				clockHand = prev;
			}
			getStat(liveBytes, key).addAndGet(reserve);
		} finally {
			clockLock.unlock();
		}
	}

	private void creditSpace(long credit, DfsStreamKey key) {
		clockLock.lock();
		try {
			getStat(liveBytes, key).addAndGet(-credit);
		} finally {
			clockLock.unlock();
		}
	}

	@SuppressWarnings("unchecked")
	private void addToClock(Ref ref, long credit) {
		clockLock.lock();
		try {
			if (credit != 0) {
				getStat(liveBytes, ref.key).addAndGet(-credit);
			}
			Ref ptr = clockHand;
			ref.next = ptr.next;
			ptr.next = ref;
			clockHand = ref;
		} finally {
			clockLock.unlock();
		}
	}

	void put(DfsBlock v) {
		put(v.stream, v.start, v.size(), v);
	}

	/**
	 * Look up a cached object, creating and loading it if it doesn't exist.
	 *
	 * @param key
	 *            the stream key of the pack.
	 * @param position
	 *            the position in the key. The default should be 0.
	 * @param loader
	 *            the function to load the reference.
	 * @return the object reference.
	 * @throws IOException
	 *             the reference was not in the cache and could not be loaded.
	 */
	<T> Ref<T> getOrLoadRef(
			DfsStreamKey key, long position, RefLoader<T> loader)
			throws IOException {
		long start = System.nanoTime();
		int slot = slot(key, position);
		HashEntry e1 = table.get(slot);
		Ref<T> ref = scanRef(e1, key, position);
		if (ref != null) {
			getStat(statHit, key).incrementAndGet();
			reportIndexRequested(ref, true /* cacheHit */, start);
			return ref;
		}

		ReentrantLock regionLock = lockForRef(key);
		long lockStart = System.currentTimeMillis();
		regionLock.lock();
		try {
			HashEntry e2 = table.get(slot);
			if (e2 != e1) {
				ref = scanRef(e2, key, position);
				if (ref != null) {
					getStat(statHit, key).incrementAndGet();
					reportIndexRequested(ref, true /* cacheHit */,
							start);
					return ref;
				}
			}

			if (refLockWaitTime != null) {
				refLockWaitTime.accept(
						Long.valueOf(System.currentTimeMillis() - lockStart));
			}
			getStat(statMiss, key).incrementAndGet();
			ref = loader.load();
			ref.markHotter();
			// Reserve after loading to get the size of the object
			reserveSpace(ref.size, key);
			for (;;) {
				HashEntry n = new HashEntry(clean(e2), ref);
				if (table.compareAndSet(slot, e2, n)) {
					break;
				}
				e2 = table.get(slot);
			}
			addToClock(ref, 0);
		} finally {
			regionLock.unlock();
		}
		reportIndexRequested(ref, false /* cacheHit */, start);
		return ref;
	}

	<T> Ref<T> putRef(DfsStreamKey key, long size, T v) {
		return put(key, 0, size, v);
	}

	<T> Ref<T> put(DfsStreamKey key, long pos, long size, T v) {
		int slot = slot(key, pos);
		HashEntry e1 = table.get(slot);
		Ref<T> ref = scanRef(e1, key, pos);
		if (ref != null) {
			return ref;
		}

		reserveSpace(size, key);
		ReentrantLock regionLock = lockFor(key, pos);
		regionLock.lock();
		try {
			HashEntry e2 = table.get(slot);
			if (e2 != e1) {
				ref = scanRef(e2, key, pos);
				if (ref != null) {
					creditSpace(size, key);
					return ref;
				}
			}

			ref = new Ref<>(key, pos, size, v);
			ref.markHotter();
			for (;;) {
				HashEntry n = new HashEntry(clean(e2), ref);
				if (table.compareAndSet(slot, e2, n)) {
					break;
				}
				e2 = table.get(slot);
			}
			addToClock(ref, 0);
		} finally {
			regionLock.unlock();
		}
		return ref;
	}

	boolean contains(DfsStreamKey key, long position) {
		return scan(table.get(slot(key, position)), key, position) != null;
	}

	@SuppressWarnings("unchecked")
	<T> T get(DfsStreamKey key, long position) {
		T val = (T) scan(table.get(slot(key, position)), key, position);
		if (val == null) {
			getStat(statMiss, key).incrementAndGet();
		} else {
			getStat(statHit, key).incrementAndGet();
		}
		return val;
	}

	private <T> T scan(HashEntry n, DfsStreamKey key, long position) {
		Ref<T> r = scanRef(n, key, position);
		return r != null ? r.get() : null;
	}

	@SuppressWarnings("unchecked")
	private <T> Ref<T> scanRef(HashEntry n, DfsStreamKey key, long position) {
		for (; n != null; n = n.next) {
			Ref<T> r = n.ref;
			if (r.position == position && r.key.equals(key)) {
				return r.get() != null ? r : null;
			}
		}
		return null;
	}

	private int slot(DfsStreamKey key, long position) {
		return (hash(key.hash, position) >>> 1) % tableSize;
	}

	private ReentrantLock lockFor(DfsStreamKey key, long position) {
		return loadLocks[(hash(key.hash, position) >>> 1) % loadLocks.length];
	}

	private ReentrantLock lockForRef(DfsStreamKey key) {
		int slot = (key.hash >>> 1) % refLocks[key.packExtPos].length;
		return refLocks[key.packExtPos][slot];
	}

	private static AtomicLong[] newCounters() {
		AtomicLong[] ret = new AtomicLong[PackExt.values().length];
		for (int i = 0; i < ret.length; i++) {
			ret[i] = new AtomicLong();
		}
		return ret;
	}

	private static AtomicLong getStat(AtomicReference<AtomicLong[]> stats,
			DfsStreamKey key) {
		int pos = key.packExtPos;
		while (true) {
			AtomicLong[] vals = stats.get();
			if (pos < vals.length) {
				return vals[pos];
			}
			AtomicLong[] expect = vals;
			vals = new AtomicLong[Math.max(pos + 1, PackExt.values().length)];
			System.arraycopy(expect, 0, vals, 0, expect.length);
			for (int i = expect.length; i < vals.length; i++) {
				vals[i] = new AtomicLong();
			}
			if (stats.compareAndSet(expect, vals)) {
				return vals[pos];
			}
		}
	}

	private static long[] getStatVals(AtomicReference<AtomicLong[]> stat) {
		AtomicLong[] stats = stat.get();
		long[] cnt = new long[stats.length];
		for (int i = 0; i < stats.length; i++) {
			cnt[i] = stats[i].get();
		}
		return cnt;
	}

	private static HashEntry clean(HashEntry top) {
		while (top != null && top.ref.next == null) {
			top = top.next;
		}
		if (top == null) {
			return null;
		}
		HashEntry n = clean(top.next);
		return n == top.next ? top : new HashEntry(n, top.ref);
	}

	private void reportIndexRequested(Ref<?> ref, boolean cacheHit,
			long start) {
		if (indexEventConsumer == null
				|| !isIndexOrBitmapExtPos(ref.key.packExtPos)) {
			return;
		}
		EvictKey evictKey = new EvictKey(ref);
		Long prevEvictedTime = indexEvictionMap.get(evictKey);
		long now = System.nanoTime();
		long sinceLastEvictionNanos = prevEvictedTime == null ? 0L
				: now - prevEvictedTime.longValue();
		indexEventConsumer.acceptRequestedEvent(ref.key.packExtPos, cacheHit,
				(now - start) / 1000L /* micros */, ref.size,
				Duration.ofNanos(sinceLastEvictionNanos));
	}

	private void reportIndexEvicted(Ref<?> dead) {
		if (indexEventConsumer == null
				|| !indexEventConsumer.shouldReportEvictedEvent()
				|| !isIndexOrBitmapExtPos(dead.key.packExtPos)) {
			return;
		}
		EvictKey evictKey = new EvictKey(dead);
		Long prevEvictedTime = indexEvictionMap.get(evictKey);
		long now = System.nanoTime();
		long sinceLastEvictionNanos = prevEvictedTime == null ? 0L
				: now - prevEvictedTime.longValue();
		indexEvictionMap.put(evictKey, Long.valueOf(now));
		indexEventConsumer.acceptEvictedEvent(dead.key.packExtPos, dead.size,
				dead.totalHitCount.get(),
				Duration.ofNanos(sinceLastEvictionNanos));
	}

	private static boolean isIndexOrBitmapExtPos(int packExtPos) {
		return packExtPos == PackExt.INDEX.getPosition()
				|| packExtPos == PackExt.BITMAP_INDEX.getPosition();
	}

	private static final class HashEntry {
		/** Next entry in the hash table's chain list. */
		final HashEntry next;

		/** The referenced object. */
		final Ref ref;

		HashEntry(HashEntry n, Ref r) {
			next = n;
			ref = r;
		}
	}

	static final class Ref<T> {
		final DfsStreamKey key;
		final long position;
		final long size;
		volatile T value;
		Ref next;

		private volatile int hotCount;
		private AtomicInteger totalHitCount = new AtomicInteger();

		Ref(DfsStreamKey key, long position, long size, T v) {
			this.key = key;
			this.position = position;
			this.size = size;
			this.value = v;
		}

		T get() {
			T v = value;
			if (v != null) {
				markHotter();
			}
			return v;
		}

		boolean has() {
			return value != null;
		}

		void markHotter() {
			int cap = DfsBlockCache
					.getInstance().cacheHotLimits[key.packExtPos];
			hotCount = Math.min(cap, hotCount + 1);
			totalHitCount.incrementAndGet();
		}

		void markColder() {
			hotCount = Math.max(0, hotCount - 1);
		}

		boolean isHot() {
			return hotCount > 0;
		}
	}

	private static final class EvictKey {
		private final int keyHash;
		private final int packExtPos;
		private final long position;

		EvictKey(Ref<?> ref) {
			keyHash = ref.key.hash;
			packExtPos = ref.key.packExtPos;
			position = ref.position;
		}

		@Override
		public boolean equals(Object object) {
			if (object instanceof EvictKey) {
				EvictKey other = (EvictKey) object;
				return keyHash == other.keyHash
						&& packExtPos == other.packExtPos
						&& position == other.position;
			}
			return false;
		}

		@Override
		public int hashCode() {
			return DfsBlockCache.getInstance().hash(keyHash, position);
		}
	}

	@FunctionalInterface
	interface RefLoader<T> {
		Ref<T> load() throws IOException;
	}

	/**
	 * Supplier for readable channel
	 */
	@FunctionalInterface
	interface ReadableChannelSupplier {
		/**
		 * @return ReadableChannel
		 * @throws IOException
		 */
		ReadableChannel get() throws IOException;
	}
}