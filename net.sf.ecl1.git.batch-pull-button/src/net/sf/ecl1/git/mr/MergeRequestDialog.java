package net.sf.ecl1.git.mr;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
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
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
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

    /** Popup shell for autocomplete suggestions */
    private Shell popupShell;
    /** Table widget inside the popup */
    private Table suggestionTable;
    /** Minimum characters before triggering a search */
    private static final int MIN_QUERY_LENGTH = 2;
    /** Whether an API request is currently in flight */
    private boolean queryInFlight = false;
    /** Whether a new query should be sent as soon as the current one completes */
    private boolean queryPending = false;
    /** Flag to suppress modify events when we programmatically set text */
    private boolean suppressModify = false;
    /** Avatar size in pixels */
    private static final int AVATAR_SIZE = 32;
    /** Row height for the suggestion table (avatar + padding for two text lines) */
    private static final int ROW_HEIGHT = 44;
    /** Cached avatar images that need to be disposed */
    private final List<Image> cachedImages = new ArrayList<>();
    /** Bold font for the full name line */
    private Font boldFont;
    /** Color for the @username line */
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

                if (query.length() < MIN_QUERY_LENGTH) {
                    hidePopup();
                    return;
                }

                if (queryInFlight) {
                    // A request is already running; it will check for changes when it completes
                    queryPending = true;
                    return;
                }

                // Send request immediately
                sendQuery(textWidget, query);
            }
        });

        // Forward Up/Down/Enter keys from the text widget to the suggestion table
        textWidget.addListener(SWT.KeyDown, new Listener() {
            @Override
            public void handleEvent(Event event) {
                if (popupShell == null || !popupShell.isVisible() || suggestionTable == null || suggestionTable.isDisposed()) {
                    return;
                }
                int count = suggestionTable.getItemCount();
                if (count == 0) {
                    return;
                }
                int index = suggestionTable.getSelectionIndex();

                if (event.keyCode == SWT.ARROW_DOWN) {
                    int next = (index < count - 1) ? index + 1 : 0;
                    suggestionTable.setSelection(next);
                    suggestionTable.redraw();
                    event.doit = false;
                } else if (event.keyCode == SWT.ARROW_UP) {
                    int prev = (index > 0) ? index - 1 : count - 1;
                    suggestionTable.setSelection(prev);
                    suggestionTable.redraw();
                    event.doit = false;
                } else if (event.character == SWT.CR || event.character == SWT.LF) {
                    if (index >= 0) {
                        selectSuggestion(textWidget, index);
                        event.doit = false;
                    }
                } else if (event.character == SWT.ESC) {
                    hidePopup();
                    event.doit = false;
                }
            }
        });

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
     * Sends a user search query to the API in a background thread.
     * When the response arrives, if the text has changed meanwhile, a new request
     * is sent immediately. Otherwise the results are displayed.
     */
    private void sendQuery(final Text textWidget, final String query) {
        final Display display = textWidget.getDisplay();
        queryInFlight = true;
        queryPending = false;

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                final List<GitlabApi.UserInfo> results = gitlabApi.searchUsers(query);
                if (!textWidget.isDisposed()) {
                    display.asyncExec(new Runnable() {
                        @Override
                        public void run() {
                            if (textWidget.isDisposed()) {
                                queryInFlight = false;
                                return;
                            }
                            queryInFlight = false;
                            String currentText = textWidget.getText().trim();

                            if (currentText.length() < MIN_QUERY_LENGTH) {
                                hidePopup();
                                return;
                            }

                            if (queryPending || !query.equals(currentText)) {
                                // Input changed while the request was in flight; send a new request immediately
                                queryPending = false;
                                sendQuery(textWidget, currentText);
                            } else {
                                // Input is still the same; show the results
                                showSuggestions(textWidget, results);
                            }
                        }
                    });
                } else {
                    queryInFlight = false;
                }
            }
        }, "GitlabUserSearch");
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * Selects the item at the given index and fills the text widget with the username.
     */
    private void selectSuggestion(Text textWidget, int index) {
        if (index < 0 || suggestionTable == null || index >= suggestionTable.getItemCount()) {
            return;
        }
        String username = (String) suggestionTable.getItem(index).getData("username");
        if (username != null) {
            suppressModify = true;
            textWidget.setText(username);
            textWidget.setSelection(textWidget.getText().length());
            suppressModify = false;
            hidePopup();
            textWidget.setFocus();
        }
    }

    /**
     * Shows the autocomplete popup with user suggestions below the text widget.
     * Each row displays: [avatar] bold full name / @username (Gitlab-style).
     */
    private void showSuggestions(Text textWidget, List<GitlabApi.UserInfo> users) {
        if (textWidget.isDisposed()) {
            return;
        }

        if (users == null || users.isEmpty()) {
            hidePopup();
            return;
        }

        Shell parentShell = textWidget.getShell();
        Display display = textWidget.getDisplay();

        if (popupShell == null || popupShell.isDisposed()) {
            popupShell = new Shell(parentShell, SWT.ON_TOP | SWT.TOOL);
            GridLayout popupLayout = new GridLayout(1, false);
            popupLayout.marginWidth = 0;
            popupLayout.marginHeight = 0;
            popupShell.setLayout(popupLayout);

            suggestionTable = new Table(popupShell, SWT.SINGLE | SWT.FULL_SELECTION | SWT.V_SCROLL);
            suggestionTable.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
            suggestionTable.setHeaderVisible(false);
            suggestionTable.setLinesVisible(false);

            // Owner-draw: set row height
            suggestionTable.addListener(SWT.MeasureItem, new Listener() {
                @Override
                public void handleEvent(Event event) {
                    event.height = ROW_HEIGHT;
                }
            });

            // Owner-draw: render avatar, bold name, and @username
            suggestionTable.addListener(SWT.PaintItem, new Listener() {
                @Override
                public void handleEvent(Event event) {
                    TableItem item = (TableItem) event.item;
                    GC gc = event.gc;
                    int x = event.x + 6;
                    int y = event.y;

                    // Draw avatar
                    Image avatar = (Image) item.getData("avatar");
                    if (avatar != null && !avatar.isDisposed()) {
                        gc.drawImage(avatar, x, y + (ROW_HEIGHT - AVATAR_SIZE) / 2);
                    }
                    x += AVATAR_SIZE + 10;

                    String name = (String) item.getData("name");
                    String username = (String) item.getData("username");

                    // Draw full name in bold on the first line
                    Font previousFont = gc.getFont();
                    Color previousFg = gc.getForeground();

                    String nameText = (name != null && !name.isEmpty()) ? name : username;
                    gc.setFont(boldFont);
                    Point nameExtent = gc.textExtent(nameText);
                    int textY = y + 4;
                    gc.drawText(nameText, x, textY, true);

                    // Draw @username below in grey
                    gc.setFont(previousFont);
                    gc.setForeground(usernameColor);
                    gc.drawText("@" + username, x, textY + nameExtent.y + 1, true);

                    gc.setForeground(previousFg);
                }
            });

            // EraseItem to prevent default text drawing
            suggestionTable.addListener(SWT.EraseItem, new Listener() {
                @Override
                public void handleEvent(Event event) {
                    event.detail &= ~SWT.FOREGROUND;
                }
            });

            // On double-click, select the user
            suggestionTable.addListener(SWT.DefaultSelection, new Listener() {
                @Override
                public void handleEvent(Event event) {
                    selectSuggestion(textWidget, suggestionTable.getSelectionIndex());
                }
            });

            // On single mouse click, select the user
            suggestionTable.addListener(SWT.MouseUp, new Listener() {
                @Override
                public void handleEvent(Event event) {
                    selectSuggestion(textWidget, suggestionTable.getSelectionIndex());
                }
            });

            // On Enter key in the table itself
            suggestionTable.addListener(SWT.KeyDown, new Listener() {
                @Override
                public void handleEvent(Event event) {
                    if (event.character == SWT.CR || event.character == SWT.LF) {
                        selectSuggestion(textWidget, suggestionTable.getSelectionIndex());
                    }
                }
            });
        }

        // Dispose previous avatars and clear
        disposeCachedImages();
        suggestionTable.removeAll();

        // Populate table items
        for (GitlabApi.UserInfo user : users) {
            TableItem item = new TableItem(suggestionTable, SWT.NONE);
            item.setData("username", user.username);
            item.setData("name", user.name);
            item.setText(""); // text drawn via owner-draw

            // Load avatar asynchronously
            if (user.avatarUrl != null && !user.avatarUrl.isEmpty()) {
                final String avatarUrl = user.avatarUrl;
                final TableItem tableItem = item;
                Thread avatarThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Image img = downloadAvatar(display, avatarUrl);
                        if (img != null && !display.isDisposed()) {
                            display.asyncExec(new Runnable() {
                                @Override
                                public void run() {
                                    if (!tableItem.isDisposed()) {
                                        tableItem.setData("avatar", img);
                                        cachedImages.add(img);
                                        if (suggestionTable != null && !suggestionTable.isDisposed()) {
                                            suggestionTable.redraw();
                                        }
                                    } else {
                                        img.dispose();
                                    }
                                }
                            });
                        }
                    }
                }, "AvatarDownload");
                avatarThread.setDaemon(true);
                avatarThread.start();
            }
        }

        // Position popup below the text widget
        Point textLocation = textWidget.toDisplay(0, textWidget.getBounds().height);
        Rectangle textBounds = textWidget.getBounds();
        int visibleRows = Math.min(users.size(), 6);
        int height = visibleRows * ROW_HEIGHT + suggestionTable.getBorderWidth() * 2 + 2;
        popupShell.setBounds(textLocation.x, textLocation.y, textBounds.width, height);
        popupShell.setVisible(true);
    }

    /**
     * Downloads an avatar image from the given URL and scales it to {@link #AVATAR_SIZE}.
     *
     * @return the scaled Image, or null on error
     */
    private Image downloadAvatar(Display display, String avatarUrl) {
        try {
            URL url = new URL(avatarUrl);
            try (InputStream is = url.openStream()) {
                ImageData original = new ImageData(is);
                ImageData scaled = original.scaledTo(AVATAR_SIZE, AVATAR_SIZE);
                return new Image(display, scaled);
            }
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Disposes all cached avatar images.
     */
    private void disposeCachedImages() {
        for (Image img : cachedImages) {
            if (!img.isDisposed()) {
                img.dispose();
            }
        }
        cachedImages.clear();
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
        disposeCachedImages();
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
