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

package dk.dbc.dataio.sink.openupdate;

import dk.dbc.commons.addi.AddiReader;
import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.dataio.sink.util.DocumentTransformer;
import dk.dbc.oss.ns.catalogingupdate.BibliographicRecord;
import org.junit.Test;
import org.w3c.dom.Element;
import org.xmlunit.matchers.CompareMatcher;

import javax.xml.transform.TransformerException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class AddiRecordPreprocessorTest {

    private static final String UPDATE_TEMPLATE_ATTRIBUTE_VALUE = "bog";
    private final DocumentTransformer documentTransformer = new DocumentTransformer();


    @Test(expected = NullPointerException.class)
    public void constructor_addiArgIsNull_throws() {
        new AddiRecordPreprocessor(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_addiArgIsInvalid_throws() {
        final AddiRecord addiRecord = toAddiRecord(getAddi("", getContentXml()));
        new AddiRecordPreprocessor(addiRecord);
    }

    @Test
    public void constructor_updateElementNotFound_throws() {
        final AddiRecord addiRecord = toAddiRecord(getAddi(getMetaXmlWithoutUpdateElement(), getContentXml()));
        try {
            new AddiRecordPreprocessor(addiRecord);
            fail();
        } catch (IllegalArgumentException e) {
            assertThat("Message contains: " + AddiRecordPreprocessor.ELEMENT, e.getMessage().contains(AddiRecordPreprocessor.ELEMENT), is(true));
            assertThat("Message contains: " + AddiRecordPreprocessor.NAMESPACE_URI, e.getMessage().contains(AddiRecordPreprocessor.NAMESPACE_URI), is(true));
        }
    }

    @Test
    public void constructor_updateTemplateAttributeFound_setsTemplateToAttributeValue() {
        final AddiRecord addiRecord = toAddiRecord(getAddi(getMetaXml(AddiRecordPreprocessor.UPDATE_TEMPLATE_ATTRIBUTE), getContentXml()));

        // Subject under test
        final AddiRecordPreprocessor addiRecordPreprocessor = new AddiRecordPreprocessor(addiRecord);

        // Verify
        final String template = addiRecordPreprocessor.getTemplate();
        assertThat("update template attribute value not null", template, not(nullValue()));
        assertThat("update template attribute value value", template, is(UPDATE_TEMPLATE_ATTRIBUTE_VALUE));
    }

    @Test
    public void constructor_updateTemplateAttributeNotFound_setsTemplateToEmptyString() {
        final AddiRecord addiRecord = toAddiRecord(getAddi(getMetaXml("fisk"), getContentXml()));

        // Subject under test
        final AddiRecordPreprocessor addiRecordPreprocessor = new AddiRecordPreprocessor(addiRecord);

        // Verify
        final String template = addiRecordPreprocessor.getTemplate();
        assertThat("update template attribute value not null", template, not(nullValue()));
        assertThat("update template attribute value is empty", template, is(""));
    }

    @Test public void getMarcXChangeRecord_bibliographicalRecordIsBuild_returnsBibliographicalRecord() throws TransformerException {
        final String contentXml = getContentXml();
        final AddiRecord addiRecord = toAddiRecord(getAddi(getMetaXml(AddiRecordPreprocessor.UPDATE_TEMPLATE_ATTRIBUTE), contentXml));
        final AddiRecordPreprocessor addiRecordPreprocessor = new AddiRecordPreprocessor(addiRecord);

        // Subject under test
        final BibliographicRecord bibliographicalRecord = addiRecordPreprocessor.getMarcXChangeRecord();

        // Verify
        assertThat("Bibliographical record not null", bibliographicalRecord, not(nullValue()));
        assertThat("BibliographicRecord recordPackaging", bibliographicalRecord.getRecordPacking(), is(AddiRecordPreprocessor.RECORD_PACKAGING));
        assertThat("BibliographicRecord recordSchema", bibliographicalRecord.getRecordSchema(), is(AddiRecordPreprocessor.RECORD_SCHEMA));
        assertThat("BibliographicRecord extraRecordData is empty", bibliographicalRecord.getExtraRecordData().getContent().size(), is(0));

        final Element element = (Element) bibliographicalRecord.getRecordData().getContent().get(0);
        final byte[] bibliographicalContent = documentTransformer.documentToByteArray(element.getOwnerDocument());
        final String bibliographicalContentXml = new String(bibliographicalContent, StandardCharsets.UTF_8);
        assertThat("BibliographicalRecord content matches addi content", bibliographicalContentXml, isEquivalentTo(contentXml));
    }

    /*
     * Private methods
     */

    public static CompareMatcher isEquivalentTo(Object control) {
        return CompareMatcher.isSimilarTo(control)
                .throwComparisonFailure()
                .normalizeWhitespace()
                .ignoreComments();
    }

    private String getMetaXml(String attribute) {
        return "<es:referencedata xmlns:es=\"http://oss.dbc.dk/ns/es\">" +
                "<es:info format=\"basis\" language=\"dan\" submitter=\"870970\"/>" +
                "<dataio:sink-update-template xmlns:dataio=\"dk.dbc.dataio.processing\"" + " " + attribute + "=\"bog\" charset=\"danmarc2\"/>" +
                "</es:referencedata>";
    }

    private String getMetaXmlWithoutUpdateElement() {
        return "<es:referencedata xmlns:es=\"http://oss.dbc.dk/ns/es\">" +
                "<es:info format=\"basis\" language=\"dan\" submitter=\"870970\"/>" +
                " charset=\"danmarc2\"/>" +
                "</es:referencedata>";
    }

    private String getContentXml() {
        return "<marcx:collection xmlns:marcx=\"info:lc/xmlns/marcxchange-v1\">" +
                "<marcx:record format=\"danMARC2\">" +
                "<marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"245\">" +
                "<marcx:subfield code=\"a\">field1</marcx:subfield>" +
                "</marcx:datafield></marcx:record></marcx:collection>";
    }

    private AddiRecord toAddiRecord(byte[] data) {
        final AddiReader addiReader = new AddiReader(new ByteArrayInputStream(data));
        try {
            return addiReader.getNextRecord();
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private byte[] getAddi(String metaXml, String contentXml) {
        return (metaXml.trim().getBytes().length +
                System.lineSeparator() +
                metaXml +
                System.lineSeparator() +
                contentXml.trim().getBytes().length +
                System.lineSeparator() +
                contentXml).getBytes();
    }

}