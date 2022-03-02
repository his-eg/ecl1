package net.sf.ecl1.classpath.container;

import java.util.HashSet;
import java.util.Set;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.CoreException;
import net.sf.ecl1.classpath.Activator;
import net.sf.ecl1.utilities.general.ConsoleLogger;
import net.sf.ecl1.utilities.general.ProjectNature;


/**
 * 
 * Listens for changes in the workspace that have the potential to require updating
 * the ecl1 classpath container
 *
 */
public class ExtensionClasspathContainerListener2 implements IResourceChangeListener {

	private static final ConsoleLogger logger = new ConsoleLogger(Activator.getDefault().getLog(), Activator.PLUGIN_ID, ExtensionClasspathContainerListener2.class.getSimpleName());
	
	private static final ExtensionClasspathContainerListener2 instance = new ExtensionClasspathContainerListener2();
	
	private ExtensionClasspathContainerListener2() {}
	
	
	@Override
	public void resourceChanged(IResourceChangeEvent event) {
		/* 
		 * Closing or removing a project must trigger an update job, because
		 * the closed or removed project might have contained an ecl1 classpath container. 
		 */
		Set<IProject> closedOrRemovedProjects = new HashSet<IProject>();
		/*
		 * Modifying a classpath file must trigger an update job, because 
		 * an ecl1 classpath entry might have been removed from a .classpath file
		 */
		Set<IProject> projectsWithModifiedClasspathFile = new HashSet<IProject>();
		/*
		 * Modifying an extension project must trigger an update job, because 
		 * the extension project might be a member of the ecl1 classpath container
		 */
		Set<IProject> extensionProjects = new HashSet<IProject>();
		
		
		int type = event.getType();
		if(IResourceChangeEvent.PRE_CLOSE == type || IResourceChangeEvent.PRE_DELETE == type) {
			IProject project = (IProject) event.getResource();
			closedOrRemovedProjects.add(project);
		}
		
		
		
		IResourceDelta delta = event.getDelta();
		IResourceDelta[] projectDeltas = delta.getAffectedChildren(IResourceDelta.REMOVED | IResourceDelta.CHANGED | IResourceDelta.ADDED);
		for(IResourceDelta projectDelta : projectDeltas) {
	        IProject project = (IProject) projectDelta.getResource();
			
			
	        //Check, if a .classpath file was modified
	        if(projectDelta.getKind() == IResourceDelta.CHANGED) {
				projectDelta.findMember(projectDelta.getFullPath().append(".classpath"));
				projectsWithModifiedClasspathFile.add(project);
			}
			
			
			if (projectDelta.getKind() == IResourceDelta.CHANGED && (projectDelta.getFlags() & IResourceDelta.OPEN) != 1 ) {
				//Project was changed, but not closed/opened --> Cannot change the content of an ecl1 classpath container. Therefore we break
				break;
			}

	        try {
				if (project.hasNature(ProjectNature.ECL1.getNature())) {
					/*
					 * When we reached this part: 
					 * An extension project was either added, removed, closed or opened in the workspace. 
					 * We must trigger the update job to check, if an update is necessary. 
					 */
					extensionProjects.add(project);
				}
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}
			
		if ( !closedOrRemovedProjects.isEmpty() || !projectsWithModifiedClasspathFile.isEmpty() || !extensionProjects.isEmpty() ) {
			Activator.getDefault().addJob(new ExtensionClasspathContainerUpdateJob2(closedOrRemovedProjects, projectsWithModifiedClasspathFile, extensionProjects));
		}
		
	}

	public static IResourceChangeListener getInstance() {
		return instance;
	}

}
