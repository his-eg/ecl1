package net.sf.ecl1.git.mr;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * Dialog to collect merge request parameters from the user.
 * Fields: merge request title, target branch, assignee (with autocomplete), and force sync option.
 */
public class MergeRequestDialog extends TitleAreaDialog {

    private Text messageText;
    private Text targetBranchText;
    private Text assignToText;
    private Button forceSyncButton;

    private String message;
    private String targetBranch;
    private String assignTo;
    private boolean forceSync;

    private final String currentBranch;
    private final String detectedTargetBranch;
    private final boolean hasLfs;
    private final GitlabApi gitlabApi;

    /** Autocomplete drop-down for the assignee field */
    private GitlabUserDropDown userDropDown;
    /** Bold font for the suggestion popup (owned by this dialog) */
    private Font boldFont;
    /** Color for the @username line in the suggestion popup (owned by this dialog) */
    private Color usernameColor;

    /**
     * Creates a new MergeRequestDialog.
     *
     * @param parentShell the parent shell
     * @param currentBranch the current git branch name
     * @param detectedTargetBranch the auto-detected target branch (may be null)
     * @param message the pre-filled merge request title (may be null)
     * @param hasLfs whether the repository has LFS objects
     * @param gitlabApi the Gitlab API instance for user search
     */
    public MergeRequestDialog(Shell parentShell, String currentBranch, String detectedTargetBranch, String message, boolean hasLfs, GitlabApi gitlabApi) {
        super(parentShell);
        this.currentBranch = currentBranch;
        this.detectedTargetBranch = detectedTargetBranch;
        this.message = message;
        this.hasLfs = hasLfs;
        this.gitlabApi = gitlabApi;
    }

    @Override
    public void create() {
        super.create();
        setTitle("Create Merge Request");
        setMessage("Push current branch '" + (currentBranch != null ? currentBranch : "<detached>")
                + "' and create a Gitlab merge request.");
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite area = (Composite) super.createDialogArea(parent);
        Composite container = new Composite(area, SWT.NONE);
        container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        GridLayout layout = new GridLayout(2, false);
        layout.marginWidth = 10;
        layout.marginHeight = 10;
        container.setLayout(layout);

        // Create bold font and username color
        Font defaultFont = parent.getFont();
        FontData[] fontData = defaultFont.getFontData();
        for (FontData fd : fontData) {
            fd.setStyle(fd.getStyle() | SWT.BOLD);
        }
        boldFont = new Font(parent.getDisplay(), fontData);
        usernameColor = new Color(parent.getDisplay(), 100, 100, 100);

        // Message / Title
        Label messageLabel = new Label(container, SWT.NONE);
        messageLabel.setText("Merge Request Title:");
        messageLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));

        messageText = new Text(container, SWT.BORDER);
        if (message != null) {
            messageText.setText(message);
        }
        messageText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        messageText.setToolTipText("Title of the merge request and commit message for the merge commit.\n"
                + "If empty, Gitlab will use the branch name or single commit message.");

        // Target Branch
        Label targetBranchLabel = new Label(container, SWT.NONE);
        targetBranchLabel.setText("Target Branch:");
        targetBranchLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));

        targetBranchText = new Text(container, SWT.BORDER);
        targetBranchText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        if (detectedTargetBranch != null) {
            targetBranchText.setText(detectedTargetBranch);
        }
        targetBranchText.setToolTipText("The branch to merge into.");

        // Assign To (with autocomplete)
        Label assignToLabel = new Label(container, SWT.NONE);
        assignToLabel.setText("Assign To:");
        assignToLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));

        assignToText = new Text(container, SWT.BORDER);
        assignToText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        assignToText.setToolTipText("Gitlab username to assign the merge request to. Leave empty to not assign.");

        ControlDecoration assignToDeco = new ControlDecoration(assignToText, SWT.TOP | SWT.LEFT);
        assignToDeco.setImage(FieldDecorationRegistry.getDefault()
                .getFieldDecoration(FieldDecorationRegistry.DEC_CONTENT_PROPOSAL).getImage());
        assignToDeco.setDescriptionText("Start typing to search for Gitlab users");
        assignToDeco.setShowOnlyOnFocus(false);

        userDropDown = new GitlabUserDropDown(assignToText, gitlabApi, boldFont, usernameColor);

        // Force Sync checkbox
        Label spacer = new Label(container, SWT.NONE);
        spacer.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));

        forceSyncButton = new Button(container, SWT.CHECK);
        forceSyncButton.setText("Force sync fork" + (hasLfs ? " (LFS detected, sync enabled by default)" : ""));
        forceSyncButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        forceSyncButton.setSelection(hasLfs);
        forceSyncButton.setToolTipText("Forces a sync of the fork on the server.");

        return area;
    }

    @Override
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
        newShell.setText("Create Merge Request");
        newShell.setMinimumSize(500, 350);
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        createButton(parent, IDialogConstants.OK_ID, "Create Merge Request", true);
        createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
    }

    @Override
    protected void okPressed() {
        message = messageText.getText().trim();
        targetBranch = targetBranchText.getText().trim();
        assignTo = assignToText.getText().trim();
        forceSync = forceSyncButton.getSelection();
        if (userDropDown != null) {
            userDropDown.hidePopup();
        }
        super.okPressed();
    }

    @Override
    public boolean close() {
        if (userDropDown != null) {
            userDropDown.dispose();
        }
        if (boldFont != null && !boldFont.isDisposed()) {
            boldFont.dispose();
        }
        if (usernameColor != null && !usernameColor.isDisposed()) {
            usernameColor.dispose();
        }
        return super.close();
    }

    /**
     * Returns the collected parameters as a {@link MergeRequestCreator.Params} object.
     */
    public MergeRequestCreator.Params getParams() {
        MergeRequestCreator.Params params = new MergeRequestCreator.Params();
        params.message = message;
        params.targetBranch = targetBranch;
        params.assignTo = assignTo;
        params.forceSync = forceSync;
        return params;
    }
}
