package de.his.cs.sys.extensions.wizards.utils.templates;

import static net.sf.ecl1.utilities.preferences.ExtensionToolsPreferenceConstants.TEMPLATE_ROOT_URLS;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import com.google.common.collect.Sets;

import h1modules.utilities.utils.Activator;
import net.sf.ecl1.utilities.general.ConsoleLogger;

/**
 * A utility to fetch templates from the URLs specified in preferences.
 *
 * @author keunecke
 */
public class TemplateFetcher {

    private static final ConsoleLogger logger = ConsoleLogger.getEcl1Logger();

	private final List<String> templateRootUrls;

    /**
     * Create a Fetcher that is able to try to read from several sources
     */
    public TemplateFetcher() {
        String templateRootUrlPreferenceValue = Activator.getPreferences().getString(TEMPLATE_ROOT_URLS);
        templateRootUrls = Arrays.asList(templateRootUrlPreferenceValue.split(","));
    }

    /**
     * Fetch templates to add, trying all template root URLs defined in preferences
     * @return collection of strings containing template names, or null if template list could not be read
     */
    public Collection<String> getTemplates() {
        for (String templateRootUrl : templateRootUrls) {
        	String trimmedTemplateRootUrl = templateRootUrl.trim();
        	if (trimmedTemplateRootUrl.isEmpty()) continue;
    		String templateListUrl = trimmedTemplateRootUrl + "/templatelist.txt";
    		Collection<String> templates = getTemplates(templateListUrl);
    		if (templates != null) {
    			logger.debug("Loaded template list from '" + templateListUrl + "'");
        		return templates;
    		}
    		// else: log a warning and try next URL
    		logger.error("Failed loading template list from '" + templateListUrl + "', server not available?");
        }
        return null; // complete fail
    }

    /**
     * Fetch templates to add from the given URL.
     * @param templateListUrl the URL of the template list file
     * @return collection of strings containing template names, or null if template list could not be read
     */
    private Collection<String> getTemplates(String templateListUrl) {
        HashSet<String> result = Sets.newHashSet();
        try (InputStream is = DownloadHelper.getInputStreamFromUrlFollowingRedirects(templateListUrl);) {
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String line = "";
            while ((line = br.readLine()) != null) {
                result.add(line);
                logger.debug("TemplateFetcher found template '" + line + "' for download.");
            }
        } catch (IOException e) {
        	logger.error("Error downloading template list '" + templateListUrl + "': " + e.getClass() + ": " + e.getMessage());
            //e.printStackTrace();
            return null;
        }
        return result;
    }
}
