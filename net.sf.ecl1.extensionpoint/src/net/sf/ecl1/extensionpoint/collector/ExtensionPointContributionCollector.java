package net.sf.ecl1.extensionpoint.collector;

import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.compiler.CompilationParticipant;

/**
 * This class collects all contributions to extension points from projects, that have a file
 * extension.ant.properties in their project root.
 * 
 * @author keunecke
 *
 */
public class ExtensionPointContributionCollector extends CompilationParticipant {

	private static final String EXTENSION_ANT_PROPERTIES = "extension.ant.properties";

	@Override
	public int aboutToBuild(IJavaProject project) {
		return super.aboutToBuild(project);
	}

	@Override
	public void buildFinished(IJavaProject project) {
		// TODO 
		super.buildFinished(project);
	}

	@Override
	public boolean isActive(IJavaProject project) {
		boolean containsExtensionAntProperties = false;
		IFile file = project.getProject().getFile(EXTENSION_ANT_PROPERTIES);
		containsExtensionAntProperties = file.exists();
		return containsExtensionAntProperties;
	}

}
