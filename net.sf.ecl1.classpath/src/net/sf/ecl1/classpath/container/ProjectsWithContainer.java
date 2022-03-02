package net.sf.ecl1.classpath.container;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jdt.core.IJavaProject;

import net.sf.ecl1.classpath.Activator;
import net.sf.ecl1.utilities.general.ConsoleLogger;

/**
 * 
 * This class stores all projects that possess an ecl1 classpath container. 
 * 
 * If no project exists any more with an ecl1 classpath container, the respective listener for updating ecl1 classpath containers 
 * is deactivated. 
 * 
 * @author sohrt
 *
 */
public class ProjectsWithContainer {
	
	private static final ConsoleLogger logger = new ConsoleLogger(Activator.getDefault().getLog(), Activator.PLUGIN_ID, ProjectsWithContainer.class.getSimpleName());
	
	private static final ProjectsWithContainer instance = new ProjectsWithContainer();
	
	private Set<IProject> projectsWithEcl1ClasspathContainer = new HashSet<>();
	
	private ProjectsWithContainer() {}	
	
	public static ProjectsWithContainer getInstance() {
		return instance;
	}
	
	synchronized public void addProject(IProject project) {
		projectsWithEcl1ClasspathContainer.add(project);
		
		//We only want to be informed after a workspace change is completed
		//Even if addResourceChangeListener is called multiple times, the listener will only be added once.
		ResourcesPlugin.getWorkspace().addResourceChangeListener(ExtensionClasspathContainerListener2.getInstance(), IResourceChangeEvent.POST_CHANGE);
		
		logger.debug("The following project was either imported/opened or an ecl1 classpath container was added: " + project.getName());
	}
	
	synchronized public boolean removeProject(IProject project) {
		boolean returnValue = projectsWithEcl1ClasspathContainer.remove(project);
		
		//Remove listener, if no more projects with ecl1 classpath containers are present any longer
		if (projectsWithEcl1ClasspathContainer.isEmpty()) {
			logger.debug("No more projects with ecl1 classpath containers in the workspace."
					+ " Removing listener that monitors changes which might trigger an update of the ecl1 classpath container.");
			ResourcesPlugin.getWorkspace().removeResourceChangeListener(ExtensionClasspathContainerListener2.getInstance());
		}
		
		logger.debug("The following project was either closed/deleted or does no longer possess an ecl1 classpath container: " + project.getName());
		return returnValue;
	}
	
	synchronized public boolean isEmpty() {
		return projectsWithEcl1ClasspathContainer.isEmpty();
	}
	
	synchronized public Set<IProject> getProjects() {
		return projectsWithEcl1ClasspathContainer;
	}
	
}
