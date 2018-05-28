package de.his.cs.sys.extensions.wizards.utils;

import h1modules.utilities.utils.Activator;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.TreeSet;

import net.sf.ecl1.utilities.general.ConsoleLogger;
import net.sf.ecl1.utilities.preferences.ExtensionToolsPreferenceConstants;

import org.apache.commons.io.IOUtils;
import org.eclipse.jface.preference.IPreferenceStore;

/**
 * Read remote projects from a jenkins view.
 * Constants default to HIS context.
 *
 * @author keunecke
 */
public class RemoteProjectSearchSupport {

    private static final ConsoleLogger logger = ConsoleLogger.getEcl1Logger();

    private class BuildJob {
        private String name;

        public String getName() {
            return name;
        }
    }

    private class BuildJobView {
        private Collection<BuildJob> jobs;

        public Collection<? extends String> getBuildJobNames() {
            Collection<String> result = new TreeSet<>();
            for (BuildJob buildJob : jobs) {
                result.add(buildJob.getName());
            }
            return result;
        }
    }

    public static final String JENKINS_VIEW_INFIX = "view/";
    public static final String JENKINS_JOB_INFIX = "job/";
    public static final String JENKINS_WORKSPACE_INFIX = "/ws/";
    
    /** Jenkins default addition for REST api calls */
    public static final String JENKINS_API_ADDITION = "/api/json";

    /** Jenkins addition to view files */
    public static final String JENKINS_VIEW_ADDITION = "/*view*/";
    
    public Collection<String> getProjects() {
        IPreferenceStore store = Activator.getPreferences();
        String buildServer = store.getString(ExtensionToolsPreferenceConstants.BUILD_SERVER_PREFERENCE); // z.B. "http://build.his.de/build/"
        String buildServerView = store.getString(ExtensionToolsPreferenceConstants.BUILD_SERVER_VIEW_PREFERENCE); // branch
        String lookUpTarget = buildServer + JENKINS_VIEW_INFIX + buildServerView + JENKINS_API_ADDITION;
        logger.info("Get projects from " + lookUpTarget);
        TreeSet<String> result = new TreeSet<String>();
        InputStream jsonStream = RestUtil.getJsonStream(lookUpTarget);
        if (jsonStream != null) {
            BuildJobView view = JsonUtil.fromJson(BuildJobView.class, jsonStream);
            result.addAll(view.getBuildJobNames());
        }
        return result;
    }

    /**
     * Returns the content of an extension project file from Jenkins as a String, or null if the file does not exist.
     * @param extension The name of the extension project from which to get the file
     * @param fileName the name of the file, relative to the extension project base folder
     * @return String or null if the file does not exist
     */
    public String getRemoteFileContent(String extension, String fileName) {
        IPreferenceStore store = Activator.getPreferences();
        String buildServer = store.getString(ExtensionToolsPreferenceConstants.BUILD_SERVER_PREFERENCE); // z.B. "http://build.his.de/build/"
        String buildServerView = store.getString(ExtensionToolsPreferenceConstants.BUILD_SERVER_VIEW_PREFERENCE); // branch
        String lookUpTarget = buildServer + JENKINS_JOB_INFIX + extension + "_" + buildServerView + JENKINS_WORKSPACE_INFIX + fileName + JENKINS_VIEW_ADDITION;
        return getRemoteFileContent(lookUpTarget);
    }
    
    /**
     * Read the content of an arbitrary file from Jenkins.
     * @param lookUpTarget the REST-URL of the file to read
     * @return file content as String
     */
    public String getRemoteFileContent(String lookUpTarget) {
        logger.debug("Get file " + lookUpTarget);
    	InputStream inStream = RestUtil.getJsonStream(lookUpTarget);
    	if (inStream == null) {
    		// wrong URL, file doesn't exist? 
    		return null;
    	}
    	String fileContent = null;
    	try {
    		fileContent = IOUtils.toString(inStream);
    	} catch (IOException ioe) {
    		logger.error("IOException reading remote file: " + ioe);
    	}
    	try {
    		inStream.close();
    	} catch (IOException ioe) {
    		logger.error("IOException closing InputStream: " + ioe);
    	}
    	logger.debug("File content = "  + fileContent);
    	return fileContent;

    }
}
