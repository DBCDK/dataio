package dk.dbc.dataio.harvester.utils.datafileverifier;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;

/**
 * Document Object Model (DOM) util class used for various asDocument()
 * conversions
 */
public class DomUtil {
    private final DocumentBuilder documentBuilder;

    /**
     * Class constructor
     * @throws ParserConfigurationException if a DocumentBuilder
     *   cannot be created which satisfies the configuration requested.
     */
    public DomUtil() throws ParserConfigurationException {
        final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setNamespaceAware(true);
        documentBuilder = documentBuilderFactory.newDocumentBuilder();
    }

    /**
     * @param dataFile file whose content is to be converted into Document
     * @return Document representation of file content
     * @throws IOException if unable to read file
     * @throws SAXException if unable to parse file as XML
     */
    public Document asDocument(File dataFile) throws IOException, SAXException {
        try {
            return documentBuilder.parse(dataFile);
        } finally {
            documentBuilder.reset();
        }
    }

    /**
     * @param element Element to be converted into Document
     * @return Document representation of given Element
     */
    public Document asDocument(Element element) {
        try {
            final Document document = documentBuilder.newDocument();
            final Node importedNode = document.importNode(element, true);
            document.appendChild(importedNode);
            return document;
        } finally {
            documentBuilder.reset();
        }
    }
}
