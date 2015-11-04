package net.sf.ecl1.updatecheck;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.equinox.internal.p2.ui.model.ElementUtils;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.engine.IProfileRegistry;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.MetadataFactory;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.operations.ProvisioningJob;
import org.eclipse.equinox.p2.operations.ProvisioningSession;
import org.eclipse.equinox.p2.operations.Update;
import org.eclipse.equinox.p2.operations.UpdateOperation;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**
 * Provisioning platform util
 * 
 * taken from: 
 * 
 * @author keunecke
 *
 */
public class P2Util {

	static void doCheckForUpdates(IProgressMonitor monitor) {
		BundleContext bundleContext = UpdateCheckActivator.getDefault().getBundle().getBundleContext();
		@SuppressWarnings("rawtypes")
		ServiceReference reference = bundleContext.getServiceReference(IProvisioningAgent.SERVICE_NAME);
		if (reference == null) {
			UpdateCheckActivator.log(new Status(IStatus.ERROR, UpdateCheckActivator.PLUGIN_ID,
					"No provisioning agent found.  This application is not set up for updates."));
			return;
		}

		@SuppressWarnings("unchecked")
		final IProvisioningAgent agent = (IProvisioningAgent) bundleContext.getService(reference);
		try {
			IStatus updateStatus = P2Util.checkForUpdates(agent, monitor);
			UpdateCheckActivator.log(updateStatus);
			if (updateStatus.getCode() == UpdateOperation.STATUS_NOTHING_TO_UPDATE) {
				UpdateCheckActivator.info(updateStatus.getMessage());
				return;
			}
			if (updateStatus.getSeverity() != IStatus.ERROR) {
				UpdateCheckActivator.info(updateStatus.getMessage());
				
			}
		} finally {
			bundleContext.ungetService(reference);
		}
	}

	static IStatus checkForUpdates(IProvisioningAgent agent, IProgressMonitor monitor)
			throws OperationCanceledException {
		ProvisioningSession session = new ProvisioningSession(agent);
		IQuery<IInstallableUnit> query = QueryUtil.createLatestQuery(QueryUtil.createIUQuery("h1modulesfeature"));
		UpdateCheckActivator.info("Update Query Expression: " + query.getExpression());
		IProfileRegistry registry= (IProfileRegistry) agent.getService(IProfileRegistry.SERVICE_NAME);
		final IProfile profile= registry.getProfile(IProfileRegistry.SELF);
		IQueryResult<IInstallableUnit> result = profile.query(query, monitor);
		Set<IInstallableUnit> unitsForUpdate = result.toUnmodifiableSet();
		UpdateCheckActivator.info("Installable Units for update: " + unitsForUpdate);
		UpdateOperation operation = new UpdateOperation(session);
		if(!unitsForUpdate.isEmpty()) {
			UpdateCheckActivator.info("Creating UpdateOperation for: " + unitsForUpdate);
			operation = new UpdateOperation(session, unitsForUpdate);
		}
	    
		SubMonitor sub = SubMonitor.convert(monitor, "Checking for application updates...", 200);
		IStatus status = operation.resolveModal(sub.newChild(100));
		if (status.getCode() == UpdateOperation.STATUS_NOTHING_TO_UPDATE) {
			UpdateCheckActivator.info("Update Status: " + status.getMessage());
			return status;
		}
		if (status.getSeverity() == IStatus.CANCEL)
			throw new OperationCanceledException();

		if (status.getSeverity() != IStatus.ERROR) {
			// More complex status handling might include showing the user what
			// updates are available if there are multiples, differentiating
			// patches vs. updates, etc. In this example, we simply update as
			// suggested by the operation.
			
			status = restrictUpdateToEcl1(operation, monitor, sub);
			
			if (status.getSeverity() == IStatus.CANCEL)
				throw new OperationCanceledException();
		}
		return status;
	}

	private static IStatus restrictUpdateToEcl1(UpdateOperation operation, IProgressMonitor monitor, SubMonitor sub) {
		ProvisioningJob job = operation.getProvisioningJob(monitor);
		List<Update> chosenUpdates = Arrays.asList(operation.getPossibleUpdates());
		Update ecl1 = null;
		for (Update update : chosenUpdates) {
			UpdateCheckActivator.info("Possible Update from " + update.toUpdate.getId() + " " + update.toUpdate.getVersion() + " to " + update.replacement.getVersion());
			if(isEcl1(update)) {
				UpdateCheckActivator.info("Is ecl1-Update: " + update.toUpdate.getId());
				ecl1 = update;
			}
		}
		//uncomment
		//if(ecl1 != null) {
			//operation.setSelectedUpdates(new Update[]{ecl1});
			if (job == null) {
				return new Status(IStatus.ERROR, UpdateCheckActivator.PLUGIN_ID,
						"ProvisioningJob could not be created - does this application support p2 software installation?");
			}
			return job.runModal(sub.newChild(100));
		//}
		//return new Status(Status.INFO, UpdateCheckActivator.PLUGIN_ID, "No update for ecl1 found.");
	}

	private static boolean isEcl1(Update update) {
		String updateId = update.toUpdate.getId();
		if(updateId != null) {
			
			return true;
		}
		return false;
	}

}
