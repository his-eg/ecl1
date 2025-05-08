package net.sf.ecl1.utilities.logging;

public interface ICommonLogger {

    void debug(String message);
    void debug(String message, Throwable t);

    void info(String message);
    void info(String message, Throwable t);

    void warn(String message);
    void warn(String message, Throwable t);

    void warn2(String message);
    void warn2(String message, Throwable t);

    void error(String message);
    void error(String message, Throwable t);

    void error2(String message);
    void error2(String message, Throwable t);
}
