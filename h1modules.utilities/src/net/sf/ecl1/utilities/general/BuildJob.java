package net.sf.ecl1.utilities.general;

import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;

import net.sf.ecl1.utilities.hisinone.EclipseUtil;

public class BuildJob extends Job {

	public BuildJob() {
		super("Building Workspace");
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		
		try {
			ResourcesPlugin.getWorkspace().build(IncrementalProjectBuilder.INCREMENTAL_BUILD, monitor);
		} catch (CoreException e) {
			return e.getStatus();
		}
		
		return Status.OK_STATUS;
	}

}
