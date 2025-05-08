package net.sf.ecl1.utilities.logging;

import org.eclipse.core.runtime.IStatus;

/**
 * Log levels available to the console logger.
 * @author tneumann
 */
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
