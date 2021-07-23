package net.sf.ecl1.classpath.container;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.core.runtime.jobs.Job;

import net.sf.ecl1.classpath.Activator;
import net.sf.ecl1.utilities.general.ConsoleLogger;


/**
 * 
 * Listens for changes in the workspace that have the potential to require updating
 * the ecl1 classpath container
 *
 */
public class ExtensionClasspathContainerListener implements IResourceChangeListener {

	private static final ConsoleLogger logger = new ConsoleLogger(Activator.getDefault().getLog(), Activator.PLUGIN_ID, ExtensionClasspathContainerListener.class.getSimpleName());
	
	private static final ExtensionClasspathContainerListener instance = new ExtensionClasspathContainerListener();
	
	Set<IJavaProject> javaProjectsWithClasspathContainer = new HashSet<>();
		
	ExtensionClasspathContainerUpdateJob updateJob;
	
	
	/** How many ms should I wait before starting the job to update the ecl1 classpath container?
	 * <br>
	 *  Note: This is done to "collect" multiple calls to this listener to start the update job only once. 
	 *  <br>
	 *  This makes sense in the following scenario: 
	 *  Suppose multiple extension projects are deleted: Instead of updating the workspace after 
	 *  every deletion, we wait a little until every deletion has finished and then update the ecl1
	 *  classpath container update job.
	 */
	static final long DELAY = 5000; //ms
	

	private ExtensionClasspathContainerListener() {}
	
	public static ExtensionClasspathContainerListener getInstance() {
		return instance;
	}
	
	public void addProjectWithClasspathContainer(IJavaProject p) {
		javaProjectsWithClasspathContainer.add(p);
	}
	
	/**
	 * Checks if projects with ecl1 classpath containers are still in the workspace or if they have
	 * been deleted by the user
	 * 
	 */
	private void checkIfProjectsWithClasspathContainerAreStillPresentInTheWorkspace() {
		
		Iterator<IJavaProject> it = javaProjectsWithClasspathContainer.iterator();
		while(it.hasNext()) {
			IJavaProject project = it.next();
			if(!project.exists()) {
				it.remove();
			}
		}
		
	}
	
	
	/**
	 * Collects all projects in which the extension is present in the project's ecl1 classpath container
	 * 
	 * @param extensionName
	 * @param allProjects
	 * @return
	 */
	private Set<IJavaProject> collectProjectsWhereThisExtensionIsPresent(String extensionName, Set<IJavaProject> allProjects) {
		Set<IJavaProject> returnSet = new HashSet<>();
		
		for(IJavaProject project : allProjects) {
			try {
				for(IClasspathEntry classpathEntry : project.getRawClasspath()) {
					
					//Exit early if anything else than IClasspathEntry.CPE_CONTAINER
					if(classpathEntry.getEntryKind() != IClasspathEntry.CPE_CONTAINER) {
						continue;
					}
					
					//Exit early if not an ecl1 classpath container
					if(!classpathEntry.getPath().segment(0).equals(ExtensionClasspathContainerPage.NET_SF_ECL1_ECL1_CONTAINER_ID)) {
						continue;
					}
					
					//extension is a member of this ecl1 classpath container
					if(classpathEntry.getPath().segment(1).contains(extensionName)) {
						returnSet.add(project);
						break;
					}
				}
			} catch (JavaModelException e) {
				e.printStackTrace();
			}
		}
		
		return returnSet;
	}
	
	
	@Override
	public void resourceChanged(IResourceChangeEvent event) {  

		IResourceDelta rootDelta = event.getDelta();
		
		Set<IJavaProject> projectsThatNeedToBeUpdated = new HashSet<>();
		
		checkIfProjectsWithClasspathContainerAreStillPresentInTheWorkspace();

		//Just for debugging purposes.
		//TODO: Comment me before releasing!
//		System.out.println("Full path of root Delta: " + rootDelta.getFullPath());
//		System.out.println("Printing all children of delta:");
//		for(IResourceDelta child : rootDelta.getAffectedChildren()) {
//			System.out.println("Path of child: " + child.getFullPath());
//			System.out.println("Kind of child: " + child.getKind());
//			System.out.println("Flags of child: " + child.getFlags());
//		}
		
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
		 * Sometimes, when importing, I also saw: 
		 * Kind --> IResourceDelta.ADDED 
		 * Flags --> IResourceDelta.OPEN   
		 * 
		 * 
		 *-----------------------------------
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
		 * Kind --> IResourceDelta.CHANGED | IResourceDelta.ADDED
		 * Flags --> IResourceDelta.OPEN   
		 * 
		 */

		
		//Only check direct children (aka projects) of rootDelta 
		for(IResourceDelta delta : rootDelta.getAffectedChildren( (IResourceDelta.REMOVED | IResourceDelta.CHANGED | IResourceDelta.ADDED) )) {
			/*
			 * If the project changes which contains an ecl1 classpath container, 
			 * we can exit early.
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
			if (javaProjectsWithClasspathContainer.stream().anyMatch( t -> t.getElementName().equals(delta.getResource().getName()))) {
				continue;
			}
											
						
			
			/*
			 * Note: We first check for flags for performance reasons and only after this we check if the 
			 * resourceName matches any extension within the ecl1 classpath container
			 */
			if( (delta.getKind() & IResourceDelta.REMOVED) == IResourceDelta.REMOVED ) {
				String extensionName = delta.getResource().getName();
				Set<IJavaProject> temp = collectProjectsWhereThisExtensionIsPresent(extensionName, javaProjectsWithClasspathContainer);
				if(!temp.isEmpty()) {
					String outputString = "The following extension project: " + extensionName + " was removed.\n";
					outputString += "The project is a member of the ecl1 classpath container of the following projects: \n";
					for(IJavaProject p : temp) {
						outputString += p.getElementName() + "\n";
					}
					outputString += "The ecl1 classpath container of these projects will be updated.";
					logger.info(outputString);
					projectsThatNeedToBeUpdated.addAll(temp);
				}
			}

			if( (delta.getFlags() & IResourceDelta.OPEN) == IResourceDelta.OPEN ) {
				String extensionName = delta.getResource().getName();
				Set<IJavaProject> temp = collectProjectsWhereThisExtensionIsPresent(extensionName, javaProjectsWithClasspathContainer);
				if(!temp.isEmpty()) {
					String outputString = "The following extension project: " + extensionName + " was either opened or closed!\n";
					outputString += "The project is a member of the ecl1 classpath container of the following projects: \n";
					for(IJavaProject p : temp) {
						outputString += p.getElementName() + "\n";
					}
					outputString += "The ecl1 classpath container of these projects will be updated.";
					logger.info(outputString);
					projectsThatNeedToBeUpdated.addAll(temp);
				}
			}

		}

		
		
		if(!projectsThatNeedToBeUpdated.isEmpty()) {
			scheduleUpdateJob(projectsThatNeedToBeUpdated);
		}
		
	}
	
	
	private void scheduleUpdateJob(Set<IJavaProject> projectsThatNeedUpdates) {		
		/*
		 * The listener was triggered again while waiting to update the ecl1 classpath container. 
		 * 
		 * Since it is possible that now more projects with ecl1 classpath containers need to be updated, we need to cancel
		 * the old job and start a new job with the updated list of projects that need to be updated.  
		 */
		Job.getJobManager().cancel(ExtensionClasspathContainerUpdateJob.FAMILY);
		
		ExtensionClasspathContainerUpdateJob updateJob = new ExtensionClasspathContainerUpdateJob(projectsThatNeedUpdates);
		updateJob.setRule(ResourcesPlugin.getWorkspace().getRuleFactory().buildRule());
		updateJob.schedule(DELAY);
		
	}

}
