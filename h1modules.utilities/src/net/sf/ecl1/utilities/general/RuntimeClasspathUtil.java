package net.sf.ecl1.utilities.general;

import java.util.LinkedHashSet;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IAccessRule;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.ClasspathEntry;
import org.eclipse.jdt.internal.launching.RuntimeClasspathEntry;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry;

import h1modules.utilities.utils.Activator;
import net.sf.ecl1.utilities.hisinone.ExtensionUtil;
import net.sf.ecl1.utilities.hisinone.HisConstants;

/**
 * Utility for manipulation of the runtime classpath.
 * @author TNeumann
 */
public class RuntimeClasspathUtil {
    private static final ConsoleLogger logger = new ConsoleLogger(Activator.getDefault().getLog(), Activator.PLUGIN_ID, RuntimeClasspathUtil.class.getSimpleName());

    private static final String JRE_CONTAINER_PREFIX = "org.eclipse.jdt.launching.JRE_CONTAINER";

    /**
     * If available, add all extension projects to the runtime classpath.
     * @param javaProject The project containing the extensions
     * @param runtimeClasspath the runtime classpath to update
     */
	public static void addAllExtensionsToRuntimeClasspath(IJavaProject javaProject, LinkedHashSet<IRuntimeClasspathEntry> runtimeClasspath) {
        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		Map<String, String> extensions = ExtensionUtil.getInstance().findAllExtensions();
		if (extensions!=null && extensions.size()>0) {
			for (Map.Entry<String, String> extensionEntry : extensions.entrySet()) {
				String simpleExtensionPath = extensionEntry.getValue();
                if (simpleExtensionPath.endsWith(".jar")) {
					// TODO exclude non-Java jars
                	IPath fullExtensionPath = javaProject.getPath().append(HisConstants.EXTENSIONS_FOLDER).append(simpleExtensionPath);
					logger.debug("Add extension jar " + fullExtensionPath + " to runtime classpath");
					IRuntimeClasspathEntry extensionRuntimeClasspathEntry = createLibraryRuntimeClasspathEntry(fullExtensionPath);
					//logger.debug("extensionRuntimeClasspathEntry = " + extensionRuntimeClasspathEntry);
					runtimeClasspath.add(extensionRuntimeClasspathEntry);
                } else {
                	// if an extension is added as an extension project, it's dependencies must be added, too
                    IProject extensionProject = root.getProject(simpleExtensionPath);
                    if (ProjectUtil.isJavaProject(extensionProject)) {
                    	logger.debug("Add Java extension project " + extensionProject.getFullPath() + " to runtime classpath...");
                    	addJavaProjectToRuntimeClasspath(JavaCore.create(extensionProject), runtimeClasspath);
                    } else {
                    	logger.debug("Skip non-Java extension project " + extensionProject.getFullPath());
                    }
                }
			}
		}
	}
	
	/**
	 * Add a Java project including all its dependencies to the runtime classpath.
	 * 
	 * @param javaProject The java project to add
	 * @param runtimeClasspath the runtime classpath to update
	 */
	public static void addJavaProjectToRuntimeClasspath(IJavaProject javaProject, LinkedHashSet<IRuntimeClasspathEntry> runtimeClasspath) {
		try {
			// Has the project already been added? We check this using the output folder
			IPath outputFolder = javaProject.getOutputLocation();
			IRuntimeClasspathEntry outputFolderRuntimeClasspathEntry = createLibraryRuntimeClasspathEntry(outputFolder);
			if (runtimeClasspath.contains(outputFolderRuntimeClasspathEntry)) {
				logger.debug("Skip project " + javaProject.getElementName() + " which has been added to the runtime classpath before");
				return;
			}
			
			// If available then instrumented classes must be added first
			IPath classesInstrFolder = outputFolder.removeLastSegments(1).append("classes.instr");
			if (javaProject.getProject().exists(classesInstrFolder.removeFirstSegments(1))) {
				logger.debug("Add instrumented classes folder " + classesInstrFolder + " to runtime classpath");
				IRuntimeClasspathEntry runtimeClasspathEntry = createLibraryRuntimeClasspathEntry(classesInstrFolder);
				runtimeClasspath.add(runtimeClasspathEntry);
			} else {
				logger.debug("Project " + javaProject.getElementName() + " does not contain instrumented classes folder.");
			}
			
			// Next add the output folder to the runtime classpath so that patched classes override the unpatched library classes
			logger.debug("Add output folder " + outputFolder + " to runtime classpath");
			runtimeClasspath.add(outputFolderRuntimeClasspathEntry);
	
			// Add all other entries to the runtime classpath (typically libraries)
			IClasspathEntry[] compileClasspath = javaProject.getRawClasspath();
			for (IClasspathEntry compileClasspathEntry : compileClasspath) {
				if (compileClasspathEntry.getPath().equals(classesInstrFolder)) {
					logger.debug("Skip compile classpath entry " + compileClasspathEntry + " which has been added to the runtime classpath before");
				} else {
					addCompileClasspathEntryToRuntimeClasspath(compileClasspathEntry, javaProject, runtimeClasspath);
				}
			}
		} catch (CoreException e) {
			logger.error("Adding project " + javaProject.getElementName() + " to the runtime classpath caused an exception: " + e, e);
		}
	}
	
	/**
	 * Add an arbitrary compile classpath entry to the runtime classpath.
	 * This method is the core of the runtime classpath construction procedure.
	 * 
	 * @param compileClasspathEntry The compile classpath entry to add to the runtime classpath
	 * @param javaProject The java project containing the compile classpath entry
	 * @param runtimeClasspath  the runtime classpath to update
	 */
	public static void addCompileClasspathEntryToRuntimeClasspath(IClasspathEntry compileClasspathEntry, IJavaProject javaProject, LinkedHashSet<IRuntimeClasspathEntry> runtimeClasspath) {
		switch (compileClasspathEntry.getEntryKind()) {
		// CPE_LIBRARY=1, CPE_PROJECT=2, CPE_SOURCE=3, CPE_VARIABLE=4, CPE_CONTAINER=5
		case IClasspathEntry.CPE_LIBRARY:
			IRuntimeClasspathEntry runtimeClasspathEntry = createRuntimeClasspathEntry(compileClasspathEntry);
			if (!runtimeClasspath.contains(runtimeClasspathEntry)) {
				logger.debug("Add compile classpath entry of library kind " + compileClasspathEntry + " to runtime classpath");
				runtimeClasspath.add(runtimeClasspathEntry);
			} else {
				logger.debug("Ignore duplicate compile classpath entry " + compileClasspathEntry);
			}
			return;
		case IClasspathEntry.CPE_PROJECT:
			IPath path = compileClasspathEntry.getPath();
	        String projectName = path.lastSegment();
	        logger.debug("Add compile classpath entry of project kind to runtime classpath: path = " + path + ", projectName = " + projectName);
			IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
	        IJavaProject javaProject2 = JavaCore.create(project);
	        addJavaProjectToRuntimeClasspath(javaProject2, runtimeClasspath);
	        return;
		case IClasspathEntry.CPE_SOURCE:
	        logger.debug("Skip compile classpath entry of source kind: " + compileClasspathEntry);
			return; // skip source entries
		case IClasspathEntry.CPE_VARIABLE:
			IClasspathEntry resolvedVariable = JavaCore.getResolvedClasspathEntry(compileClasspathEntry);
			logger.debug("Resolved compile classpath variable " + compileClasspathEntry + " to " + resolvedVariable);
			addCompileClasspathEntryToRuntimeClasspath(resolvedVariable, javaProject, runtimeClasspath);
			return;
		case IClasspathEntry.CPE_CONTAINER:
			IPath containerPath = compileClasspathEntry.getPath();
			if (containerPath.toString().startsWith(JRE_CONTAINER_PREFIX)) {
				logger.debug("Skip JRE container...");
			} else {
				logger.debug("Add compile classpath container " + compileClasspathEntry);
				addClasspathContainerToRuntimeClasspath(compileClasspathEntry.getPath(), javaProject, runtimeClasspath);
			}
			return;
		}
	}
	
	public static IRuntimeClasspathEntry createLibraryRuntimeClasspathEntry(IPath path) {
		IClasspathEntry classpathEntry = new ClasspathEntry(IPackageFragmentRoot.K_BINARY, IClasspathEntry.CPE_LIBRARY, path, new IPath[] {}, new IPath[] {}, null, null, null, false, new IAccessRule[] {}, false, new IClasspathAttribute[] {});
		return createRuntimeClasspathEntry(classpathEntry);
	}
	
	public static IRuntimeClasspathEntry resolveClasspathVariable(IRuntimeClasspathEntry unresolvedEntry) {
		IClasspathEntry resolvedEntry = JavaCore.getResolvedClasspathEntry(unresolvedEntry.getClasspathEntry());
		return createRuntimeClasspathEntry(resolvedEntry);
	}
	
	/**
	 * Add a classpath container and all its contents to the runtime classpath.
	 * @param containerPath the path to the container
	 * @param javaProject The javaProject that has the container in its compile classpath
	 * @param runtimeClasspath the runtime classpath to update
	 */
	public static void addClasspathContainerToRuntimeClasspath(IPath containerPath, IJavaProject javaProject, LinkedHashSet<IRuntimeClasspathEntry> runtimeClasspath) {
		IClasspathContainer resolvedContainer = null;
		try {
			resolvedContainer = JavaCore.getClasspathContainer(containerPath, javaProject);
			logger.debug("Resolved classpath container " + resolvedContainer.getDescription());
		} catch (JavaModelException e) {
			logger.error("Resolving the classpath container " + containerPath + " failed with exception " + e, e);
		}
		IClasspathEntry[] containerEntries = resolvedContainer!=null ? resolvedContainer.getClasspathEntries() : null;
		if (containerEntries==null || containerEntries.length==0) {
			logger.debug("Resolved classpath container has no content");
			return;
		}
		for (IClasspathEntry containerEntry : containerEntries) {
			addCompileClasspathEntryToRuntimeClasspath(containerEntry, javaProject, runtimeClasspath);
		}
		return;
	}
	
	public static IRuntimeClasspathEntry createRuntimeClasspathEntry(IClasspathEntry compileClasspathEntry) {
		return new RuntimeClasspathEntry(compileClasspathEntry);
	}
}
