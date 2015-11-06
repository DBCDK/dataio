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

import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.dataio.sink.util.DocumentTransformer;
import dk.dbc.oss.ns.catalogingupdate.BibliographicRecord;
import org.junit.Test;
import org.w3c.dom.Element;

import javax.xml.transform.TransformerException;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class AddiRecordPreprocessorTest extends AbstractOpenUpdateSinkTestBase {
    private final AddiRecordPreprocessor addiRecordPreprocessor = new AddiRecordPreprocessor();
    private final DocumentTransformer documentTransformer = new DocumentTransformer();

    @Test(expected = NullPointerException.class)
    public void preprocess_addiArgIsNull_throws() {
        addiRecordPreprocessor.preprocess(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void preprocess_addiArgIsInvalid_throws() {
        final AddiRecord addiRecord = toAddiRecord(getAddi("", getContentXml()));
        addiRecordPreprocessor.preprocess(addiRecord);
    }

    @Test
    public void preprocess_esInfoElementNotFound_throws() {
        final String invalidMetaXml = getInvalidMetaXml(
                "<dataio:sink-update-template xmlns:dataio=\"dk.dbc.dataio.processing\"/>");
        final AddiRecord addiRecord = toAddiRecord(getAddi(invalidMetaXml, getContentXml()));
        try {
            addiRecordPreprocessor.preprocess(addiRecord);
            fail();
        } catch (IllegalArgumentException e) {
            assertThat("Message contains: " + AddiRecordPreprocessor.ES_INFO_ELEMENT, e.getMessage().contains(AddiRecordPreprocessor.ES_INFO_ELEMENT), is(true));
            assertThat("Message contains: " + AddiRecordPreprocessor.ES_NAMESPACE_URI, e.getMessage().contains(AddiRecordPreprocessor.ES_NAMESPACE_URI), is(true));
        }
    }

    @Test
    public void preprocess_updateElementNotFound_throws() {
        final String invalidMetaXml = getInvalidMetaXml(
                "<es:info submitter=\"870970\"/>");
        final AddiRecord addiRecord = toAddiRecord(getAddi(invalidMetaXml, getContentXml()));
        try {
            addiRecordPreprocessor.preprocess(addiRecord);
            fail();
        } catch (IllegalArgumentException e) {
            assertThat("Message contains: " + AddiRecordPreprocessor.UPDATE_TEMPLATE_ELEMENT, e.getMessage().contains(AddiRecordPreprocessor.UPDATE_TEMPLATE_ELEMENT), is(true));
            assertThat("Message contains: " + AddiRecordPreprocessor.DATAIO_PROCESSING_NAMESPACE_URI, e.getMessage().contains(AddiRecordPreprocessor.DATAIO_PROCESSING_NAMESPACE_URI), is(true));
        }
    }

    @Test
    public void preprocess_esInfoSubmitterAttributeFound_setsSubmitterToAttributeValue() {
        final AddiRecord addiRecord = toAddiRecord(getAddi(getMetaXml(), getContentXml()));

        // Subject under test
        final AddiRecordPreprocessor.Result result = addiRecordPreprocessor.preprocess(addiRecord);

        // Verify
        assertThat("update value", result.getTemplate(), is(UPDATE_TEMPLATE_ATTRIBUTE_VALUE));
    }

    @Test
    public void preprocess_updateTemplateAttributeFound_setsTemplateToAttributeValue() {
        final AddiRecord addiRecord = toAddiRecord(getAddi(getMetaXml(), getContentXml()));

        // Subject under test
        final AddiRecordPreprocessor.Result result = addiRecordPreprocessor.preprocess(addiRecord);

        // Verify
        assertThat("submitter value", result.getSubmitter(), is(ES_INFO_SUBMITTER_ATTRIBUTE_VALUE));
    }

    @Test
    public void preprocess_esInfoSubmitterAttributeNotFound_setsSubmitterToEmptyString() {
        final String invalidMetaXml = getInvalidMetaXml(
                "<es:info/>" +
                "<dataio:sink-update-template xmlns:dataio=\"dk.dbc.dataio.processing\"/>"
        );
        final AddiRecord addiRecord = toAddiRecord(getAddi(invalidMetaXml, getContentXml()));

        // Subject under test
        final AddiRecordPreprocessor.Result result = addiRecordPreprocessor.preprocess(addiRecord);

        // Verify
        assertThat("submitter is empty", result.getSubmitter(), is(""));
    }

    @Test
    public void preprocess_updateTemplateAttributeNotFound_setsTemplateToEmptyString() {
        final String invalidMetaXml = getInvalidMetaXml(
                "<es:info submitter=\"870970\"/>" +
                "<dataio:sink-update-template xmlns:dataio=\"dk.dbc.dataio.processing\"/>"
        );
        final AddiRecord addiRecord = toAddiRecord(getAddi(invalidMetaXml, getContentXml()));

        // Subject under test
        final AddiRecordPreprocessor.Result result = addiRecordPreprocessor.preprocess(addiRecord);

        // Verify
        assertThat("template is empty", result.getTemplate(), is(""));
    }

    @Test
    public void preprocess_contentIsValid_setsBibliographicalRecord() throws TransformerException {
        final String contentXml = getContentXml();
        final AddiRecord addiRecord = toAddiRecord(getAddi(getMetaXml(), contentXml));

        // Subject under test
        final AddiRecordPreprocessor.Result result = addiRecordPreprocessor.preprocess(addiRecord);
        final BibliographicRecord bibliographicalRecord = result.getBibliographicRecord();

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
}