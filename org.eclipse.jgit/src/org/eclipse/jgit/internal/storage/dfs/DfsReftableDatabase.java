/*
 * Copyright (C) 2017, Google Inc. and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.eclipse.jgit.internal.storage.dfs;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.eclipse.jgit.annotations.Nullable;
import org.eclipse.jgit.internal.storage.reftable.MergedReftable;
import org.eclipse.jgit.internal.storage.reftable.ReftableConfig;
import org.eclipse.jgit.internal.storage.reftable.ReftableDatabase;
import org.eclipse.jgit.lib.BatchRefUpdate;
import org.eclipse.jgit.lib.NullProgressMonitor;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.ReceiveCommand;
import org.eclipse.jgit.util.RefList;
import org.eclipse.jgit.util.RefMap;

/**
 * A {@link org.eclipse.jgit.internal.storage.dfs.DfsRefDatabase} that uses
 * reftable for storage.
 * <p>
 * A {@code DfsRefDatabase} instance is thread-safe.
 * <p>
 * Implementors may wish to use
 * {@link org.eclipse.jgit.internal.storage.dfs.DfsPackDescription#getMaxUpdateIndex()}
 * as the primary key identifier for a
 * {@link org.eclipse.jgit.internal.storage.pack.PackExt#REFTABLE} only pack
 * description, ensuring that when there are competing transactions one wins,
 * and one will fail.
 */
public class DfsReftableDatabase extends DfsRefDatabase {
	final ReftableDatabase reftableDatabase;

	private DfsReader ctx;
	private DfsReftableStack stack;

	/**
	 * Initialize the reference database for a repository.
	 *
	 * @param repo
	 *            the repository this database instance manages references for.
	 */
	protected DfsReftableDatabase(DfsRepository repo) {
		super(repo);
		reftableDatabase = new ReftableDatabase() {
			@Override
			public MergedReftable openMergedReftable() throws IOException {
				Lock l = DfsReftableDatabase.this.getLock();
				l.lock();
				try {
					return new MergedReftable(stack().readers());
				} finally {
					l.unlock();
				}
			}
		};
		stack = null;
	}

	/** {@inheritDoc} */
	@Override
	public boolean hasVersioning() {
		return true;
	}

	/** {@inheritDoc} */
	@Override
	public boolean performsAtomicTransactions() {
		return true;
	}

	/** {@inheritDoc} */
	@Override
	public BatchRefUpdate newBatchUpdate() {
		DfsObjDatabase odb = getRepository().getObjectDatabase();
		return new DfsReftableBatchRefUpdate(this, odb);
	}

	/**
	 * Get configuration to write new reftables with.
	 *
	 * @return configuration to write new reftables with.
	 */
	public ReftableConfig getReftableConfig() {
		return new ReftableConfig(getRepository());
	}

	/**
	 * Get the lock protecting this instance's state.
	 *
	 * @return the lock protecting this instance's state.
	 */
	protected ReentrantLock getLock() {
		return reftableDatabase.getLock();
	}

	/**
	 * Whether to compact reftable instead of extending the stack depth.
	 *
	 * @return {@code true} if commit of a new small reftable should try to
	 *         replace a prior small reftable by performing a compaction,
	 *         instead of extending the stack depth.
	 */
	protected boolean compactDuringCommit() {
		return true;
	}


	/**
	 * Obtain a handle to the stack of reftables. Must hold lock.
	 *
	 * @return (possibly cached) handle to the stack.
	 * @throws java.io.IOException
	 *             if tables cannot be opened.
	 */
	protected DfsReftableStack stack() throws IOException {
		if (!getLock().isLocked()) {
			throw new IllegalStateException("most hold lock to access stack"); //$NON-NLS-1$
		}
		DfsObjDatabase odb = getRepository().getObjectDatabase();

		if (ctx == null) {
			ctx = odb.newReader();
		}
		if (stack == null) {
			stack = DfsReftableStack.open(ctx, Arrays.asList(odb.getReftables()));
		}
		return stack;
	}

	@Override
	public boolean isNameConflicting(String refName) throws IOException {
		return reftableDatabase.isNameConflicting(refName, new TreeSet<>(), new HashSet<>());
	}

	/** {@inheritDoc} */
	@Override
	public Ref exactRef(String name) throws IOException {
		return reftableDatabase.exactRef(name);
	}

	/** {@inheritDoc} */
	@Override
	public Map<String, Ref> getRefs(String prefix) throws IOException {
		List<Ref> refs = reftableDatabase.getRefsByPrefix(prefix);
		RefList.Builder<Ref> builder = new RefList.Builder<>(refs.size());
		for (Ref r : refs) {
			builder.add(r);
		}
		return new RefMap(prefix, builder.toRefList(), RefList.emptyList(),
			RefList.emptyList());
	}

	/** {@inheritDoc} */
	@Override
	public List<Ref> getRefsByPrefix(String prefix) throws IOException {

		return reftableDatabase.getRefsByPrefix(prefix);
	}

	/** {@inheritDoc} */
	@Override
	public List<Ref> getRefsByPrefixWithExclusions(String include, Set<String> excludes)
			throws IOException {
		return reftableDatabase.getRefsByPrefixWithExclusions(include, excludes);
	}

	/** {@inheritDoc} */
	@Override
	public Set<Ref> getTipsWithSha1(ObjectId id) throws IOException {
		if (!getReftableConfig().isIndexObjects()) {
			return super.getTipsWithSha1(id);
		}
		return reftableDatabase.getTipsWithSha1(id);
	}

	/** {@inheritDoc} */
	@Override
	public boolean hasFastTipsWithSha1() throws IOException {
		return reftableDatabase.hasFastTipsWithSha1();
	}

	/** {@inheritDoc} */
	@Override
	public Ref peel(Ref ref) throws IOException {
		Ref oldLeaf = ref.getLeaf();
		if (oldLeaf.isPeeled() || oldLeaf.getObjectId() == null) {
			return ref;
		}
		return recreate(ref, doPeel(oldLeaf), hasVersioning());
	}

	@Override
	boolean exists() throws IOException {
		DfsObjDatabase odb = getRepository().getObjectDatabase();
		return odb.getReftables().length > 0;
	}

	@Override
	void clearCache() {
		ReentrantLock l = getLock();
		l.lock();
		try {
			if (ctx != null) {
				ctx.close();
				ctx = null;
			}
			reftableDatabase.clearCache();
			if (stack != null) {
				stack.close();
				stack = null;
			}
		} finally {
			l.unlock();
		}
	}

	/** {@inheritDoc} */
	@Override
	protected boolean compareAndPut(Ref oldRef, @Nullable Ref newRef)
			throws IOException {
		ReceiveCommand cmd = ReftableDatabase.toCommand(oldRef, newRef);
		try (RevWalk rw = new RevWalk(getRepository())) {
			rw.setRetainBody(false);
			newBatchUpdate().setAllowNonFastForwards(true).addCommand(cmd)
					.execute(rw, NullProgressMonitor.INSTANCE);
		}
		switch (cmd.getResult()) {
		case OK:
			return true;
		case REJECTED_OTHER_REASON:
			throw new IOException(cmd.getMessage());
		case LOCK_FAILURE:
		default:
			return false;
		}
	}

	/** {@inheritDoc} */
	@Override
	protected boolean compareAndRemove(Ref oldRef) throws IOException {
		return compareAndPut(oldRef, null);
	}

	/** {@inheritDoc} */
	@Override
	protected RefCache scanAllRefs() throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	void stored(Ref ref) {
		// Unnecessary; DfsReftableBatchRefUpdate calls clearCache().
	}

	@Override
	void removed(String refName) {
		// Unnecessary; DfsReftableBatchRefUpdate calls clearCache().
	}

	/** {@inheritDoc} */
	@Override
	protected void cachePeeledState(Ref oldLeaf, Ref newLeaf) {
		// Do not cache peeled state in reftable.
	}

}
