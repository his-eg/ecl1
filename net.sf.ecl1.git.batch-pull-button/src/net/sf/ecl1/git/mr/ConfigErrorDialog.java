package net.sf.ecl1.git.mr;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * Dialog shown when the Gitlab configuration file is missing or broken.
 * Displays the error message and a copyable example configuration.
 */
public class ConfigErrorDialog extends TitleAreaDialog {

    private static final int COPY_BUTTON_ID = 1000;

    private final String errorMessage;

    /**
     * @param parentShell the parent shell
     * @param errorMessage the error message to display
     */
    public ConfigErrorDialog(Shell parentShell, String errorMessage) {
        super(parentShell);
        this.errorMessage = errorMessage;
    }

    @Override
    public void create() {
        super.create();
        setTitle("Configuration Error");
        setMessage(errorMessage, IMessageProvider.ERROR);
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite area = (Composite) super.createDialogArea(parent);
        Composite container = new Composite(area, SWT.NONE);
        container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        container.setLayout(new GridLayout(1, false));

        Label label = new Label(container, SWT.NONE);
        label.setText("Example configuration:");

        Text exampleText = new Text(container,
                SWT.BORDER | SWT.MULTI | SWT.READ_ONLY | SWT.V_SCROLL | SWT.H_SCROLL);
        exampleText.setText(GitlabConfig.getConfigExampleJson());
        GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
        gd.heightHint = 200;
        gd.widthHint = 450;
        exampleText.setLayoutData(gd);

        return area;
    }

    @Override
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
        newShell.setText("Create Merge Request");
        newShell.setMinimumSize(550, 400);
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        createButton(parent, COPY_BUTTON_ID, "Copy to Clipboard", false);
        createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
    }

    @Override
    protected void buttonPressed(int buttonId) {
        if (buttonId == COPY_BUTTON_ID) {
            Clipboard clipboard = new Clipboard(getShell().getDisplay());
            clipboard.setContents(
                    new Object[] { GitlabConfig.getConfigExampleJson() },
                    new Transfer[] { TextTransfer.getInstance() });
            clipboard.dispose();
        } else {
            super.buttonPressed(buttonId);
        }
    }
}
