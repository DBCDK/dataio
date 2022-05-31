package dk.dbc.dataio.sink.util;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class DocumentTransformer {

    public static final String DATAIO_PROCESSING_NAMESPACE_URI = "dk.dbc.dataio.processing";
    public static final String DATAIO_PROCESSING_ELEMENT = "sink-processing";
    public static final String UPDATE_TEMPLATE_ELEMENT = "sink-update-template";

    public static final String ES_NAMESPACE_URI = "http://oss.dbc.dk/ns/es";
    public static final String ES_INFO_ELEMENT = "info";

    private DocumentBuilder documentBuilder;
    private Transformer transformer;

    public DocumentTransformer() {
        final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setNamespaceAware(true);
        final TransformerFactory transformerFactory = TransformerFactory.newInstance();
        try {
            documentBuilder = documentBuilderFactory.newDocumentBuilder();
            transformer = transformerFactory.newTransformer();
        } catch (ParserConfigurationException | TransformerConfigurationException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Converts a byte array to a document
     *
     * @param byteArray the byte array to convert
     * @return the byte array as document
     * @throws IOException  If any IO errors occur.
     * @throws SAXException If any parse errors occur
     */
    public Document byteArrayToDocument(byte[] byteArray) throws IOException, SAXException {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteArray);
        documentBuilder.reset();
        return documentBuilder.parse(byteArrayInputStream);
    }

    /**
     * This method removes all nodes in given node list from the dom
     *
     * @param nodes the nodes to remove
     */
    public void removeFromDom(NodeList nodes) {
        while (nodes.getLength() > 0) {
            final Node node = nodes.item(0);
            node.getParentNode().removeChild(node); // Remove node from dom
        }
    }

    /**
     * Converts a document to a byte array
     *
     * @param document the document to convert
     * @return document content as byte array
     * @throws TransformerException If an unrecoverable error occurs
     *                              during the course of the transformation.
     */
    public byte[] documentToByteArray(Document document) throws TransformerException {
        Source source = new DOMSource(document);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        Result result = new StreamResult(byteArrayOutputStream);
        transformer.reset();
        transformer.transform(source, result);
        return byteArrayOutputStream.toByteArray();
    }

    /**
     * Extracts attribute value from given document
     *
     * @param document      document from which to extract
     * @param namespaceUri  namespace URI of element containing attribute
     * @param elementName   name of element containing attribute
     * @param attributeName name of attribute
     * @return value of attribute or empty string if no attribute could be found on specified element
     * @throws NullPointerException     if given any null valued argument
     * @throws IllegalArgumentException if named element could not be found or if multiple elements were found
     */
    public String extractAttributeValue(Document document, String namespaceUri, String elementName, String attributeName)
            throws NullPointerException, IllegalArgumentException {
        final NodeList nodeList = document.getElementsByTagNameNS(namespaceUri, elementName);
        if (nodeList.getLength() > 0) { // element found
            if (nodeList.getLength() == 1) {
                return ((Element) nodeList.item(0)).getAttribute(attributeName);
            }
            throw new IllegalArgumentException(String.format(
                    "Multiple %s elements found with namespace URI %s", elementName, namespaceUri));
        }
        throw new IllegalArgumentException(String.format(
                "No element found matching local name %s and namespace URI %s", elementName, namespaceUri));
    }
}
