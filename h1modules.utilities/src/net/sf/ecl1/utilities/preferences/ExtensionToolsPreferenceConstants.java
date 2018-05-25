package net.sf.ecl1.utilities.preferences;

/**
 * Constant definitions for HISinOne-Extension tools plug-in preferences
 */
public class ExtensionToolsPreferenceConstants {

    /**
     * The name of the preference value determining if detailed logging should occur
     */
    public static final String LOGGING_PREFERENCE = "net.sf.ecl1.extensionpoint.extensionsLogging";
    // TODO choose more general name
    
    /**
     * Preference for URI to git server
     */
    public static final String GIT_SERVER_PREFERENCE = "gitServer";

    /**
     * Preference for URL of jenkins build server
     */
    public static final String BUILD_SERVER_PREFERENCE = "buildServer";

    /**
     * Preference for search view on jenkins build server
     */
    public static final String BUILD_SERVER_VIEW_PREFERENCE = "buildServerView";

    /**
     * Preference for the root URLs of the templates (comma-separated list)
     */
    public static final String TEMPLATE_ROOT_URLS = "templateRootUrls";

}
