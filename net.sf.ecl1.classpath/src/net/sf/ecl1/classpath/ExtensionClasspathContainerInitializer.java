/**
 *
 */
package net.sf.ecl1.classpath;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFolder;
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
import com.google.common.collect.Lists;

import net.sf.ecl1.utilities.general.ConsoleLogger;
import net.sf.ecl1.utilities.hisinone.HisConstants;

/**
 * Classpath Initializer for HISinOne-Extension projects
 *
 * @author keunecke
 */
public class ExtensionClasspathContainerInitializer extends ClasspathContainerInitializer {

    private static final ConsoleLogger logger = new ConsoleLogger(Activator.getDefault().getLog(), Activator.PLUGIN_ID);

    private final Collection<String> extensionsForClassPath = new HashSet<>();

    @Override
    public void initialize(IPath containerPath, IJavaProject javaProject) throws CoreException {
        try {
            if (containerPath != null) {
            	logger.debug("Supplied path: " + containerPath.toOSString());
            }
            updateClasspathContainer(containerPath, javaProject);
        } catch (CoreException e) {
            logger.error(e.getMessage(), e);
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
        	logger.error(e.getMessage(), e);
        }
    }

    /**
     * @param containerPath
     * @param project
     * @throws JavaModelException
     */
    private void updateClasspathContainer(final IPath containerPath, final IJavaProject project) throws JavaModelException {
        initializeExtensionsToAddToClasspath(containerPath);
        Collection<IClasspathEntry> entryList = Collections2.filter(Arrays.asList(createEntries(project)), Predicates.notNull());
        if (!entryList.isEmpty()) {
            IClasspathEntry[] array = Lists.newArrayList(entryList).toArray(new IClasspathEntry[entryList.size()]);
            ExtensionClassPathContainer extensionClassPathContainer = new ExtensionClassPathContainer(containerPath, array);
            IClasspathContainer[] iClasspathContainers = new IClasspathContainer[] { extensionClassPathContainer };
            IJavaProject[] iJavaProjects = new IJavaProject[] { project };
            JavaCore.setClasspathContainer(containerPath, iJavaProjects, iClasspathContainers, new NullProgressMonitor());
        } else {
        	logger.debug("No entries for classpath container '" + containerPath + "' in  '" + project.getElementName() + "'.");
        }
    }

    /**
     * Initialize Extension needed to be added to Classpath
     *
     * @param containerPath
     */
    private void initializeExtensionsToAddToClasspath(IPath containerPath) {
        this.extensionsForClassPath.clear();
        if (containerPath != null && containerPath.segmentCount() == 2) {
            String commaSeparatedExtensionsToIgnore = containerPath.segment(1);
            if (commaSeparatedExtensionsToIgnore != null) {
                List<String> extensionsToIgnore = Splitter.on(",").splitToList(commaSeparatedExtensionsToIgnore);
                ExtensionUtil extensionUtil = new ExtensionUtil(); // avoids searching webapps for each checked project
                for (String extension : extensionsToIgnore) {
                    boolean projectExists = extensionUtil.doesExtensionProjectExist(extension);
                    boolean jarExists = extensionUtil.doesExtensionJarExist(extension);
                    if (projectExists || jarExists) {
                        extensionsForClassPath.add(extension);
                    }
                }
            }
        }
        logger.debug("Extensions for export: " + extensionsForClassPath);
    }

    private IClasspathEntry[] createEntries(IJavaProject javaProject) {
        Map<String, String> extensions = new HashMap<String, String>();

        scanForExtensionJars(javaProject, extensions);
        scanForExtensionProjects(extensions);

        ArrayList<IClasspathEntry> result = new ArrayList<>();

        //uniquely register extensions either as jar or as project
        for (Map.Entry<String, String> extension : extensions.entrySet()) {

            String extensionName = extension.getKey();
            String extensionPath = extension.getValue();

            boolean extensionNeedsToBeExported = extensionNeedsToBeAddedToClasspath(extensionName);
            if (extensionNeedsToBeExported) {
                IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
                IPath workspace = root.getRawLocation();
                if (extensionPath.endsWith(".jar")) {
                    IPath path = javaProject.getPath().append(HisConstants.EXTENSIONS_FOLDER).append(extensionPath);
                    //create a lib entry
                    IPath sourceAttachmentPath = workspace.append(path);
                    IPath sourceAttachmentRootPath = null;
                    IClasspathEntry libraryEntry = JavaCore.newLibraryEntry(path, sourceAttachmentPath, sourceAttachmentRootPath, true);
                    result.add(libraryEntry);
                    logger.info("Creating new container library entry for: " + path.toString() + "\n * exported: " + extensionNeedsToBeExported
                    		+ "\n * sourceAttachmentPath: " + sourceAttachmentPath + "\n * sourceAttachmentRootPath: " + sourceAttachmentRootPath);
                } else {
                    IProject project = root.getProject(extensionPath);
                    if (project.exists()) {
                    	logger.info("Creating new container entry for project: " + project.getName() + " exported: " + extensionNeedsToBeExported);
                        IPath location = project.getLocation();
                        IClasspathEntry newProjectEntry = JavaCore.newProjectEntry(location.makeRelativeTo(workspace).makeAbsolute(), true);
                        result.add(newProjectEntry);
                    } else {
                    	logger.warn("Extension does not exist as project: " + extensionPath);
                    }
                }
            }
        }
        return result.toArray(new IClasspathEntry[result.size()]);
    }

    /**
     * Determine if extension should be added to classpath
     *
     * @param extensionName
     * @return true iff extensionName is contained in container path
     */
    private boolean extensionNeedsToBeAddedToClasspath(String extensionName) {
        boolean extensionIsListed = extensionsForClassPath.contains(extensionName);
        return extensionIsListed;
    }

    /**
     * Scans for extension projects in workspace
     *
     * @param javaProject
     * @param extensions
     */
    private void scanForExtensionProjects(Map<String, String> extensions) {
        //scan workspace for extension projects
        IWorkspaceRoot ws = ResourcesPlugin.getWorkspace().getRoot();
        List<IProject> projects = Arrays.asList(ws.getProjects(0));
        ExtensionUtil extensionUtil = new ExtensionUtil(); // avoids searching webapps for each checked project
        for (IProject project : projects) {
            if (extensionUtil.isExtensionProject(project)) {
                extensions.put(project.getName(), project.getName());
            }
        }
    }

    /**
     * Scan the java project for jar files in the extensions folder
     *
     * @param javaProject
     * @param extensions
     */
    private void scanForExtensionJars(IJavaProject javaProject, Map<String, String> extensions) {
        //scan workspace for extension jars
        IFolder extensionsFolder = javaProject.getProject().getFolder(HisConstants.EXTENSIONS_FOLDER);
        if (extensionsFolder.exists()) {
            //if there is an extensions folder, scan it
            IPath rawLocation = extensionsFolder.getRawLocation();
            List<File> extensionJars = Arrays.asList(rawLocation.toFile().listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name != null && name.endsWith("jar");
                }
            }));
            for (File extensionJar : extensionJars) {
                extensions.put(extensionJar.getName().replace(".jar", ""), extensionJar.getName());
            }
        }
    }

}
