/*
 * Copyright (C) 2018, Thomas Wolf <thomas.wolf@paranor.ch> and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.eclipse.jgit.internal.transport.sshd.auth;

import static java.text.MessageFormat.format;

import java.io.IOException;
import java.net.InetSocketAddress;

import org.eclipse.jgit.internal.transport.sshd.GssApiMechanisms;
import org.eclipse.jgit.internal.transport.sshd.SshdText;
import org.ietf.jgss.GSSContext;

/**
 * An abstract implementation of a GSS-API multi-round authentication.
 *
 * @param <ParameterType>
 *            defining the parameter type for the authentication
 * @param <TokenType>
 *            defining the token type for the authentication
 */
public abstract class GssApiAuthentication<ParameterType, TokenType>
		extends AbstractAuthenticationHandler<ParameterType, TokenType> {

	private GSSContext context;

	/** The last token generated. */
	protected byte[] token;

	/**
	 * Creates a new {@link GssApiAuthentication} to authenticate with the given
	 * {@code proxy}.
	 *
	 * @param proxy
	 *            the {@link InetSocketAddress} of the proxy to connect to
	 */
	public GssApiAuthentication(InetSocketAddress proxy) {
		super(proxy);
	}

	@Override
	public void close() {
		GssApiMechanisms.closeContextSilently(context);
		context = null;
		done = true;
	}

	@Override
	public final void start() throws Exception {
		try {
			context = createContext();
			context.requestMutualAuth(true);
			context.requestConf(false);
			context.requestInteg(false);
			byte[] empty = new byte[0];
			token = context.initSecContext(empty, 0, 0);
		} catch (Exception e) {
			close();
			throw e;
		}
	}

	@Override
	public final void process() throws Exception {
		if (context == null) {
			throw new IOException(
					format(SshdText.get().proxyCannotAuthenticate, proxy));
		}
		try {
			byte[] received = extractToken(params);
			token = context.initSecContext(received, 0, received.length);
			checkDone();
		} catch (Exception e) {
			close();
			throw e;
		}
	}

	private void checkDone() throws Exception {
		done = context.isEstablished();
		if (done) {
			context.dispose();
			context = null;
		}
	}

	/**
	 * Creates the {@link GSSContext} to use.
	 *
	 * @return a fresh {@link GSSContext} to use
	 * @throws Exception
	 *             if the context cannot be created
	 */
	protected abstract GSSContext createContext() throws Exception;

	/**
	 * Extracts the token from the last set parameters.
	 *
	 * @param input
	 *            to extract the token from
	 * @return the extracted token, or {@code null} if none
	 * @throws Exception
	 *             if an error occurs
	 */
	protected abstract byte[] extractToken(ParameterType input)
			throws Exception;
}
