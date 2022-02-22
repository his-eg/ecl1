package net.sf.ecl1.importwizard;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import net.sf.ecl1.utilities.general.ConsoleLogger;
import net.sf.ecl1.utilities.general.RemoteProjectSearchSupport;
import net.sf.ecl1.utilities.preferences.PreferenceWrapper;

/**
 * The data model of the Extension Import Wizard.
 * @author tneumann
 */
public class ExtensionImportWizardModel {
    
	private static final ConsoleLogger logger = new ConsoleLogger(Activator.getDefault().getLog(), Activator.PLUGIN_ID, ExtensionImportWizardModel.class.getSimpleName());

	public static final String JENKINS_WEBAPPS_NAME = "/webapps";

    private RemoteProjectSearchSupport remoteProjectSearchSupport;
    
    // actual branch
    private String branch;
    
    // All extensions existing on repo server
    private Set<String> remoteExtensions;

    // All extensions existing in workspace
    private Set<String> extensionsInWorkspace;

    // Extensions chosen by the user for installation
    private Set<String> selectedExtensions;
    
    /**
     * Key --> Name of the extension.
     * Value --> classpath file from the extension, which was obtained from build.his.de 
     */
    private Map<String, ClasspathFile> classpathFiles;

    // dependent extensions
    private Collection<String> allRequiredDependencies = null;

    public ExtensionImportWizardModel() {
    	branch = PreferenceWrapper.getBuildServerView();
    	remoteProjectSearchSupport = new RemoteProjectSearchSupport();
    	initRemoteExtensions();
        initExtensionsInWorkspace();
        classpathFiles = new HashMap<String, ClasspathFile>();
    }

    String getBranch() {
    	return branch;
    }

    /**
     * Read remote extensions from configured build server.
     */
    private void initRemoteExtensions() {
    	remoteExtensions = new TreeSet<String>();
        Collection<String> remoteProjectsIncludingBranch = remoteProjectSearchSupport.getProjects();
        String branch = getBranch();
        for (String remoteProjectIncludingBranch : remoteProjectsIncludingBranch) {
            String remoteProject = remoteProjectIncludingBranch.replace("_" + branch, "");
            if (remoteProject!=null && !remoteProject.trim().isEmpty()) {
            	//logger.debug("Found remote project '" + remoteProjectIncludingBranch + "' , store it as '" + remoteProject + "'");
            	remoteExtensions.add(remoteProject);
            }
        }
    }

    /**
     * @return collection of remote extensions, initialized by wizard page 1
     */
    Collection<String> getRemoteExtensions() {
    	return remoteExtensions;
    }
    
    /**
     * Read extension projects already present in workspace.
     * Only valid projects are returned, folders without project structure omitted.
     * 
     * @see {@link ExtensionImportJob.checkForExistingFolders()}
     */
    private void initExtensionsInWorkspace() {
    	extensionsInWorkspace = new TreeSet<String>();
        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        IWorkspaceRoot root = workspace.getRoot();
        IProject[] projects = root.getProjects();
        for (IProject iProject : projects) {
            String name = iProject.getName();
            extensionsInWorkspace.add(name);
        }
    }

	Set<String> getExtensionsInWorkspace() {
		return extensionsInWorkspace;
	}
	
	void setSelectedExtensions(Set<String> selectedExtensions) {
		this.selectedExtensions = selectedExtensions;
	}
	
	Set<String> getSelectedExtensions() {
		return selectedExtensions;
	}
    
	/**
	 * Find all extensions on which the current selection depends on. The evaluation is "deep", i.e. iteratively
	 * finds dependencies of dependencies, too. Extension on which the selected extensions depend on but are
	 * already in workspace are omitted.
	 */
    void findDependenciesOfSelectedExtensions() {
    	logger.debug("remoteExtensions = " + remoteExtensions);
    	logger.debug("selectedExtensions = " + selectedExtensions);
        
    	allRequiredDependencies = new ArrayList<String>(); // the result of this method
    	Set<String> processedExtensions = new HashSet<String>(); // required to avoid infinite loops when cyclic extension dependencies exist
        Set<String> unprocessedExtensions = new TreeSet<String>(selectedExtensions); // copy to keep selectedExtensions unmodified
        while (!unprocessedExtensions.isEmpty()) {
        	// get one unprocessed extension
        	String extension = unprocessedExtensions.iterator().next();
        	unprocessedExtensions.remove(extension);
        	
        	ClasspathFile classpathFile = getClasspathFile(extension);
        	
        	// find all required extensions
        	for (String directDependency : classpathFile.getRegularDependencies()) {
        		if (processedExtensions.contains(directDependency)) {
        			logger.debug("Extension " + directDependency + " has already been analyzed, skip...");
        		} else if (!selectedExtensions.contains(directDependency) && !allRequiredDependencies.contains(directDependency)) {
        			// the required extension has not been selected and not been registered before
        			if (!extensionsInWorkspace.contains(directDependency)) {
        				// the required extension does not exist yet, so it must be imported
        				logger.info("Extension " + directDependency + " has been auto-selected for import, because the selected extensions depend on it.");
        				allRequiredDependencies.add(directDependency);
        			} // else: the required extension is already in workspace -> do not re-import, but check it's dependencies
        			unprocessedExtensions.add(directDependency);
        		}
        	}
        	
        	
        	
        	
        	/*
        	 * TODO: 
        	 * After finishing ticket https://hiszilla.his.de/hiszilla/show_bug.cgi?id=261078, continue work here. 
        	 * Namely, finish this ticket here: https://hiszilla.his.de/hiszilla/show_bug.cgi?id=261078. 
        	 * 
        	 * At this code point the variable classpathFile already knows all members of the ecl1 classpath container. 
        	 * Thus, we can use it to obtain the optional dependencies. 
        	 */
        	
        	processedExtensions.add(extension);
        }
    }

	
    private ClasspathFile getClasspathFile(String extension) {
    	ClasspathFile classpathFile = classpathFiles.get(extension);
    	if (classpathFile != null) {
    		return classpathFile;
    	}
    	classpathFile = new ClasspathFile(extension);
    	classpathFiles.put(extension, classpathFile);
    	return classpathFile;
    }

    /**
     * @return extensions that the user-selected extensions depend on.
     */
    Collection<String> getDependenciesOfSelectedExtensions() {
    	return allRequiredDependencies;
    }
}
