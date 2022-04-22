/*******************************************************************************
 * Copyright (c) 2018, 2020 Thomas Wolf <thomas.wolf@paranor.ch> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Alexander Nittka <alex@nittka.de> - Bug 545123
 *******************************************************************************/
package org.eclipse.egit.ui.internal.repository;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IExecutableExtensionFactory;

/**
 * Extension factory to create the content provider for the repositories view.
 */
public class RepositoriesViewExtensionFactory
		implements IExecutableExtensionFactory {

	@Override
	public Object create() throws CoreException {
		return new RepositoriesViewContentProvider(true)
				.showingRepositoryGroups(true)
				.withFilterCache(FilterCache.INSTANCE);
	}

}
