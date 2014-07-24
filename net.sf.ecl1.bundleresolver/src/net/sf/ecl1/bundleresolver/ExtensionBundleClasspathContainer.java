/**
 * 
 */
package net.sf.ecl1.bundleresolver;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;

/**
 * Classpath container responsible for adding extension bundles correctly to the classpath
 * @author keunecke
 */
public class ExtensionBundleClasspathContainer implements IClasspathContainer {
    
    /** identifying path element */
    public final static Path ID = new Path(Constants.CONTAINER_ID);

    private final IPath path;

    private final IJavaProject project;

    /**
     * Create a new container
     * @param path the path within the given project including the container id
     * @param project the project this container is defined for
     */
    public ExtensionBundleClasspathContainer(IPath path, IJavaProject project) {
        this.path = path;
        this.project = project;
    }

    /* (non-Javadoc)
     * @see org.eclipse.jdt.core.IClasspathContainer#getClasspathEntries()
     */
    @Override
    public IClasspathEntry[] getClasspathEntries() {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.eclipse.jdt.core.IClasspathContainer#getDescription()
     */
    @Override
    public String getDescription() {
        return "HISinOne-Extension-Bundles-Container";
    }

    /* (non-Javadoc)
     * @see org.eclipse.jdt.core.IClasspathContainer#getKind()
     */
    @Override
    public int getKind() {
        return IClasspathContainer.K_APPLICATION;
    }

    /* (non-Javadoc)
     * @see org.eclipse.jdt.core.IClasspathContainer#getPath()
     */
    @Override
    public IPath getPath() {
        return null;
    }

    /**
     * @return true iff this container is valid and applicable
     */
    public boolean isValid() {
        return false;
    }

    /**
     * Extract the files from the found bundles into a subdirectory
     */
    public void initialize() {
        // TODO implement extraction to subfolders
    }

}
