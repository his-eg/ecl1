package net.sf.ecl1.updatecheck;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.engine.IProfileRegistry;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.operations.ProvisioningJob;
import org.eclipse.equinox.p2.operations.ProvisioningSession;
import org.eclipse.equinox.p2.operations.Update;
import org.eclipse.equinox.p2.operations.UpdateOperation;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import net.sf.ecl1.utilities.general.ConsoleLogger;

/**
 * Provisioning platform util
 * 
 * taken from: 
 * 
 * @author keunecke
 *
 */
public class P2Util {
    private static final ConsoleLogger logger = new ConsoleLogger(UpdateCheckActivator.getDefault().getLog(), UpdateCheckActivator.PLUGIN_ID);

	static void doCheckForUpdates(IProgressMonitor monitor) {
		BundleContext bundleContext = UpdateCheckActivator.getDefault().getBundle().getBundleContext();
		ServiceReference<?> reference = bundleContext.getServiceReference(IProvisioningAgent.SERVICE_NAME);
		if (reference == null) {
			logger.error2("No provisioning agent found. This application is not set up for updates.");
			return;
		}
		
		final IProvisioningAgent agent = (IProvisioningAgent) bundleContext.getService(reference);
		try {
			checkForUpdates(agent, monitor);
		} finally {
			// TODO: In rare cases, the following ungetService() statement throws an IllegalStateException.
			// From https://www.cct.lsu.edu/~rguidry/ecl31docs/api/org/osgi/framework/BundleContext.html:
			// "The BundleContext object is only valid during the execution of its context bundle; 
			// that is, during the period from when the context bundle is in the STARTING, STOPPING, 
			// and ACTIVE bundle states. If the BundleContext object is used subsequently, an 
			// IllegalStateException must be thrown."
			try {
				bundleContext.ungetService(reference);
			} catch (IllegalStateException e) {
				logger.error2("bundleContext.ungetService() failed: " + e.getMessage(), e); 
			}
		}
	}

	static IStatus checkForUpdates(IProvisioningAgent agent, IProgressMonitor monitor) throws OperationCanceledException {
		ProvisioningSession session = new ProvisioningSession(agent);
		// Here we restrict the possible updates to ecl1
		IQuery<IInstallableUnit> query = QueryUtil.createLatestQuery(QueryUtil.createIUQuery("h1modulesfeature.feature.group"));
		logger.debug("Update Query Expression: " + query.getExpression());
		IProfileRegistry registry = (IProfileRegistry) agent.getService(IProfileRegistry.SERVICE_NAME);
		final IProfile profile = registry.getProfile(IProfileRegistry.SELF);
		IQueryResult<IInstallableUnit> result = profile.query(query, monitor);
		Set<IInstallableUnit> unitsForUpdate = result.toUnmodifiableSet();
		logger.debug("Installable Units for update: " + unitsForUpdate);
		
		UpdateOperation operation = new UpdateOperation(session);
		if(!unitsForUpdate.isEmpty()) {
			logger.info("Creating UpdateOperation for: " + unitsForUpdate);
			operation = new UpdateOperation(session, unitsForUpdate);
		}
		
		// Check if updates for ecl1 are available
		SubMonitor sub = SubMonitor.convert(monitor, "Checking for application updates...", 200);
		IStatus status = operation.resolveModal(sub.newChild(100));
		logger.info("Status after update check: " + status.getMessage());
		// TODO Currently all available software sites are searched for ecl1 updates (see #171739).
		// It would be much faster to identify the repository first and then do repository.query(...)

		// React on the update check result
		if (status.getCode() == UpdateOperation.STATUS_NOTHING_TO_UPDATE) {
			return status;
		}
		if (status.getSeverity() == IStatus.CANCEL)
			throw new OperationCanceledException();

		if (status.getSeverity() != IStatus.ERROR) {
			// More complex status handling might include showing the user what
			// updates are available if there are multiples, differentiating
			// patches vs. updates, etc. In this example, we simply update as
			// suggested by the operation.
			
			// Do the update
			status = restrictUpdateToEcl1(operation, monitor, sub);
			logger.info("Status after update: " + status.getMessage());
			
			if (status.getSeverity() == IStatus.CANCEL) {
				Throwable t = status.getException();
				if (t != null) {
					logger.error2(status.getMessage() + ": " + t.getMessage(), t);
				} else {
					logger.error2(status.getMessage());
				}
				throw new OperationCanceledException();
			}
		}
		return status;
	}

	// TODO The implementation of this method seems to be incomplete.
	//	    Is the ecl1 check not necessary anymore because above the query is restricted to h1modules ?
	// TODO uncomment code?
	private static IStatus restrictUpdateToEcl1(UpdateOperation operation, IProgressMonitor monitor, SubMonitor sub) {
		ProvisioningJob job = operation.getProvisioningJob(monitor);
		List<Update> chosenUpdates = Arrays.asList(operation.getPossibleUpdates());
		Update ecl1 = null;
		for (Update update : chosenUpdates) {
			logger.info("Possible Update from " + update.toUpdate.getId() + " " + update.toUpdate.getVersion() + " to " + update.replacement.getVersion());
			if(isEcl1(update)) {
				logger.info("Identified ecl1-Update: " + update.toUpdate.getId());
				ecl1 = update;
			}
		}
		
//		if(ecl1 != null) {
//		operation.setSelectedUpdates(new Update[]{ecl1});
		if (job == null) {
			return new Status(IStatus.ERROR, UpdateCheckActivator.PLUGIN_ID,
					"ProvisioningJob could not be created - does this application support p2 software installation?");
		}
		return job.runModal(sub.newChild(100));
//		}
//		return new Status(Status.INFO, UpdateCheckActivator.PLUGIN_ID, "No update for ecl1 found.");
	}

	private static boolean isEcl1(Update update) {
		String updateId = update.toUpdate.getId();
		logger.debug("updateId = " + updateId);
		if(updateId != null) {
			// TODO recognize ecl1
			return true;
		}
		return false;
	}
}
