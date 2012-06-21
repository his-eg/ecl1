package de.his.cs.sys.extensions.wizards.utils;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;

/**
 * Simple Resource access support
 *
 * @author keunecke, brummermann
 */
public class ResourceSupport {
	private final IProject project;

	/**
	 * ResourceSupport
	 *
	 * @param project eclipse project
	 */
	public ResourceSupport(IProject project) {
		this.project = project;
	}


	/**
	 * creates the spring bean configuration and other files for the extension
	 *
	 * @throws CoreException if the file creation fails
	 * @throws UnsupportedEncodingException
	 */
	public void createFiles() throws CoreException, UnsupportedEncodingException {
		String filename = "/src/java/extension.beans.spring.xml";
		InputStream is = ResourceSupport.class.getResourceAsStream("templates/src/java/extension.beans.spring.xml.template");
		writeProjectFile(filename, is);

		is = new ByteArrayInputStream(("extension.name=" + project.getName() + "\nextension.version=0.0.1").getBytes("UTF-8"));
		writeProjectFile("/extension.ant.properties", is);

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
