package net.sf.ecl1.utilities.general;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import net.sf.ecl1.utilities.Activator;
import net.sf.ecl1.utilities.standalone.ClasspathHandler;
import net.sf.ecl1.utilities.standalone.workspace.WorkspaceFactory;

/**
 * manages project creation
 *
 * @author keunecke
 */
public class ProjectSupport {

    private static final String[] SOURCE_FOLDERS = { "src/java", "src/test", "src/generated"};
    
    private static final String[] FOLDERS_TO_CREATE =  { "src/java", "src/test", "src/generated", "resource", ".settings" };

	private static final String JRE_ENVIRONMENT = "org.eclipse.jdt.launching.JRE_CONTAINER";
    
    private final Collection<String> packagesToCreate;
    
    /**
     * Create a new ProjectSupport
     * 
     * @param packages packages that should be created 
     */
	public ProjectSupport(Collection<String> packages) {
	    this.packagesToCreate = packages;
	}

	/**
     * creates a new project with a skeleton
     *
     * @param choices choices container from the setup pages 
     * @param projectName name of project
     * @param location location
     * @return IProject instance
     */
	public IProject createProject(InitialProjectConfigurationChoices choices, URI location) {
		Assert.isNotNull(choices.getName());
		Assert.isTrue(choices.getName().trim().length() > 0);

		String projectName = choices.getName();
		IProject project;
		
		try {
			project = createBaseProject(projectName, location);
			addToProjectStructure(project, FOLDERS_TO_CREATE);
			if(Activator.isRunningInEclipse()){  
				setSourceFolders(project, SOURCE_FOLDERS);
				addProjectDependencies(project, choices.getProjectsToReference());
				setJreEnvironment(project);
			}else{
				setupClasspathStandalone(project.getLocation().toString(), choices.getProjectsToReference());
			}

        } catch (CoreException e) {
			throw new RuntimeException("Exception creating new project '" + projectName + "': " + e.getMessage(), e);
        }

		return project;
	}

	/**
	 * Does the same as setSourceFolders, addProjectDependencies, setJreEnvironment but for standalone
	 */
	private void setupClasspathStandalone(String projectPath, Collection<String> projectsToReference){
		ClasspathHandler handler = new ClasspathHandler(projectPath);
		// setSourceFolders
		for (String srcPathName : SOURCE_FOLDERS) {
			handler.addEntry("src", srcPathName);
		}
		// addProjectDependencies
		for (String referencedProject : projectsToReference) {
			handler.addEntry("src", referencedProject);
		}
		// setJreEnvironment
		handler.addEntry("con", JRE_ENVIRONMENT);
		// set output
		handler.addEntry("output", "bin");
		handler.save();
	}

	/**
	 * configure the project's jre environment
	 * 
	 * @param project
	 * @throws JavaModelException
	 */
	public void setJreEnvironment(IProject project) throws JavaModelException {
		IJavaProject javaProject = createJavaProject(project);
		
		// add JRE container to classpath
		IClasspathEntry containerEntry = JavaCore.newContainerEntry(new Path(JRE_ENVIRONMENT), false);
		IClasspathEntry[] oldClassPath = javaProject.getRawClasspath();
		ArrayList<IClasspathEntry> list = new ArrayList<IClasspathEntry>(Arrays.asList(oldClassPath));
		list.add(containerEntry);
		javaProject.setRawClasspath(list.toArray(new IClasspathEntry[0]), null);
		
		// compiler compliance settings are set with help of org.eclipse.core.resources.prefs template
	}

	/**
	 * configure the project's referenced projects
	 * 
	 * @param project
	 * @param referencedProjects
	 * @throws JavaModelException
	 */
	public void addProjectDependencies(IProject project, Collection<String> referencedProjects) throws JavaModelException {
    	IJavaProject javaProject = createJavaProject(project);
    	for (String referencedProject : referencedProjects) {
    		IPath path = new Path("/" + referencedProject);
    		IClasspathEntry projectEntry = JavaCore.newProjectEntry(path);
    		IClasspathEntry[] oldClassPath = javaProject.getRawClasspath();
    		ArrayList<IClasspathEntry> list = new ArrayList<IClasspathEntry>(Arrays.asList(oldClassPath));
    		list.add(projectEntry);
    		javaProject.setRawClasspath(list.toArray(new IClasspathEntry[0]), null);
    	}
	}

	/**
	 * set the project's source folders
	 * 
	 * @param project
	 * @param paths
	 * @throws JavaModelException 
	 */
	public void setSourceFolders(IProject project, String[] paths) throws JavaModelException {
		IJavaProject javaProject = createJavaProject(project);
		IClasspathEntry[] newEntries = new IClasspathEntry[paths.length];
		int count = 0;
		for (String srcPathName : paths) {
			IPath srcPath= javaProject.getPath().append(srcPathName);
			IClasspathEntry srcEntry= JavaCore.newSourceEntry(srcPath, null);
			newEntries[count] = JavaCore.newSourceEntry(srcEntry.getPath());
			count = count + 1;
		}
		javaProject.setRawClasspath(newEntries, null);
	}

	private IJavaProject createJavaProject(IProject project) {
		IJavaProject javaProject = JavaCore.create(project);
		return javaProject;
	}

	/**
     * Creates an empty project at the given location
     * 
     * @param name
     * @param location
     * @return the new project
	 * @throws CoreException 
	*/
	public IProject createBaseProject(String name, URI location) throws CoreException {
		IProject project = WorkspaceFactory.getWorkspace().getRoot().getProject(name);
		// no need for standalone to create project
		if (!project.exists() && Activator.isRunningInEclipse()) {
			IProjectDescription desc = project.getWorkspace().newProjectDescription(project.getName());
			URI workspaceLocation = WorkspaceFactory.getWorkspace().getRoot().getLocationURI();
			URI projectLocation = location;
			if (location != null && location.equals(workspaceLocation)) {
				projectLocation = null;
			}
			desc.setLocationURI(projectLocation);
			// Add initial java nature, because it is expected by IJavaProject.setRawClasspath
			desc.setNatureIds(new String[]{JavaCore.NATURE_ID});
			
			project.create(desc, null);
			if (!project.isOpen()) {
				project.open(null);
			}
		}
		return project;
	}

	private void createFolder(IFolder folder) throws CoreException {
		IContainer parent = folder.getParent();
		if (parent instanceof IFolder) {
			createFolder((IFolder) parent);
		}
		if (!folder.exists()) {
			folder.create(false, true, null);
		}
	}

	/**
	 * Create a folder structure with a parent root, overlay, and a few child
	 * folders.
	 *
	 * @param newProject
	 * @param paths
	 * @throws CoreException
	 */
	public void addToProjectStructure(IProject newProject,
			String[] paths) throws CoreException {
		for (String path : paths) {
			IFolder etcFolders = newProject.getFolder(path);
			createFolder(etcFolders);
		}
		for (String packageName : this.packagesToCreate) {
		    createFolder(newProject.getFolder("/src/java/" + packageName.replace('.', '/')));
		    createFolder(newProject.getFolder("/src/test/" + packageName.replace('.', '/')));
        }
	}
}
