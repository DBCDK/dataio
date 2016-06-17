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

package dk.dbc.dataio.commons.utils.lang;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * Utility for working with XML documents.
 * This class is not thread safe.
 */
public class XmlUtil {
    private final DocumentBuilder documentBuilder;

    public XmlUtil() throws IllegalStateException {
        final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setNamespaceAware(true);
        try {
            documentBuilder = documentBuilderFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Converts a byte array to its XML document representation
     * @param bytes input bytes
     * @return document representation
     * @throws NullPointerException if given null-valued bytes argument
     * @throws IOException If any IO errors occur.
     * @throws SAXException If any parse errors occur
     */
    public Document toDocument(byte[] bytes) throws NullPointerException, IOException, SAXException {
        final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
        documentBuilder.reset();
        return documentBuilder.parse(byteArrayInputStream);
    }
}
