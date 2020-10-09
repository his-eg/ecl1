package net.sf.ecl1.commit.exporter;

import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;

public class SimplePropertyChangeListener implements IPropertyChangeListener {
	private CommitExporterWizardPage page;
	public SimplePropertyChangeListener(CommitExporterWizardPage page) {
		this.page = page;
	}
	@Override
	public void propertyChange(PropertyChangeEvent changeEvent) {
    	// clear previous messages
        page.setMessage(null);
        page.setErrorMessage(null);
		page.checkSetHotfixInformation();
	}
}