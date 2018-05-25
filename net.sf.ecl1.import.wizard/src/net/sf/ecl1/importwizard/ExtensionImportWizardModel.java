package net.sf.ecl1.importwizard;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import de.his.cs.sys.extensions.wizards.utils.RemoteProjectSearchSupport;
import h1modules.utilities.utils.Activator;
import net.sf.ecl1.utilities.general.ConsoleLogger;

/**
 * The data model of the Extension Import Wizard.
 * @author tneumann
 */
public class ExtensionImportWizardModel {
    
    private static final ConsoleLogger logger = ConsoleLogger.getEcl1Logger();

	private static final String JENKINS_WEBAPPS_NAME = "/webapps";

    private RemoteProjectSearchSupport remoteProjectSearchSupport;
    
    // actual branch
    private String branch;
    
    // All extensions existing on repo server
    private Set<String> remoteExtensions;

    // All extensions existing in workspace
    private Set<String> extensionsInWorkspace;

    // Extensions chosen by the user for installation
    private Set<String> selectedExtensions;

    // map from extension to the extensions they need.
    // a null list means that the extension dependency has not been initialized yet.
    private Map<String, List<String>> extensions2dependencyExtensions;

    // dependent extensions
    private Collection<String> totalRequiredExtensions = null;

    public ExtensionImportWizardModel() {
    	branch = Activator.getHISinOneBranch();
    	remoteProjectSearchSupport = new RemoteProjectSearchSupport();
    	initRemoteExtensions();
        initExtensionsInWorkspace();
        extensions2dependencyExtensions = new HashMap<String, List<String>>();
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
            logger.log("Replacing original '" + remoteProjectIncludingBranch + "' with '" + remoteProject + "'");
            remoteExtensions.add(remoteProject);
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
    void findTotalRequiredExtensions() {
    	//ConsoleLogger.getEcl1Logger().log("remoteExtensions = " + remoteExtensions);
    	//ConsoleLogger.getEcl1Logger().log("selectedExtensions = " + selectedExtensions);
        
    	totalRequiredExtensions = new ArrayList<String>();
        Set<String> unprocessedExtensions = new TreeSet<String>(selectedExtensions); // copy to keep selectedExtensions unmodified
        while (!unprocessedExtensions.isEmpty()) {
        	// get one unprocessed extension
        	String extension = unprocessedExtensions.iterator().next();
        	unprocessedExtensions.remove(extension);
        	// find all required extensions
        	Collection<String> directlyRequiredExtensions = findDirectlyRequiredExtensions(extension);
        	for (String requiredExtension : directlyRequiredExtensions) {
        		if (!selectedExtensions.contains(requiredExtension) && !totalRequiredExtensions.contains(requiredExtension)) {
        			// the required extension has not been selected and not been registered before
        			if (!extensionsInWorkspace.contains(requiredExtension)) {
        				// the required extension does not exist yet, so it must be imported
        				totalRequiredExtensions.add(requiredExtension);
        			} // else: the required extension is already in workspace -> do not re-import, but check it's own dependencies
        			unprocessedExtensions.add(requiredExtension);
        		}
        	}
        }
    }

	/**
	 * Returns the list of extensions directly required by the given extension.
	 * Only one dependency level is evaluated.
	 * 
	 * @param extension
	 * @return list of extension names
	 */
	private List<String> findDirectlyRequiredExtensions(String extension) {
		List<String> dependencyExtensions = extensions2dependencyExtensions.get(extension);
		if (dependencyExtensions != null) {
			return dependencyExtensions;
		}
		
		// dependencies have not been initialized yet for the given extension.
		// register empty list to signal that initialization has taken place now:
		dependencyExtensions = new ArrayList<String>();
		extensions2dependencyExtensions.put(extension, dependencyExtensions);
		// read extension ".classpath" file
    	String classpathContent = remoteProjectSearchSupport.getRemoteFileContent(extension, ".classpath");
    	if (classpathContent == null) {
    		// there is no ".classpath"-file in the extension project?
    		return dependencyExtensions; // return empty list
    	}
    	
    	// create XML document
    	Document doc = null;
    	InputStream classpathContentStream = null;
    	try {
        	classpathContentStream = new ByteArrayInputStream(classpathContent.getBytes("UTF-8"));
        	doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(classpathContentStream);
    		classpathContentStream.close();
    	} catch (IOException | SAXException | ParserConfigurationException e) {
    		logger.error("Exception parsing '.classpath' file for extension " + extension  + ": " + e);
    	}
    	if (doc==null) {
    		logger.error("Could not create XML document from '.classpath' file of extension " + extension);
    		return dependencyExtensions; // return empty list
    	}
    	
    	// parse XML: adapted from class ExtensionProjectDependencyLoader,
    	// and converted to org.w3c.dom because there is no Eclipse Osgi bundle for org.jdom
		Element root = doc.getDocumentElement();
		NodeList classpathEntries = root.getElementsByTagName("classpathentry");
		if (classpathEntries!=null) {
	        int classpathEntriesCount = classpathEntries.getLength();
	        for (int index=0; index<classpathEntriesCount; index++) {
	        	Node node = classpathEntries.item(index);
	        	if (!(node instanceof Element)) continue;
	        	Element classpathEntry = (Element) node;
	        	if (isProjectDependency(classpathEntry)) {
	                String projectDependency = classpathEntry.getAttribute("path").substring(1);
	                dependencyExtensions.add(projectDependency);
	            }
	        }
		}
		return dependencyExtensions;
	}

	// adapted from class ExtensionProjectDependencyLoader
    private boolean isProjectDependency(Element classpathEntry) {
        String kind = classpathEntry.getAttribute("kind");
        boolean isSourceEntry = kind != null && "src".equals(kind);
        String path = classpathEntry.getAttribute("path");
        boolean isProjectRelatedEntry = path != null && !path.isEmpty() && path.startsWith("/") && !JENKINS_WEBAPPS_NAME.equals(path);
    	return isSourceEntry && isProjectRelatedEntry;
    }

    /**
     * @return extensions that the user-selected extensions depend on.
     */
    Collection<String> getTotalRequiredExtensions() {
    	return totalRequiredExtensions;
    }
}
