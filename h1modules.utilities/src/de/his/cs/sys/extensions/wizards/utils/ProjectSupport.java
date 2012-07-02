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
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

/**
 * manages project creation
 *
 * @author keunecke
 */
public class ProjectSupport {

	/**
	 * @param projects the projects to reference
	 */
	public ProjectSupport() {
	}

	/**
	 * creates a new project with a skeleton
	 *
	 * @param projectName name of project
	 * @param location location
	 * @return IProject instance
	 */
	public IProject createProject(String projectName, URI location) {
		Assert.isNotNull(projectName);
		Assert.isTrue(projectName.trim().length() > 0);

		IProject project = createBaseProject(projectName, location);

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
		IClasspathEntry containerEntry = JavaCore.newContainerEntry(new Path("org.eclipse.jdt.launching.JRE_CONTAINER"), false);
		IClasspathEntry[] oldClassPath = javaProject.getRawClasspath();
		ArrayList<IClasspathEntry> list = new ArrayList<IClasspathEntry>(Arrays.asList(oldClassPath));
		list.add(containerEntry);
		javaProject.setRawClasspath(list.toArray(new IClasspathEntry[0]), null);
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
	        e.printStackTrace();
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
	 * @return
	 */
	public IProject createBaseProject(String name, URI location) {
		IProject project = ResourcesPlugin.getWorkspace().getRoot()
				.getProject(name);
		if (!project.exists()) {
			IProjectDescription desc = project.getWorkspace()
					.newProjectDescription(project.getName());
			URI workspaceLocation = ResourcesPlugin.getWorkspace().getRoot()
					.getLocationURI();
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
				e.printStackTrace();
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
		createFolder(newProject.getFolder("/src/java/" + newProject.getName().replace('.', '/')));
	}

	public void addNatures(IProject project) throws CoreException {
		addNature(project, ProjectNature.JAVA);
//		addNature(project, ProjectNature.MACKER);
	}

	private void addNature(IProject project, ProjectNature nature) throws CoreException {
		if (!project.hasNature(nature.getNature())) {
			IProjectDescription description = project.getDescription();
			String[] prevNatures = description.getNatureIds();
			String[] newNatures = new String[prevNatures.length + 1];
			System.arraycopy(prevNatures, 0, newNatures, 0, prevNatures.length);
			newNatures[prevNatures.length] = nature.getNature();
			description.setNatureIds(newNatures);
			IProgressMonitor monitor = null;
			project.setDescription(description, monitor);
		}
	}

}
