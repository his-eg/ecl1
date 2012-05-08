package de.his.cs.sys.extensions.wizards.utils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;

/**
 * Simple Resource access support
 * 
 * @author keunecke
 */
public class ResourceSupport {
	
	public static void createModuleBeanFile(IProject project) throws CoreException {
		IFile file = project.getFile("/src/java/module.beans.spring.xml");
		InputStream is = ResourceSupport.class.getResourceAsStream("templates/modulebeanstemplate.xml");
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
