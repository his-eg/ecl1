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

    private enum LogLevel {
    	DEBUG,
    	INFO,
    	WARN,
    	ERROR;
    }

    private static final MessageConsole console = findConsole(Ecl1Constants.CONSOLE_NAME);
    
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

    public void debug(String message) {
    	log(LogLevel.DEBUG, message, null);
    }

    public void debug(String message, Throwable t) {
    	log(LogLevel.DEBUG, message, t);
    }

    public void info(String message) {
    	log(LogLevel.INFO, message, null);
    }

    public void info(String message, Throwable t) {
    	log(LogLevel.INFO, message, t);
    }

    public void warn(String message) {
    	log(LogLevel.WARN, message, null);
    }

    public void warn(String message, Throwable t) {
    	log(LogLevel.WARN, message, t);
    }

    public void error(String message) {
    	log(LogLevel.ERROR, message, null);
    }

    public void error(String message, Throwable t) {
    	log(LogLevel.ERROR, message, t);
    }
    
    /**
     * Log a message to the console.
     * 
     * @param message
     */
    private void log(LogLevel logLevel, String message, Throwable t) {
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
		
        // Always log warnings and errors in "Error Log" view
        if (errorLogLogger!=null && (logLevel == LogLevel.ERROR || logLevel == LogLevel.WARN)) {
        	int statusCode = (logLevel == LogLevel.ERROR) ? IStatus.ERROR : IStatus.WARNING;
        	IStatus status = new Status(statusCode, pluginId, message, t);
        	errorLogLogger.log(status);
        }
    }
    
    private String getVisibleLogLevel() {
    	return Activator.getDefault().getPreferenceStore().getString(ExtensionToolsPreferenceConstants.LOG_LEVEL_PREFERENCE);
    }
}
