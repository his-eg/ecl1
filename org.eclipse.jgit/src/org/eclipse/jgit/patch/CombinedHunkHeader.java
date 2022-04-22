/*
 * Copyright (C) 2008, Google Inc. and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.eclipse.jgit.patch;

import static org.eclipse.jgit.util.RawParseUtils.nextLF;
import static org.eclipse.jgit.util.RawParseUtils.parseBase10;

import java.io.IOException;
import java.io.OutputStream;
import java.text.MessageFormat;

import org.eclipse.jgit.internal.JGitText;
import org.eclipse.jgit.lib.AbbreviatedObjectId;
import org.eclipse.jgit.util.MutableInteger;

/**
 * Hunk header for a hunk appearing in a "diff --cc" style patch.
 */
public class CombinedHunkHeader extends HunkHeader {
	private abstract static class CombinedOldImage extends OldImage {
		int nContext;
	}

	private CombinedOldImage[] old;

	CombinedHunkHeader(CombinedFileHeader fh, int offset) {
		super(fh, offset, null);
		old = new CombinedOldImage[fh.getParentCount()];
		for (int i = 0; i < old.length; i++) {
			final int imagePos = i;
			old[i] = new CombinedOldImage() {
				@Override
				public AbbreviatedObjectId getId() {
					return fh.getOldId(imagePos);
				}
			};
		}
	}

	/** {@inheritDoc} */
	@Override
	public CombinedFileHeader getFileHeader() {
		return (CombinedFileHeader) super.getFileHeader();
	}

	/** {@inheritDoc} */
	@Override
	public OldImage getOldImage() {
		return getOldImage(0);
	}

	/**
	 * Get the OldImage data related to the nth ancestor
	 *
	 * @param nthParent
	 *            the ancestor to get the old image data of
	 * @return image data of the requested ancestor.
	 */
	public OldImage getOldImage(int nthParent) {
		return old[nthParent];
	}

	@Override
	void parseHeader() {
		// Parse "@@@ -55,12 -163,13 +163,15 @@@ protected boolean"
		//
		final byte[] buf = file.buf;
		final MutableInteger ptr = new MutableInteger();
		ptr.value = nextLF(buf, startOffset, ' ');

		for (CombinedOldImage o : old) {
			o.startLine = -parseBase10(buf, ptr.value, ptr);
			if (buf[ptr.value] == ',') {
				o.lineCount = parseBase10(buf, ptr.value + 1, ptr);
			} else {
				o.lineCount = 1;
			}
		}

		newStartLine = parseBase10(buf, ptr.value + 1, ptr);
		if (buf[ptr.value] == ',')
			newLineCount = parseBase10(buf, ptr.value + 1, ptr);
		else
			newLineCount = 1;
	}

	@Override
	int parseBody(Patch script, int end) {
		final byte[] buf = file.buf;
		int c = nextLF(buf, startOffset);

		for (CombinedOldImage o : old) {
			o.nDeleted = 0;
			o.nAdded = 0;
			o.nContext = 0;
		}
		nContext = 0;
		int nAdded = 0;

		SCAN: for (int eol; c < end; c = eol) {
			eol = nextLF(buf, c);

			if (eol - c < old.length + 1) {
				// Line isn't long enough to mention the state of each
				// ancestor. It must be the end of the hunk.
				break SCAN;
			}

			switch (buf[c]) {
			case ' ':
			case '-':
			case '+':
				break;

			default:
				// Line can't possibly be part of this hunk; the first
				// ancestor information isn't recognizable.
				//
				break SCAN;
			}

			int localcontext = 0;
			for (int ancestor = 0; ancestor < old.length; ancestor++) {
				switch (buf[c + ancestor]) {
				case ' ':
					localcontext++;
					old[ancestor].nContext++;
					continue;

				case '-':
					old[ancestor].nDeleted++;
					continue;

				case '+':
					old[ancestor].nAdded++;
					nAdded++;
					continue;

				default:
					break SCAN;
				}
			}
			if (localcontext == old.length)
				nContext++;
		}

		for (int ancestor = 0; ancestor < old.length; ancestor++) {
			final CombinedOldImage o = old[ancestor];
			final int cmp = o.nContext + o.nDeleted;
			if (cmp < o.lineCount) {
				final int missingCnt = o.lineCount - cmp;
				script.error(buf, startOffset, MessageFormat.format(
						JGitText.get().truncatedHunkLinesMissingForAncestor,
						Integer.valueOf(missingCnt),
						Integer.valueOf(ancestor + 1)));
			}
		}

		if (nContext + nAdded < newLineCount) {
			final int missingCount = newLineCount - (nContext + nAdded);
			script.error(buf, startOffset, MessageFormat.format(
					JGitText.get().truncatedHunkNewLinesMissing,
					Integer.valueOf(missingCount)));
		}

		return c;
	}

	@Override
	void extractFileLines(OutputStream[] out) throws IOException {
		final byte[] buf = file.buf;
		int ptr = startOffset;
		int eol = nextLF(buf, ptr);
		if (endOffset <= eol)
			return;

		// Treat the hunk header as though it were from the ancestor,
		// as it may have a function header appearing after it which
		// was copied out of the ancestor file.
		//
		out[0].write(buf, ptr, eol - ptr);

		SCAN: for (ptr = eol; ptr < endOffset; ptr = eol) {
			eol = nextLF(buf, ptr);

			if (eol - ptr < old.length + 1) {
				// Line isn't long enough to mention the state of each
				// ancestor. It must be the end of the hunk.
				break SCAN;
			}

			switch (buf[ptr]) {
			case ' ':
			case '-':
			case '+':
				break;

			default:
				// Line can't possibly be part of this hunk; the first
				// ancestor information isn't recognizable.
				//
				break SCAN;
			}

			int delcnt = 0;
			for (int ancestor = 0; ancestor < old.length; ancestor++) {
				switch (buf[ptr + ancestor]) {
				case '-':
					delcnt++;
					out[ancestor].write(buf, ptr, eol - ptr);
					continue;

				case ' ':
					out[ancestor].write(buf, ptr, eol - ptr);
					continue;

				case '+':
					continue;

				default:
					break SCAN;
				}
			}
			if (delcnt < old.length) {
				// This line appears in the new file if it wasn't deleted
				// relative to all ancestors.
				//
				out[old.length].write(buf, ptr, eol - ptr);
			}
		}
	}

	@Override
	void extractFileLines(final StringBuilder sb, final String[] text,
			final int[] offsets) {
		final byte[] buf = file.buf;
		int ptr = startOffset;
		int eol = nextLF(buf, ptr);
		if (endOffset <= eol)
			return;
		copyLine(sb, text, offsets, 0);
		SCAN: for (ptr = eol; ptr < endOffset; ptr = eol) {
			eol = nextLF(buf, ptr);

			if (eol - ptr < old.length + 1) {
				// Line isn't long enough to mention the state of each
				// ancestor. It must be the end of the hunk.
				break SCAN;
			}

			switch (buf[ptr]) {
			case ' ':
			case '-':
			case '+':
				break;

			default:
				// Line can't possibly be part of this hunk; the first
				// ancestor information isn't recognizable.
				//
				break SCAN;
			}

			boolean copied = false;
			for (int ancestor = 0; ancestor < old.length; ancestor++) {
				switch (buf[ptr + ancestor]) {
				case ' ':
				case '-':
					if (copied)
						skipLine(text, offsets, ancestor);
					else {
						copyLine(sb, text, offsets, ancestor);
						copied = true;
					}
					continue;

				case '+':
					continue;

				default:
					break SCAN;
				}
			}
			if (!copied) {
				// If none of the ancestors caused the copy then this line
				// must be new across the board, so it only appears in the
				// text of the new file.
				//
				copyLine(sb, text, offsets, old.length);
			}
		}
	}
}
