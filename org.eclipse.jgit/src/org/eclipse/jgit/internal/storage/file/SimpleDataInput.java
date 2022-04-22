/*
 * Copyright (C) 2012, Google Inc. and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.eclipse.jgit.internal.storage.file;

import java.io.DataInput;
import java.io.IOException;
import java.io.InputStream;

import org.eclipse.jgit.util.IO;
import org.eclipse.jgit.util.NB;

/**
 * An implementation of DataInput that only handles readInt() and readLong()
 * using the Git conversion utilities for network byte order handling. This is
 * needed to read {@link com.googlecode.javaewah.EWAHCompressedBitmap}s.
 */
class SimpleDataInput implements DataInput {
	private final InputStream fd;

	private final byte[] buf = new byte[8];

	SimpleDataInput(InputStream fd) {
		this.fd = fd;
	}

	/** {@inheritDoc} */
	@Override
	public int readInt() throws IOException {
		readFully(buf, 0, 4);
		return NB.decodeInt32(buf, 0);
	}

	/** {@inheritDoc} */
	@Override
	public long readLong() throws IOException {
		readFully(buf, 0, 8);
		return NB.decodeInt64(buf, 0);
	}

	/**
	 * Read unsigned int
	 *
	 * @return a long.
	 * @throws java.io.IOException
	 *             if any.
	 */
	public long readUnsignedInt() throws IOException {
		readFully(buf, 0, 4);
		return NB.decodeUInt32(buf, 0);
	}

	/** {@inheritDoc} */
	@Override
	public void readFully(byte[] b) throws IOException {
		readFully(b, 0, b.length);
	}

	/** {@inheritDoc} */
	@Override
	public void readFully(byte[] b, int off, int len) throws IOException {
		IO.readFully(fd, b, off, len);
	}

	/** {@inheritDoc} */
	@Override
	public int skipBytes(int n) throws IOException {
		throw new UnsupportedOperationException();
	}

	/** {@inheritDoc} */
	@Override
	public boolean readBoolean() throws IOException {
		throw new UnsupportedOperationException();
	}

	/** {@inheritDoc} */
	@Override
	public byte readByte() throws IOException {
		throw new UnsupportedOperationException();
	}

	/** {@inheritDoc} */
	@Override
	public int readUnsignedByte() throws IOException {
		throw new UnsupportedOperationException();
	}

	/** {@inheritDoc} */
	@Override
	public short readShort() throws IOException {
		throw new UnsupportedOperationException();
	}

	/** {@inheritDoc} */
	@Override
	public int readUnsignedShort() throws IOException {
		throw new UnsupportedOperationException();
	}

	/** {@inheritDoc} */
	@Override
	public char readChar() throws IOException {
		throw new UnsupportedOperationException();
	}

	/** {@inheritDoc} */
	@Override
	public float readFloat() throws IOException {
		throw new UnsupportedOperationException();
	}

	/** {@inheritDoc} */
	@Override
	public double readDouble() throws IOException {
		throw new UnsupportedOperationException();
	}

	/** {@inheritDoc} */
	@Override
	public String readLine() throws IOException {
		throw new UnsupportedOperationException();
	}

	/** {@inheritDoc} */
	@Override
	public String readUTF() throws IOException {
		throw new UnsupportedOperationException();
	}
}
