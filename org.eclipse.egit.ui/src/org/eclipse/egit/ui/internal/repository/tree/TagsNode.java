/*******************************************************************************
 * Copyright (c) 2010 SAP AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.repository.tree;

import org.eclipse.jgit.lib.Repository;

/**
 * Represents the "Tags" node
 */
public class TagsNode extends RepositoryTreeNode<Repository>
		implements FilterableNode {

	private String filter;

	/**
	 * Constructs the node.
	 *
	 * @param parent
	 *            the parent node (may be null)
	 * @param repository
	 *            the {@link Repository}
	 */
	public TagsNode(RepositoryTreeNode parent, Repository repository) {
		super(parent, RepositoryTreeNodeType.TAGS, repository, repository);
	}

	@Override
	public String getFilter() {
		return filter;
	}

	@Override
	public void setFilter(String filter) {
		this.filter = filter;
	}

	@Override
	public boolean equals(Object obj) {
		// "filter" doesn't participate
		return super.equals(obj);
	}

	@Override
	public int hashCode() {
		// "filter" doesn't participate
		return super.hashCode();
	}
}
