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
    private static final ConsoleLogger logger = ConsoleLogger.getEcl1Logger();

	static void doCheckForUpdates(IProgressMonitor monitor) {
		BundleContext bundleContext = UpdateCheckActivator.getDefault().getBundle().getBundleContext();
		@SuppressWarnings("rawtypes")
		ServiceReference reference = bundleContext.getServiceReference(IProvisioningAgent.SERVICE_NAME);
		if (reference == null) {
			UpdateCheckActivator.error("No provisioning agent found.  This application is not set up for updates.");
			return;
		}

		@SuppressWarnings("unchecked")
		final IProvisioningAgent agent = (IProvisioningAgent) bundleContext.getService(reference);
		try {
			@SuppressWarnings("unused") // in case we need it later
			IStatus updateStatus = P2Util.checkForUpdates(agent, monitor);
		} finally {
			bundleContext.ungetService(reference);
		}
	}

	static IStatus checkForUpdates(IProvisioningAgent agent, IProgressMonitor monitor) throws OperationCanceledException {
		
		// Here we restrict the possible updates to ecl1
		IQuery<IInstallableUnit> query = QueryUtil.createLatestQuery(QueryUtil.createIUQuery("h1modulesfeature.feature.group"));
		//UpdateCheckActivator.info("Update Query Expression: " + query.getExpression());
		IProfileRegistry registry= (IProfileRegistry) agent.getService(IProfileRegistry.SERVICE_NAME);
		final IProfile profile= registry.getProfile(IProfileRegistry.SELF);
		IQueryResult<IInstallableUnit> result = profile.query(query, monitor);
		Set<IInstallableUnit> unitsForUpdate = result.toUnmodifiableSet();
		
		UpdateCheckActivator.info("Installable Units for update: " + unitsForUpdate);
		ProvisioningSession session = new ProvisioningSession(agent);
		UpdateOperation operation = new UpdateOperation(session);
		if(!unitsForUpdate.isEmpty()) {
			UpdateCheckActivator.info("Creating UpdateOperation for: " + unitsForUpdate);
			operation = new UpdateOperation(session, unitsForUpdate);
		}
	    
		// Check if updates for ecl1 are available
		SubMonitor sub = SubMonitor.convert(monitor, "Checking for application updates...", 200);
		IStatus status = operation.resolveModal(sub.newChild(100));
		UpdateCheckActivator.info("Status after update check: " + status.getMessage());

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
			UpdateCheckActivator.info("Status after update: " + status.getMessage());
			
			if (status.getSeverity() == IStatus.CANCEL) {
				UpdateCheckActivator.error(status.getMessage());
				if(status.getException() != null) {
					UpdateCheckActivator.error(status.getException());
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
			UpdateCheckActivator.info("Possible Update from " + update.toUpdate.getId() + " " + update.toUpdate.getVersion() + " to " + update.replacement.getVersion());
			if(isEcl1(update)) {
				UpdateCheckActivator.info("Identified ecl1-Update: " + update.toUpdate.getId());
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
