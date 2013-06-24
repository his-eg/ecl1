package net.sf.ecl1.bundleresolver;

/**
 * Interface holding constants
 *  
 * @author keunecke
 */
public interface Constants {

    /** The Container ID provided by this plugin */
    public static final String CONTAINER_ID = "ExtensionBundleClasspathContainer.BUNDLES";
    
    /** Name of the container config page */
    public static final String CONTAINER_PAGE_NAME = "Extension Bundle Classpath Container";

    /** Label for the text field to enter the bundle directory */
    public static final String DIRECTORY_LABEL = "Bundles Directory";

    /** Default directory location of bundles */
    public static final String DEFAULT_BUNDLE_DIRECTORY = "qisserver/WEB-INF/extensions";

    /** Error message template */
    public static final String PAGE_ERROR_MESSAGE_TEMPLATE = "The directory %s does not exist in the project %s";

}
