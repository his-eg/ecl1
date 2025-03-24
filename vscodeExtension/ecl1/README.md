# Ecl1 - Toolset for HISinOne Extensions

Extension to support Ecl1 Plugins in Visual Studio Code.

## Features

### Start Plugins

* Plugins can be started directly from the **ecl1 Sidebar**.
* Alternatively, you can start plugins using the **QuickPick 'Ecl1: Run Task'** in the command palette (press `Ctrl+Shift+P` or `F1` to open the command palette).

### Auto Start Plugins

* Ecl1 Git Auto Lfs Prune
    * Runs git lfs prune in every git versioned project in the workspace. Git lfs prune deletes local copies of LFS files that are old, thus freeing up disk space.

* Ecl1 Update Git Hooks
    * Installs a git hook script in every HISinOne-Extension in the current workspace. These git hook scripts try to enforce the rule that every commit message must include a ticket number (format: #123456).

* Ecl1 Update Check
    * Checks for ecl1 updates and pulls them automatically if any are found.


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

## Requirements

For this extension to work the ecl1 project must be present at following location:

* vscWorkspaceFolder/eclipse-workspace/ecl1

[Ecl1 on GitHub](https://github.com/his-eg/ecl1)


## Extension Settings

This extension contributes the following settings:

* `ecl1.autostartTasks`: Enable or disable automatic start of ecl1 autostart tasks.

