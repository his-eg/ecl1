package net.sf.ecl1.extensionpoint.collector.model;

import com.google.common.base.Objects;


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

    @Override
    public int hashCode() {
        return Objects.hashCode(id, name, iface);
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof ExtensionPointInformation) {
            ExtensionPointInformation that = (ExtensionPointInformation) object;
            return Objects.equal(this.id, that.id) && Objects.equal(this.name, that.name) && Objects.equal(this.iface, that.iface);
        }
        return false;
    }

}
