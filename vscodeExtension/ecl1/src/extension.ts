import * as vscode from 'vscode';
import { spawn } from 'child_process';
import path from 'path';


class Ecl1CommandTreeItem extends vscode.TreeItem {
    constructor(public readonly name: string) {
        super(name, vscode.TreeItemCollapsibleState.None);
        const commandId = `ecl1.runJar.${getCommandIdFromName(name)}`;
        this.tooltip = `Run ${name}`;
        this.command = {
            command: commandId,
            title: `Run ${name}`
        };
    }
}

class Ecl1CommandTreeDataProvider implements vscode.TreeDataProvider<vscode.TreeItem> {
    private _onDidChangeTreeData: vscode.EventEmitter<Ecl1CommandTreeItem | undefined | null | void> = new vscode.EventEmitter();
    readonly onDidChangeTreeData: vscode.Event<Ecl1CommandTreeItem | undefined | null | void> = this._onDidChangeTreeData.event;

    refresh(): void {
        this._onDidChangeTreeData.fire();
    }

    getTreeItem(element: vscode.TreeItem): vscode.TreeItem {
        return element;
    }

    async getChildren(): Promise<vscode.TreeItem[]> {
        return Object.keys(ecl1Jars).map(name => new Ecl1CommandTreeItem(name));
    }

    dispose(): void {
        this._onDidChangeTreeData.dispose();
    }
}

function startEcl1AutostartTasks(extensionPath: string) {
    const config = vscode.workspace.getConfiguration();
    const isAutostartTasks = config.get<boolean>("ecl1.autostartTasks");

    if (!isAutostartTasks) {
        return;
    }

    for(var key in ecl1JarsAutostart) {
        const jarPath = ecl1JarsAutostart[key];
        vscode.window.showInformationMessage(`Starting ecl1 autostart job ${key}...`);
        runEcl1Jar(extensionPath, jarPath, path.join(workspaceFolder, 'eclipse-workspace'));
    }
}

/** Replaces whitespace with '-' to get valid command name*/
function getCommandIdFromName(name: string){
    return name.replace(/\s+/g, '-').toLowerCase();
}

const workspaceFolder = vscode.workspace.workspaceFolders ? vscode.workspace.workspaceFolders[0].uri.fsPath : '';

const ecl1Jars: { [key: string]: string } = {
    "Import Wizard": "jars/net.sf.ecl1.import.wizard-all.jar",
    "Commit Exporter": "jars/net.sf.ecl1.commit.exporter-all.jar",
    "Open Preferences": "jars/h1modules.utilities-all.jar",
    "New Extension Wizard": "jars/h1modules-all.jar",
    "Git Batch Pull": "jars/net.sf.ecl1.git.batch-pull-button-all.jar"
};

const ecl1JarsAutostart: { [key: string]: string } = {
    "Hook Updater": "jars/net.sf.ecl1.git.updatehooks-all.jar",
    "LFS Prune": "jars/net.sf.ecl1.git.auto-lfs-prune-all.jar",
};

export function activate(context: vscode.ExtensionContext) {

    startEcl1AutostartTasks(context.extensionPath);

    // Register tree view
    const treeDataProvider = new Ecl1CommandTreeDataProvider();
    vscode.window.createTreeView('ecl1CommandsTreeView', {
        treeDataProvider
    });

    // Refresh icon in tree view navigation
    const refreshCommands = vscode.commands.registerCommand('ecl1CommandsTreeView.refresh', () =>
        treeDataProvider.refresh()
    );

    // Register commands for ecl1 jars
    for(const [name, jarPath] of Object.entries(ecl1Jars)) {
        const commandId = `ecl1.runJar.${getCommandIdFromName(name)}`;

        const command = vscode.commands.registerCommand(commandId, () => {
            runEcl1Jar(context.extensionPath, jarPath, path.join(workspaceFolder, 'eclipse-workspace'));
        });

        context.subscriptions.push(command);
    }

    context.subscriptions.push(treeDataProvider, refreshCommands);
}

/**
 * Runs an ecl1 jar.
 * @param extensionPath this extension path
 * @param jarPath path to jar
 * @param workspaceFolder path to inner workspace folder (not vscode folder)
 */
function runEcl1Jar(extensionPath: string, jarPath: string, workspaceFolder: string) {
    const fullJarPath = path.join(extensionPath, jarPath);
    const args = ['-jar', fullJarPath, workspaceFolder];
    const javaProcess = spawn('java', args, { stdio: 'pipe' });
    
    javaProcess.stdout.on('data', (data) => {
        console.log(`${data}`);
    });

    javaProcess.stderr.on('data', (data) => {
        console.log(`${data}`);
    });

    javaProcess.on('close', (code) => {
        console.log(`Exit Code: ${code}`);
    });
}

export function deactivate() {}