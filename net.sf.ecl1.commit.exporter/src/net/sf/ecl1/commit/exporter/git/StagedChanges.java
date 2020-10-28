package net.sf.ecl1.commit.exporter.git;

public class StagedChanges {

    public StagedChanges() {
    }

    public String getID() {
        return "---";
    }

    public String getShortMessage() {
        return "Staged Changes";
    }

    public String getEmailAddress() {
        return "---";
    }

    public String getWhen() {
        return "---";
    }
}
