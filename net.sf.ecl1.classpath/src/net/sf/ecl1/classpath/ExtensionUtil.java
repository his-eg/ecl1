package net.sf.ecl1.classpath;

import static net.sf.ecl1.classpath.ClasspathContainerConstants.EXTENSIONS_FOLDER;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import com.google.common.collect.Lists;

/**
 * Utilities for extension handling
 * @author keunecke
 *
 */
public class ExtensionUtil {

    public boolean doesExtensionJarExist(String extension) {
        return doesExtensionExistAsJar(extension);
    }

    /**
     * checks if an extension jar in webapps project exists
     *
     * @param extension
     * @return
     */
    private boolean doesExtensionExistAsJar(String extension) {
        IProject webappsProject = findWebappsProject();
        if (webappsProject != null) {
            IPath extensionJarPath = new Path(EXTENSIONS_FOLDER).append(extension).addFileExtension("jar");
            return webappsProject.exists(extensionJarPath);
        }
        return false;
    }

    /**
     * Find the webapps project in the workspace
     *
     * @return the project serving as core webapps
     */
    private IProject findWebappsProject() {
        List<IProject> projects = Arrays.asList(ResourcesPlugin.getWorkspace().getRoot().getProjects(IWorkspaceRoot.INCLUDE_HIDDEN));
        for (IProject project : projects) {
            IFolder extensionsFolder = project.getFolder(EXTENSIONS_FOLDER);
            if (extensionsFolder != null && extensionsFolder.exists()) {
                return project;
            }
        }
        return null;
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
    public Collection<String> findExtensionJars() {
        IProject webapps = findWebappsProject();
        Collection<String> result = Lists.newArrayList();
        if (webapps != null) {
            IFolder folder = webapps.getFolder(EXTENSIONS_FOLDER);
            try {
                IResource[] members = folder.members();
                for (IResource member : members) {
                    if (member.getType() == IResource.FILE && "jar".equals(member.getFileExtension())) {
                        String name = member.getName().replace(".jar", "");
                        result.add(name);
                    }
                }
            } catch (CoreException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

}
