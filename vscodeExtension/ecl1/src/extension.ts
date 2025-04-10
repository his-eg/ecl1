import * as vscode from 'vscode';
import { spawn } from 'child_process';
import path from 'path';
import { readdirSync, existsSync } from 'fs';

const INNER_WORKSPACE_NAME = 'eclipse-workspace';

class Ecl1CommandTreeItem extends vscode.TreeItem {
    constructor(public readonly name: string) {
        super(name, vscode.TreeItemCollapsibleState.None);
        const commandId = `ecl1.runJar.${getCommandIdFromName(name)}`;
        this.tooltip = `Run ${name}`;
        this.iconPath = new vscode.ThemeIcon("run");
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
        runEcl1Jar(extensionPath, jarPath, path.join(workspaceFolder, INNER_WORKSPACE_NAME), key);
    }
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

export async function activate(context: vscode.ExtensionContext) {
    // only activate in HisInOne workspace
    if(!isHisInOneWorkspace()){
        return;
    }
    // only activate if Java is installed
    if(!await isJavaInstalled()){
        vscode.window.showErrorMessage(
            'Ecl1 requires a local Java installation! Please install Java and restart the workspace.'
        );
        return;
    }
    hideNonProjectsInWs();
    setGitRepositoryScanMaxDepth();

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
            runEcl1Jar(context.extensionPath, jarPath, path.join(workspaceFolder, INNER_WORKSPACE_NAME), name);
        });
        context.subscriptions.push(command);
    }
    context.subscriptions.push(treeDataProvider, refreshCommands);
}

export function deactivate() {}


/** Replaces whitespace with '-' to get valid command name*/
function getCommandIdFromName(name: string){
    return name.replace(/\s+/g, '-').toLowerCase();
}

/** Sets git.repositoryScanMaxDepth to 2 */
function setGitRepositoryScanMaxDepth(){
    const configuration = vscode.workspace.getConfiguration();
    configuration.update('git.repositoryScanMaxDepth', 2, vscode.ConfigurationTarget.Workspace);
}

/** Returns true if webapps or a HISinOne-Extension-Project is present in workspace */
function isHisInOneWorkspace(){
    return getProjects().length>0;
}

/** Returns an array of directory names in the workspace that are hisinone projects */
function getProjects() {
    const WEBAPPS_EXTENSIONS_FOLDER = "qisserver/WEB-INF/extensions/";
    const EXTENSION_PROJECT_FILE = "extension.ant.properties";
    let wsDirs = readdirSync(workspaceFolder, {withFileTypes: true}).map(item => item.name);

    // Filter out projects
    const projects = wsDirs.filter(dir => {
        const webapps = path.join(workspaceFolder, dir, WEBAPPS_EXTENSIONS_FOLDER);
        const extensionProject = path.join(workspaceFolder, dir, EXTENSION_PROJECT_FILE);
        return existsSync(webapps) || existsSync(extensionProject);
    });

    return projects;
}

/** Hides non projects in workspace root*/
function hideNonProjectsInWs() {
    const configuration = vscode.workspace.getConfiguration();
    const dirsToKeep = ['.vscode', INNER_WORKSPACE_NAME];
    const wsDirs = readdirSync(workspaceFolder, {withFileTypes: true}).map(item => item.name);
    const projects = getProjects();
    const dirsToExclude = wsDirs.filter(dir => !projects.includes(dir) && !dirsToKeep.includes(dir));
    
    // Clone the objects to avoid any issues with immutability
    let filesExclude = { ...configuration.get<Record<string, boolean>>('files.exclude') || {} };
    let searchExclude = { ...configuration.get<Record<string, boolean>>('search.exclude') || {} };
    let filesWatcherExclude = { ...configuration.get<Record<string, boolean>>('files.watcherExclude') || {} };

    // Exclude non projects
    dirsToExclude.forEach((dir) => {
        filesExclude[dir] = true;
        searchExclude[dir] = true;
        filesWatcherExclude[dir] = true;
    });

    // Remove exclusions for folders that dont exist anymore
    for(let name in filesExclude) {
        if (name.startsWith('*')) {
            continue;
        }
        const fullPath = path.join(workspaceFolder, name);
        if (!existsSync(fullPath)) {
            delete filesExclude[name];
            delete searchExclude[name];
            delete filesWatcherExclude[name];
        }
    }

    // Update the settings
    configuration.update('files.exclude', filesExclude, vscode.ConfigurationTarget.Workspace);
    configuration.update('search.exclude', searchExclude, vscode.ConfigurationTarget.Workspace);
    configuration.update('files.watcherExclude', filesWatcherExclude, vscode.ConfigurationTarget.Workspace);
}

const outputChannels: { [name: string]: vscode.OutputChannel } = {};

function getOutputChannelByName(name: string): vscode.OutputChannel {
    if (!outputChannels[name]) {
        outputChannels[name] = vscode.window.createOutputChannel(name);
    }
    return outputChannels[name];
}

/**
 * Runs an ecl1 jar.
 * @param extensionPath this extension path
 * @param jarPath path to jar
 * @param workspaceFolder path to inner workspace folder (not vscode folder)
 * @param name name for displaying the output
 */
function runEcl1Jar(extensionPath: string, jarPath: string, workspaceFolder: string, name: string) {
    const fullJarPath = path.join(extensionPath, jarPath);
    const args = ['-jar', fullJarPath, workspaceFolder];
    const javaProcess = spawn('java', args, { stdio: 'pipe' });
    
    const outputChannel = getOutputChannelByName('ecl1: ' + name);
    outputChannel.show();

    javaProcess.stdout.on('data', (data) => {
        outputChannel.appendLine(stripAnsiColor(data));
    });

    javaProcess.stderr.on('data', (data) => {
        outputChannel.appendLine(stripAnsiColor(data));
    });

    javaProcess.on('close', (code) => {
        outputChannel.appendLine(`Exit Code: ${code} \n\n`);
    });
}

function stripAnsiColor(input: string) {
    return input.toString().replace(/\x1B\[[0-9;]*m/g, '');
}

function isJavaInstalled() {
    return new Promise<boolean>((resolve) => {
        const javaProcess = spawn('java', ['--version'], { stdio: 'pipe' });
        javaProcess.on('close', (code) => {
            if (code !== 0) {
                resolve(false);
            } else {
                resolve(true);
            }
        });
    });
}