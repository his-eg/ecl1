package net.sf.ecl1.utilities.templates;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.Map.Entry;

import net.sf.ecl1.utilities.Activator;
import net.sf.ecl1.utilities.logging.ICommonLogger;
import net.sf.ecl1.utilities.logging.LoggerFactory;

public class VariableReplacer {
	
    private static final ICommonLogger logger = LoggerFactory.getLogger(VariableReplacer.class.getSimpleName(), Activator.PLUGIN_ID, Activator.getDefault() != null ? Activator.getDefault().getLog() : null);
	
	private final Map<String, String> variables;

	public VariableReplacer(Map<String, String> variables) {
		this.variables = variables;
	}
	
	/**
	 * Replaces all variables in the given inputStream and returns a String
	 * 
	 * @param inputStream
	 * @return
	 */
	public String replaceVariables(InputStream inputStream) {
		StringBuilder result = new StringBuilder();
		BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
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
			logger.error2("Error replacing variables. This was the exception: ", e);
		}
    return result.toString().trim();
	}
	
	
}
