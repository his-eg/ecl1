package net.sf.ecl1.commit.exporter;

import java.util.EventObject;

import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.ICheckStateListener;

public class SimplePropertyChangeListener implements IPropertyChangeListener, ICheckStateListener {
	private CommitExporterWizardPage page;
	public SimplePropertyChangeListener(CommitExporterWizardPage page) {
		this.page = page;
	}
	@Override
	public void propertyChange(PropertyChangeEvent changeEvent) {
        handleEvent(changeEvent);
	}

    @Override
    public void checkStateChanged(CheckStateChangedEvent event) {
        handleEvent(event);

    }

    private void handleEvent(EventObject event) {
        //clear previous messages
        page.createHotfix();
    }
}