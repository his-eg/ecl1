package net.sf.ecl1.utilities.general;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import net.sf.ecl1.utilities.Activator;
import net.sf.ecl1.utilities.hisinone.HisConstants;
import net.sf.ecl1.utilities.hisinone.WebappsUtil;
import net.sf.ecl1.utilities.templates.TemplateFetcher;
import net.sf.ecl1.utilities.templates.TemplateManager;
import net.sf.ecl1.utilities.templates.VariableReplacer;

/**
 * Simple Resource access support.
 *
 * @author keunecke, brummermann
 */
public class ResourceSupport {

    private static final ConsoleLogger logger = new ConsoleLogger(Activator.getDefault().getLog(), Activator.PLUGIN_ID, ResourceSupport.class.getSimpleName());

    private static final String TEMPLATE_FOLDER_IN_WEBAPPS = "qisserver/WEB-INF/internal/extensionTemplates/current";
    
    private static final String DEPENDENCIES_VARIABLE_NAME = "[dependencies]";

    private static final String DEPENDENCY_VARIABLE_NAME = "[dependency]";

    private static final String TEMPLATE = ".template";

    private static final String SONARBINARIESELEMENT_TEMPLATE = "sonarbinarieselement" + TEMPLATE;

    private static final String COMPILE_CLASSPATH_XML = "buildscript/compile-classpath.xml";
    
    private final Map<String, String> extensionAntPropertiesReplacements = new HashMap<String, String>();

    private final IProject project;

    private final Collection<String> requiredProjects;
    
    private final VariableReplacer variableReplacer;

    private final String PROPERTY_DEPENDENCY_TEMPLATE = "[dependency]=${WORKSPACE}/../../../[dependency]/workspace";

    /**
     * ResourceSupport
     *
     * @param project eclipse project
     * @param choices configuration choices from setup
     */
    public ResourceSupport(IProject project, InitialProjectConfigurationChoices choices) {
        this.project = project;
        this.requiredProjects = choices.getProjectsToReference();
        this.variableReplacer = new VariableReplacer(extensionAntPropertiesReplacements);
        extensionAntPropertiesReplacements.put("[name]", choices.getName());
        extensionAntPropertiesReplacements.put("[version]", choices.getVersion());
    }


    /**
     * creates the spring bean configuration and other files for the extension
     *
     * @throws CoreException if the file creation fails
     * @throws UnsupportedEncodingException
     */
    public void createFiles() throws CoreException, UnsupportedEncodingException {
        // Prepare variables
        this.extensionAntPropertiesReplacements.put(DEPENDENCIES_VARIABLE_NAME, createAdditionalClassesFolderReferencesForSonar());
        this.extensionAntPropertiesReplacements.put("[additionaldependencies]", createAdditionalDependencyProperties());
        this.extensionAntPropertiesReplacements.put("[conditionelements]", createConditionElements());
        this.extensionAntPropertiesReplacements.put("[pathelements]", createPathElements());

        //Get templates either from webapps or from the web
        IProject webapps = WebappsUtil.findWebappsProject();
    	if (webapps != null ) {
    		logger.info("Using templates from webapps");
    		createResourceFilesFromWebapps(webapps);
    	} else {
    		logger.info("Obtaining templates from the web");
    		createResorceFilesFromWeb();
    	}
    }

    /**
     * Copies templates from webapps to new extension project 
     * 
     * @param webapps
     * @throws CoreException
     */
    private void createResourceFilesFromWebapps(IProject webapps) throws CoreException {
    	//Get base folder
    	IFolder templateSourceFolder = webapps.getFolder(TEMPLATE_FOLDER_IN_WEBAPPS);
    	
    	int segmentsOfTemplateSourceFolder = templateSourceFolder.getFullPath().segmentCount();
    	for(IResource resource : templateSourceFolder.members()) {
    		copyResourceToNewExtensionProject(resource, segmentsOfTemplateSourceFolder);
    	}
	}
    
    
    /**
     * Copies the given resource to the new extension project and replaces 
     * and fills all placeholder variables with the desired value. 
     * 
     * @param resource
     * @param segmentsOfBaseFolder
     * @throws CoreException
     */
    private void copyResourceToNewExtensionProject(IResource resource, int segmentsOfBaseFolder) throws CoreException {
		IPath relativePathWithFilename = resource.getFullPath().removeFirstSegments(segmentsOfBaseFolder);
		
		if (resource.getType() == IResource.FOLDER) {
    		//Create folder in project if it does not exist
			if(!project.exists(relativePathWithFilename)) {
				project.getFolder(relativePathWithFilename).create(IResource.FORCE, true, null);
			}
			//Recursively copy contents of this folder to new project as well
			for(IResource child : ((IFolder)resource).members()) {
				copyResourceToNewExtensionProject(child, segmentsOfBaseFolder);
			}
			
		}
		
		if(resource.getType() == IResource.FILE) {
			IFile file = (IFile)resource;
			String fileContentAsString = variableReplacer.replaceVariables(file.getContents());
			ByteArrayInputStream fileContentAsStream = new ByteArrayInputStream(fileContentAsString.getBytes());
			/*
			 * Special handling of .project file. This file was already created at an earlier stage, because otherwise the project could not have been
			 * opened (among other things).
			 * 
			 * We now replace (deleta and then copy) the already existing .project file with the file from our template folder, because the .project-file
			 * from the template folder might contain more recent configuration. 
			 */
			if (file.getName().equals(".project")) {
				project.getFile(relativePathWithFilename).delete(true, null);
			} 
			project.getFile(relativePathWithFilename).create(fileContentAsStream, true, null);	
		}
		
		
	}


	private void createResorceFilesFromWeb() throws CoreException{
    	// Downloading the template list from SF requires a JDK with strong encryption!
        // See http://stackoverflow.com/questions/38203971/javax-net-ssl-sslhandshakeexception-received-fatal-alert-handshake-failure/
        Collection<String> templates = new TemplateFetcher().getTemplates();
		if (templates == null) {
			logger.error2("Could not get templates. The new project could not be set up completely.");
			return;
		}
		
		// Now download the templates one-by-one and copy them to the project
        for (String template : templates) {
        	TemplateManager manager = new TemplateManager(template, extensionAntPropertiesReplacements);
            String content = manager.getContent();
            if (content != null) {
            	// write to project
            	manager.writeContent(project, content);
            } else {
    			logger.error2("Could not load template '" + template + "'. The new project could not be set up completely.");
    			// continue anyway (?)
            }
        }

        // if no additional dependencies delete additional classpath definition file
        if (this.extensionAntPropertiesReplacements.get("[additionaldependencies]").isEmpty()) {
            this.project.getFile(COMPILE_CLASSPATH_XML).delete(true, new NullProgressMonitor());
        }
	}


	/**
     * Creates the references to classes folders that are needed for sonar checks
     *
     * @return String with references to classes folders of required extensions' build results
     */
    private String createAdditionalClassesFolderReferencesForSonar() {
        StringBuilder classesReferences = new StringBuilder();
        HashMap<String, String> variables = new HashMap<String, String>();
        for (String project : requiredProjects) {
            boolean projectIsWebapps = HisConstants.WEBAPPS.equals(project);
            if(!projectIsWebapps) {
                variables.put(DEPENDENCY_VARIABLE_NAME, project);
                String pathElement = new TemplateManager(SONARBINARIESELEMENT_TEMPLATE, variables).getContent();
                classesReferences.append("," + pathElement);
                variables.clear();
            }
        }
        return classesReferences.toString().trim();
    }


    /**
     * Creates the path elements for the imṕorted compile-classpath.xml
     *
     * @return the XML String containing path refs for all required extensions
     */
    private String createPathElements() {
        StringBuilder pathElementsStringBuilder = new StringBuilder();
        Map<String, String> variables = new HashMap<String, String>();
        for (String project : requiredProjects) {
            boolean projectIsWebapps = HisConstants.WEBAPPS.equals(project);
            if(!projectIsWebapps) {
                variables.put(DEPENDENCY_VARIABLE_NAME, project);
                String pathElement = new TemplateManager("pathelement.template", variables).getContent();
                pathElementsStringBuilder.append(pathElement);
                pathElementsStringBuilder.append("\n");
                variables.clear();
            }
        }
        return pathElementsStringBuilder.toString().trim();
    }

    /**
     * Creates all conditional property elements for the compile-classpath.xml
     *
     * @return the XML String containing path refs for all required extensions
     */
    private String createConditionElements() {
        StringBuilder conditionElementsStringBuilder = new StringBuilder();
        for (String project : requiredProjects) {
            boolean projectIsWebapps = HisConstants.WEBAPPS.equals(project);
            if(!projectIsWebapps) {
                Map<String, String> variables = new HashMap<String, String>();
                variables.put(DEPENDENCY_VARIABLE_NAME, project);
                String conditionElement = new TemplateManager("conditionelement.template", variables).getContent();
                conditionElementsStringBuilder.append(conditionElement);
                conditionElementsStringBuilder.append("\n");
            }
        }
        return conditionElementsStringBuilder.toString();
    }

    /**
     * Creates the properties for required extensions
     *
     * @return properties as String to write into a property file
     */
    private String createAdditionalDependencyProperties() {
        StringBuilder sb = new StringBuilder();
        for (String project : requiredProjects) {
            boolean projectIsWebapps = HisConstants.WEBAPPS.equals(project);
            if(!projectIsWebapps) {
                String additionalDependencyProperty = PROPERTY_DEPENDENCY_TEMPLATE.replace(DEPENDENCY_VARIABLE_NAME, project);
                sb.append(additionalDependencyProperty);
                sb.append("\n");
            }
        }
        return sb.toString();
    }
}