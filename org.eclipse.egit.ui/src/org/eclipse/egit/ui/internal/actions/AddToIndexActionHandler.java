/*******************************************************************************
 * Copyright (C) 2010, Matthias Sohn <matthias.sohn@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.actions;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.core.op.AddToIndexOperation;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.JobFamilies;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.operations.GitScopeUtil;
import org.eclipse.ui.IWorkbenchPart;

/**
 * Action for adding a resource to the git index
 *
 * @see AddToIndexOperation
 *
 */
public class AddToIndexActionHandler extends RepositoryActionHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		final IResource[] sel = getSelectedResources(event);
		if (sel.length == 0)
			return null;

		IResource[] resourcesInScope;
		try {
			IWorkbenchPart part = getPart(event);
			resourcesInScope = GitScopeUtil.getRelatedChanges(part, sel);
		} catch (InterruptedException e) {
			// ignore, we will not add the files in case the user
			// cancels the scope operation
			return null;
		}

		final AddToIndexOperation operation = new AddToIndexOperation(
				resourcesInScope);
		String jobname = UIText.AddToIndexAction_addingFiles;
		Job job = new Job(jobname) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					operation.execute(monitor);
				} catch (CoreException e) {
					return Activator.createErrorStatus(e.getStatus()
							.getMessage(), e);
				}
				return Status.OK_STATUS;
			}

			@Override
			public boolean belongsTo(Object family) {
				if (JobFamilies.ADD_TO_INDEX.equals(family))
					return true;

				return super.belongsTo(family);
			}
		};
		job.setUser(true);
		job.setRule(operation.getSchedulingRule());
		job.schedule();
		return null;
	}

	@Override
	public boolean isEnabled() {
		return haveSelectedResourcesWithRepository();
	}

}
