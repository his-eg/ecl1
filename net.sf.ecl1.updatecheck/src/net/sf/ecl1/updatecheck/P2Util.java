package net.sf.ecl1.updatecheck;

import java.util.Arrays;
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
 * Provisioning platform util.
 * 
 * @author keunecke
 */
public class P2Util {
    private static final ConsoleLogger logger = new ConsoleLogger(UpdateCheckActivator.getDefault().getLog(), UpdateCheckActivator.PLUGIN_ID, P2Util.class.getSimpleName());

    private static final String ECL1_UPDATE_ID = "h1modulesfeature.feature.group";
    
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

	private static IStatus checkForUpdates(IProvisioningAgent agent, IProgressMonitor monitor) throws OperationCanceledException {
		ProvisioningSession session = new ProvisioningSession(agent);
		// Here we restrict the possible updates to ecl1
		IQuery<IInstallableUnit> query = QueryUtil.createLatestQuery(QueryUtil.createIUQuery(ECL1_UPDATE_ID));
		logger.debug("Update Query Expression: " + query.getExpression());
		IProfileRegistry registry = (IProfileRegistry) agent.getService(IProfileRegistry.SERVICE_NAME);
		final IProfile profile = registry.getProfile(IProfileRegistry.SELF);
		if (profile == null ) {
			String errorMessage = "Could not update ecl1, because the running profile instance could not be located. "
					+ "One possible reason for failing to locate the running profile instance is that this eclipse instance was started from another eclipse instance. "
					+ "When starting eclipse from another eclipse instance, make sure to check the checkbox \"Support software installation in the launched application\" under Run Configurations --> Configuration";
			logger.error2(errorMessage);
			return new Status(IStatus.ERROR, ECL1_UPDATE_ID, errorMessage);
		}
		IQueryResult<IInstallableUnit> result = profile.query(query, monitor);
		Set<IInstallableUnit> unitsForUpdate = result.toUnmodifiableSet();
		logger.debug("Installable Units for update: " + unitsForUpdate);
		
		UpdateOperation operation;
		if(!unitsForUpdate.isEmpty()) {
			logger.info("Creating UpdateOperation for: " + unitsForUpdate);
			operation = new UpdateOperation(session, unitsForUpdate);
		} else {
			// XXX can this case happen at all ?
			logger.debug("Creating UpdateOperation for everything");
			operation = new UpdateOperation(session);
		}
		
		// Check if updates for ecl1 are available
		SubMonitor sub = SubMonitor.convert(monitor, "Checking for application updates...", 200);
		IStatus status = operation.resolveModal(sub.newChild(100)); // sets possible updates
		logger.info("Status after update check: " + status.getMessage());
		Update[] possibleUpdates = operation.getPossibleUpdates();
		logger.debug("Possible updates = " + Arrays.toString(possibleUpdates));

		// React on the update check result
		if (status.getCode() == UpdateOperation.STATUS_NOTHING_TO_UPDATE) return status;
		if (status.getSeverity() == IStatus.CANCEL) throw new OperationCanceledException();
		
		if (status.getSeverity() != IStatus.ERROR) {
			// More complex status handling might include showing the user what
			// updates are available if there are multiples, differentiating
			// patches vs. updates, etc. In this example, we simply update as
			// suggested by the operation.
			
			Update ecl1Update = findEcl1Update(possibleUpdates); // XXX only required if the update operation is not restricted to ecl1 ?
			if(ecl1Update != null) {
				operation.setSelectedUpdates(new Update[]{ecl1Update});
				ProvisioningJob job = operation.getProvisioningJob(monitor);
				if (job == null) {
					status = new Status(IStatus.ERROR, UpdateCheckActivator.PLUGIN_ID,
							"ProvisioningJob could not be created - does this application support p2 software installation?");
				} else {
					// Do the update
					status = job.runModal(sub.newChild(100));
					if (status.getSeverity() == IStatus.CANCEL) { // the update has been cancelled by the user
						Throwable t = status.getException();
						if (t != null) {
							logger.error2(status.getMessage() + ": " + t.getMessage(), t);
						} else {
							logger.error2(status.getMessage());
						}
						throw new OperationCanceledException();
					}
				}
			} else {
				status = new Status(Status.INFO, UpdateCheckActivator.PLUGIN_ID, "No update for ecl1 found.");
			}
			logger.info("Status after update: " + status.getMessage());
		}
		return status;
	}

	private static Update findEcl1Update(Update[] possibleUpdates) {
		if (possibleUpdates != null && possibleUpdates.length>0) {
			for (Update update : possibleUpdates) {
				String updateId = update.toUpdate.getId();
				if(updateId != null && updateId.equals(ECL1_UPDATE_ID)) {
					logger.info("Found ecl1 update " + update);
					return update;
				} else {
					logger.debug("Discarded non-ecl1 update " + update);
				}
			}
			logger.info("None of the available updates is an ecl1 update...");
		} else {
			logger.info("There are no updates available");
		}
		return null;
	}
}
