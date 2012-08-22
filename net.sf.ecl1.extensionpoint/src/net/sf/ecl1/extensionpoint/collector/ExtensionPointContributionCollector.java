package net.sf.ecl1.extensionpoint.collector;

import java.util.Collection;
import java.util.LinkedList;

import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
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
	private static final String EXTENSION_ANNOTATION_NAME = "Extension";
	
	private Collection<String> contributingClasses = new LinkedList<String>();

	@Override
	public int aboutToBuild(IJavaProject project) {
		return super.aboutToBuild(project);
	}

	@Override
	public void buildFinished(IJavaProject project) {
		try {
			IPackageFragment[] fragmentRoots = project.getPackageFragments();
			for (IPackageFragment iPackageFragment : fragmentRoots) {
				if(IPackageFragmentRoot.K_SOURCE == iPackageFragment.getKind()) {
					scanPackage(iPackageFragment);
				}
			}
		} catch (JavaModelException e) {
			e.printStackTrace();
		}
		super.buildFinished(project);
	}

	private void scanPackage(IPackageFragment iPackageFragment) throws JavaModelException {
		ICompilationUnit[] units = iPackageFragment.getCompilationUnits();
		for (ICompilationUnit unit : units) {
			IType[] types = unit.getAllTypes();
			for (IType type : types) {
				scanType(type);
			}
		}
	}

	private void scanType(IType type) throws JavaModelException {
		IAnnotation[] annotations = type.getAnnotations();
		for (IAnnotation annotation : annotations) {
			String elementName = annotation.getElementName();
			if(EXTENSION_ANNOTATION_NAME.equals(elementName)) {
				this.contributingClasses.add(type.getFullyQualifiedName());
			}
		}
	}

	@Override
	public boolean isActive(IJavaProject project) {
		boolean containsExtensionAntProperties = false;
		IFile file = project.getProject().getFile(EXTENSION_ANT_PROPERTIES);
		containsExtensionAntProperties = file.exists();
		return containsExtensionAntProperties;
	}

}
