package net.sf.ecl1.updatecheck;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import net.sf.ecl1.utilities.general.ConsoleLogger;

/**
 * The activator class controls the plug-in life cycle
 */
public class UpdateCheckActivator extends AbstractUIPlugin {

    private static final ConsoleLogger logger = ConsoleLogger.getEcl1Logger();

	// The plug-in ID
	public static final String PLUGIN_ID = "net.sf.ecl1.updatecheck"; //$NON-NLS-1$

	// The shared instance
	private static UpdateCheckActivator plugin;
	
	/**
	 * The constructor
	 */
	public UpdateCheckActivator() {
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static UpdateCheckActivator getDefault() {
		return plugin;
	}
	
	public static void info(String info) {
		log(new Status(IStatus.INFO, PLUGIN_ID,info));
	}
	
	public static void log(IStatus status) {
		logger.debug(status.getMessage());
		ILog log = getDefault().getLog();
		if (log != null) {
			log.log(status); // TODO logs to std:out and "Error Log" view
		} else {
			if (status.getException() != null)
				status.getException().printStackTrace();
		}
	}
	
	public static void log(Throwable e) {
		log(new Status(IStatus.ERROR, PLUGIN_ID, e.getMessage(), e));
	}

	public static void error(String message) {
		log(new Status(IStatus.ERROR, PLUGIN_ID, message));
	}

}
