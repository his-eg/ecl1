package net.sf.ecl1.utilities.standalone.vscode;

/**
 * Class to toggle ecl1 visibility in vscode workspace.
 */
public class ToggleEcl1VisibilityInWorkspace {

    private static final String ECL1_FOLDER = "**/ecl1";
    
    public static void main(String[] args) {
        SettingsHelper helper = new SettingsHelper();
        helper.toggleExclusion("files.exclude", ECL1_FOLDER);
        helper.toggleExclusion("search.exclude", ECL1_FOLDER);
        helper.toggleExclusion("files.watcherExclude", ECL1_FOLDER);
        helper.save();
    }

}