package net.sf.ecl1.updatecheck;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ui.IStartup;

public class UpdateCheck implements IStartup {

	@Override
	public void earlyStartup() {
		UpdateCheckActivator.info("Update Check!"); // TODO logs in std:out console and "Error Log" view
		Job job = new Job("ecl1UpdateCheck") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				UpdateCheckActivator.info("Running scheduled ecl1 update check");
				doUpdateCheck(monitor);
				UpdateCheckActivator.info("Finished scheduled ecl1 update check");
				return Status.OK_STATUS;
			}
		};
		job.schedule();
	}
	
	void doUpdateCheck(IProgressMonitor monitor) {
		P2Util.doCheckForUpdates(monitor);
	}
}
