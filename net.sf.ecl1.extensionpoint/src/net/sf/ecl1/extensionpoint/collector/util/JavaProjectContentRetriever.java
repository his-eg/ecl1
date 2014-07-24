package net.sf.ecl1.extensionpoint.collector.util;

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

/**
 * Utility class to retrieve certain parts out of a certain project
 * 
 * @author keunecke
 */
public class JavaProjectContentRetriever {

    private final IJavaProject project;

    /**
     * Create a new JavaProjectContentRetriever
     * @param project
     */
    public JavaProjectContentRetriever(IJavaProject project) {
        this.project = project;
    }

    /**
     * Get all classes out of a java project
     * @return collection of IType
     * @throws JavaModelException
     */
    public Collection<IType> getClasses() throws JavaModelException {
        Collection<IType> result = new ArrayList<IType>();
        IPackageFragment[] fragmentRoots = project.getPackageFragments();
        for (IPackageFragment iPackageFragment : fragmentRoots) {
            if (IPackageFragmentRoot.K_SOURCE == iPackageFragment.getKind()) {
                scanPackage(iPackageFragment, result);
            }
        }
        return result;
    }

    private void scanPackage(IPackageFragment iPackageFragment, Collection<IType> result) throws JavaModelException {
        ICompilationUnit[] units = iPackageFragment.getCompilationUnits();
        for (ICompilationUnit unit : units) {
            IType[] types = unit.getAllTypes();
            for (IType type : types) {
                result.add(type);
            }
        }
    }

}
