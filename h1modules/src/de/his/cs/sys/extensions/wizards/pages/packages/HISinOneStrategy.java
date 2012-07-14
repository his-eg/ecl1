/*
 * Copyright (c) 2012 HIS GmbH All Rights Reserved.
 *
 * $Id$
 *
 * $Log$
 *
 * Created on 14.07.2012 by keunecke
 */
package de.his.cs.sys.extensions.wizards.pages.packages;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author keunecke
 * @version $Revision$ 
 */
public class HISinOneStrategy implements PackageStructureStrategy {
	
	private static final String[] prefixes = {"de.his.appserver.model.", 
												"de.his.appserver.service.iface.", 
												"de.his.appserver.service.impl.",
												"de.his.appserver.persistence.iface.",
												"de.his.appserver.persistence.impl.hibernate.",
												"de.his.appclient.jsf"};

	@Override
	public Collection<String> packagesToCreate(String project) {
		Collection<String> result = new ArrayList<String>();
		for (String prefix : prefixes) {
			result.add(prefix + project);
		}
		return result;
	}

}
