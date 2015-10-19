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

public class DocumentTransformer {

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
     * @param byteArray the byte array to convert
     * @return the byte array as document
     *
     * @throws IOException If any IO errors occur.
     * @throws SAXException If any parse errors occur
     */
    public Document byteArrayToDocument(byte[] byteArray) throws IOException, SAXException {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteArray);
        documentBuilder.reset();
        return documentBuilder.parse(byteArrayInputStream);
    }

    /**
     * This method removes all nodes in given node list from the dom
     * @param nodes the nodes to remove
     */
    public void removeFromDom(NodeList nodes) {
        while(nodes.getLength() > 0) {
            final Node node = nodes.item(0);
            node.getParentNode().removeChild(node); // Remove node from dom
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
    public byte[] documentToByteArray(Document document) throws TransformerException {
        Source source = new DOMSource(document);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        Result result = new StreamResult(byteArrayOutputStream);
        transformer.reset();
        transformer.transform(source, result);
        return byteArrayOutputStream.toByteArray();
    }
}
