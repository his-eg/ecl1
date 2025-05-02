# Ecl1 - Toolset for HISinOne Extensions

Extension to support Ecl1 Plugins in Visual Studio Code.

[Website](https://ecl1.sourceforge.net)

## Requirements / Activation

* **Java**: A local Java 1.8+ installation.
* **Inner Workspace Folder**: The workspace must include an inner "workspace folder", which must currently be named `eclipse-workspace` or `workspace`.
* **Extension-Project**: The workspace must contain either a `webapps` directory or a `HISinOne-Extension-Project` directory.
- Folder structure:
    ```
    WorkspaceRoot/
    ├── eclipse-workspace/ or workspace/
    │   └── Extension-Project/
    ├── webapps/
    ├── Extension-Project/
    ```

## Extension Settings

This extension contributes the following settings:

* `ecl1.autostartHookUpdater`: Enable or disable automatic start of ecl1 Hook Updater.

* `ecl1.autostartLfsPrune`: Enable or disable automatic start of ecl1 LFS Prune.

* `ecl1.gitRepositoryScanMaxDepth`: Set git.RepositoryScanMaxDepth to 2 (requires window reload).

* `ecl1.hideNonProjects`: Enable or disable exclusion of non projects in workspace root folder.


## Features

### Start Plugins

* Plugins can be started directly from the **ecl1 Sidebar**.
* Alternatively, you can start plugins via the **command palette** (`Ctrl+Shift+P` or `F1`) and run commands prefixed with `ecl1`.

### Auto Start Plugins

* Ecl1 Git Auto Lfs Prune
    * Runs git lfs prune in every git versioned project in the workspace. Git lfs prune deletes local copies of LFS files that are old, thus freeing up disk space.

* Ecl1 Update Git Hooks
    * Installs a git hook script in every HISinOne-Extension in the current workspace. These git hook scripts try to enforce the rule that every commit message must include a ticket number (format: #123456).


### Manually Started Plugins

* Preference Manager
    * Manager to configure preferences for ecl1.

* Create a new Extension
    * Wizard to create a HISinOne-extension skeleton.

* Import existing Extensions
    * Wizard to import Extensions. The used build server can be configured in the **Preference Manager**.
* Commit Exporter
    * Wizard to create HIS specific hotfix XML snippets. Useful for creating hotfixes for HISinOne.

* Git Batch Pull
    * Command to execute git pull for all git versioned projects in the workspace.

### Unsupported Ecl1 Plugins

* Ecl1 Classpath Container

* HIS Runtime Classpath Provider

* HIS Extension Point Collector

* Ecl1 Update Check (not needed for this extension)

