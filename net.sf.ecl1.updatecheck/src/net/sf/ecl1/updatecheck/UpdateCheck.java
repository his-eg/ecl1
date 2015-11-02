package net.sf.ecl1.updatecheck;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.IJobRunnable;

public class UpdateCheck implements IStartup {

	@Override
	public void earlyStartup() {
		System.out.println("Update Check!");
		Job job = new Job("ecl1UpdateCheck") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				System.out.println("Running scheduled ecl1 update check");
				doUpdateCheck(monitor);
				System.out.println("Finished scheduled ecl1 update check");
				return Status.OK_STATUS;
			}
		};
		job.schedule();
	}
	
	void doUpdateCheck(IProgressMonitor monitor) {
		P2Util.doCheckForUpdates(monitor);
	}
	
}