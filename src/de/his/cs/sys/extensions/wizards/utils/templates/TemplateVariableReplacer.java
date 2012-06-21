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
import java.util.Map;
import java.util.Map.Entry;

import de.his.cs.sys.extensions.wizards.utils.ResourceSupport;

/**
 * Replaces variables in templates given as input streams
 * 
 * @author keunecke
 * @version $Revision$ 
 */
public class TemplateVariableReplacer {
	
	private final InputStream template;
	
	private final Map<String, String> variables;
	
	public TemplateVariableReplacer(String templatePath, Map<String, String> variables) {
		
		this.template = TemplateVariableReplacer.class.getResourceAsStream(templatePath);;
		this.variables = variables;
	}
	
	public String replace() {
		StringBuilder result = new StringBuilder();
		BufferedReader br = new BufferedReader(new InputStreamReader(this.template));
		String line = "";
		try {
			while((line = br.readLine() )!= null) {
				String temp = line;
				for (Entry<String, String> variableAssignment : this.variables.entrySet()) {
					temp = temp.replace(variableAssignment.getKey(), variableAssignment.getValue());
				}
				result.append(temp + "\n");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return result.toString();
	}

}
