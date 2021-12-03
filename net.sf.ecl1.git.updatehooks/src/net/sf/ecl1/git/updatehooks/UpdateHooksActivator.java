package net.sf.ecl1.git.updatehooks;

import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class UpdateHooksActivator extends Plugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "net.sf.ecl1.git.updatehooks"; //$NON-NLS-1$

	// The shared instance
	private static UpdateHooksActivator plugin;
	
	private Job updateHooksJob;
	
	public void setUpdateHooksJobs(Job job) {
		updateHooksJob = job;
	}
	
	/**
	 * The constructor
	 */
	public UpdateHooksActivator() {
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
		//Wait for the update hooks job to complete since no job of this plugin must run after the plugin stopped
		if (updateHooksJob != null) {
			updateHooksJob.cancel();
			updateHooksJob.join();
		}
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static UpdateHooksActivator getDefault() {
		return plugin;
	}
}
