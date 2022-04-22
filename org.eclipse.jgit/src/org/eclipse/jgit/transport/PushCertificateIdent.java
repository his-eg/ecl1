/*
 * Copyright (C) 2015, Google Inc. and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.eclipse.jgit.transport;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.eclipse.jgit.util.RawParseUtils.lastIndexOfTrim;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.util.MutableInteger;
import org.eclipse.jgit.util.RawParseUtils;

/**
 * Identity in a push certificate.
 * <p>
 * This is similar to a {@link org.eclipse.jgit.lib.PersonIdent} in that it
 * contains a name, timestamp, and timezone offset, but differs in the following
 * ways:
 * <ul>
 * <li>It is always parsed from a UTF-8 string, rather than a raw commit
 * buffer.</li>
 * <li>It is not guaranteed to contain a name and email portion, since any UTF-8
 * string is a valid OpenPGP User ID (RFC4880 5.1.1). The raw User ID is always
 * available as {@link #getUserId()}, but {@link #getEmailAddress()} may return
 * null.</li>
 * <li>The raw text from which the identity was parsed is available with
 * {@link #getRaw()}. This is necessary for losslessly reconstructing the signed
 * push certificate payload.</li>
 * <li>
 * </ul>
 *
 * @since 4.1
 */
public class PushCertificateIdent {
	/**
	 * Parse an identity from a string.
	 * <p>
	 * Spaces are trimmed when parsing the timestamp and timezone offset, with
	 * one exception. The timestamp must be preceded by a single space, and the
	 * rest of the string prior to that space (including any additional
	 * whitespace) is treated as the OpenPGP User ID.
	 * <p>
	 * If either the timestamp or timezone offsets are missing, mimics
	 * {@link RawParseUtils#parsePersonIdent(String)} behavior and sets them
	 * both to zero.
	 *
	 * @param str
	 *            string to parse.
	 * @return a {@link org.eclipse.jgit.transport.PushCertificateIdent} object.
	 */
	public static PushCertificateIdent parse(String str) {
		MutableInteger p = new MutableInteger();
		byte[] raw = str.getBytes(UTF_8);
		int tzBegin = raw.length - 1;
		tzBegin = lastIndexOfTrim(raw, ' ', tzBegin);
		if (tzBegin < 0 || raw[tzBegin] != ' ') {
			return new PushCertificateIdent(str, str, 0, 0);
		}
		int whenBegin = tzBegin++;
		int tz = RawParseUtils.parseTimeZoneOffset(raw, tzBegin, p);
		boolean hasTz = p.value != tzBegin;

		whenBegin = lastIndexOfTrim(raw, ' ', whenBegin);
		if (whenBegin < 0 || raw[whenBegin] != ' ') {
			return new PushCertificateIdent(str, str, 0, 0);
		}
		int idEnd = whenBegin++;
		long when = RawParseUtils.parseLongBase10(raw, whenBegin, p);
		boolean hasWhen = p.value != whenBegin;

		if (hasTz && hasWhen) {
			idEnd = whenBegin - 1;
		} else {
			// If either tz or when are non-numeric, mimic parsePersonIdent behavior and
			// set them both to zero.
			tz = 0;
			when = 0;
			if (hasTz && !hasWhen) {
				// Only one trailing numeric field; assume User ID ends before this
				// field, but discard its value.
				idEnd = tzBegin - 1;
			} else {
				// No trailing numeric fields; User ID is whole raw value.
				idEnd = raw.length;
			}
		}
		String id = new String(raw, 0, idEnd, UTF_8);

		return new PushCertificateIdent(str, id, when * 1000L, tz);
	}

	private final String raw;
	private final String userId;
	private final long when;
	private final int tzOffset;

	/**
	 * Construct a new identity from an OpenPGP User ID.
	 *
	 * @param userId
	 *            OpenPGP User ID; any UTF-8 string.
	 * @param when
	 *            local time.
	 * @param tzOffset
	 *            timezone offset; see {@link #getTimeZoneOffset()}.
	 */
	public PushCertificateIdent(String userId, long when, int tzOffset) {
		this.userId = userId;
		this.when = when;
		this.tzOffset = tzOffset;
		StringBuilder sb = new StringBuilder(userId).append(' ').append(when / 1000)
				.append(' ');
		PersonIdent.appendTimezone(sb, tzOffset);
		raw = sb.toString();
	}

	private PushCertificateIdent(String raw, String userId, long when,
			int tzOffset) {
		this.raw = raw;
		this.userId = userId;
		this.when = when;
		this.tzOffset = tzOffset;
	}

	/**
	 * Get the raw string from which this identity was parsed.
	 * <p>
	 * If the string was constructed manually, a suitable canonical string is
	 * returned.
	 * <p>
	 * For the purposes of bytewise comparisons with other OpenPGP IDs, the string
	 * must be encoded as UTF-8.
	 *
	 * @return the raw string.
	 */
	public String getRaw() {
		return raw;
	}

	/**
	 * Get the OpenPGP User ID, which may be any string.
	 *
	 * @return the OpenPGP User ID, which may be any string.
	 */
	public String getUserId() {
		return userId;
	}

	/**
	 * Get the name portion of the User ID.
	 *
	 * @return the name portion of the User ID. If no email address would be
	 *         parsed by {@link #getEmailAddress()}, returns the full User ID
	 *         with spaces trimmed.
	 */
	public String getName() {
		int nameEnd = userId.indexOf('<');
		if (nameEnd < 0 || userId.indexOf('>', nameEnd) < 0) {
			nameEnd = userId.length();
		}
		nameEnd--;
		while (nameEnd >= 0 && userId.charAt(nameEnd) == ' ') {
			nameEnd--;
		}
		int nameBegin = 0;
		while (nameBegin < nameEnd && userId.charAt(nameBegin) == ' ') {
			nameBegin++;
		}
		return userId.substring(nameBegin, nameEnd + 1);
	}

	/**
	 * Get the email portion of the User ID
	 *
	 * @return the email portion of the User ID, if one was successfully parsed
	 *         from {@link #getUserId()}, or null.
	 */
	public String getEmailAddress() {
		int emailBegin = userId.indexOf('<');
		if (emailBegin < 0) {
			return null;
		}
		int emailEnd = userId.indexOf('>', emailBegin);
		if (emailEnd < 0) {
			return null;
		}
		return userId.substring(emailBegin + 1, emailEnd);
	}

	/**
	 * Get the timestamp of the identity.
	 *
	 * @return the timestamp of the identity.
	 */
	public Date getWhen() {
		return new Date(when);
	}

	/**
	 * Get this person's declared time zone
	 *
	 * @return this person's declared time zone; null if the timezone is
	 *         unknown.
	 */
	public TimeZone getTimeZone() {
		return PersonIdent.getTimeZone(tzOffset);
	}

	/**
	 * Get this person's declared time zone as minutes east of UTC.
	 *
	 * @return this person's declared time zone as minutes east of UTC. If the
	 *         timezone is to the west of UTC it is negative.
	 */
	public int getTimeZoneOffset() {
		return tzOffset;
	}

	/** {@inheritDoc} */
	@Override
	public boolean equals(Object o) {
		return (o instanceof PushCertificateIdent)
			&& raw.equals(((PushCertificateIdent) o).raw);
	}

	/** {@inheritDoc} */
	@Override
	public int hashCode() {
		return raw.hashCode();
	}

	/** {@inheritDoc} */
	@SuppressWarnings("nls")
	@Override
	public String toString() {
		SimpleDateFormat fmt;
		fmt = new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy Z", Locale.US);
		fmt.setTimeZone(getTimeZone());
		return getClass().getSimpleName()
			+ "[raw=\"" + raw + "\","
			+ " userId=\"" + userId + "\","
			+ " " + fmt.format(Long.valueOf(when)) + "]";
	}
}
