/*
 * Copyright (C) 2008, 2019 Google Inc. and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.eclipse.jgit.util;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.util.Arrays;

import org.eclipse.jgit.lib.Constants;

/**
 * Utility functions related to quoted string handling.
 */
public abstract class QuotedString {
	/** Quoting style that obeys the rules Git applies to file names */
	public static final GitPathStyle GIT_PATH = new GitPathStyle(true);

	/**
	 * Quoting style that obeys the rules Git applies to file names when
	 * {@code core.quotePath = false}.
	 *
	 * @since 5.6
	 */
	public static final QuotedString GIT_PATH_MINIMAL = new GitPathStyle(false);

	/**
	 * Quoting style used by the Bourne shell.
	 * <p>
	 * Quotes are unconditionally inserted during {@link #quote(String)}. This
	 * protects shell meta-characters like <code>$</code> or <code>~</code> from
	 * being recognized as special.
	 */
	public static final BourneStyle BOURNE = new BourneStyle();

	/** Bourne style, but permits <code>~user</code> at the start of the string. */
	public static final BourneUserPathStyle BOURNE_USER_PATH = new BourneUserPathStyle();

	/**
	 * Quote an input string by the quoting rules.
	 * <p>
	 * If the input string does not require any quoting, the same String
	 * reference is returned to the caller.
	 * <p>
	 * Otherwise a quoted string is returned, including the opening and closing
	 * quotation marks at the start and end of the string. If the style does not
	 * permit raw Unicode characters then the string will first be encoded in
	 * UTF-8, with unprintable sequences possibly escaped by the rules.
	 *
	 * @param in
	 *            any non-null Unicode string.
	 * @return a quoted string. See above for details.
	 */
	public abstract String quote(String in);

	/**
	 * Clean a previously quoted input, decoding the result via UTF-8.
	 * <p>
	 * This method must match quote such that:
	 *
	 * <pre>
	 * a.equals(dequote(quote(a)));
	 * </pre>
	 *
	 * is true for any <code>a</code>.
	 *
	 * @param in
	 *            a Unicode string to remove quoting from.
	 * @return the cleaned string.
	 * @see #dequote(byte[], int, int)
	 */
	public String dequote(String in) {
		final byte[] b = Constants.encode(in);
		return dequote(b, 0, b.length);
	}

	/**
	 * Decode a previously quoted input, scanning a UTF-8 encoded buffer.
	 * <p>
	 * This method must match quote such that:
	 *
	 * <pre>
	 * a.equals(dequote(Constants.encode(quote(a))));
	 * </pre>
	 *
	 * is true for any <code>a</code>.
	 * <p>
	 * This method removes any opening/closing quotation marks added by
	 * {@link #quote(String)}.
	 *
	 * @param in
	 *            the input buffer to parse.
	 * @param offset
	 *            first position within <code>in</code> to scan.
	 * @param end
	 *            one position past in <code>in</code> to scan.
	 * @return the cleaned string.
	 */
	public abstract String dequote(byte[] in, int offset, int end);

	/**
	 * Quoting style used by the Bourne shell.
	 * <p>
	 * Quotes are unconditionally inserted during {@link #quote(String)}. This
	 * protects shell meta-characters like <code>$</code> or <code>~</code> from
	 * being recognized as special.
	 */
	public static class BourneStyle extends QuotedString {
		@Override
		public String quote(String in) {
			final StringBuilder r = new StringBuilder();
			r.append('\'');
			int start = 0, i = 0;
			for (; i < in.length(); i++) {
				switch (in.charAt(i)) {
				case '\'':
				case '!':
					r.append(in, start, i);
					r.append('\'');
					r.append('\\');
					r.append(in.charAt(i));
					r.append('\'');
					start = i + 1;
					break;
				}
			}
			r.append(in, start, i);
			r.append('\'');
			return r.toString();
		}

		@Override
		public String dequote(byte[] in, int ip, int ie) {
			boolean inquote = false;
			final byte[] r = new byte[ie - ip];
			int rPtr = 0;
			while (ip < ie) {
				final byte b = in[ip++];
				switch (b) {
				case '\'':
					inquote = !inquote;
					continue;
				case '\\':
					if (inquote || ip == ie)
						r[rPtr++] = b; // literal within a quote
					else
						r[rPtr++] = in[ip++];
					continue;
				default:
					r[rPtr++] = b;
					continue;
				}
			}
			return RawParseUtils.decode(UTF_8, r, 0, rPtr);
		}
	}

	/** Bourne style, but permits <code>~user</code> at the start of the string. */
	public static class BourneUserPathStyle extends BourneStyle {
		@Override
		public String quote(String in) {
			if (in.matches("^~[A-Za-z0-9_-]+$")) { //$NON-NLS-1$
				// If the string is just "~user" we can assume they
				// mean "~user/".
				//
				return in + "/"; //$NON-NLS-1$
			}

			if (in.matches("^~[A-Za-z0-9_-]*/.*$")) { //$NON-NLS-1$
				// If the string is of "~/path" or "~user/path"
				// we must not escape ~/ or ~user/ from the shell.
				//
				final int i = in.indexOf('/') + 1;
				if (i == in.length())
					return in;
				return in.substring(0, i) + super.quote(in.substring(i));
			}

			return super.quote(in);
		}
	}

	/** Quoting style that obeys the rules Git applies to file names */
	public static final class GitPathStyle extends QuotedString {
		private static final byte[] quote;
		static {
			quote = new byte[128];
			Arrays.fill(quote, (byte) -1);

			for (int i = '0'; i <= '9'; i++)
				quote[i] = 0;
			for (int i = 'a'; i <= 'z'; i++)
				quote[i] = 0;
			for (int i = 'A'; i <= 'Z'; i++)
				quote[i] = 0;
			quote[' '] = 0;
			quote['$'] = 0;
			quote['%'] = 0;
			quote['&'] = 0;
			quote['*'] = 0;
			quote['+'] = 0;
			quote[','] = 0;
			quote['-'] = 0;
			quote['.'] = 0;
			quote['/'] = 0;
			quote[':'] = 0;
			quote[';'] = 0;
			quote['='] = 0;
			quote['?'] = 0;
			quote['@'] = 0;
			quote['_'] = 0;
			quote['^'] = 0;
			quote['|'] = 0;
			quote['~'] = 0;

			quote['\u0007'] = 'a';
			quote['\b'] = 'b';
			quote['\f'] = 'f';
			quote['\n'] = 'n';
			quote['\r'] = 'r';
			quote['\t'] = 't';
			quote['\u000B'] = 'v';
			quote['\\'] = '\\';
			quote['"'] = '"';
		}

		private final boolean quoteHigh;

		@Override
		public String quote(String instr) {
			if (instr.isEmpty()) {
				return "\"\""; //$NON-NLS-1$
			}
			boolean reuse = true;
			final byte[] in = Constants.encode(instr);
			final byte[] out = new byte[4 * in.length + 2];
			int o = 0;
			out[o++] = '"';
			for (byte element : in) {
				final int c = element & 0xff;
				if (c < quote.length) {
					final byte style = quote[c];
					if (style == 0) {
						out[o++] = (byte) c;
						continue;
					}
					if (style > 0) {
						reuse = false;
						out[o++] = '\\';
						out[o++] = style;
						continue;
					}
				} else if (!quoteHigh) {
					out[o++] = (byte) c;
					continue;
				}

				reuse = false;
				out[o++] = '\\';
				out[o++] = (byte) (((c >> 6) & 03) + '0');
				out[o++] = (byte) (((c >> 3) & 07) + '0');
				out[o++] = (byte) (((c >> 0) & 07) + '0');
			}
			if (reuse) {
				return instr;
			}
			out[o++] = '"';
			return new String(out, 0, o, UTF_8);
		}

		@Override
		public String dequote(byte[] in, int inPtr, int inEnd) {
			if (2 <= inEnd - inPtr && in[inPtr] == '"' && in[inEnd - 1] == '"')
				return dq(in, inPtr + 1, inEnd - 1);
			return RawParseUtils.decode(UTF_8, in, inPtr, inEnd);
		}

		private static String dq(byte[] in, int inPtr, int inEnd) {
			final byte[] r = new byte[inEnd - inPtr];
			int rPtr = 0;
			while (inPtr < inEnd) {
				final byte b = in[inPtr++];
				if (b != '\\') {
					r[rPtr++] = b;
					continue;
				}

				if (inPtr == inEnd) {
					// Lone trailing backslash. Treat it as a literal.
					//
					r[rPtr++] = '\\';
					break;
				}

				switch (in[inPtr++]) {
				case 'a':
					r[rPtr++] = 0x07 /* \a = BEL */;
					continue;
				case 'b':
					r[rPtr++] = '\b';
					continue;
				case 'f':
					r[rPtr++] = '\f';
					continue;
				case 'n':
					r[rPtr++] = '\n';
					continue;
				case 'r':
					r[rPtr++] = '\r';
					continue;
				case 't':
					r[rPtr++] = '\t';
					continue;
				case 'v':
					r[rPtr++] = 0x0B/* \v = VT */;
					continue;

				case '\\':
				case '"':
					r[rPtr++] = in[inPtr - 1];
					continue;

				case '0':
				case '1':
				case '2':
				case '3': {
					int cp = in[inPtr - 1] - '0';
					for (int n = 1; n < 3 && inPtr < inEnd; n++) {
						final byte c = in[inPtr];
						if ('0' <= c && c <= '7') {
							cp <<= 3;
							cp |= c - '0';
							inPtr++;
						} else {
							break;
						}
					}
					r[rPtr++] = (byte) cp;
					continue;
				}

				default:
					// Any other code is taken literally.
					//
					r[rPtr++] = '\\';
					r[rPtr++] = in[inPtr - 1];
					continue;
				}
			}

			return RawParseUtils.decode(UTF_8, r, 0, rPtr);
		}

		private GitPathStyle(boolean doQuote) {
			quoteHigh = doQuote;
		}
	}
}
