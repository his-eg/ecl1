package net.sf.ecl1.utilities.general;

import java.io.IOException;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
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

    public enum LogLevel {
    	DEBUG {
    		int toIStatus() { return IStatus.OK; }
    	},
    	INFO {
    		int toIStatus() { return IStatus.INFO; }
    	},
    	WARN {
    		int toIStatus() { return IStatus.WARNING; }
    	},
    	ERROR {
    		int toIStatus() { return IStatus.ERROR; }
    	};
    	
    	abstract int toIStatus();
    }

    /**
     * The name of the console this project should always acquire
     */
    public static final String CONSOLE_NAME = "Extensions";

    private static final MessageConsole console = findConsole(CONSOLE_NAME);
    
    private static MessageConsole findConsole(String name) {
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

    private String pluginId = null;
    private ILog errorLogLogger = null;
    
    /**
     * Create a new ConsoleLogger. This constructor is for cases where we can not get an ILog (wizards !?)
     */
    public ConsoleLogger() {
    }

    /**
     * Create a new ConsoleLogger.
     * 
     * @param plugin the Eclipse plugin that wants to log
     */
    public ConsoleLogger(ILog errorLogLogger, String pluginId) {
    	if (errorLogLogger != null) {
    		this.errorLogLogger = errorLogLogger;
    		this.pluginId = pluginId;
    	}
    }

    /**
     * Logs a debug log to the console.
     * @param message
     */
    public void debug(String message) {
    	log(LogLevel.DEBUG, message, null, false);
    }

    public void debug(String message, Throwable t) {
    	log(LogLevel.DEBUG, message, t, false);
    }

    /**
     * Logs an info log to the console.
     * @param message
     */
    public void info(String message) {
    	log(LogLevel.INFO, message, null, false);
    }

    public void info(String message, Throwable t) {
    	log(LogLevel.INFO, message, t, false);
    }

    /**
     * Logs an error to the console.
     * @param message
     */
    public void warn(String message) {
    	log(LogLevel.WARN, message, null, false);
    }

    public void warn(String message, Throwable t) {
    	log(LogLevel.WARN, message, t, false);
    }

    /**
     * Logs a warning to console and the "Error Log" view.
     * @param message
     */
    public void warn2(String message) {
    	log(LogLevel.WARN, message, null, true);
    }

    public void warn2(String message, Throwable t) {
    	log(LogLevel.WARN, message, t, true);
    }

    /**
     * Logs an error to the console .
     * @param message
     */
    public void error(String message) {
    	log(LogLevel.ERROR, message, null, false);
    }

    public void error(String message, Throwable t) {
    	log(LogLevel.ERROR, message, t, false);
    }

    /**
     * Logs an error to console and the "Error Log" view.
     * @param message
     */
    public void error2(String message) {
    	log(LogLevel.ERROR, message, null, true);
    }

    public void error2(String message, Throwable t) {
    	log(LogLevel.ERROR, message, t, true);
    }
    
    /**
     * Log a message to the console.
     * 
     * @param logLevel DEBUG, INFO, WARN or ERROR
     * @param message
     * @param t Throwable (optional)
     * @param insertIntoErrorLogView if true then the message is also posted to the "Error Log" view
     */
    private void log(LogLevel logLevel, String message, Throwable t, boolean insertIntoErrorLogView) {
    	LogLevel visibleLogLevel = LogLevel.valueOf(getVisibleLogLevel());
		if (logLevel.ordinal() >= visibleLogLevel.ordinal()) {
			// https://www.eclipse.org/forums/index.php/t/172855/ :
			// Setting colors must be run synchronized to avoid an illegal thread access exception.
			Display.getDefault().syncExec(new Runnable() {
				public void run() {
					// prepare full message
	            	String logLevelStr = logLevel.toString() + ": ";
	            	if (logLevelStr.length()==6) logLevelStr += " ";
	                String fullMessage = logLevelStr + message + "\n";
	                // add stack trace if available
	                if (t != null) {
	                    for (StackTraceElement elem : t.getStackTrace()) {
	                    	fullMessage += "   " + elem + "\n";
	                    }
	                }
	                // create stream, eventually set color
					MessageConsoleStream newMessageStream = console.newMessageStream();
		            if (logLevel == LogLevel.ERROR) {
		            	Color red = new Color(null, 255, 0, 0);
		            	newMessageStream.setColor(red); 
		            }
		            // stream
		            try {
		                newMessageStream.write(fullMessage);
		                newMessageStream.flush();
		            } catch (IOException e) {
		            	System.err.println("IOException while writing message to console: " + e.getMessage());
		                e.printStackTrace();
		                System.err.println("The message was: " + message);
		            } finally {
		                try {
		                    newMessageStream.close();
		                } catch (IOException e) {
			            	System.err.println("IOException at closing MessageConsoleStream: " + e.getMessage());
			                e.printStackTrace();
		                }
		            }
				}
			});
        }
		
        // Also log in "Error Log" view?
		if (insertIntoErrorLogView) {
			if (errorLogLogger != null) {
	        	IStatus status = new Status(logLevel.toIStatus(), pluginId, message, t);
	        	errorLogLogger.log(status);
			} else {
				debug("errorLogLogger is null, cannot log to 'Error Log' view!", new Throwable());
			}
		}
    }
    
    private String getVisibleLogLevel() {
    	String visibleLogLevel = Activator.getDefault().getPreferenceStore().getString(ExtensionToolsPreferenceConstants.LOG_LEVEL_PREFERENCE);
    	if (visibleLogLevel == null || visibleLogLevel.isEmpty()) {
    		// preference store not initialized yet
    		return "DEBUG";
    	}
    	return visibleLogLevel;
    }
}
