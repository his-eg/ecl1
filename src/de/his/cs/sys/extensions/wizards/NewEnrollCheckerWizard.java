package de.his.cs.sys.extensions.wizards;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.dialogs.WizardNewFileCreationPage;
import org.eclipse.ui.model.IWorkbenchAdapter;

import de.his.cs.sys.extensions.wizards.utils.ResourceSupport;
import de.his.cs.sys.extensions.wizards.utils.templates.TemplateVariableReplacer;

/**
 * creates a new EnrollChecker
 *
 * @author brummermann
 */
public class NewEnrollCheckerWizard extends Wizard implements INewWizard {

	private IStructuredSelection selection;
	private NewEnrollCheckerWizardPage newFileWizardPage;

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

	@SuppressWarnings("hiding")
	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		this.selection = selection;
	}

	class NewEnrollCheckerWizardPage extends WizardNewFileCreationPage {
		private final IProject project;

		public NewEnrollCheckerWizardPage(IStructuredSelection selection) {
			super("NewConfigFileWizardPage", selection);
			setTitle("New EnrollChecker");
			setDescription("Creates a new EnrollChecker ");
			setFileExtension("java");
			this.project = convertSelection(selection);
		}

		@Override
		protected InputStream getInitialContents() {

			// guess package and name
			String name = this.getFileName().replace(".java", "");
			String packageName = this.getContainerFullPath().toPortableString();
			int pos = packageName.indexOf("/src/java/");
			packageName = packageName.substring(pos + 10).replace('/', '.');
			writeSpringEntry(packageName, name);
			// write file with replaced variables
			Map<String, String> variables = new HashMap<String, String>();
			variables.put("[name]", name);
			variables.put("[package]", packageName);
			String content = new TemplateVariableReplacer("src/java/EnrollChecker.java.template", variables).replace();
			return new ByteArrayInputStream(content.getBytes());
		}

		private void writeSpringEntry(String packageName, String name) {
			String firstLowercaseName = firstCharLowerCase(name);
			String sep = System.getProperty("line.separator");
			String newEntry =
					"		<bean id=\"" + firstLowercaseName + "\"" + sep +
					"		  class=\"" + packageName + "." + name + "\"" + sep +
					"		  scope=\"prototype\">" + sep +
					"		  <property name=\"planelementDao\" ref=\"planelementDao\"/>" + sep +
					"		  <property name=\"personDao\" ref=\"personDao\"/>" + sep +
					"		</bean>" + sep;

			IFile file = project.getFile("/src/java/extension.beans.spring.xml");
			try {
				InputStream is = file.getContents();
				BufferedReader br = new BufferedReader(new InputStreamReader(is));
				ByteArrayOutputStream buffer = new ByteArrayOutputStream();
				PrintStream writer = new PrintStream(buffer);

				String line = br.readLine();
				while (line != null) {
					if (line.trim().equals("</beans>")) {
						writer.println(newEntry);
					}
					writer.println(line);
					line = br.readLine();
				}
				br.close();
				writer.close();
				file.setContents(new ByteArrayInputStream(buffer.toByteArray()), true, true, null);
			} catch (IOException e) {
				e.printStackTrace();
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}

		/**
		 * converts the first character to lower case
		 *
		 * @param name name
		 * @return first character in lower case
		 */
		private String firstCharLowerCase(String name) {
			if ((name == null) || name.equals("")) {
				return "";
			} else if (name.length() == 1) {
				return name.toLowerCase();
			}
			return name.substring(0, 1).toLowerCase() + name.substring(1);
		}

		// http://stackoverflow.com/a/10970127 by mchr
		private IProject convertSelection(IStructuredSelection structuredSelection) {
			IProject res = null;
			Object element = structuredSelection.getFirstElement();

			if (element instanceof IResource) {
				res = ((IResource) element).getProject();
			} else if (element instanceof IJavaElement) {
				IJavaElement javaElement = (IJavaElement) element;
				res = javaElement.getJavaProject().getProject();
			} else if (element instanceof IAdaptable) {
				IAdaptable adaptable = (IAdaptable) element;
				IWorkbenchAdapter adapter = (IWorkbenchAdapter) adaptable
						.getAdapter(IWorkbenchAdapter.class);
				if (adapter != null) {
					Object parent = adapter.getParent(adaptable);
					if (parent instanceof IJavaProject) {
						IJavaProject javaProject = (IJavaProject) parent;
						res = javaProject.getProject();
					}
				}
			}

			return res;
		}

	}
}
