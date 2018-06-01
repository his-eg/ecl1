package net.sf.ecl1.classpath;

import java.util.Collection;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import com.google.common.collect.Lists;

import net.sf.ecl1.utilities.general.ConsoleLogger;
import net.sf.ecl1.utilities.hisinone.HisConstants;
import net.sf.ecl1.utilities.hisinone.WebappsUtil;

/**
 * Utilities for extension handling
 * @author keunecke
 *
 */
public class ExtensionUtil {

    private static final ConsoleLogger logger = new ConsoleLogger(Activator.getDefault().getLog(), Activator.PLUGIN_ID);

    private IProject webappsProject;

    public ExtensionUtil() {
        webappsProject = WebappsUtil.findWebappsProject();
    }
    
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
}
