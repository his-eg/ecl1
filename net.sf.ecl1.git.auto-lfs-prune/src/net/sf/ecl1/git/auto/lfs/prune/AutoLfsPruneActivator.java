package net.sf.ecl1.git.auto.lfs.prune;

import org.eclipse.core.runtime.Plugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class AutoLfsPruneActivator extends Plugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "net.sf.ecl1.git.auto-lfs-prune"; //$NON-NLS-1$

	// The shared instance
	private static AutoLfsPruneActivator plugin;
	
	private AutoLfsPruneJob pruneJob;

	/**
	 * The constructor
	 */
	public AutoLfsPruneActivator() {
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		pruneJob = new AutoLfsPruneJob();
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		//Cancel job properly if platform (eclipse) shuts down. Otherwise we would get a nasty entry in our error log...
		if (pruneJob != null) {
			pruneJob.cancel();
			//Wait for cancel to complete
			pruneJob.join();
		}
		super.stop(context);
		plugin = null;
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static AutoLfsPruneActivator getDefault() {
		return plugin;
	}
	
	public AutoLfsPruneJob getPruneJob() {
		return pruneJob;
	}
}
