package net.sf.ecl1.classpath;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaProject;

import com.google.common.collect.Lists;

import net.sf.ecl1.utilities.general.ConsoleLogger;
import net.sf.ecl1.utilities.hisinone.HisConstants;
import net.sf.ecl1.utilities.hisinone.WebappsUtil;

/**
 * Utilities for extension handling.
 * @author keunecke
 */
public class ExtensionUtil {

    private static final ConsoleLogger logger = new ConsoleLogger(Activator.getDefault().getLog(), Activator.PLUGIN_ID);

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
     * Find all Extension Jars in webapps project
     * @return empty Collection if no webapps found, otherwise Collection of Strings with extension names
     */
    // TODO use findAllExtensions() instead
    public Collection<String> findExtensionJars() {
        Collection<String> result = Lists.newArrayList();
        if (webappsProject != null) {
            IFolder folder = webappsProject.getFolder(HisConstants.EXTENSIONS_FOLDER);
            try {
                IResource[] members = folder.members();
                for (IResource member : members) {
                    if (member.getType() == IResource.FILE && "jar".equals(member.getFileExtension())) {
                        String name = member.getName().replace(".jar", "");
                        result.add(name);
                    }
                }
            } catch (CoreException e) {
        		logger.error2(e.getMessage(), e);
            }
        }
        return result;
    }

    /**
     * Find all extensions lying as jars in the extension folder of the given java project or as own projects in the workspace.
     * Extension projects override extension jars.
     * @param javaProject
     * @return map from extension names to extension project/jar names
     */
    public Map<String, String> findAllExtensions(IJavaProject javaProject) {
        Map<String, String> extensions = new HashMap<String, String>();
        scanForExtensionJars(javaProject, extensions);
        scanForExtensionProjects(extensions);
        // TODO evaluate deactivated-extensions.txt and override extension jars with extension projects only if the project is not deactivated
        return extensions;
    }
    
    /**
     * Scan the java project for jar files in the extensions folder
     *
     * @param javaProject
     * @param extensions
     */
    public void scanForExtensionJars(IJavaProject javaProject, Map<String, String> extensions) {
        //scan workspace for extension jars
        IFolder extensionsFolder = javaProject.getProject().getFolder(HisConstants.EXTENSIONS_FOLDER);
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

    /**
     * Scans for extension projects in workspace
     *
     * @param javaProject
     * @param extensions
     */
    public void scanForExtensionProjects(Map<String, String> extensions) {
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
