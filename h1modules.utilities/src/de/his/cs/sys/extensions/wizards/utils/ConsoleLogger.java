package de.his.cs.sys.extensions.wizards.utils;

import java.io.IOException;

import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;

/**
 * Helper class for logging messages to a console.
 * 
 * @author keunecke, tneumann
 */
public class ConsoleLogger {

    /** project in which context we are logging */
    private String targetProjectName;

    private String consoleName;

    private boolean debugging = true;

    /**
     * Create a new ConsoleLogger.
     * 
     * @param targetProjectName the name of the project ecl1 is currently working on 
     * @param consoleName The name of the console (should be something like ECL1)
     */
    public ConsoleLogger(String targetProjectName, String consoleName) {
        this.targetProjectName = targetProjectName;
        this.consoleName = consoleName;
    }

    /**
     * Log a message to the console.
     * 
     * @param message
     */
    public void logToConsole(String message) {
        if (debugging) {
            MessageConsole console = findConsole(consoleName);
            MessageConsoleStream newMessageStream = console.newMessageStream();
            try {
                newMessageStream.write("[" + targetProjectName + "] " + message + "\n");
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

    private MessageConsole findConsole(String name) {
        ConsolePlugin plugin = ConsolePlugin.getDefault();
        IConsoleManager conMan = plugin.getConsoleManager();
        IConsole[] existing = conMan.getConsoles();
        for (int i = 0; i < existing.length; i++)
            if (name.equals(existing[i].getName())) return (MessageConsole) existing[i];
        //no console found, so create a new one
        MessageConsole myConsole = new MessageConsole(name, null);
        conMan.addConsoles(new IConsole[] { myConsole });
        return myConsole;
    }
}
