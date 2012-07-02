/*
 * Copyright (c) 2012 HIS GmbH All Rights Reserved.
 *
 * $Id$
 *
 * $Log$
 *
 * Created on 21.06.2012 by keunecke
 */
package de.his.cs.sys.extensions.wizards.utils.templates;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;

/**
 * Replaces variables in templates given by name
 * 
 * @author keunecke
 * @version $Revision$ 
 */
public class TemplateManager {
	
	private final InputStream template;
	
	private final Map<String, String> variables;

    private String templatePath;
	
	/**
	 * Create a new template manager with a template and variables to replace in the template
	 * 
	 * @param templatePath
	 * @param variables
	 */
	public TemplateManager(String templatePath, Map<String, String> variables) {
		this.template = TemplateManager.class.getResourceAsStream(templatePath);
		this.templatePath = templatePath;
		this.variables = variables;
	}
	
	/**
	 * Create a new template manager without variables to replace
	 * 
	 * @param template the template path
	 */
	public TemplateManager(String template) {
		this(template, new HashMap<String, String>());
	}

	/**
	 * Do line-wise variable replacement on template
	 * 
	 * @return result string with replaced variables
	 */
	public String getContent() {
		StringBuilder result = new StringBuilder();
		BufferedReader br = new BufferedReader(new InputStreamReader(this.template));
		String line = "";
		try {
			while((line = br.readLine() )!= null) {
				String temp = line;
				for (Entry<String, String> variableAssignment : this.variables.entrySet()) {
					temp = temp.replace(variableAssignment.getKey(), variableAssignment.getValue());
				}
				result.append(temp + System.getProperty("line.separator"));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return result.toString().trim();
	}
	
	/**
	 * Writes out the content of the given template with replaced variables to the given project in the same path as the template path.
	 * The suffix ".template" is removed on file creation.
	 * 
	 * @param project
	 */
	public void writeContent(IProject project) {
	    IFile file = project.getFile("/" + this.templatePath.replace(".template", ""));
	    InputStream is = new ByteArrayInputStream(getContent().getBytes());
	    try {
            file.create(is, true, null);
            is.close();
        } catch (CoreException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
	}

}
