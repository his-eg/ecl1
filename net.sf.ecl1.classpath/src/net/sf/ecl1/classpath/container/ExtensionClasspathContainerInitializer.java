package net.sf.ecl1.classpath.container;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceChangeEvent;
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

import net.sf.ecl1.classpath.Activator;
import net.sf.ecl1.utilities.general.ConsoleLogger;
import net.sf.ecl1.utilities.hisinone.ExtensionUtil;
import net.sf.ecl1.utilities.hisinone.HisConstants;
import net.sf.ecl1.utilities.hisinone.WebappsUtil;

/**
 * Initializes the ecl1 classpath container with HISinOne-Extension projects
 *
 * @author keunecke
 */
public class ExtensionClasspathContainerInitializer extends ClasspathContainerInitializer {

	private static final ConsoleLogger logger = new ConsoleLogger(Activator.getDefault().getLog(), Activator.PLUGIN_ID, ExtensionClasspathContainerInitializer.class.getSimpleName());

	private static final ExtensionUtil EXTENSION_UTIL = ExtensionUtil.getInstance();

	@Override
	public void initialize(IPath containerPath, IJavaProject javaProject) throws CoreException {
		try {
			if (containerPath != null) {
				logger.debug("The following project contains an ecl1 classpath container: " + javaProject.toString() +
						"\nThis is the content of this container: " + containerPath.toOSString() + 
						"\nThe ecl1 plugin will now attempt to initialize the container.");
			}
			
			ProjectsWithContainer.getInstance().addProject(javaProject.getProject());
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
	public static void updateClasspathContainer(final IPath containerPath, final IJavaProject project) throws JavaModelException {
		// search for webapps each time we want to update the classpath container
		EXTENSION_UTIL.updateWebappsProjectReference();

		// now search extensions etc.
		Set<String> extensionsForClasspathContainerSet = getExtensionsInClasspathContainer(containerPath);
		
		Collection<IClasspathEntry> classpathContainerEntryList = createClasspathContainerEntries(extensionsForClasspathContainerSet);
		if (classpathContainerEntryList != null && !classpathContainerEntryList.isEmpty()) {
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
	 * Parse the ecl1 container and return the names of all extensions within the container
	 * 
	 * @param containerPath
	 * @return
	 */
	public static Set<String> getExtensionsInClasspathContainer(IPath containerPath){
		if (containerPath != null && containerPath.segmentCount() == 2) {
			String extensionsForClasspathContainerStr = containerPath.segment(1);
			if (extensionsForClasspathContainerStr != null) {
				return new HashSet<String>(Splitter.on(",").splitToList(extensionsForClasspathContainerStr));
			}
		}
		return new HashSet<String>();
	}


	/**
	 * @param extensionsForClasspathContainer names of all the extensions that need to be added to the classpath container
	 * @return resolved classpath entries. Either a *.jar or a link to a checked out project within the workspace. 
	 */
	private static ArrayList<IClasspathEntry> createClasspathContainerEntries(Set<String> extensionsForClasspathContainer) {
		IProject p = WebappsUtil.findWebappsProject();
		if(p == null) {
			logger.error2("Couldn't find webapps projects in workspace!");
			return null;
		}
		IJavaProject webapps = JavaCore.create(p);
		
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
					IPath fullExtensionPath = webapps.getPath().append(HisConstants.EXTENSIONS_FOLDER).append(simpleExtensionPath);
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
