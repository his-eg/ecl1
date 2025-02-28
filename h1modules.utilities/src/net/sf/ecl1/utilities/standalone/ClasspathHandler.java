package net.sf.ecl1.utilities.standalone;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

import net.sf.ecl1.utilities.Activator;
import net.sf.ecl1.utilities.logging.ICommonLogger;
import net.sf.ecl1.utilities.logging.LoggerFactory;

/**
 * Class for adding entries to the classpath without validation.
 */
public class ClasspathHandler {

    private static final ICommonLogger logger = LoggerFactory.getLogger(ClasspathHandler.class.getSimpleName(), Activator.PLUGIN_ID, Activator.getDefault() != null ? Activator.getDefault().getLog() : null);

    private final Path classpathFilePath;

    public ClasspathHandler(String classpathFilePath) {
        this.classpathFilePath = Paths.get(classpathFilePath).resolve(".classpath");
        initClasspathFile();
    }

    private void initClasspathFile() {
        if (!Files.exists(classpathFilePath)) {
            try {
                String xmlSkeleton = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<classpath>\n</classpath>";
                Files.write(classpathFilePath, xmlSkeleton.getBytes(), StandardOpenOption.CREATE);
            } catch (IOException e) {
                logger.error("Error initializing classpath file: " + e.getMessage());
            }
        }
    }

    /**
     * Adds an Entry to the classpath, does not validate
     * @param kind kind
     * @param path path
     */
    public void addEntry(String kind, String path) {
        List<String> lines = readClasspathFile();
        
        String newEntry = String.format("\t<classpathentry kind=\"%s\" path=\"%s\"/>", kind, path);

        int lastIndex = lines.indexOf("</classpath>");
        lines.add(lastIndex, newEntry);
        
        // Write updated classpath back to the file
        writeClasspathFile(lines);
    }

    // Reads the classpath file and returns its content as a list of strings
    private List<String> readClasspathFile() {
        List<String> lines = new ArrayList<>();
        try {
            if (Files.exists(classpathFilePath)) {
                lines = Files.readAllLines(classpathFilePath);
            }
        } catch (IOException e) {
            logger.error("Error reading classpath file: " + classpathFilePath.toString() + "\n" + e.getMessage());
        }
        return lines;
    }

    // Writes the list of lines to the classpath file
    private void writeClasspathFile(List<String> lines) {
        try {
            Files.write(classpathFilePath, lines);
        } catch (IOException e) {
            logger.error("Error writing classpath file: " + classpathFilePath.toString() + "\n" + e.getMessage());
        }
    }
}
