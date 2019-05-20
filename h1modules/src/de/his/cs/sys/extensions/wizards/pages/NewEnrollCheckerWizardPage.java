package de.his.cs.sys.extensions.wizards.pages;

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
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.dialogs.WizardNewFileCreationPage;

import de.his.cs.sys.extensions.Activator;
import net.sf.ecl1.utilities.general.ConsoleLogger;
import net.sf.ecl1.utilities.templates.TemplateManager;

/**
 * The single page of the NewEnrollChecker wizard.
 * @author brummermann
 */
public class NewEnrollCheckerWizardPage extends WizardNewFileCreationPage {
	private static final ConsoleLogger logger = new ConsoleLogger(Activator.getDefault().getLog(), Activator.PLUGIN_ID, NewEnrollCheckerWizardPage.class.getSimpleName());

	public NewEnrollCheckerWizardPage(IStructuredSelection selection) {
		super("NewConfigFileWizardPage", selection);
		setTitle("New EnrollChecker");
		setDescription("Creates a new EnrollChecker ");
		setFileExtension("java");
	}

	@Override
	protected InputStream getInitialContents() {
		// The name of the source file to create
		String fileName = this.getFileName().replace(".java", "");
		// The full path to the folder in which we want to create the file
		String containerPath = this.getContainerFullPath().toPortableString();
		logger.debug("fileName = " + fileName + ", containerPath = " + containerPath);
		
		// Project and package names are guessed from the full path exploiting the position of the substring "/src/java/".
		// TODO Maybe "/de/his/" would be more robust?
		int pos = containerPath.indexOf("/src/java/");
		String projectName = containerPath.substring(0, pos).replace("/", "");
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
		String packageName = containerPath.substring(pos + 10).replace('/', '.');
		logger.debug("projectName = " + projectName + ", packageName = " + packageName);
		writeSpringEntry(project, packageName, fileName);
		
		// Write file with replaced variables
		Map<String, String> variables = new HashMap<String, String>();
		variables.put("[name]", fileName);
		variables.put("[package]", packageName);
		String content = new TemplateManager("src/java/EnrollChecker.java.template", variables).getContent();
		return new ByteArrayInputStream(content.getBytes());
	}

	private void writeSpringEntry(IProject project, String packageName, String fileName) {
		String firstLowercaseName = firstCharLowerCase(fileName);
		String sep = System.getProperty("line.separator");
		String newEntry =
				"		<bean id=\"" + project.getName() + "." + firstLowercaseName + "\"" + sep +
				"		  class=\"" + packageName + "." + fileName + "\"" + sep +
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
		} catch (IOException | CoreException e) {
    		logger.error2(e.getMessage(), e);
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
}
