package de.his.cs.sys.extensions.wizards;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.dialogs.WizardNewFileCreationPage;

import de.his.cs.sys.extensions.wizards.utils.ResourceSupport;

/**
 * creates a new EnrollChecker
 *
 * @author brummermann
 */
public class NewEnrollCheckerWizard extends Wizard implements INewWizard {

	private IStructuredSelection selection;
	private NewEnrollCheckerWizardPage	 newFileWizardPage;
	private IWorkbench workbench;

	/**
	 * creates a wizard for new EnrollChecks
	 */
	public NewEnrollCheckerWizard() {
		setWindowTitle("New EnrollCheck");
	}

	@Override
	public void addPages() {
	    newFileWizardPage = new NewEnrollCheckerWizardPage(selection);
	    addPage(newFileWizardPage);
	}

	@Override
	public boolean performFinish() {

	    IFile file = newFileWizardPage.createNewFile();
	    if (file != null) {
			return true;
		} else {
			return false;
		}
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
	    this.workbench = workbench;
	    this.selection = selection;
	}

	class NewEnrollCheckerWizardPage extends WizardNewFileCreationPage {

	    public NewEnrollCheckerWizardPage (IStructuredSelection selection) {
	        super("NewConfigFileWizardPage", selection);
	        setTitle("New EnrollChecker");
	        setDescription("Creates a new EnrollChecker ");
	        setFileExtension("java");
	    }

	    @Override
	    protected InputStream getInitialContents() {

	    	// guess package and name
	    	String name = this.getFileName().replace(".java", "");
	    	String packageName = this.getContainerFullPath().toPortableString();
	    	int pos = packageName.indexOf("/src/java/");
	    	packageName = packageName.substring(pos + 10).replace('/', '.');

	    	// write file with replaced variables
	    	ByteArrayOutputStream buffer = new ByteArrayOutputStream();
	    	PrintStream writer = new PrintStream(buffer);
	    	InputStream is = ResourceSupport.class.getResourceAsStream("templates/src/java/EnrollChecker.java.template");
	    	BufferedReader br = new BufferedReader(new InputStreamReader(is));
	    	try {
		    	String line = br.readLine();
		    	while (line != null) {
		    		String temp = line.replace("[name]", name).replace("[package]", packageName);
		    		writer.println(temp);
		    		line = br.readLine();
		    	}
	    	} catch (IOException e) {
	    		e.printStackTrace();
	    	}
	    	return new ByteArrayInputStream(buffer.toByteArray());
	    }
	}
}