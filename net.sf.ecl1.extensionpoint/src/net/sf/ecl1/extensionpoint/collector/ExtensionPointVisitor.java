package net.sf.ecl1.extensionpoint.collector;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

import net.sf.ecl1.extensionpoint.Constants;
import net.sf.ecl1.extensionpoint.collector.manager.ExtensionPointManager;
import net.sf.ecl1.extensionpoint.collector.model.ExtensionPointInformation;
import net.sf.ecl1.extensionpoint.collector.util.ConsoleLoggingHelper;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

class ExtensionPointVisitor implements IResourceVisitor {

    private final ConsoleLoggingHelper logger;

    private static final String JAVA_FILE_EXTENSION = "java";

    private static final String EXTENSION_ANNOTATION_NAME = "Extension";

    private static final String EXTENSION_POINT_ANNOTATION_NAME = "ExtensionPoint";

    private IJavaProject project;

    private Collection<String> contributors = new HashSet<String>();

    /**
     * Create a new ExtensionPointVisitor
     * 
     * @param project
     */
    public ExtensionPointVisitor(IJavaProject project) {
        this.logger = new ConsoleLoggingHelper(project, Constants.CONSOLE_NAME, Constants.LOGGING_PREFERENCE);
        this.project = project;
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
            if (project.isOnClasspath(resource)) {
                try {
                    resource.refreshLocal(0, null);
                } catch (CoreException e) {
                    e.printStackTrace();
                }
                ICompilationUnit compilationUnit = JavaCore.createCompilationUnitFrom(resource);
                for (IType type : compilationUnit.getTypes()) {
                    IAnnotation[] annotations = type.getAnnotations();
                    for (IAnnotation extensionAnnotation : annotations) {
                        if (extensionAnnotation != null) {
                            if (EXTENSION_ANNOTATION_NAME.equals(extensionAnnotation.getElementName())) {
                                if (extensionAnnotation.exists()) {
                                    this.contributors.add(type.getFullyQualifiedName());
                                    logger.logToConsole("Found contribution: " + type.getFullyQualifiedName());
                                }
                            }
                            if (EXTENSION_POINT_ANNOTATION_NAME.equals(extensionAnnotation.getElementName())) {
                                if (extensionAnnotation != null && extensionAnnotation.exists()) {
                                    ExtensionPointInformation epi = ExtensionPointInformation.create(extensionAnnotation, type);
                                    logger.logToConsole("Found Extension Point: " + epi);
                                    ExtensionPointManager.get().addExtensions(type, Arrays.asList(epi));
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * @return the contributors
     */
    public Collection<String> getContributors() {
        return contributors;
    }
}