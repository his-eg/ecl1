package net.sf.ecl1.utilities.general;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.eclipse.core.resources.IFile;

import net.sf.ecl1.utilities.Activator;
import net.sf.ecl1.utilities.logging.ICommonLogger;
import net.sf.ecl1.utilities.logging.LoggerFactory;

/**
 * Utility methods to access files in a HISinOne project.
 * 
 * @author keunecke / tneumann
 */
public class FileUtil {

    private static final ICommonLogger logger = LoggerFactory.getLogger(FileUtil.class.getSimpleName(), Activator.PLUGIN_ID, Activator.getDefault() != null ? Activator.getDefault().getLog() : null);

    /**
     * Read a file's content to a string
     *
     * @param file
     * @return
     */
    public static String readContent(IFile file) {
        byte[] encoded;
        try {
            encoded = Files.readAllBytes(Paths.get(file.getLocationURI()));
            return new String(encoded, Charset.defaultCharset()).trim();
        } catch (IOException e) {
    		logger.error2(e.getMessage(), e);
        }
        return null;
    }
}
