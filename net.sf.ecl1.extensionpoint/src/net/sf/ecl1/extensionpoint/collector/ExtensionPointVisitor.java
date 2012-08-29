package net.sf.ecl1.extensionpoint.collector;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Properties;

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

import com.google.common.base.Splitter;

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
        this.logger = new ConsoleLoggingHelper(project, Constants.CONSOLE_NAME);
        this.project = project;
        IFile props = this.project.getProject().getFile("extension.ant.properties");
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
            logger.logToConsole("Java Resource: " + resource.getName());
            if (project.isOnClasspath(resource)) {
                ICompilationUnit compilationUnit = JavaCore.createCompilationUnitFrom(resource);
                for (IType type : compilationUnit.getTypes()) {
                    logger.logToConsole("Type: " + type.getElementName());
                    ExtensionPointManager.get().removeExtensions(type);
                    IAnnotation extensionAnnotation = type.getAnnotation(EXTENSION_ANNOTATION_NAME);
                    if (extensionAnnotation != null) {
                        //TODO manage contributions
                    }
                    IAnnotation extensionPointAnnotation = type.getAnnotation(EXTENSION_POINT_ANNOTATION_NAME);
                    if (extensionPointAnnotation != null && extensionPointAnnotation.exists()) {
                        ExtensionPointInformation epi = ExtensionPointInformation.create(extensionPointAnnotation, type);
                        logger.logToConsole("Found Extension Point: " + epi);
                        ExtensionPointManager.get().addExtensions(type, Arrays.asList(epi));
                    }
                }
            }
        }
    }
}