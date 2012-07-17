package net.sf.ecl1.extensionpoint.collector.model;
/**
 * Container for extension point information
 * 
 * @author keunecke
 */
public class ExtensionPointInformation {
	
	private final String id;
	
	private final String name;
	
	private final String iface;

	public ExtensionPointInformation(String id, String name, String iface) {
		this.id = id;
		this.name = name;
		this.iface = iface;
	}
	
	@Override
	public String toString() {
		return "ExtensionPointInformation [id=" + id + ", name=" + name
				+ ", iface=" + iface + "]";
	}

}
