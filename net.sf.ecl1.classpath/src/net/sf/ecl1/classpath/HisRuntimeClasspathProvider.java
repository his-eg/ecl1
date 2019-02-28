package net.sf.ecl1.classpath;

import java.util.ArrayList;
import java.util.Arrays;

import org.eclipse.core.resources.IProject;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry;
import org.eclipse.jdt.launching.IRuntimeClasspathProvider;

import net.sf.ecl1.utilities.general.ConsoleLogger;
import net.sf.ecl1.utilities.general.ProjectUtil;
import net.sf.ecl1.utilities.general.RuntimeClasspathUtil;
import net.sf.ecl1.utilities.hisinone.WebappsUtil;

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
    @Override
	public IRuntimeClasspathEntry[] computeUnresolvedClasspath(ILaunchConfiguration launchConfig) {
		logger.info("Compute unresolved classpath for launch configuration " + launchConfig + "...");
		IJavaProject javaProject = ProjectUtil.getJavaProjectForLaunchConfiguration(launchConfig);
		if (javaProject == null) {
			return new IRuntimeClasspathEntry[] {};
		}
		
		ArrayList<IRuntimeClasspathEntry> runtimeClasspath = new ArrayList<>();
		// add the project containing the JUnit test
		RuntimeClasspathUtil.addJavaProjectToRuntimeClasspath(javaProject, runtimeClasspath);
		// if the project containing the JUnit test is an extension project then we must add the webapps project, too
		if (!WebappsUtil.isWebapps(javaProject.getProject())) {
			IProject webappsProject = WebappsUtil.findWebappsProject(); // TODO retrieve from ExtensionUtil
			if (webappsProject != null) {
				RuntimeClasspathUtil.addJavaProjectToRuntimeClasspath(JavaCore.create(webappsProject), runtimeClasspath);
			}
		}
		// add Java extensions to runtime classpath
		RuntimeClasspathUtil.addAllExtensionsToRuntimeClasspath(javaProject, runtimeClasspath);
		
		return runtimeClasspath.toArray(new IRuntimeClasspathEntry[runtimeClasspath.size()]);
	}
	
	/**
	 * Resolve the given runtime classpath entries, i.e. replace classpath variables and/or containers by real paths.
	 * @param classpathEntries
	 * @param launchConfig
	 * @return array of runtime classpath entries
	 */
    @Override
	public IRuntimeClasspathEntry[] resolveClasspath(IRuntimeClasspathEntry[] classpathEntries, ILaunchConfiguration launchConfig) {
		logger.info("Resolve runtime classpath for launch configuration " + launchConfig + "...");
		logger.debug("Unresolved classpath entries: " + Arrays.toString(classpathEntries));
		if (classpathEntries==null || classpathEntries.length==0) {
			return new IRuntimeClasspathEntry[] {};
		}
		
		IJavaProject javaProject = ProjectUtil.getJavaProjectForLaunchConfiguration(launchConfig);

		ArrayList<IRuntimeClasspathEntry> resolvedEntries = new ArrayList<>();
		for (IRuntimeClasspathEntry unresolvedEntry : classpathEntries) {
			int entryKind = unresolvedEntry.getClasspathEntry().getEntryKind();
			//logger.debug("unresolvedEntry = " + unresolvedEntry + ", entryKind=" + entryKind);
			switch (entryKind) {
			case IClasspathEntry.CPE_PROJECT: // XXX Needs a different handling?
			case IClasspathEntry.CPE_LIBRARY:
				resolvedEntries.add(unresolvedEntry);
				break;
			case IClasspathEntry.CPE_VARIABLE:
				resolvedEntries.add(RuntimeClasspathUtil.resolveClasspathVariable(unresolvedEntry));
				break;
			case IClasspathEntry.CPE_CONTAINER:
				resolvedEntries.addAll(RuntimeClasspathUtil.resolveClasspathContainer(unresolvedEntry, javaProject));
			default:
				// src entries should already have been skipped in computeUnresolvedClasspath()
				logger.warn("Launch configuration " + launchConfig + ": Unexpected classpath entry kind " + entryKind + " found while resolving the classpath. This entry will be ignored...");
			}
		}
		logger.info("Resolved runtime classpath with " + resolvedEntries.size() + " elements.");
		logger.debug("Resolved classpath entries: " + resolvedEntries);
		return resolvedEntries.toArray(new IRuntimeClasspathEntry[resolvedEntries.size()]);
	}
}
