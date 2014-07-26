package net.sf.ecl1.classpath;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import com.google.common.base.Splitter;
/**
 * Classpath Container for working with HISinOne-Extensions
 * 
 * @author markus
 */
class ExtensionClassPathContainer implements
		IClasspathContainer {
	
	public static final String NET_SF_ECL1_ECL1_CONTAINER_ID = "net.sf.ecl1.ECL1_CONTAINER";

	public static final int K_EXTENSION = 99;
	
	public static final String EXTENSIONS_FOLDER = "qisserver/WEB-INF/extensions/";
	
	private final IJavaProject javaProject;
	
	private final Collection<String> extensionsForClassPath = new HashSet<>();

	/**
	 * @param javaProject
	 * @param containerPath 
	 */
	public ExtensionClassPathContainer(IJavaProject javaProject, IPath containerPath) {
		this.javaProject = javaProject;
		this.initializeExtensionsToIgnore(containerPath);
	}

	/**
	 * Initiali
	 * @param containerPath
	 */
	private void initializeExtensionsToIgnore(IPath containerPath) {
		if(containerPath != null && containerPath.segmentCount() == 2) {
			String commaSeparatedExtensionsToIgnore = containerPath.segment(1);
			if(commaSeparatedExtensionsToIgnore != null) {
				List<String> extensionsToIgnore = Splitter.on(",").splitToList(commaSeparatedExtensionsToIgnore);
				this.extensionsForClassPath.addAll(extensionsToIgnore);
			}
		}
	}

	@Override
	public IPath getPath() {
		return new Path(NET_SF_ECL1_ECL1_CONTAINER_ID);
	}

	@Override
	public int getKind() {
		return IClasspathEntry.CPE_CONTAINER;
	}

	@Override
	public String getDescription() {
		return "ecl1 Extensions Classpath Container";
	}

	@Override
	public IClasspathEntry[] getClasspathEntries() {
		Map<String, String> extensions = new HashMap<String, String>();
		
		scanForExtensionJars(javaProject, extensions);
		scanForExtensionProjects(extensions);
		
		ArrayList<IClasspathEntry> result = new ArrayList<>();
		
		//uniquely register extensions either as jar or as project
		for (Map.Entry<String, String> extension : extensions.entrySet()) {
			
			String extensionName = extension.getKey();
			String extensionPath = extension.getValue();
		
			boolean extensionNeedsToBeExported = extensionNeedsToBeAddedToClasspath(extensionName);
			if(extensionNeedsToBeExported) {
				if(extensionPath.endsWith(".jar")) {
					IPath path = javaProject.getPath().append(EXTENSIONS_FOLDER).append(extensionPath);
					//create a lib entry
					System.out.println("Creating new container entry for: " + path.toString() 
							+ " exported: " + extensionNeedsToBeExported);
					IClasspathEntry libraryEntry = JavaCore.newLibraryEntry(path, path, path, true);
					result.add(libraryEntry);
				} else {
					IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(extensionPath);
					System.out.println("Creating new container entry for project: " + project.getName() 
							+ " exported: " + extensionNeedsToBeExported);
					JavaCore.newProjectEntry(project.getLocation(), true);
				}
			}
		}
		return result.toArray(new IClasspathEntry[1]);
	}

	private boolean extensionNeedsToBeAddedToClasspath(String extensionName) {
		boolean configuredExtensionsEmpty = extensionsForClassPath.isEmpty();
		boolean extensionIsListed = extensionsForClassPath.contains(extensionName);
		return configuredExtensionsEmpty || extensionIsListed;
	}
	
	/**
	 * 
	 * @param javaProject
	 * @param extensions
	 */
	private void scanForExtensionProjects(Map<String, String> extensions) {
		//scan workspace for extension projects
		IWorkspaceRoot ws = ResourcesPlugin.getWorkspace().getRoot();
		List<IProject> projects = Arrays.asList(ws.getProjects(0));
		for (IProject project : projects) {
			if(isExtensionProject(project)) {
				extensions.put(project.getName(), project.getName());
			}
		}
	}
	
	private boolean isExtensionProject(IProject project) {
		IFile file = project.getFile("extension.ant.properties");
		return file.exists();
	}

	/**
	 * Scan the java project for jar files in the extensions folder
	 * 
	 * @param javaProject
	 * @param extensions
	 */
	private void scanForExtensionJars(IJavaProject javaProject,
			Map<String, String> extensions) {
		//scan workspace for extension jars
		IFolder extensionsFolder = javaProject.getProject().getFolder(EXTENSIONS_FOLDER);
		if(extensionsFolder.exists()) {
			//if there is an extensions folder, scan it
			IPath rawLocation = extensionsFolder.getRawLocation();
			List<File> extensionJars = Arrays.asList(rawLocation.toFile().listFiles(new FilenameFilter() {
				@Override
				public boolean accept(File dir, String name) {
					return name != null && name.endsWith("jar");
				}
			}));
			for (File extensionJar : extensionJars) {
				extensions.put(extensionJar.getName().replace(".jar", ""), extensionJar.getName());
			}
		}
	}
}