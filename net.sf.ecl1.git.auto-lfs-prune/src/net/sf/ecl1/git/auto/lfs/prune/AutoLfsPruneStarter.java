package net.sf.ecl1.git.auto.lfs.prune;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.ui.IStartup;

public class AutoLfsPruneStarter implements IStartup {
    	
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    
	@Override
	public void earlyStartup() {	
		
		AutoLfsPruneJob pruneJob;
		if(net.sf.ecl1.utilities.Activator.isRunningInEclipse()){
			pruneJob = AutoLfsPruneActivator.getDefault().getPruneJob();
		}else{
			pruneJob = new AutoLfsPruneJob();
		}
		
		// Schedules a new auto lfs prune job for immediate execution when run
		Runnable autoLfsPruneJobScheduler = new Runnable() {
			
			@Override
			public void run() {
				if(net.sf.ecl1.utilities.Activator.isRunningInEclipse()){
					pruneJob.schedule();
				}else{
					pruneJob.run(new NullProgressMonitor());
				}
			}
		};
		
		/*
		 * Schedule the scheduler ;)
		 * The runnable is invoked immediately and then periodically every day. If the runnable is run, a new auto lfs prune job is scheduled.
		 * 
		 * Why do we run periodically every day?
		 * At least one user (see 251312#c26) is leaving eclipse open for a very long time (multiple days). This user
		 * wants eclipse to run "git lfs prune" once a day automatically. 
		 * 
		 * Why do we schedule a runnable on the {@see ScheduledExecutorService} to schedule the eclipse job? Why not use the eclipse scheduler to run the job periodically?
		 * The officially recommended way of implementing a repeating eclipse job is described here: 
		 * https://wiki.eclipse.org/FAQ_How_do_I_create_a_repeating_background_task%3F
		 * The recommended approach has one downside, however: 
		 * The progress bar for this job will not disappear and will instead indicate a sleeping job to the user.  
		 * This is correct (since the job is indeed sleeping), but some users (including me) did not like the indication
		 * of a sleeping job that never goes away (actually I discovered, that it is no longer displayed, when closing and 
		 * re-opening the progress-view, but some users never close the progress-view and therefore the sleeping indication never goes away).
		 * Since the state of the java scheduler is not graphically represented by eclipse, we use it to schedule the eclipse job periodically. 
		 */
		scheduler.scheduleAtFixedRate(autoLfsPruneJobScheduler, 0, 1, TimeUnit.DAYS);
	}
		
}
