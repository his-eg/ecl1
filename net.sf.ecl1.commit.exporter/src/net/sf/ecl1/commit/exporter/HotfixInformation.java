package net.sf.ecl1.commit.exporter;

import java.util.List;
import java.util.Set;

/**
 * Information about a hotfix
 * @author keunecke, tneumann
 */
public class HotfixInformation {

    private static final String FILE_ELEMENT = "<file name=\"%s\" />";
    
    private static final String DEL_ELEMENT = "<removed name=\"%s\" />";

    private static final String DESC_START = "<desc>";

    private static final String DESC_END = "</desc>";

    private static final String PATCH_START = "<patch name=\"%s\" hiszilla=\"%s\" dbUpdate=\"%s\">";

    private static final String PATCH_END = "</patch>";

    private static final String INDENT = "    ";

    private static final String NEW_LINE = "\n";

    private String hiszilla;

    private String description;

    private String title;

    private String dbUpdate;
    
    private Set<String> fileNames;
    
    private Set<String> deletedFiles;

    /**
     * Create a new HotfixInformation with all data given.
     * @param title
     * @param description
     * @param hiszilla
     * @param dbUpdate
     * @param fileNames
     */
    public HotfixInformation(String title, String description, String hiszilla, boolean dbUpdate, Set<String> fileNames, Set<String> deletedFiles) {
        this.title = title;
        this.description = description;
        this.hiszilla = hiszilla;
        this.dbUpdate = dbUpdate ? "true" : "false";
        this.fileNames = fileNames;
        this.deletedFiles = deletedFiles;
    }
    
    /**
     * Plot HotfixInformation to XML
     * @return xml string
     */
    public String toXml() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format(INDENT + PATCH_START + NEW_LINE, title, hiszilla, dbUpdate));
        //List added or modified files
        for (String fileName : fileNames) {
            sb.append(String.format(INDENT + INDENT + FILE_ELEMENT + NEW_LINE, fileName));
        }
        //List removed files
        for (String deleted : deletedFiles) {
            sb.append(String.format(INDENT + INDENT + DEL_ELEMENT + NEW_LINE, deleted));
        }
        sb.append(INDENT + INDENT + DESC_START + NEW_LINE);
        sb.append(INDENT + INDENT + INDENT + description + NEW_LINE);
        sb.append(INDENT + INDENT + DESC_END + NEW_LINE);
        sb.append(INDENT + PATCH_END);
        return sb.toString();
    }
}
