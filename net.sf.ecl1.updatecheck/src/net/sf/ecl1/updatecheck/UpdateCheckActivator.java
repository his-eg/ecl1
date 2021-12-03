package net.sf.ecl1.updatecheck;

import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class UpdateCheckActivator extends Plugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "net.sf.ecl1.updatecheck"; //$NON-NLS-1$

	// The shared instance
	private static UpdateCheckActivator plugin;
	
	private Job checkUpdatesJob;
	
	public void setCheckUpdatesJob(Job job) {
		checkUpdatesJob = job;
	}
	
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
		//Wait for the job to complete since no job of this plugin must run after the plugin stopped
		if ( checkUpdatesJob != null ) {
			checkUpdatesJob.cancel();
			checkUpdatesJob.join();
		}
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
}
