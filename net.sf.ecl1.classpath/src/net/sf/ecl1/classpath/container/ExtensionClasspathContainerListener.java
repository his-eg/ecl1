package net.sf.ecl1.classpath.container;

import java.util.HashSet;
import java.util.Set;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.ResourcesPlugin;
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
	
	private ExtensionClasspathContainerListener() {}
	
	
	@Override
	public void resourceChanged(IResourceChangeEvent event) {
		
		Set<IProject> deletedProjects = new HashSet<IProject>();
		Set<IProject> addedProjects = new HashSet<IProject>();
		
		IResourceDelta delta = event.getDelta();
		
		/*
		 * Project was deleted
		 */
		IResourceDelta[] projectDeltas = delta.getAffectedChildren(IResourceDelta.REMOVED);
		for(IResourceDelta projectDelta : projectDeltas) {
			IProject project = (IProject) projectDelta.getResource();
			deletedProjects.add(project);
		}
		
		
		projectDeltas = delta.getAffectedChildren(IResourceDelta.CHANGED);
		for(IResourceDelta projectDelta : projectDeltas) {
	        IProject project = (IProject) projectDelta.getResource();
			
			/*
			 * Project was closed/opened 
			 */
	        if ( (projectDelta.getFlags() & IResourceDelta.OPEN ) == IResourceDelta.OPEN ) {
	        	if (project.isOpen() ) {
	        		//Project was opened
	        		addedProjects.add(project);
	        	} else {
	        		//Project was closed
	        		deletedProjects.add(project);
	        	}
	        }
		}
		
		/*
		 * Projects were added
		 */
		projectDeltas = delta.getAffectedChildren(IResourceDelta.ADDED);
		for(IResourceDelta projectDelta : projectDeltas) {
	        IProject project = (IProject) projectDelta.getResource();
	        addedProjects.add(project);		
		}
			
		
		if ( !deletedProjects.isEmpty() || !addedProjects.isEmpty()) {
			ExtensionClasspathContainerUpdateJob updateJob = new ExtensionClasspathContainerUpdateJob(deletedProjects,addedProjects);
			Activator.getDefault().addJob(updateJob);
			updateJob.setRule(ResourcesPlugin.getWorkspace().getRuleFactory().buildRule());
			updateJob.schedule();
		}
		
	}

	public static IResourceChangeListener getInstance() {
		return instance;
	}

}
