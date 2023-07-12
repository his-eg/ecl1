package net.sf.ecl1.commit.exporter;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;

import com.google.common.base.Joiner;

import net.sf.ecl1.commit.exporter.HotfixInformation;

/**
 * @author keunecke
 *
 */
public class HotfixInformationTest {

    private HotfixInformation hotfixInformation;

    @Before
    public void setUp() {
    	Set<String> fileNames = new HashSet<>();
    	fileNames.add("WEB-INF/conf/importdata/gx-hisinone.xml");
        hotfixInformation = new HotfixInformation("test", "description", "12345,54321", false, fileNames, new HashSet<>(), new HashSet<>());
    }

    /**
     * Test method for {@link net.sf.ecl1.commit.exporter.HotfixInformation#toXml()}.
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
