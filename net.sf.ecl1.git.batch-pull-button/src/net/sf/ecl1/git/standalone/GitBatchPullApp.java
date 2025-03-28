package net.sf.ecl1.git.standalone;

import net.sf.ecl1.git.GitBatchPullHandler;
import net.sf.ecl1.utilities.standalone.AppUtil;


public class GitBatchPullApp {

    public static void main(String[] args) {
        AppUtil.setCustomWorkspacePathIfExists(args);
        new GitBatchPullHandler().execute(null);
    }
}
