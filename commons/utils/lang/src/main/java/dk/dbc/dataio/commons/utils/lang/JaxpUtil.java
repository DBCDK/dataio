package dk.dbc.dataio.commons.utils.lang;

import dk.dbc.invariant.InvariantUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

/**
 * JaxpUtil - utility class providing helper methods for XML processing safe for use in
 * a multi-threaded environment
 * <p>
 * This class ensures thread safety by using thread local variables for the DocumentBuilderFactory
 * and TransformerFactory classes used internally by the methods. If not handled carefully in
 * environments using thread pools with long lived threads this might cause memory leak problems
 * so make sure to use appropriate memory analysis tools to verify correct behaviour.
 * </p>
 */
public class JaxpUtil {

    /**
     * Thread local variable used to give each thread its own TransformerFactory (since it is not thread-safe)
     */
    private static final ThreadLocal<TransformerFactory> transformerFactory =
            ThreadLocal.withInitial(TransformerFactory::newInstance);

    /**
     * Thread local variable used to give each thread its own DocumentBuilderFactory (since it is not thread-safe)
     */
    private static final ThreadLocal<DocumentBuilderFactory> documentBuilderFactory = ThreadLocal.withInitial(() -> {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        dbf.setValidating(false);
        return dbf;
    });

    /**
     * Parses the content of the given input source as an XML document
     *
     * @param xml containing the content to be parsed
     * @return Document a new DOM Document object representation of the parsed content
     * @throws NullPointerException         if given null valued xml argument
     * @throws ParserConfigurationException if a DocumentBuilder cannot be created
     * @throws SAXException                 if any parse errors occur
     * @throws IOException                  if any IO errors occur
     */
    public static Document parseDocument(InputSource xml)
            throws NullPointerException, ParserConfigurationException, SAXException, IOException {
        InvariantUtil.checkNotNullOrThrow(xml, "xml");
        DocumentBuilderFactory documentBuilderFactory = JaxpUtil.documentBuilderFactory.get();
        DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
        return documentBuilder.parse(xml);
    }

    /**
     * Parses the content of the given input string as an XML document
     *
     * @param xml containing the content to be parsed
     * @return Document a new DOM Document object representation of the parsed content
     * @throws NullPointerException     if given null valued xml argument
     * @throws IllegalArgumentException if given empty valued xml argument
     * @throws IllegalStateException    if a DocumentBuilder cannot be created
     * @throws SAXException             if any parse errors occur
     * @throws IOException              if any IO errors occur
     */
    public static Document parseDocument(String xml) throws NullPointerException, IllegalArgumentException, IOException, SAXException, IllegalStateException {
        try {
            InvariantUtil.checkNotNullNotEmptyOrThrow(xml, "xml");
            InputSource inputSource = new InputSource(new StringReader(xml));
            return parseDocument(inputSource);
        } catch (ParserConfigurationException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Transforms an XML Node into a string containing the corresponding XML
     *
     * @param node a DOM Node to be transformed into its string representation
     * @return string representation of the node
     * @throws NullPointerException if given null valued node argument
     * @throws TransformerException if a Transformer instance cannot be created or
     *                              if an unrecoverable error occurs during the course of
     *                              the transformation
     */
    public static String asString(Node node) throws NullPointerException, TransformerException {
        InvariantUtil.checkNotNullOrThrow(node, "node");
        TransformerFactory transformerFactory = JaxpUtil.transformerFactory.get();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        StringWriter buffer = new StringWriter();
        transformer.transform(new DOMSource(node), new StreamResult(buffer));
        return buffer.toString();
    }

    /**
     * Converts a byte array to its XML document representation
     *
     * @param bytes input bytes
     * @return document representation
     * @throws NullPointerException  if given null-valued bytes argument
     * @throws IOException           If any IO errors occur.
     * @throws SAXException          If any parse errors occur
     * @throws IllegalStateException if a DocumentBuilder cannot be created
     */
    public static Document toDocument(byte[] bytes) throws IOException, SAXException, IllegalStateException {
        try {
            final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
            final DocumentBuilderFactory documentBuilderFactory = JaxpUtil.documentBuilderFactory.get();
            final DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            return documentBuilder.parse(byteArrayInputStream);
        } catch (ParserConfigurationException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Converts an element into its XML document representation
     *
     * @param element element to be represented as a document
     * @return document representation
     * @throws IllegalStateException if a DocumentBuilder cannot be created
     */
    public static Document toDocument(Element element) throws IllegalStateException {
        try {
            final DocumentBuilderFactory documentBuilderFactory = JaxpUtil.documentBuilderFactory.get();
            final DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            final Document document = documentBuilder.newDocument();
            final Node importedNode = document.importNode(element, true);
            document.appendChild(importedNode);
            return document;
        } catch (ParserConfigurationException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Transforms a document into a byte array
     *
     * @param document document representation
     * @return document content as byte array
     * @throws NullPointerException if given null-valued document argument
     * @throws TransformerException If an unrecoverable error occurs during the course of the transformation.
     */
    public static byte[] getBytes(Document document) throws NullPointerException, TransformerException {
        final Source source = new DOMSource(document);
        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        final Result result = new StreamResult(byteArrayOutputStream);
        final TransformerFactory transformerFactory = JaxpUtil.transformerFactory.get();
        final Transformer transformer = transformerFactory.newTransformer();
        transformer.transform(source, result);
        return byteArrayOutputStream.toByteArray();
    }
}
