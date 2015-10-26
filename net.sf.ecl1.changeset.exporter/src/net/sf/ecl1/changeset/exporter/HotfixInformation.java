package net.sf.ecl1.changeset.exporter;

import java.util.Collection;

import com.google.common.collect.Sets;

/**
 * Information about a hotfix
 * @author keunecke
 *
 */
public class HotfixInformation {

    private static final String NEW_LINE = "\n";

    private String hiszilla;

    private String describtion;

    private String title;

    private Collection<String> fileNames = Sets.newHashSet();

    private static final String FILE_ELEMENT = "<file name=\"%s\" />";

    private static final String DESC_START = "<desc>";

    private static final String DESC_END = "</desc>";

    private static final String PATCH_START = "<patch name=\"%s\" hiszilla=\"%s\">";

    private static final String PATCH_END = "</patch>";

    private static final String INDENT = "    ";

    /**
     * Create a new HotfixInformation with given title, description and hiszilla data
     * @param title
     * @param describtion
     * @param hiszilla
     */
    public HotfixInformation(String title, String describtion, String hiszilla) {
        this.title = title;
        this.describtion = describtion;
        this.hiszilla = hiszilla;
    }

    public void addFile(String file) {
        fileNames.add(file);
    }

    /**
     * Plot HotfixInformation to XML
     * @return xml string
     */
    public String toXml() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format(PATCH_START + NEW_LINE, title, hiszilla));
        for (String fileName : fileNames) {
            sb.append(String.format(INDENT + FILE_ELEMENT + NEW_LINE, fileName));
        }
        sb.append(INDENT + DESC_START + NEW_LINE);
        sb.append(describtion + NEW_LINE);
        sb.append(INDENT + DESC_END + NEW_LINE);
        sb.append(PATCH_END);
        return sb.toString();
    }

}
