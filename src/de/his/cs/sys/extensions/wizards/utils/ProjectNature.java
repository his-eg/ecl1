package de.his.cs.sys.extensions.wizards.utils;

import org.eclipse.jdt.core.JavaCore;

public enum ProjectNature {
	
	JAVA(JavaCore.NATURE_ID),
	
	MACKER("de.his.core.tools.cs.sys.quality.eclipsemacker.mackerNature");
	
	private final String nature;

	public String getNature() {
		return nature;
	}

	private ProjectNature(String nature) {
		this.nature = nature;
	}

}
