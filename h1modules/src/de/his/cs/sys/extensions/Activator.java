package de.his.cs.sys.extensions;

import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

	/** The plug-in ID */
	public static final String PLUGIN_ID = "de.his.h1moduleswizard"; //$NON-NLS-1$

	/** The shared instance */
	private static Activator plugin;
	
	private Job job;
	
	

	public void setJob(Job job) {
		this.job = job;
	}

	/**
	 * starts the plugin
	 *
	 * @param context a BundleContext
	 * @throws Exception on unexpected errors
	 */
	@Override
	public void start(BundleContext context) throws Exception {
		if (job != null) {
			job.cancel();
			job.join();
		}
		super.start(context);
		plugin = this;
	}

	@Override
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

}
