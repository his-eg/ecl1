package net.sf.ecl1.git.auto.lfs.prune.standalone;

import net.sf.ecl1.git.auto.lfs.prune.AutoLfsPruneStarter;
import net.sf.ecl1.utilities.standalone.AppUtil;

public class AutoLfsPruneStarterApp {

	public static void main(String[] args) {
		AppUtil.setCustomWorkspacePathIfExists(args);
		new AutoLfsPruneStarter().earlyStartup();
	}

}
