package net.sf.ecl1.classpath;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "net.sf.ecl1.classpath"; //$NON-NLS-1$

	// The shared instance
	private static Activator plugin;
	
	private Queue<Job> jobQueue = new ConcurrentLinkedQueue<>();
	
	synchronized public void addJob(Job job) {
		jobQueue.add(job);
	}
	
	synchronized public boolean isJobQueueEmpty() {
		return jobQueue.isEmpty();
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
		if (!jobQueue.isEmpty()) {
			for(Job job : jobQueue) {
				job.cancel();
				job.join();
			}
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
