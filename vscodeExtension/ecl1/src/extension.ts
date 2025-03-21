import * as vscode from 'vscode';
import { existsSync } from 'fs';

let tasks: vscode.Task[] = [];

async function fetchEcl1Tasks() {
    tasks = await vscode.tasks.fetchTasks();
    tasks = tasks.filter(task => task.name.startsWith('ecl1'));
}

class Ecl1TaskTreeItem extends vscode.TreeItem {
	
    constructor(public readonly task: vscode.Task) {
		// Remove first 6 chars from task name - 'ecl1: Name' -> 'Name'
        super(task.name.slice(6), vscode.TreeItemCollapsibleState.None);
        this.tooltip = 'Run task ' + task.name;
        this.command = {
            command: 'ecl1.runTaskFromTree',
            title: 'Run Task',
            arguments: [task]
        };
    }
}

class Ecl1TaskTreeDataProvider implements vscode.TreeDataProvider<vscode.TreeItem> {
    private _onDidChangeTreeData: vscode.EventEmitter<Ecl1TaskTreeItem | undefined | null | void> = new vscode.EventEmitter<Ecl1TaskTreeItem | undefined | null | void>();
    readonly onDidChangeTreeData: vscode.Event<Ecl1TaskTreeItem | undefined | null | void> = this._onDidChangeTreeData.event;
  
    refresh(): void {
      this._onDidChangeTreeData.fire();
    }

    getTreeItem(element: vscode.TreeItem): vscode.TreeItem {
        return element;
    }

	async getChildren(): Promise<vscode.TreeItem[]> {
        // always update tasks
        await fetchEcl1Tasks();
        // Only use tasks that start with 'ecl1:'
        const filteredTasks = tasks.filter(task => task.name.startsWith('ecl1:'));

        return filteredTasks.map(task => new Ecl1TaskTreeItem(task));
    }

    dispose(): void {
        this._onDidChangeTreeData.dispose();
    }
}

async function startEcl1AutostartTasks() {
    const config = vscode.workspace.getConfiguration();
    const isAutostartTasks = config.get<boolean>("ecl1.autostartTasks");

    if (!isAutostartTasks) {
        return;
    }

    if (tasks.length === 0) {
        await fetchEcl1Tasks();
    }
    // Only use tasks that start with 'ecl1 autostart:'
    const autostartTasks = tasks.filter(task => task.name.startsWith('ecl1 autostart:'));
    if (autostartTasks.length > 0) {
        vscode.window.showInformationMessage(`Starting ${autostartTasks.length} ecl1 autostart jobs.`);
        autostartTasks.forEach(task => vscode.tasks.executeTask(task));
    }
}

async function initWorkspace() {
    await fetchEcl1Tasks();
    const workspaceFolder = vscode.workspace.workspaceFolders ? vscode.workspace.workspaceFolders[0].uri.fsPath : '';
    if (tasks.length === 0) {
        // only init workspace if ecl1 exists in workspace
        if (!existsSync(`${workspaceFolder}/eclipse-workspace/ecl1`)) {
            return false;
        }
    }
    vscode.window.showInformationMessage("Initializing ecl1 workspace...");
    const terminal = vscode.window.createTerminal('Initialize VSCode workspace');
    const gradleCommand = process.platform === "win32" ? ".\\gradlew.bat" : "./gradlew";
    terminal.sendText(`cd ${workspaceFolder}/eclipse-workspace/ecl1`);
    terminal.sendText(`${gradleCommand} initVSCWorkspace`);
    terminal.show();
    // Dispose the terminal after 1min
    setTimeout(() => {
        terminal.dispose();
    }, 30000);
    // fetch updated tasks
    await fetchEcl1Tasks();

    startEcl1AutostartTasks();
    return true;
}


export function activate(context: vscode.ExtensionContext) {
    // Run init workspace task, pre-fetches tasks to eliminate delay, especially when opening QuickPick
    if(!initWorkspace()) {
        // only activate if workspace can be initialized
        return;
    }

    // Register tree view
    const treeDataProvider = new Ecl1TaskTreeDataProvider();
    vscode.window.createTreeView('ecl1TasksTreeView', {
        treeDataProvider
    });

    // Refresh icon in tree view navigation
    const refreshTasks = vscode.commands.registerCommand('ecl1TasksTreeView.refreshTasks', () =>
        treeDataProvider.refresh()
    );

    // Command to run the task selected in the tree view
    const runTaskFromTree = vscode.commands.registerCommand('ecl1.runTaskFromTree', (task: vscode.Task) => {
        vscode.tasks.executeTask(task);
    });

    // Command to open QuickPick
    const runTaskInQuickPick = vscode.commands.registerCommand('ecl1.runTaskInQuickPick', async () => {
        if (tasks.length === 0) {
            await fetchEcl1Tasks();
        }

        // Only use tasks that start with 'ecl1:'
        const filteredTasks = tasks.filter(task => task.name.startsWith('ecl1:'));

        // Show the tasks in a QuickPick
        const selected = await vscode.window.showQuickPick(
            filteredTasks.map(task => ({
                label: task.name,
                task: task
            })),
            { placeHolder: "Select a task to run" }
        );

        if (selected) {
            vscode.tasks.executeTask(selected.task);
        }
    });

    // Listen for task changes and fetch changed tasks
    const tasksChangeListener = vscode.workspace.onDidChangeConfiguration(event => {
        if (event.affectsConfiguration("tasks")) {
            // Updates ecl1TasksTreeView. Also calls fetchTasks() to update global tasks
            treeDataProvider.refresh();
        }
    });

    context.subscriptions.push(treeDataProvider, refreshTasks, runTaskFromTree, runTaskInQuickPick, tasksChangeListener);
}

export function deactivate() {}