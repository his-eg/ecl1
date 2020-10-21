package net.sf.ecl1.commit.exporter.commitTable;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CheckboxCellEditor;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;

public class SelectedEditingSupport extends EditingSupport {

    private final TableViewer viewer;

    public SelectedEditingSupport(TableViewer viewer) {
        super(viewer);
        this.viewer = viewer;
    }

    @Override
    protected CellEditor getCellEditor(Object element) {
        return new CheckboxCellEditor(null, SWT.CHECK | SWT.READ_ONLY);
    }

    @Override
    protected boolean canEdit(Object element) {
        return true;
    }

    @Override
    protected Object getValue(Object element) {
        SelectableRevCommit c = (SelectableRevCommit) element;
        return c.isSelected();
    }

    @Override
    protected void setValue(Object element, Object value) {
        SelectableRevCommit c = (SelectableRevCommit) element;
        c.setSelected((Boolean) value);
        viewer.update(element, null);

    }

}
