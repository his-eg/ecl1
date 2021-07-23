package net.sf.ecl1.classpath.container;

import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.ClasspathContainerInitializer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import net.sf.ecl1.classpath.Activator;
import net.sf.ecl1.utilities.general.BuildJob;
import net.sf.ecl1.utilities.general.ConsoleLogger;

/** Updates the ecl1 classpath container */
public class ExtensionClasspathContainerUpdateJob extends Job {

	private static final ConsoleLogger logger = new ConsoleLogger(Activator.getDefault().getLog(), Activator.PLUGIN_ID, ExtensionClasspathContainerUpdateJob.class.getSimpleName()); 
		
	Set<IJavaProject> projectsThatNeedToBeUpdated;
		
	public static final Object FAMILY = new Object();	
	
	public ExtensionClasspathContainerUpdateJob(Set<IJavaProject> projectsThatNeedToBeUpdated) {
		super("Updating ecl1 classpath container");
		this.projectsThatNeedToBeUpdated = projectsThatNeedToBeUpdated;
		
		logger.info("Updating the ecl1 classpath container is scheduled. The following projects will be updated: " );
		for(IJavaProject p : projectsThatNeedToBeUpdated) {
			logger.info(p.getElementName());
		}
	}

	
	@Override
	public boolean belongsTo(Object family) {
		return FAMILY.equals(family);
	}
	
	
	@Override
	protected IStatus run(IProgressMonitor monitor) {
		logger.info("Updating the ecl1 classpath container");
		try {
			
//			/*
//			 * The following code prevents the occurrence of Java Model Exceptions when
//			 * adding extension projects to the workspace.
//			 * 
//			 * It is currently not active, since it uses classes from eclipse that
//			 * do not belong to the public api and can be changed without notice. 
//			 * 
//			 * More details about this design decision in the ticket: 
//			 * https://hiszilla.his.de/hiszilla/show_bug.cgi?id=261078
//			 */
//			int indexJobsCount = JavaModelManager.getIndexManager().awaitingJobsCount();
// 
//			while (indexJobsCount > 0) {
//				long sleepTime = 5000;
//				logger.info("The indexer is still working!"
//						+ "\nCannot update the ecl1 classpath container while indexing without likely causing a Java Model Exception"
//						+ "\nWill wait for " + sleepTime + "ms for the indexer to finish "
//						+ "\n" + indexJobsCount + " files must still be indexed.");
//				try {
//					Thread.sleep(sleepTime);
//				} catch (InterruptedException e) {
//					logger.error2("Updating of the ecl1 classpath container caused an exception. This was the exception: ", e);
//					return Status.CANCEL_STATUS;
//				}
//				indexJobsCount = JavaModelManager.getIndexManager().awaitingJobsCount();
//			}			
			
			for(IJavaProject project : projectsThatNeedToBeUpdated) {
				//Get the path to the ecl1 classpath container for this project
				for(IClasspathEntry classpathEntry : project.getRawClasspath()) {
					
					//Exit early if anything else than IClasspathEntry.CPE_CONTAINER
					if(classpathEntry.getEntryKind() != IClasspathEntry.CPE_CONTAINER) {
						continue;
					}
					
					if(classpathEntry.getPath().segment(0).equals(ExtensionClasspathContainerPage.NET_SF_ECL1_ECL1_CONTAINER_ID)) {
						//Found the containerpath -> Now perform update 
						ClasspathContainerInitializer initializer = JavaCore.getClasspathContainerInitializer(ExtensionClasspathContainerPage.NET_SF_ECL1_ECL1_CONTAINER_ID);
						initializer.initialize(classpathEntry.getPath(), project);
						break;
					}
				
				}

			}
			
			
		} catch (CoreException e) {
			logger.error2("Updating of the ecl1 classpath container caused an exception. This was the exception: ", e);
			return Status.CANCEL_STATUS;
		}
		
		logger.info("Ecl1 classpath container successfully updated :)");
		
		//Rebuild the workspace manually, because eclipse does not realize that a build is necessary by itself
		new BuildJob().schedule();
		return Status.OK_STATUS;
	}

	

	
	

}
