package net.sf.ecl1.utilities.general;

import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

/**
 * Helper class for properties.
 * @author tneumann
 */
public class PropertyUtil {
	/**
	 * convert a string that contains one property assignment per line to a properties map.
	 * @param arg properties string
	 * @return map
	 */
	public static Map<String, String> stringToProperties(String arg) throws ParseException {
		Map<String, String> props = new HashMap<>();
		if (arg==null) return props;
		
		int lineNumber = 0;
		int lineStart = 0;
		int lineEnd;
		while ((lineEnd = arg.indexOf('\n', lineStart)) > -1) {
			process(arg.substring(lineStart, lineEnd), lineNumber, props);
			lineStart = lineEnd+1;
			lineNumber++;
		}
		
		// the last line may not have a line end
		process(arg.substring(lineStart, arg.length()), lineNumber, props);
		
		return props;
	}
	
	private static void process(String line, int lineNumber, Map<String, String> props) throws ParseException {
		String str = line.trim();
		if (str.isEmpty()) return;
		
		int eqPos = str.indexOf('=');
		if (eqPos < 0) throw new ParseException("argument line " + lineNumber + " is no property (missing '=')", lineNumber);
		
		String left = line.substring(0, eqPos).trim();
		if (left.isEmpty()) throw new ParseException("property line " + lineNumber + " has no key", lineNumber);
		
		String right = line.substring(eqPos+1).trim();
		// values are allowed to be empty
		props.put(left, right);
	}
}
