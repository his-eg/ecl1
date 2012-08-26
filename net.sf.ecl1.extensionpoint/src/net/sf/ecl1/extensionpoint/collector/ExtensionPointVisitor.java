package net.sf.ecl1.extensionpoint.collector;

import java.util.Arrays;
import java.util.Collection;

import net.sf.ecl1.extensionpoint.collector.model.ExtensionPointInformation;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

class ExtensionPointVisitor implements IResourceVisitor, IResourceDeltaVisitor {

	private ExtensionPointCollector extensionPointCollector;

    public boolean visit(IResource resource) {
		//return true to continue visiting children.
		return true;
	}

    public boolean visit(IResourceDelta delta) throws CoreException {
        IResource resource = delta.getResource();
        switch (delta.getKind()) {
        case IResourceDelta.ADDED:
            // handle added resource
            handleAddedResource(resource);
            break;
        case IResourceDelta.REMOVED:
            // handle removed resource
            // remove contributions
            // remove points
            break;
        case IResourceDelta.CHANGED:
            // handle changed resource
            break;
        }
        //return true to continue visiting children.
        return true;
    }

    private void handleAddedResource(IResource resource) throws JavaModelException {
        switch (resource.getType()) {
        case IResource.FILE:
            handleAddedFile((IFile) resource);
            break;
        default:
            //do nothing
            break;
        }
    }

    private void handleAddedFile(IFile resource) throws JavaModelException {
        // handle only java files
        if ("java".equals(resource.getFileExtension())) {
            ICompilationUnit compilationUnit = JavaCore.createCompilationUnitFrom(resource);
            extensionPointCollector = new ExtensionPointCollector(JavaCore.create(resource.getProject()));
            Collection<ExtensionPointInformation> collection = extensionPointCollector.extractExtensionInformation(Arrays.asList(compilationUnit.getAllTypes()));
            ExtensionPointManager.addExtensions(resource.getProject().getName(), collection);
        }
    }
}