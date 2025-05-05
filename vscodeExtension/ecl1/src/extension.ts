import * as vscode from 'vscode';
import { spawn } from 'child_process';
import path from 'path';
import { readdirSync, existsSync, mkdirSync,
        writeFile, readFile, unlink } from 'fs';

/** Keep value in sync with activationEvents in package.json */
const INNER_WORKSPACE_NAMES = ['eclipse-workspace','workspace'];

const WORKSPACE_FOLDER = vscode.workspace.workspaceFolders ? vscode.workspace.workspaceFolders[0].uri.fsPath : '';


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

class Ecl1SettingsTreeItem extends vscode.TreeItem {
    constructor(public readonly settingName: string, public readonly value: boolean) {
        const displayName = formatCamelCaseToTitleCase(settingName);
        super(displayName, vscode.TreeItemCollapsibleState.None);
        this.tooltip = `Setting: ${displayName}`;
        this.iconPath = new vscode.ThemeIcon(value ? 'check' : 'x');
        this.command = {
            command: 'ecl1.toggleSetting',
            title: `Toggle ${displayName}`,
            arguments: [settingName, value]
        };
    }
}

class Ecl1SettingsTreeDataProvider implements vscode.TreeDataProvider<vscode.TreeItem> {
    private _onDidChangeTreeData: vscode.EventEmitter<Ecl1SettingsTreeItem | undefined | null | void> = new vscode.EventEmitter();
    readonly onDidChangeTreeData: vscode.Event<Ecl1SettingsTreeItem | undefined | null | void> = this._onDidChangeTreeData.event;

    getTreeItem(element: Ecl1SettingsTreeItem): vscode.TreeItem {
        return element;
    }

    async getChildren(): Promise<Ecl1SettingsTreeItem[]> {
        const settings = vscode.workspace.getConfiguration('ecl1');
        const settingNames = Object.keys(settings).filter(name => typeof settings.get(name) === 'boolean');;
        return settingNames.map(name => new Ecl1SettingsTreeItem(name, settings.get(name) as boolean));
    }

    refresh(): void {
        this._onDidChangeTreeData.fire();
    }

    dispose(): void {
        this._onDidChangeTreeData.dispose();
    }
}

/**
 * Runs an ecl1 autostart task, if it is enabled.
 * @param extensionPath extensionPath
 * @param name task name ({@link ecl1JarsAutostart})
 */
function runAutostartTask(extensionPath: string, name: string){
    const config = vscode.workspace.getConfiguration('ecl1');
    const settingName = name.replace(/\s+/g, "");
    const isEnabled = config.get<boolean>(`autostart${settingName}`);
    if(isEnabled){
        vscode.window.showInformationMessage(`Starting ecl1 autostart job ${name}...`);
        runEcl1Jar(extensionPath, ecl1JarsAutostart[name], name);
    } 
}

/**
 * Starts all enabled ecl1 autostart tasks.
 * @param extensionPath extensionPath
 */
function startEcl1AutostartTasks(extensionPath: string) {
    for(const name in ecl1JarsAutostart){
        runAutostartTask(extensionPath, name);
    }
}

/**
 * The keys should be used as camelCase setting names (prefixed with "autostart") in package.json.
 * Avoid using acronyms (like LFS or API) in setting names because VS Code's Settings UI
 * automatically formats camelCase to Title Case with spaces, which misformats acronyms.
 */
const ecl1JarsAutostart: { [key: string]: string } = {
    "Hook Updater": "jars/net.sf.ecl1.git.updatehooks-all.jar",
    "Lfs Prune": "jars/net.sf.ecl1.git.auto-lfs-prune-all.jar",
};

const ecl1Jars: { [key: string]: string } = {
    "Import Wizard": "jars/net.sf.ecl1.import.wizard-all.jar",
    "Commit Exporter": "jars/net.sf.ecl1.commit.exporter-all.jar",
    "Open Preferences": "jars/h1modules.utilities-all.jar",
    "New Extension Wizard": "jars/h1modules-all.jar",
    "Git Batch Pull": "jars/net.sf.ecl1.git.batch-pull-button-all.jar"
};


export async function activate(context: vscode.ExtensionContext) {
    // Only activate in HisInOne workspace
    if(!isHisInOneWorkspace()){
        return;
    }
    // Only activate if Java is installed
    if(!await isJavaInstalled()){
        vscode.window.showErrorMessage(
            'Ecl1 requires a local Java installation! Please install Java and restart the workspace.'
        );
        return;
    }
    // Activate sidebar
    vscode.commands.executeCommand('setContext', 'ecl1ExtensionActivated', true);

    hideNonProjectsInWs();
    setGitRepositoryScanMaxDepth();

    startEcl1AutostartTasks(context.extensionPath);
    
    // Register commands tree view
    const commandTreeDataProvider = new Ecl1CommandTreeDataProvider();
    vscode.window.createTreeView('ecl1CommandsTreeView', {
        treeDataProvider: commandTreeDataProvider
    });

    // Refresh icon in command tree view navigation
    const refreshCommands = vscode.commands.registerCommand('ecl1CommandsTreeView.refresh', () =>
        commandTreeDataProvider.refresh()
    );

    // Register settings tree view
    const settingsTreeDataProvider = new Ecl1SettingsTreeDataProvider();
    vscode.window.createTreeView('ecl1SettingsTreeView', {
        treeDataProvider: settingsTreeDataProvider
    });
    
    // Refresh icon in settings tree view navigation
    const refreshSettings = vscode.commands.registerCommand('ecl1SettingsTreeView.refresh', () =>
        settingsTreeDataProvider.refresh()
    );

    // Regtister command to toggle settings when clicked
    const toggleSetting = vscode.commands.registerCommand('ecl1.toggleSetting', (settingKey: string, currentValue: boolean) => {
        const newValue = !currentValue;
        vscode.workspace.getConfiguration('ecl1').update(settingKey, newValue, vscode.ConfigurationTarget.Workspace);
        settingsTreeDataProvider.refresh();
    });
    
    // Register commands for ecl1 jars
    for(const [name, jarPath] of Object.entries(ecl1Jars)) {
        const commandId = `ecl1.runJar.${getCommandIdFromName(name)}`;
        const command = vscode.commands.registerCommand(commandId, () => {
            runEcl1Jar(context.extensionPath, jarPath, name);
        });
        context.subscriptions.push(command);
    }

    const configurationChangeListener = vscode.workspace.onDidChangeConfiguration((e) => {
        const configuration = vscode.workspace.getConfiguration();
        // Update exclusions when the setting changes
        if (e.affectsConfiguration('ecl1.hideNonProjects')) {
            hideNonProjectsInWs();
        // Run Hook Updater if set
        }else if (e.affectsConfiguration('ecl1.autostartHookUpdater')) {
            runAutostartTask(context.extensionPath, 'Hook Updater');
        // Run Lfs Prune if set
        }else if (e.affectsConfiguration('ecl1.autostartLfsPrune')) {
            runAutostartTask(context.extensionPath, 'Lfs Prune');
        // Update git repository scan depth
        }else if (e.affectsConfiguration('ecl1.gitRepositoryScanMaxDepth')) {
            if(configuration.get<boolean>('ecl1.gitRepositoryScanMaxDepth')) {
                setGitRepositoryScanMaxDepth();
            }else{
                // unset setting
                configuration.update('git.repositoryScanMaxDepth', undefined, vscode.ConfigurationTarget.Workspace);
            }
            vscode.window.showInformationMessage(
                "Changes to repository scan depth will apply after a window reload.",
                "Reload", "Later"
              ).then(selection => {
                if (selection === "Reload") {
                  vscode.commands.executeCommand("workbench.action.reloadWindow");
                }
              });
        }
        // Refresh settings view after changes
        settingsTreeDataProvider.refresh();
    });

    context.subscriptions.push(commandTreeDataProvider, refreshCommands, settingsTreeDataProvider, refreshSettings, toggleSetting, configurationChangeListener);
}

export function deactivate() {
    // Deactivate sidebar, reset context key
    vscode.commands.executeCommand('setContext', 'ecl1ExtensionActivated', false);
}

/**
 * Finds and returns the first existing inner workspace folder path ({@link INNER_WORKSPACE_NAMES}).
 *
 * @returns Full inner workspace path if found, else an empty string.
 */
function getInnerWorkspaceFolder() {
    return findInnerWorkspace(WORKSPACE_FOLDER);
}

/**
 * Recursively searches for a directory matching one of the inner workspace names ({@link INNER_WORKSPACE_NAMES}).
 * @param dir Directory path to start the search from.
 * @returns Full inner workspace path if found, else an empty string.
 */
function findInnerWorkspace(dir: string, depth: number = 0): string {
    const MAX_DEPTH = 5;
    if (depth > MAX_DEPTH) {
        return '';
    }
    const subDirs = readdirSync(dir, { withFileTypes: true })
        .filter(entry => entry.isDirectory()).map(entry => entry.name);;

    for (const subDirName of subDirs) {
        const fullPath = path.join(dir, subDirName);

        if (INNER_WORKSPACE_NAMES.includes(subDirName)) {
            return fullPath;
        }

        const found = findInnerWorkspace(fullPath, depth + 1);
        if (found) {
            return found;
        }
    }
    return '';
}

/**
 * Formats a camelCase string into Title Case with spaces.
 * (Analog to VS Code's Settings UI)
 * @param camelCaseString string to format
 * @returns A Title Case string with spaces.
 */
function formatCamelCaseToTitleCase(camelCaseString: string): string {
    // Add space before each capital letter
    const withSpaces = camelCaseString.replace(/([a-z])([A-Z])/g, '$1 $2');
    // Capitalize the first letter
    return withSpaces[0].toUpperCase() + withSpaces.slice(1);
  }

/** Replaces whitespace with '-' to get valid command name*/
function getCommandIdFromName(name: string){
    return name.replace(/\s+/g, '-').toLowerCase();
}

/** Sets git.repositoryScanMaxDepth to 2 */
function setGitRepositoryScanMaxDepth(){
    const configuration = vscode.workspace.getConfiguration();
    const isGitRepositoryScanMaxDepth = configuration.get<boolean>("ecl1.gitRepositoryScanMaxDepth");   
    if(isGitRepositoryScanMaxDepth){
        configuration.update('git.repositoryScanMaxDepth', 2, vscode.ConfigurationTarget.Workspace);
    }
}

/** Returns true if {@link INNER_WORKSPACE_NAMES} exists and webapps or a HISinOne-Extension-Project is present in workspace */
function isHisInOneWorkspace() {
    const innerWsPath = getInnerWorkspaceFolder();
    if(!innerWsPath){
        return false;
    }
    return getProjects(WORKSPACE_FOLDER).length > 0 || getProjects(innerWsPath).length > 0;
}

/**
 * Returns an array of directory names in the given folder that are HISinOne projects.
 * @param folderPath folderPath
 * @returns array of HISinOne project directory names
 */
function getProjects(folderPath: string) {
    const WEBAPPS_EXTENSIONS_FOLDER = "qisserver/WEB-INF/extensions/";
    const EXTENSION_PROJECT_FILE = "extension.ant.properties";
    const wsDirs = readdirSync(folderPath, {withFileTypes: true}).map(item => item.name);

    // Filter out projects
    const projects = wsDirs.filter(dir => {
        const webapps = path.join(folderPath, dir, WEBAPPS_EXTENSIONS_FOLDER);
        const extensionProject = path.join(folderPath, dir, EXTENSION_PROJECT_FILE);
        return existsSync(webapps) || existsSync(extensionProject);
    });

    return projects;
}

/** Hides non projects in workspace root*/
function hideNonProjectsInWs() {
    const configuration = vscode.workspace.getConfiguration();
    const isHideNonProjects = configuration.get<boolean>("ecl1.hideNonProjects");
    if(!isHideNonProjects) {
        removeExclusions();
        return;
    }

    const innerWsPath = getInnerWorkspaceFolder();
    const relativePath = path.relative(WORKSPACE_FOLDER, innerWsPath);
    const innerWsRoot = relativePath.split(path.sep)[0];
    
    const dirsToKeep = ['.vscode', innerWsRoot];
    const wsDirs = readdirSync(WORKSPACE_FOLDER, {withFileTypes: true}).map(item => item.name);
    const projects = getProjects(WORKSPACE_FOLDER);
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
        const fullPath = path.join(WORKSPACE_FOLDER, name);
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

    writeExclusionsToFile(dirsToExclude);
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
 * @param name name for displaying the output
 */
function runEcl1Jar(extensionPath: string, jarPath: string, name: string) {
    const fullJarPath = path.join(extensionPath, jarPath);
    const innerWsPath = getInnerWorkspaceFolder();
    const args = ['-jar', fullJarPath, innerWsPath];
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

/** Removes file exclusions created by {@link hideNonProjectsInWs} */
function removeExclusions() {
    const filePath = path.join(WORKSPACE_FOLDER, '.vscode', 'excludedNames.txt');
    if (!existsSync(filePath)) {
        // do nothing no files are excluded
        return;
    }
    const configuration = vscode.workspace.getConfiguration();
    // Clone the objects to avoid any issues with immutability
    let filesExclude = { ...configuration.get<Record<string, boolean>>('files.exclude') || {} };
    let searchExclude = { ...configuration.get<Record<string, boolean>>('search.exclude') || {} };
    let filesWatcherExclude = { ...configuration.get<Record<string, boolean>>('files.watcherExclude') || {} };

    // Read the file content
    readFile(filePath, 'utf8', (err, data) => {
        if (err) {
            vscode.window.showErrorMessage('Failed to read file excludedNames.txt: ' + err.message);
        } else {
            const fileNames = data.split('\n');
            // Remove exclusions
            fileNames.forEach((name) => {
                delete filesExclude[name];
                delete searchExclude[name];
                delete filesWatcherExclude[name];
            });
            // Update the settings
            configuration.update('files.exclude', filesExclude, vscode.ConfigurationTarget.Workspace);
            configuration.update('search.exclude', searchExclude, vscode.ConfigurationTarget.Workspace);
            configuration.update('files.watcherExclude', filesWatcherExclude, vscode.ConfigurationTarget.Workspace);

            // Remove excludedNames.txt
            unlink(filePath, (err) => {
                if (err) {
                    vscode.window.showErrorMessage('Error deleting file excludedNames.txt: ' + err.message);
                }
            });
        }
    });
}

/**
 * Writes given file names to excludedNames.txt
 * @param fileNames array of names
 */
function writeExclusionsToFile(fileNames: Array<string>) {
    const vscodeFolderPath = path.join(WORKSPACE_FOLDER, '.vscode');
    const filePath = path.join(vscodeFolderPath, 'excludedNames.txt');
    // Ensure the .vscode folder exists
    if (!existsSync(vscodeFolderPath)) {
        mkdirSync(vscodeFolderPath); 
    }
    const fileContent = fileNames.join('\n');

    writeFile(filePath, fileContent, 'utf8', (err) => {
        if (err) {
            vscode.window.showErrorMessage('Failed to write file excludedNames.txt: ' + err.message);
        }
    });
}