/*
 * Copyright (C) 2010, Google Inc. and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.eclipse.jgit.lib;

import static org.eclipse.jgit.lib.Constants.OBJECT_ID_LENGTH;
import static org.eclipse.jgit.lib.Constants.OBJ_TREE;
import static org.eclipse.jgit.lib.Constants.encode;
import static org.eclipse.jgit.lib.FileMode.GITLINK;
import static org.eclipse.jgit.lib.FileMode.REGULAR_FILE;
import static org.eclipse.jgit.lib.FileMode.TREE;

import java.io.IOException;

import org.eclipse.jgit.errors.CorruptObjectException;
import org.eclipse.jgit.internal.JGitText;
import org.eclipse.jgit.revwalk.RevBlob;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.util.TemporaryBuffer;

/**
 * Mutable formatter to construct a single tree object.
 *
 * This formatter does not process subtrees. Callers must handle creating each
 * subtree on their own.
 *
 * To maintain good performance for bulk operations, this formatter does not
 * validate its input. Callers are responsible for ensuring the resulting tree
 * object is correctly well formed by writing entries in the correct order.
 */
public class TreeFormatter {
	/**
	 * Compute the size of a tree entry record.
	 *
	 * This method can be used to estimate the correct size of a tree prior to
	 * allocating a formatter. Getting the size correct at allocation time
	 * ensures the internal buffer is sized correctly, reducing copying.
	 *
	 * @param mode
	 *            the mode the entry will have.
	 * @param nameLen
	 *            the length of the name, in bytes.
	 * @return the length of the record.
	 */
	public static int entrySize(FileMode mode, int nameLen) {
		return mode.copyToLength() + nameLen + OBJECT_ID_LENGTH + 2;
	}

	private byte[] buf;

	private int ptr;

	private TemporaryBuffer.Heap overflowBuffer;

	/**
	 * Create an empty formatter with a default buffer size.
	 */
	public TreeFormatter() {
		this(8192);
	}

	/**
	 * Create an empty formatter with the specified buffer size.
	 *
	 * @param size
	 *            estimated size of the tree, in bytes. Callers can use
	 *            {@link #entrySize(FileMode, int)} to estimate the size of each
	 *            entry in advance of allocating the formatter.
	 */
	public TreeFormatter(int size) {
		buf = new byte[size];
	}

	/**
	 * Add a link to a submodule commit, mode is {@link org.eclipse.jgit.lib.FileMode#GITLINK}.
	 *
	 * @param name
	 *            name of the entry.
	 * @param commit
	 *            the ObjectId to store in this entry.
	 */
	public void append(String name, RevCommit commit) {
		append(name, GITLINK, commit);
	}

	/**
	 * Add a subtree, mode is {@link org.eclipse.jgit.lib.FileMode#TREE}.
	 *
	 * @param name
	 *            name of the entry.
	 * @param tree
	 *            the ObjectId to store in this entry.
	 */
	public void append(String name, RevTree tree) {
		append(name, TREE, tree);
	}

	/**
	 * Add a regular file, mode is {@link org.eclipse.jgit.lib.FileMode#REGULAR_FILE}.
	 *
	 * @param name
	 *            name of the entry.
	 * @param blob
	 *            the ObjectId to store in this entry.
	 */
	public void append(String name, RevBlob blob) {
		append(name, REGULAR_FILE, blob);
	}

	/**
	 * Append any entry to the tree.
	 *
	 * @param name
	 *            name of the entry.
	 * @param mode
	 *            mode describing the treatment of {@code id}.
	 * @param id
	 *            the ObjectId to store in this entry.
	 */
	public void append(String name, FileMode mode, AnyObjectId id) {
		append(encode(name), mode, id);
	}

	/**
	 * Append any entry to the tree.
	 *
	 * @param name
	 *            name of the entry. The name should be UTF-8 encoded, but file
	 *            name encoding is not a well defined concept in Git.
	 * @param mode
	 *            mode describing the treatment of {@code id}.
	 * @param id
	 *            the ObjectId to store in this entry.
	 */
	public void append(byte[] name, FileMode mode, AnyObjectId id) {
		append(name, 0, name.length, mode, id);
	}

	/**
	 * Append any entry to the tree.
	 *
	 * @param nameBuf
	 *            buffer holding the name of the entry. The name should be UTF-8
	 *            encoded, but file name encoding is not a well defined concept
	 *            in Git.
	 * @param namePos
	 *            first position within {@code nameBuf} of the name data.
	 * @param nameLen
	 *            number of bytes from {@code nameBuf} to use as the name.
	 * @param mode
	 *            mode describing the treatment of {@code id}.
	 * @param id
	 *            the ObjectId to store in this entry.
	 */
	public void append(byte[] nameBuf, int namePos, int nameLen, FileMode mode,
			AnyObjectId id) {
		append(nameBuf, namePos, nameLen, mode, id, false);
	}

	/**
	 * Append any entry to the tree.
	 *
	 * @param nameBuf
	 *            buffer holding the name of the entry. The name should be UTF-8
	 *            encoded, but file name encoding is not a well defined concept
	 *            in Git.
	 * @param namePos
	 *            first position within {@code nameBuf} of the name data.
	 * @param nameLen
	 *            number of bytes from {@code nameBuf} to use as the name.
	 * @param mode
	 *            mode describing the treatment of {@code id}.
	 * @param id
	 *            the ObjectId to store in this entry.
	 * @param allowEmptyName
	 *            allow an empty filename (creating a corrupt tree)
	 * @since 4.6
	 */
	public void append(byte[] nameBuf, int namePos, int nameLen, FileMode mode,
			AnyObjectId id, boolean allowEmptyName) {
		if (nameLen == 0 && !allowEmptyName) {
			throw new IllegalArgumentException(
					JGitText.get().invalidTreeZeroLengthName);
		}
		if (fmtBuf(nameBuf, namePos, nameLen, mode)) {
			id.copyRawTo(buf, ptr);
			ptr += OBJECT_ID_LENGTH;

		} else {
			try {
				fmtOverflowBuffer(nameBuf, namePos, nameLen, mode);
				id.copyRawTo(overflowBuffer);
			} catch (IOException badBuffer) {
				// This should never occur.
				throw new RuntimeException(badBuffer);
			}
		}
	}

	/**
	 * Append any entry to the tree.
	 *
	 * @param nameBuf
	 *            buffer holding the name of the entry. The name should be UTF-8
	 *            encoded, but file name encoding is not a well defined concept
	 *            in Git.
	 * @param namePos
	 *            first position within {@code nameBuf} of the name data.
	 * @param nameLen
	 *            number of bytes from {@code nameBuf} to use as the name.
	 * @param mode
	 *            mode describing the treatment of {@code id}.
	 * @param idBuf
	 *            buffer holding the raw ObjectId of the entry.
	 * @param idPos
	 *            first position within {@code idBuf} to copy the id from.
	 */
	public void append(byte[] nameBuf, int namePos, int nameLen, FileMode mode,
			byte[] idBuf, int idPos) {
		if (fmtBuf(nameBuf, namePos, nameLen, mode)) {
			System.arraycopy(idBuf, idPos, buf, ptr, OBJECT_ID_LENGTH);
			ptr += OBJECT_ID_LENGTH;

		} else {
			try {
				fmtOverflowBuffer(nameBuf, namePos, nameLen, mode);
				overflowBuffer.write(idBuf, idPos, OBJECT_ID_LENGTH);
			} catch (IOException badBuffer) {
				// This should never occur.
				throw new RuntimeException(badBuffer);
			}
		}
	}

	private boolean fmtBuf(byte[] nameBuf, int namePos, int nameLen,
			FileMode mode) {
		if (buf == null || buf.length < ptr + entrySize(mode, nameLen))
			return false;

		mode.copyTo(buf, ptr);
		ptr += mode.copyToLength();
		buf[ptr++] = ' ';

		System.arraycopy(nameBuf, namePos, buf, ptr, nameLen);
		ptr += nameLen;
		buf[ptr++] = 0;
		return true;
	}

	private void fmtOverflowBuffer(byte[] nameBuf, int namePos, int nameLen,
			FileMode mode) throws IOException {
		if (buf != null) {
			overflowBuffer = new TemporaryBuffer.Heap(Integer.MAX_VALUE);
			overflowBuffer.write(buf, 0, ptr);
			buf = null;
		}

		mode.copyTo(overflowBuffer);
		overflowBuffer.write((byte) ' ');
		overflowBuffer.write(nameBuf, namePos, nameLen);
		overflowBuffer.write((byte) 0);
	}

	/**
	 * Insert this tree and obtain its ObjectId.
	 *
	 * @param ins
	 *            the inserter to store the tree.
	 * @return computed ObjectId of the tree
	 * @throws java.io.IOException
	 *             the tree could not be stored.
	 */
	public ObjectId insertTo(ObjectInserter ins) throws IOException {
		if (buf != null)
			return ins.insert(OBJ_TREE, buf, 0, ptr);

		final long len = overflowBuffer.length();
		return ins.insert(OBJ_TREE, len, overflowBuffer.openInputStream());
	}

	/**
	 * Compute the ObjectId for this tree
	 *
	 * @param ins a {@link org.eclipse.jgit.lib.ObjectInserter} object.
	 * @return ObjectId for this tree
	 */
	public ObjectId computeId(ObjectInserter ins) {
		if (buf != null)
			return ins.idFor(OBJ_TREE, buf, 0, ptr);

		final long len = overflowBuffer.length();
		try {
			return ins.idFor(OBJ_TREE, len, overflowBuffer.openInputStream());
		} catch (IOException e) {
			// this should never happen
			throw new RuntimeException(e);
		}
	}

	/**
	 * Copy this formatter's buffer into a byte array.
	 *
	 * This method is not efficient, as it needs to create a copy of the
	 * internal buffer in order to supply an array of the correct size to the
	 * caller. If the buffer is just to pass to an ObjectInserter, consider
	 * using {@link org.eclipse.jgit.lib.ObjectInserter#insert(TreeFormatter)}
	 * instead.
	 *
	 * @return a copy of this formatter's buffer.
	 */
	public byte[] toByteArray() {
		if (buf != null) {
			byte[] r = new byte[ptr];
			System.arraycopy(buf, 0, r, 0, ptr);
			return r;
		}

		try {
			return overflowBuffer.toByteArray();
		} catch (IOException err) {
			// This should never happen, its read failure on a byte array.
			throw new RuntimeException(err);
		}
	}

	/** {@inheritDoc} */
	@SuppressWarnings("nls")
	@Override
	public String toString() {
		byte[] raw = toByteArray();

		CanonicalTreeParser p = new CanonicalTreeParser();
		p.reset(raw);

		StringBuilder r = new StringBuilder();
		r.append("Tree={");
		if (!p.eof()) {
			r.append('\n');
			try {
				new ObjectChecker().checkTree(raw);
			} catch (CorruptObjectException error) {
				r.append("*** ERROR: ").append(error.getMessage()).append("\n");
				r.append('\n');
			}
		}
		while (!p.eof()) {
			final FileMode mode = p.getEntryFileMode();
			r.append(mode);
			r.append(' ');
			r.append(Constants.typeString(mode.getObjectType()));
			r.append(' ');
			r.append(p.getEntryObjectId().name());
			r.append(' ');
			r.append(p.getEntryPathString());
			r.append('\n');
			p.next();
		}
		r.append("}");
		return r.toString();
	}
}
