/*
 * Copyright (C) 2008-2009, Google Inc.
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org> and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.eclipse.jgit.util;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;

import org.eclipse.jgit.internal.JGitText;
import org.eclipse.jgit.lib.NullProgressMonitor;
import org.eclipse.jgit.lib.ProgressMonitor;

/**
 * A fully buffered output stream.
 * <p>
 * Subclasses determine the behavior when the in-memory buffer capacity has been
 * exceeded and additional bytes are still being received for output.
 */
public abstract class TemporaryBuffer extends OutputStream {
	/** Default limit for in-core storage. */
	protected static final int DEFAULT_IN_CORE_LIMIT = 1024 * 1024;

	/** Chain of data, if we are still completely in-core; otherwise null. */
	ArrayList<Block> blocks;

	/**
	 * Maximum number of bytes we will permit storing in memory.
	 * <p>
	 * When this limit is reached the data will be shifted to a file on disk,
	 * preventing the JVM heap from growing out of control.
	 */
	private int inCoreLimit;

	/** Initial size of block list. */
	private int initialBlocks;

	/** If {@link #inCoreLimit} has been reached, remainder goes here. */
	private OutputStream overflow;

	/**
	 * Create a new empty temporary buffer.
	 *
	 * @param limit
	 *            maximum number of bytes to store in memory before entering the
	 *            overflow output path; also used as the estimated size.
	 */
	protected TemporaryBuffer(int limit) {
		this(limit, limit);
	}

	/**
	 * Create a new empty temporary buffer.
	 *
	 * @param estimatedSize
	 *            estimated size of storage used, to size the initial list of
	 *            block pointers.
	 * @param limit
	 *            maximum number of bytes to store in memory before entering the
	 *            overflow output path.
	 * @since 4.0
	 */
	protected TemporaryBuffer(int estimatedSize, int limit) {
		if (estimatedSize > limit)
			throw new IllegalArgumentException();
		this.inCoreLimit = limit;
		this.initialBlocks = (estimatedSize - 1) / Block.SZ + 1;
		reset();
	}

	/** {@inheritDoc} */
	@Override
	public void write(int b) throws IOException {
		if (overflow != null) {
			overflow.write(b);
			return;
		}

		Block s = last();
		if (s.isFull()) {
			if (reachedInCoreLimit()) {
				overflow.write(b);
				return;
			}

			s = new Block();
			blocks.add(s);
		}
		s.buffer[s.count++] = (byte) b;
	}

	/** {@inheritDoc} */
	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		if (overflow == null) {
			while (len > 0) {
				Block s = last();
				if (s.isFull()) {
					if (reachedInCoreLimit())
						break;

					s = new Block();
					blocks.add(s);
				}

				final int n = Math.min(s.buffer.length - s.count, len);
				System.arraycopy(b, off, s.buffer, s.count, n);
				s.count += n;
				len -= n;
				off += n;
			}
		}

		if (len > 0)
			overflow.write(b, off, len);
	}

	/**
	 * Dumps the entire buffer into the overflow stream, and flushes it.
	 *
	 * @throws java.io.IOException
	 *             the overflow stream cannot be started, or the buffer contents
	 *             cannot be written to it, or it failed to flush.
	 */
	protected void doFlush() throws IOException {
		if (overflow == null)
			switchToOverflow();
		overflow.flush();
	}

	/**
	 * Copy all bytes remaining on the input stream into this buffer.
	 *
	 * @param in
	 *            the stream to read from, until EOF is reached.
	 * @throws java.io.IOException
	 *             an error occurred reading from the input stream, or while
	 *             writing to a local temporary file.
	 */
	public void copy(InputStream in) throws IOException {
		if (blocks != null) {
			for (;;) {
				Block s = last();
				if (s.isFull()) {
					if (reachedInCoreLimit())
						break;
					s = new Block();
					blocks.add(s);
				}

				int n = in.read(s.buffer, s.count, s.buffer.length - s.count);
				if (n < 1)
					return;
				s.count += n;
			}
		}

		final byte[] tmp = new byte[Block.SZ];
		int n;
		while ((n = in.read(tmp)) > 0)
			overflow.write(tmp, 0, n);
	}

	/**
	 * Obtain the length (in bytes) of the buffer.
	 * <p>
	 * The length is only accurate after {@link #close()} has been invoked.
	 *
	 * @return total length of the buffer, in bytes.
	 */
	public long length() {
		return inCoreLength();
	}

	private long inCoreLength() {
		final Block last = last();
		return ((long) blocks.size() - 1) * Block.SZ + last.count;
	}

	/**
	 * Convert this buffer's contents into a contiguous byte array.
	 * <p>
	 * The buffer is only complete after {@link #close()} has been invoked.
	 *
	 * @return the complete byte array; length matches {@link #length()}.
	 * @throws java.io.IOException
	 *             an error occurred reading from a local temporary file
	 */
	public byte[] toByteArray() throws IOException {
		final long len = length();
		if (Integer.MAX_VALUE < len)
			throw new OutOfMemoryError(JGitText.get().lengthExceedsMaximumArraySize);
		final byte[] out = new byte[(int) len];
		int outPtr = 0;
		for (Block b : blocks) {
			System.arraycopy(b.buffer, 0, out, outPtr, b.count);
			outPtr += b.count;
		}
		return out;
	}

	/**
	 * Convert first {@code limit} number of bytes of the buffer content to
	 * String.
	 *
	 * @param limit
	 *            the maximum number of bytes to be converted to String
	 * @return first {@code limit} number of bytes of the buffer content
	 *         converted to String.
	 * @since 5.12
	 */
	public String toString(int limit) {
		try {
			return RawParseUtils.decode(toByteArray(limit));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/**
	 * Convert this buffer's contents into a contiguous byte array. If this size
	 * of the buffer exceeds the limit only return the first {@code limit} bytes
	 * <p>
	 * The buffer is only complete after {@link #close()} has been invoked.
	 *
	 * @param limit
	 *            the maximum number of bytes to be returned
	 * @return the byte array limited to {@code limit} bytes.
	 * @throws java.io.IOException
	 *             an error occurred reading from a local temporary file
	 * @since 4.2
	 */
	public byte[] toByteArray(int limit) throws IOException {
		final long len = Math.min(length(), limit);
		if (Integer.MAX_VALUE < len)
			throw new OutOfMemoryError(
					JGitText.get().lengthExceedsMaximumArraySize);
		int length = (int) len;
		final byte[] out = new byte[length];
		int outPtr = 0;
		for (Block b : blocks) {
			int toCopy = Math.min(length - outPtr, b.count);
			System.arraycopy(b.buffer, 0, out, outPtr, toCopy);
			outPtr += toCopy;
			if (outPtr == length) {
				break;
			}
		}
		return out;
	}

	/**
	 * Send this buffer to an output stream.
	 * <p>
	 * This method may only be invoked after {@link #close()} has completed
	 * normally, to ensure all data is completely transferred.
	 *
	 * @param os
	 *            stream to send this buffer's complete content to.
	 * @param pm
	 *            if not null progress updates are sent here. Caller should
	 *            initialize the task and the number of work units to <code>
	 *            {@link #length()}/1024</code>.
	 * @throws java.io.IOException
	 *             an error occurred reading from a temporary file on the local
	 *             system, or writing to the output stream.
	 */
	public void writeTo(OutputStream os, ProgressMonitor pm)
			throws IOException {
		if (pm == null)
			pm = NullProgressMonitor.INSTANCE;
		for (Block b : blocks) {
			os.write(b.buffer, 0, b.count);
			pm.update(b.count / 1024);
		}
	}

	/**
	 * Open an input stream to read from the buffered data.
	 * <p>
	 * This method may only be invoked after {@link #close()} has completed
	 * normally, to ensure all data is completely transferred.
	 *
	 * @return a stream to read from the buffer. The caller must close the
	 *         stream when it is no longer useful.
	 * @throws java.io.IOException
	 *             an error occurred opening the temporary file.
	 */
	public InputStream openInputStream() throws IOException {
		return new BlockInputStream();
	}

	/**
	 * Same as {@link #openInputStream()} but handling destruction of any
	 * associated resources automatically when closing the returned stream.
	 *
	 * @return an InputStream which will automatically destroy any associated
	 *         temporary file on {@link #close()}
	 * @throws IOException
	 *             in case of an error.
	 * @since 4.11
	 */
	public InputStream openInputStreamWithAutoDestroy() throws IOException {
		return new BlockInputStream() {
			@Override
			public void close() throws IOException {
				super.close();
				destroy();
			}
		};
	}

	/**
	 * Reset this buffer for reuse, purging all buffered content.
	 */
	public void reset() {
		if (overflow != null) {
			destroy();
		}
		if (blocks != null)
			blocks.clear();
		else
			blocks = new ArrayList<>(initialBlocks);
		blocks.add(new Block(Math.min(inCoreLimit, Block.SZ)));
	}

	/**
	 * Open the overflow output stream, so the remaining output can be stored.
	 *
	 * @return the output stream to receive the buffered content, followed by
	 *         the remaining output.
	 * @throws java.io.IOException
	 *             the buffer cannot create the overflow stream.
	 */
	protected abstract OutputStream overflow() throws IOException;

	private Block last() {
		return blocks.get(blocks.size() - 1);
	}

	private boolean reachedInCoreLimit() throws IOException {
		if (inCoreLength() < inCoreLimit)
			return false;

		switchToOverflow();
		return true;
	}

	private void switchToOverflow() throws IOException {
		overflow = overflow();

		final Block last = blocks.remove(blocks.size() - 1);
		for (Block b : blocks)
			overflow.write(b.buffer, 0, b.count);
		blocks = null;

		overflow = new BufferedOutputStream(overflow, Block.SZ);
		overflow.write(last.buffer, 0, last.count);
	}

	/** {@inheritDoc} */
	@Override
	public void close() throws IOException {
		if (overflow != null) {
			try {
				overflow.close();
			} finally {
				overflow = null;
			}
		}
	}

	/**
	 * Clear this buffer so it has no data, and cannot be used again.
	 */
	public void destroy() {
		blocks = null;

		if (overflow != null) {
			try {
				overflow.close();
			} catch (IOException err) {
				// We shouldn't encounter an error closing the file.
			} finally {
				overflow = null;
			}
		}
	}

	/**
	 * A fully buffered output stream using local disk storage for large data.
	 * <p>
	 * Initially this output stream buffers to memory and is therefore similar
	 * to ByteArrayOutputStream, but it shifts to using an on disk temporary
	 * file if the output gets too large.
	 * <p>
	 * The content of this buffered stream may be sent to another OutputStream
	 * only after this stream has been properly closed by {@link #close()}.
	 */
	public static class LocalFile extends TemporaryBuffer {
		/** Directory to store the temporary file under. */
		private final File directory;

		/**
		 * Location of our temporary file if we are on disk; otherwise null.
		 * <p>
		 * If we exceeded the {@link #inCoreLimit} we nulled out {@link #blocks}
		 * and created this file instead. All output goes here through
		 * {@link #overflow}.
		 */
		private File onDiskFile;

		/**
		 * Create a new temporary buffer, limiting memory usage.
		 *
		 * @param directory
		 *            if the buffer has to spill over into a temporary file, the
		 *            directory where the file should be saved. If null the
		 *            system default temporary directory (for example /tmp) will
		 *            be used instead.
		 */
		public LocalFile(File directory) {
			this(directory, DEFAULT_IN_CORE_LIMIT);
		}

		/**
		 * Create a new temporary buffer, limiting memory usage.
		 *
		 * @param directory
		 *            if the buffer has to spill over into a temporary file, the
		 *            directory where the file should be saved. If null the
		 *            system default temporary directory (for example /tmp) will
		 *            be used instead.
		 * @param inCoreLimit
		 *            maximum number of bytes to store in memory. Storage beyond
		 *            this limit will use the local file.
		 */
		public LocalFile(File directory, int inCoreLimit) {
			super(inCoreLimit);
			this.directory = directory;
		}

		@Override
		protected OutputStream overflow() throws IOException {
			onDiskFile = File.createTempFile("jgit_", ".buf", directory); //$NON-NLS-1$ //$NON-NLS-2$
			return new BufferedOutputStream(new FileOutputStream(onDiskFile));
		}

		@Override
		public long length() {
			if (onDiskFile == null) {
				return super.length();
			}
			return onDiskFile.length();
		}

		@Override
		public byte[] toByteArray() throws IOException {
			if (onDiskFile == null) {
				return super.toByteArray();
			}

			final long len = length();
			if (Integer.MAX_VALUE < len)
				throw new OutOfMemoryError(JGitText.get().lengthExceedsMaximumArraySize);
			final byte[] out = new byte[(int) len];
			try (FileInputStream in = new FileInputStream(onDiskFile)) {
				IO.readFully(in, out, 0, (int) len);
			}
			return out;
		}

		@Override
		public byte[] toByteArray(int limit) throws IOException {
			if (onDiskFile == null) {
				return super.toByteArray(limit);
			}
			final long len = Math.min(length(), limit);
			if (Integer.MAX_VALUE < len) {
				throw new OutOfMemoryError(
						JGitText.get().lengthExceedsMaximumArraySize);
			}
			final byte[] out = new byte[(int) len];
			try (FileInputStream in = new FileInputStream(onDiskFile)) {
				int read = 0;
				int chunk;
				while ((chunk = in.read(out, read, out.length - read)) >= 0) {
					read += chunk;
					if (read == out.length) {
						break;
					}
				}
			}
			return out;
		}

		@Override
		public void writeTo(OutputStream os, ProgressMonitor pm)
				throws IOException {
			if (onDiskFile == null) {
				super.writeTo(os, pm);
				return;
			}
			if (pm == null)
				pm = NullProgressMonitor.INSTANCE;
			try (FileInputStream in = new FileInputStream(onDiskFile)) {
				int cnt;
				final byte[] buf = new byte[Block.SZ];
				while ((cnt = in.read(buf)) >= 0) {
					os.write(buf, 0, cnt);
					pm.update(cnt / 1024);
				}
			}
		}

		@Override
		public InputStream openInputStream() throws IOException {
			if (onDiskFile == null)
				return super.openInputStream();
			return new FileInputStream(onDiskFile);
		}

		@Override
		public InputStream openInputStreamWithAutoDestroy() throws IOException {
			if (onDiskFile == null) {
				return super.openInputStreamWithAutoDestroy();
			}
			return new FileInputStream(onDiskFile) {
				@Override
				public void close() throws IOException {
					super.close();
					destroy();
				}
			};
		}

		@Override
		public void destroy() {
			super.destroy();

			if (onDiskFile != null) {
				try {
					if (!onDiskFile.delete())
						onDiskFile.deleteOnExit();
				} finally {
					onDiskFile = null;
				}
			}
		}
	}

	/**
	 * A temporary buffer that will never exceed its in-memory limit.
	 * <p>
	 * If the in-memory limit is reached an IOException is thrown, rather than
	 * attempting to spool to local disk.
	 */
	public static class Heap extends TemporaryBuffer {
		/**
		 * Create a new heap buffer with a maximum storage limit.
		 *
		 * @param limit
		 *            maximum number of bytes that can be stored in this buffer;
		 *            also used as the estimated size. Storing beyond this many
		 *            will cause an IOException to be thrown during write.
		 */
		public Heap(int limit) {
			super(limit);
		}

		/**
		 * Create a new heap buffer with a maximum storage limit.
		 *
		 * @param estimatedSize
		 *            estimated size of storage used, to size the initial list of
		 *            block pointers.
		 * @param limit
		 *            maximum number of bytes that can be stored in this buffer.
		 *            Storing beyond this many will cause an IOException to be
		 *            thrown during write.
		 * @since 4.0
		 */
		public Heap(int estimatedSize, int limit) {
			super(estimatedSize, limit);
		}

		@Override
		protected OutputStream overflow() throws IOException {
			throw new IOException(JGitText.get().inMemoryBufferLimitExceeded);
		}
	}

	static class Block {
		static final int SZ = 8 * 1024;

		final byte[] buffer;

		int count;

		Block() {
			buffer = new byte[SZ];
		}

		Block(int sz) {
			buffer = new byte[sz];
		}

		boolean isFull() {
			return count == buffer.length;
		}
	}

	private class BlockInputStream extends InputStream {
		private byte[] singleByteBuffer;
		private int blockIndex;
		private Block block;
		private int blockPos;

		BlockInputStream() {
			block = blocks.get(blockIndex);
		}

		@Override
		public int read() throws IOException {
			if (singleByteBuffer == null)
				singleByteBuffer = new byte[1];
			int n = read(singleByteBuffer);
			return n == 1 ? singleByteBuffer[0] & 0xff : -1;
		}

		@Override
		public long skip(long cnt) throws IOException {
			long skipped = 0;
			while (0 < cnt) {
				int n = (int) Math.min(block.count - blockPos, cnt);
				if (0 < n) {
					blockPos += n;
					skipped += n;
					cnt -= n;
				} else if (nextBlock())
					continue;
				else
					break;
			}
			return skipped;
		}

		@Override
		public int read(byte[] b, int off, int len) throws IOException {
			if (len == 0)
				return 0;
			int copied = 0;
			while (0 < len) {
				int c = Math.min(block.count - blockPos, len);
				if (0 < c) {
					System.arraycopy(block.buffer, blockPos, b, off, c);
					blockPos += c;
					off += c;
					len -= c;
					copied += c;
				} else if (nextBlock())
					continue;
				else
					break;
			}
			return 0 < copied ? copied : -1;
		}

		private boolean nextBlock() {
			if (++blockIndex < blocks.size()) {
				block = blocks.get(blockIndex);
				blockPos = 0;
				return true;
			}
			return false;
		}
	}
}
