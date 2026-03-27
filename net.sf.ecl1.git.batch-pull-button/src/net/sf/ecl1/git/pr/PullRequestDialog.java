package net.sf.ecl1.git.pr;

import java.util.List;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * Dialog to collect merge request parameters from the user.
 * Fields: merge request title, target branch, assignee (with autocomplete), and force sync option.
 */
public class PullRequestDialog extends TitleAreaDialog {

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

    /** Popup shell for autocomplete suggestions */
    private Shell popupShell;
    /** List widget inside the popup */
    private org.eclipse.swt.widgets.List suggestionList;
    /** Tracks the scheduled debounce runnable to cancel previous ones */
    private Runnable pendingQuery;
    /** Delay in ms before querying the API after a keystroke */
    private static final int DEBOUNCE_DELAY_MS = 300;
    /** Minimum characters before triggering a search */
    private static final int MIN_QUERY_LENGTH = 2;
    /** Flag to suppress modify events when we programmatically set text */
    private boolean suppressModify = false;

    /**
     * Creates a new PullRequestDialog.
     *
     * @param parentShell the parent shell
     * @param currentBranch the current git branch name
     * @param detectedTargetBranch the auto-detected target branch (may be null)
     * @param message the pre-filled merge request title (may be null)
     * @param hasLfs whether the repository has LFS objects
     * @param gitlabApi the Gitlab API instance for user search
     */
    public PullRequestDialog(Shell parentShell, String currentBranch, String detectedTargetBranch, String message, boolean hasLfs, GitlabApi gitlabApi) {
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

        setupAutocomplete(assignToText);

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

    /**
     * Sets up autocomplete behaviour on the given text widget.
     * A popup with suggestions appears after the user types at least
     * {@link #MIN_QUERY_LENGTH} characters, queried with a debounce delay.
     */
    private void setupAutocomplete(Text textWidget) {
        Display display = textWidget.getDisplay();

        textWidget.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                if (suppressModify) {
                    return;
                }
                String query = textWidget.getText().trim();

                // Cancel any previously scheduled query
                if (pendingQuery != null) {
                    display.timerExec(-1, pendingQuery);
                    pendingQuery = null;
                }

                if (query.length() < MIN_QUERY_LENGTH) {
                    hidePopup();
                    return;
                }

                // Schedule a debounced query
                pendingQuery = new Runnable() {
                    @Override
                    public void run() {
                        if (textWidget.isDisposed()) {
                            return;
                        }
                        final String currentQuery = textWidget.getText().trim();
                        if (currentQuery.length() < MIN_QUERY_LENGTH) {
                            hidePopup();
                            return;
                        }

                        // Run the API call in a background thread to keep the UI responsive
                        Thread thread = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                final List<String> results = gitlabApi.searchUsers(currentQuery);
                                if (!textWidget.isDisposed()) {
                                    display.asyncExec(new Runnable() {
                                        @Override
                                        public void run() {
                                            if (textWidget.isDisposed()) {
                                                return;
                                            }
                                            // Only show if the text hasn't changed since the query
                                            if (currentQuery.equals(textWidget.getText().trim())) {
                                                showSuggestions(textWidget, results);
                                            }
                                        }
                                    });
                                }
                            }
                        }, "GitlabUserSearch");
                        thread.setDaemon(true);
                        thread.start();
                    }
                };
                display.timerExec(DEBOUNCE_DELAY_MS, pendingQuery);
            }
        });

        // Hide popup when the text field loses focus (with a small delay so clicks on
        // the popup list are processed first)
        textWidget.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                display.timerExec(200, new Runnable() {
                    @Override
                    public void run() {
                        hidePopup();
                    }
                });
            }
        });
    }

    /**
     * Shows the autocomplete popup with the given suggestions below the text widget.
     */
    private void showSuggestions(Text textWidget, List<String> suggestions) {
        if (textWidget.isDisposed()) {
            return;
        }

        if (suggestions == null || suggestions.isEmpty()) {
            hidePopup();
            return;
        }

        Shell parentShell = textWidget.getShell();

        if (popupShell == null || popupShell.isDisposed()) {
            popupShell = new Shell(parentShell, SWT.ON_TOP | SWT.TOOL);
            popupShell.setLayout(new GridLayout(1, false));
            suggestionList = new org.eclipse.swt.widgets.List(popupShell, SWT.SINGLE | SWT.V_SCROLL);
            suggestionList.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

            // When the user clicks a suggestion, fill the text field
            suggestionList.addListener(SWT.Selection, event -> {
                int index = suggestionList.getSelectionIndex();
                if (index >= 0) {
                    suppressModify = true;
                    textWidget.setText(suggestionList.getItem(index));
                    textWidget.setSelection(textWidget.getText().length());
                    suppressModify = false;
                    hidePopup();
                    textWidget.setFocus();
                }
            });

            // Also accept Enter key on the list
            suggestionList.addListener(SWT.KeyDown, event -> {
                if (event.character == SWT.CR || event.character == SWT.LF) {
                    int index = suggestionList.getSelectionIndex();
                    if (index >= 0) {
                        suppressModify = true;
                        textWidget.setText(suggestionList.getItem(index));
                        textWidget.setSelection(textWidget.getText().length());
                        suppressModify = false;
                        hidePopup();
                        textWidget.setFocus();
                    }
                }
            });
        }

        // Update items
        suggestionList.removeAll();
        for (String s : suggestions) {
            suggestionList.add(s);
        }

        // Position popup below the text widget
        Point textLocation = textWidget.toDisplay(0, textWidget.getBounds().height);
        Rectangle textBounds = textWidget.getBounds();
        popupShell.setBounds(textLocation.x, textLocation.y, textBounds.width, Math.min(suggestions.size() * 20 + 10, 150));
        popupShell.setVisible(true);
    }

    /**
     * Hides the autocomplete popup if it is visible.
     */
    private void hidePopup() {
        if (popupShell != null && !popupShell.isDisposed()) {
            popupShell.setVisible(false);
        }
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
        hidePopup();
        super.okPressed();
    }

    @Override
    public boolean close() {
        if (popupShell != null && !popupShell.isDisposed()) {
            popupShell.dispose();
        }
        return super.close();
    }

    /**
     * Returns the collected parameters as a {@link PullRequestCreator.Params} object.
     */
    public PullRequestCreator.Params getParams() {
        PullRequestCreator.Params params = new PullRequestCreator.Params();
        params.message = message;
        params.targetBranch = targetBranch;
        params.assignTo = assignTo;
        params.forceSync = forceSync;
        return params;
    }
}
