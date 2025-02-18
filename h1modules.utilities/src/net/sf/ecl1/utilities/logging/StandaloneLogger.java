package net.sf.ecl1.utilities.logging;

import org.slf4j.Logger;

/**
 * Class for logging to other environments than Eclipse.
 */
public class StandaloneLogger extends LoggerDelegateStub implements ICommonLogger{

    private final Logger slf4jLogger;

    public StandaloneLogger(Logger slf4jLogger) {
        this.slf4jLogger = slf4jLogger;
    }
    
    /**
     * Logs a debug log to the console.
     * @param message message
     */
    @Override
    public void debug(String message) {
    	slf4jLogger.debug(message);
    }

    /**
     * Logs a debug log to the console.
     * @param message message
     * @param t throwable
     */
    @Override
    public void debug(String message, Throwable t) {
        slf4jLogger.debug(message, t);
    }

    /**
     * Logs an info log to the console.
     * @param message message
     */
    @Override
    public void info(String message) {
    	slf4jLogger.info(message);
    }

    /**
     * Logs an info log to the console.
     * @param message message
     * @param t throwable
     */
    @Override
    public void info(String message, Throwable t) {
        slf4jLogger.info(message, t);
    }

    /**
     * Logs an error to the console.
     * @param message message
     */
    @Override
    public void warn(String message) {
    	slf4jLogger.warn(message);
    }

    /**
     * Logs an error to the console.
     * @param message message
     * @param t throwable
     */
    @Override
    public void warn(String message, Throwable t) {
    	slf4jLogger.warn(message, t);
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
    	slf4jLogger.error(message);
    }

    /**
     * Logs an error to the console .
     * @param message message
     * @param t throwable
     */
    @Override
    public void error(String message, Throwable t) {
    	slf4jLogger.error(message, t);
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
