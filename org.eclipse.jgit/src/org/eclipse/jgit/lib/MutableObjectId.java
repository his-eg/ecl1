/*
 * Copyright (C) 2008-2009, Google Inc.
 * Copyright (C) 2008, Marek Zawirski <marek.zawirski@gmail.com>
 * Copyright (C) 2007-2009, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2006-2008, Shawn O. Pearce <spearce@spearce.org> and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.eclipse.jgit.lib;

import java.text.MessageFormat;

import org.eclipse.jgit.errors.InvalidObjectIdException;
import org.eclipse.jgit.internal.JGitText;
import org.eclipse.jgit.util.NB;
import org.eclipse.jgit.util.RawParseUtils;

/**
 * A mutable SHA-1 abstraction.
 */
public class MutableObjectId extends AnyObjectId {
	/**
	 * Empty constructor. Initialize object with default (zeros) value.
	 */
	public MutableObjectId() {
		super();
	}

	/**
	 * Copying constructor.
	 *
	 * @param src
	 *            original entry, to copy id from
	 */
	MutableObjectId(MutableObjectId src) {
		fromObjectId(src);
	}

	/**
	 * Set any byte in the id.
	 *
	 * @param index
	 *            index of the byte to set in the raw form of the ObjectId. Must
	 *            be in range [0,
	 *            {@link org.eclipse.jgit.lib.Constants#OBJECT_ID_LENGTH}).
	 * @param value
	 *            the value of the specified byte at {@code index}. Values are
	 *            unsigned and thus are in the range [0,255] rather than the
	 *            signed byte range of [-128, 127].
	 * @throws java.lang.ArrayIndexOutOfBoundsException
	 *             {@code index} is less than 0, equal to
	 *             {@link org.eclipse.jgit.lib.Constants#OBJECT_ID_LENGTH}, or
	 *             greater than
	 *             {@link org.eclipse.jgit.lib.Constants#OBJECT_ID_LENGTH}.
	 */
	public void setByte(int index, int value) {
		switch (index >> 2) {
		case 0:
			w1 = set(w1, index & 3, value);
			break;
		case 1:
			w2 = set(w2, index & 3, value);
			break;
		case 2:
			w3 = set(w3, index & 3, value);
			break;
		case 3:
			w4 = set(w4, index & 3, value);
			break;
		case 4:
			w5 = set(w5, index & 3, value);
			break;
		default:
			throw new ArrayIndexOutOfBoundsException(index);
		}
	}

	private static int set(int w, int index, int value) {
		value &= 0xff;

		switch (index) {
		case 0:
			return (w & 0x00ffffff) | (value << 24);
		case 1:
			return (w & 0xff00ffff) | (value << 16);
		case 2:
			return (w & 0xffff00ff) | (value << 8);
		case 3:
			return (w & 0xffffff00) | value;
		default:
			throw new ArrayIndexOutOfBoundsException();
		}
	}

	/**
	 * Make this id match {@link org.eclipse.jgit.lib.ObjectId#zeroId()}.
	 */
	public void clear() {
		w1 = 0;
		w2 = 0;
		w3 = 0;
		w4 = 0;
		w5 = 0;
	}

	/**
	 * Copy an ObjectId into this mutable buffer.
	 *
	 * @param src
	 *            the source id to copy from.
	 */
	public void fromObjectId(AnyObjectId src) {
		this.w1 = src.w1;
		this.w2 = src.w2;
		this.w3 = src.w3;
		this.w4 = src.w4;
		this.w5 = src.w5;
	}

	/**
	 * Convert an ObjectId from raw binary representation.
	 *
	 * @param bs
	 *            the raw byte buffer to read from. At least 20 bytes must be
	 *            available within this byte array.
	 */
	public void fromRaw(byte[] bs) {
		fromRaw(bs, 0);
	}

	/**
	 * Convert an ObjectId from raw binary representation.
	 *
	 * @param bs
	 *            the raw byte buffer to read from. At least 20 bytes after p
	 *            must be available within this byte array.
	 * @param p
	 *            position to read the first byte of data from.
	 */
	public void fromRaw(byte[] bs, int p) {
		w1 = NB.decodeInt32(bs, p);
		w2 = NB.decodeInt32(bs, p + 4);
		w3 = NB.decodeInt32(bs, p + 8);
		w4 = NB.decodeInt32(bs, p + 12);
		w5 = NB.decodeInt32(bs, p + 16);
	}

	/**
	 * Convert an ObjectId from binary representation expressed in integers.
	 *
	 * @param ints
	 *            the raw int buffer to read from. At least 5 integers must be
	 *            available within this integers array.
	 */
	public void fromRaw(int[] ints) {
		fromRaw(ints, 0);
	}

	/**
	 * Convert an ObjectId from binary representation expressed in integers.
	 *
	 * @param ints
	 *            the raw int buffer to read from. At least 5 integers after p
	 *            must be available within this integers array.
	 * @param p
	 *            position to read the first integer of data from.
	 */
	public void fromRaw(int[] ints, int p) {
		w1 = ints[p];
		w2 = ints[p + 1];
		w3 = ints[p + 2];
		w4 = ints[p + 3];
		w5 = ints[p + 4];
	}

	/**
	 * Convert an ObjectId from binary representation expressed in integers.
	 *
	 * @param a
	 *            an int.
	 * @param b
	 *            an int.
	 * @param c
	 *            an int.
	 * @param d
	 *            an int.
	 * @param e
	 *            an int.
	 * @since 4.7
	 */
	public void set(int a, int b, int c, int d, int e) {
		w1 = a;
		w2 = b;
		w3 = c;
		w4 = d;
		w5 = e;
	}

	/**
	 * Convert an ObjectId from hex characters (US-ASCII).
	 *
	 * @param buf
	 *            the US-ASCII buffer to read from. At least 40 bytes after
	 *            offset must be available within this byte array.
	 * @param offset
	 *            position to read the first character from.
	 */
	public void fromString(byte[] buf, int offset) {
		fromHexString(buf, offset);
	}

	/**
	 * Convert an ObjectId from hex characters.
	 *
	 * @param str
	 *            the string to read from. Must be 40 characters long.
	 */
	public void fromString(String str) {
		if (str.length() != Constants.OBJECT_ID_STRING_LENGTH)
			throw new IllegalArgumentException(MessageFormat.format(
					JGitText.get().invalidId, str));
		fromHexString(Constants.encodeASCII(str), 0);
	}

	private void fromHexString(byte[] bs, int p) {
		try {
			w1 = RawParseUtils.parseHexInt32(bs, p);
			w2 = RawParseUtils.parseHexInt32(bs, p + 8);
			w3 = RawParseUtils.parseHexInt32(bs, p + 16);
			w4 = RawParseUtils.parseHexInt32(bs, p + 24);
			w5 = RawParseUtils.parseHexInt32(bs, p + 32);
		} catch (ArrayIndexOutOfBoundsException e) {
			InvalidObjectIdException e1 = new InvalidObjectIdException(bs, p,
					Constants.OBJECT_ID_STRING_LENGTH);
			e1.initCause(e);
			throw e1;
		}
	}

	/** {@inheritDoc} */
	@Override
	public ObjectId toObjectId() {
		return new ObjectId(this);
	}
}
