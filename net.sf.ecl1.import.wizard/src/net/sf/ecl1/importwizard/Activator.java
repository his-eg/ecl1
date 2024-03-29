package net.sf.ecl1.importwizard;

import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "net.sf.ecl1.import.wizard"; //$NON-NLS-1$

	// The shared instance
	private static Activator plugin;
	
	private Job job;

	public void setJob(Job job) {
		this.job = job;
	}


	
	/**
	 * The constructor
	 */
	public Activator() {
	}

	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
	}

	public void stop(BundleContext context) throws Exception {
		if (job != null ) {
			job.cancel();
			job.join();
		}
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance of the plugin activator
	 *
	 * @return the shared instance
	 */
	public static Activator getDefault() {
		return plugin;
	}

}
