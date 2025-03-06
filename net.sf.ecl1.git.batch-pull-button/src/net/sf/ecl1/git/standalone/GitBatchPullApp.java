package net.sf.ecl1.git.standalone;

import net.sf.ecl1.git.GitBatchPullHandler;


public class GitBatchPullApp {

    public static void main(String[] args) {
        new GitBatchPullHandler().execute(null);
    }
}
