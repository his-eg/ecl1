package net.sf.ecl1.git.updatehooks.standalone;

import net.sf.ecl1.git.updatehooks.UpdateHooks;
import net.sf.ecl1.utilities.standalone.AppUtil;

public class UpdateHooksApp {

    public static void main(String[] args) {
        AppUtil.setCustomWorkspacePathIfExists(args);
        new UpdateHooks().earlyStartup();
    }
}
