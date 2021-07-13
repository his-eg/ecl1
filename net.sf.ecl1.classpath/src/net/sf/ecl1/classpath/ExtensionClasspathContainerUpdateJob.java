package net.sf.ecl1.classpath;

import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import net.sf.ecl1.importwizard.ExtensionImportJob;
import net.sf.ecl1.utilities.general.ConsoleLogger;

/** Updates the ecl1 classpath container */
public class ExtensionClasspathContainerUpdateJob extends Job {

	private static final ConsoleLogger logger = new ConsoleLogger(Activator.getDefault().getLog(), Activator.PLUGIN_ID, ExtensionClasspathContainerUpdateJob.class.getSimpleName());
	
	/** Path to the ecl1 classpath container*/
	IPath containerPath;
	/** Project that contains an ecl classpath container */
	IJavaProject javaProject;
	
	long wakeUpTime = System.currentTimeMillis();
	
	public static final Object FAMILY = new Object();
	
	Job buildWorkspaceJob = new Job("Building Workspace") {
		
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			try {
				ResourcesPlugin.getWorkspace().build(IncrementalProjectBuilder.INCREMENTAL_BUILD, monitor);
			} catch (CoreException e) {
				e.getStatus();
			}
			return Status.OK_STATUS;
		}
	};
	
	
	public ExtensionClasspathContainerUpdateJob(IPath containerPath, IJavaProject javaProject) {
		super("Updating ecl1 classpath container");
		this.containerPath = containerPath;
		this.javaProject = javaProject;
	}

	
	@Override
	public boolean belongsTo(Object family) {
		return FAMILY.equals(family);
	}
	
	
	public void delayStart(long delay) {
		this.wakeUpTime = System.currentTimeMillis() + delay;
	}
	
	@Override
	protected IStatus run(IProgressMonitor monitor) {
		
		boolean importJobRunning = true;
		while(importJobRunning) {
			//Maybe the import Job is still running? If so, delay this job...
			Job[] importJobs = Job.getJobManager().find(ExtensionImportJob.JOB_FAMILY);
			if(importJobs.length != 0) {
				logger.info("The ecl1 extension import job is still running. Updating the ecl1 classpath container will be delayed by: " + ExtensionClasspathContainerListener.DELAY + " ms");
				try {
					Thread.sleep(ExtensionClasspathContainerListener.DELAY);
				} catch (InterruptedException e) {
					return Status.CANCEL_STATUS;
				}
			} else {
				importJobRunning = false;
			}
		}
		
		boolean shouldSleep = true;
		while(shouldSleep) {
			if(System.currentTimeMillis() < wakeUpTime) {
				try {
					Thread.sleep(wakeUpTime - System.currentTimeMillis());
				} catch (InterruptedException e) {
					return Status.CANCEL_STATUS;
				}
			} else {
				shouldSleep = false;
			}
		}
		
		
		
		logger.info("Updating the ecl1 classpath container");
		try {
			//Update ecl1 classpath container
//			ClasspathContainerInitializer initializer = JavaCore.getClasspathContainerInitializer(ExtensionClasspathContainerPage.NET_SF_ECL1_ECL1_CONTAINER_ID);
//			initializer.initialize(containerPath, javaProject);
			
			//Update ecl1 classpath container
			ExtensionClasspathContainerInitializer.updateClasspathContainer(containerPath, javaProject);
			//Updating the index is necessary or else an exception occurs when trying to build the workspace
			JavaCore.rebuildIndex(monitor);
		} catch (CoreException e) {
			logger.error2("Updating of the ecl1 classpath container caused an exception. This was the exception: ", e);
			return Status.CANCEL_STATUS;
		}

		//Maybe add this to prevent race conditions? Doesn't seem to be necessary on my machine, though...
//		buildWorkspaceJob.setRule(ResourcesPlugin.getWorkspace().getRoot());
		//Start a build
		buildWorkspaceJob.schedule();
		return Status.OK_STATUS;
	}

	

	
	

}
