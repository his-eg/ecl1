package de.his.cs.sys.extensions.wizards.utils;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
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

		is = ResourceSupport.class.getResourceAsStream("templates/extension.ant.properties");
		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		PrintStream writer = new PrintStream(buffer);
		try {
			String line = br.readLine();
			while(br.readLine() != null)  {
				String temp = line.replace("[name]", this.project.getName());
				System.out.println(line + " -> " + temp);
				writer.println(temp);
				writer.flush();
				line = br.readLine();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		writeProjectFile("/extension.ant.properties", new ByteArrayInputStream(buffer.toByteArray()));

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
