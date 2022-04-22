/*
 * Copyright (C) 2009, Constantine Plotnikov <constantine.plotnikov@gmail.com>
 * Copyright (C) 2008-2010, Google Inc.
 * Copyright (C) 2008-2009, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2009, Sasa Zivkov <sasa.zivkov@sap.com>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org> and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.eclipse.jgit.transport;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jgit.errors.MissingBundlePrerequisiteException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.errors.PackProtocolException;
import org.eclipse.jgit.errors.TransportException;
import org.eclipse.jgit.internal.JGitText;
import org.eclipse.jgit.lib.NullProgressMonitor;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectIdRef;
import org.eclipse.jgit.lib.ObjectInserter;
import org.eclipse.jgit.lib.ProgressMonitor;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevFlag;
import org.eclipse.jgit.revwalk.RevObject;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.util.IO;
import org.eclipse.jgit.util.RawParseUtils;

/**
 * Fetch connection for bundle based classes. It used by
 * instances of {@link TransportBundle}
 */
class BundleFetchConnection extends BaseFetchConnection {

	private final Transport transport;

	InputStream bin;

	final Map<ObjectId, String> prereqs = new HashMap<>();

	private String lockMessage;

	private PackLock packLock;

	BundleFetchConnection(Transport transportBundle, InputStream src) throws TransportException {
		transport = transportBundle;
		bin = new BufferedInputStream(src);
		try {
			switch (readSignature()) {
			case 2:
				readBundleV2();
				break;
			default:
				throw new TransportException(transport.uri, JGitText.get().notABundle);
			}
		} catch (TransportException err) {
			close();
			throw err;
		} catch (IOException | RuntimeException err) {
			close();
			throw new TransportException(transport.uri, err.getMessage(), err);
		}
	}

	private int readSignature() throws IOException {
		final String rev = readLine(new byte[1024]);
		if (TransportBundle.V2_BUNDLE_SIGNATURE.equals(rev))
			return 2;
		throw new TransportException(transport.uri, JGitText.get().notABundle);
	}

	private void readBundleV2() throws IOException {
		final byte[] hdrbuf = new byte[1024];
		final LinkedHashMap<String, Ref> avail = new LinkedHashMap<>();
		for (;;) {
			String line = readLine(hdrbuf);
			if (line.length() == 0)
				break;

			if (line.charAt(0) == '-') {
				ObjectId id = ObjectId.fromString(line.substring(1, 41));
				String shortDesc = null;
				if (line.length() > 42)
					shortDesc = line.substring(42);
				prereqs.put(id, shortDesc);
				continue;
			}

			final String name = line.substring(41, line.length());
			final ObjectId id = ObjectId.fromString(line.substring(0, 40));
			final Ref prior = avail.put(name, new ObjectIdRef.Unpeeled(
					Ref.Storage.NETWORK, name, id));
			if (prior != null)
				throw duplicateAdvertisement(name);
		}
		available(avail);
	}

	private PackProtocolException duplicateAdvertisement(String name) {
		return new PackProtocolException(transport.uri,
				MessageFormat.format(JGitText.get().duplicateAdvertisementsOf, name));
	}

	private String readLine(byte[] hdrbuf) throws IOException {
		StringBuilder line = new StringBuilder();
		boolean done = false;
		while (!done) {
			bin.mark(hdrbuf.length);
			final int cnt = bin.read(hdrbuf);
			if (cnt < 0) {
				throw new EOFException(JGitText.get().shortReadOfBlock);
			}
			int lf = 0;
			while (lf < cnt && hdrbuf[lf] != '\n') {
				lf++;
			}
			bin.reset();
			IO.skipFully(bin, lf);
			if (lf < cnt && hdrbuf[lf] == '\n') {
				IO.skipFully(bin, 1);
				done = true;
			}
			line.append(RawParseUtils.decode(UTF_8, hdrbuf, 0, lf));
		}
		return line.toString();
	}

	/** {@inheritDoc} */
	@Override
	public boolean didFetchTestConnectivity() {
		return false;
	}

	/** {@inheritDoc} */
	@Override
	protected void doFetch(final ProgressMonitor monitor,
			final Collection<Ref> want, final Set<ObjectId> have)
			throws TransportException {
		verifyPrerequisites();
		try {
			try (ObjectInserter ins = transport.local.newObjectInserter()) {
				PackParser parser = ins.newPackParser(bin);
				parser.setAllowThin(true);
				parser.setObjectChecker(transport.getObjectChecker());
				parser.setLockMessage(lockMessage);
				packLock = parser.parse(NullProgressMonitor.INSTANCE);
				ins.flush();
			}
		} catch (IOException | RuntimeException err) {
			close();
			throw new TransportException(transport.uri, err.getMessage(), err);
		}
	}

	/** {@inheritDoc} */
	@Override
	public void setPackLockMessage(String message) {
		lockMessage = message;
	}

	/** {@inheritDoc} */
	@Override
	public Collection<PackLock> getPackLocks() {
		if (packLock != null)
			return Collections.singleton(packLock);
		return Collections.<PackLock> emptyList();
	}

	private void verifyPrerequisites() throws TransportException {
		if (prereqs.isEmpty())
			return;

		try (RevWalk rw = new RevWalk(transport.local)) {
			final RevFlag PREREQ = rw.newFlag("PREREQ"); //$NON-NLS-1$
			final RevFlag SEEN = rw.newFlag("SEEN"); //$NON-NLS-1$

			final Map<ObjectId, String> missing = new HashMap<>();
			final List<RevObject> commits = new ArrayList<>();
			for (Map.Entry<ObjectId, String> e : prereqs.entrySet()) {
				ObjectId p = e.getKey();
				try {
					final RevCommit c = rw.parseCommit(p);
					if (!c.has(PREREQ)) {
						c.add(PREREQ);
						commits.add(c);
					}
				} catch (MissingObjectException notFound) {
					missing.put(p, e.getValue());
				} catch (IOException err) {
					throw new TransportException(transport.uri, MessageFormat
							.format(JGitText.get().cannotReadCommit, p.name()),
							err);
				}
			}
			if (!missing.isEmpty())
				throw new MissingBundlePrerequisiteException(transport.uri,
						missing);

			List<Ref> localRefs;
			try {
				localRefs = transport.local.getRefDatabase().getRefs();
			} catch (IOException e) {
				throw new TransportException(transport.uri, e.getMessage(), e);
			}
			for (Ref r : localRefs) {
				try {
					rw.markStart(rw.parseCommit(r.getObjectId()));
				} catch (IOException readError) {
					// If we cannot read the value of the ref skip it.
				}
			}

			int remaining = commits.size();
			try {
				RevCommit c;
				while ((c = rw.next()) != null) {
					if (c.has(PREREQ)) {
						c.add(SEEN);
						if (--remaining == 0)
							break;
					}
				}
			} catch (IOException err) {
				throw new TransportException(transport.uri,
						JGitText.get().cannotReadObject, err);
			}

			if (remaining > 0) {
				for (RevObject o : commits) {
					if (!o.has(SEEN))
						missing.put(o, prereqs.get(o));
				}
				throw new MissingBundlePrerequisiteException(transport.uri,
						missing);
			}
		}
	}

	/** {@inheritDoc} */
	@Override
	public void close() {
		if (bin != null) {
			try {
				bin.close();
			} catch (IOException ie) {
				// Ignore close failures.
			} finally {
				bin = null;
			}
		}
	}
}
