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
}

export function deactivate() {}