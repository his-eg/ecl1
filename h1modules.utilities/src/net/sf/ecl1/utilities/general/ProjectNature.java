package net.sf.ecl1.utilities.general;

import org.eclipse.jdt.core.JavaCore;

/**
 * Project Natures
 *
 * @author keunecke
 */
public enum ProjectNature {

	/** a java project */
	JAVA(JavaCore.NATURE_ID),

	/** a project with macker support */
    MACKER("de.his.core.tools.cs.sys.quality.eclipsemacker.mackerNature"),

    /** an extension project */
    ECL1("net.sf.ecl1.extensionpoint.hisinoneExtensionsNature");

	private final String nature;

	private ProjectNature(String nature) {
		this.nature = nature;
	}

	/**
	 * gets the nature string
	 *
	 * @return nature string
	 */
	public String getNature() {
		return nature;
	}

}
