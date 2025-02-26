package net.sf.ecl1.utilities.hisinone;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.Path;

import net.sf.ecl1.utilities.Activator;
import net.sf.ecl1.utilities.logging.ICommonLogger;
import net.sf.ecl1.utilities.logging.LoggerFactory;
import net.sf.ecl1.utilities.standalone.wokspace.WorkspaceFactory;
import net.sf.ecl1.utilities.standalone.wokspace.WorkspaceRootImpl;

public class WebappsUtil {

    private static final ICommonLogger logger = LoggerFactory.getLogger(WebappsUtil.class.getSimpleName(), Activator.PLUGIN_ID, Activator.getDefault() != null ? Activator.getDefault().getLog() : null);

    /**
     * Find the webapps project in the workspace.
     * We consider only HisInOne projects, which (in contrast to QIS-projects have a qisserver/WEB-INF/extensions folder.
     * If several such projects exist, a project named webapps has priority; otherwise the first found project is returned.
     * 
     * @return the project serving as core webapps
     */
    public static IProject findWebappsProject() {
        List<IProject> projects;
        if(Activator.isRunningInEclipse()){
            projects = Arrays.asList(WorkspaceFactory.getWorkspace().getRoot().getProjects(IWorkspaceRoot.INCLUDE_HIDDEN));
        }else{
            IWorkspaceRoot root = new WorkspaceRootImpl(true);
            projects = Arrays.asList(root.getProjects(IWorkspaceRoot.INCLUDE_HIDDEN));
        }
        IProject webapps = null;
        List<IProject> webappsCandidates = new ArrayList<>();
    	List<String> candidateNames = new ArrayList<>();
        for (IProject project : projects) {
        	// the extension folder distinguishes hisinone and qis projects
            IFolder extensionsFolder = project.getFolder(HisConstants.EXTENSIONS_FOLDER);           
            if (extensionsFolder != null && extensionsFolder.exists()) {
            	// We found a hisinone project.
            	webappsCandidates.add(project);
            	String projectName = project.getName();
            	candidateNames.add(projectName);
            	logger.debug("Found webapps candidate: " + projectName);
            	if (HisConstants.WEBAPPS.equals(projectName)) {
            		webapps = project;
            	}
            }
        }
        int candidatesCount = webappsCandidates.size();
        if (candidatesCount == 0) {
        	logger.warn("No webapps project found in workspace...");
        	return null;
        }
        if (candidatesCount == 1) {
        	webapps = webappsCandidates.get(0);
        	logger.debug("Found single HisInOne-project '" + webapps.getName() + "'");
        	return webapps;
        }
        // more than one hit
        if (webapps != null) {
        	logger.debug("Found several HisInOne-projects: " + candidateNames + ". The project named 'webapps' is returned.");
        	return webapps;
        }
    	logger.warn("Found several HisInOne-projects: " + candidateNames + ", none of them called 'webapps'. The first candidate is returned.");
    	return webappsCandidates.get(0);
    }

    public static boolean isWebapps(IProject project) {
    	boolean isWebapps = project.exists(new Path(HisConstants.EXTENSIONS_FOLDER));
    	logger.debug("Project " + project.getName() + " is " + (isWebapps ? "" : "not ") + "webapps");
        return isWebapps;
    }
}
