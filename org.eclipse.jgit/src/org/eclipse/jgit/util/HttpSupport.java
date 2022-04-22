/*
 * Copyright (C) 2010, Google Inc.
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org> and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.eclipse.jgit.util;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;

import org.eclipse.jgit.internal.JGitText;
import org.eclipse.jgit.transport.http.HttpConnection;
import org.eclipse.jgit.transport.http.NoCheckX509TrustManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Extra utilities to support usage of HTTP.
 */
public class HttpSupport {
	private final static Logger LOG = LoggerFactory
			.getLogger(HttpSupport.class);

	/** The {@code GET} HTTP method. */
	public static final String METHOD_GET = "GET"; //$NON-NLS-1$

	/** The {@code HEAD} HTTP method.
	 * @since 4.3 */
	public static final String METHOD_HEAD = "HEAD"; //$NON-NLS-1$

	/** The {@code POST} HTTP method.
	 * @since 4.3 */
	public static final String METHOD_PUT = "PUT"; //$NON-NLS-1$

	/** The {@code POST} HTTP method. */
	public static final String METHOD_POST = "POST"; //$NON-NLS-1$

	/** The {@code Cache-Control} header. */
	public static final String HDR_CACHE_CONTROL = "Cache-Control"; //$NON-NLS-1$

	/** The {@code Pragma} header. */
	public static final String HDR_PRAGMA = "Pragma"; //$NON-NLS-1$

	/** The {@code User-Agent} header. */
	public static final String HDR_USER_AGENT = "User-Agent"; //$NON-NLS-1$

	/**
	 * The {@code Server} header.
	 * @since 4.0
	 */
	public static final String HDR_SERVER = "Server"; //$NON-NLS-1$

	/** The {@code Date} header. */
	public static final String HDR_DATE = "Date"; //$NON-NLS-1$

	/** The {@code Expires} header. */
	public static final String HDR_EXPIRES = "Expires"; //$NON-NLS-1$

	/** The {@code ETag} header. */
	public static final String HDR_ETAG = "ETag"; //$NON-NLS-1$

	/** The {@code If-None-Match} header. */
	public static final String HDR_IF_NONE_MATCH = "If-None-Match"; //$NON-NLS-1$

	/** The {@code Last-Modified} header. */
	public static final String HDR_LAST_MODIFIED = "Last-Modified"; //$NON-NLS-1$

	/** The {@code If-Modified-Since} header. */
	public static final String HDR_IF_MODIFIED_SINCE = "If-Modified-Since"; //$NON-NLS-1$

	/** The {@code Accept} header. */
	public static final String HDR_ACCEPT = "Accept"; //$NON-NLS-1$

	/** The {@code Content-Type} header. */
	public static final String HDR_CONTENT_TYPE = "Content-Type"; //$NON-NLS-1$

	/** The {@code Content-Length} header. */
	public static final String HDR_CONTENT_LENGTH = "Content-Length"; //$NON-NLS-1$

	/** The {@code Content-Encoding} header. */
	public static final String HDR_CONTENT_ENCODING = "Content-Encoding"; //$NON-NLS-1$

	/** The {@code Content-Range} header. */
	public static final String HDR_CONTENT_RANGE = "Content-Range"; //$NON-NLS-1$

	/** The {@code Accept-Ranges} header. */
	public static final String HDR_ACCEPT_RANGES = "Accept-Ranges"; //$NON-NLS-1$

	/** The {@code If-Range} header. */
	public static final String HDR_IF_RANGE = "If-Range"; //$NON-NLS-1$

	/** The {@code Range} header. */
	public static final String HDR_RANGE = "Range"; //$NON-NLS-1$

	/** The {@code Accept-Encoding} header. */
	public static final String HDR_ACCEPT_ENCODING = "Accept-Encoding"; //$NON-NLS-1$

	/**
	 * The {@code Location} header.
	 * @since 4.7
	 */
	public static final String HDR_LOCATION = "Location"; //$NON-NLS-1$

	/** The {@code gzip} encoding value for {@link #HDR_ACCEPT_ENCODING}. */
	public static final String ENCODING_GZIP = "gzip"; //$NON-NLS-1$

	/**
	 * The {@code x-gzip} encoding value for {@link #HDR_ACCEPT_ENCODING}.
	 * @since 4.6
	 */
	public static final String ENCODING_X_GZIP = "x-gzip"; //$NON-NLS-1$

	/** The standard {@code text/plain} MIME type. */
	public static final String TEXT_PLAIN = "text/plain"; //$NON-NLS-1$

	/** The {@code Authorization} header. */
	public static final String HDR_AUTHORIZATION = "Authorization"; //$NON-NLS-1$

	/** The {@code WWW-Authenticate} header. */
	public static final String HDR_WWW_AUTHENTICATE = "WWW-Authenticate"; //$NON-NLS-1$

	/**
	 * The {@code Cookie} header.
	 *
	 * @since 5.4
	 */
	public static final String HDR_COOKIE = "Cookie"; //$NON-NLS-1$

	/**
	 * The {@code Set-Cookie} header.
	 *
	 * @since 5.4
	 */
	public static final String HDR_SET_COOKIE = "Set-Cookie"; //$NON-NLS-1$

	/**
	 * The {@code Set-Cookie2} header.
	 *
	 * @since 5.4
	 */
	public static final String HDR_SET_COOKIE2 = "Set-Cookie2"; //$NON-NLS-1$

	private static Set<String> configuredHttpsProtocols;

	/**
	 * URL encode a value string into an output buffer.
	 *
	 * @param urlstr
	 *            the output buffer.
	 * @param key
	 *            value which must be encoded to protected special characters.
	 */
	public static void encode(StringBuilder urlstr, String key) {
		if (key == null || key.length() == 0)
			return;
		try {
			urlstr.append(URLEncoder.encode(key, UTF_8.name()));
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(JGitText.get().couldNotURLEncodeToUTF8, e);
		}
	}

	/**
	 * Get the HTTP response code from the request.
	 * <p>
	 * Roughly the same as <code>c.getResponseCode()</code> but the
	 * ConnectException is translated to be more understandable.
	 *
	 * @param c
	 *            connection the code should be obtained from.
	 * @return r HTTP status code, usually 200 to indicate success. See
	 *         {@link org.eclipse.jgit.transport.http.HttpConnection} for other
	 *         defined constants.
	 * @throws java.io.IOException
	 *             communications error prevented obtaining the response code.
	 * @since 3.3
	 */
	public static int response(HttpConnection c) throws IOException {
		try {
			return c.getResponseCode();
		} catch (ConnectException ce) {
			final URL url = c.getURL();
			final String host = (url == null) ? "<null>" : url.getHost(); //$NON-NLS-1$
			// The standard J2SE error message is not very useful.
			//
			if ("Connection timed out: connect".equals(ce.getMessage())) //$NON-NLS-1$
				throw new ConnectException(MessageFormat.format(JGitText.get().connectionTimeOut, host));
			throw new ConnectException(ce.getMessage() + " " + host); //$NON-NLS-1$
		}
	}

	/**
	 * Get the HTTP response code from the request.
	 * <p>
	 * Roughly the same as <code>c.getResponseCode()</code> but the
	 * ConnectException is translated to be more understandable.
	 *
	 * @param c
	 *            connection the code should be obtained from.
	 * @return r HTTP status code, usually 200 to indicate success. See
	 *         {@link org.eclipse.jgit.transport.http.HttpConnection} for other
	 *         defined constants.
	 * @throws java.io.IOException
	 *             communications error prevented obtaining the response code.
	 */
	public static int response(java.net.HttpURLConnection c)
			throws IOException {
		try {
			return c.getResponseCode();
		} catch (ConnectException ce) {
			final URL url = c.getURL();
			final String host = (url == null) ? "<null>" : url.getHost(); //$NON-NLS-1$
			// The standard J2SE error message is not very useful.
			//
			if ("Connection timed out: connect".equals(ce.getMessage())) //$NON-NLS-1$
				throw new ConnectException(MessageFormat.format(
						JGitText.get().connectionTimeOut, host));
			throw new ConnectException(ce.getMessage() + " " + host); //$NON-NLS-1$
		}
	}

	/**
	 * Extract a HTTP header from the response.
	 *
	 * @param c
	 *            connection the header should be obtained from.
	 * @param headerName
	 *            the header name
	 * @return the header value
	 * @throws java.io.IOException
	 *             communications error prevented obtaining the header.
	 * @since 4.7
	 */
	public static String responseHeader(final HttpConnection c,
			final String headerName) throws IOException {
		return c.getHeaderField(headerName);
	}

	/**
	 * Determine the proxy server (if any) needed to obtain a URL.
	 *
	 * @param proxySelector
	 *            proxy support for the caller.
	 * @param u
	 *            location of the server caller wants to talk to.
	 * @return proxy to communicate with the supplied URL.
	 * @throws java.net.ConnectException
	 *             the proxy could not be computed as the supplied URL could not
	 *             be read. This failure should never occur.
	 */
	public static Proxy proxyFor(ProxySelector proxySelector, URL u)
			throws ConnectException {
		try {
			URI uri = new URI(u.getProtocol(), null, u.getHost(), u.getPort(),
					null, null, null);
			return proxySelector.select(uri).get(0);
		} catch (URISyntaxException e) {
			final ConnectException err;
			err = new ConnectException(MessageFormat.format(JGitText.get().cannotDetermineProxyFor, u));
			err.initCause(e);
			throw err;
		}
	}

	/**
	 * Disable SSL and hostname verification for given HTTP connection
	 *
	 * @param conn
	 *            a {@link org.eclipse.jgit.transport.http.HttpConnection}
	 *            object.
	 * @throws java.io.IOException
	 * @since 4.3
	 */
	public static void disableSslVerify(HttpConnection conn)
			throws IOException {
		TrustManager[] trustAllCerts = {
				new NoCheckX509TrustManager() };
		try {
			conn.configure(null, trustAllCerts, null);
			conn.setHostnameVerifier((name, session) -> true);
		} catch (KeyManagementException | NoSuchAlgorithmException e) {
			throw new IOException(e.getMessage(), e);
		}
	}

	/**
	 * Enables all supported TLS protocol versions on the socket given. If
	 * system property "https.protocols" is set, only protocols specified there
	 * are enabled.
	 * <p>
	 * This is primarily a mechanism to deal with using TLS on IBM JDK. IBM JDK
	 * returns sockets that support all TLS protocol versions but have only the
	 * one specified in the context enabled. Oracle or OpenJDK return sockets
	 * that have all available protocols enabled already, up to the one
	 * specified.
	 * <p>
	 * <table>
	 * <tr>
	 * <td>SSLContext.getInstance()</td>
	 * <td>OpenJDK</td>
	 * <td>IDM JDK</td>
	 * </tr>
	 * <tr>
	 * <td>"TLS"</td>
	 * <td>Supported: TLSv1, TLSV1.1, TLSv1.2 (+ TLSv1.3)<br />
	 * Enabled: TLSv1, TLSV1.1, TLSv1.2 (+ TLSv1.3)</td>
	 * <td>Supported: TLSv1, TLSV1.1, TLSv1.2<br />
	 * Enabled: TLSv1</td>
	 * </tr>
	 * <tr>
	 * <td>"TLSv1.2"</td>
	 * <td>Supported: TLSv1, TLSV1.1, TLSv1.2<br />
	 * Enabled: TLSv1, TLSV1.1, TLSv1.2</td>
	 * <td>Supported: TLSv1, TLSV1.1, TLSv1.2<br />
	 * Enabled: TLSv1.2</td>
	 * </tr>
	 * </table>
	 *
	 * @param socket
	 *            to configure
	 * @see <a href=
	 *      "https://www.ibm.com/support/knowledgecenter/en/SSYKE2_8.0.0/com.ibm.java.security.component.80.doc/security-component/jsse2Docs/matchsslcontext_tls.html">Behavior
	 *      of SSLContext.getInstance("TLS") on IBM JDK</a>
	 * @see <a href=
	 *      "https://docs.oracle.com/javase/8/docs/technotes/guides/security/jsse/JSSERefGuide.html#InstallationAndCustomization">Customizing
	 *      JSSE about https.protocols</a>
	 * @since 5.7
	 */
	public static void configureTLS(SSLSocket socket) {
		// 1. Enable all available TLS protocol versions
		Set<String> enabled = new LinkedHashSet<>(
				Arrays.asList(socket.getEnabledProtocols()));
		for (String s : socket.getSupportedProtocols()) {
			if (s.startsWith("TLS")) { //$NON-NLS-1$
				enabled.add(s);
			}
		}
		// 2. Respect the https.protocols system property
		Set<String> configured = getConfiguredProtocols();
		if (!configured.isEmpty()) {
			enabled.retainAll(configured);
		}
		if (!enabled.isEmpty()) {
			socket.setEnabledProtocols(enabled.toArray(new String[0]));
		}
	}

	private static Set<String> getConfiguredProtocols() {
		Set<String> result = configuredHttpsProtocols;
		if (result == null) {
			String configured = getProperty("https.protocols"); //$NON-NLS-1$
			if (StringUtils.isEmptyOrNull(configured)) {
				result = Collections.emptySet();
			} else {
				result = new LinkedHashSet<>(
						Arrays.asList(configured.split("\\s*,\\s*"))); //$NON-NLS-1$
			}
			configuredHttpsProtocols = result;
		}
		return result;
	}

	private static String getProperty(String property) {
		try {
			return SystemReader.getInstance().getProperty(property);
		} catch (SecurityException e) {
			LOG.warn(JGitText.get().failedReadHttpsProtocols, e);
			return null;
		}
	}

	/**
	 * Scan a RFC 7230 token as it appears in HTTP headers.
	 *
	 * @param header
	 *            to scan in
	 * @param from
	 *            index in {@code header} to start scanning at
	 * @return the index after the token, that is, on the first non-token
	 *         character or {@code header.length}
	 * @throws IndexOutOfBoundsException
	 *             if {@code from < 0} or {@code from > header.length()}
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc7230#appendix-B">RFC 7230,
	 *      Appendix B: Collected Grammar; "token" production</a>
	 * @since 5.10
	 */
	public static int scanToken(String header, int from) {
		int length = header.length();
		int i = from;
		if (i < 0 || i > length) {
			throw new IndexOutOfBoundsException();
		}
		while (i < length) {
			char c = header.charAt(i);
			switch (c) {
			case '!':
			case '#':
			case '$':
			case '%':
			case '&':
			case '\'':
			case '*':
			case '+':
			case '-':
			case '.':
			case '^':
			case '_':
			case '`':
			case '|':
			case '~':
			case '0':
			case '1':
			case '2':
			case '3':
			case '4':
			case '5':
			case '6':
			case '7':
			case '8':
			case '9':
				i++;
				break;
			default:
				if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) {
					i++;
					break;
				}
				return i;
			}
		}
		return i;
	}

	private HttpSupport() {
		// Utility class only.
	}
}
