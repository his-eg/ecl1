/**
 * 
 */
package net.sf.ecl1.extensionpoint.collector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

import net.sf.ecl1.extensionpoint.collector.model.ExtensionPointInformation;
import net.sf.ecl1.extensionpoint.collector.util.ConsoleLoggingHelper;

import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMemberValuePair;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

/**
 * @author keunecke
 *
 */
public class ExtensionPointCollector {

	private static final String EXTENSION_POINT_ANNOTATION_NAME = "ExtensionPoint";
	
    private Collection<ExtensionPointInformation> found = new ArrayList<ExtensionPointInformation>();
	
    private ConsoleLoggingHelper logger;

    private IJavaProject project;

    public ExtensionPointCollector(IJavaProject project) {
        this.project = project;
        logger = new ConsoleLoggingHelper(project, this.getClass().getSimpleName());
    }

    public Collection<ExtensionPointInformation> extractExtensionInformation(Collection<IType> classes) {
        logger.logToConsole("Starting Extension Point Collection");
        this.found = new HashSet<ExtensionPointInformation>();
		try {
            for (IType iType : classes) {
                scanType(iType);
            }
		} catch (JavaModelException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
        logger.logToConsole("Finished Extension Point Collection");
        return this.found;
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
        logger.logToConsole("Found " + annotation.getElementName());
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
                    IType findType = project.findType(ifaceName);
                    if (findType != null) {
                        iface = findType.getFullyQualifiedName();
                    } else {
                        logger.logToConsole("Referenced Interface Type not found in project '" + this.project.getElementName() + "': '" + ifaceName + "'");
                    }
				}
			}
		}
		if(id != null && name != null && iface != null) {
			ExtensionPointInformation e = new ExtensionPointInformation(id, name, iface);
            logger.logToConsole(e.toString());
			found.add(e);
		} else {
            logger.logToConsole("Extension Information incomplete: " + annotation.getSource());
		}
	}

}
