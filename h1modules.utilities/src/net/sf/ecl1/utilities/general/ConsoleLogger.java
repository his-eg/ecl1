package net.sf.ecl1.utilities.general;

import java.io.IOException;
import java.util.HashMap;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;

import h1modules.utilities.utils.Activator;
import net.sf.ecl1.utilities.preferences.ExtensionToolsPreferenceConstants;

/**
 * Helper class for logging messages to a console.
 * 
 * @author keunecke, tneumann
 */
public class ConsoleLogger {

    private static HashMap<String, ConsoleLogger> NAME_TO_LOGGER_MAP = new HashMap<>();
    
    private MessageConsole console;

    private enum LogLevel {
    	DEBUG,
    	INFO,
    	WARN,
    	ERROR;
    }
    
    public static ConsoleLogger getLogger(String name) {
    	ConsoleLogger logger = NAME_TO_LOGGER_MAP.get(name);
    	if (logger == null) {
    		logger = new ConsoleLogger(name);
    		NAME_TO_LOGGER_MAP.put(name, logger);
    	}
    	return logger;
    }

    public static ConsoleLogger getEcl1Logger() {
    	return getLogger(Ecl1Constants.CONSOLE_NAME);
    }
    
    /**
     * Create a new ConsoleLogger.
     * 
     * @param consoleName The name of the console (should be something like "Extensions")
     */
    private ConsoleLogger(String consoleName) {
        console = findConsole(consoleName);
    }
    
    private MessageConsole findConsole(String name) {
        ConsolePlugin plugin = ConsolePlugin.getDefault();
        IConsoleManager conMan = plugin.getConsoleManager();
        IConsole[] existing = conMan.getConsoles();
        for (int i = 0; i < existing.length; i++) {
            if (name.equals(existing[i].getName())) {
            	return (MessageConsole) existing[i];
            }
        }
        //no console found, so create a new one
        MessageConsole myConsole = new MessageConsole(name, null);
        conMan.addConsoles(new IConsole[] { myConsole });
        return myConsole;
    }

    public void debug(String message) {
    	log(LogLevel.DEBUG, message, null);
    }

    public void debug(String message, StackTraceElement[] stackTrace) {
    	log(LogLevel.DEBUG, message, stackTrace);
    }

    public void info(String message) {
    	log(LogLevel.INFO, message, null);
    }

    public void info(String message, StackTraceElement[] stackTrace) {
    	log(LogLevel.INFO, message, stackTrace);
    }

    public void warn(String message) {
    	log(LogLevel.WARN, message, null);
    }

    public void warn(String message, StackTraceElement[] stackTrace) {
    	log(LogLevel.WARN, message, stackTrace);
    }

    public void error(String message) {
    	log(LogLevel.ERROR, message, null);
    }

    public void error(String message, StackTraceElement[] stackTrace) {
    	log(LogLevel.ERROR, message, stackTrace);
    }
    
    /**
     * Log a message to the console.
     * 
     * @param message
     */
    private void log(LogLevel logLevel, String message, StackTraceElement[] stackTrace) {
    	LogLevel visibleLogLevel = LogLevel.valueOf(getVisibleLogLevel());
		if (logLevel.ordinal() >= visibleLogLevel.ordinal()) {
			// https://www.eclipse.org/forums/index.php/t/172855/ :
			// Setting colors must be run synchronized to avoid an illegal thread access exception.
			Display.getDefault().syncExec(new Runnable() {
				public void run() {
					MessageConsoleStream newMessageStream = console.newMessageStream();
		            if (logLevel == LogLevel.ERROR) {
		            	Color red = new Color(null, 255, 0, 0);
		            	newMessageStream.setColor(red); 
		            } 
		            try {
		            	String logLevelStr = logLevel.toString() + ": ";
		            	if (logLevelStr.length()==6) logLevelStr += " ";
		                newMessageStream.write(logLevelStr + message + "\n");
		                // log stack trace if available
		                if (stackTrace != null) {
		                    for (StackTraceElement elem : stackTrace) {
				                newMessageStream.write("   " + elem + "\n");
		                    }
		                }
		                newMessageStream.flush();
		            } catch (IOException e) {
		                e.printStackTrace();
		            } finally {
		                try {
		                    newMessageStream.close();
		                } catch (IOException e) {
		                    e.printStackTrace();
		                }
		            }
		            // TODO log warnings and errors in "Error Log" view, too, using ILog
				}
			});
        }
    }
    
    private String getVisibleLogLevel() {
    	return Activator.getDefault().getPreferenceStore().getString(ExtensionToolsPreferenceConstants.LOG_LEVEL_PREFERENCE);
    }
}
