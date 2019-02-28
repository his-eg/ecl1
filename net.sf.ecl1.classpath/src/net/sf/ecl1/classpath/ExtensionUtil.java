package net.sf.ecl1.classpath;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.List;
import java.util.TreeMap;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import net.sf.ecl1.utilities.hisinone.HisConstants;
import net.sf.ecl1.utilities.hisinone.WebappsUtil;

/**
 * Utilities for extension handling.
 * @author keunecke
 */
public class ExtensionUtil {

	// singleton
    private static final ExtensionUtil INSTANCE = new ExtensionUtil();

    private IProject webappsProject;

    private ExtensionUtil() {
    	// singleton
    }
    
    /**
     * @return the single ExtensionUtil instance
     */
    public static ExtensionUtil getInstance() {
    	return INSTANCE;
    }
    
    /**
     * Find the webapps project in the workspace if there is one.
     */
    public void findWebappsProject() {
        webappsProject = WebappsUtil.findWebappsProject();
    }
    
    /**
     * checks if an extension jar in webapps project exists
     *
     * @param extension
     * @return
     */
    public boolean doesExtensionJarExist(String extension) {
        if (webappsProject != null) {
            IPath extensionJarPath = new Path(HisConstants.EXTENSIONS_FOLDER).append(extension).addFileExtension("jar");
            return webappsProject.exists(extensionJarPath);
        }
        return false;
    }

    /**
     * Does an extension project with the given name exist
     *
     * @param extension
     * @return
     */
    public boolean doesExtensionProjectExist(String extension) {
        IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(extension);
        return project.exists() && isExtensionProject(project);
    }

    /**
     * Check if a project is an extension project
     *
     * @param project
     * @return
     */
    public boolean isExtensionProject(IProject project) {
        IFile file = project.getFile("extension.ant.properties");
        return file.exists();
    }

    /**
     * Find all extensions lying as jars in the extension folder of the webapps project or as own projects in the workspace.
     * Extension projects override extension jars.
     * @param javaProject
     * @return map from extension names to extension project/jar names
     */
    public TreeMap<String, String> findAllExtensions() {
    	TreeMap<String, String> extensions = new TreeMap<String, String>(); // TreeMap required to sort extensions by name
        scanForExtensionJars(extensions);
        scanForExtensionProjects(extensions);
        // TODO evaluate deactivated-extensions.txt and override extension jars with extension projects only if the project is not deactivated
        return extensions;
    }
    
    /**
     * Scan for jar files in the extensions folder of the webapps project.
     *
     * @param javaProject
     * @param extensions the map from extension names to extension project/jar names, will be updated by this method
     */
    public void scanForExtensionJars(TreeMap<String, String> extensions) {
        // scan folder webapps/qisserver/WEB-INF/extensions/ for extension jars
        if (webappsProject != null) {
            IFolder extensionsFolder = webappsProject.getFolder(HisConstants.EXTENSIONS_FOLDER);
	        if (extensionsFolder.exists()) {
	            //if there is an extensions folder, scan it
	            IPath rawLocation = extensionsFolder.getRawLocation();
	            List<File> extensionJars = Arrays.asList(rawLocation.toFile().listFiles(new FilenameFilter() {
	                @Override
	                public boolean accept(File dir, String name) {
	                    return name != null && name.endsWith("jar");
	                }
	            }));
	            for (File extensionJar : extensionJars) {
	                extensions.put(extensionJar.getName().replace(".jar", ""), extensionJar.getName());
	            }
	        }
        }
    }

    /**
     * Scans for extension projects in workspace
     *
     * @param javaProject
     * @param extensions
     */
    public void scanForExtensionProjects(TreeMap<String, String> extensions) {
        //scan workspace for extension projects
        IWorkspaceRoot ws = ResourcesPlugin.getWorkspace().getRoot();
        List<IProject> projects = Arrays.asList(ws.getProjects(0));
        for (IProject project : projects) {
            if (isExtensionProject(project)) {
                extensions.put(project.getName(), project.getName());
            }
        }
    }
}
