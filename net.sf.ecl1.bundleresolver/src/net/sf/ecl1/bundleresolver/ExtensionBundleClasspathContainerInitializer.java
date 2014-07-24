package net.sf.ecl1.bundleresolver;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.ClasspathContainerInitializer;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

/**
 * Initializes a ExtensionBundleClaspathContainer if it is valid
 *  
 * @author keunecke
 */
public class ExtensionBundleClasspathContainerInitializer extends ClasspathContainerInitializer {

    @Override
    public void initialize(IPath path, IJavaProject project) throws CoreException {
        ExtensionBundleClasspathContainer c = new ExtensionBundleClasspathContainer(path, project);
        if (c.isValid()) {
            c.initialize();
            JavaCore.setClasspathContainer(path, new IJavaProject[] { project }, new IClasspathContainer[] { c }, null);
        }
    }

    @Override
    public boolean canUpdateClasspathContainer(IPath containerPath, IJavaProject project) {
        return true;
    }

    @Override
    public void requestClasspathContainerUpdate(IPath containerPath, IJavaProject project, IClasspathContainer containerSuggestion) throws CoreException {
        JavaCore.setClasspathContainer(containerPath, new IJavaProject[] { project }, new IClasspathContainer[] { containerSuggestion }, null);
    }

}
