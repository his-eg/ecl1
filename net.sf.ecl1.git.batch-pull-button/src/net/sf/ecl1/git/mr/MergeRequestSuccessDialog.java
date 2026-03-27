package net.sf.ecl1.git.mr;

import java.net.URL;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

/**
 * Dialog shown after a merge request has been created successfully.
 * Displays a clickable link to the merge request.
 */
public class MergeRequestSuccessDialog extends TitleAreaDialog {

    private final String mergeRequestUrl;

    /**
     * @param parentShell the parent shell
     * @param mergeRequestUrl the URL of the created merge request (may be null)
     */
    public MergeRequestSuccessDialog(Shell parentShell, String mergeRequestUrl) {
        super(parentShell);
        this.mergeRequestUrl = mergeRequestUrl;
    }

    @Override
    public void create() {
        super.create();
        setTitle("Merge Request Created");
        setMessage("The merge request was created successfully.");
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite area = (Composite) super.createDialogArea(parent);
        Composite container = new Composite(area, SWT.NONE);
        container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        GridLayout layout = new GridLayout(1, false);
        layout.marginWidth = 15;
        layout.marginHeight = 15;
        container.setLayout(layout);

        if (mergeRequestUrl != null && !mergeRequestUrl.isEmpty()) {
            Label label = new Label(container, SWT.NONE);
            label.setText("Your merge request is ready for review:");
            label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

            Link link = new Link(container, SWT.NONE);
            link.setText("<a>" + mergeRequestUrl + "</a>");
            link.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
            link.addListener(SWT.Selection, event -> {
                openUrl(mergeRequestUrl);
            });
        } else {
            Label label = new Label(container, SWT.NONE);
            label.setText("Merge request created successfully.");
            label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        }

        return area;
    }

    @Override
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
        newShell.setText("Merge Request Created");
        newShell.setMinimumSize(500, 250);
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
    }

    /**
     * Opens the given URL in the system's default browser.
     */
    private void openUrl(String url) {
        try {
            PlatformUI.getWorkbench().getBrowserSupport()
                    .getExternalBrowser()
                    .openURL(new URL(url));
        } catch (Exception e) {
            // Fallback: try Desktop.browse
            try {
                java.awt.Desktop.getDesktop().browse(new java.net.URI(url));
            } catch (Exception ex) {
                // ignore
            }
        }
    }
}
