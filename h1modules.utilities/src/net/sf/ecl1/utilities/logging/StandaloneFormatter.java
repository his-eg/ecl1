package net.sf.ecl1.utilities.logging;

import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class StandaloneFormatter extends Formatter {

    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_BLUE = "\u001B[34m";
    private static final String ANSI_YELLOW = "\u001B[33m";
    private static final String ANSI_RED = "\u001B[31m";

    @Override
    public String format(LogRecord record) {
        String levelName = record.getLevel().getName();
        String ansiColor;

        switch(levelName){
            case "FINE":
                levelName = "DEBUG";
                ansiColor = ANSI_GREEN;
                break;
            case "INFO":
                ansiColor = ANSI_BLUE;
                break;
            case "WARNING":
                ansiColor = ANSI_YELLOW;
                break;
            case "SEVERE":
                levelName = "ERROR";
                ansiColor = ANSI_RED;
                break;
            default:
                ansiColor = "";
                System.err.println("Got unknown log level in StandaloneFormatter: " + levelName);
                break;
        }

        return String.format("%1$s[%2$s] %3$s: %4$s %5$s%n", ansiColor, levelName, record.getLoggerName(), record.getMessage(), ANSI_RESET);
    }
}