package de.his.cs.sys.extensions.wizards.utils.templates;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;

import com.google.common.collect.Sets;

public class TemplateFetcher {

    private final String templateListSourceUrl;

    /**
     * Create a Fetcher reading from given source
     *
     * @param templateListSourceUrl
     */
    public TemplateFetcher(String templateListSourceUrl) {
        this.templateListSourceUrl = templateListSourceUrl;
    }

    /**
     * Fetch templates to add from sf.net
     * @return iterable of strings containing template names
     */
    public Iterable<String> getTemplates() {
        HashSet<String> result = Sets.newHashSet();
        try {
            URL templateListUrl = new URL(templateListSourceUrl);
            try (InputStream is = templateListUrl.openStream();) {
                BufferedReader br = new BufferedReader(new InputStreamReader(is));
                String line = "";
                while ((line = br.readLine()) != null) {
                    result.add(line);
                    System.out.println("TemplateFetcher found template '" + line + "' for download.");

                }
            } catch (IOException e) {
                System.out.println("Could not download template list from " + templateListSourceUrl);
                e.printStackTrace();
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return result;
    }

}
