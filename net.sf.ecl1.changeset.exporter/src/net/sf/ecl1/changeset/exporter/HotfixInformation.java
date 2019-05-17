package net.sf.ecl1.changeset.exporter;

import java.util.ArrayList;

/**
 * Information about a hotfix
 * @author keunecke, tneumann
 */
public class HotfixInformation {

    private static final String FILE_ELEMENT = "<file name=\"%s\" />";

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
    
    private ArrayList<String> fileNames = new ArrayList<>();

    /**
     * Create a new HotfixInformation with all data given.
     * @param title
     * @param description
     * @param hiszilla
     * @param dbUpdate
     * @param fileNames
     */
    public HotfixInformation(String title, String description, String hiszilla, boolean dbUpdate, ArrayList<String> fileNames) {
        this.title = title;
        this.description = description;
        this.hiszilla = hiszilla;
        this.dbUpdate = dbUpdate ? "true" : "false";
        this.fileNames = fileNames;
    }
    
    /**
     * Plot HotfixInformation to XML
     * @return xml string
     */
    public String toXml() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format(PATCH_START + NEW_LINE, title, hiszilla, dbUpdate));
        for (String fileName : fileNames) {
            sb.append(String.format(INDENT + FILE_ELEMENT + NEW_LINE, fileName));
        }
        sb.append(INDENT + DESC_START + NEW_LINE);
        sb.append(description + NEW_LINE);
        sb.append(INDENT + DESC_END + NEW_LINE);
        sb.append(PATCH_END);
        return sb.toString();
    }
}
