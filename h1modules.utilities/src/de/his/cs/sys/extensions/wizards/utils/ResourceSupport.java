package de.his.cs.sys.extensions.wizards.utils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;

import de.his.cs.sys.extensions.wizards.utils.templates.TemplateManager;



/**
 * Simple Resource access support
 *
 * @author keunecke, brummermann
 */
public class ResourceSupport {


    private static final String SETTINGS = "settings";

    private static final String DEPENDENCIES_VARIABLE_NAME = "[dependencies]";

    private static final String DEPENDENCY_VARIABLE_NAME = "[dependency]";

    private static final String TEMPLATE = ".template";

    private static final String SONARBINARIESELEMENT_TEMPLATE = "sonarbinarieselement" + TEMPLATE;

    private static final String GITIGNORE = "gitignore" + TEMPLATE;

    private static final String COMPILE_CLASSPATH_XML = "compile-classpath.xml";

    private static final String COMPILE_CLASSPATH_XML_TEMPLATE = COMPILE_CLASSPATH_XML + TEMPLATE;

    private static final String SRC_JAVA_EXTENSION_BEANS_SPRING_XML = "src/java/extension.beans.spring.xml";

    private static final String SRC_JAVA_EXTENSION_BEANS_SPRING_XML_TEMPLATE = SRC_JAVA_EXTENSION_BEANS_SPRING_XML + TEMPLATE;

    private static final String SRC_TEST_DUMMY_TEST_JAVA = "src/test/DummyTest.java";

    private static final String SRC_TEST_DUMMY_TEST_JAVA_TEMPLATE = SRC_TEST_DUMMY_TEST_JAVA + TEMPLATE;

    private static final String SRC_GENERATED_DUMMY_TXT = "src/generated/dummy.txt";

    private static final String RESOURCE_DUMMY_TXT = "resource/dummy.txt";

    private static final String SRC_GENERATED_DUMMY_TXT_TEMPLATE = SRC_GENERATED_DUMMY_TXT + TEMPLATE;

    private static final String RESOURCE_DUMMY_TXT_TEMPLATE = RESOURCE_DUMMY_TXT + TEMPLATE;

    private static final String BUILD_XML_TEMPLATE = "build.xml" + TEMPLATE;

    private static final String EXTENSION_ANT_PROPERTIES_TEMPLATE = "extension.ant.properties" + TEMPLATE;

    private static final String SETTINGS_SONAR_PROJECT_PROPERTIES_TEMPLATE = SETTINGS + "/sonar-project.properties"+ TEMPLATE;

    private static final String SETTINGS_ORG_ECLIPSE_CORE_RESOURCES_PREFS = SETTINGS + "/org.eclipse.core.resources.prefs";

    private static final String SETTINGS_ORG_ECLIPSE_JDT_CORE_PREFS = SETTINGS + "/org.eclipse.jdt.core.prefs";

    private static final String SETTINGS_ORG_ECLIPSE_JDT_UI_PREFS = SETTINGS + "/org.eclipse.jdt.ui.prefs";

    private static final String JENKINS_ANT_PROPERTIES = "jenkins.ant.properties";

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

        new TemplateManager(SRC_JAVA_EXTENSION_BEANS_SPRING_XML_TEMPLATE, extensionAntPropertiesReplacements).writeContent(project);
        new TemplateManager(SRC_TEST_DUMMY_TEST_JAVA_TEMPLATE, extensionAntPropertiesReplacements).writeContent(project);
        new TemplateManager(SRC_GENERATED_DUMMY_TXT_TEMPLATE, extensionAntPropertiesReplacements).writeContent(project);
        new TemplateManager(RESOURCE_DUMMY_TXT_TEMPLATE, extensionAntPropertiesReplacements).writeContent(project);
        new TemplateManager(EXTENSION_ANT_PROPERTIES_TEMPLATE, extensionAntPropertiesReplacements).writeContent(project);
        new TemplateManager(GITIGNORE, extensionAntPropertiesReplacements).writeContent(project);

        createEclipseProjectSpecificConfigFiles();
        createSonarInfrastructureFiles();
        prepareBuildConfiguration();
    }

    /**
     * Setup of project specific configuration for compiler settings etc.
     */
    private void createEclipseProjectSpecificConfigFiles() {
        new TemplateManager(SETTINGS_ORG_ECLIPSE_CORE_RESOURCES_PREFS).writeContent(this.project);
        new TemplateManager(SETTINGS_ORG_ECLIPSE_JDT_CORE_PREFS).writeContent(this.project);
        new TemplateManager(SETTINGS_ORG_ECLIPSE_JDT_UI_PREFS).writeContent(this.project);
    }

    /**
     * Setup sonar configuration file
     */
    private void createSonarInfrastructureFiles() {
        Map<String, String> variables = new HashMap<String, String>();
        variables.putAll(this.extensionAntPropertiesReplacements);
        String additionalClassesFolders = createAdditionalClassesFolderReferencesForSonar();
        variables.put(DEPENDENCIES_VARIABLE_NAME, additionalClassesFolders );
        new TemplateManager(SETTINGS_SONAR_PROJECT_PROPERTIES_TEMPLATE, variables).writeContent(this.project);
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
     * Creates the jenkins.ant.properties file with all needed properties
     */
    private void prepareBuildConfiguration() {
        try {
            new TemplateManager(BUILD_XML_TEMPLATE, this.extensionAntPropertiesReplacements).writeContent(this.project);
            new TemplateManager(JENKINS_ANT_PROPERTIES).writeContent(this.project);
            IFile file = this.project.getFile(JENKINS_ANT_PROPERTIES);
            String additionalDependencies = createAdditionalDependencyProperties();
            if(!additionalDependencies.isEmpty()) {
                InputStream additionalDependenciesStream = new ByteArrayInputStream(additionalDependencies.getBytes());
                file.appendContents(additionalDependenciesStream, IFile.KEEP_HISTORY, new NullProgressMonitor());
                createAdditionalClasspathStructure();
            }
        } catch (CoreException e) {
            e.printStackTrace();
        }
    }


    /**
     * Creates the additional classpath structure in compile-classpath.xml needed for the new extension
     */
    private void createAdditionalClasspathStructure() {
        Map<String, String> variables = new HashMap<String, String>();
        variables.put("[conditionelements]", createConditionElements());
        variables.put("[pathelements]", createPathElements());
        new TemplateManager(COMPILE_CLASSPATH_XML_TEMPLATE, variables).writeContent(project);
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
        if(this.requiredProjects.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("\n");
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