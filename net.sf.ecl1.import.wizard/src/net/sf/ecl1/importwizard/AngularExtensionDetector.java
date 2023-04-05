package net.sf.ecl1.importwizard;

import net.sf.ecl1.utilities.general.RemoteProjectSearchSupport;

public class AngularExtensionDetector {
	
	public static boolean isAngularExtension(String extension) {
		RemoteProjectSearchSupport remoteProjectSearchSupport = new RemoteProjectSearchSupport();
		String angularJson = remoteProjectSearchSupport.getRemoteFileContent(extension, "angular.json", false);
		if (angularJson != null) {
			return true;
		}
		return false;
	}
	
}
