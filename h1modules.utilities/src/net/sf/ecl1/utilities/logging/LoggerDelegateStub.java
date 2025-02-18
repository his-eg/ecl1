package net.sf.ecl1.utilities.logging;

import java.util.Arrays;

import org.slf4j.Marker;

/**
 * Base class for not implemented {@link org.slf4j.Logger} methods.
 */
public abstract class LoggerDelegateStub {

    public String getName() {
        throw new UnsupportedOperationException("Unimplemented method 'getName'");
    }

    public boolean isTraceEnabled() {
        throw new UnsupportedOperationException("Unimplemented method 'isTraceEnabled'");
    }

    public void trace(String msg) {
        throw new UnsupportedOperationException("Unimplemented method 'trace' with msg: " + msg);
    }

    public void trace(String format, Object arg) {
        throw new UnsupportedOperationException("Unimplemented method 'trace' with format: " + format + " and arg: " + arg);
    }

    public void trace(String format, Object arg1, Object arg2) {
        throw new UnsupportedOperationException("Unimplemented method 'trace' with format: " + format + " and args: " + arg1 + ", " + arg2);
    }

    public void trace(String format, Object... arguments) {
        throw new UnsupportedOperationException("Unimplemented method 'trace' with format: " + format + " and arguments: " + Arrays.toString(arguments));
    }

    public void trace(String msg, Throwable t) {
        throw new UnsupportedOperationException("Unimplemented method 'trace' with msg: " + msg + " and throwable: " + t);
    }

    public boolean isTraceEnabled(Marker marker) {
        throw new UnsupportedOperationException("Unimplemented method 'isTraceEnabled' with marker: " + marker);
    }

    public void trace(Marker marker, String msg) {
        throw new UnsupportedOperationException("Unimplemented method 'trace' with marker: " + marker + " and msg: " + msg);
    }

    public void trace(Marker marker, String format, Object arg) {
        throw new UnsupportedOperationException("Unimplemented method 'trace' with marker: " + marker + ", format: " + format + " and arg: " + arg);
    }

    public void trace(Marker marker, String format, Object arg1, Object arg2) {
        throw new UnsupportedOperationException("Unimplemented method 'trace' with marker: " + marker + ", format: " + format + " and args: " + arg1 + ", " + arg2);
    }

    public void trace(Marker marker, String format, Object... argArray) {
        throw new UnsupportedOperationException("Unimplemented method 'trace' with marker: " + marker + ", format: " + format + " and arguments: " + Arrays.toString(argArray));
    }

    public void trace(Marker marker, String msg, Throwable t) {
        throw new UnsupportedOperationException("Unimplemented method 'trace' with marker: " + marker + ", msg: " + msg + " and throwable: " + t);
    }

    public boolean isDebugEnabled() {
        throw new UnsupportedOperationException("Unimplemented method 'isDebugEnabled'");
    }

    public void debug(String format, Object arg) {
        throw new UnsupportedOperationException("Unimplemented method 'debug' with format: " + format + " and arg: " + arg);
    }

    public void debug(String format, Object arg1, Object arg2) {
        throw new UnsupportedOperationException("Unimplemented method 'debug' with format: " + format + " and args: " + arg1 + ", " + arg2);
    }

    public void debug(String format, Object... arguments) {
        throw new UnsupportedOperationException("Unimplemented method 'debug' with format: " + format + " and arguments: " + Arrays.toString(arguments));
    }

    public boolean isDebugEnabled(Marker marker) {
        throw new UnsupportedOperationException("Unimplemented method 'isDebugEnabled' with marker: " + marker);
    }

    public void debug(Marker marker, String msg) {
        throw new UnsupportedOperationException("Unimplemented method 'debug' with marker: " + marker + " and msg: " + msg);
    }

    public void debug(Marker marker, String format, Object arg) {
        throw new UnsupportedOperationException("Unimplemented method 'debug' with marker: " + marker + ", format: " + format + " and arg: " + arg);
    }

    public void debug(Marker marker, String format, Object arg1, Object arg2) {
        throw new UnsupportedOperationException("Unimplemented method 'debug' with marker: " + marker + ", format: " + format + " and args: " + arg1 + ", " + arg2);
    }

    public void debug(Marker marker, String format, Object... arguments) {
        throw new UnsupportedOperationException("Unimplemented method 'debug' with marker: " + marker + ", format: " + format + " and arguments: " + Arrays.toString(arguments));
    }

    public void debug(Marker marker, String msg, Throwable t) {
        throw new UnsupportedOperationException("Unimplemented method 'debug' with marker: " + marker + ", msg: " + msg + " and throwable: " + t);
    }

    public boolean isInfoEnabled() {
        throw new UnsupportedOperationException("Unimplemented method 'isInfoEnabled'");
    }

    public void info(String format, Object arg) {
        throw new UnsupportedOperationException("Unimplemented method 'info' with format: " + format + " and arg: " + arg);
    }

    public void info(String format, Object arg1, Object arg2) {
        throw new UnsupportedOperationException("Unimplemented method 'info' with format: " + format + " and args: " + arg1 + ", " + arg2);
    }

    public void info(String format, Object... arguments) {
        throw new UnsupportedOperationException("Unimplemented method 'info' with format: " + format + " and arguments: " + Arrays.toString(arguments));
    }

    public boolean isInfoEnabled(Marker marker) {
        throw new UnsupportedOperationException("Unimplemented method 'isInfoEnabled' with marker: " + marker);
    }

    public void info(Marker marker, String msg) {
        throw new UnsupportedOperationException("Unimplemented method 'info' with marker: " + marker + " and msg: " + msg);
    }

    public void info(Marker marker, String format, Object arg) {
        throw new UnsupportedOperationException("Unimplemented method 'info' with marker: " + marker + ", format: " + format + " and arg: " + arg);
    }

    public void info(Marker marker, String format, Object arg1, Object arg2) {
        throw new UnsupportedOperationException("Unimplemented method 'info' with marker: " + marker + ", format: " + format + " and args: " + arg1 + ", " + arg2);
    }

    public void info(Marker marker, String format, Object... arguments) {
        throw new UnsupportedOperationException("Unimplemented method 'info' with marker: " + marker + ", format: " + format + " and arguments: " + Arrays.toString(arguments));
    }

    public void info(Marker marker, String msg, Throwable t) {
        throw new UnsupportedOperationException("Unimplemented method 'info' with marker: " + marker + ", msg: " + msg + " and throwable: " + t);
    }

    public boolean isWarnEnabled() {
        throw new UnsupportedOperationException("Unimplemented method 'isWarnEnabled'");
    }

    public void warn(String format, Object arg) {
        throw new UnsupportedOperationException("Unimplemented method 'warn' with format: " + format + " and arg: " + arg);
    }

    public void warn(String format, Object... arguments) {
        throw new UnsupportedOperationException("Unimplemented method 'warn' with format: " + format + " and arguments: " + Arrays.toString(arguments));
    }

    public void warn(String format, Object arg1, Object arg2) {
        throw new UnsupportedOperationException("Unimplemented method 'warn' with format: " + format + " and args: " + arg1 + ", " + arg2);
    }

    public boolean isWarnEnabled(Marker marker) {
        throw new UnsupportedOperationException("Unimplemented method 'isWarnEnabled' with marker: " + marker);
    }

    public void warn(Marker marker, String msg) {
        throw new UnsupportedOperationException("Unimplemented method 'warn' with marker: " + marker + " and msg: " + msg);
    }

    public void warn(Marker marker, String format, Object arg) {
        throw new UnsupportedOperationException("Unimplemented method 'warn' with marker: " + marker + ", format: " + format + " and arg: " + arg);
    }

    public void warn(Marker marker, String format, Object arg1, Object arg2) {
        throw new UnsupportedOperationException("Unimplemented method 'warn' with marker: " + marker + ", format: " + format + " and args: " + arg1 + ", " + arg2);
    }

    public void warn(Marker marker, String format, Object... arguments) {
        throw new UnsupportedOperationException("Unimplemented method 'warn' with marker: " + marker + ", format: " + format + " and arguments: " + Arrays.toString(arguments));
    }

    public void warn(Marker marker, String msg, Throwable t) {
        throw new UnsupportedOperationException("Unimplemented method 'warn' with marker: " + marker + ", msg: " + msg + " and throwable: " + t);
    }

    public boolean isErrorEnabled() {
        throw new UnsupportedOperationException("Unimplemented method 'isErrorEnabled'");
    }

    public void error(String format, Object arg) {
        throw new UnsupportedOperationException("Unimplemented method 'error' with format: " + format + " and arg: " + arg);
    }

    public void error(String format, Object arg1, Object arg2) {
        throw new UnsupportedOperationException("Unimplemented method 'error' with format: " + format + " and args: " + arg1 + ", " + arg2);
    }

    public void error(String format, Object... arguments) {
        throw new UnsupportedOperationException("Unimplemented method 'error' with format: " + format + " and arguments: " + Arrays.toString(arguments));
    }

    public boolean isErrorEnabled(Marker marker) {
        throw new UnsupportedOperationException("Unimplemented method 'isErrorEnabled' with marker: " + marker);
    }

    public void error(Marker marker, String msg) {
        throw new UnsupportedOperationException("Unimplemented method 'error' with marker: " + marker + " and msg: " + msg);
    }

    public void error(Marker marker, String format, Object arg) {
        throw new UnsupportedOperationException("Unimplemented method 'error' with marker: " + marker + ", format: " + format + " and arg: " + arg);
    }

    public void error(Marker marker, String format, Object arg1, Object arg2) {
        throw new UnsupportedOperationException("Unimplemented method 'error' with marker: " + marker + ", format: " + format + " and args: " + arg1 + ", " + arg2);
    }

    public void error(Marker marker, String format, Object... arguments) {
        throw new UnsupportedOperationException("Unimplemented method 'error' with marker: " + marker + ", format: " + format + " and arguments: " + Arrays.toString(arguments));
    }

    public void error(Marker marker, String msg, Throwable t) {
        throw new UnsupportedOperationException("Unimplemented method 'error' with marker: " + marker + ", msg: " + msg + " and throwable: " + t);
    }
}
