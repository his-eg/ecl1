package net.sf.ecl1.git;

import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * Plugin life cycle management
 *  
 * @author keunecke
 */
public class Activator extends AbstractUIPlugin {

	public static final String PLUGIN_ID = "net.sf.ecl1.git.batch-pull-button";

	// The shared instance
	private static Activator plugin;
	
	private Job gitBatchPullJob;
	
	public void setGitBatchPullJob(Job job) {
		gitBatchPullJob = job;
	}
	
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
		if ( gitBatchPullJob != null ) {
			gitBatchPullJob.cancel();
			gitBatchPullJob.join();
		}
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
