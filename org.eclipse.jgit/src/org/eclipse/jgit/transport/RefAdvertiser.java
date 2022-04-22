/*
 * Copyright (C) 2008, 2020 Google Inc. and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.eclipse.jgit.transport;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.eclipse.jgit.lib.Constants.OBJECT_ID_STRING_LENGTH;
import static org.eclipse.jgit.transport.GitProtocolConstants.OPTION_SYMREF;
import static org.eclipse.jgit.transport.GitProtocolConstants.REF_ATTR_PEELED;
import static org.eclipse.jgit.transport.GitProtocolConstants.REF_ATTR_SYMREF_TARGET;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.RefComparator;
import org.eclipse.jgit.lib.Repository;

/**
 * Support for the start of {@link org.eclipse.jgit.transport.UploadPack} and
 * {@link org.eclipse.jgit.transport.ReceivePack}.
 */
public abstract class RefAdvertiser {
	/** Advertiser which frames lines in a {@link PacketLineOut} format. */
	public static class PacketLineOutRefAdvertiser extends RefAdvertiser {
		private final CharsetEncoder utf8 = UTF_8.newEncoder();
		private final PacketLineOut pckOut;

		private byte[] binArr = new byte[256];
		private ByteBuffer binBuf = ByteBuffer.wrap(binArr);

		private char[] chArr = new char[256];
		private CharBuffer chBuf = CharBuffer.wrap(chArr);

		/**
		 * Create a new advertiser for the supplied stream.
		 *
		 * @param out
		 *            the output stream.
		 */
		public PacketLineOutRefAdvertiser(PacketLineOut out) {
			pckOut = out;
		}

		@Override
		public void advertiseId(AnyObjectId id, String refName)
				throws IOException {
			id.copyTo(binArr, 0);
			binArr[OBJECT_ID_STRING_LENGTH] = ' ';
			binBuf.position(OBJECT_ID_STRING_LENGTH + 1);
			append(refName);
			if (first) {
				first = false;
				if (!capablities.isEmpty()) {
					append('\0');
					for (String cap : capablities) {
						append(' ');
						append(cap);
					}
				}
			}
			append('\n');
			pckOut.writePacket(binArr, 0, binBuf.position());
		}

		private void append(String str) throws CharacterCodingException {
			int n = str.length();
			if (n > chArr.length) {
				chArr = new char[n + 256];
				chBuf = CharBuffer.wrap(chArr);
			}
			str.getChars(0, n, chArr, 0);
			chBuf.position(0).limit(n);
			utf8.reset();
			for (;;) {
				CoderResult cr = utf8.encode(chBuf, binBuf, true);
				if (cr.isOverflow()) {
					grow();
				} else if (cr.isUnderflow()) {
					break;
				} else {
					cr.throwException();
				}
			}
		}

		private void append(int b) {
			if (!binBuf.hasRemaining()) {
				grow();
			}
			binBuf.put((byte) b);
		}

		private void grow() {
			int cnt = binBuf.position();
			byte[] tmp = new byte[binArr.length << 1];
			System.arraycopy(binArr, 0, tmp, 0, cnt);
			binArr = tmp;
			binBuf = ByteBuffer.wrap(binArr);
			binBuf.position(cnt);
		}

		@Override
		protected void writeOne(CharSequence line) throws IOException {
			pckOut.writeString(line.toString());
		}

		@Override
		protected void end() throws IOException {
			pckOut.end();
		}
	}

	private final StringBuilder tmpLine = new StringBuilder(100);

	private final char[] tmpId = new char[Constants.OBJECT_ID_STRING_LENGTH];

	final Set<String> capablities = new LinkedHashSet<>();

	private final Set<ObjectId> sent = new HashSet<>();

	private Repository repository;

	private boolean derefTags;

	boolean first = true;

	private boolean useProtocolV2;

	/* only used in protocol v2 */
	private final Map<String, String> symrefs = new HashMap<>();

	/**
	 * Initialize this advertiser with a repository for peeling tags.
	 *
	 * @param src
	 *            the repository to read from.
	 */
	public void init(Repository src) {
		repository = src;
	}

	/**
	 * @param b
	 *              true if this advertiser should advertise using the protocol
	 *              v2 format, false otherwise
	 * @since 5.0
	 */
	public void setUseProtocolV2(boolean b) {
		useProtocolV2 = b;
	}

	/**
	 * Toggle tag peeling.
	 * <p>
	 * <p>
	 * This method must be invoked prior to any of the following:
	 * <ul>
	 * <li>{@link #send(Map)}</li>
	 * <li>{@link #send(Collection)}</li>
	 * </ul>
	 *
	 * @param deref
	 *            true to show the dereferenced value of a tag as the special
	 *            ref <code>$tag^{}</code> ; false to omit it from the output.
	 */
	public void setDerefTags(boolean deref) {
		derefTags = deref;
	}

	/**
	 * Add one protocol capability to the initial advertisement.
	 * <p>
	 * This method must be invoked prior to any of the following:
	 * <ul>
	 * <li>{@link #send(Map)}</li>
	 * <li>{@link #send(Collection)}</li>
	 * <li>{@link #advertiseHave(AnyObjectId)}</li>
	 * </ul>
	 *
	 * @param name
	 *            the name of a single protocol capability supported by the
	 *            caller. The set of capabilities are sent to the client in the
	 *            advertisement, allowing the client to later selectively enable
	 *            features it recognizes.
	 */
	public void advertiseCapability(String name) {
		capablities.add(name);
	}

	/**
	 * Add one protocol capability with a value ({@code "name=value"}).
	 *
	 * @param name
	 *            name of the capability.
	 * @param value
	 *            value. If null the capability will not be added.
	 * @since 4.0
	 */
	public void advertiseCapability(String name, String value) {
		if (value != null) {
			capablities.add(name + '=' + value);
		}
	}

	/**
	 * Add a symbolic ref to capabilities.
	 * <p>
	 * This method must be invoked prior to any of the following:
	 * <ul>
	 * <li>{@link #send(Map)}</li>
	 * <li>{@link #send(Collection)}</li>
	 * <li>{@link #advertiseHave(AnyObjectId)}</li>
	 * </ul>
	 *
	 * @param from
	 *            The symbolic ref, e.g. "HEAD"
	 * @param to
	 *            The real ref it points to, e.g. "refs/heads/master"
	 * @since 3.6
	 */
	public void addSymref(String from, String to) {
		if (useProtocolV2) {
			symrefs.put(from, to);
		} else {
			advertiseCapability(OPTION_SYMREF, from + ':' + to);
		}
	}

	/**
	 * Format an advertisement for the supplied refs.
	 *
	 * @param refs
	 *            zero or more refs to format for the client. The collection is
	 *            sorted before display if necessary, and therefore may appear
	 *            in any order.
	 * @return set of ObjectIds that were advertised to the client.
	 * @throws java.io.IOException
	 *             the underlying output stream failed to write out an
	 *             advertisement record.
	 * @deprecated use {@link #send(Collection)} instead.
	 */
	@Deprecated
	public Set<ObjectId> send(Map<String, Ref> refs) throws IOException {
		return send(refs.values());
	}

	/**
	 * Format an advertisement for the supplied refs.
	 *
	 * @param refs
	 *            zero or more refs to format for the client. The collection is
	 *            sorted before display if necessary, and therefore may appear
	 *            in any order.
	 * @return set of ObjectIds that were advertised to the client.
	 * @throws java.io.IOException
	 *             the underlying output stream failed to write out an
	 *             advertisement record.
	 * @since 5.0
	 */
	public Set<ObjectId> send(Collection<Ref> refs) throws IOException {
		for (Ref ref : RefComparator.sort(refs)) {
			// TODO(jrn) revive the SortedMap optimization e.g. by introducing
			// SortedList
			ObjectId objectId = ref.getObjectId();
			if (objectId == null) {
				continue;
			}

			if (useProtocolV2) {
				String symrefPart = symrefs.containsKey(ref.getName())
						? (' ' + REF_ATTR_SYMREF_TARGET
								+ symrefs.get(ref.getName()))
						: ""; //$NON-NLS-1$
				String peelPart = ""; //$NON-NLS-1$
				if (derefTags) {
					if (!ref.isPeeled() && repository != null) {
						ref = repository.getRefDatabase().peel(ref);
					}
					ObjectId peeledObjectId = ref.getPeeledObjectId();
					if (peeledObjectId != null) {
						peelPart = ' ' + REF_ATTR_PEELED
								+ peeledObjectId.getName();
					}
				}
				writeOne(objectId.getName() + " " + ref.getName() + symrefPart //$NON-NLS-1$
						+ peelPart + "\n"); //$NON-NLS-1$
				continue;
			}

			advertiseAny(objectId, ref.getName());

			if (!derefTags)
				continue;

			if (!ref.isPeeled()) {
				if (repository == null)
					continue;
				ref = repository.getRefDatabase().peel(ref);
			}

			if (ref.getPeeledObjectId() != null)
				advertiseAny(ref.getPeeledObjectId(), ref.getName() + "^{}"); //$NON-NLS-1$
		}
		return sent;
	}

	/**
	 * Advertise one object is available using the magic {@code .have}.
	 * <p>
	 * The magic {@code .have} advertisement is not available for fetching by a
	 * client, but can be used by a client when considering a delta base
	 * candidate before transferring data in a push. Within the record created
	 * by this method the ref name is simply the invalid string {@code .have}.
	 *
	 * @param id
	 *            identity of the object that is assumed to exist.
	 * @throws java.io.IOException
	 *             the underlying output stream failed to write out an
	 *             advertisement record.
	 */
	public void advertiseHave(AnyObjectId id) throws IOException {
		advertiseAnyOnce(id, ".have"); //$NON-NLS-1$
	}

	/**
	 * Whether no advertisements have been sent yet.
	 *
	 * @return true if no advertisements have been sent yet.
	 */
	public boolean isEmpty() {
		return first;
	}

	private void advertiseAnyOnce(AnyObjectId obj, String refName)
			throws IOException {
		if (!sent.contains(obj))
			advertiseAny(obj, refName);
	}

	private void advertiseAny(AnyObjectId obj, String refName)
			throws IOException {
		sent.add(obj.toObjectId());
		advertiseId(obj, refName);
	}

	/**
	 * Advertise one object under a specific name.
	 * <p>
	 * If the advertised object is a tag, this method does not advertise the
	 * peeled version of it.
	 *
	 * @param id
	 *            the object to advertise.
	 * @param refName
	 *            name of the reference to advertise the object as, can be any
	 *            string not including the NUL byte.
	 * @throws java.io.IOException
	 *             the underlying output stream failed to write out an
	 *             advertisement record.
	 */
	public void advertiseId(AnyObjectId id, String refName)
			throws IOException {
		tmpLine.setLength(0);
		id.copyTo(tmpId, tmpLine);
		tmpLine.append(' ');
		tmpLine.append(refName);
		if (first) {
			first = false;
			if (!capablities.isEmpty()) {
				tmpLine.append('\0');
				for (String capName : capablities) {
					tmpLine.append(' ');
					tmpLine.append(capName);
				}
				tmpLine.append(' ');
			}
		}
		tmpLine.append('\n');
		writeOne(tmpLine);
	}

	/**
	 * Write a single advertisement line.
	 *
	 * @param line
	 *            the advertisement line to be written. The line always ends
	 *            with LF. Never null or the empty string.
	 * @throws java.io.IOException
	 *             the underlying output stream failed to write out an
	 *             advertisement record.
	 */
	protected abstract void writeOne(CharSequence line) throws IOException;

	/**
	 * Mark the end of the advertisements.
	 *
	 * @throws java.io.IOException
	 *             the underlying output stream failed to write out an
	 *             advertisement record.
	 */
	protected abstract void end() throws IOException;
}
