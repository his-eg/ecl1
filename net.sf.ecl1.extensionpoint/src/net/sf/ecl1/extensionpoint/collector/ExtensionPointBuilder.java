package net.sf.ecl1.extensionpoint.collector;

import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.JavaCore;

public class ExtensionPointBuilder extends IncrementalProjectBuilder {

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

	protected void fullBuild(final IProgressMonitor monitor)
			throws CoreException {
		try {
            getProject().accept(visitor);
		} catch (CoreException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
		}
	}

	protected void incrementalBuild(IResourceDelta delta,
			IProgressMonitor monitor) throws CoreException {
        delta.accept(visitor);
	}
}
