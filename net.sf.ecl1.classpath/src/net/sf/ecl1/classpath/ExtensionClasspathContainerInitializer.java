package net.sf.ecl1.classpath;

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

import net.sf.ecl1.utilities.general.ConsoleLogger;
import net.sf.ecl1.utilities.hisinone.ExtensionUtil;
import net.sf.ecl1.utilities.hisinone.HisConstants;

/**
 * Initializes the ecl1 classpath container with HISinOne-Extension projects
 *
 * @author keunecke
 */
public class ExtensionClasspathContainerInitializer extends ClasspathContainerInitializer {

	private static final ConsoleLogger logger = new ConsoleLogger(Activator.getDefault().getLog(), Activator.PLUGIN_ID, ExtensionClasspathContainerInitializer.class.getSimpleName());

	private static final ExtensionUtil EXTENSION_UTIL = ExtensionUtil.getInstance();
	
	boolean listenerRegistered = false;

	@Override
	public void initialize(IPath containerPath, IJavaProject javaProject) throws CoreException {
		try {
			if (containerPath != null) {
				logger.debug("Supplied path: " + containerPath.toOSString());
			}
			registerListener(containerPath,javaProject);
			updateClasspathContainer(containerPath, javaProject);
		} catch (CoreException e) {
			logger.error2(e.getMessage(), e);
			throw e;
		}
	}


	
	private void registerListener(IPath containerPath, IJavaProject javaProject) {
		/*
		 * Add listener to workspace only once. This is necessary, because the initialize
		 * method can be called by eclipse multiple times. 
		 * 
		 */
		if(listenerRegistered == false) {
			ExtensionClasspathContainerListener listener = new ExtensionClasspathContainerListener(containerPath, javaProject);
			//We only want to be informed after a workspace change is completed
			ResourcesPlugin.getWorkspace().addResourceChangeListener(listener, IResourceChangeEvent.POST_CHANGE);
			listenerRegistered = true;
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
	 * @deprecated use {@link #getExtensionsInClasspathContainer(IPath)} instead. 
	 * This method was deprecated, because the check for the existence of extensions either as a jars
	 * or as projects is costly and might even prevent eclipse from compiling webapps. Why?
	 * <br>
	 * All extensions within ecl1 are needed to compile webapps. This method checks, if
	 * all extensions from the container are present as either jars or projects. However, if 
	 * a project cannot be found as either a jar or as a project, this method just assumes that
	 * the extensions is not needed to compile webapps... ¯\_(ツ)_/¯
	 *
	 * @param containerPath
	 * @return set of extension that need to be added to the classpath container
	 */
	@Deprecated
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
	 * 
	 * @param javaProject MUST be webapps or else this will fail. 
	 * @param extensionsForClasspathContainer names of all the extensions that need to be added to the classpath container
	 * @return resolved classpath entries. Either a *.jar or a link to a checked out project within the workspace. 
	 */
	private static ArrayList<IClasspathEntry> createClasspathContainerEntries(IJavaProject javaProject, Set<String> extensionsForClasspathContainer) {
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
