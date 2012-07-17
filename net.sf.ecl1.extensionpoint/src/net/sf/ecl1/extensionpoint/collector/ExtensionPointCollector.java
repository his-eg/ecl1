/**
 * 
 */
package net.sf.ecl1.extensionpoint.collector;

import java.util.Collection;
import java.util.LinkedList;

import net.sf.ecl1.extensionpoint.collector.model.ExtensionPointInformation;

import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMemberValuePair;
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
	
	private Collection<ExtensionPointInformation> found;
	
	private IJavaProject projectUnderScan;

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
		System.out.println("Project: " + project.getElementName());
		this.found = new LinkedList<ExtensionPointInformation>();
		this.projectUnderScan = project;
		try {
			IPackageFragment[] fragmentRoots = project.getPackageFragments();
			for (IPackageFragment iPackageFragment : fragmentRoots) {
				if(IPackageFragmentRoot.K_SOURCE == iPackageFragment.getKind()) {
					scanPackage(iPackageFragment);
				}
			}
		} catch (JavaModelException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		ExtensionPointManager.addExtensions(project.getElementName(), this.found);
		System.out.println("Finished Extension Point Collection");
	}

	private void scanPackage(IPackageFragment iPackageFragment) throws JavaModelException, ClassNotFoundException {
		ICompilationUnit[] units = iPackageFragment.getCompilationUnits();
		for (ICompilationUnit unit : units) {
			IType[] types = unit.getAllTypes();
			for (IType type : types) {
				scanType(type);
			}
		}
	}

	private void scanType(IType type) throws JavaModelException, ClassNotFoundException {
		IAnnotation[] annotations = type.getAnnotations();
		for (IAnnotation annotation : annotations) {
			String elementName = annotation.getElementName();
			if(EXTENSION_POINT_ANNOTATION_NAME.equals(elementName)) {
				handleAnnotation(annotation);
			}
		}
	}

	private void handleAnnotation(IAnnotation annotation)
			throws JavaModelException, ClassNotFoundException {
		System.out.println("Found " + annotation.getElementName());
		IMemberValuePair[] pairs = annotation.getMemberValuePairs();
		String id = null;
		String name = null;
		String iface = null;
		for (IMemberValuePair pair : pairs) {
			if(pair.getMemberName().equals("id")) {
				id = (String) pair.getValue();
			}
			if(pair.getMemberName().equals("name")) {
				name = (String) pair.getValue();
			}
			if(pair.getMemberName().equals("extensionInterface")) {
				if(pair.getValueKind() == IMemberValuePair.K_CLASS) {
					String ifaceName = (String) pair.getValue();
					IType findType = projectUnderScan.findType(ifaceName);
					iface = findType.getFullyQualifiedName();
				}
			}
		}
		if(id != null && name != null && iface != null) {
			ExtensionPointInformation e = new ExtensionPointInformation(id, name, iface);
			System.out.println(e);
			found.add(e);
		} else {
			System.err.println("Extension Information incomplete: " + annotation.getSource());
		}
	}

}
