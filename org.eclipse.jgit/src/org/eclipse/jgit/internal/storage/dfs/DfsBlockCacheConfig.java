/*
 * Copyright (C) 2011, Google Inc. and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.eclipse.jgit.internal.storage.dfs;

import static org.eclipse.jgit.lib.ConfigConstants.CONFIG_CORE_SECTION;
import static org.eclipse.jgit.lib.ConfigConstants.CONFIG_DFS_SECTION;
import static org.eclipse.jgit.lib.ConfigConstants.CONFIG_KEY_BLOCK_LIMIT;
import static org.eclipse.jgit.lib.ConfigConstants.CONFIG_KEY_BLOCK_SIZE;
import static org.eclipse.jgit.lib.ConfigConstants.CONFIG_KEY_CONCURRENCY_LEVEL;
import static org.eclipse.jgit.lib.ConfigConstants.CONFIG_KEY_STREAM_RATIO;

import java.text.MessageFormat;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.function.Consumer;

import org.eclipse.jgit.internal.JGitText;
import org.eclipse.jgit.internal.storage.pack.PackExt;
import org.eclipse.jgit.lib.Config;

/**
 * Configuration parameters for
 * {@link org.eclipse.jgit.internal.storage.dfs.DfsBlockCache}.
 */
public class DfsBlockCacheConfig {
	/** 1024 (number of bytes in one kibibyte/kilobyte) */
	public static final int KB = 1024;

	/** 1024 {@link #KB} (number of bytes in one mebibyte/megabyte) */
	public static final int MB = 1024 * KB;

	/** Default number of max cache hits. */
	public static final int DEFAULT_CACHE_HOT_MAX = 1;

	private long blockLimit;
	private int blockSize;
	private double streamRatio;
	private int concurrencyLevel;

	private Consumer<Long> refLock;
	private Map<PackExt, Integer> cacheHotMap;

	private IndexEventConsumer indexEventConsumer;

	/**
	 * Create a default configuration.
	 */
	public DfsBlockCacheConfig() {
		setBlockLimit(32 * MB);
		setBlockSize(64 * KB);
		setStreamRatio(0.30);
		setConcurrencyLevel(32);
		cacheHotMap = Collections.emptyMap();
	}

	/**
	 * Get maximum number bytes of heap memory to dedicate to caching pack file
	 * data.
	 *
	 * @return maximum number bytes of heap memory to dedicate to caching pack
	 *         file data. <b>Default is 32 MB.</b>
	 */
	public long getBlockLimit() {
		return blockLimit;
	}

	/**
	 * Set maximum number bytes of heap memory to dedicate to caching pack file
	 * data.
	 * <p>
	 * It is strongly recommended to set the block limit to be an integer multiple
	 * of the block size. This constraint is not enforced by this method (since
	 * it may be called before {@link #setBlockSize(int)}), but it is enforced by
	 * {@link #fromConfig(Config)}.
	 *
	 * @param newLimit
	 *            maximum number bytes of heap memory to dedicate to caching
	 *            pack file data; must be positive.
	 * @return {@code this}
	 */
	public DfsBlockCacheConfig setBlockLimit(long newLimit) {
		if (newLimit <= 0) {
			throw new IllegalArgumentException(MessageFormat.format(
					JGitText.get().blockLimitNotPositive,
					Long.valueOf(newLimit)));
		}
		blockLimit = newLimit;
		return this;
	}

	/**
	 * Get size in bytes of a single window mapped or read in from the pack
	 * file.
	 *
	 * @return size in bytes of a single window mapped or read in from the pack
	 *         file. <b>Default is 64 KB.</b>
	 */
	public int getBlockSize() {
		return blockSize;
	}

	/**
	 * Set size in bytes of a single window read in from the pack file.
	 *
	 * @param newSize
	 *            size in bytes of a single window read in from the pack file.
	 *            The value must be a power of 2.
	 * @return {@code this}
	 */
	public DfsBlockCacheConfig setBlockSize(int newSize) {
		int size = Math.max(512, newSize);
		if ((size & (size - 1)) != 0) {
			throw new IllegalArgumentException(
					JGitText.get().blockSizeNotPowerOf2);
		}
		blockSize = size;
		return this;
	}

	/**
	 * Get the estimated number of threads concurrently accessing the cache.
	 *
	 * @return the estimated number of threads concurrently accessing the cache.
	 *         <b>Default is 32.</b>
	 */
	public int getConcurrencyLevel() {
		return concurrencyLevel;
	}

	/**
	 * Set the estimated number of threads concurrently accessing the cache.
	 *
	 * @param newConcurrencyLevel
	 *            the estimated number of threads concurrently accessing the
	 *            cache.
	 * @return {@code this}
	 */
	public DfsBlockCacheConfig setConcurrencyLevel(
			final int newConcurrencyLevel) {
		concurrencyLevel = newConcurrencyLevel;
		return this;
	}

	/**
	 * Get highest percentage of {@link #getBlockLimit()} a single pack can
	 * occupy while being copied by the pack reuse strategy.
	 *
	 * @return highest percentage of {@link #getBlockLimit()} a single pack can
	 *         occupy while being copied by the pack reuse strategy. <b>Default
	 *         is 0.30, or 30%</b>.
	 */
	public double getStreamRatio() {
		return streamRatio;
	}

	/**
	 * Set percentage of cache to occupy with a copied pack.
	 *
	 * @param ratio
	 *            percentage of cache to occupy with a copied pack.
	 * @return {@code this}
	 */
	public DfsBlockCacheConfig setStreamRatio(double ratio) {
		streamRatio = Math.max(0, Math.min(ratio, 1.0));
		return this;
	}

	/**
	 * Get the consumer of the object reference lock wait time in milliseconds.
	 *
	 * @return consumer of wait time in milliseconds.
	 */
	public Consumer<Long> getRefLockWaitTimeConsumer() {
		return refLock;
	}

	/**
	 * Set the consumer for lock wait time.
	 *
	 * @param c
	 *            consumer of wait time in milliseconds.
	 * @return {@code this}
	 */
	public DfsBlockCacheConfig setRefLockWaitTimeConsumer(Consumer<Long> c) {
		refLock = c;
		return this;
	}

	/**
	 * Get the map of hot count per pack extension for {@code DfsBlockCache}.
	 *
	 * @return map of hot count per pack extension for {@code DfsBlockCache}.
	 */
	public Map<PackExt, Integer> getCacheHotMap() {
		return cacheHotMap;
	}

	/**
	 * Set the map of hot count per pack extension for {@code DfsBlockCache}.
	 *
	 * @param cacheHotMap
	 *            map of hot count per pack extension for {@code DfsBlockCache}.
	 * @return {@code this}
	 */
	public DfsBlockCacheConfig setCacheHotMap(
			Map<PackExt, Integer> cacheHotMap) {
		this.cacheHotMap = Collections.unmodifiableMap(cacheHotMap);
		return this;
	}

	/**
	 * Get the consumer of cache index events.
	 *
	 * @return consumer of cache index events.
	 */
	public IndexEventConsumer getIndexEventConsumer() {
		return indexEventConsumer;
	}

	/**
	 * Set the consumer of cache index events.
	 *
	 * @param indexEventConsumer
	 *            consumer of cache index events.
	 * @return {@code this}
	 */
	public DfsBlockCacheConfig setIndexEventConsumer(
			IndexEventConsumer indexEventConsumer) {
		this.indexEventConsumer = indexEventConsumer;
		return this;
	}

	/**
	 * Update properties by setting fields from the configuration.
	 * <p>
	 * If a property is not defined in the configuration, then it is left
	 * unmodified.
	 * <p>
	 * Enforces certain constraints on the combination of settings in the config,
	 * for example that the block limit is a multiple of the block size.
	 *
	 * @param rc
	 *            configuration to read properties from.
	 * @return {@code this}
	 */
	public DfsBlockCacheConfig fromConfig(Config rc) {
		long cfgBlockLimit = rc.getLong(
				CONFIG_CORE_SECTION,
				CONFIG_DFS_SECTION,
				CONFIG_KEY_BLOCK_LIMIT,
				getBlockLimit());
		int cfgBlockSize = rc.getInt(
				CONFIG_CORE_SECTION,
				CONFIG_DFS_SECTION,
				CONFIG_KEY_BLOCK_SIZE,
				getBlockSize());
		if (cfgBlockLimit % cfgBlockSize != 0) {
			throw new IllegalArgumentException(MessageFormat.format(
					JGitText.get().blockLimitNotMultipleOfBlockSize,
					Long.valueOf(cfgBlockLimit),
					Long.valueOf(cfgBlockSize)));
		}

		setBlockLimit(cfgBlockLimit);
		setBlockSize(cfgBlockSize);

		setConcurrencyLevel(rc.getInt(
				CONFIG_CORE_SECTION,
				CONFIG_DFS_SECTION,
				CONFIG_KEY_CONCURRENCY_LEVEL,
				getConcurrencyLevel()));

		String v = rc.getString(
				CONFIG_CORE_SECTION,
				CONFIG_DFS_SECTION,
				CONFIG_KEY_STREAM_RATIO);
		if (v != null) {
			try {
				setStreamRatio(Double.parseDouble(v));
			} catch (NumberFormatException e) {
				throw new IllegalArgumentException(MessageFormat.format(
						JGitText.get().enumValueNotSupported3,
						CONFIG_CORE_SECTION,
						CONFIG_DFS_SECTION,
						CONFIG_KEY_STREAM_RATIO, v), e);
			}
		}
		return this;
	}

	/** Consumer of DfsBlockCache loading and eviction events for indexes. */
	public interface IndexEventConsumer {
		/**
		 * Accept an event of an index requested. It could be loaded from either
		 * cache or storage.
		 *
		 * @param packExtPos
		 *            position in {@code PackExt} enum
		 * @param cacheHit
		 *            true if an index was already in cache. Otherwise, the
		 *            index was loaded from storage into the cache in the
		 *            current request,
		 * @param loadMicros
		 *            time to load an index from cache or storage in
		 *            microseconds
		 * @param bytes
		 *            number of bytes loaded
		 * @param lastEvictionDuration
		 *            time since last eviction, 0 if was not evicted yet
		 */
		void acceptRequestedEvent(int packExtPos, boolean cacheHit,
				long loadMicros, long bytes, Duration lastEvictionDuration);

		/**
		 * Accept an event of an index evicted from cache.
		 *
		 * @param packExtPos
		 *            position in {@code PackExt} enum
		 * @param bytes
		 *            number of bytes evicted
		 * @param totalCacheHitCount
		 *            number of times an index was accessed while in cache
		 * @param lastEvictionDuration
		 *            time since last eviction, 0 if was not evicted yet
		 */
		default void acceptEvictedEvent(int packExtPos, long bytes,
				int totalCacheHitCount, Duration lastEvictionDuration) {
			// Off by default.
		}

		/**
		 * @return true if reporting evicted events is enabled.
		 */
		default boolean shouldReportEvictedEvent() {
			return false;
		}
	}
}