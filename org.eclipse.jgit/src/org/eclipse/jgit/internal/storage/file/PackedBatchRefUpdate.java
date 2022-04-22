/*
 * Copyright (C) 2017, Google Inc. and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.eclipse.jgit.internal.storage.file;

import static java.util.stream.Collectors.toList;
import static org.eclipse.jgit.transport.ReceiveCommand.Result.LOCK_FAILURE;
import static org.eclipse.jgit.transport.ReceiveCommand.Result.NOT_ATTEMPTED;
import static org.eclipse.jgit.transport.ReceiveCommand.Result.REJECTED_NONFASTFORWARD;
import static org.eclipse.jgit.transport.ReceiveCommand.Result.REJECTED_OTHER_REASON;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jgit.annotations.Nullable;
import org.eclipse.jgit.errors.LockFailedException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.internal.JGitText;
import org.eclipse.jgit.internal.storage.file.RefDirectory.PackedRefList;
import org.eclipse.jgit.lib.BatchRefUpdate;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectIdRef;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.ProgressMonitor;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.RefDatabase;
import org.eclipse.jgit.lib.ReflogEntry;
import org.eclipse.jgit.revwalk.RevObject;
import org.eclipse.jgit.revwalk.RevTag;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.ReceiveCommand;
import org.eclipse.jgit.util.RefList;

/**
 * Implementation of {@link BatchRefUpdate} that uses the {@code packed-refs}
 * file to support atomically updating multiple refs.
 * <p>
 * The algorithm is designed to be compatible with traditional single ref
 * updates operating on single refs only. Regardless of success or failure, the
 * results are atomic: from the perspective of any reader, either all updates in
 * the batch will be visible, or none will. In the case of process failure
 * during any of the following steps, removal of stale lock files is always
 * safe, and will never result in an inconsistent state, although the update may
 * or may not have been applied.
 * <p>
 * The algorithm is:
 * <ol>
 * <li>Pack loose refs involved in the transaction using the normal pack-refs
 * operation. This ensures that creating lock files in the following step
 * succeeds even if a batch contains both a delete of {@code refs/x} (loose) and
 * a create of {@code refs/x/y}.</li>
 * <li>Create locks for all loose refs involved in the transaction, even if they
 * are not currently loose.</li>
 * <li>Pack loose refs again, this time while holding all lock files (see {@link
 * RefDirectory#pack(Map)}), without deleting them afterwards. This covers a
 * potential race where new loose refs were created after the initial packing
 * step. If no new loose refs were created during this race, this step does not
 * modify any files on disk. Keep the merged state in memory.</li>
 * <li>Update the in-memory packed refs with the commands in the batch, possibly
 * failing the whole batch if any old ref values do not match.</li>
 * <li>If the update succeeds, lock {@code packed-refs} and commit by atomically
 * renaming the lock file.</li>
 * <li>Delete loose ref lock files.</li>
 * </ol>
 *
 * Because the packed-refs file format is a sorted list, this algorithm is
 * linear in the total number of refs, regardless of the batch size. This can be
 * a significant slowdown on repositories with large numbers of refs; callers
 * that prefer speed over atomicity should use {@code setAtomic(false)}. As an
 * optimization, an update containing a single ref update does not use the
 * packed-refs protocol.
 */
class PackedBatchRefUpdate extends BatchRefUpdate {
	private RefDirectory refdb;

	PackedBatchRefUpdate(RefDirectory refdb) {
		super(refdb);
		this.refdb = refdb;
	}

	/** {@inheritDoc} */
	@Override
	public void execute(RevWalk walk, ProgressMonitor monitor,
			List<String> options) throws IOException {
		if (!isAtomic()) {
			// Use default one-by-one implementation.
			super.execute(walk, monitor, options);
			return;
		}
		List<ReceiveCommand> pending =
				ReceiveCommand.filter(getCommands(), NOT_ATTEMPTED);
		if (pending.isEmpty()) {
			return;
		}
		if (pending.size() == 1) {
			// Single-ref updates are always atomic, no need for packed-refs.
			super.execute(walk, monitor, options);
			return;
		}
		if (containsSymrefs(pending)) {
			// packed-refs file cannot store symrefs
			reject(pending.get(0), REJECTED_OTHER_REASON,
					JGitText.get().atomicSymRefNotSupported, pending);
			return;
		}

		// Required implementation details copied from super.execute.
		if (!blockUntilTimestamps(MAX_WAIT)) {
			return;
		}
		if (options != null) {
			setPushOptions(options);
		}
		// End required implementation details.

		// Check for conflicting names before attempting to acquire locks, since
		// lockfile creation may fail on file/directory conflicts.
		if (!checkConflictingNames(pending)) {
			return;
		}

		if (!checkObjectExistence(walk, pending)) {
			return;
		}

		if (!checkNonFastForwards(walk, pending)) {
			return;
		}

		// Pack refs normally, so we can create lock files even in the case where
		// refs/x is deleted and refs/x/y is created in this batch.
		try {
			refdb.pack(
					pending.stream().map(ReceiveCommand::getRefName).collect(toList()));
		} catch (LockFailedException e) {
			lockFailure(pending.get(0), pending);
			return;
		}

		Map<String, LockFile> locks = null;
		refdb.inProcessPackedRefsLock.lock();
		try {
			PackedRefList oldPackedList;
			if (!refdb.isInClone()) {
				locks = lockLooseRefs(pending);
				if (locks == null) {
					return;
				}
				oldPackedList = refdb.pack(locks);
			} else {
				// During clone locking isn't needed since no refs exist yet.
				// This also helps to avoid problems with refs only differing in
				// case on a case insensitive filesystem (bug 528497)
				oldPackedList = refdb.getPackedRefs();
			}
			RefList<Ref> newRefs = applyUpdates(walk, oldPackedList, pending);
			if (newRefs == null) {
				return;
			}
			LockFile packedRefsLock = refdb.lockPackedRefs();
			if (packedRefsLock == null) {
				lockFailure(pending.get(0), pending);
				return;
			}
			// commitPackedRefs removes lock file (by renaming over real file).
			refdb.commitPackedRefs(packedRefsLock, newRefs, oldPackedList,
					true);
		} finally {
			try {
				unlockAll(locks);
			} finally {
				refdb.inProcessPackedRefsLock.unlock();
			}
		}

		refdb.fireRefsChanged();
		pending.forEach(c -> c.setResult(ReceiveCommand.Result.OK));
		writeReflog(pending);
	}

	private static boolean containsSymrefs(List<ReceiveCommand> commands) {
		for (ReceiveCommand cmd : commands) {
			if (cmd.getOldSymref() != null || cmd.getNewSymref() != null) {
				return true;
			}
		}
		return false;
	}

	private boolean checkConflictingNames(List<ReceiveCommand> commands)
			throws IOException {
		Set<String> takenNames = new HashSet<>();
		Set<String> takenPrefixes = new HashSet<>();
		Set<String> deletes = new HashSet<>();
		for (ReceiveCommand cmd : commands) {
			if (cmd.getType() != ReceiveCommand.Type.DELETE) {
				takenNames.add(cmd.getRefName());
				addPrefixesTo(cmd.getRefName(), takenPrefixes);
			} else {
				deletes.add(cmd.getRefName());
			}
		}
		Set<String> initialRefs = refdb.getRefs(RefDatabase.ALL).keySet();
		for (String name : initialRefs) {
			if (!deletes.contains(name)) {
				takenNames.add(name);
				addPrefixesTo(name, takenPrefixes);
			}
		}

		for (ReceiveCommand cmd : commands) {
			if (cmd.getType() != ReceiveCommand.Type.DELETE &&
					takenPrefixes.contains(cmd.getRefName())) {
				// This ref is a prefix of some other ref. This check doesn't apply when
				// this command is a delete, because if the ref is deleted nobody will
				// ever be creating a loose ref with that name.
				lockFailure(cmd, commands);
				return false;
			}
			for (String prefix : getPrefixes(cmd.getRefName())) {
				if (takenNames.contains(prefix)) {
					// A prefix of this ref is already a refname. This check does apply
					// when this command is a delete, because we would need to create the
					// refname as a directory in order to create a lockfile for the
					// to-be-deleted ref.
					lockFailure(cmd, commands);
					return false;
				}
			}
		}
		return true;
	}

	private boolean checkObjectExistence(RevWalk walk,
			List<ReceiveCommand> commands) throws IOException {
		for (ReceiveCommand cmd : commands) {
			try {
				if (!cmd.getNewId().equals(ObjectId.zeroId())) {
					walk.parseAny(cmd.getNewId());
				}
			} catch (MissingObjectException e) {
				// ReceiveCommand#setResult(Result) converts REJECTED to
				// REJECTED_NONFASTFORWARD, even though that result is also used for a
				// missing object. Eagerly handle this case so we can set the right
				// result.
				reject(cmd, ReceiveCommand.Result.REJECTED_MISSING_OBJECT, commands);
				return false;
			}
		}
		return true;
	}

	private boolean checkNonFastForwards(RevWalk walk,
			List<ReceiveCommand> commands) throws IOException {
		if (isAllowNonFastForwards()) {
			return true;
		}
		for (ReceiveCommand cmd : commands) {
			cmd.updateType(walk);
			if (cmd.getType() == ReceiveCommand.Type.UPDATE_NONFASTFORWARD) {
				reject(cmd, REJECTED_NONFASTFORWARD, commands);
				return false;
			}
		}
		return true;
	}

	/**
	 * Lock loose refs corresponding to a list of commands.
	 *
	 * @param commands
	 *            commands that we intend to execute.
	 * @return map of ref name in the input commands to lock file. Always contains
	 *         one entry for each ref in the input list. All locks are acquired
	 *         before returning. If any lock was not able to be acquired: the
	 *         return value is null; no locks are held; and all commands that were
	 *         pending are set to fail with {@code LOCK_FAILURE}.
	 * @throws IOException
	 *             an error occurred other than a failure to acquire; no locks are
	 *             held if this exception is thrown.
	 */
	@Nullable
	private Map<String, LockFile> lockLooseRefs(List<ReceiveCommand> commands)
			throws IOException {
		ReceiveCommand failed = null;
		Map<String, LockFile> locks = new HashMap<>();
		try {
			RETRY: for (int ms : refdb.getRetrySleepMs()) {
				failed = null;
				// Release all locks before trying again, to prevent deadlock.
				unlockAll(locks);
				locks.clear();
				RefDirectory.sleep(ms);

				for (ReceiveCommand c : commands) {
					String name = c.getRefName();
					LockFile lock = new LockFile(refdb.fileFor(name));
					if (locks.put(name, lock) != null) {
						throw new IOException(
								MessageFormat.format(JGitText.get().duplicateRef, name));
					}
					if (!lock.lock()) {
						failed = c;
						continue RETRY;
					}
				}
				Map<String, LockFile> result = locks;
				locks = null;
				return result;
			}
		} finally {
			unlockAll(locks);
		}
		lockFailure(failed != null ? failed : commands.get(0), commands);
		return null;
	}

	private static RefList<Ref> applyUpdates(RevWalk walk, RefList<Ref> refs,
			List<ReceiveCommand> commands) throws IOException {
		// Construct a new RefList by merging the old list with the updates.
		// This assumes that each ref occurs at most once as a ReceiveCommand.
		Collections.sort(commands,
				Comparator.comparing(ReceiveCommand::getRefName));

		int delta = 0;
		for (ReceiveCommand c : commands) {
			switch (c.getType()) {
			case DELETE:
				delta--;
				break;
			case CREATE:
				delta++;
				break;
			default:
			}
		}

		RefList.Builder<Ref> b = new RefList.Builder<>(refs.size() + delta);
		int refIdx = 0;
		int cmdIdx = 0;
		while (refIdx < refs.size() || cmdIdx < commands.size()) {
			Ref ref = (refIdx < refs.size()) ? refs.get(refIdx) : null;
			ReceiveCommand cmd = (cmdIdx < commands.size())
					? commands.get(cmdIdx)
					: null;
			int cmp = 0;
			if (ref != null && cmd != null) {
				cmp = ref.getName().compareTo(cmd.getRefName());
			} else if (ref == null) {
				cmp = 1;
			} else if (cmd == null) {
				cmp = -1;
			}

			if (cmp < 0) {
				b.add(ref);
				refIdx++;
			} else if (cmp > 0) {
				assert cmd != null;
				if (cmd.getType() != ReceiveCommand.Type.CREATE) {
					lockFailure(cmd, commands);
					return null;
				}

				b.add(peeledRef(walk, cmd));
				cmdIdx++;
			} else {
				assert cmd != null;
				assert ref != null;
				if (!cmd.getOldId().equals(ref.getObjectId())) {
					lockFailure(cmd, commands);
					return null;
				}

				if (cmd.getType() != ReceiveCommand.Type.DELETE) {
					b.add(peeledRef(walk, cmd));
				}
				cmdIdx++;
				refIdx++;
			}
		}
		return b.toRefList();
	}

	private void writeReflog(List<ReceiveCommand> commands) {
		PersonIdent ident = getRefLogIdent();
		if (ident == null) {
			ident = new PersonIdent(refdb.getRepository());
		}
		for (ReceiveCommand cmd : commands) {
			// Assume any pending commands have already been executed atomically.
			if (cmd.getResult() != ReceiveCommand.Result.OK) {
				continue;
			}
			String name = cmd.getRefName();

			if (cmd.getType() == ReceiveCommand.Type.DELETE) {
				try {
					RefDirectory.delete(refdb.logFor(name), RefDirectory.levelsIn(name));
				} catch (IOException e) {
					// Ignore failures, see below.
				}
				continue;
			}

			if (isRefLogDisabled(cmd)) {
				continue;
			}

			String msg = getRefLogMessage(cmd);
			if (isRefLogIncludingResult(cmd)) {
				String strResult = toResultString(cmd);
				if (strResult != null) {
					msg = msg.isEmpty()
							? strResult : msg + ": " + strResult; //$NON-NLS-1$
				}
			}
			try {
				new ReflogWriter(refdb, isForceRefLog(cmd))
						.log(name, cmd.getOldId(), cmd.getNewId(), ident, msg);
			} catch (IOException e) {
				// Ignore failures, but continue attempting to write more reflogs.
				//
				// In this storage format, it is impossible to atomically write the
				// reflog with the ref updates, so we have to choose between:
				// a. Propagating this exception and claiming failure, even though the
				//    actual ref updates succeeded.
				// b. Ignoring failures writing the reflog, so we claim success if and
				//    only if the ref updates succeeded.
				// We choose (b) in order to surprise callers the least.
				//
				// Possible future improvements:
				// * Log a warning to a logger.
				// * Retry a fixed number of times in case the error was transient.
			}
		}
	}

	private String toResultString(ReceiveCommand cmd) {
		switch (cmd.getType()) {
		case CREATE:
			return ReflogEntry.PREFIX_CREATED;
		case UPDATE:
			// Match the behavior of a single RefUpdate. In that case, setting the
			// force bit completely bypasses the potentially expensive isMergedInto
			// check, by design, so the reflog message may be inaccurate.
			//
			// Similarly, this class bypasses the isMergedInto checks when the force
			// bit is set, meaning we can't actually distinguish between UPDATE and
			// UPDATE_NONFASTFORWARD when isAllowNonFastForwards() returns true.
			return isAllowNonFastForwards()
					? ReflogEntry.PREFIX_FORCED_UPDATE : ReflogEntry.PREFIX_FAST_FORWARD;
		case UPDATE_NONFASTFORWARD:
			return ReflogEntry.PREFIX_FORCED_UPDATE;
		default:
			return null;
		}
	}

	private static Ref peeledRef(RevWalk walk, ReceiveCommand cmd)
			throws IOException {
		ObjectId newId = cmd.getNewId().copy();
		RevObject obj = walk.parseAny(newId);
		if (obj instanceof RevTag) {
			return new ObjectIdRef.PeeledTag(
					Ref.Storage.PACKED, cmd.getRefName(), newId, walk.peel(obj).copy());
		}
		return new ObjectIdRef.PeeledNonTag(
				Ref.Storage.PACKED, cmd.getRefName(), newId);
	}

	private static void unlockAll(@Nullable Map<?, LockFile> locks) {
		if (locks != null) {
			locks.values().forEach(LockFile::unlock);
		}
	}

	private static void lockFailure(ReceiveCommand cmd,
			List<ReceiveCommand> commands) {
		reject(cmd, LOCK_FAILURE, commands);
	}

	private static void reject(ReceiveCommand cmd, ReceiveCommand.Result result,
			List<ReceiveCommand> commands) {
		reject(cmd, result, null, commands);
	}

	private static void reject(ReceiveCommand cmd, ReceiveCommand.Result result,
			String why, List<ReceiveCommand> commands) {
		cmd.setResult(result, why);
		for (ReceiveCommand c2 : commands) {
			if (c2.getResult() == ReceiveCommand.Result.OK) {
				// Undo OK status so ReceiveCommand#abort aborts it. Assumes this method
				// is always called before committing any updates to disk.
				c2.setResult(ReceiveCommand.Result.NOT_ATTEMPTED);
			}
		}
		ReceiveCommand.abort(commands);
	}
}
