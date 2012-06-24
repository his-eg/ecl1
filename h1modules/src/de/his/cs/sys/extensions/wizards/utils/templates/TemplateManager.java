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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Replaces variables in templates given by name
 * 
 * @author keunecke
 * @version $Revision$ 
 */
public class TemplateManager {
	
	private final InputStream template;
	
	private final Map<String, String> variables;
	
	/**
	 * Create a new template manager with a template and variables to replace in the template
	 * 
	 * @param templatePath
	 * @param variables
	 */
	public TemplateManager(String templatePath, Map<String, String> variables) {
		this.template = TemplateManager.class.getResourceAsStream(templatePath);;
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

}
