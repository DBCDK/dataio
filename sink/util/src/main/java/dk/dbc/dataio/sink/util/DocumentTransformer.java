package dk.dbc.dataio.sink.util;

import org.w3c.dom.Document;
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

public abstract class DocumentTransformer {

    private final static DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
    private final static TransformerFactory transformerFactory = TransformerFactory.newInstance();

    static {
        documentBuilderFactory.setNamespaceAware(true);
    }

    /**
     * Converts a byte array to a document
     * @param byteArray the byte array to convert
     * @return the byte array as document
     *
     * @throws IOException If any IO errors occur.
     * @throws SAXException If any parse errors occur
     */
    public static Document byteArrayToDocument(byte[] byteArray) throws IOException, SAXException {
        final DocumentBuilder documentBuilder;
        try {
            documentBuilder = documentBuilderFactory.newDocumentBuilder();
            final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteArray);
            return documentBuilder.parse(byteArrayInputStream);
        } catch (ParserConfigurationException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * This method removes all nodes in given node list from the dom
     * @param nodes the nodes to remove
     */
    public static void removeFromDom(NodeList nodes) {
        while(nodes.getLength() > 0) {
            final Node node = nodes.item(0);
            node.getParentNode().removeChild(node); // Removes node from dom
        }
    }

    /**
     * Converts a document to a byte array
     * @param document the document to convert
     *
     * @return document content as byte array
     * @throws TransformerException If an unrecoverable error occurs
     *         during the course of the transformation.
     */
    public static byte[] documentToByteArray(Document document) throws TransformerException{
        final Source source = new DOMSource(document);
        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        final Result result = new StreamResult(byteArrayOutputStream);
        final Transformer transformer;
        try {
            transformer = transformerFactory.newTransformer();
            transformer.transform(source, result);
            return byteArrayOutputStream.toByteArray();
        } catch (TransformerConfigurationException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * This method retrieves the value of a specified node
     * @param name the specified name of the node
     * @param node the node
     * @return the node value, null if the specified node is not found
     */
    public static String getNodeValue(String name, Node node) {
        final Node namedItem = node.getAttributes().getNamedItem(name);
        return namedItem == null ? null : namedItem.getNodeValue();
    }
}
