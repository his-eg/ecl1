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

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URL;
import java.net.URLConnection;
import java.security.DigestOutputStream;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.SortedMap;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.stream.Collectors;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import org.eclipse.jgit.internal.JGitText;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.NullProgressMonitor;
import org.eclipse.jgit.lib.ProgressMonitor;
import org.eclipse.jgit.util.Base64;
import org.eclipse.jgit.util.HttpSupport;
import org.eclipse.jgit.util.StringUtils;
import org.eclipse.jgit.util.TemporaryBuffer;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

/**
 * A simple HTTP REST client for the Amazon S3 service.
 * <p>
 * This client uses the REST API to communicate with the Amazon S3 servers and
 * read or write content through a bucket that the user has access to. It is a
 * very lightweight implementation of the S3 API and therefore does not have all
 * of the bells and whistles of popular client implementations.
 * <p>
 * Authentication is always performed using the user's AWSAccessKeyId and their
 * private AWSSecretAccessKey.
 * <p>
 * Optional client-side encryption may be enabled if requested. The format is
 * compatible with <a href="http://jets3t.s3.amazonaws.com/index.html">jets3t</a>,
 * a popular Java based Amazon S3 client library. Enabling encryption can hide
 * sensitive data from the operators of the S3 service.
 */
public class AmazonS3 {
	private static final Set<String> SIGNED_HEADERS;

	private static final String HMAC = "HmacSHA1"; //$NON-NLS-1$

	private static final String X_AMZ_ACL = "x-amz-acl"; //$NON-NLS-1$

	private static final String X_AMZ_META = "x-amz-meta-"; //$NON-NLS-1$

	static {
		SIGNED_HEADERS = new HashSet<>();
		SIGNED_HEADERS.add("content-type"); //$NON-NLS-1$
		SIGNED_HEADERS.add("content-md5"); //$NON-NLS-1$
		SIGNED_HEADERS.add("date"); //$NON-NLS-1$
	}

	private static boolean isSignedHeader(String name) {
		final String nameLC = StringUtils.toLowerCase(name);
		return SIGNED_HEADERS.contains(nameLC) || nameLC.startsWith("x-amz-"); //$NON-NLS-1$
	}

	private static String toCleanString(List<String> list) {
		final StringBuilder s = new StringBuilder();
		for (String v : list) {
			if (s.length() > 0)
				s.append(',');
			s.append(v.replaceAll("\n", "").trim()); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return s.toString();
	}

	private static String remove(Map<String, String> m, String k) {
		final String r = m.remove(k);
		return r != null ? r : ""; //$NON-NLS-1$
	}

	private static String httpNow() {
		final String tz = "GMT"; //$NON-NLS-1$
		final SimpleDateFormat fmt;
		fmt = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss", Locale.US); //$NON-NLS-1$
		fmt.setTimeZone(TimeZone.getTimeZone(tz));
		return fmt.format(new Date()) + " " + tz; //$NON-NLS-1$
	}

	private static MessageDigest newMD5() {
		try {
			return MessageDigest.getInstance("MD5"); //$NON-NLS-1$
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(JGitText.get().JRELacksMD5Implementation, e);
		}
	}

	/** AWSAccessKeyId, public string that identifies the user's account. */
	private final String publicKey;

	/** Decoded form of the private AWSSecretAccessKey, to sign requests. */
	private final SecretKeySpec privateKey;

	/** Our HTTP proxy support, in case we are behind a firewall. */
	private final ProxySelector proxySelector;

	/** ACL to apply to created objects. */
	private final String acl;

	/** Maximum number of times to try an operation. */
	final int maxAttempts;

	/** Encryption algorithm, may be a null instance that provides pass-through. */
	private final WalkEncryption encryption;

	/** Directory for locally buffered content. */
	private final File tmpDir;

	/** S3 Bucket Domain. */
	private final String domain;

	/** Property names used in amazon connection configuration file. */
	interface Keys {
		String ACCESS_KEY = "accesskey"; //$NON-NLS-1$
		String SECRET_KEY = "secretkey"; //$NON-NLS-1$
		String PASSWORD = "password"; //$NON-NLS-1$
		String CRYPTO_ALG = "crypto.algorithm"; //$NON-NLS-1$
		String CRYPTO_VER = "crypto.version"; //$NON-NLS-1$
		String ACL = "acl"; //$NON-NLS-1$
		String DOMAIN = "domain"; //$NON-NLS-1$
		String HTTP_RETRY = "httpclient.retry-max"; //$NON-NLS-1$
		String TMP_DIR = "tmpdir"; //$NON-NLS-1$
	}

	/**
	 * Create a new S3 client for the supplied user information.
	 * <p>
	 * The connection properties are a subset of those supported by the popular
	 * <a href="http://jets3t.s3.amazonaws.com/index.html">jets3t</a> library.
	 * For example:
	 *
	 * <pre>
	 * # AWS Access and Secret Keys (required)
	 * accesskey: &lt;YourAWSAccessKey&gt;
	 * secretkey: &lt;YourAWSSecretKey&gt;
	 *
	 * # Access Control List setting to apply to uploads, must be one of:
	 * # PRIVATE, PUBLIC_READ (defaults to PRIVATE).
	 * acl: PRIVATE
	 *
	 * # S3 Domain
	 * # AWS S3 Region Domain (defaults to s3.amazonaws.com)
	 * domain: s3.amazonaws.com
	 *
	 * # Number of times to retry after internal error from S3.
	 * httpclient.retry-max: 3
	 *
	 * # End-to-end encryption (hides content from S3 owners)
	 * password: &lt;encryption pass-phrase&gt;
	 * crypto.algorithm: PBEWithMD5AndDES
	 * </pre>
	 *
	 * @param props
	 *            connection properties.
	 */
	public AmazonS3(final Properties props) {
		domain = props.getProperty(Keys.DOMAIN, "s3.amazonaws.com"); //$NON-NLS-1$

		publicKey = props.getProperty(Keys.ACCESS_KEY);
		if (publicKey == null)
			throw new IllegalArgumentException(JGitText.get().missingAccesskey);

		final String secret = props.getProperty(Keys.SECRET_KEY);
		if (secret == null)
			throw new IllegalArgumentException(JGitText.get().missingSecretkey);
		privateKey = new SecretKeySpec(Constants.encodeASCII(secret), HMAC);

		final String pacl = props.getProperty(Keys.ACL, "PRIVATE"); //$NON-NLS-1$
		if (StringUtils.equalsIgnoreCase("PRIVATE", pacl)) //$NON-NLS-1$
			acl = "private"; //$NON-NLS-1$
		else if (StringUtils.equalsIgnoreCase("PUBLIC", pacl)) //$NON-NLS-1$
			acl = "public-read"; //$NON-NLS-1$
		else if (StringUtils.equalsIgnoreCase("PUBLIC-READ", pacl)) //$NON-NLS-1$
			acl = "public-read"; //$NON-NLS-1$
		else if (StringUtils.equalsIgnoreCase("PUBLIC_READ", pacl)) //$NON-NLS-1$
			acl = "public-read"; //$NON-NLS-1$
		else
			throw new IllegalArgumentException("Invalid acl: " + pacl); //$NON-NLS-1$

		try {
			encryption = WalkEncryption.instance(props);
		} catch (GeneralSecurityException e) {
			throw new IllegalArgumentException(JGitText.get().invalidEncryption, e);
		}

		maxAttempts = Integer
				.parseInt(props.getProperty(Keys.HTTP_RETRY, "3")); //$NON-NLS-1$
		proxySelector = ProxySelector.getDefault();

		String tmp = props.getProperty(Keys.TMP_DIR);
		tmpDir = tmp != null && tmp.length() > 0 ? new File(tmp) : null;
	}

	/**
	 * Get the content of a bucket object.
	 *
	 * @param bucket
	 *            name of the bucket storing the object.
	 * @param key
	 *            key of the object within its bucket.
	 * @return connection to stream the content of the object. The request
	 *         properties of the connection may not be modified by the caller as
	 *         the request parameters have already been signed.
	 * @throws java.io.IOException
	 *             sending the request was not possible.
	 */
	public URLConnection get(String bucket, String key)
			throws IOException {
		for (int curAttempt = 0; curAttempt < maxAttempts; curAttempt++) {
			final HttpURLConnection c = open("GET", bucket, key); //$NON-NLS-1$
			authorize(c);
			switch (HttpSupport.response(c)) {
			case HttpURLConnection.HTTP_OK:
				encryption.validate(c, X_AMZ_META);
				return c;
			case HttpURLConnection.HTTP_NOT_FOUND:
				throw new FileNotFoundException(key);
			case HttpURLConnection.HTTP_INTERNAL_ERROR:
				continue;
			default:
				throw error(JGitText.get().s3ActionReading, key, c);
			}
		}
		throw maxAttempts(JGitText.get().s3ActionReading, key);
	}

	/**
	 * Decrypt an input stream from {@link #get(String, String)}.
	 *
	 * @param u
	 *            connection previously created by {@link #get(String, String)}}.
	 * @return stream to read plain text from.
	 * @throws java.io.IOException
	 *             decryption could not be configured.
	 */
	public InputStream decrypt(URLConnection u) throws IOException {
		return encryption.decrypt(u.getInputStream());
	}

	/**
	 * List the names of keys available within a bucket.
	 * <p>
	 * This method is primarily meant for obtaining a "recursive directory
	 * listing" rooted under the specified bucket and prefix location.
	 * It returns the keys sorted in reverse order of LastModified time
	 * (freshest keys first).
	 *
	 * @param bucket
	 *            name of the bucket whose objects should be listed.
	 * @param prefix
	 *            common prefix to filter the results by. Must not be null.
	 *            Supplying the empty string will list all keys in the bucket.
	 *            Supplying a non-empty string will act as though a trailing '/'
	 *            appears in prefix, even if it does not.
	 * @return list of keys starting with <code>prefix</code>, after removing
	 *         <code>prefix</code> (or <code>prefix + "/"</code>)from all
	 *         of them.
	 * @throws java.io.IOException
	 *             sending the request was not possible, or the response XML
	 *             document could not be parsed properly.
	 */
	public List<String> list(String bucket, String prefix)
			throws IOException {
		if (prefix.length() > 0 && !prefix.endsWith("/")) //$NON-NLS-1$
			prefix += "/"; //$NON-NLS-1$
		final ListParser lp = new ListParser(bucket, prefix);
		do {
			lp.list();
		} while (lp.truncated);

		Comparator<KeyInfo> comparator = Comparator.comparingLong(KeyInfo::getLastModifiedSecs);
		return lp.entries.stream().sorted(comparator.reversed())
			.map(KeyInfo::getName).collect(Collectors.toList());
	}

	/**
	 * Delete a single object.
	 * <p>
	 * Deletion always succeeds, even if the object does not exist.
	 *
	 * @param bucket
	 *            name of the bucket storing the object.
	 * @param key
	 *            key of the object within its bucket.
	 * @throws java.io.IOException
	 *             deletion failed due to communications error.
	 */
	public void delete(String bucket, String key)
			throws IOException {
		for (int curAttempt = 0; curAttempt < maxAttempts; curAttempt++) {
			final HttpURLConnection c = open("DELETE", bucket, key); //$NON-NLS-1$
			authorize(c);
			switch (HttpSupport.response(c)) {
			case HttpURLConnection.HTTP_NO_CONTENT:
				return;
			case HttpURLConnection.HTTP_INTERNAL_ERROR:
				continue;
			default:
				throw error(JGitText.get().s3ActionDeletion, key, c);
			}
		}
		throw maxAttempts(JGitText.get().s3ActionDeletion, key);
	}

	/**
	 * Atomically create or replace a single small object.
	 * <p>
	 * This form is only suitable for smaller contents, where the caller can
	 * reasonable fit the entire thing into memory.
	 * <p>
	 * End-to-end data integrity is assured by internally computing the MD5
	 * checksum of the supplied data and transmitting the checksum along with
	 * the data itself.
	 *
	 * @param bucket
	 *            name of the bucket storing the object.
	 * @param key
	 *            key of the object within its bucket.
	 * @param data
	 *            new data content for the object. Must not be null. Zero length
	 *            array will create a zero length object.
	 * @throws java.io.IOException
	 *             creation/updating failed due to communications error.
	 */
	public void put(String bucket, String key, byte[] data)
			throws IOException {
		if (encryption != WalkEncryption.NONE) {
			// We have to copy to produce the cipher text anyway so use
			// the large object code path as it supports that behavior.
			//
			try (OutputStream os = beginPut(bucket, key, null, null)) {
				os.write(data);
			}
			return;
		}

		final String md5str = Base64.encodeBytes(newMD5().digest(data));
		final String lenstr = String.valueOf(data.length);
		for (int curAttempt = 0; curAttempt < maxAttempts; curAttempt++) {
			final HttpURLConnection c = open("PUT", bucket, key); //$NON-NLS-1$
			c.setRequestProperty("Content-Length", lenstr); //$NON-NLS-1$
			c.setRequestProperty("Content-MD5", md5str); //$NON-NLS-1$
			c.setRequestProperty(X_AMZ_ACL, acl);
			authorize(c);
			c.setDoOutput(true);
			c.setFixedLengthStreamingMode(data.length);
			try (OutputStream os = c.getOutputStream()) {
				os.write(data);
			}

			switch (HttpSupport.response(c)) {
			case HttpURLConnection.HTTP_OK:
				return;
			case HttpURLConnection.HTTP_INTERNAL_ERROR:
				continue;
			default:
				throw error(JGitText.get().s3ActionWriting, key, c);
			}
		}
		throw maxAttempts(JGitText.get().s3ActionWriting, key);
	}

	/**
	 * Atomically create or replace a single large object.
	 * <p>
	 * Initially the returned output stream buffers data into memory, but if the
	 * total number of written bytes starts to exceed an internal limit the data
	 * is spooled to a temporary file on the local drive.
	 * <p>
	 * Network transmission is attempted only when <code>close()</code> gets
	 * called at the end of output. Closing the returned stream can therefore
	 * take significant time, especially if the written content is very large.
	 * <p>
	 * End-to-end data integrity is assured by internally computing the MD5
	 * checksum of the supplied data and transmitting the checksum along with
	 * the data itself.
	 *
	 * @param bucket
	 *            name of the bucket storing the object.
	 * @param key
	 *            key of the object within its bucket.
	 * @param monitor
	 *            (optional) progress monitor to post upload completion to
	 *            during the stream's close method.
	 * @param monitorTask
	 *            (optional) task name to display during the close method.
	 * @return a stream which accepts the new data, and transmits once closed.
	 * @throws java.io.IOException
	 *             if encryption was enabled it could not be configured.
	 */
	public OutputStream beginPut(final String bucket, final String key,
			final ProgressMonitor monitor, final String monitorTask)
			throws IOException {
		final MessageDigest md5 = newMD5();
		final TemporaryBuffer buffer = new TemporaryBuffer.LocalFile(tmpDir) {
			@Override
			public void close() throws IOException {
				super.close();
				try {
					putImpl(bucket, key, md5.digest(), this, monitor,
							monitorTask);
				} finally {
					destroy();
				}
			}
		};
		return encryption.encrypt(new DigestOutputStream(buffer, md5));
	}

	void putImpl(final String bucket, final String key,
			final byte[] csum, final TemporaryBuffer buf,
			ProgressMonitor monitor, String monitorTask) throws IOException {
		if (monitor == null)
			monitor = NullProgressMonitor.INSTANCE;
		if (monitorTask == null)
			monitorTask = MessageFormat.format(JGitText.get().progressMonUploading, key);

		final String md5str = Base64.encodeBytes(csum);
		final long len = buf.length();
		for (int curAttempt = 0; curAttempt < maxAttempts; curAttempt++) {
			final HttpURLConnection c = open("PUT", bucket, key); //$NON-NLS-1$
			c.setFixedLengthStreamingMode(len);
			c.setRequestProperty("Content-MD5", md5str); //$NON-NLS-1$
			c.setRequestProperty(X_AMZ_ACL, acl);
			encryption.request(c, X_AMZ_META);
			authorize(c);
			c.setDoOutput(true);
			monitor.beginTask(monitorTask, (int) (len / 1024));
			try (OutputStream os = c.getOutputStream()) {
				buf.writeTo(os, monitor);
			} finally {
				monitor.endTask();
			}

			switch (HttpSupport.response(c)) {
			case HttpURLConnection.HTTP_OK:
				return;
			case HttpURLConnection.HTTP_INTERNAL_ERROR:
				continue;
			default:
				throw error(JGitText.get().s3ActionWriting, key, c);
			}
		}
		throw maxAttempts(JGitText.get().s3ActionWriting, key);
	}

	IOException error(final String action, final String key,
			final HttpURLConnection c) throws IOException {
		final IOException err = new IOException(MessageFormat.format(
				JGitText.get().amazonS3ActionFailed, action, key,
				Integer.valueOf(HttpSupport.response(c)),
				c.getResponseMessage()));
		if (c.getErrorStream() == null) {
			return err;
		}

		try (InputStream errorStream = c.getErrorStream()) {
			final ByteArrayOutputStream b = new ByteArrayOutputStream();
			byte[] buf = new byte[2048];
			for (;;) {
				final int n = errorStream.read(buf);
				if (n < 0) {
					break;
				}
				if (n > 0) {
					b.write(buf, 0, n);
				}
			}
			buf = b.toByteArray();
			if (buf.length > 0) {
				err.initCause(new IOException("\n" + new String(buf, UTF_8))); //$NON-NLS-1$
			}
		}
		return err;
	}

	IOException maxAttempts(String action, String key) {
		return new IOException(MessageFormat.format(
				JGitText.get().amazonS3ActionFailedGivingUp, action, key,
				Integer.valueOf(maxAttempts)));
	}

	private HttpURLConnection open(final String method, final String bucket,
			final String key) throws IOException {
		final Map<String, String> noArgs = Collections.emptyMap();
		return open(method, bucket, key, noArgs);
	}

	HttpURLConnection open(final String method, final String bucket,
			final String key, final Map<String, String> args)
			throws IOException {
		final StringBuilder urlstr = new StringBuilder();
		urlstr.append("http://"); //$NON-NLS-1$
		urlstr.append(bucket);
		urlstr.append('.');
		urlstr.append(domain);
		urlstr.append('/');
		if (key.length() > 0)
			HttpSupport.encode(urlstr, key);
		if (!args.isEmpty()) {
			final Iterator<Map.Entry<String, String>> i;

			urlstr.append('?');
			i = args.entrySet().iterator();
			while (i.hasNext()) {
				final Map.Entry<String, String> e = i.next();
				urlstr.append(e.getKey());
				urlstr.append('=');
				HttpSupport.encode(urlstr, e.getValue());
				if (i.hasNext())
					urlstr.append('&');
			}
		}

		final URL url = new URL(urlstr.toString());
		final Proxy proxy = HttpSupport.proxyFor(proxySelector, url);
		final HttpURLConnection c;

		c = (HttpURLConnection) url.openConnection(proxy);
		c.setRequestMethod(method);
		c.setRequestProperty("User-Agent", "jgit/1.0"); //$NON-NLS-1$ //$NON-NLS-2$
		c.setRequestProperty("Date", httpNow()); //$NON-NLS-1$
		return c;
	}

	void authorize(HttpURLConnection c) throws IOException {
		final Map<String, List<String>> reqHdr = c.getRequestProperties();
		final SortedMap<String, String> sigHdr = new TreeMap<>();
		for (Map.Entry<String, List<String>> entry : reqHdr.entrySet()) {
			final String hdr = entry.getKey();
			if (isSignedHeader(hdr))
				sigHdr.put(StringUtils.toLowerCase(hdr), toCleanString(entry.getValue()));
		}

		final StringBuilder s = new StringBuilder();
		s.append(c.getRequestMethod());
		s.append('\n');

		s.append(remove(sigHdr, "content-md5")); //$NON-NLS-1$
		s.append('\n');

		s.append(remove(sigHdr, "content-type")); //$NON-NLS-1$
		s.append('\n');

		s.append(remove(sigHdr, "date")); //$NON-NLS-1$
		s.append('\n');

		for (Map.Entry<String, String> e : sigHdr.entrySet()) {
			s.append(e.getKey());
			s.append(':');
			s.append(e.getValue());
			s.append('\n');
		}

		final String host = c.getURL().getHost();
		s.append('/');
		s.append(host.substring(0, host.length() - domain.length() - 1));
		s.append(c.getURL().getPath());

		final String sec;
		try {
			final Mac m = Mac.getInstance(HMAC);
			m.init(privateKey);
			sec = Base64.encodeBytes(m.doFinal(s.toString().getBytes(UTF_8)));
		} catch (NoSuchAlgorithmException e) {
			throw new IOException(MessageFormat.format(JGitText.get().noHMACsupport, HMAC, e.getMessage()));
		} catch (InvalidKeyException e) {
			throw new IOException(MessageFormat.format(JGitText.get().invalidKey, e.getMessage()));
		}
		c.setRequestProperty("Authorization", "AWS " + publicKey + ":" + sec); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	static Properties properties(File authFile)
			throws FileNotFoundException, IOException {
		final Properties p = new Properties();
		try (FileInputStream in = new FileInputStream(authFile)) {
			p.load(in);
		}
		return p;
	}

	/**
	 * KeyInfo enables sorting of keys by lastModified time
	 */
	private static final class KeyInfo {
		private final String name;
		private final long lastModifiedSecs;
		public KeyInfo(String aname, long lsecs) {
			name = aname;
			lastModifiedSecs = lsecs;
		}
		public String getName() {
			return name;
		}
		public long getLastModifiedSecs() {
			return lastModifiedSecs;
		}
	}

	private final class ListParser extends DefaultHandler {
		final List<KeyInfo> entries = new ArrayList<>();

		private final String bucket;

		private final String prefix;

		boolean truncated;

		private StringBuilder data;
		private String keyName;
		private Instant keyLastModified;

		ListParser(String bn, String p) {
			bucket = bn;
			prefix = p;
		}

		void list() throws IOException {
			final Map<String, String> args = new TreeMap<>();
			if (prefix.length() > 0)
				args.put("prefix", prefix); //$NON-NLS-1$
			if (!entries.isEmpty())
				args.put("marker", prefix + entries.get(entries.size() - 1).getName()); //$NON-NLS-1$

			for (int curAttempt = 0; curAttempt < maxAttempts; curAttempt++) {
				final HttpURLConnection c = open("GET", bucket, "", args); //$NON-NLS-1$ //$NON-NLS-2$
				authorize(c);
				switch (HttpSupport.response(c)) {
				case HttpURLConnection.HTTP_OK:
					truncated = false;
					data = null;
					keyName = null;
					keyLastModified = null;

					final XMLReader xr;
					try {
						xr = SAXParserFactory.newInstance().newSAXParser()
								.getXMLReader();
					} catch (SAXException | ParserConfigurationException e) {
						throw new IOException(
								JGitText.get().noXMLParserAvailable, e);
					}
					xr.setContentHandler(this);
					try (InputStream in = c.getInputStream()) {
						xr.parse(new InputSource(in));
					} catch (SAXException parsingError) {
						throw new IOException(
								MessageFormat.format(
										JGitText.get().errorListing, prefix),
								parsingError);
					}
					return;

				case HttpURLConnection.HTTP_INTERNAL_ERROR:
					continue;

				default:
					throw AmazonS3.this.error("Listing", prefix, c); //$NON-NLS-1$
				}
			}
			throw maxAttempts("Listing", prefix); //$NON-NLS-1$
		}

		@Override
		public void startElement(final String uri, final String name,
				final String qName, final Attributes attributes)
				throws SAXException {
			if ("Key".equals(name) || "IsTruncated".equals(name) || "LastModified".equals(name)) { //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				data = new StringBuilder();
			}
			if ("Contents".equals(name)) { //$NON-NLS-1$
				keyName = null;
				keyLastModified = null;
			}
		}

		@Override
		public void ignorableWhitespace(final char[] ch, final int s,
				final int n) throws SAXException {
			if (data != null)
				data.append(ch, s, n);
		}

		@Override
		public void characters(char[] ch, int s, int n)
				throws SAXException {
			if (data != null)
				data.append(ch, s, n);
		}

		@Override
		public void endElement(final String uri, final String name,
				final String qName) throws SAXException {
			if ("Key".equals(name))  { //$NON-NLS-1$
				keyName = data.toString().substring(prefix.length());
			} else if ("IsTruncated".equals(name)) { //$NON-NLS-1$
				truncated = StringUtils.equalsIgnoreCase("true", data.toString()); //$NON-NLS-1$
			} else if ("LastModified".equals(name)) { //$NON-NLS-1$
				keyLastModified = Instant.parse(data.toString());
			} else if ("Contents".equals(name)) { //$NON-NLS-1$
				entries.add(new KeyInfo(keyName, keyLastModified.getEpochSecond()));
			}

			data = null;
		}
	}
}
