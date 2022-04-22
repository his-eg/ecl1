/*
 * Copyright (C) 2007, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2006-2021, Shawn O. Pearce <spearce@spearce.org> and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.eclipse.jgit.internal.storage.file;

import static org.eclipse.jgit.lib.Constants.LOCK_SUFFIX;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.text.MessageFormat;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

import org.eclipse.jgit.internal.JGitText;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.util.FS;
import org.eclipse.jgit.util.FS.LockToken;
import org.eclipse.jgit.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Git style file locking and replacement.
 * <p>
 * To modify a ref file Git tries to use an atomic update approach: we write the
 * new data into a brand new file, then rename it in place over the old name.
 * This way we can just delete the temporary file if anything goes wrong, and
 * nothing has been damaged. To coordinate access from multiple processes at
 * once Git tries to atomically create the new temporary file under a well-known
 * name.
 */
public class LockFile {
	private static final Logger LOG = LoggerFactory.getLogger(LockFile.class);

	/**
	 * Unlock the given file.
	 * <p>
	 * This method can be used for recovering from a thrown
	 * {@link org.eclipse.jgit.errors.LockFailedException} . This method does
	 * not validate that the lock is or is not currently held before attempting
	 * to unlock it.
	 *
	 * @param file
	 *            a {@link java.io.File} object.
	 * @return true if unlocked, false if unlocking failed
	 */
	public static boolean unlock(File file) {
		final File lockFile = getLockFile(file);
		final int flags = FileUtils.RETRY | FileUtils.SKIP_MISSING;
		try {
			FileUtils.delete(lockFile, flags);
		} catch (IOException ignored) {
			// Ignore and return whether lock file still exists
		}
		return !lockFile.exists();
	}

	/**
	 * Get the lock file corresponding to the given file.
	 *
	 * @param file
	 * @return lock file
	 */
	static File getLockFile(File file) {
		return new File(file.getParentFile(),
				file.getName() + LOCK_SUFFIX);
	}

	/** Filter to skip over active lock files when listing a directory. */
	static final FilenameFilter FILTER = (File dir,
			String name) -> !name.endsWith(LOCK_SUFFIX);

	private final File ref;

	private final File lck;

	private boolean haveLck;

	private FileOutputStream os;

	private boolean needSnapshot;

	private boolean fsync;

	private boolean isAppend;

	private boolean written;

	private boolean snapshotNoConfig;

	private FileSnapshot commitSnapshot;

	private LockToken token;

	/**
	 * Create a new lock for any file.
	 *
	 * @param f
	 *            the file that will be locked.
	 */
	public LockFile(File f) {
		ref = f;
		lck = getLockFile(ref);
	}

	/**
	 * Try to establish the lock.
	 *
	 * @return true if the lock is now held by the caller; false if it is held
	 *         by someone else.
	 * @throws java.io.IOException
	 *             the temporary output file could not be created. The caller
	 *             does not hold the lock.
	 */
	public boolean lock() throws IOException {
		if (haveLck) {
			throw new IllegalStateException(
					MessageFormat.format(JGitText.get().lockAlreadyHeld, ref));
		}
		FileUtils.mkdirs(lck.getParentFile(), true);
		try {
			token = FS.DETECTED.createNewFileAtomic(lck);
		} catch (IOException e) {
			LOG.error(JGitText.get().failedCreateLockFile, lck, e);
			throw e;
		}
		boolean obtainedLock = token.isCreated();
		if (obtainedLock) {
			haveLck = true;
			isAppend = false;
			written = false;
		} else {
			closeToken();
		}
		return obtainedLock;
	}

	/**
	 * Try to establish the lock for appending.
	 *
	 * @return true if the lock is now held by the caller; false if it is held
	 *         by someone else.
	 * @throws java.io.IOException
	 *             the temporary output file could not be created. The caller
	 *             does not hold the lock.
	 */
	public boolean lockForAppend() throws IOException {
		if (!lock()) {
			return false;
		}
		copyCurrentContent();
		isAppend = true;
		written = false;
		return true;
	}

	// For tests only
	boolean isLocked() {
		return haveLck;
	}

	private FileOutputStream getStream() throws IOException {
		return new FileOutputStream(lck, isAppend);
	}

	/**
	 * Copy the current file content into the temporary file.
	 * <p>
	 * This method saves the current file content by inserting it into the
	 * temporary file, so that the caller can safely append rather than replace
	 * the primary file.
	 * <p>
	 * This method does nothing if the current file does not exist, or exists
	 * but is empty.
	 *
	 * @throws java.io.IOException
	 *             the temporary file could not be written, or a read error
	 *             occurred while reading from the current file. The lock is
	 *             released before throwing the underlying IO exception to the
	 *             caller.
	 * @throws java.lang.RuntimeException
	 *             the temporary file could not be written. The lock is released
	 *             before throwing the underlying exception to the caller.
	 */
	public void copyCurrentContent() throws IOException {
		requireLock();
		try (FileOutputStream out = getStream()) {
			try (FileInputStream fis = new FileInputStream(ref)) {
				if (fsync) {
					FileChannel in = fis.getChannel();
					long pos = 0;
					long cnt = in.size();
					while (0 < cnt) {
						long r = out.getChannel().transferFrom(in, pos, cnt);
						pos += r;
						cnt -= r;
					}
				} else {
					final byte[] buf = new byte[2048];
					int r;
					while ((r = fis.read(buf)) >= 0) {
						out.write(buf, 0, r);
					}
				}
			} catch (FileNotFoundException fnfe) {
				if (ref.exists()) {
					throw fnfe;
				}
				// Don't worry about a file that doesn't exist yet, it
				// conceptually has no current content to copy.
			}
		} catch (IOException | RuntimeException | Error ioe) {
			unlock();
			throw ioe;
		}
	}

	/**
	 * Write an ObjectId and LF to the temporary file.
	 *
	 * @param id
	 *            the id to store in the file. The id will be written in hex,
	 *            followed by a sole LF.
	 * @throws java.io.IOException
	 *             the temporary file could not be written. The lock is released
	 *             before throwing the underlying IO exception to the caller.
	 * @throws java.lang.RuntimeException
	 *             the temporary file could not be written. The lock is released
	 *             before throwing the underlying exception to the caller.
	 */
	public void write(ObjectId id) throws IOException {
		byte[] buf = new byte[Constants.OBJECT_ID_STRING_LENGTH + 1];
		id.copyTo(buf, 0);
		buf[Constants.OBJECT_ID_STRING_LENGTH] = '\n';
		write(buf);
	}

	/**
	 * Write arbitrary data to the temporary file.
	 *
	 * @param content
	 *            the bytes to store in the temporary file. No additional bytes
	 *            are added, so if the file must end with an LF it must appear
	 *            at the end of the byte array.
	 * @throws java.io.IOException
	 *             the temporary file could not be written. The lock is released
	 *             before throwing the underlying IO exception to the caller.
	 * @throws java.lang.RuntimeException
	 *             the temporary file could not be written. The lock is released
	 *             before throwing the underlying exception to the caller.
	 */
	public void write(byte[] content) throws IOException {
		requireLock();
		try (FileOutputStream out = getStream()) {
			if (written) {
				throw new IOException(MessageFormat
						.format(JGitText.get().lockStreamClosed, ref));
			}
			if (fsync) {
				FileChannel fc = out.getChannel();
				ByteBuffer buf = ByteBuffer.wrap(content);
				while (0 < buf.remaining()) {
					fc.write(buf);
				}
				fc.force(true);
			} else {
				out.write(content);
			}
			written = true;
		} catch (IOException | RuntimeException | Error ioe) {
			unlock();
			throw ioe;
		}
	}

	/**
	 * Obtain the direct output stream for this lock.
	 * <p>
	 * The stream may only be accessed once, and only after {@link #lock()} has
	 * been successfully invoked and returned true. Callers must close the
	 * stream prior to calling {@link #commit()} to commit the change.
	 *
	 * @return a stream to write to the new file. The stream is unbuffered.
	 */
	public OutputStream getOutputStream() {
		requireLock();

		if (written || os != null) {
			throw new IllegalStateException(MessageFormat
					.format(JGitText.get().lockStreamMultiple, ref));
		}

		return new OutputStream() {

			private OutputStream out;

			private boolean closed;

			private OutputStream get() throws IOException {
				if (written) {
					throw new IOException(MessageFormat
							.format(JGitText.get().lockStreamMultiple, ref));
				}
				if (out == null) {
					os = getStream();
					if (fsync) {
						out = Channels.newOutputStream(os.getChannel());
					} else {
						out = os;
					}
				}
				return out;
			}

			@Override
			public void write(byte[] b, int o, int n) throws IOException {
				get().write(b, o, n);
			}

			@Override
			public void write(byte[] b) throws IOException {
				get().write(b);
			}

			@Override
			public void write(int b) throws IOException {
				get().write(b);
			}

			@Override
			public void close() throws IOException {
				if (closed) {
					return;
				}
				closed = true;
				try {
					if (written) {
						throw new IOException(MessageFormat
								.format(JGitText.get().lockStreamClosed, ref));
					}
					if (out != null) {
						if (fsync) {
							os.getChannel().force(true);
						}
						out.close();
						os = null;
					}
					written = true;
				} catch (IOException | RuntimeException | Error ioe) {
					unlock();
					throw ioe;
				}
			}
		};
	}

	void requireLock() {
		if (!haveLck) {
			unlock();
			throw new IllegalStateException(MessageFormat.format(JGitText.get().lockOnNotHeld, ref));
		}
	}

	/**
	 * Request that {@link #commit()} remember modification time.
	 * <p>
	 * This is an alias for {@code setNeedSnapshot(true)}.
	 *
	 * @param on
	 *            true if the commit method must remember the modification time.
	 */
	public void setNeedStatInformation(boolean on) {
		setNeedSnapshot(on);
	}

	/**
	 * Request that {@link #commit()} remember the
	 * {@link org.eclipse.jgit.internal.storage.file.FileSnapshot}.
	 *
	 * @param on
	 *            true if the commit method must remember the FileSnapshot.
	 */
	public void setNeedSnapshot(boolean on) {
		needSnapshot = on;
	}

	/**
	 * Request that {@link #commit()} remember the
	 * {@link org.eclipse.jgit.internal.storage.file.FileSnapshot} without using
	 * config file to get filesystem timestamp resolution.
	 * This method should be invoked before the file is accessed.
	 * It is used by FileBasedConfig to avoid endless recursion.
	 *
	 * @param on
	 *            true if the commit method must remember the FileSnapshot.
	 */
	public void setNeedSnapshotNoConfig(boolean on) {
		needSnapshot = on;
		snapshotNoConfig = on;
	}

	/**
	 * Request that {@link #commit()} force dirty data to the drive.
	 *
	 * @param on
	 *            true if dirty data should be forced to the drive.
	 */
	public void setFSync(boolean on) {
		fsync = on;
	}

	/**
	 * Wait until the lock file information differs from the old file.
	 * <p>
	 * This method tests the last modification date. If both are the same, this
	 * method sleeps until it can force the new lock file's modification date to
	 * be later than the target file.
	 *
	 * @throws java.lang.InterruptedException
	 *             the thread was interrupted before the last modified date of
	 *             the lock file was different from the last modified date of
	 *             the target file.
	 */
	public void waitForStatChange() throws InterruptedException {
		FileSnapshot o = FileSnapshot.save(ref);
		FileSnapshot n = FileSnapshot.save(lck);
		long fsTimeResolution = FS.getFileStoreAttributes(lck.toPath())
				.getFsTimestampResolution().toNanos();
		while (o.equals(n)) {
			TimeUnit.NANOSECONDS.sleep(fsTimeResolution);
			try {
				Files.setLastModifiedTime(lck.toPath(),
						FileTime.from(Instant.now()));
			} catch (IOException e) {
				n.waitUntilNotRacy();
			}
			n = FileSnapshot.save(lck);
		}
	}

	/**
	 * Commit this change and release the lock.
	 * <p>
	 * If this method fails (returns false) the lock is still released.
	 *
	 * @return true if the commit was successful and the file contains the new
	 *         data; false if the commit failed and the file remains with the
	 *         old data.
	 * @throws java.lang.IllegalStateException
	 *             the lock is not held.
	 */
	public boolean commit() {
		if (os != null) {
			unlock();
			throw new IllegalStateException(MessageFormat.format(JGitText.get().lockOnNotClosed, ref));
		}

		saveStatInformation();
		try {
			FileUtils.rename(lck, ref, StandardCopyOption.ATOMIC_MOVE);
			haveLck = false;
			isAppend = false;
			written = false;
			closeToken();
			return true;
		} catch (IOException e) {
			unlock();
			return false;
		}
	}

	private void closeToken() {
		if (token != null) {
			token.close();
			token = null;
		}
	}

	private void saveStatInformation() {
		if (needSnapshot) {
			commitSnapshot = snapshotNoConfig ?
				// don't use config in this snapshot to avoid endless recursion
				FileSnapshot.saveNoConfig(lck)
				: FileSnapshot.save(lck);
		}
	}

	/**
	 * Get the modification time of the output file when it was committed.
	 *
	 * @return modification time of the lock file right before we committed it.
	 * @deprecated use {@link #getCommitLastModifiedInstant()} instead
	 */
	@Deprecated
	public long getCommitLastModified() {
		return commitSnapshot.lastModified();
	}

	/**
	 * Get the modification time of the output file when it was committed.
	 *
	 * @return modification time of the lock file right before we committed it.
	 */
	public Instant getCommitLastModifiedInstant() {
		return commitSnapshot.lastModifiedInstant();
	}

	/**
	 * Get the {@link FileSnapshot} just before commit.
	 *
	 * @return get the {@link FileSnapshot} just before commit.
	 */
	public FileSnapshot getCommitSnapshot() {
		return commitSnapshot;
	}

	/**
	 * Update the commit snapshot {@link #getCommitSnapshot()} before commit.
	 * <p>
	 * This may be necessary if you need time stamp before commit occurs, e.g
	 * while writing the index.
	 */
	public void createCommitSnapshot() {
		saveStatInformation();
	}

	/**
	 * Unlock this file and abort this change.
	 * <p>
	 * The temporary file (if created) is deleted before returning.
	 */
	public void unlock() {
		if (os != null) {
			try {
				os.close();
			} catch (IOException e) {
				LOG.error(MessageFormat
						.format(JGitText.get().unlockLockFileFailed, lck), e);
			}
			os = null;
		}

		if (haveLck) {
			haveLck = false;
			try {
				FileUtils.delete(lck, FileUtils.RETRY);
			} catch (IOException e) {
				LOG.error(MessageFormat
						.format(JGitText.get().unlockLockFileFailed, lck), e);
			} finally {
				closeToken();
			}
		}
		isAppend = false;
		written = false;
	}

	/** {@inheritDoc} */
	@SuppressWarnings("nls")
	@Override
	public String toString() {
		return "LockFile[" + lck + ", haveLck=" + haveLck + "]";
	}
}
