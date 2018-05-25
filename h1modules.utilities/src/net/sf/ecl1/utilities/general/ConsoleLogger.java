package net.sf.ecl1.utilities.general;

import java.io.IOException;
import java.util.HashMap;

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

    /**
     * Log a message to the console.
     * 
     * @param targetProjectName the name of the project ecl1 is currently working on (optional)
     * @param message
     */
    public void log(String targetProjectName, String message) {
    	if (targetProjectName!=null && !targetProjectName.trim().isEmpty()) {
    		log("[" + targetProjectName + "] " + message);
    	} else {
    		log(message);
    	}
    }

    /**
     * Log a message to the console.
     * 
     * @param message
     */
    public void log(String message) {
		if (isLoggingEnabled()) {
            MessageConsoleStream newMessageStream = console.newMessageStream();
            try {
                newMessageStream.write(message + "\n");
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
        }
    }

    public void error(String message) {
    	// TODO red text color
    	String errorMessage = "ERROR: " + message;
    	log(errorMessage);
    }
    
    private boolean isLoggingEnabled() {
    	return Activator.getDefault().getPreferenceStore().getBoolean(ExtensionToolsPreferenceConstants.LOGGING_PREFERENCE);
    }
}
