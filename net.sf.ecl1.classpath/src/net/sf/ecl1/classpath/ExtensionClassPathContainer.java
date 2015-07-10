package net.sf.ecl1.classpath;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
/**
 * Classpath Container for working with HISinOne-Extensions
 *
 * @author markus
 */
class ExtensionClassPathContainer implements
IClasspathContainer {

    private final IClasspathEntry[] entries;

    private final IPath path;

    /**
     * @param javaProject
     * @param containerPath
     */
    public ExtensionClassPathContainer(IPath containerPath, IClasspathEntry[] entries) {
        this.entries = entries;
        this.path = containerPath;
    }


    @Override
    public IPath getPath() {
        return path;
    }

    @Override
    public int getKind() {
        return IClasspathContainer.K_APPLICATION;
    }

    @Override
    public String getDescription() {
        return "ecl1 Extensions Classpath Container";
    }

    @Override
    public IClasspathEntry[] getClasspathEntries() {
        return entries;
    }

}