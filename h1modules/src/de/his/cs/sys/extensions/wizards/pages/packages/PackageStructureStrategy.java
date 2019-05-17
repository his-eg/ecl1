package de.his.cs.sys.extensions.wizards.pages.packages;

import java.util.Collection;

/**
 * @company HIS eG
 * @author keunecke
 */
public interface PackageStructureStrategy {
	
	/**
	 * @param project
	 * @return package names to create
	 */
	public Collection<String> packagesToCreate(String project);
}
