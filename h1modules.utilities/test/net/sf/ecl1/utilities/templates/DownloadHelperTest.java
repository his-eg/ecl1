package net.sf.ecl1.utilities.templates;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

/**
 * Tests for the DownloadHelper
 * 
 * @author keunecke
 */
public class DownloadHelperTest {
	private static final Logger LOG = Logger.getLogger(DownloadHelperTest.class.getSimpleName());
	
	/**
	 * Tests if the downloaded version equals the local version
	 * @throws IOException
	 */
	@Test
	public void testGetInputStreamFromUrlFollowingRedirects() throws IOException {
		String templateListFileStr = System.getProperty("user.dir") + "/templates/templatelist.txt";
		LOG.info("templateListFileStr = " + templateListFileStr);
		File templateListFile = new File(templateListFileStr);
		try (FileInputStream localFile = new FileInputStream(templateListFile);) {
			assertNotNull(localFile);
			String contentsLocal = IOUtils.toString(localFile);
			LOG.log(Level.INFO, "local content = \n" + contentsLocal);
			assertThat(Boolean.valueOf(contentsLocal.isEmpty()), not(equalTo(Boolean.TRUE)));
			InputStream is = DownloadHelper.getInputStreamFromUrlFollowingRedirects("http://ecl1.sf.net/templates/templatelist.txt");
			assertThat(is, notNullValue());
			String contentsRemote = IOUtils.toString(is);
			assertThat(Boolean.valueOf(contentsRemote.isEmpty()), not(equalTo(Boolean.TRUE)));
			assertThat(contentsRemote, equalTo(contentsLocal));
		} catch (IOException e) {
			LOG.log(Level.SEVERE, "Caught IOException " + e, e);
			fail();
		}
	}
}
