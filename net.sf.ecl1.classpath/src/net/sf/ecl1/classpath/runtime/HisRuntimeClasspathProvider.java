package net.sf.ecl1.classpath.runtime;

import java.util.Arrays;
import java.util.LinkedHashSet;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry;
import org.eclipse.jdt.launching.IRuntimeClasspathProvider;

import net.sf.ecl1.classpath.Activator;
import net.sf.ecl1.utilities.general.ConsoleLogger;
import net.sf.ecl1.utilities.general.ProjectUtil;
import net.sf.ecl1.utilities.general.RuntimeClasspathUtil;
import net.sf.ecl1.utilities.hisinone.ExtensionUtil;
import net.sf.ecl1.utilities.hisinone.WebappsUtil;

/**
 * A custom runtime classpath provider. Currently only used for JUnit launch configurations.
 * @author TNeumann
 */
public class HisRuntimeClasspathProvider implements IRuntimeClasspathProvider {
    private static final ConsoleLogger logger = new ConsoleLogger(Activator.getDefault().getLog(), Activator.PLUGIN_ID, HisRuntimeClasspathProvider.class.getSimpleName());

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
		
		LinkedHashSet<IRuntimeClasspathEntry> runtimeClasspath = new LinkedHashSet<>();
		
		// Add the Java project the JUnit test class belongs to, and all its classpath dependencies.
		// If the JUnit test is part of a HisInOne extension, webapps will be a dependency.
		IJavaProject javaProject = ProjectUtil.getJavaProjectForLaunchConfiguration(launchConfig);
		if(javaProject == null ) {
			logger.info("No java project was found for the following launch Configuration: " + launchConfig
					   + "\nThus no runtime classpath can be calculated. ");
			return runtimeClasspath.toArray(new IRuntimeClasspathEntry[runtimeClasspath.size()]);
		}
		RuntimeClasspathUtil.addJavaProjectToRuntimeClasspath(javaProject, runtimeClasspath);
		if (WebappsUtil.isWebapps(javaProject.getProject())) {
			// Add all Java extensions from webapps to the runtime classpath
			RuntimeClasspathUtil.addAllExtensionsToRuntimeClasspath(javaProject, runtimeClasspath);
		}
		
		/* 
		 * Fixes #253541 
		 * When starting a HIS-unit-test with Java <=8 , the HIS-unit-test was able to add the extension-projects (either as jars or
		 * as a workspace-project-folder) to the classpath itself. Because the old Java-VM-classloader was deprecated in Java 9, 
		 * the HIS-unit-test is no longer capable of adding the extensions-projects itself, and therefore ecl1 must do it instead. 
		 */
		if(ExtensionUtil.getInstance().isExtensionProject(javaProject.getProject())) {
			logger.info("Enriching classpath of the following extension project: " + javaProject.getElementName());
			//First parameter must be webapps as a IJavaproject and NOT the extension project itself
			RuntimeClasspathUtil.addAllExtensionsToRuntimeClasspath(JavaCore.create(WebappsUtil.findWebappsProject()), runtimeClasspath);
		}
		
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

		LinkedHashSet<IRuntimeClasspathEntry> resolvedEntries = new LinkedHashSet<>();
		for (IRuntimeClasspathEntry unresolvedEntry : classpathEntries) {
			RuntimeClasspathUtil.addCompileClasspathEntryToRuntimeClasspath(unresolvedEntry.getClasspathEntry(), javaProject, resolvedEntries);
		}
		logger.info("Resolved runtime classpath with " + resolvedEntries.size() + " elements.");
		logger.debug("Resolved classpath entries: " + resolvedEntries);
		return resolvedEntries.toArray(new IRuntimeClasspathEntry[resolvedEntries.size()]);
	}
}
