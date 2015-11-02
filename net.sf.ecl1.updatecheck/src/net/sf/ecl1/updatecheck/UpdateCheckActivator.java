package net.sf.ecl1.updatecheck;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class UpdateCheckActivator extends AbstractUIPlugin {

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
		ILog log = getDefault().getLog();
		if (log != null) {
			log.log(status);
		} else {
			System.out.println(status.getMessage());
			if (status.getException() != null)
				status.getException().printStackTrace();
		}
	}
	
	public static void log(Exception e) {
		log(new Status(IStatus.ERROR, PLUGIN_ID, e.getMessage(), e));
	}

}
