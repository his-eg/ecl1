package net.sf.ecl1.utilities.templates;

import net.sf.ecl1.utilities.Activator;
import net.sf.ecl1.utilities.logging.ConsoleLogger;
import net.sf.ecl1.utilities.preferences.PreferenceWrapper;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;

import com.google.common.collect.Maps;

/**
 * Replaces variables in templates given by name.
 *
 * Templates will be retrieved from http://ecl1.sourceforge.net/templates
 *
 * @company HIS eG
 * @author keunecke
 */
public class TemplateManager {

    private static final ConsoleLogger logger = new ConsoleLogger(Activator.getDefault().getLog(), Activator.PLUGIN_ID, TemplateManager.class.getSimpleName());

    private static final Map<String, String> nameReplacements = Maps.newHashMap();
    static {
        nameReplacements.put("settings", ".settings");
        nameReplacements.put(".template", "");
        nameReplacements.put("gitignore", ".gitignore");
    }
    
    private final Map<String, String> variables;

    private String templatePath;

	private final List<String> templateRootUrls;
	
	private final VariableReplacer variableReplacer;

    /**
     * Create a new template manager with a template and variables to replace in the template
     *
     * @param templatePath
     * @param variables
     */
    public TemplateManager(String templatePath, Map<String, String> variables) {
        templateRootUrls = PreferenceWrapper.getTemplateRootUrls();
        this.templatePath = templatePath;
        this.variables = variables;
        this.variableReplacer = new VariableReplacer(variables);
    }

    /**
     * Create a new template manager without variables to replace
     *
     * @param template the template path
     */
    public TemplateManager(String template) {
        this(template, new HashMap<String, String>());
    }

    /**
     * Do line-wise variable replacement on template, trying to read the template from several source URLs.
     *
     * @return result string with replaced variables, or null if the template could not be read
     */
    public String getContent() {
        for (String templateRootUrl : templateRootUrls) {
        	String trimmedTemplateRootUrl = templateRootUrl.trim();
        	if (trimmedTemplateRootUrl.isEmpty()) continue;
        	String content = getContent(trimmedTemplateRootUrl);
        	if (content!=null) {
        		return content;
        	}
        	// else: log warning and try next URL
        	logger.error2("Failed to load template '" + templatePath + "' from '" + trimmedTemplateRootUrl + "', server not available?");
        }
        return null; // complete fail
    }

    /**
     * Do line-wise variable replacement on template.
     *
     * @param templateRootUrl the template root URL from which to try to read
     * @return result string with replaced variables, or empty string if the template could not be read
     */
    private String getContent(String templateRootUrl) {
        String fullTemplateUrlString = templateRootUrl + "/" + templatePath;
        logger.debug("Loading template from: " + fullTemplateUrlString);
        try {
			return variableReplacer.replaceVariables(DownloadHelper.getInputStreamFromUrlFollowingRedirects(fullTemplateUrlString));
		} catch (IOException e) {
			logger.error("Failed to get the template from the following URL: " + fullTemplateUrlString + "\nThis was the exception: ", e);
			return ""; //Return empty string
		}
    }

    /**
     * Writes out the content of the given template with replaced variables to the given project in the same path as the template path.
     * The suffix ".template" is removed on file creation.
     *
     * @param project
     * @param content the content to write to the project
     */
    public void writeContent(IProject project, String content) {
        IFile file = project.getFile(doFolderAndFileRenaming(this.templatePath));
        InputStream is = new ByteArrayInputStream(content.getBytes());
        try {
        	IContainer parent = file.getParent();
        	if(parent instanceof IFolder) {
        		prepareFolder((IFolder) parent);
        	}
            file.create(is, true, null);
        } catch (CoreException e) {
        	logger.error2("Error creating file from template '" + this.templatePath + "': " + e.getMessage(), e);
        } finally {
            try {
                is.close();
            } catch (IOException e1) {
            	logger.error2("Error creating file from template '" + this.templatePath + "': " + e1.getMessage(), e1);
            }
        }
    }

    private String doFolderAndFileRenaming(String path) {
        String replacedFileName = path;
        for (Map.Entry<String, String> replacement : nameReplacements.entrySet()) {
            replacedFileName = replacedFileName.replace(replacement.getKey(), replacement.getValue());
        }
        return replacedFileName;
    }
    
    private void prepareFolder(IFolder folder) throws CoreException {
    	IContainer parent = folder.getParent();
    	if (parent instanceof IFolder){
    		prepareFolder((IFolder) parent);
    	}
    	if (!folder.exists()){
    		folder.create(true, true, null);
    	}
    }
}
