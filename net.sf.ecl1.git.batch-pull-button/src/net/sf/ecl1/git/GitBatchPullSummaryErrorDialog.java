package net.sf.ecl1.git;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import net.sf.ecl1.utilities.preferences.PreferenceWrapper;

public class GitBatchPullSummaryErrorDialog extends ErrorDialog {

	public GitBatchPullSummaryErrorDialog(IStatus status) {
		/*
		 * Use standard shell, title and message by setting them to null
		 * Only display dialog if WARNING, INFO or ERROR occurred
		 */
		super(null, null, null, status, IStatus.INFO | IStatus.WARNING | IStatus.ERROR);
	}

	/**
	 * Constructor for standalone 
	 */
    public GitBatchPullSummaryErrorDialog(Shell activeShell, IStatus multiStatus) {
        super(activeShell, null, null, multiStatus, IStatus.INFO | IStatus.WARNING | IStatus.ERROR);
    }
	
	/**
	 * Adds a checkbox under the normal dialog area. 
	 * Checkbox controls if this dialog should be shown the next time when problems occurred. 
	 */
	@Override
	protected Control createDialogArea(Composite parent) {
		//Display all the information our super class displays
		Composite composite = (Composite)super.createDialogArea(parent);
		
		//Add our new checkbox
		Button checkbox = new Button(composite, SWT.CHECK);
		checkbox.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
        checkbox.setText("Don't show this dialog again (can be re-enabled in preferences).");
        checkbox.setSelection(!PreferenceWrapper.isDisplaySummaryOfGitPull());
        checkbox.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if(PreferenceWrapper.isDisplaySummaryOfGitPull()) {
                	PreferenceWrapper.setDisplaySummaryOfGitPull(false);
                } else {
                	PreferenceWrapper.setDisplaySummaryOfGitPull(true);
                }
            }
        });
	 	return composite;	
	}
	

}
