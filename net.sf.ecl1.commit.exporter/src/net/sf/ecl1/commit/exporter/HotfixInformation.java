package net.sf.ecl1.commit.exporter;

import java.util.List;

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
    
    private List<String> fileNames;

    /**
     * Create a new HotfixInformation with all data given.
     * @param title
     * @param description
     * @param hiszilla
     * @param dbUpdate
     * @param fileNames
     */
    public HotfixInformation(String title, String description, String hiszilla, boolean dbUpdate, List<String> fileNames) {
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
        sb.append(String.format(INDENT + PATCH_START + NEW_LINE, title, hiszilla, dbUpdate));
        for (String fileName : fileNames) {
            sb.append(String.format(INDENT + INDENT + FILE_ELEMENT + NEW_LINE, fileName));
        }
        sb.append(INDENT + INDENT + DESC_START + NEW_LINE);
        sb.append(INDENT + INDENT + INDENT + description + NEW_LINE);
        sb.append(INDENT + INDENT + DESC_END + NEW_LINE);
        sb.append(INDENT + PATCH_END);
        return sb.toString();
    }
}
