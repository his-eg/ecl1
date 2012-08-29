package net.sf.ecl1.extensionpoint.collector;

import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.JavaCore;

/**
 * Project Builder to collect and process extension point and contribution information
 *  
 * @author keunecke
 */
public class ExtensionPointBuilder extends IncrementalProjectBuilder {

    /** Builder ID constant */
    public static final String BUILDER_ID = "net.sf.ecl1.extensionpoint.extensionPointBuilder";

    private ExtensionPointVisitor visitor;

    protected IProject[] build(int kind, @SuppressWarnings("rawtypes")
    Map args, IProgressMonitor monitor)
			throws CoreException {
        visitor = new ExtensionPointVisitor(JavaCore.create(getProject()));
		if (kind == FULL_BUILD) {
			fullBuild(monitor);
		} else {
			IResourceDelta delta = getDelta(getProject());
			if (delta == null) {
				fullBuild(monitor);
			} else {
				incrementalBuild(delta, monitor);
			}
		}
		return null;
	}

    /**
     * Perform a full build
     * 
     * @param monitor
     * @throws CoreException
     */
	protected void fullBuild(final IProgressMonitor monitor)
			throws CoreException {
		try {
            getProject().accept(visitor);
		} catch (CoreException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
		}
	}

    /**
     * Perform an incremental build
     * 
     * @param delta
     * @param monitor
     * @throws CoreException
     */
	protected void incrementalBuild(IResourceDelta delta,
			IProgressMonitor monitor) throws CoreException {
        delta.accept(visitor);
	}
}
