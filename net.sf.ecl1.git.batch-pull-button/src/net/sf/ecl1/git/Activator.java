package net.sf.ecl1.git;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
/**
 * Plugin life cycle management
 *  
 * @author keunecke
 */
public class Activator extends AbstractUIPlugin {
	
	public static String PLUGIN_ID = "net.sf.ecl1.git.batch-pull-button";
	

	// The shared instance
	private static Activator plugin;
	
	/**
	 * The constructor
	 */
	public Activator() {
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
	public static Activator getDefault() {
		return plugin;
	}
	
	public static void info(String info) {
		log(new Status(IStatus.INFO, PLUGIN_ID,info));
	}
	
	public static void log(IStatus status) {
		ILog log = getDefault().getLog();
		if (log != null) {
			log.log(status);
		} else {
			System.out.println(status.getMessage());
			if (status.getException() != null)
				status.getException().printStackTrace();
		}
	}
	
	public static void error(String message, Throwable e) {
		log(new Status(IStatus.ERROR, PLUGIN_ID, message, e));
	}

	public static void error(String message) {
		log(new Status(IStatus.ERROR, PLUGIN_ID, message));
	}

}
