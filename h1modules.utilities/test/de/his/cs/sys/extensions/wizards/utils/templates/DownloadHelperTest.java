/**
 * 
 */
package de.his.cs.sys.extensions.wizards.utils.templates;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

/**
 * Tests for the DownloadHelper
 * 
 * @author keunecke
 */
public class DownloadHelperTest {
	

	/**
	 * Tests if the downloaded version equals the local version
	 * @throws IOException
	 */
	@Test
	public void testGetInputStreamFromUrlFollowingRedirects() throws IOException {
		InputStream localFile = getClass().getResourceAsStream("/de/his/cs/sys/extensions/wizards/utils/templates/templatelist.txt");
		String contentsLocal = IOUtils.toString(localFile);
		assertThat(Boolean.valueOf(contentsLocal.isEmpty()), not(equalTo(Boolean.TRUE)));
		InputStream is = DownloadHelper.getInputStreamFromUrlFollowingRedirects("http://ecl1.sf.net/templates/templatelist.txt");
		assertThat(is, notNullValue());
		String contentsRemote = IOUtils.toString(is);
		assertThat(Boolean.valueOf(contentsRemote.isEmpty()), not(equalTo(Boolean.TRUE)));
		assertThat(contentsRemote, equalTo(contentsLocal));
	}

}