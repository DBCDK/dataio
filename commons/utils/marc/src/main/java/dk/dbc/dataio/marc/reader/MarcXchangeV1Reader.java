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

package dk.dbc.dataio.marc.reader;

import dk.dbc.dataio.marc.binding.ControlField;
import dk.dbc.dataio.marc.binding.DataField;
import dk.dbc.dataio.marc.binding.Leader;
import dk.dbc.dataio.marc.binding.MarcRecord;
import dk.dbc.dataio.marc.binding.SubField;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Reader implementation for MarcXchange v1.1
 */
public class MarcXchangeV1Reader implements MarcReader {
    private static final String MARCXCHANGE_NAMESPACE_URI = "info:lc/xmlns/marcxchange-v1";

    private final MarcXchangeParser parser;

    /**
     * Creates new MarcXchange reader
     * @param inputStream stream containing MarcXchange records
     * @param encoding input stream encoding
     * @throws MarcReaderException on failure to create MarxXchange parser
     */
    public MarcXchangeV1Reader(BufferedInputStream inputStream, Charset encoding) throws MarcReaderException {
        parser = new MarcXchangeParser();
        try {
            parser.parse(inputStream, encoding);
        } catch (XMLStreamException e) {
            throw new MarcReaderException("Caught exception while creating parser", e);
        }
    }

    /**
     * Reads next record from input stream
     * @return record as MarcRecord instance or null if no record could be read
     * @throws MarcReaderException on unrecoverable error
     */
    @Override
    public MarcRecord read() throws MarcReaderException {
        try {
            return parser.read();
        } catch (XMLStreamException e) {
            final String error = String.format("Caught exception while parsing MarcXchange record %d: %s",
                    parser.getCurrentRecordNo(), e.getMessage());
            throw new MarcReaderException(error, e);
        }
    }

    private static class MarcXchangeParser {
        protected final XMLInputFactory xmlInputFactory;
        protected XMLStreamReader xmlStreamReader;
        protected int currentRecordNo = 0;

        public MarcXchangeParser() {
            xmlInputFactory = XMLInputFactory.newInstance();
            xmlInputFactory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, Boolean.TRUE);
        }

        public void parse(InputStream is, Charset encoding) throws XMLStreamException {
            xmlStreamReader = xmlInputFactory.createXMLStreamReader(is, encoding.name());
        }

        public MarcRecord read() throws XMLStreamException {
            if (!xmlStreamReader.hasNext()) {
                return null;
            }
            return readNext();
        }

        public int getCurrentRecordNo() {
            return currentRecordNo;
        }

        private MarcRecord readNext() throws XMLStreamException {
            while (xmlStreamReader.hasNext()) {
                final int next = xmlStreamReader.next();
                if (XMLStreamConstants.START_ELEMENT == next && isElement("record")) {
                    return readRecord();
                }
            }
            return null;
        }

        private MarcRecord readRecord() throws XMLStreamException {
            currentRecordNo++;

            final MarcRecord marcRecord = new MarcRecord();
            while (xmlStreamReader.hasNext()) {
                final int next = xmlStreamReader.next();

                if (XMLStreamConstants.START_ELEMENT == next) {
                    if (!xmlStreamReader.getNamespaceURI().equals(MARCXCHANGE_NAMESPACE_URI)) {
                        continue;
                    }
                    switch (xmlStreamReader.getLocalName()) {
                        case "leader":
                            marcRecord.setLeader(readLeader());
                            continue;
                        case "controlfield":
                            marcRecord.addField(readControlField());
                            continue;
                        case "datafield":
                            marcRecord.addField(readDataField());
                            continue;
                        default:
                            continue;
                    }
                }

                if (XMLStreamConstants.END_ELEMENT == next && isElement("record")) {
                    return marcRecord;
                }
            }
            return null;
        }

        private Leader readLeader() throws XMLStreamException {
            return new Leader().setData(xmlStreamReader.getElementText());
        }

        private ControlField readControlField() throws XMLStreamException {
            return new ControlField()
                    .setTag(xmlStreamReader.getAttributeValue(null, "tag"))
                    .setData(xmlStreamReader.getElementText());
        }

        private DataField readDataField() throws XMLStreamException {
            return new DataField()
                    .setTag(xmlStreamReader.getAttributeValue(null, "tag"))
                    .setInd1(getAttributeValue("ind1"))
                    .setInd2(getAttributeValue("ind2"))
                    .setInd3(getAttributeValue("ind3"))
                    .addAllSubFields(readSubFields());
        }

        private Collection<SubField> readSubFields() throws XMLStreamException {
            final ArrayList<SubField> subfields = new ArrayList<>();
            while (xmlStreamReader.hasNext()) {
                final int next = xmlStreamReader.next();

                if (XMLStreamConstants.START_ELEMENT == next) {
                    if (!xmlStreamReader.getNamespaceURI().equals(MARCXCHANGE_NAMESPACE_URI)) {
                        continue;
                    }
                    if ("subfield".equals(xmlStreamReader.getLocalName())) {
                        subfields.add(new SubField()
                            .setCode(getAttributeValue("code"))
                            .setData(xmlStreamReader.getElementText())
                        );
                        continue;
                    }
                }

                if (XMLStreamConstants.END_ELEMENT == next && isElement("datafield")) {
                    break;
                }
            }
            return subfields;
        }

        private boolean isElement(String localName) {
            return MARCXCHANGE_NAMESPACE_URI.equals(xmlStreamReader.getNamespaceURI())
                    && xmlStreamReader.getLocalName().equals(localName);
        }

        private Character getAttributeValue(String attributeName) {
            final String attributeValue = xmlStreamReader.getAttributeValue(null, attributeName);
            if (attributeValue != null) {
                return attributeValue.charAt(0);
            }
            return null;
        }
    }
}
