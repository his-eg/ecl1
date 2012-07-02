package de.his.cs.sys.extensions.wizards.utils;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;

import utils.templates.TemplateManager;


/**
 * Simple Resource access support
 *
 * @author keunecke, brummermann
 */
public class ResourceSupport {
	
	private final Map<String, String> extensionAntPropertiesReplacements = new HashMap<String, String>();
	
	private final IProject project;

	/**
	 * ResourceSupport
	 *
	 * @param project eclipse project
	 * @param version initial extension version
	 */
	public ResourceSupport(IProject project, InitialProjectConfigurationChoices choices) {
		this.project = project;
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
		InputStream is = ResourceSupport.class.getResourceAsStream("templates/src/java/extension.beans.spring.xml.template");
		writeProjectFile("/src/java/extension.beans.spring.xml", is);

		new TemplateManager("extension.ant.properties.template", extensionAntPropertiesReplacements).writeContent(project);
		
		new TemplateManager("build.xml.template").writeContent(project);
		
		is = new ByteArrayInputStream(("/bin" + System.getProperty("line.separator") + "/build").getBytes("UTF-8"));
		writeProjectFile("/.gitignore", is);
	}

	/**
	 * writes a file
	 *
	 * @param filename filename relative to project root
	 * @param is input stream with data to write
	 * @throws CoreException in case of an exception
	 */
	private void writeProjectFile(String filename, InputStream is) throws CoreException {
		IFile file = project.getFile(filename);
		try {
			file.create(is, true, null);
			is.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
