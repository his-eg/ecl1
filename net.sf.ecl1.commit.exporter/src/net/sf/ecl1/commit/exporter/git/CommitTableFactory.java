package net.sf.ecl1.commit.exporter.git;

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
     * <p>Creates a TableViewer on the given shell with a nice-looking layout for displaying git commits. </p>
     * 
     * <p>The returned table can handle {@link org.eclipse.jgit.revwalk.RevCommit RevCommits}
     * and {@link net.sf.ecl1.commit.exporter.git.StagedChanges StagedChanges} as inputs. </p>
     * 
     * <p>A note about the implementation: The runtime-performance of the returned CheckboxTableViewer can be optimized. 
     * How? 
     * When the input of the table is later set the StagedChanges are always set in the first row and the commits are set after the StagedChanges. 
     * In the {@link net.sf.ecl1.commit.exporter.git.CommitTableFactory#createColumns createColumns} this information is not used. Instead, 
     * for every cell an instanceof-check is performed. This could be avoided by simply assuming that the first row is StagedChanges and all 
     * subsequent rows are commits. Therefore, we could avoid a lot of instanceof-checks in {@link net.sf.ecl1.commit.exporter.git.CommitTableFactory#createColumns createColumns}.
     *  A possible solution would be to direclty set the input as {@link org.eclipse.swt.widgets.TableItem TableItems}
     *  instead of creating columns. See the source for the Change Set Exporter in ecl1 how  a table must be constructed by utilizing TableItems. </p>
     *
     * <p>Why was the runtime-performance not optimized?
     * The runtime-performance is already so fast, that any changes could be filed under "premature optimization". 
     * So in conclusion: The author of this factory is aware that the performance can be optimized, but has chosen not to optimize, 
     * because it is already fast enough. </p>
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
        tableViewer.getTable().setHeaderVisible(true);
        tableViewer.getTable().setLinesVisible(true);


        /* ----------------------
         * Create the columns for the table
         * ----------------------
         */

        createColumns(tableViewer, tableColumnLayout);

        return tableViewer;

    }

    /**
     * Creates the definitions of the columns. 
     * 
     * @param tableViewer
     * @param tableColumnLayout
     */
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
