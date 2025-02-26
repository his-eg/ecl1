package net.sf.ecl1.utilities.logging;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.ecl1.utilities.preferences.PreferenceWrapper;


/**
 * Class for logging to other environments than Eclipse.
 */
public class StandaloneLogger implements ICommonLogger{

    private final Logger logger;

    public StandaloneLogger(String className) {
        logger = Logger.getLogger(className);
        Level level = getLogLevel();
        for (Handler handler : logger.getParent().getHandlers()) {
            handler.setLevel(level);
            handler.setFormatter(new StandaloneFormatter());
        }
        logger.setLevel(level);
    }

    private Level getLogLevel(){
        String preferenceLogLevel = PreferenceWrapper.getLogLevel();
        switch (preferenceLogLevel) {
            case "DEBUG": 
                return Level.FINE;
            case "INFO": 
                return Level.INFO;
            case "WARN": 
                return Level.WARNING;
            case "ERROR": 
                return Level.SEVERE;
            default:
                //no preference store, use fine/debug as default
                return Level.FINE;
        }
    }
    
    /**
     * Logs a debug log to the console.
     * @param message message
     */
    @Override
    public void debug(String message) {
        logger.log(Level.FINE, message);
    }

    /**
     * Logs a debug log to the console.
     * @param message message
     * @param t throwable
     */
    @Override
    public void debug(String message, Throwable t) {
        logger.log(Level.FINE, message, t);
    }

    /**
     * Logs an info log to the console.
     * @param message message
     */
    @Override
    public void info(String message) {
    	logger.log(Level.INFO, message);
    }

    /**
     * Logs an info log to the console.
     * @param message message
     * @param t throwable
     */
    @Override
    public void info(String message, Throwable t) {
        logger.log(Level.INFO, message, t);
    }

    /**
     * Logs an error to the console.
     * @param message message
     */
    @Override
    public void warn(String message) {
    	logger.log(Level.WARNING, message);
    }

    /**
     * Logs an error to the console.
     * @param message message
     * @param t throwable
     */
    @Override
    public void warn(String message, Throwable t) {
    	logger.log(Level.WARNING, message, t);
    }

    /**
     * Logs a warning to console. StandaloneLogger does NOT log to eclipse "Error Log" view.
     * @param message message
     */
    @Override
    public void warn2(String message) {
    	warn(message);
    }

    /**
     * Logs a warning to console. StandaloneLogger does NOT log to eclipse "Error Log" view.
     * @param message message
     * @param t throwable
     */
    @Override
    public void warn2(String message, Throwable t) {
    	warn(message, t);
    }

    /**
     * Logs an error to the console .
     * @param message message
     */
    @Override
    public void error(String message) {
    	logger.log(Level.SEVERE, message);
    }

    /**
     * Logs an error to the console .
     * @param message message
     * @param t throwable
     */
    @Override
    public void error(String message, Throwable t) {
    	logger.log(Level.SEVERE, message, t);
    }

    /**
     * Logs an error to console. StandaloneLogger does NOT log to eclipse "Error Log" view.
     * @param message message
     */
    @Override
    public void error2(String message) {
    	error(message);
    }

    /**
     * Logs an error to console. StandaloneLogger does NOT log to eclipse "Error Log" view.
     * @param message message
     * @param t throwable
     */
    @Override
    public void error2(String message, Throwable t) {
    	error(message, t);
    }
}
