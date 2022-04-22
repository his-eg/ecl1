/*
 * Copyright (C) 2010, Google Inc.
 * Copyright (C) 2009, Robin Rosenberg and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.eclipse.jgit.internal.storage.file;

import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.StandardCopyOption;

import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.RefRename;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.lib.RefUpdate.Result;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Rename any reference stored by {@link RefDirectory}.
 * <p>
 * This class works by first renaming the source reference to a temporary name,
 * then renaming the temporary name to the final destination reference.
 * <p>
 * This strategy permits switching a reference like {@code refs/heads/foo},
 * which is a file, to {@code refs/heads/foo/bar}, which is stored inside a
 * directory that happens to match the source name.
 */
class RefDirectoryRename extends RefRename {
	private static final Logger LOG = LoggerFactory
			.getLogger(RefDirectoryRename.class);

	private final RefDirectory refdb;

	/**
	 * The value of the source reference at the start of the rename.
	 * <p>
	 * At the end of the rename the destination reference must have this same
	 * value, otherwise we have a concurrent update and the rename must fail
	 * without making any changes.
	 */
	private ObjectId objId;

	/** A reference we backup {@link #objId} into during the rename. */
	private RefDirectoryUpdate tmp;

	RefDirectoryRename(RefDirectoryUpdate src, RefDirectoryUpdate dst) {
		super(src, dst);
		refdb = src.getRefDatabase();
	}

	/** {@inheritDoc} */
	@Override
	protected Result doRename() throws IOException {
		if (source.getRef().isSymbolic())
			return Result.IO_FAILURE; // not supported

		objId = source.getOldObjectId();
		boolean updateHEAD = needToUpdateHEAD();
		tmp = refdb.newTemporaryUpdate();
		try (RevWalk rw = new RevWalk(refdb.getRepository())) {
			// First backup the source so its never unreachable.
			tmp.setNewObjectId(objId);
			tmp.setForceUpdate(true);
			tmp.disableRefLog();
			switch (tmp.update(rw)) {
			case NEW:
			case FORCED:
			case NO_CHANGE:
				break;
			default:
				return tmp.getResult();
			}

			// Save the source's log under the temporary name, we must do
			// this before we delete the source, otherwise we lose the log.
			if (!renameLog(source, tmp))
				return Result.IO_FAILURE;

			// If HEAD has to be updated, link it now to destination.
			// We have to link before we delete, otherwise the delete
			// fails because its the current branch.
			RefUpdate dst = destination;
			if (updateHEAD) {
				if (!linkHEAD(destination)) {
					renameLog(tmp, source);
					return Result.LOCK_FAILURE;
				}

				// Replace the update operation so HEAD will log the rename.
				dst = refdb.newUpdate(Constants.HEAD, false);
				dst.setRefLogIdent(destination.getRefLogIdent());
				dst.setRefLogMessage(destination.getRefLogMessage(), false);
			}

			// Delete the source name so its path is free for replacement.
			source.setExpectedOldObjectId(objId);
			source.setForceUpdate(true);
			source.disableRefLog();
			if (source.delete(rw) != Result.FORCED) {
				renameLog(tmp, source);
				if (updateHEAD)
					linkHEAD(source);
				return source.getResult();
			}

			// Move the log to the destination.
			if (!renameLog(tmp, destination)) {
				renameLog(tmp, source);
				source.setExpectedOldObjectId(ObjectId.zeroId());
				source.setNewObjectId(objId);
				source.update(rw);
				if (updateHEAD)
					linkHEAD(source);
				return Result.IO_FAILURE;
			}

			// Create the destination, logging the rename during the creation.
			dst.setExpectedOldObjectId(ObjectId.zeroId());
			dst.setNewObjectId(objId);
			if (dst.update(rw) != Result.NEW) {
				// If we didn't create the destination we have to undo
				// our work. Put the log back and restore source.
				if (renameLog(destination, tmp))
					renameLog(tmp, source);
				source.setExpectedOldObjectId(ObjectId.zeroId());
				source.setNewObjectId(objId);
				source.update(rw);
				if (updateHEAD)
					linkHEAD(source);
				return dst.getResult();
			}

			return Result.RENAMED;
		} finally {
			// Always try to free the temporary name.
			try {
				refdb.delete(tmp);
			} catch (IOException err) {
				FileUtils.delete(refdb.fileFor(tmp.getName()));
			}
		}
	}

	private boolean renameLog(RefUpdate src, RefUpdate dst) {
		File srcLog = refdb.logFor(src.getName());
		File dstLog = refdb.logFor(dst.getName());

		if (!srcLog.exists())
			return true;

		if (!rename(srcLog, dstLog))
			return false;

		try {
			final int levels = RefDirectory.levelsIn(src.getName()) - 2;
			RefDirectory.delete(srcLog, levels);
			return true;
		} catch (IOException e) {
			rename(dstLog, srcLog);
			return false;
		}
	}

	private static boolean rename(File src, File dst) {
		try {
			FileUtils.rename(src, dst, StandardCopyOption.ATOMIC_MOVE);
			return true;
		} catch (AtomicMoveNotSupportedException e) {
			LOG.error(e.getMessage(), e);
		} catch (IOException e) {
			// ignore
		}

		File dir = dst.getParentFile();
		if ((dir.exists() || !dir.mkdirs()) && !dir.isDirectory())
			return false;
		try {
			FileUtils.rename(src, dst, StandardCopyOption.ATOMIC_MOVE);
			return true;
		} catch (IOException e) {
			LOG.error(e.getMessage(), e);
			return false;
		}
	}

	private boolean linkHEAD(RefUpdate target) {
		try {
			RefUpdate u = refdb.newUpdate(Constants.HEAD, false);
			u.disableRefLog();
			switch (u.link(target.getName())) {
			case NEW:
			case FORCED:
			case NO_CHANGE:
				return true;
			default:
				return false;
			}
		} catch (IOException e) {
			return false;
		}
	}
}
