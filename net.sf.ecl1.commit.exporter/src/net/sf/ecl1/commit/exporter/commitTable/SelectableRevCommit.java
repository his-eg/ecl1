package net.sf.ecl1.commit.exporter.commitTable;

import org.eclipse.jgit.revwalk.RevCommit;

public class SelectableRevCommit {

    private boolean selected;

    private RevCommit commit;

    public SelectableRevCommit(RevCommit c) {
        this.commit = c;
        this.selected = false;
    }


    public boolean isSelected() {
        return selected;
    }


    public void setSelected(boolean selected) {
        this.selected = selected;
    }


    public RevCommit getCommit() {
        return commit;
    }

}
