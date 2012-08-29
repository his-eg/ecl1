package net.sf.ecl1.extensionpoint.collector;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Properties;

import net.sf.ecl1.extensionpoint.collector.manager.ExtensionPointManager;
import net.sf.ecl1.extensionpoint.collector.model.ExtensionPointInformation;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import com.google.common.base.Splitter;

class ExtensionPointVisitor implements IResourceVisitor, IResourceDeltaVisitor {

    private static final String JAVA_FILE_EXTENSION = "java";

    private static final String EXTENSION_ANNOTATION_NAME = "Extension";

    private static final String EXTENSION_POINT_ANNOTATION_NAME = "ExtensionPoint";

    private Collection<String> contributors = new HashSet<String>();

    private IJavaProject project;

    public ExtensionPointVisitor(IJavaProject project) {
        IFile props = project.getProject().getFile("extension.ant.properties");
        this.project = project;
        if (props != null && props.exists()) {
            try {
                Properties p = new Properties();
                p.load(props.getContents());
                String propString = p.getProperty("extension.extended-points");
                Iterable<String> contribs = Splitter.on(",").split(propString);
                for (String contrib : contribs) {
                    this.contributors.add(contrib);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (CoreException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean visit(IResource resource) {
		//return true to continue visiting children.
        try {
            handleResource(resource);
        } catch (JavaModelException e) {
            e.printStackTrace();
        }
		return true;
	}

    public boolean visit(IResourceDelta delta) throws CoreException {
        IResource resource = delta.getResource();
        switch (delta.getKind()) {
        case IResourceDelta.ADDED:
            // handle added resource
            handleResource(resource);
            break;
        case IResourceDelta.REMOVED:
            // handle removed resource
            // remove contributions
            // remove points
            handelDeletedResource(resource);
            break;
        case IResourceDelta.CHANGED:
            // handle changed resource
            handleResource(resource);
            break;
        }
        //return true to continue visiting children.
        return true;
    }

    private void handelDeletedResource(IResource resource) throws JavaModelException {
        switch (resource.getType()) {
        case IResource.FILE:
            handleDeletedFile((IFile) resource);
            break;
        default:
            //do nothing
            break;
        }
    }

    private void handleDeletedFile(IFile resource) throws JavaModelException {
        // handle only java files
        if (JAVA_FILE_EXTENSION.equals(resource.getFileExtension())) {
            ICompilationUnit compilationUnit = JavaCore.createCompilationUnitFrom(resource);
            for (IType type : compilationUnit.getTypes()) {
                IAnnotation extensionAnnotation = type.getAnnotation(EXTENSION_ANNOTATION_NAME);
                if (extensionAnnotation != null) {
                    //TODO
                }
                IAnnotation extensionPointAnnotation = type.getAnnotation(EXTENSION_POINT_ANNOTATION_NAME);
                if (extensionPointAnnotation != null) {
                    ExtensionPointManager.removeExtensions(this.project.getElementName(), Arrays.asList(ExtensionPointInformation.create(extensionPointAnnotation, type)));
                }
            }
        }
    }

    private void handleResource(IResource resource) throws JavaModelException {
        switch (resource.getType()) {
        case IResource.FILE:
            handleFile((IFile) resource);
            break;
        default:
            //do nothing
            break;
        }
    }

    private void handleFile(IFile resource) throws JavaModelException {
        // handle only java files
        if (JAVA_FILE_EXTENSION.equals(resource.getFileExtension())) {
            ICompilationUnit compilationUnit = JavaCore.createCompilationUnitFrom(resource);
            for (IType type : compilationUnit.getTypes()) {
                IAnnotation extensionAnnotation = type.getAnnotation(EXTENSION_ANNOTATION_NAME);
                if (extensionAnnotation != null) {
                    //TODO
                }
                IAnnotation extensionPointAnnotation = type.getAnnotation(EXTENSION_POINT_ANNOTATION_NAME);
                if (extensionPointAnnotation != null && extensionPointAnnotation.exists()) {
                    ExtensionPointManager.addExtensions(this.project.getElementName(), Arrays.asList(ExtensionPointInformation.create(extensionPointAnnotation, type)));
                }
            }
        }
    }
}