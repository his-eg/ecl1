package net.sf.ecl1.utilities.standalone;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import net.sf.ecl1.utilities.Activator;
import net.sf.ecl1.utilities.logging.ICommonLogger;
import net.sf.ecl1.utilities.logging.LoggerFactory;

/**
 * Class for adding entries to the classpath.
 */
public class ClasspathHandler {

    private static final ICommonLogger logger = LoggerFactory.getLogger(ClasspathHandler.class.getSimpleName(), Activator.PLUGIN_ID, Activator.getDefault());

    private final Path classpathFilePath;

    public ClasspathHandler(String classpathFilePath) {
        this.classpathFilePath = Paths.get(classpathFilePath).resolve(".classpath");
        initClasspathFile();
    }

    private void initClasspathFile() {
        if (!Files.exists(classpathFilePath)) {
            try {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                factory.setIgnoringElementContentWhitespace(true);
                DocumentBuilder builder = factory.newDocumentBuilder();
                Document doc = builder.newDocument();

                Element rootElement = doc.createElement("classpath");
                doc.appendChild(rootElement);

                saveDocument(doc);
            } catch (ParserConfigurationException e) {
                logger.error("Error initializing classpath file: " + e.getMessage());
            }
        }
    }

    /**
     * Adds an entry to the classpath.
     * @param kind kind
     * @param path path
     */
    public void addEntry(String kind, String path) {
        try {
            Document doc = loadDocument();
            if(doc == null){
                logger.error("Error loading classpath file: Entry could not be added");
                return;
            }
            Element root = doc.getDocumentElement();

            // Create new classpathentry element
            Element newEntry = doc.createElement("classpathentry");
            newEntry.setAttribute("kind", kind);
            newEntry.setAttribute("path", path);

            root.appendChild(newEntry);
            saveDocument(doc);
        } catch (DOMException e) {
            logger.error("Error modifying classpath file: " + e.getMessage());
        }
    }

    // Loads the XML document from the file
    private Document loadDocument() {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder;
        try {
            builder = factory.newDocumentBuilder();
            return builder.parse(classpathFilePath.toFile());
        } catch (SAXException | IOException | ParserConfigurationException e) {
            logger.error("Error loading classpath file: " + e.getMessage());
            return null;
        }
    }

    // Saves the XML document back to the file
    private void saveDocument(Document doc) {
        doc.setXmlStandalone(true);
        trimWhitespace(doc);
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        try {
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(new File(classpathFilePath.toString()));
        
            transformer.transform(source, result);
        } catch (TransformerException e) {
            logger.error("Error saving classpath file: " + e.getMessage());
        }
    }

    private static void trimWhitespace(Node node) {
        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.TEXT_NODE) {
                String trimmedText = child.getTextContent().trim();
                child.setTextContent(trimmedText);
                // If the trimmed content is empty, remove the node
                if (trimmedText.isEmpty()) {
                    node.removeChild(child);
                    i--; // Adjust index after removing
                }
            } else {
                trimWhitespace(child);
            }
        }
    }
}