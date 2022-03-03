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
		
	
	Set<IProject> modifiedProjects;
	
	/* 
	 * Closing or removing a project must trigger an update job, because
	 * the closed or removed project might have contained an ecl1 classpath container. 
	 */
	Set<IProject> removedProjects;
	/*
	 * Modifying an extension project must trigger an update job, because 
	 * the extension project might be a member of the ecl1 classpath container
	 */
	Set<IProject> addedProjects;
		
	public ExtensionClasspathContainerUpdateJob2(Set<IProject> removedProjects,
			Set<IProject> addedProjects) {
		super("Updating ecl1 classpath container");
		this.removedProjects = removedProjects;
		this.addedProjects = addedProjects;
	}
	
	
	@Override
	protected IStatus run(IProgressMonitor monitor) {		
		ProjectsWithContainer projectsWithContainer = ProjectsWithContainer.getInstance();
		/*
		 * TODO: Delete me?
		 * 1. Process projects that might have an ecl1 classpath container and have been deleted <-- Done 
		 * 2. Process projects that might have an ecl1 classpath container and their .classpath-file was modified <-- Done
		 * 3. Update the ecl1 classpath container for all remaining projects with an ecl1 classpath container. 
		 * 4. If there are no other jobs of this kind in the queue --> Trigger a full build 
		 */
		try {
			
			/*
			 * Remove projects that might have an ecl1 classpath container and have been deleted 
			 */
			for(IProject project : removedProjects) {
				/* 
				 * If the project is not in the collection, the "removeProject" method will do no harm. 
				 * Since it is safe, we always call this method. 
				 */
				projectsWithContainer.removeProject(project);
			}

			
			if (!removedProjects.isEmpty() || !addedProjects.isEmpty() || !projectsWithContainer.isEmpty() ) {
				/*
				 * Update all projects with an ecl1 classpath container
				 */
				ClasspathContainerInitializer initializer = JavaCore.getClasspathContainerInitializer(ExtensionClasspathContainerPage.NET_SF_ECL1_ECL1_CONTAINER_ID);
				projectLoop: for(IProject project : projectsWithContainer.getProjects()) {
					IJavaProject javaProject = JavaCore.create(project);
					for(IClasspathEntry classpathEntry : javaProject.getRawClasspath()) {
						
						if (classpathEntry.getEntryKind() == IClasspathEntry.CPE_CONTAINER && 
								classpathEntry.getPath().segment(0).equals(ExtensionClasspathContainerPage.NET_SF_ECL1_ECL1_CONTAINER_ID )) {
							initializer.initialize(classpathEntry.getPath(), javaProject);
							logger.debug("The ecl1 classpath container of the following project was updated");
							//We are done with this project. Check the next one
							continue projectLoop;
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
