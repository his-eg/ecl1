/**
 *
 */
package net.sf.ecl1.changeset.exporter;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;

import com.google.common.base.Joiner;

/**
 * @author keunecke
 *
 */
public class HotfixInformationTest {

    private HotfixInformation hotfixInformation;

    @Before
    public void setUp() {
        hotfixInformation = new HotfixInformation("test", "describtion", "12345,54321");
        hotfixInformation.addFile("WEB-INF/conf/importdata/gx-hisinone.xml");
    }

    /**
     * Test method for {@link net.sf.ecl1.changeset.exporter.HotfixInformation#toXml()}.
     */
    @Test
    public void testToXml() throws Exception {
        String actual = hotfixInformation.toXml();
        String expected = readTestFile();
        assertThat(actual, is(expected));
    }

    private String readTestFile() throws IOException {
        InputStream is = getClass().getResourceAsStream("testhotfix.xml");
        List<String> lines = IOUtils.readLines(is);
        return Joiner.on("\n").join(lines);
    }

}
