package net.sf.ecl1.classpath;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.core.runtime.jobs.Job;
import net.sf.ecl1.utilities.general.ConsoleLogger;


/**
 * 
 * Listens for changes in the workspace that have the potential to require updating
 * the ecl1 classpath container
 *
 */
public class ExtensionClasspathContainerListener implements IResourceChangeListener {

	private static final ConsoleLogger logger = new ConsoleLogger(Activator.getDefault().getLog(), Activator.PLUGIN_ID, ExtensionClasspathContainerListener.class.getSimpleName());
	
	Set<String> extensionsInClasspathContainer = new HashSet<String>();
	
	/** Path to the ecl1 classpath container*/
	IPath containerPath;
	/** Project that contains an ecl classpath container */
	IJavaProject javaProject;
	
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
	

	
	
	//TODO: Both parameters necessary? Maybe only containerPath parameter is necessary?
	public ExtensionClasspathContainerListener(IPath containerPath, IJavaProject javaProject) {
		this.containerPath = containerPath;
		this.javaProject = javaProject;
		this.updateJob = new ExtensionClasspathContainerUpdateJob(containerPath, javaProject);
	}
	
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
		
		extensionsInClasspathContainer = ExtensionClasspathContainerInitializer.getExtensionsInClasspathContainer(containerPath);     

		IResourceDelta rootDelta = event.getDelta();

		//Just for debugging purposes.
		//TODO: Comment me before releasing!
//		System.out.println("Full path of root Delta: " + rootDelta.getFullPath());
//		System.out.println("Printing all children of delta:");
//		for(IResourceDelta child : rootDelta.getAffectedChildren()) {
//			System.out.println("Path of child: " + child.getFullPath());
//			System.out.println("Kind of child: " + child.getKind());
//			System.out.println("Flags of child: " + child.getFlags());
//		}

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
			 * If the project changes which contains the ecl1 classpath container, 
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

		
		
		if(classpathContainerUpdateNecessary) {
			scheduleUpdateJob();
		}
		
	}
	
	
	private void scheduleUpdateJob() {
		//Only schedule this job once
		Job[] alreadyScheduledJobs = Job.getJobManager().find(ExtensionClasspathContainerUpdateJob.FAMILY);
		if(alreadyScheduledJobs.length == 0) {
			//Setting this rule prevents auto builds
			updateJob.setRule(ResourcesPlugin.getWorkspace().getRuleFactory().buildRule());
			updateJob.schedule(DELAY);
		}
		
		/*
		 * Every time the listener is triggered, the job is delayed further. 
		 * 
		 * This is done to wait for the workspace to "calm down" and it becomes static 
		 * (no more projects are deleted/added/changed and thus the listener is not triggered anymore). 
		 * 
		 * We do this to avoid updating the ecl1 classpath container multiple times (after every little change) and 
		 * instead only update it (hopefully) once by grouping all relevant events together.  
		 * 
		 */
		updateJob.wakeUp(DELAY);
	}

}
