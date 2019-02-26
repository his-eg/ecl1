package net.sf.ecl1.classpath;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.debug.core.ILaunchConfiguration;
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
import org.eclipse.jdt.launching.IRuntimeClasspathProvider;

import net.sf.ecl1.utilities.general.ConsoleLogger;
import net.sf.ecl1.utilities.hisinone.HisConstants;

/**
 * A custom runtime classpath provider. Currently only used for JUnit launch configurations.
 * @author TNeumann
 */
public class HisRuntimeClasspathProvider implements IRuntimeClasspathProvider {
    private static final ConsoleLogger logger = new ConsoleLogger(Activator.getDefault().getLog(), Activator.PLUGIN_ID);

    public static final String CLASSPATH_PROVIDER_EXTENSION_ID = "net.sf.ecl1.HisRuntimeClasspathProvider";

	/**
	 * Compute the unresolved runtime classpath for the given launch configuration.
	 * Unresolved means it may still contain classpath variables and/or containers.
	 * @param launchConfig
	 * @return array of runtime classpath entries
	 */
	public IRuntimeClasspathEntry[] computeUnresolvedClasspath(ILaunchConfiguration launchConfig) {
		logger.info("Compute unresolved classpath for launch configuration " + launchConfig + "...");
		IJavaProject javaProject = getJavaProjectForLaunchConfiguration(launchConfig);
		if (javaProject == null) {
			return new IRuntimeClasspathEntry[] {};
		}
		
		try {
			// initialize runtime classpath with the output folder
			IPath outputFolder = javaProject.getOutputLocation();
			logger.debug("output folder = " + outputFolder);
			ArrayList<IRuntimeClasspathEntry> runtimeClasspath = new ArrayList<>();
			IRuntimeClasspathEntry outputFolderRuntimeClasspathEntry = createRuntimeClasspathEntry(outputFolder);
			runtimeClasspath.add(outputFolderRuntimeClasspathEntry);
			
			// get compile classpath from Java project and convert it into a runtime classpath
			IClasspathEntry[] compileClasspath = javaProject.getRawClasspath();
			for (IClasspathEntry compileClasspathEntry : compileClasspath) {
				// Compile classpath entries with content kind IPackageFragmentRoot.K_SOURCE must not be added to the runtime classpath
				int contentKind = compileClasspathEntry.getContentKind(); // K_SOURCE=1, K_BINARY=2
				//int entryKind = compileClasspathEntry.getEntryKind(); // CPE_LIBRARY=1, CPE_PROJECT=2, CPE_SOURCE=3, CPE_VARIABLE=4, CPE_CONTAINER=5
				//logger.debug("contentKind=" + contentKind + ", entryKind=" + entryKind);
				if (contentKind == IPackageFragmentRoot.K_BINARY) {
					IRuntimeClasspathEntry runtimeClasspathEntry = new RuntimeClasspathEntry(compileClasspathEntry);
					runtimeClasspath.add(runtimeClasspathEntry);
					logger.debug("Add compile classpath entry " + compileClasspathEntry + " to runtime classpath");
				} else {
					logger.debug("Skip compile classpath entry " + compileClasspathEntry);
				}
			}

			// add Java extensions to runtime classpath
            IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
			Map<String, String> extensions = ExtensionUtil.getInstance().findAllExtensions(javaProject);
			if (extensions!=null && extensions.size()>0) {
				for (Map.Entry<String, String> extensionEntry : extensions.entrySet()) {
					String simpleExtensionPath = extensionEntry.getValue();
					IPath fullExtensionPath;
	                if (simpleExtensionPath.endsWith(".jar")) {
	                    fullExtensionPath = javaProject.getPath().append(HisConstants.EXTENSIONS_FOLDER).append(simpleExtensionPath);
						logger.debug("Add extension jar " + fullExtensionPath + " to runtime classpath");
	                } else {
	                    IProject extensionProject = root.getProject(simpleExtensionPath);
	                    fullExtensionPath = extensionProject.getFullPath();
						logger.debug("Add extension project " + fullExtensionPath + " to runtime classpath");
	                }
					// TODO exclude non-Java projects / jars
					IRuntimeClasspathEntry extensionRuntimeClasspathEntry = createRuntimeClasspathEntry(fullExtensionPath);
					//logger.debug("extensionRuntimeClasspathEntry = " + extensionRuntimeClasspathEntry);
					runtimeClasspath.add(extensionRuntimeClasspathEntry);
				}
			}
			return runtimeClasspath.toArray(new IRuntimeClasspathEntry[runtimeClasspath.size()]);
		} catch (CoreException e) {
			logger.error("Caught exception while computing the unresolved classpath for launch configuration " + launchConfig + ": " + e, e);
			return new IRuntimeClasspathEntry[] {};
		}
	}
	
	/**
	 * Resolve the given runtime classpath entries, i.e. replace classpath variables and/or containers by real paths.
	 * @param classpathEntries
	 * @param launchConfig
	 * @return array of runtime classpath entries
	 */
	public IRuntimeClasspathEntry[] resolveClasspath(IRuntimeClasspathEntry[] classpathEntries, ILaunchConfiguration launchConfig) {
		logger.info("Resolve runtime classpath for launch configuration " + launchConfig + "...");
		logger.debug("Unresolved classpath entries: " + Arrays.toString(classpathEntries));
		if (classpathEntries==null || classpathEntries.length==0) {
			return new IRuntimeClasspathEntry[] {};
		}
		
		IJavaProject javaProject = getJavaProjectForLaunchConfiguration(launchConfig);

		ArrayList<IRuntimeClasspathEntry> resolvedEntries = new ArrayList<>();
		for (IRuntimeClasspathEntry unresolvedEntry : classpathEntries) {
			int entryKind = unresolvedEntry.getClasspathEntry().getEntryKind();
			//logger.debug("unresolvedEntry = " + unresolvedEntry + ", entryKind=" + entryKind);
			switch (entryKind) {
			case IClasspathEntry.CPE_PROJECT:
			case IClasspathEntry.CPE_LIBRARY:
				resolvedEntries.add(unresolvedEntry);
				break;
			case IClasspathEntry.CPE_VARIABLE:
				resolvedEntries.add(resolveClasspathVariable(unresolvedEntry));
				break;
			case IClasspathEntry.CPE_CONTAINER:
				resolvedEntries.addAll(resolveClasspathContainer(unresolvedEntry, javaProject));
			default:
				// src entries should already have been skipped in computeUnresolvedClasspath()
				logger.warn("Launch configuration " + launchConfig + ": Unexpected classpath entry kind " + entryKind + " found while resolving the classpath. This entry will be ignored...");
			}
		}
		logger.info("Resolved runtime classpath with " + resolvedEntries.size() + " elements.");
		logger.debug("Resolved classpath entries: " + resolvedEntries);
		return resolvedEntries.toArray(new IRuntimeClasspathEntry[resolvedEntries.size()]);
	}

	private IJavaProject getJavaProjectForLaunchConfiguration(ILaunchConfiguration launchConfig) {
		try {
			IResource[] mappedResources = launchConfig.getMappedResources();
			if (mappedResources==null || mappedResources.length==0) {
				logger.error("The launch configuration " + launchConfig + " has no mapped resources -> cannot find the project");
				return null;
			} else {
				logger.debug("mappedResources = " + Arrays.toString(mappedResources));
			}
			// XXX make sure there is exactly one mapped resource?
			IResource mappedResource = mappedResources[0];
			logger.debug("mappedResources[0] = " + mappedResource);
			IProject project = mappedResource.getProject();
			logger.debug("project = " + project);
			IJavaProject javaProject = JavaCore.create(project);
			logger.info("The launch configuration " + launchConfig + " belongs to java project " + javaProject.getElementName());
			return javaProject;
		} catch (CoreException e) {
			logger.error("Looking for the Java project of launch configuration " + launchConfig + " caused exception " + e, e);
			return null;
		}

	}
	
	private IRuntimeClasspathEntry createRuntimeClasspathEntry(IPath path) {
		ClasspathEntry classpathEntry = new ClasspathEntry(IPackageFragmentRoot.K_BINARY, IClasspathEntry.CPE_LIBRARY, path, new IPath[] {}, new IPath[] {}, null, null, null, false, new IAccessRule[] {}, false, new IClasspathAttribute[] {});
		return new RuntimeClasspathEntry(classpathEntry);
	}
	
	private IRuntimeClasspathEntry resolveClasspathVariable(IRuntimeClasspathEntry unresolvedEntry) {
		IClasspathEntry resolvedEntry = JavaCore.getResolvedClasspathEntry(unresolvedEntry.getClasspathEntry());
		return new RuntimeClasspathEntry(resolvedEntry);
	}
	
	private ArrayList<IRuntimeClasspathEntry> resolveClasspathContainer(IRuntimeClasspathEntry unresolvedEntry, IJavaProject javaProject) {
		ArrayList<IRuntimeClasspathEntry> result = new ArrayList<>();
		IClasspathContainer resolvedContainer = null;
		try {
			resolvedContainer = JavaCore.getClasspathContainer(unresolvedEntry.getPath(), javaProject);
		} catch (JavaModelException e) {
			logger.error("Resolving the classpath container " + unresolvedEntry + " failed with exception " + e, e);
		}
		IClasspathEntry[] containerEntries = resolvedContainer!=null ? resolvedContainer.getClasspathEntries() : null;
		if (containerEntries==null || containerEntries.length==0) {
			return result;
		}
		for (IClasspathEntry containerEntry : containerEntries) {
			result.add(new RuntimeClasspathEntry(containerEntry));
		}
		return result;
	}
}
