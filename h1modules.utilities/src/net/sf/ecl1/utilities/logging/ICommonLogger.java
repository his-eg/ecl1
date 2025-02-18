package net.sf.ecl1.utilities.logging;

import org.slf4j.Logger;

public interface ICommonLogger extends Logger {

    void warn2(String message);
    void warn2(String message, Throwable t);

    void error2(String message);
    void error2(String message, Throwable t);
}
