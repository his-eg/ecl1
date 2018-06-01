package de.his.cs.sys.extensions.wizards.utils;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import h1modules.utilities.utils.Activator;
import net.sf.ecl1.utilities.general.ConsoleLogger;

/**
 * manages project creation
 *
 * @author keunecke
 */
public class ProjectSupport {

    private static final ConsoleLogger logger = new ConsoleLogger(Activator.getDefault().getLog(), Activator.PLUGIN_ID);

    private static final String[] PATHS = { "src/java", "src/test", "src/generated", "resource", ".settings" };
    
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
		IProject project = createBaseProject(projectName, location);
		
		try {
            addNatures(project, projectName);
            addToProjectStructure(project, PATHS);
            setSourceFolders(project, PATHS);
            addProjectDependencies(project, choices.getProjectsToReference());
    		setJreEnvironment(project);
        } catch (CoreException e) {
            logger.error2("Exception creating new project '" + projectName + "': " + e.getMessage(), e);
            project = null; // XXX: This may cause an NPE somewhere else, like in TemplateManager.writeContent()
        }

		return project;
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
		IClasspathEntry containerEntry = JavaCore.newContainerEntry(new Path("org.eclipse.jdt.launching.JRE_CONTAINER"), false);
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
	public void addProjectDependencies(IProject project, Collection<String> referencedProjects) {
	    try {
    		IJavaProject javaProject = createJavaProject(project);
    		for (String referencedProject : referencedProjects) {
    			IPath path = new Path("/" + referencedProject);
    			IClasspathEntry projectEntry = JavaCore.newProjectEntry(path);
    			IClasspathEntry[] oldClassPath = javaProject.getRawClasspath();
    			ArrayList<IClasspathEntry> list = new ArrayList<IClasspathEntry>(Arrays.asList(oldClassPath));
    			list.add(projectEntry);
    			javaProject.setRawClasspath(list.toArray(new IClasspathEntry[0]), null);
    		}
	    } catch (JavaModelException e) {
    		logger.error2(e.getMessage(), e);
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
     */
	public IProject createBaseProject(String name, URI location) {
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(name);
		if (!project.exists()) {
			IProjectDescription desc = project.getWorkspace().newProjectDescription(project.getName());
			URI workspaceLocation = ResourcesPlugin.getWorkspace().getRoot().getLocationURI();
			URI projectLocation = location;
			if (location != null && location.equals(workspaceLocation)) {
				projectLocation = null;
			}
			desc.setLocationURI(projectLocation);
			try {
				project.create(desc, null);
				if (!project.isOpen()) {
					project.open(null);
				}
			} catch (CoreException e) {
	    		logger.error2(e.getMessage(), e);
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

    /**
     * Add natures to the project
     * @param project
     * @param projectName
     * @throws CoreException
     */
	public void addNatures(IProject project, String projectName) throws CoreException {
		addNature(project, projectName, ProjectNature.JAVA);
        addNature(project, projectName, ProjectNature.ECL1);
        addNature(project, projectName, ProjectNature.MACKER);
	}

	private void addNature(IProject project, String projectName, ProjectNature nature) throws CoreException {
		String natureStr = nature.getNature();
		if (!project.hasNature(natureStr)) {
			IProjectDescription description = project.getDescription();
			String[] prevNatures = description.getNatureIds();
			String[] newNatures = new String[prevNatures.length + 1];
			System.arraycopy(prevNatures, 0, newNatures, 0, prevNatures.length);
			newNatures[prevNatures.length] = natureStr;
			// Check if the new nature is known in the workspace. Otherwise (e.g. if we'ld try to add the Macker nature
			// without having Eclipse Macker installed) we'ld get an Eclipse CoreException thrown by project.setDescription()
			IStatus status = project.getWorkspace().validateNatureSet(newNatures);
			if (status.getCode() == IStatus.OK) {
				description.setNatureIds(newNatures);
				IProgressMonitor monitor = null; // here we could create a proper progress monitor
				project.setDescription(description, monitor);
			} else {
				logger.error2("Project nature '" + natureStr + "' could not be added. Probably it is not supported in the workspace.");
			}
		}
	}
}
