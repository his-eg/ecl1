package net.sf.ecl1.utilities.general;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import net.sf.ecl1.utilities.Activator;

/**
 * A utility for Eclipse projects and Java projects.
 * @author TNeumann
 */
public class ProjectUtil {
    private static final ConsoleLogger logger = new ConsoleLogger(Activator.getDefault().getLog(), Activator.PLUGIN_ID, ProjectUtil.class.getSimpleName());

	
    public static boolean isJavaProject(IProject project) {
    	try {
			return project.hasNature(JavaCore.NATURE_ID);
		} catch (CoreException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
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
	
    /**
     * Get all classes declared in the given java project.
     * @param javaProject
     * @return collection of IType
     * @throws JavaModelException
     */
    public static Collection<IType> getClasses(IJavaProject project) throws JavaModelException {
        Collection<IType> result = new ArrayList<IType>();
        IPackageFragment[] fragmentRoots = project.getPackageFragments();
        for (IPackageFragment iPackageFragment : fragmentRoots) {
            if (IPackageFragmentRoot.K_SOURCE == iPackageFragment.getKind()) {
                scanPackage(iPackageFragment, result);
            }
        }
        return result;
    }

    private static void scanPackage(IPackageFragment iPackageFragment, Collection<IType> result) throws JavaModelException {
        ICompilationUnit[] units = iPackageFragment.getCompilationUnits();
        for (ICompilationUnit unit : units) {
            IType[] types = unit.getAllTypes();
            for (IType type : types) {
                result.add(type);
            }
        }
    }
}
