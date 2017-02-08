/*
 * DataIO - Data IO
 * Copyright (C) 2015 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
 * Denmark. CVR: 15149043
 *
 * This file is part of DataIO.
 *
 * DataIO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DataIO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DataIO.  If not, see <http://www.gnu.org/licenses/>.
 */

package dk.dbc.oclc.wciru;

import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
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
// TODO: 2/8/17 Merge with XmlUtil class in commons/utils/lang module
public class JaxpUtil {
    /**
     * Thread local variable used to give each thread its own TransformerFactory (since it is not thread-safe)
     */
    public static final ThreadLocal<TransformerFactory> transformerFactory =
            ThreadLocal.withInitial(TransformerFactory::newInstance);

    /**
     * Thread local variable used to give each thread its own DocumentBuilderFactory (since it is not thread-safe)
     */
    public static final ThreadLocal<DocumentBuilderFactory> documentBuilderFactory = ThreadLocal.withInitial(() -> {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        dbf.setValidating(false);
        return dbf;
    });

    /**
     * Parses the content of the given input source as an XML document
     *
     * @param xml containing the content to be parsed
     *
     * @return Document a new DOM Document object representation of the parsed content
     *
     * @throws NullPointerException if given null valued xml argument
     * @throws ParserConfigurationException if a DocumentBuilder cannot be created
     * @throws SAXException if any parse errors occur
     * @throws IOException if any IO errors occur
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
     *
     * @return Document a new DOM Document object representation of the parsed content
     *
     * @throws NullPointerException if given null valued xml argument
     * @throws IllegalArgumentException if given empty valued xml argument
     * @throws ParserConfigurationException if a DocumentBuilder cannot be created
     * @throws SAXException if any parse errors occur
     * @throws IOException if any IO errors occur
     */
    public static Document parseDocument(String xml)
            throws NullPointerException, IllegalArgumentException, IOException, SAXException, ParserConfigurationException {
        InvariantUtil.checkNotNullNotEmptyOrThrow(xml, "xml");
        InputSource inputSource = new InputSource(new StringReader(xml));
        return parseDocument(inputSource);
    }

    /**
     * Transforms an XML Node into a string containing the corresponding XML
     *
     * @param node a DOM Node to be transformed into its string representation
     *
     * @return string representation of the node
     *
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
}
