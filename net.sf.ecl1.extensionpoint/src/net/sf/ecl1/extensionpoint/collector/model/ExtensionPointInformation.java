package net.sf.ecl1.extensionpoint.collector.model;

import net.sf.ecl1.extensionpoint.Constants;
import net.sf.ecl1.extensionpoint.collector.util.ConsoleLoggingHelper;

import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.IMemberValuePair;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import com.google.common.base.Objects;


/**
 * Container for extension point information
 * 
 * @author keunecke
 */
public class ExtensionPointInformation {
	
	private final String id;
	
	private final String name;
	
	private final String iface;

    /**
     * Factory method for a new ExtensionPointInformation object
     * 
     * @param a the Annotation declaring the point
     * @param type the type containing the point
     * @return a new ExtensionPointInformation object
     * @throws JavaModelException
     */
    public static ExtensionPointInformation create(IAnnotation a, IType type) throws JavaModelException {
        ConsoleLoggingHelper logger = new ConsoleLoggingHelper(a.getJavaProject(), 
        														Constants.CONSOLE_NAME, 
        														Constants.LOGGING_PREFERENCE);
        String idValue = null;
        String nameValue = null;
        String ifaceValue = null;
        IMemberValuePair[] pairs = a.getMemberValuePairs();
        for (IMemberValuePair pair : pairs) {
            if ("id".equals(pair.getMemberName())) {
                if (pair.getValueKind() == IMemberValuePair.K_QUALIFIED_NAME) {
                    idValue = (String) pair.getValue();
                } else {
                    idValue = (String) pair.getValue();
                }
            }
            if ("extensionInterface".equals(pair.getMemberName())) {
                String ifaceSimpleName = (String) pair.getValue();
                String[][] resolveType = type.resolveType(ifaceSimpleName);
                if (resolveType != null && resolveType.length > 0) {
                    ifaceValue = resolveType[0][0] + "." + resolveType[0][1];
                } else {
                    logger.logToConsole("Missing information for extension point: \n" + a.getSource());
                    logger.logToConsole("Could not find referenced type from iface attribute: " + ifaceValue);
                }
            }
            if ("name".equals(pair.getMemberName())) {
                nameValue = (String) pair.getValue();
            }
        }
        if (idValue == null || nameValue == null || ifaceValue == null) {
            logger.logToConsole("Missing information for extension point: " + a.getSource());
        }
        return new ExtensionPointInformation(idValue, nameValue, ifaceValue);
    }

    /**
     * Create a new ExtensionPointInformation object
     * 
     * @param id
     * @param name
     * @param iface
     */
    private ExtensionPointInformation(String id, String name, String iface) {
		this.id = id;
		this.name = name;
		this.iface = iface;
	}
	
	@Override
	public String toString() {
		return "ExtensionPointInformation [id=" + id + ", name=" + name
				+ ", iface=" + iface + "]";
	}

    @Override
    public int hashCode() {
        return Objects.hashCode(id, name, iface);
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof ExtensionPointInformation) {
            ExtensionPointInformation that = (ExtensionPointInformation) object;
            return Objects.equal(this.id, that.id) && Objects.equal(this.name, that.name) && Objects.equal(this.iface, that.iface);
        }
        return false;
    }

}
