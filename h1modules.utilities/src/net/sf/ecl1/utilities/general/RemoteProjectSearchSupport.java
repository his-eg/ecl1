package net.sf.ecl1.utilities.general;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.TreeSet;

import org.apache.commons.io.IOUtils;

import net.sf.ecl1.utilities.Activator;
import net.sf.ecl1.utilities.logging.ICommonLogger;
import net.sf.ecl1.utilities.logging.LoggerFactory;
import net.sf.ecl1.utilities.preferences.PreferenceWrapper;

/**
 * Read remote projects from a jenkins view.
 * Constants default to HIS context.
 *
 * @author keunecke
 */
public class RemoteProjectSearchSupport {

    private static final ICommonLogger logger = LoggerFactory.getLogger(RemoteProjectSearchSupport.class.getSimpleName(), Activator.PLUGIN_ID, Activator.getDefault());

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
    public static final String JENKINS_LAST_SUCCESSFUL_BUILD_ARTIFACTS = "/lastSuccessfulBuild/artifact/";

    /** Jenkins default addition for REST api calls */
    public static final String JENKINS_API_ADDITION = "/api/json";

    /** Jenkins addition to view files */
    public static final String JENKINS_VIEW_ADDITION = "/*view*/";

    public Collection<String> getProjects() {
        TreeSet<String> result = new TreeSet<String>();
        String buildServer = PreferenceWrapper.getBuildServer(); // z.B. "http://build.his.de/build/"
        String buildServerView = PreferenceWrapper.getBuildServerView(); // branch
        if (buildServerView.equals(GitUtil.UNKNOWN_BRANCH)) {
        	//Since we are locally on an unknown branch, we can abort here before contacting the remote
        	return result;
        }
        String lookUpTarget = buildServer + JENKINS_VIEW_INFIX + buildServerView + JENKINS_API_ADDITION;
        logger.debug("Get projects from " + lookUpTarget);
        InputStream jsonStream = RestUtil.getJsonStream(lookUpTarget, true);
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
     * @param targetShouldExist if true then we expect that the target exists
     * @return String or null if the file does not exist
     */
    public String getRemoteFileContent(String extension, String fileName, boolean targetShouldExist) {
        String buildServer = PreferenceWrapper.getBuildServer(); // z.B. "http://build.his.de/build/"
        String buildServerView = PreferenceWrapper.getBuildServerView(); // branch
        String lookUpTarget = buildServer + JENKINS_JOB_INFIX + extension + "_" + buildServerView + JENKINS_WORKSPACE_INFIX + fileName + JENKINS_VIEW_ADDITION;
        return getRemoteFileContent(lookUpTarget, targetShouldExist);
    }

    public String getRemoteArtifactContent(String jobName, String artifactFileName, boolean targetShouldExist) {
        String buildServer = PreferenceWrapper.getBuildServer(); // z.B. "http://build.his.de/build/"
        String buildServerView = PreferenceWrapper.getBuildServerView(); // branch
        String lookUpTarget = buildServer + JENKINS_JOB_INFIX + jobName + "_" + buildServerView + JENKINS_LAST_SUCCESSFUL_BUILD_ARTIFACTS + artifactFileName;
        return getRemoteFileContent(lookUpTarget, targetShouldExist);
    }

    /**
     * Read the content of an arbitrary file from Jenkins.
     * @param lookUpTarget the REST-URL of the file to read
     * @param targetShouldExist if true then we expect that the target exists
     * @return file content as String
     */
    public String getRemoteFileContent(String lookUpTarget, boolean targetShouldExist) {
        logger.debug("Get file " + lookUpTarget);
    	InputStream inStream = RestUtil.getJsonStream(lookUpTarget, targetShouldExist);
    	if (inStream == null) {
    		// wrong URL, file doesn't exist?
    		return null;
    	}
    	String fileContent = null;
    	try {
    		fileContent = IOUtils.toString(inStream, StandardCharsets.UTF_8);
    	} catch (IOException e) {
    		logger.error2("IOException reading remote file: " + e.getMessage(), e);
    	}
    	try {
    		inStream.close();
    	} catch (IOException e) {
    		logger.error2("IOException closing InputStream: " + e.getMessage(), e);
    	}
    	logger.debug("File content = "  + fileContent);
    	return fileContent;
    }
}
