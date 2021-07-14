package net.sf.ecl1.classpath.container;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import net.sf.ecl1.classpath.Activator;
import net.sf.ecl1.utilities.general.ConsoleLogger;

/** Updates the ecl1 classpath container */
public class ExtensionClasspathContainerUpdateJob extends Job {

	private static final ConsoleLogger logger = new ConsoleLogger(Activator.getDefault().getLog(), Activator.PLUGIN_ID, ExtensionClasspathContainerUpdateJob.class.getSimpleName()); 
	
	/** Path to the ecl1 classpath container*/
	IPath containerPath;
	/** Project that contains an ecl classpath container */
	IJavaProject javaProject;
		
	public static final Object FAMILY = new Object();	
	
	public ExtensionClasspathContainerUpdateJob(IPath containerPath, IJavaProject javaProject) {
		super("Updating ecl1 classpath container");
		this.containerPath = containerPath;
		this.javaProject = javaProject;
	}

	
	@Override
	public boolean belongsTo(Object family) {
		return FAMILY.equals(family);
	}
	
	
	@Override
	protected IStatus run(IProgressMonitor monitor) {
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
		
		logger.info("Ecl1 classpath container successfully updated :)");
		return Status.OK_STATUS;
	}

	

	
	

}
