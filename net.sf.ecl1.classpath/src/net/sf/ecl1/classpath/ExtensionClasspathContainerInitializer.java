package net.sf.ecl1.classpath;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.ClasspathContainerInitializer;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import com.google.common.base.Predicates;
import com.google.common.base.Splitter;
import com.google.common.collect.Collections2;

import net.sf.ecl1.utilities.general.ConsoleLogger;
import net.sf.ecl1.utilities.hisinone.HisConstants;

/**
 * Initializes the ecl1 classpath container with HISinOne-Extension projects
 *
 * @author keunecke
 */
public class ExtensionClasspathContainerInitializer extends ClasspathContainerInitializer {

    private static final ConsoleLogger logger = new ConsoleLogger(Activator.getDefault().getLog(), Activator.PLUGIN_ID);
    
    private static final ExtensionUtil EXTENSION_UTIL = ExtensionUtil.getInstance();
    
    @Override
    public void initialize(IPath containerPath, IJavaProject javaProject) throws CoreException {
        try {
            if (containerPath != null) {
            	logger.debug("Supplied path: " + containerPath.toOSString());
            }
            updateClasspathContainer(containerPath, javaProject);
        } catch (CoreException e) {
            logger.error2(e.getMessage(), e);
            throw e;
        }
    }

    /* (non-Javadoc)
     * @see org.eclipse.jdt.core.ClasspathContainerInitializer#canUpdateClasspathContainer(org.eclipse.core.runtime.IPath, org.eclipse.jdt.core.IJavaProject)
     */
    @Override
    public boolean canUpdateClasspathContainer(IPath containerPath, IJavaProject project) {
        return true;
    }

    /* (non-Javadoc)
     * @see org.eclipse.jdt.core.ClasspathContainerInitializer#requestClasspathContainerUpdate(org.eclipse.core.runtime.IPath, org.eclipse.jdt.core.IJavaProject, org.eclipse.jdt.core.IClasspathContainer)
     */
    @Override
    public void requestClasspathContainerUpdate(final IPath containerPath, final IJavaProject project, IClasspathContainer containerSuggestion) {
        try {
            updateClasspathContainer(containerPath, project);
        } catch (JavaModelException e) {
        	logger.error2(e.getMessage(), e);
        }
    }

    /**
     * @param containerPath
     * @param project
     * @throws JavaModelException
     */
    private void updateClasspathContainer(final IPath containerPath, final IJavaProject project) throws JavaModelException {
    	// search for webapps each time we want to update the classpath container
    	EXTENSION_UTIL.findWebappsProject();
    	
    	// now search extensions etc.
    	HashSet<String> extensionsForClasspathContainerSet = getExtensionsForClasspathContainerAsSet(containerPath);
        Collection<IClasspathEntry> classpathContainerEntryList = Collections2.filter(createClasspathContainerEntries(project, extensionsForClasspathContainerSet), Predicates.notNull());
        if (!classpathContainerEntryList.isEmpty()) {
            IClasspathEntry[] classpathContainerEntryArray = classpathContainerEntryList.toArray(new IClasspathEntry[classpathContainerEntryList.size()]);
            ExtensionClassPathContainer extensionClassPathContainer = new ExtensionClassPathContainer(containerPath, classpathContainerEntryArray);
            IClasspathContainer[] iClasspathContainers = new IClasspathContainer[] { extensionClassPathContainer };
            IJavaProject[] iJavaProjects = new IJavaProject[] { project };
            JavaCore.setClasspathContainer(containerPath, iJavaProjects, iClasspathContainers, new NullProgressMonitor());
        } else {
        	logger.debug("No entries for classpath container '" + containerPath + "' in  '" + project.getElementName() + "'.");
        }
    }

    /**
     * Determine the extensions that need to be added to the ecl1 classpath container.
     *
     * @param containerPath
     * @return set of extension that need to be added to the classpath container
     */
    private HashSet<String> getExtensionsForClasspathContainerAsSet(IPath containerPath) {
    	HashSet<String> extensionsForClasspathContainer = new HashSet<>();
        if (containerPath != null && containerPath.segmentCount() == 2) {
            String extensionsForClasspathContainerStr = containerPath.segment(1);
            if (extensionsForClasspathContainerStr != null) {
                List<String> extensionsForClasspathContainerList = Splitter.on(",").splitToList(extensionsForClasspathContainerStr);
                for (String extension : extensionsForClasspathContainerList) {
                    boolean projectExists = EXTENSION_UTIL.doesExtensionProjectExist(extension);
                    boolean jarExists = EXTENSION_UTIL.doesExtensionJarExist(extension);
                    if (projectExists || jarExists) {
                        extensionsForClasspathContainer.add(extension);
                    }
                }
            }
        }
        logger.info("Extensions to add to the ecl1 classpath container: " + extensionsForClasspathContainer);
        return extensionsForClasspathContainer;
    }

    private ArrayList<IClasspathEntry> createClasspathContainerEntries(IJavaProject javaProject, HashSet<String> extensionsForClasspathContainer) {
        ArrayList<IClasspathEntry> result = new ArrayList<>();
        
        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
        IPath workspace = root.getRawLocation();

        Map<String, String> extensions = EXTENSION_UTIL.findAllExtensions();
        for (Map.Entry<String, String> extension : extensions.entrySet()) {
            String extensionName = extension.getKey();
            String simpleExtensionPath = extension.getValue();

            if (extensionsForClasspathContainer.contains(extensionName)) {
            	// We want the extension to be added to the classpath container.
                // Uniquely register extensions either as jar or as project.
                if (simpleExtensionPath.endsWith(".jar")) {
                    IPath fullExtensionPath = javaProject.getPath().append(HisConstants.EXTENSIONS_FOLDER).append(simpleExtensionPath);
                    // create a lib entry
                    IPath sourceAttachmentPath = workspace.append(fullExtensionPath);
                    IPath sourceAttachmentRootPath = null;
                    IClasspathEntry libraryEntry = JavaCore.newLibraryEntry(fullExtensionPath, sourceAttachmentPath, sourceAttachmentRootPath, true);
                    result.add(libraryEntry);
                    logger.debug("Creating new container entry for library: " + fullExtensionPath.toString()
                    		+ "\n * sourceAttachmentPath: " + sourceAttachmentPath + "\n * sourceAttachmentRootPath: " + sourceAttachmentRootPath);
                } else {
                    IProject extensionProject = root.getProject(simpleExtensionPath);
                    if (extensionProject.exists()) {
                    	IPath fullExtensionPath = extensionProject.getFullPath();
                    	logger.debug("Creating new container entry for project: " + fullExtensionPath);
                        IClasspathEntry newProjectEntry = JavaCore.newProjectEntry(fullExtensionPath, true);
                        result.add(newProjectEntry);
                    } else {
                    	logger.warn("Extension does not exist as project: " + simpleExtensionPath);
                    }
                }
            }
        }
        return result;
    }
}
