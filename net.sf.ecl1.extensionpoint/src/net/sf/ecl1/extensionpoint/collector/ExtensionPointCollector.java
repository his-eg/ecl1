/**
 * 
 */
package net.sf.ecl1.extensionpoint.collector;

import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.compiler.CompilationParticipant;

/**
 * @author keunecke
 *
 */
public class ExtensionPointCollector extends CompilationParticipant {

	private static final String EXTENSION_POINT_ANNOTATION_NAME = "ExtensionPoint";

	@Override
	public int aboutToBuild(IJavaProject project) {
		return super.aboutToBuild(project);
	}

	@Override
	public boolean isActive(IJavaProject project) {
		return true;
	}

	@Override
	public void buildFinished(IJavaProject project) {
		System.out.println("Starting Extension Point Collection");
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
		System.out.println("Finished Extension Point Collection");
	}

	private void scanPackage(IPackageFragment iPackageFragment) throws JavaModelException {
		ICompilationUnit[] units = iPackageFragment.getCompilationUnits();
		for (ICompilationUnit unit : units) {
			IType[] types = unit.getAllTypes();
			for (IType type : types) {
				System.out.println("Current Type: "+type.getElementName());
				scanType(type);
			}
		}
	}

	private void scanType(IType type) throws JavaModelException {
		IAnnotation[] annotations = type.getAnnotations();
		for (IAnnotation annotation : annotations) {
			String elementName = annotation.getElementName();
			if(EXTENSION_POINT_ANNOTATION_NAME.equals(elementName)) {
				System.out.println("Found " + elementName);
				System.out.println(annotation.getSource());
			}
		}
	}

}
