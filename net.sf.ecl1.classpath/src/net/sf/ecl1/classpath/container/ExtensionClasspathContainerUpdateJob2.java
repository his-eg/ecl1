package net.sf.ecl1.classpath.container;

import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.ClasspathContainerInitializer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.GlobalBuildAction;
import net.sf.ecl1.classpath.Activator;
import net.sf.ecl1.utilities.general.ConsoleLogger;

/** Updates the ecl1 classpath container */
public class ExtensionClasspathContainerUpdateJob2 extends Job {

	private static final ConsoleLogger logger = new ConsoleLogger(Activator.getDefault().getLog(), Activator.PLUGIN_ID, ExtensionClasspathContainerUpdateJob2.class.getSimpleName()); 
		
	/* 
	 * Closing or removing a project must trigger an update job, because
	 * the closed or removed project might have contained an ecl1 classpath container. 
	 */
	Set<IProject> closedOrRemovedProjects;
	/*
	 * Modifying a classpath file must trigger an update job, because 
	 * an ecl1 classpath entry might have been removed from a .classpath file
	 */
	Set<IProject> projectsWithModifiedClasspathFile;
	/*
	 * Modifying an extension project must trigger an update job, because 
	 * the extension project might be a member of the ecl1 classpath container
	 */
	Set<IProject> extensionProjects;
		
	public ExtensionClasspathContainerUpdateJob2(Set<IProject> closedOrRemovedProjects,
			Set<IProject> projectsWithModifiedClasspathFile,
			Set<IProject> modifiedExtensionProjects) {
		super("Updating ecl1 classpath container");
		this.closedOrRemovedProjects = closedOrRemovedProjects;
		this.projectsWithModifiedClasspathFile = projectsWithModifiedClasspathFile;
		this.extensionProjects = modifiedExtensionProjects;
	}
	
	
	@Override
	protected IStatus run(IProgressMonitor monitor) {
		ProjectsWithContainer projectsWithContainer = ProjectsWithContainer.getInstance();

		/*
		 * 1. Process projects that might have an ecl1 classpath container and have been deleted <-- Done 
		 * 2. Process projects that might have an ecl1 classpath container and their .classpath-file was modified <-- Done
		 * 3. Update the ecl1 classpath container for all remaining projects with an ecl1 classpath container. 
		 * 4. If there are no other jobs of this kind in the queue --> Trigger a full build 
		 */
		try {
			
			/*
			 * Remove projects that might have an ecl1 classpath container and have been deleted 
			 */
			for(IProject project : closedOrRemovedProjects) {
				/* 
				 * If the project is not in the collection, the "removeProject" method will do no harm. 
				 * Since it is safe, we always call this method. 
				 */
				projectsWithContainer.removeProject(project);
				logger.debug("The following project had an ecl1 classpath container and was either removed or closed: " + project.getName());
			}
	
			/*
			 * Remove projects that had an ecl1 classpath container, that was deleted from the 
			 * .classpath file 
			 */
			for(IProject project : projectsWithModifiedClasspathFile) {
				IJavaProject javaProject = JavaCore.create(project);
				for(IClasspathEntry classpathEntry : javaProject.getRawClasspath()) {
					
					if (classpathEntry.getEntryKind() == IClasspathEntry.CPE_CONTAINER && 
							classpathEntry.getPath().segment(0).equals(ExtensionClasspathContainerPage.NET_SF_ECL1_ECL1_CONTAINER_ID)) {
						//Add project. If it is already added, we do no harm by calling the add method again. Thus always add. 
						projectsWithContainer.addProject(project);
						logger.debug("The .classpath-file of the following project was modified: " + project.getName() + 
								"\nAfter the modification the .classpath-files contains an ecl1 classpath container.");
					}
					
				}
				
			}
			
			/*
			 * Update the ecl1 classpath container for all projects, if necessary
			 */
			ClasspathContainerInitializer initializer = JavaCore.getClasspathContainerInitializer(ExtensionClasspathContainerPage.NET_SF_ECL1_ECL1_CONTAINER_ID);
			//Go through all projects with an ecl1 classpath containers
			outerLoop: for(IProject project : projectsWithContainer.getProjects()) {
					IJavaProject javaProject = JavaCore.create(project);
						for(IClasspathEntry classpathEntry : javaProject.getRawClasspath()) {
							
							if (classpathEntry.getEntryKind() == IClasspathEntry.CPE_CONTAINER && 
									classpathEntry.getPath().segment(0).equals(ExtensionClasspathContainerPage.NET_SF_ECL1_ECL1_CONTAINER_ID )) {
								//At this point, we found the ecl1 classpath container
								
								//Check if any of the modified extension projects is a member of the ecl1 classpath containers. If so --> Update. 
								for(IProject extensionProject : extensionProjects) {
									if(classpathEntry.getPath().segment(1).contains(extensionProject.getName())) {
										initializer.initialize(classpathEntry.getPath(), javaProject);
										logger.debug("The ecl1 classpath container of the following project was updated, because the status of one of the projects within the container changed.");
										//We are done with this project. Check the next one
										continue outerLoop;
									}
								}
							}
						}
	
				}
		
				
				
		} catch (CoreException e) {
			logger.error2("Updating of the ecl1 classpath container caused an exception. This was the exception: ", e);
			return Status.CANCEL_STATUS;
		}
		/*
		 * Check, if there are other jobs scheduled for updating the ecl1 classpath container. 
		 * If no job is scheduled anymore, we are the last one and can trigger a full build
		 */
		if (Activator.getDefault().isJobQueueEmpty()) {
			//Trigger a global rebuild, because eclipse does not realize that a build is necessary by itself
			GlobalBuildAction build = new GlobalBuildAction(PlatformUI.getWorkbench().getActiveWorkbenchWindow(),
					IncrementalProjectBuilder.INCREMENTAL_BUILD);
			build.doBuild();
		}
		
		return Status.OK_STATUS;
	}

}
