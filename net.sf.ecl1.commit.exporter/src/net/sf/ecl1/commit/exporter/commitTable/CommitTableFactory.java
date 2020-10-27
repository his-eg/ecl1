package net.sf.ecl1.commit.exporter.commitTable;

import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TableColumn;

public class CommitTableFactory {

    /**
     * 
     * Creates a TableViewer on the given shell with a nice-looking layout for displaying git commits. 
     * 
     * 
     * @param parentComposite
     * @return
     */
    static public CheckboxTableViewer createCommitTable(Composite parentComposite) {
        /* ----------------------
         * Create the table that displays everything
         * ----------------------
         */


        //Necessary to assign weights to the columns at a later stage
        Composite tableComposite = new Composite(parentComposite, SWT.NONE);
        TableColumnLayout tableColumnLayout = new TableColumnLayout();
        tableComposite.setLayout(tableColumnLayout);
        GridData layoutData = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1);
        tableComposite.setLayoutData(layoutData);

        CheckboxTableViewer tableViewer = CheckboxTableViewer.newCheckList(tableComposite,
                                                                           SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.BORDER | SWT.CHECK | SWT.BORDER);
        //TableViewer tableViewer = new TableViewer(shell);
        //TableViewer tableViewer = new TableViewer(shell, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.BORDER);
        tableViewer.getTable().setHeaderVisible(true);
        tableViewer.getTable().setLinesVisible(true);


        /* ----------------------
         * Create the columns for the table
         * ----------------------
         */

        createColumns(tableViewer, tableColumnLayout);

        return tableViewer;

    }

    private static void createColumns(TableViewer tableViewer, TableColumnLayout tableColumnLayout) {

        String[] headers = { "ID", "Message", "Author", "Date" };
        int[] columnWeights = { 1, 4, 2, 2 };


        //First column
        TableViewerColumn col = createTableViewerColumn(tableViewer, headers[0], tableColumnLayout, columnWeights[0]);
        col.setLabelProvider(new ColumnLabelProvider() {

            @Override
            public String getText(Object element) {

                if (element instanceof RevCommit) {
                    RevCommit c = (RevCommit) element;
                    return c.getId().name();
                }
                //Must be StagedChanges then...
                StagedChanges c = (StagedChanges) element;
                return c.getID();
            }
        });

        //Second column
        col = createTableViewerColumn(tableViewer, headers[1], tableColumnLayout, columnWeights[1]);
        col.setLabelProvider(new ColumnLabelProvider() {

            @Override
            public String getText(Object element) {
                if (element instanceof RevCommit) {
                    RevCommit c = (RevCommit) element;
                    return c.getShortMessage();
                }
                //Must be StagedChanges then...
                StagedChanges c = (StagedChanges) element;
                return c.getShortMessage();
            }
        });

        //Third column
        col = createTableViewerColumn(tableViewer, headers[2], tableColumnLayout, columnWeights[2]);
        col.setLabelProvider(new ColumnLabelProvider() {

            @Override
            public String getText(Object element) {
                if (element instanceof RevCommit) {
                    RevCommit c = (RevCommit) element;
                    return c.getAuthorIdent().getEmailAddress();
                }
                //Must be StagedChanges then...
                StagedChanges c = (StagedChanges) element;
                return c.getEmailAddress();
            }
        });

        //Fourth column
        col = createTableViewerColumn(tableViewer, headers[3], tableColumnLayout, columnWeights[3]);
        col.setLabelProvider(new ColumnLabelProvider() {

            @Override
            public String getText(Object element) {
                if (element instanceof RevCommit) {
                    RevCommit c = (RevCommit) element;
                    return c.getAuthorIdent().getWhen().toString();
                }
                //Must be StagedChanges then...
                StagedChanges c = (StagedChanges) element;
                return c.getWhen();
            }
        });



    }

    private static TableViewerColumn createTableViewerColumn(TableViewer viewer, String title, TableColumnLayout tableColumnLayout, int columnWeight) {
        final TableViewerColumn viewerColumn = new TableViewerColumn(viewer, SWT.NONE);
        final TableColumn column = viewerColumn.getColumn();
        //Set weight of this column
        tableColumnLayout.setColumnData(column, new ColumnWeightData(columnWeight, 10, true));
        column.setText(title);
        column.setResizable(true);
        column.setMoveable(true);
        return viewerColumn;

    }

}
