/**
 * 
 */
package net.sf.ecl1.classpath;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.ClasspathContainerInitializer;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

/**
 * Classpath Initializer for HISinOne-Extension projects
 * 
 * @author keunecke
 */
public class ExtensionClasspathContainerInitializer extends
		ClasspathContainerInitializer {
	
	@Override
	public void initialize(IPath containerPath, IJavaProject javaProject) throws CoreException {
		ExtensionClassPathContainer container = new ExtensionClassPathContainer(javaProject);
		JavaCore.setClasspathContainer(containerPath, new IJavaProject[]{javaProject}, 
													  new IClasspathContainer[]{container}, null);
	}
	
	/* (non-Javadoc)
     * @see org.eclipse.jdt.core.ClasspathContainerInitializer#canUpdateClasspathContainer(org.eclipse.core.runtime.IPath, org.eclipse.jdt.core.IJavaProject)
     */
    @Override
    public boolean canUpdateClasspathContainer(IPath containerPath, 
            IJavaProject project) {
        return true;
    }

    /* (non-Javadoc)
     * @see org.eclipse.jdt.core.ClasspathContainerInitializer#requestClasspathContainerUpdate(org.eclipse.core.runtime.IPath, org.eclipse.jdt.core.IJavaProject, org.eclipse.jdt.core.IClasspathContainer)
     */
    @Override
    public void requestClasspathContainerUpdate(IPath containerPath, IJavaProject project, IClasspathContainer containerSuggestion) throws CoreException {
        JavaCore.setClasspathContainer(containerPath, new IJavaProject[] { project },   new IClasspathContainer[] { containerSuggestion }, null);
    }

}
