package net.sf.ecl1.classpath;

import java.util.ArrayList;
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

import net.sf.ecl1.utilities.general.ConsoleLogger;
import net.sf.ecl1.utilities.hisinone.HisConstants;

/**
 * Utility for manipulation of the runtime classpath.
 * @author TNeumann
 */
public class RuntimeClasspathUtil {
    private static final ConsoleLogger logger = new ConsoleLogger(Activator.getDefault().getLog(), Activator.PLUGIN_ID);

	public void addAllExtensionsToRuntimeClasspath(IJavaProject javaProject, ArrayList<IRuntimeClasspathEntry> runtimeClasspath) {
        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		Map<String, String> extensions = ExtensionUtil.getInstance().findAllExtensions();
		if (extensions!=null && extensions.size()>0) {
			for (Map.Entry<String, String> extensionEntry : extensions.entrySet()) {
				String simpleExtensionPath = extensionEntry.getValue();
                if (simpleExtensionPath.endsWith(".jar")) {
					// TODO exclude non-Java jars
                	IPath fullExtensionPath = javaProject.getPath().append(HisConstants.EXTENSIONS_FOLDER).append(simpleExtensionPath);
					logger.debug("Add extension jar " + fullExtensionPath + " to runtime classpath");
					IRuntimeClasspathEntry extensionRuntimeClasspathEntry = createRuntimeClasspathEntry(fullExtensionPath);
					//logger.debug("extensionRuntimeClasspathEntry = " + extensionRuntimeClasspathEntry);
					runtimeClasspath.add(extensionRuntimeClasspathEntry);
                } else {
                	// if an extension is added as an extension project, it's dependencies must be added, too
					// TODO exclude non-Java projects
                    IProject extensionProject = root.getProject(simpleExtensionPath);
					logger.debug("Add extension project " + extensionProject.getFullPath() + " to runtime classpath...");
					addJavaProjectToRuntimeClasspath(JavaCore.create(extensionProject), runtimeClasspath);
                }
			}
		}
	}
	
	public void addJavaProjectToRuntimeClasspath(IJavaProject javaProject, ArrayList<IRuntimeClasspathEntry> runtimeClasspath) {
		try {
			// initialize runtime classpath with the output folder
			IPath outputFolder = javaProject.getOutputLocation();
			logger.debug("Add output folder " + outputFolder + " to runtime classpath");
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
		} catch (CoreException e) {
			logger.error("Adding project " + javaProject + " to the runtime classpath caused an exception: " + e, e);
		}
	}
	
	public IRuntimeClasspathEntry createRuntimeClasspathEntry(IPath path) {
		ClasspathEntry classpathEntry = new ClasspathEntry(IPackageFragmentRoot.K_BINARY, IClasspathEntry.CPE_LIBRARY, path, new IPath[] {}, new IPath[] {}, null, null, null, false, new IAccessRule[] {}, false, new IClasspathAttribute[] {});
		return new RuntimeClasspathEntry(classpathEntry);
	}
	
	public IRuntimeClasspathEntry resolveClasspathVariable(IRuntimeClasspathEntry unresolvedEntry) {
		IClasspathEntry resolvedEntry = JavaCore.getResolvedClasspathEntry(unresolvedEntry.getClasspathEntry());
		return new RuntimeClasspathEntry(resolvedEntry);
	}
	
	public ArrayList<IRuntimeClasspathEntry> resolveClasspathContainer(IRuntimeClasspathEntry unresolvedEntry, IJavaProject javaProject) {
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
