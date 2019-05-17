package net.sf.ecl1.utilities.templates;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Set;

import com.google.common.collect.Sets;

/**
 * Class to help with downloading files from sourceforge whilst allowing to follow redirects.
 * 
 * @author keunecke
 */
public abstract class DownloadHelper {
	
	/**
	 * private default constructor
	 */
	private DownloadHelper() {
		// nop
	}
	
	/**
	 * Get input stream from an URL whilst following redirects
	 * 
	 * @param url
	 * @return the {@link InputStream}
	 * @throws IOException
	 */
	public static InputStream getInputStreamFromUrlFollowingRedirects(String url) throws IOException {
		URL urlToFetch = new URL(url);
		HttpURLConnection conn = (HttpURLConnection) urlToFetch.openConnection();
		Set<String> passedRedirects = Sets.newHashSet(url);

		boolean redirect = isConnectionRedirected(conn);
		
		//follow redirects till redirects have stopped
		while (redirect) {
			// get redirect url from "location" header field
			String newUrl = conn.getHeaderField("Location");
			
			// check if we already were redirected to this url
			if(!passedRedirects.contains(newUrl)) {
				// open the new connnection again
				passedRedirects.add(newUrl);
				conn = (HttpURLConnection) new URL(newUrl).openConnection();
				redirect = isConnectionRedirected(conn);
			} else {
				throw new IOException(String.format("The URL '%s' lead to a redirect circle!", url));
			}
		}
		
		return conn.getInputStream();
	}

	private static boolean isConnectionRedirected(HttpURLConnection conn) throws IOException {
		boolean redirect = false;
		
		// normally, 3xx is redirect
		int status = conn.getResponseCode();
		if (status != HttpURLConnection.HTTP_OK) {
			if (status == HttpURLConnection.HTTP_MOVED_TEMP 
					|| status == HttpURLConnection.HTTP_MOVED_PERM 
					|| status == HttpURLConnection.HTTP_SEE_OTHER) {
				redirect = true;
			}
		}
		
		return redirect;
	}
}
