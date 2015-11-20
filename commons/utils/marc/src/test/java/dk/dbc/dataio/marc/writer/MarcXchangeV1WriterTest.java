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

package dk.dbc.dataio.marc.writer;

import dk.dbc.dataio.marc.binding.ControlField;
import dk.dbc.dataio.marc.binding.DataField;
import dk.dbc.dataio.marc.binding.Leader;
import dk.dbc.dataio.marc.binding.MarcRecord;
import dk.dbc.dataio.marc.binding.SubField;
import org.junit.Test;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class MarcXchangeV1WriterTest {
    private static final String JAXP_SCHEMA_SOURCE = "http://java.sun.com/xml/jaxp/properties/schemaSource";
    private static final String JAXP_SCHEMA_LANGUAGE = "http://java.sun.com/xml/jaxp/properties/schemaLanguage";
    private static final String W3C_XML_SCHEMA = "http://www.w3.org/2001/XMLSchema";

    @Test
    public void write_equality() {
        final MarcXchangeV1Writer writer = new MarcXchangeV1Writer();
        final byte[] out = writer.write(getMarcRecord(), StandardCharsets.UTF_8);
        assertThat(asString(out), is(getMarcRecordAsMarcXchange()));
    }

    @Test
    public void write_skipAddXmlDeclaration() {
        final MarcXchangeV1Writer writer = new MarcXchangeV1Writer()
                .setProperty(MarcXchangeV1Writer.Property.ADD_XML_DECLARATION, Boolean.FALSE);
        final byte[] out = writer.write(getMarcRecord(), StandardCharsets.UTF_8);
        final String expectedOutput = getMarcRecordAsMarcXchange();
        assertThat(asString(out), is(expectedOutput.substring(expectedOutput.indexOf('\n') + 1)));
    }

    @Test
    public void write_validity() throws ParserConfigurationException, IOException, SAXException {
        final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setNamespaceAware(true);
        documentBuilderFactory.setValidating(true);
        documentBuilderFactory.setAttribute(JAXP_SCHEMA_LANGUAGE, W3C_XML_SCHEMA);
        documentBuilderFactory.setAttribute(JAXP_SCHEMA_SOURCE,
                MarcXchangeV1WriterTest.class.getResource("/marcxchange-1-1.xsd").toString());
        final DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
        documentBuilder.setErrorHandler(new ErrorHandler() {
            @Override
            public void fatalError(SAXParseException exception) throws SAXException {
                throw new IllegalStateException(exception);
            }
            @Override
            public void error(SAXParseException exception) throws SAXException {
                throw new IllegalStateException(exception);
            }
            @Override
            public void warning(SAXParseException exception) throws SAXException {
                throw new IllegalStateException(exception);
            }
        });

        final MarcXchangeV1Writer writer = new MarcXchangeV1Writer();
        final byte[] out = writer.write(getMarcRecord(), StandardCharsets.UTF_8);
        documentBuilder.parse(new ByteArrayInputStream(out));
    }

    @Test
    public void setProperty_valueOfIncompatibleType_throws() {
        final MarcXchangeV1Writer writer = new MarcXchangeV1Writer();
        try {
            writer.setProperty(MarcXchangeV1Writer.Property.ADD_XML_DECLARATION, "true");
            fail("No ClassCastException thrown");
        } catch (ClassCastException e) {
        }
    }

    private String asString(byte[] bytes) {
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private MarcRecord getMarcRecord() {
        final ControlField f001 = new ControlField()
                .setTag("001")
                .setData("5 047 389 9");
        final ControlField f008 = new ControlField()
                .setTag("008")
                .setData("1980");
        final DataField f222 = new DataField()
                .setTag("222")
                .setInd1('0')
                .setInd2('0');
        f222.getSubfields()
                .add(new SubField()
                    .setCode('a')
                    .setData("Meddelelser om Grønland. Man & society"));
        f222.getSubfields()
                .add(new SubField()
                    .setCode('b')
                    .setData("Papirform"));
        final DataField f245 = new DataField()
                .setTag("245")
                .setInd1('0')
                .setInd2('0');
        f245.getSubfields()
                .add(new SubField()
                    .setCode('a')
                    .setData("Meddelelser om Grønland. Man & society"));
        final Leader leader = new Leader()
                .setData("01471cjm a2200349 a 4500");
        final MarcRecord marcRecord = new MarcRecord()
                .setLeader(leader);
        marcRecord.getFields().addAll(Arrays.asList(f001, f008, f222, f245));
        return marcRecord;
    }

    private String getMarcRecordAsMarcXchange() {
        return  "<?xml version='1.0' encoding='UTF-8'?>\n" +
                "<record xmlns='info:lc/xmlns/marcxchange-v1' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' xsi:schemaLocation='info:lc/xmlns/marcxchange-v1 http://www.loc.gov/standards/iso25577/marcxchange-1-1.xsd'>" +
                "<leader>01471cjm a2200349 a 4500</leader>" +
                "<controlfield tag='001'>5 047 389 9</controlfield>" +
                "<controlfield tag='008'>1980</controlfield>" +
                "<datafield ind1='0' ind2='0' tag='222'>" +
                "<subfield code='a'>Meddelelser om Grønland. Man &amp; society</subfield>" +
                "<subfield code='b'>Papirform</subfield>" +
                "</datafield>" +
                "<datafield ind1='0' ind2='0' tag='245'>" +
                "<subfield code='a'>Meddelelser om Grønland. Man &amp; society</subfield>" +
                "</datafield>" +
                "</record>";
    }
}