package h1modules.wizards.utils;

public enum ProjectNature {
	
	JAVA("org.eclipse.jdt.core.javanature"),
	
	MACKER("de.his.core.tools.cs.sys.quality.eclipsemacker.mackerNature");
	
	private final String nature;

	public String getNature() {
		return nature;
	}

	private ProjectNature(String nature) {
		this.nature = nature;
	}

}
