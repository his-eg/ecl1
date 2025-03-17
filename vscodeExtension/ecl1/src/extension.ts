// The module 'vscode' contains the VS Code extensibility API
import * as vscode from 'vscode';

class Ecl1TaskTreeItem extends vscode.TreeItem {
	
    constructor(public readonly task: vscode.Task) {
		// Remove first 6 chars from task name - 'ecl1: Name' -> 'Name'
        super(task.name.slice(6), vscode.TreeItemCollapsibleState.None);
        this.tooltip = 'Run task ' + task.name;
        this.command = {
            command: 'runTaskFromTree',
            title: 'Run Task',
            arguments: [task]
        };
    }
}

class Ecl1TaskTreeDataProvider implements vscode.TreeDataProvider<vscode.TreeItem> {

    getTreeItem(element: vscode.TreeItem): vscode.TreeItem {
        return element;
    }

	async getChildren(): Promise<vscode.TreeItem[]> {
        const tasks = await vscode.tasks.fetchTasks();
        // Only use tasks that start with 'ecl1:'
        const filteredTasks = tasks.filter(task => task.name.startsWith('ecl1:'));

        return filteredTasks.map(task => new Ecl1TaskTreeItem(task));
    }
}

async function startEcl1AutostartTasks() {
    const tasks = await vscode.tasks.fetchTasks();
    // Only use tasks that start with 'ecl1 autostart:'
    const autostartTasks = tasks.filter(task => task.name.startsWith('ecl1 autostart:'));
    if (autostartTasks.length > 0) {
        vscode.window.showInformationMessage(`Starting ${autostartTasks.length} ecl1 autostart jobs.`);
        autostartTasks.forEach(task => vscode.tasks.executeTask(task));
    }
}

export function activate(context: vscode.ExtensionContext) {
    // Register tree view
    const treeDataProvider = new Ecl1TaskTreeDataProvider();
    vscode.window.createTreeView('ecl1TasksTreeView', {
        treeDataProvider
    });

    // Command to run the task selected in the tree view
    let runTaskFromTree = vscode.commands.registerCommand('runTaskFromTree', (task: vscode.Task) => {
        vscode.tasks.executeTask(task);
    });

    context.subscriptions.push(runTaskFromTree);

    startEcl1AutostartTasks();
}

export function deactivate() {}