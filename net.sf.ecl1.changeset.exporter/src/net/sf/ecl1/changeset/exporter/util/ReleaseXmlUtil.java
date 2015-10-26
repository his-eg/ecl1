package net.sf.ecl1.changeset.exporter.util;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;

import com.google.common.collect.Sets;

/**
 * Utilities for management of release.xml files
 *
 * @author keunecke
 *
 */
public class ReleaseXmlUtil {

    /**
     * Find release xml files in workspace
     * @return
     */
    public static Collection<IFile> getReleaseXmlFiles() {
        Collection<IFile> result = Sets.newHashSet();
        IFolder releaseFilesFolder = getReleaseXmlFolder();
        if (releaseFilesFolder.exists()) {
            try {
                List<IResource> releaseFileResources = Arrays.asList(releaseFilesFolder.members());
                for (IResource releaseFileResource : releaseFileResources) {
                    result.add((IFile) releaseFileResource);
                }
            } catch (CoreException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    /**
     * @param webapps
     * @return
     */
    public static IFolder getReleaseXmlFolder() {
        return getWebapps().getFolder("qisserver/WEB-INF/conf/service/patches/hisinone");
    }

    /**
     * Get a specific release.xml file
     *
     * @param file name
     * @return IFile
     */
    public static IFile getReleaseXmlFile(String file) {
        IFile releaseFile = getReleaseXmlFolder().getFile(file);
        if(releaseFile.exists()){
            return releaseFile;
        }
        return null;
    }

    /**
     * Utility method for determining webapps project branch and respective version number
     *
     * examples:
     * - HISinOne_VERSION_07 --> 7.0
     * - HISinOne_VERSION_06_RELEASE_01 --> 6.1
     * @return
     */
    public static String getVersionShortString() {
        IProject webapps = getWebapps();
        IFile file = webapps.getFile("CVS/Tag");
        if (!file.exists()) {
            return "HEAD";
        }
        String contents = readContent(file);
        if (contents != null) {
            String reducedBranch = contents.trim().replace("THISinOne_", "");
            List<String> branchNameComponents = Arrays.asList(reducedBranch.split("_"));
            String majorVersion = Integer.toString(Integer.parseInt(branchNameComponents.get(1)));
            String minorVersion = "0";
            if (branchNameComponents.size() > 2) {
                minorVersion = Integer.toString(Integer.parseInt(branchNameComponents.get(3)));
            }
            return majorVersion + "." + minorVersion;
        }
        return "UNKNOWN_VERSION";
    }

    /**
     * Read a file's content to a string
     *
     * @param file
     * @return
     */
    private static String readContent(IFile file) {
        byte[] encoded;
        try {
            encoded = Files.readAllBytes(Paths.get(file.getLocationURI()));
            return new String(encoded, Charset.defaultCharset()).trim();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static IProject getWebapps() {
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
