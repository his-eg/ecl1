package net.sf.ecl1.utilities.general;

import java.util.Arrays;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import net.sf.ecl1.utilities.Activator;
import net.sf.ecl1.utilities.logging.ICommonLogger;
import net.sf.ecl1.utilities.logging.LoggerFactory;

/**
 * A utility for Eclipse projects and Java projects.
 * @author TNeumann
 */
public class ProjectUtil {

    private static final ICommonLogger logger = LoggerFactory.getLogger(ProjectUtil.class.getSimpleName(), Activator.PLUGIN_ID, Activator.getDefault());
	
    public static boolean isJavaProject(IProject project) {
    	try {
			return project.hasNature(JavaCore.NATURE_ID);
		} catch (CoreException e) {
            logger.error("Error checking Java nature for project: " + project.getName() + "\n" + e.getMessage(), e);
		}
		return false;
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
