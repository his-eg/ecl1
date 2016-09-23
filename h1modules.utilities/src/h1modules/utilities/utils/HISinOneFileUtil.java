package h1modules.utilities.utils;

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

/**
 * Utility methods to access files in a HISinOne project.
 * 
 * @author keunecke / tneumann
 */
public class HISinOneFileUtil {

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
            e.printStackTrace();
        }
        return null;
    }

    public static IProject getWebapps() {
        IWorkspace ws = ResourcesPlugin.getWorkspace();
        List<IProject> projects = Arrays.asList(ws.getRoot().getProjects());
        for (IProject project : projects) {
            if (isWebapps(project)) {
                return project;
            }
        }
        return null;
    }

    private static boolean isWebapps(IProject project) {
        return project.exists(new Path("qisserver"));
    }
}
