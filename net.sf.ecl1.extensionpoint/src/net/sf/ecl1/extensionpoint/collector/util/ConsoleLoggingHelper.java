package net.sf.ecl1.extensionpoint.collector.util;

import java.io.IOException;

import net.sf.ecl1.extensionpoint.ExtensionPointBuilderPlugin;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;

/**
 * Helper class for logging messages to a console
 * 
 * @author keunecke
 *
 */
public class ConsoleLoggingHelper {

    /** project in which context we are logging */
    private IJavaProject javaProject;

    private String name;
    
    private final boolean loggingEnabled;

    /**
     * Create a new ConsoleLoggingHelper
     * 
     * @param javaProject
     * @param name
     */
    public ConsoleLoggingHelper(IJavaProject javaProject, String name, String activationPreferenceKey) {
        this.javaProject = javaProject;
        this.name = name;
        IPreferenceStore preferenceStore = ExtensionPointBuilderPlugin.getDefault().getPreferenceStore();
        this.loggingEnabled = preferenceStore.getBoolean(activationPreferenceKey);
    }

    /**
     * Log a message to the console
     * 
     * @param message
     */
    public void logToConsole(String message) {
    	if(this.loggingEnabled) {
    		MessageConsole console = findConsole(this.name);
    		MessageConsoleStream newMessageStream = console.newMessageStream();
    		try {
    			newMessageStream.write("[" + this.javaProject.getElementName() + "] " + message + "\n");
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


}
