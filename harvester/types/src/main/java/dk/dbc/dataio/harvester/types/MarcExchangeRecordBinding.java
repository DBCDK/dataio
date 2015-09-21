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

package dk.dbc.dataio.harvester.types;

import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * This class exposes specialized field value bindings from MarcXchange documents
 * <p>
 * * Extracts identifier from 001a (must be non-empty)
 * <p>
 * * Extracts library from 001b (must be non-empty numeric)
 */
public class MarcExchangeRecordBinding {
    public static final String DATAFIELD_ELEMENT_NAME = "datafield";
    public static final String DATAFIELD_TAG_ATTRIBUTE_NAME = "tag";
    public static final String SUBFIELD_ELEMENT_NAME = "subfield";
    public static final String SUBFIELD_CODE_ATTRIBUTE_NAME = "code";
    private String namespace;
    private String id = null;
    private int library = 0;

    /**
     * Class constructor
     * @param document MARCXchange document
     * @throws NullPointerException if given null valued argument
     * @throws IllegalArgumentException if unable to extract required values from given document
     */
    public MarcExchangeRecordBinding(final Document document) throws NullPointerException, IllegalArgumentException {
        InvariantUtil.checkNotNullOrThrow(document, "document");
        final Element documentElement = document.getDocumentElement();
        namespace = documentElement.getNamespaceURI();
        extract(documentElement);
    }

    public String getId() {
        return id;
    }

    public int getLibrary() {
        return library;
    }

    private void extract(Element documentElement) throws IllegalArgumentException {
        final NodeList datafields = documentElement.getElementsByTagNameNS(namespace, DATAFIELD_ELEMENT_NAME);
        for (int i = 0; i < datafields.getLength(); i++) {
            final Element datafield = (Element) datafields.item(i);
            final String tag = datafield.getAttribute(DATAFIELD_TAG_ATTRIBUTE_NAME);
            switch (tag) {
                case "001": extractFrom001(datafield);
                    break;
                default:
            }
        }
        verifyExtraction();
    }

    private void extractFrom001(Element datafield) {
        final NodeList subfields = datafield.getElementsByTagNameNS(namespace, SUBFIELD_ELEMENT_NAME);
        for (int i = 0; i < subfields.getLength(); i++) {
            final Element subfield = (Element) subfields.item(i);
            final String code = subfield.getAttribute(SUBFIELD_CODE_ATTRIBUTE_NAME);
            switch (code) {
                case "a": id = subfield.getTextContent();
                    break;
                case "b": library = Integer.parseInt(subfield.getTextContent());
                    break;
                default:
            }
        }
    }

    private void verifyExtraction() throws IllegalArgumentException {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("invalid identifier (001*a)");
        }
        if (library == 0) {
            throw new IllegalArgumentException("invalid library (001*b)");
        }
    }
}
