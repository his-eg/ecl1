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

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.ClasspathContainerInitializer;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import com.google.common.base.Splitter;

/**
 * Classpath Initializer for HISinOne-Extension projects
 *
 * @author keunecke
 */
public class ExtensionClasspathContainerInitializer extends
ClasspathContainerInitializer {

    private final Collection<String> extensionsForClassPath = new HashSet<>();

    public static final String EXTENSIONS_FOLDER = "qisserver/WEB-INF/extensions/";

    @Override
    public void initialize(IPath containerPath, IJavaProject javaProject) throws CoreException {
        if(containerPath != null) {
            System.out.println("Supplied path: " + containerPath.toOSString());
        }
        initializeExtensionsToAddToClasspath(containerPath);
        ExtensionClassPathContainer container = new ExtensionClassPathContainer(containerPath, createEntries(javaProject));
        JavaCore.setClasspathContainer(containerPath, new IJavaProject[] { javaProject }, new IClasspathContainer[] { container }, new NullProgressMonitor());
    }

    /**
     * Initialize Extension needed to be added to Classpath
     *
     * @param containerPath
     */
    private void initializeExtensionsToAddToClasspath(IPath containerPath) {
        if (containerPath != null && containerPath.segmentCount() == 2) {
            String commaSeparatedExtensionsToIgnore = containerPath.segment(1);
            if (commaSeparatedExtensionsToIgnore != null) {
                List<String> extensionsToIgnore = Splitter.on(",").splitToList(commaSeparatedExtensionsToIgnore);
                this.extensionsForClassPath.addAll(extensionsToIgnore);
            }
        }
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
                if (extensionPath.endsWith(".jar")) {
                    IPath path = javaProject.getPath().append(EXTENSIONS_FOLDER).append(extensionPath);
                    //create a lib entry
                    IPath workspace = ResourcesPlugin.getWorkspace().getRoot().getRawLocation();
                    IPath sourceAttachmentPath = workspace.append(path);
                    IPath sourceAttachmentRootPath = null;
                    IClasspathEntry libraryEntry = JavaCore.newLibraryEntry(path, sourceAttachmentPath, sourceAttachmentRootPath, true);
                    result.add(libraryEntry);
                    System.out.println("Creating new container library entry for: " + path.toString() + "\n * exported: " + extensionNeedsToBeExported
                                       + "\n * sourceAttachmentPath: " + sourceAttachmentPath + "\n * sourceAttachmentRootPath: " + sourceAttachmentRootPath);
                } else {
                    IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(extensionPath);
                    System.out.println("Creating new container entry for project: " + project.getName() + " exported: " + extensionNeedsToBeExported);
                    result.add(JavaCore.newProjectEntry(project.getLocation(), true));
                }
            }
        }
        return result.toArray(new IClasspathEntry[1]);
    }

    private boolean extensionNeedsToBeAddedToClasspath(String extensionName) {
        boolean configuredExtensionsEmpty = extensionsForClassPath.isEmpty();
        boolean extensionIsListed = extensionsForClassPath.contains(extensionName);
        return configuredExtensionsEmpty || extensionIsListed;
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
        for (IProject project : projects) {
            if (isExtensionProject(project)) {
                extensions.put(project.getName(), project.getName());
            }
        }
    }

    /**
     * Check if a project is an extension project
     *
     * @param project
     * @return
     */
    private boolean isExtensionProject(IProject project) {
        IFile file = project.getFile("extension.ant.properties");
        return file.exists();
    }

    /**
     * Scan the java project for jar files in the extensions folder
     *
     * @param javaProject
     * @param extensions
     */
    private void scanForExtensionJars(IJavaProject javaProject, Map<String, String> extensions) {
        //scan workspace for extension jars
        IFolder extensionsFolder = javaProject.getProject().getFolder(EXTENSIONS_FOLDER);
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

    /* (non-Javadoc)
     * @see org.eclipse.jdt.core.ClasspathContainerInitializer#canUpdateClasspathContainer(org.eclipse.core.runtime.IPath, org.eclipse.jdt.core.IJavaProject)
     */
    @Override
    public boolean canUpdateClasspathContainer(IPath containerPath,
                                               IJavaProject project) {
        return true;
    }

    /* (non-Javadoc)
     * @see org.eclipse.jdt.core.ClasspathContainerInitializer#requestClasspathContainerUpdate(org.eclipse.core.runtime.IPath, org.eclipse.jdt.core.IJavaProject, org.eclipse.jdt.core.IClasspathContainer)
     */
    @Override
    public void requestClasspathContainerUpdate(final IPath containerPath, final IJavaProject project, IClasspathContainer containerSuggestion) {
        new Job("ecl1-classpath-container-update") {
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                try {
                    IClasspathEntry[] entries = createEntries(project);
                    ExtensionClassPathContainer extensionClassPathContainer = new ExtensionClassPathContainer(containerPath, entries);
                    IClasspathContainer[] iClasspathContainers = new IClasspathContainer[] { extensionClassPathContainer };
                    IJavaProject[] iJavaProjects = new IJavaProject[] { project };
                    JavaCore.setClasspathContainer(containerPath, iJavaProjects, iClasspathContainers, new NullProgressMonitor());
                } catch (JavaModelException e) {
                    System.out.println(e.getMessage());
                    return Status.CANCEL_STATUS;
                }
                return Status.OK_STATUS;
            }

        }.schedule();
        ;
    }

}
