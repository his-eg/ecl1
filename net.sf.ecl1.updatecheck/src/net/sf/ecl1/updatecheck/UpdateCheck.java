package net.sf.ecl1.updatecheck;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ui.IStartup;

import net.sf.ecl1.utilities.general.ConsoleLogger;

public class UpdateCheck implements IStartup {
    private static final ConsoleLogger logger = new ConsoleLogger(UpdateCheckActivator.getDefault().getLog(), UpdateCheckActivator.PLUGIN_ID);

	@Override
	public void earlyStartup() {
		logger.info("Update Check!");
		Job job = new Job("ecl1UpdateCheck") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				logger.info("Running scheduled ecl1 update check");
				doUpdateCheck(monitor);
				logger.info("Finished scheduled ecl1 update check");
				return Status.OK_STATUS;
			}
		};
		job.schedule();
	}
	
	void doUpdateCheck(IProgressMonitor monitor) {
		P2Util.doCheckForUpdates(monitor);
	}
}
