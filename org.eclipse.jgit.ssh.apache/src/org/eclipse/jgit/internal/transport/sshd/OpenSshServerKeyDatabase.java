/*
 * Copyright (C) 2018, 2021 Thomas Wolf <thomas.wolf@paranor.ch> and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.eclipse.jgit.internal.transport.sshd;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.text.MessageFormat.format;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import org.apache.sshd.client.config.hosts.HostPatternsHolder;
import org.apache.sshd.client.config.hosts.KnownHostDigest;
import org.apache.sshd.client.config.hosts.KnownHostEntry;
import org.apache.sshd.client.config.hosts.KnownHostHashValue;
import org.apache.sshd.client.keyverifier.KnownHostsServerKeyVerifier.HostEntryPair;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.NamedFactory;
import org.apache.sshd.common.config.keys.AuthorizedKeyEntry;
import org.apache.sshd.common.config.keys.KeyUtils;
import org.apache.sshd.common.config.keys.PublicKeyEntry;
import org.apache.sshd.common.config.keys.PublicKeyEntryResolver;
import org.apache.sshd.common.digest.BuiltinDigests;
import org.apache.sshd.common.mac.Mac;
import org.apache.sshd.common.util.io.ModifiableFileWatcher;
import org.apache.sshd.common.util.net.SshdSocketAddress;
import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.internal.storage.file.LockFile;
import org.eclipse.jgit.transport.CredentialItem;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.sshd.ServerKeyDatabase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A sever host key verifier that honors the {@code StrictHostKeyChecking} and
 * {@code UserKnownHostsFile} values from the ssh configuration.
 * <p>
 * The verifier can be given default known_hosts files in the constructor, which
 * will be used if the ssh config does not specify a {@code UserKnownHostsFile}.
 * If the ssh config <em>does</em> set {@code UserKnownHostsFile}, the verifier
 * uses the given files in the order given. Non-existing or unreadable files are
 * ignored.
 * <p>
 * {@code StrictHostKeyChecking} accepts the following values:
 * </p>
 * <dl>
 * <dt>ask</dt>
 * <dd>Ask the user whether new or changed keys shall be accepted and be added
 * to the known_hosts file.</dd>
 * <dt>yes/true</dt>
 * <dd>Accept only keys listed in the known_hosts file.</dd>
 * <dt>no/false</dt>
 * <dd>Silently accept all new or changed keys, add new keys to the known_hosts
 * file.</dd>
 * <dt>accept-new</dt>
 * <dd>Silently accept keys for new hosts and add them to the known_hosts
 * file.</dd>
 * </dl>
 * <p>
 * If {@code StrictHostKeyChecking} is not set, or set to any other value, the
 * default value <b>ask</b> is active.
 * </p>
 * <p>
 * This implementation relies on the {@link ClientSession} being a
 * {@link JGitClientSession}. By default Apache MINA sshd does not forward the
 * config file host entry to the session, so it would be unknown here which
 * entry it was and what setting of {@code StrictHostKeyChecking} should be
 * used. If used with some other session type, the implementation assumes
 * "<b>ask</b>".
 * <p>
 * <p>
 * Asking the user is done via a {@link CredentialsProvider} obtained from the
 * session. If none is set, the implementation falls back to strict host key
 * checking ("<b>yes</b>").
 * </p>
 * <p>
 * Note that adding a key to the known hosts file may create the file. You can
 * specify in the constructor whether the user shall be asked about that, too.
 * If the user declines updating the file, but the key was otherwise
 * accepted (user confirmed for "<b>ask</b>", or "no" or "accept-new" are
 * active), the key is accepted for this session only.
 * </p>
 * <p>
 * If several known hosts files are specified, a new key is always added to the
 * first file (even if it doesn't exist yet; see the note about file creation
 * above).
 * </p>
 *
 * @see <a href="http://man.openbsd.org/OpenBSD-current/man5/ssh_config.5">man
 *      ssh-config</a>
 */
public class OpenSshServerKeyDatabase
		implements ServerKeyDatabase {

	// TODO: GlobalKnownHostsFile? May need some kind of LRU caching; these
	// files may be large!

	private static final Logger LOG = LoggerFactory
			.getLogger(OpenSshServerKeyDatabase.class);

	/** Can be used to mark revoked known host lines. */
	private static final String MARKER_REVOKED = "revoked"; //$NON-NLS-1$

	private final boolean askAboutNewFile;

	private final Map<Path, HostKeyFile> knownHostsFiles = new ConcurrentHashMap<>();

	private final List<HostKeyFile> defaultFiles = new ArrayList<>();

	private Random prng;

	/**
	 * Creates a new {@link OpenSshServerKeyDatabase}.
	 *
	 * @param askAboutNewFile
	 *            whether to ask the user, if possible, about creating a new
	 *            non-existing known_hosts file
	 * @param defaultFiles
	 *            typically ~/.ssh/known_hosts and ~/.ssh/known_hosts2. May be
	 *            empty or {@code null}, in which case no default files are
	 *            installed. The files need not exist.
	 */
	public OpenSshServerKeyDatabase(boolean askAboutNewFile,
			List<Path> defaultFiles) {
		if (defaultFiles != null) {
			for (Path file : defaultFiles) {
				HostKeyFile newFile = new HostKeyFile(file);
				knownHostsFiles.put(file, newFile);
				this.defaultFiles.add(newFile);
			}
		}
		this.askAboutNewFile = askAboutNewFile;
	}

	private List<HostKeyFile> getFilesToUse(@NonNull Configuration config) {
		List<HostKeyFile> filesToUse = defaultFiles;
		List<HostKeyFile> userFiles = addUserHostKeyFiles(
				config.getUserKnownHostsFiles());
		if (!userFiles.isEmpty()) {
			filesToUse = userFiles;
		}
		return filesToUse;
	}

	@Override
	public List<PublicKey> lookup(@NonNull String connectAddress,
			@NonNull InetSocketAddress remoteAddress,
			@NonNull Configuration config) {
		List<HostKeyFile> filesToUse = getFilesToUse(config);
		List<PublicKey> result = new ArrayList<>();
		Collection<SshdSocketAddress> candidates = getCandidates(
				connectAddress, remoteAddress);
		for (HostKeyFile file : filesToUse) {
			for (HostEntryPair current : file.get()) {
				KnownHostEntry entry = current.getHostEntry();
				if (!isRevoked(entry)) {
					for (SshdSocketAddress host : candidates) {
						if (entry.isHostMatch(host.getHostName(),
								host.getPort())) {
							result.add(current.getServerKey());
							break;
						}
					}
				}
			}
		}
		return result;
	}

	@Override
	public boolean accept(@NonNull String connectAddress,
			@NonNull InetSocketAddress remoteAddress,
			@NonNull PublicKey serverKey,
			@NonNull Configuration config, CredentialsProvider provider) {
		List<HostKeyFile> filesToUse = getFilesToUse(config);
		AskUser ask = new AskUser(config, provider);
		HostEntryPair[] modified = { null };
		Path path = null;
		Collection<SshdSocketAddress> candidates = getCandidates(connectAddress,
				remoteAddress);
		for (HostKeyFile file : filesToUse) {
			try {
				if (find(candidates, serverKey, file.get(), modified)) {
					return true;
				}
			} catch (RevokedKeyException e) {
				ask.revokedKey(remoteAddress, serverKey, file.getPath());
				return false;
			}
			if (path == null && modified[0] != null) {
				// Remember the file in which we might need to update the
				// entry
				path = file.getPath();
			}
		}
		if (modified[0] != null) {
			// We found an entry, but with a different key
			AskUser.ModifiedKeyHandling toDo = ask.acceptModifiedServerKey(
					remoteAddress, modified[0].getServerKey(),
					serverKey, path);
			if (toDo == AskUser.ModifiedKeyHandling.ALLOW_AND_STORE) {
				try {
					updateModifiedServerKey(serverKey, modified[0], path);
					knownHostsFiles.get(path).resetReloadAttributes();
				} catch (IOException e) {
					LOG.warn(format(SshdText.get().knownHostsCouldNotUpdate,
							path));
				}
			}
			if (toDo == AskUser.ModifiedKeyHandling.DENY) {
				return false;
			}
			// TODO: OpenSsh disables password and keyboard-interactive
			// authentication in this case. Also agent and local port forwarding
			// are switched off. (Plus a few other things such as X11 forwarding
			// that are of no interest to a git client.)
			return true;
		} else if (ask.acceptUnknownKey(remoteAddress, serverKey)) {
			if (!filesToUse.isEmpty()) {
				HostKeyFile toUpdate = filesToUse.get(0);
				path = toUpdate.getPath();
				try {
					if (Files.exists(path) || !askAboutNewFile
							|| ask.createNewFile(path)) {
						updateKnownHostsFile(candidates, serverKey, path,
								config);
						toUpdate.resetReloadAttributes();
					}
				} catch (Exception e) {
					LOG.warn(format(SshdText.get().knownHostsCouldNotUpdate,
							path), e);
				}
			}
			return true;
		}
		return false;
	}

	private static class RevokedKeyException extends Exception {
		private static final long serialVersionUID = 1L;
	}

	private boolean isRevoked(KnownHostEntry entry) {
		return MARKER_REVOKED.equals(entry.getMarker());
	}

	private boolean find(Collection<SshdSocketAddress> candidates,
			PublicKey serverKey, List<HostEntryPair> entries,
			HostEntryPair[] modified) throws RevokedKeyException {
		for (HostEntryPair current : entries) {
			KnownHostEntry entry = current.getHostEntry();
			for (SshdSocketAddress host : candidates) {
				if (entry.isHostMatch(host.getHostName(), host.getPort())) {
					boolean revoked = isRevoked(entry);
					if (KeyUtils.compareKeys(serverKey,
							current.getServerKey())) {
						// Exact match
						if (revoked) {
							throw new RevokedKeyException();
						}
						modified[0] = null;
						return true;
					} else if (!revoked) {
						// Server sent a different key
						modified[0] = current;
						// Keep going -- maybe there's another entry for this
						// host
					}
					break;
				}
			}
		}
		return false;
	}

	private List<HostKeyFile> addUserHostKeyFiles(List<String> fileNames) {
		if (fileNames == null || fileNames.isEmpty()) {
			return Collections.emptyList();
		}
		List<HostKeyFile> userFiles = new ArrayList<>();
		for (String name : fileNames) {
			try {
				Path path = Paths.get(name);
				HostKeyFile file = knownHostsFiles.computeIfAbsent(path,
						p -> new HostKeyFile(path));
				userFiles.add(file);
			} catch (InvalidPathException e) {
				LOG.warn(format(SshdText.get().knownHostsInvalidPath,
						name));
			}
		}
		return userFiles;
	}

	private void updateKnownHostsFile(Collection<SshdSocketAddress> candidates,
			PublicKey serverKey, Path path, Configuration config)
			throws Exception {
		String newEntry = createHostKeyLine(candidates, serverKey, config);
		if (newEntry == null) {
			return;
		}
		LockFile lock = new LockFile(path.toFile());
		if (lock.lockForAppend()) {
			try {
				try (BufferedWriter writer = new BufferedWriter(
						new OutputStreamWriter(lock.getOutputStream(),
								UTF_8))) {
					writer.newLine();
					writer.write(newEntry);
					writer.newLine();
				}
				lock.commit();
			} catch (IOException e) {
				lock.unlock();
				throw e;
			}
		} else {
			LOG.warn(format(SshdText.get().knownHostsFileLockedUpdate,
					path));
		}
	}

	private void updateModifiedServerKey(PublicKey serverKey,
			HostEntryPair entry, Path path)
			throws IOException {
		KnownHostEntry hostEntry = entry.getHostEntry();
		String oldLine = hostEntry.getConfigLine();
		if (oldLine == null) {
			return;
		}
		String newLine = updateHostKeyLine(oldLine, serverKey);
		if (newLine == null || newLine.isEmpty()) {
			return;
		}
		if (oldLine.isEmpty() || newLine.equals(oldLine)) {
			// Shouldn't happen.
			return;
		}
		LockFile lock = new LockFile(path.toFile());
		if (lock.lock()) {
			try {
				try (BufferedWriter writer = new BufferedWriter(
						new OutputStreamWriter(lock.getOutputStream(), UTF_8));
						BufferedReader reader = Files.newBufferedReader(path,
								UTF_8)) {
					boolean done = false;
					String line;
					while ((line = reader.readLine()) != null) {
						String toWrite = line;
						if (!done) {
							int pos = line.indexOf('#');
							String toTest = pos < 0 ? line
									: line.substring(0, pos);
							if (toTest.trim().equals(oldLine)) {
								toWrite = newLine;
								done = true;
							}
						}
						writer.write(toWrite);
						writer.newLine();
					}
				}
				lock.commit();
			} catch (IOException e) {
				lock.unlock();
				throw e;
			}
		} else {
			LOG.warn(format(SshdText.get().knownHostsFileLockedUpdate,
					path));
		}
	}

	private static class AskUser {

		public enum ModifiedKeyHandling {
			DENY, ALLOW, ALLOW_AND_STORE
		}

		private enum Check {
			ASK, DENY, ALLOW;
		}

		private final @NonNull Configuration config;

		private final CredentialsProvider provider;

		public AskUser(@NonNull Configuration config,
				CredentialsProvider provider) {
			this.config = config;
			this.provider = provider;
		}

		private static boolean askUser(CredentialsProvider provider, URIish uri,
				String prompt, String... messages) {
			List<CredentialItem> items = new ArrayList<>(messages.length + 1);
			for (String message : messages) {
				items.add(new CredentialItem.InformationalMessage(message));
			}
			if (prompt != null) {
				CredentialItem.YesNoType answer = new CredentialItem.YesNoType(
						prompt);
				items.add(answer);
				return provider.get(uri, items) && answer.getValue();
			}
			return provider.get(uri, items);
		}

		private Check checkMode(SocketAddress remoteAddress, boolean changed) {
			if (!(remoteAddress instanceof InetSocketAddress)) {
				return Check.DENY;
			}
			switch (config.getStrictHostKeyChecking()) {
			case REQUIRE_MATCH:
				return Check.DENY;
			case ACCEPT_ANY:
				return Check.ALLOW;
			case ACCEPT_NEW:
				return changed ? Check.DENY : Check.ALLOW;
			default:
				return provider == null ? Check.DENY : Check.ASK;
			}
		}

		public void revokedKey(SocketAddress remoteAddress, PublicKey serverKey,
				Path path) {
			if (provider == null) {
				return;
			}
			InetSocketAddress remote = (InetSocketAddress) remoteAddress;
			URIish uri = JGitUserInteraction.toURI(config.getUsername(),
					remote);
			String sha256 = KeyUtils.getFingerPrint(BuiltinDigests.sha256,
					serverKey);
			String md5 = KeyUtils.getFingerPrint(BuiltinDigests.md5, serverKey);
			String keyAlgorithm = serverKey.getAlgorithm();
			askUser(provider, uri, null, //
					format(SshdText.get().knownHostsRevokedKeyMsg,
							remote.getHostString(), path),
					format(SshdText.get().knownHostsKeyFingerprints,
							keyAlgorithm),
					md5, sha256);
		}

		public boolean acceptUnknownKey(SocketAddress remoteAddress,
				PublicKey serverKey) {
			Check check = checkMode(remoteAddress, false);
			if (check != Check.ASK) {
				return check == Check.ALLOW;
			}
			InetSocketAddress remote = (InetSocketAddress) remoteAddress;
			// Ask the user
			String sha256 = KeyUtils.getFingerPrint(BuiltinDigests.sha256,
					serverKey);
			String md5 = KeyUtils.getFingerPrint(BuiltinDigests.md5, serverKey);
			String keyAlgorithm = serverKey.getAlgorithm();
			String remoteHost = remote.getHostString();
			URIish uri = JGitUserInteraction.toURI(config.getUsername(),
					remote);
			String prompt = SshdText.get().knownHostsUnknownKeyPrompt;
			return askUser(provider, uri, prompt, //
					format(SshdText.get().knownHostsUnknownKeyMsg,
							remoteHost),
					format(SshdText.get().knownHostsKeyFingerprints,
							keyAlgorithm),
					md5, sha256);
		}

		public ModifiedKeyHandling acceptModifiedServerKey(
				InetSocketAddress remoteAddress, PublicKey expected,
				PublicKey actual, Path path) {
			Check check = checkMode(remoteAddress, true);
			if (check == Check.ALLOW) {
				// Never auto-store on CHECK.ALLOW
				return ModifiedKeyHandling.ALLOW;
			}
			String keyAlgorithm = actual.getAlgorithm();
			String remoteHost = remoteAddress.getHostString();
			URIish uri = JGitUserInteraction.toURI(config.getUsername(),
					remoteAddress);
			List<String> messages = new ArrayList<>();
			String warning = format(
					SshdText.get().knownHostsModifiedKeyWarning,
					keyAlgorithm, expected.getAlgorithm(), remoteHost,
					KeyUtils.getFingerPrint(BuiltinDigests.md5, expected),
					KeyUtils.getFingerPrint(BuiltinDigests.sha256, expected),
					KeyUtils.getFingerPrint(BuiltinDigests.md5, actual),
					KeyUtils.getFingerPrint(BuiltinDigests.sha256, actual));
			messages.addAll(Arrays.asList(warning.split("\n"))); //$NON-NLS-1$

			if (check == Check.DENY) {
				if (provider != null) {
					messages.add(format(
							SshdText.get().knownHostsModifiedKeyDenyMsg, path));
					askUser(provider, uri, null,
							messages.toArray(new String[0]));
				}
				return ModifiedKeyHandling.DENY;
			}
			// ASK -- two questions: procceed? and store?
			List<CredentialItem> items = new ArrayList<>(messages.size() + 2);
			for (String message : messages) {
				items.add(new CredentialItem.InformationalMessage(message));
			}
			CredentialItem.YesNoType proceed = new CredentialItem.YesNoType(
					SshdText.get().knownHostsModifiedKeyAcceptPrompt);
			CredentialItem.YesNoType store = new CredentialItem.YesNoType(
					SshdText.get().knownHostsModifiedKeyStorePrompt);
			items.add(proceed);
			items.add(store);
			if (provider.get(uri, items) && proceed.getValue()) {
				return store.getValue() ? ModifiedKeyHandling.ALLOW_AND_STORE
						: ModifiedKeyHandling.ALLOW;
			}
			return ModifiedKeyHandling.DENY;
		}

		public boolean createNewFile(Path path) {
			if (provider == null) {
				// We can't ask, so don't create the file
				return false;
			}
			URIish uri = new URIish().setPath(path.toString());
			return askUser(provider, uri, //
					format(SshdText.get().knownHostsUserAskCreationPrompt,
							path), //
					format(SshdText.get().knownHostsUserAskCreationMsg, path));
		}
	}

	private static class HostKeyFile extends ModifiableFileWatcher
			implements Supplier<List<HostEntryPair>> {

		private List<HostEntryPair> entries = Collections.emptyList();

		public HostKeyFile(Path path) {
			super(path);
		}

		@Override
		public List<HostEntryPair> get() {
			Path path = getPath();
			synchronized (this) {
				try {
					if (checkReloadRequired()) {
						entries = reload(getPath());
					}
				} catch (IOException e) {
					LOG.warn(format(SshdText.get().knownHostsFileReadFailed,
							path));
				}
				return Collections.unmodifiableList(entries);
			}
		}

		private List<HostEntryPair> reload(Path path) throws IOException {
			try {
				List<KnownHostEntry> rawEntries = KnownHostEntryReader
						.readFromFile(path);
				updateReloadAttributes();
				if (rawEntries == null || rawEntries.isEmpty()) {
					return Collections.emptyList();
				}
				List<HostEntryPair> newEntries = new LinkedList<>();
				for (KnownHostEntry entry : rawEntries) {
					AuthorizedKeyEntry keyPart = entry.getKeyEntry();
					if (keyPart == null) {
						continue;
					}
					try {
						PublicKey serverKey = keyPart.resolvePublicKey(null,
								PublicKeyEntryResolver.IGNORING);
						if (serverKey == null) {
							LOG.warn(format(
									SshdText.get().knownHostsUnknownKeyType,
									path, entry.getConfigLine()));
						} else {
							newEntries.add(new HostEntryPair(entry, serverKey));
						}
					} catch (GeneralSecurityException e) {
						LOG.warn(format(SshdText.get().knownHostsInvalidLine,
								path, entry.getConfigLine()));
					}
				}
				return newEntries;
			} catch (FileNotFoundException | NoSuchFileException e) {
				resetReloadAttributes();
				return Collections.emptyList();
			}
		}
	}

	private int parsePort(String s) {
		try {
			return Integer.parseInt(s);
		} catch (NumberFormatException e) {
			return -1;
		}
	}

	private SshdSocketAddress toSshdSocketAddress(@NonNull String address) {
		String host = null;
		int port = 0;
		if (HostPatternsHolder.NON_STANDARD_PORT_PATTERN_ENCLOSURE_START_DELIM == address
				.charAt(0)) {
			int end = address.indexOf(
					HostPatternsHolder.NON_STANDARD_PORT_PATTERN_ENCLOSURE_END_DELIM);
			if (end <= 1) {
				return null; // Invalid
			}
			host = address.substring(1, end);
			if (end < address.length() - 1
					&& HostPatternsHolder.PORT_VALUE_DELIMITER == address
							.charAt(end + 1)) {
				port = parsePort(address.substring(end + 2));
			}
		} else {
			int i = address
					.lastIndexOf(HostPatternsHolder.PORT_VALUE_DELIMITER);
			if (i > 0) {
				port = parsePort(address.substring(i + 1));
				host = address.substring(0, i);
			} else {
				host = address;
			}
		}
		if (port < 0 || port > 65535) {
			return null;
		}
		return new SshdSocketAddress(host, port);
	}

	private Collection<SshdSocketAddress> getCandidates(
			@NonNull String connectAddress,
			@NonNull InetSocketAddress remoteAddress) {
		Collection<SshdSocketAddress> candidates = new TreeSet<>(
				SshdSocketAddress.BY_HOST_AND_PORT);
		candidates.add(SshdSocketAddress.toSshdSocketAddress(remoteAddress));
		SshdSocketAddress address = toSshdSocketAddress(connectAddress);
		if (address != null) {
			candidates.add(address);
		}
		return candidates;
	}

	private String createHostKeyLine(Collection<SshdSocketAddress> patterns,
			PublicKey key, Configuration config) throws Exception {
		StringBuilder result = new StringBuilder();
		if (config.getHashKnownHosts()) {
			// SHA1 is the only algorithm for host name hashing known to OpenSSH
			// or to Apache MINA sshd.
			NamedFactory<Mac> digester = KnownHostDigest.SHA1;
			Mac mac = digester.create();
			if (prng == null) {
				prng = new SecureRandom();
			}
			byte[] salt = new byte[mac.getDefaultBlockSize()];
			for (SshdSocketAddress address : patterns) {
				if (result.length() > 0) {
					result.append(',');
				}
				prng.nextBytes(salt);
				KnownHostHashValue.append(result, digester, salt,
						KnownHostHashValue.calculateHashValue(
								address.getHostName(), address.getPort(), mac,
								salt));
			}
		} else {
			for (SshdSocketAddress address : patterns) {
				if (result.length() > 0) {
					result.append(',');
				}
				KnownHostHashValue.appendHostPattern(result,
						address.getHostName(), address.getPort());
			}
		}
		result.append(' ');
		PublicKeyEntry.appendPublicKeyEntry(result, key);
		return result.toString();
	}

	private String updateHostKeyLine(String line, PublicKey newKey)
			throws IOException {
		// Replaces an existing public key by the new key
		int pos = line.indexOf(' ');
		if (pos > 0 && line.charAt(0) == KnownHostEntry.MARKER_INDICATOR) {
			// We're at the end of the marker. Skip ahead to the next blank.
			pos = line.indexOf(' ', pos + 1);
		}
		if (pos < 0) {
			// Don't update if bogus format
			return null;
		}
		StringBuilder result = new StringBuilder(line.substring(0, pos + 1));
		PublicKeyEntry.appendPublicKeyEntry(result, newKey);
		return result.toString();
	}

}
