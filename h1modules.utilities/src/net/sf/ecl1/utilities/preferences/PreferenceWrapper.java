package net.sf.ecl1.utilities.preferences;

import java.util.Arrays;
import java.util.List;

import net.sf.ecl1.utilities.Activator;
import net.sf.ecl1.utilities.general.GitUtil;

/**
 * Encapsulate access to Eclipse preferences.
 * @author tneumann
 */
public class PreferenceWrapper {
    
    /** Preference for URI to git server */
    public static final String GIT_SERVER_PREFERENCE_KEY = "gitServer";

    /** Preference for URL of jenkins build server */
    public static final String BUILD_SERVER_PREFERENCE_KEY = "buildServer";

    /** Preference for search view (branch) on jenkins build server */
    public static final String BUILD_SERVER_VIEW_PREFERENCE_KEY = "buildServerView";

    /** Preference for the root URLs of the templates (comma-separated list) */
    public static final String TEMPLATE_ROOT_URLS_PREFERENCE_KEY = "templateRootUrls";

    /** Messages with a log level greater or equal than this preference are logged, others not. */
    public static final String LOG_LEVEL_PREFERENCE_KEY = "net.sf.ecl1.logLevel";
    
    /** Stores if the current branch of webapps should be detected automatically */
    public static final String DETECT_BRANCH_AUTOMATICALLY = "detectBranchAutomatically";
    
    /** Stores if the summary of the git batch pull should be displayed in a dialog to the user */
    public static final String DISPLAY_SUMMARY_OF_GIT_PULL = "displaySummaryOfGitPull";

    
    /**
     * @return git repository server URL
     */
	public static String getGitServer() {
		return Activator.getPreferences().getString(GIT_SERVER_PREFERENCE_KEY);
	}
	
	/**
	 * @return build server base URL, e.g. "http://build.his.de/build/"
	 */
	public static String getBuildServer() {
        return Activator.getPreferences().getString(BUILD_SERVER_PREFERENCE_KEY);
	}
	
    /**
     * @return the build server view / HisInOne branch declared on the preferences page,
     * like "HEAD" or "HISinOne_VERSION_07_RELEASE_01".
     */
    public static String getBuildServerView() {
    	if(Activator.getPreferences().getBoolean(DETECT_BRANCH_AUTOMATICALLY)) {
    		Activator.getPreferences().setValue(BUILD_SERVER_VIEW_PREFERENCE_KEY, GitUtil.getCheckedOutBranchOfWebapps());
    	}
        return Activator.getPreferences().getString(BUILD_SERVER_VIEW_PREFERENCE_KEY);
    }
    
    /**
     * @return a list of URLs from where extension template files may be fetched.
     */
    public static List<String> getTemplateRootUrls() {
        String templateRootUrlsStr = Activator.getPreferences().getString(TEMPLATE_ROOT_URLS_PREFERENCE_KEY);
        return Arrays.asList(templateRootUrlsStr.split(","));
    }
    
    /**
     * @return the users log level preference
     */
    public static String getLogLevel() {
    	return Activator.getPreferences().getString(LOG_LEVEL_PREFERENCE_KEY);
    }

	public static boolean isDetectBranchAutomatically() {
		return Activator.getPreferences().getBoolean(DETECT_BRANCH_AUTOMATICALLY);
	}
	
	public static boolean isDisplaySummaryOfGitPull() {
		return Activator.getPreferences().getBoolean(DISPLAY_SUMMARY_OF_GIT_PULL);
	}
	
	public static void setDisplaySummaryOfGitPull(boolean v) {
		Activator.getPreferences().setValue(DISPLAY_SUMMARY_OF_GIT_PULL, v);
	}
}
