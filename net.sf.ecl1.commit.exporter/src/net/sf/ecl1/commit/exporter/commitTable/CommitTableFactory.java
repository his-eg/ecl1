package net.sf.ecl1.commit.exporter.commitTable;

import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TableColumn;

public class CommitTableFactory {

    /**
     * 
     * Creates a TableViewer on the given shell with a suitable layout for displaying git commits. 
     * 
     * 
     * @param parentComposite
     * @return
     */
    static public TableViewer createCommitTable(Composite parentComposite) {
        /* ----------------------
         * Create the table that displays everything
         * ----------------------
         */


        //Necessary to assign weights to the columns at a later stage
        Composite tableComposite = new Composite(parentComposite, SWT.NONE);
        TableColumnLayout tableColumnLayout = new TableColumnLayout();
        tableComposite.setLayout(tableColumnLayout);

        TableViewer tableViewer = new TableViewer(tableComposite, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.BORDER);
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

        String[] headers = { "Checked?", "Short ID", "Message", "Author", "Date" };
        int[] columnWeights = { 1, 1, 4, 2, 2 };


        //First column
        TableViewerColumn col = createTableViewerColumn(tableViewer, headers[0], tableColumnLayout, columnWeights[0]);
        col.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                SelectableRevCommit c = (SelectableRevCommit) element;
                if (c.isSelected()) {
                    return Character.toString((char) 0x2611);
                } else {
                    return Character.toString((char) 0x2610);
                }
            }
        });
        col.setEditingSupport(new SelectedEditingSupport(tableViewer));

        //Second column
        col = createTableViewerColumn(tableViewer, headers[1], tableColumnLayout, columnWeights[1]);
        col.setLabelProvider(new ColumnLabelProvider() {

            @Override
            public String getText(Object element) {
                RevCommit c = ((SelectableRevCommit) element).getCommit();
                return c.getId().abbreviate(7).name();
            }
        });

        //Third column
        col = createTableViewerColumn(tableViewer, headers[2], tableColumnLayout, columnWeights[2]);
        col.setLabelProvider(new ColumnLabelProvider() {

            @Override
            public String getText(Object element) {
                RevCommit c = ((SelectableRevCommit) element).getCommit();
                return c.getShortMessage();
            }
        });

        //Fourth column
        col = createTableViewerColumn(tableViewer, headers[3], tableColumnLayout, columnWeights[3]);
        col.setLabelProvider(new ColumnLabelProvider() {

            @Override
            public String getText(Object element) {
                RevCommit c = ((SelectableRevCommit) element).getCommit();
                return c.getAuthorIdent().getEmailAddress();
            }
        });

        //Fifth column
        col = createTableViewerColumn(tableViewer, headers[4], tableColumnLayout, columnWeights[4]);
        col.setLabelProvider(new ColumnLabelProvider() {

            @Override
            public String getText(Object element) {
                RevCommit c = ((SelectableRevCommit) element).getCommit();
                //Implement me!
                return c.getAuthorIdent().getWhen().toString();
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
