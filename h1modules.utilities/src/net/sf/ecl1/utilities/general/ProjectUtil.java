package net.sf.ecl1.utilities.general;

import java.util.Arrays;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import h1modules.utilities.utils.Activator;

/**
 * A utility for Eclipse projects and Java projects.
 * @author TNeumann
 */
public class ProjectUtil {
    private static final ConsoleLogger logger = new ConsoleLogger(Activator.getDefault().getLog(), Activator.PLUGIN_ID);

	public static boolean isJavaProject(IProject project) {
		IFile classpathFile = project.getProject().getFile(".classpath");
		return classpathFile.exists();
	}

	public static IJavaProject getJavaProjectForLaunchConfiguration(ILaunchConfiguration launchConfig) {
		try {
			IResource[] mappedResources = launchConfig.getMappedResources();
			if (mappedResources==null || mappedResources.length==0) {
				logger.error("The launch configuration " + launchConfig + " has no mapped resources -> cannot find the project");
				return null;
			}
			logger.debug("mappedResources = " + Arrays.toString(mappedResources));
			// XXX make sure there is exactly one mapped resource?
			IProject project = mappedResources[0].getProject();
			logger.debug("project = " + project);
			IJavaProject javaProject = JavaCore.create(project);
			logger.info("The launch configuration " + launchConfig + " belongs to the java project " + javaProject.getElementName());
			return javaProject;
		} catch (CoreException e) {
			logger.error("Looking for the Java project of launch configuration " + launchConfig + " caused exception " + e, e);
			return null;
		}
	}
}
