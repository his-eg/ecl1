/*-
 * Copyright (C) 2019, 2020 Salesforce and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.eclipse.jgit.gpg.bc.internal;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.text.MessageFormat;

import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.util.encoders.Hex;
import org.eclipse.jgit.api.errors.CanceledException;
import org.eclipse.jgit.errors.UnsupportedCredentialItem;
import org.eclipse.jgit.transport.CredentialItem.InformationalMessage;
import org.eclipse.jgit.transport.CredentialItem.Password;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.URIish;

/**
 * Prompts for a passphrase and caches it until {@link #clear() cleared}.
 * <p>
 * Implements {@link AutoCloseable} so it can be used within a
 * try-with-resources block.
 * </p>
 */
class BouncyCastleGpgKeyPassphrasePrompt implements AutoCloseable {

	private Password passphrase;

	private CredentialsProvider credentialsProvider;

	public BouncyCastleGpgKeyPassphrasePrompt(
			CredentialsProvider credentialsProvider) {
		this.credentialsProvider = credentialsProvider;
	}

	/**
	 * Clears any cached passphrase
	 */
	public void clear() {
		if (passphrase != null) {
			passphrase.clear();
			passphrase = null;
		}
	}

	@Override
	public void close() {
		clear();
	}

	private URIish createURI(Path keyLocation) throws URISyntaxException {
		return new URIish(keyLocation.toUri().toString());
	}

	/**
	 * Prompts use for a passphrase unless one was cached from a previous
	 * prompt.
	 *
	 * @param keyFingerprint
	 *            the fingerprint to show to the user during prompting
	 * @param keyLocation
	 *            the location the key was loaded from
	 * @return the passphrase (maybe <code>null</code>)
	 * @throws PGPException
	 * @throws CanceledException
	 *             in case passphrase was not entered by user
	 * @throws URISyntaxException
	 * @throws UnsupportedCredentialItem
	 */
	public char[] getPassphrase(byte[] keyFingerprint, Path keyLocation)
			throws PGPException, CanceledException, UnsupportedCredentialItem,
			URISyntaxException {
		if (passphrase == null) {
			passphrase = new Password(BCText.get().credentialPassphrase);
		}

		if (credentialsProvider == null) {
			throw new PGPException(BCText.get().gpgNoCredentialsProvider);
		}

		if (passphrase.getValue() == null
				&& !credentialsProvider.get(createURI(keyLocation),
						new InformationalMessage(
								MessageFormat.format(BCText.get().gpgKeyInfo,
										Hex.toHexString(keyFingerprint))),
						passphrase)) {
			throw new CanceledException(BCText.get().gpgSigningCancelled);
		}
		return passphrase.getValue();
	}

	/**
	 * Determines whether a passphrase was already obtained.
	 *
	 * @return {@code true} if a passphrase is already set, {@code false}
	 *         otherwise
	 */
	public boolean hasPassphrase() {
		return passphrase != null && passphrase.getValue() != null;
	}
}
