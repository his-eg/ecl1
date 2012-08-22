package net.sf.ecl1.extensionpoint.collector;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.compiler.CompilationParticipant;

public class ExtensionPointContributionCollector extends CompilationParticipant {

	@Override
	public int aboutToBuild(IJavaProject project) {
		return super.aboutToBuild(project);
	}

	@Override
	public void buildFinished(IJavaProject project) {
		// TODO Auto-generated method stub
		super.buildFinished(project);
	}

	@Override
	public boolean isActive(IJavaProject project) {
		boolean containsExtensionAntProperties = false;
		IFile file = project.getProject().getFile("extension.ant.properties");
		containsExtensionAntProperties = file.exists();
		return containsExtensionAntProperties;
	}

}
