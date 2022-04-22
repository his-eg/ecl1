/*
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org> and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.eclipse.jgit.transport;

import static org.eclipse.jgit.transport.WalkRemoteObjectDatabase.ROOT_DIR;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.eclipse.jgit.errors.TransportException;
import org.eclipse.jgit.internal.JGitText;
import org.eclipse.jgit.internal.storage.file.PackFile;
import org.eclipse.jgit.internal.storage.pack.PackExt;
import org.eclipse.jgit.internal.storage.pack.PackWriter;
import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectIdRef;
import org.eclipse.jgit.lib.ProgressMonitor;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Ref.Storage;
import org.eclipse.jgit.lib.RefWriter;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.RemoteRefUpdate.Status;

/**
 * Generic push support for dumb transport protocols.
 * <p>
 * Since there are no Git-specific smarts on the remote side of the connection
 * the client side must handle everything on its own. The generic push support
 * requires being able to delete, create and overwrite files on the remote side,
 * as well as create any missing directories (if necessary). Typically this can
 * be handled through an FTP style protocol.
 * <p>
 * Objects not on the remote side are uploaded as pack files, using one pack
 * file per invocation. This simplifies the implementation as only two data
 * files need to be written to the remote repository.
 * <p>
 * Push support supplied by this class is not multiuser safe. Concurrent pushes
 * to the same repository may yield an inconsistent reference database which may
 * confuse fetch clients.
 * <p>
 * A single push is concurrently safe with multiple fetch requests, due to the
 * careful order of operations used to update the repository. Clients fetching
 * may receive transient failures due to short reads on certain files if the
 * protocol does not support atomic file replacement.
 *
 * @see WalkRemoteObjectDatabase
 */
class WalkPushConnection extends BaseConnection implements PushConnection {
	/** The repository this transport pushes out of. */
	private final Repository local;

	/** Location of the remote repository we are writing to. */
	private final URIish uri;

	/** Database connection to the remote repository. */
	final WalkRemoteObjectDatabase dest;

	/** The configured transport we were constructed by. */
	private final Transport transport;

	/**
	 * Packs already known to reside in the remote repository.
	 * <p>
	 * This is a LinkedHashMap to maintain the original order.
	 */
	private LinkedHashMap<String, String> packNames;

	/** Complete listing of refs the remote will have after our push. */
	private Map<String, Ref> newRefs;

	/**
	 * Updates which require altering the packed-refs file to complete.
	 * <p>
	 * If this collection is non-empty then any refs listed in {@link #newRefs}
	 * with a storage class of {@link Storage#PACKED} will be written.
	 */
	private Collection<RemoteRefUpdate> packedRefUpdates;

	WalkPushConnection(final WalkTransport walkTransport,
			final WalkRemoteObjectDatabase w) {
		transport = (Transport) walkTransport;
		local = transport.local;
		uri = transport.getURI();
		dest = w;
	}

	/** {@inheritDoc} */
	@Override
	public void push(final ProgressMonitor monitor,
			final Map<String, RemoteRefUpdate> refUpdates)
			throws TransportException {
		push(monitor, refUpdates, null);
	}

	/** {@inheritDoc} */
	@Override
	public void push(final ProgressMonitor monitor,
			final Map<String, RemoteRefUpdate> refUpdates, OutputStream out)
			throws TransportException {
		markStartedOperation();
		packNames = null;
		newRefs = new TreeMap<>(getRefsMap());
		packedRefUpdates = new ArrayList<>(refUpdates.size());

		// Filter the commands and issue all deletes first. This way we
		// can correctly handle a directory being cleared out and a new
		// ref using the directory name being created.
		//
		final List<RemoteRefUpdate> updates = new ArrayList<>();
		for (RemoteRefUpdate u : refUpdates.values()) {
			final String n = u.getRemoteName();
			if (!n.startsWith("refs/") || !Repository.isValidRefName(n)) { //$NON-NLS-1$
				u.setStatus(Status.REJECTED_OTHER_REASON);
				u.setMessage(JGitText.get().funnyRefname);
				continue;
			}

			if (AnyObjectId.isEqual(ObjectId.zeroId(), u.getNewObjectId()))
				deleteCommand(u);
			else
				updates.add(u);
		}

		// If we have any updates we need to upload the objects first, to
		// prevent creating refs pointing at non-existent data. Then we
		// can update the refs, and the info-refs file for dumb transports.
		//
		if (!updates.isEmpty())
			sendpack(updates, monitor);
		for (RemoteRefUpdate u : updates)
			updateCommand(u);

		// Is this a new repository? If so we should create additional
		// metadata files so it is properly initialized during the push.
		//
		if (!updates.isEmpty() && isNewRepository())
			createNewRepository(updates);

		RefWriter refWriter = new RefWriter(newRefs.values()) {
			@Override
			protected void writeFile(String file, byte[] content)
					throws IOException {
				dest.writeFile(ROOT_DIR + file, content);
			}
		};
		if (!packedRefUpdates.isEmpty()) {
			try {
				refWriter.writePackedRefs();
				for (RemoteRefUpdate u : packedRefUpdates)
					u.setStatus(Status.OK);
			} catch (IOException err) {
				for (RemoteRefUpdate u : packedRefUpdates) {
					u.setStatus(Status.REJECTED_OTHER_REASON);
					u.setMessage(err.getMessage());
				}
				throw new TransportException(uri, JGitText.get().failedUpdatingRefs, err);
			}
		}

		try {
			refWriter.writeInfoRefs();
		} catch (IOException err) {
			throw new TransportException(uri, JGitText.get().failedUpdatingRefs, err);
		}
	}

	/** {@inheritDoc} */
	@Override
	public void close() {
		dest.close();
	}

	private void sendpack(final List<RemoteRefUpdate> updates,
			final ProgressMonitor monitor) throws TransportException {
		PackFile pack = null;
		PackFile idx = null;
		try (PackWriter writer = new PackWriter(transport.getPackConfig(),
				local.newObjectReader())) {

			final Set<ObjectId> need = new HashSet<>();
			final Set<ObjectId> have = new HashSet<>();
			for (RemoteRefUpdate r : updates)
				need.add(r.getNewObjectId());
			for (Ref r : getRefs()) {
				have.add(r.getObjectId());
				if (r.getPeeledObjectId() != null)
					have.add(r.getPeeledObjectId());
			}
			writer.preparePack(monitor, need, have);

			// We don't have to continue further if the pack will
			// be an empty pack, as the remote has all objects it
			// needs to complete this change.
			//
			if (writer.getObjectCount() == 0)
				return;

			packNames = new LinkedHashMap<>();
			for (String n : dest.getPackNames())
				packNames.put(n, n);

			File packDir = new File("pack"); //$NON-NLS-1$
			pack = new PackFile(packDir, writer.computeName(),
					PackExt.PACK);
			idx = pack.create(PackExt.INDEX);

			if (packNames.remove(pack.getName()) != null) {
				// The remote already contains this pack. We should
				// remove the index before overwriting to prevent bad
				// offsets from appearing to clients.
				//
				dest.writeInfoPacks(packNames.keySet());
				dest.deleteFile(idx.getPath());
			}

			// Write the pack file, then the index, as readers look the
			// other direction (index, then pack file).
			//
			String wt = "Put " + pack.getName().substring(0, 12); //$NON-NLS-1$
			try (OutputStream os = new BufferedOutputStream(
					dest.writeFile(pack.getPath(), monitor,
							wt + "." + pack.getPackExt().getExtension()))) { //$NON-NLS-1$
				writer.writePack(monitor, monitor, os);
			}

			try (OutputStream os = new BufferedOutputStream(
					dest.writeFile(idx.getPath(), monitor,
							wt + "." + idx.getPackExt().getExtension()))) { //$NON-NLS-1$
				writer.writeIndex(os);
			}

			// Record the pack at the start of the pack info list. This
			// way clients are likely to consult the newest pack first,
			// and discover the most recent objects there.
			//
			final ArrayList<String> infoPacks = new ArrayList<>();
			infoPacks.add(pack.getName());
			infoPacks.addAll(packNames.keySet());
			dest.writeInfoPacks(infoPacks);

		} catch (IOException err) {
			safeDelete(idx);
			safeDelete(pack);

			throw new TransportException(uri, JGitText.get().cannotStoreObjects, err);
		}
	}

	private void safeDelete(File path) {
		if (path != null) {
			try {
				dest.deleteFile(path.getPath());
			} catch (IOException cleanupFailure) {
				// Ignore the deletion failure. We probably are
				// already failing and were just trying to pick
				// up after ourselves.
			}
		}
	}

	private void deleteCommand(RemoteRefUpdate u) {
		final Ref r = newRefs.remove(u.getRemoteName());
		if (r == null) {
			// Already gone.
			//
			u.setStatus(Status.OK);
			return;
		}

		if (r.getStorage().isPacked())
			packedRefUpdates.add(u);

		if (r.getStorage().isLoose()) {
			try {
				dest.deleteRef(u.getRemoteName());
				u.setStatus(Status.OK);
			} catch (IOException e) {
				u.setStatus(Status.REJECTED_OTHER_REASON);
				u.setMessage(e.getMessage());
			}
		}

		try {
			dest.deleteRefLog(u.getRemoteName());
		} catch (IOException e) {
			u.setStatus(Status.REJECTED_OTHER_REASON);
			u.setMessage(e.getMessage());
		}
	}

	private void updateCommand(RemoteRefUpdate u) {
		try {
			dest.writeRef(u.getRemoteName(), u.getNewObjectId());
			newRefs.put(u.getRemoteName(), new ObjectIdRef.Unpeeled(
					Storage.LOOSE, u.getRemoteName(), u.getNewObjectId()));
			u.setStatus(Status.OK);
		} catch (IOException e) {
			u.setStatus(Status.REJECTED_OTHER_REASON);
			u.setMessage(e.getMessage());
		}
	}

	private boolean isNewRepository() {
		return getRefsMap().isEmpty() && packNames != null
				&& packNames.isEmpty();
	}

	private void createNewRepository(List<RemoteRefUpdate> updates)
			throws TransportException {
		try {
			final String ref = "ref: " + pickHEAD(updates) + "\n"; //$NON-NLS-1$ //$NON-NLS-2$
			final byte[] bytes = Constants.encode(ref);
			dest.writeFile(ROOT_DIR + Constants.HEAD, bytes);
		} catch (IOException e) {
			throw new TransportException(uri, JGitText.get().cannotCreateHEAD, e);
		}

		try {
			final String config = "[core]\n" //$NON-NLS-1$
					+ "\trepositoryformatversion = 0\n"; //$NON-NLS-1$
			final byte[] bytes = Constants.encode(config);
			dest.writeFile(ROOT_DIR + Constants.CONFIG, bytes);
		} catch (IOException e) {
			throw new TransportException(uri, JGitText.get().cannotCreateConfig, e);
		}
	}

	private static String pickHEAD(List<RemoteRefUpdate> updates) {
		// Try to use master if the user is pushing that, it is the
		// default branch and is likely what they want to remain as
		// the default on the new remote.
		//
		for (RemoteRefUpdate u : updates) {
			final String n = u.getRemoteName();
			if (n.equals(Constants.R_HEADS + Constants.MASTER))
				return n;
		}

		// Pick any branch, under the assumption the user pushed only
		// one to the remote side.
		//
		for (RemoteRefUpdate u : updates) {
			final String n = u.getRemoteName();
			if (n.startsWith(Constants.R_HEADS))
				return n;
		}
		return updates.get(0).getRemoteName();
	}
}
