package net.sf.ecl1.classpath;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IncrementalProjectBuilder;
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


	/**
	 * Registers a listener that updates the ecll classpath container 
	 * in case a project referenced in the ecl1 classpath container was added or deleted 
	 */
	private void registerListener(IPath containerPath, IJavaProject javaProject) {

		/**
		 * This listener requests an update of the ecl1 classpath container, 
		 * if a project was added or deleted from the workspace that 
		 * matches a project within the ecl1 classpath container. 
		 * 
		 * Based on this excellent article: https://www.eclipse.org/articles/Article-Resource-deltas/resource-deltas.html
		 */
		IResourceChangeListener listener = new IResourceChangeListener() {

			Set<String> extensionsInClasspathContainer = new HashSet<String>();

			private boolean isResourceMemberOfClasspathContainer(String resourceName) {
				for(String extensionName : extensionsInClasspathContainer) {
					if(extensionName.equals(resourceName)) {
						return true;
					}
				}
				return false;
			}


			@Override
			public void resourceChanged(IResourceChangeEvent event) {

				extensionsInClasspathContainer = getExtensionsInClasspathContainer(containerPath);     

				IResourceDelta rootDelta = event.getDelta();

				//TODO: Comment before ecl1 release
				//Just for debugging purposes
				System.out.println("Full path of root Delta: " + rootDelta.getFullPath());
				System.out.println("Printing all children of delta:");
				for(IResourceDelta child : rootDelta.getAffectedChildren()) {
					System.out.println("Path of child: " + child.getFullPath());
					System.out.println("Kind of child: " + child.getKind());
					System.out.println("Flags of child: " + child.getFlags());
				}

				boolean classpathContainerUpdateNecessary = false;

				
				
				/*
				 * #############################################################
				 * On which kind of deltas with which flags should we listen to?
				 * #############################################################
				 * 
				 * -----------------------------------------
				 * Deletion of a project from the workspace: 
				 * -----------------------------------------
				 * Kind --> IResourceDelta.REMOVED
				 * Flags --> 0
				 * 
				 * ---------------------------------------
				 * Importing a project into the workspace:
				 * ---------------------------------------
				 * The listener is triggered several times. The first trigger is: 
				 * Kind -->  IResourceDelta.ADDED
				 * Flags --> 0
				 * 
				 * Reacting to this trigger would be a mistake, though, because 
				 * the project has not been opened yet and the project
				 * would not be recognized as a valid java project. 
				 * Therefore we listen to the following delta:
				 * Kind --> IResourceDelta.CHANGED
				 * Flags --> IResourceDelta.OPEN
				 * 
				 * -----------------------------------
				 * Closing a project in the workspace:
				 * -----------------------------------
				 * Kind --> IResourceDelta.CHANGED
				 * Flags --> IResourceDelta.SYNC | IResourceDelta.OPEN
				 * (IResourceDelta.OPEN-Flag is set when opening _or_ closing a project) 
				 * 
				 * -----------------------------------
				 * Opening a project in the workspace: 
				 * -----------------------------------
				 * Kind --> IResourceDelta.CHANGED
				 * Flags --> IResourceDelta.OPEN
				 * 
				 * -----------
				 * Conclusion:
				 * -----------
				 * We need to get active in the following two cases:
				 * 1. 
				 * Kind --> IResourceDelta.REMOVED
				 * Flags --> 0                    
				 * 2.
				 * Kind --> IResourceDelta.CHANGED 
				 * Flags --> IResourceDelta.OPEN   
				 * 
				 */
				
				//Only check direct children (aka projects) of rootDelta 
				for(IResourceDelta delta : rootDelta.getAffectedChildren( (IResourceDelta.REMOVED | IResourceDelta.CHANGED) )) {
					/*
					 * If the project changes which contains the ecl1 classpath container, 
					 * we can exit early (which is good for performance reasons). 
					 * 
					 * Rational behind this: 
					 * A project can only be one of two things: 
					 * 1. It is contained in the ecl1 classpath container
					 * 2. It contains the ecl classpath container 
					 * 
					 * If the project contains the ecl1 classpath container, it cannot be
					 * within the ecl1 classpath container. Since we only need to update
					 * the ecl1 classpath container if a project _within_ the container changes,
					 * we can exit early.
					 */
					if(javaProject.getElementName().equals(delta.getResource().getName())) {
						continue;
					}									
					
					/*
					 * Note: We first check for flags for performance reasons and only after this we check if the 
					 * resourceName matches any extension within the ecl1 classpath container
					 */
					if( (delta.getKind() & IResourceDelta.REMOVED) == IResourceDelta.REMOVED &&
							isResourceMemberOfClasspathContainer(delta.getResource().getName())) {
						logger.info("The extension " + delta.getResource().getName() + " was removed! Since this extension is contained in the ecl1 classpath container, the container needs to be updated!");
						classpathContainerUpdateNecessary = true;
						break;
					}

					if( (delta.getFlags() & IResourceDelta.OPEN) == IResourceDelta.OPEN && 
							isResourceMemberOfClasspathContainer(delta.getResource().getName())) {
						logger.info("The extension " + delta.getResource().getName() + " was either opened or closed! Since this extension is contained in the ecl1 classpath container, the container needs to be updated!");
						classpathContainerUpdateNecessary = true;
						break;
					}

				}

				if(classpathContainerUpdateNecessary && canUpdateClasspathContainer(containerPath, javaProject)) {
					logger.info("Updating the ecl1 classpath container");

					Job updateClassPathContainerJob = new Job("Updating ecl1 classpath container") {

						@Override
						protected IStatus run(IProgressMonitor monitor) {
							try {
								updateClasspathContainer(containerPath, javaProject);

								/*
								 * Note: I specifically request a build here not because I want one, but because
								 * eclipse _will_ start one on its own after updating the ecl1 classpath container. 
								 * 
								 * When eclipse starts the build on its own, no progress monitor is created. Thus, 
								 * the user cannot cancel the job and eclipse becomes unusable until webapps has been built 
								 * (which takes ~5 min. on my machine !). 
								 * 
								 * By giving the build-job a monitor, the user can cancel the build-process if he/she chooses to do so. 
								 *
								 */
								ResourcesPlugin.getWorkspace().build(IncrementalProjectBuilder.INCREMENTAL_BUILD, monitor);

							} catch (CoreException e) {
								logger.error2("An exception occured while trying to update the ecl1 classpath container of webapps "
										+ "\nafter a project was changed in the workspace. This was the exception: ", e);
								return Status.CANCEL_STATUS;
							}
							return Status.OK_STATUS;
						}
					};

					updateClassPathContainerJob.schedule();

				}

			}
		};

		//Add listener to workspace. We only want to be informed after a workspace change is completed
		ResourcesPlugin.getWorkspace().addResourceChangeListener(listener, IResourceChangeEvent.POST_CHANGE);
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
	private Set<String> getExtensionsInClasspathContainer(IPath containerPath){
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
	private ArrayList<IClasspathEntry> createClasspathContainerEntries(IJavaProject javaProject, Set<String> extensionsForClasspathContainer) {
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
