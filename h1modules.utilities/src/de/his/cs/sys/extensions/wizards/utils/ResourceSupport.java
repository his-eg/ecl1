package de.his.cs.sys.extensions.wizards.utils;

import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;

import de.his.cs.sys.extensions.wizards.utils.templates.TemplateFetcher;
import de.his.cs.sys.extensions.wizards.utils.templates.TemplateManager;

/**
 * Simple Resource access support.
 *
 * @author keunecke, brummermann
 */
public class ResourceSupport {

    private static final String DEPENDENCIES_VARIABLE_NAME = "[dependencies]";

    private static final String DEPENDENCY_VARIABLE_NAME = "[dependency]";

    private static final String TEMPLATE = ".template";

    private static final String SONARBINARIESELEMENT_TEMPLATE = "sonarbinarieselement" + TEMPLATE;

    private static final String COMPILE_CLASSPATH_XML = "buildscript/compile-classpath.xml";

    private final Map<String, String> extensionAntPropertiesReplacements = new HashMap<String, String>();

    private final IProject project;

    private final Collection<String> requiredProjects;

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

        // Downloading the template list from SF requires a JDK with strong encryption!
        // See http://stackoverflow.com/questions/38203971/javax-net-ssl-sslhandshakeexception-received-fatal-alert-handshake-failure/
        Collection<String> templates = new TemplateFetcher().getTemplates();
		if (templates == null) {
			System.err.println("Error: Could not load template list. The new project could not be set up completely.");
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
    			System.err.println("Error: Could not load template '" + template + "'. The new project could not be set up completely.");
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
            boolean projectIsWebapps = HISConstants.WEBAPPS.equals(project);
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
            boolean projectIsWebapps = HISConstants.WEBAPPS.equals(project);
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
            boolean projectIsWebapps = HISConstants.WEBAPPS.equals(project);
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
            boolean projectIsWebapps = HISConstants.WEBAPPS.equals(project);
            if(!projectIsWebapps) {
                String additionalDependencyProperty = PROPERTY_DEPENDENCY_TEMPLATE.replace(DEPENDENCY_VARIABLE_NAME, project);
                sb.append(additionalDependencyProperty);
                sb.append("\n");
            }
        }
        return sb.toString();
    }
}