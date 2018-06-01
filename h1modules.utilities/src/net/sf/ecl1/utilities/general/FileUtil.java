package net.sf.ecl1.utilities.general;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;

import h1modules.utilities.utils.Activator;

/**
 * Utility methods to access files in a HISinOne project.
 * 
 * @author keunecke / tneumann
 */
public class FileUtil {

    private static final ConsoleLogger logger = new ConsoleLogger(Activator.getDefault().getLog(), Activator.PLUGIN_ID);

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
