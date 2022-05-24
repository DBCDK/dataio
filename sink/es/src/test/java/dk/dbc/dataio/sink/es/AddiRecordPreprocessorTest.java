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

package dk.dbc.dataio.sink.es;

import dk.dbc.commons.addi.AddiReader;
import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.dataio.addi.AddiContext;
import dk.dbc.dataio.addi.AddiException;
import dk.dbc.dataio.addi.bindings.EsReferenceData;
import dk.dbc.dataio.commons.utils.lang.JaxpUtil;
import dk.dbc.marc.DanMarc2Charset;
import dk.dbc.marc.Iso2709Packer;
import org.junit.Test;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static dk.dbc.commons.testutil.Assert.assertThat;
import static dk.dbc.commons.testutil.Assert.isThrowing;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class AddiRecordPreprocessorTest {
    private final AddiRecordPreprocessor preprocessor = new AddiRecordPreprocessor();
    private final String trackingId = "<rr:73639io:736362&'\"";
    private final AddiContext addiContext = new AddiContext();

    @Test
    public void execute_noProcessingTag_returnsUpdatedMetadataWithUnchangedContent() throws IOException, SAXException {
        final AddiRecord addiRecord = toAddiRecord(getValidAddiWithoutProcessing());
        final AddiRecord preprocessed = preprocessor.execute(addiRecord, trackingId);
        assertThat("AddiRecord.metadata is changed", preprocessed.getMetaData(), not(addiRecord.getMetaData()));
        assertThat("AddiRecord.metadata has trackingId attribute", getTrackingId(preprocessed), is(trackingId));
        assertThat("AddiRecord.content is unchanged", preprocessed.getContentData(), is(addiRecord.getContentData()));
    }

    @Test
    public void execute_processingTagWithEncodeAs2709SetToFalse_returnsUpdatedMetadataWithUnchangedContent() {
        final AddiRecord addiRecord = toAddiRecord(getValidAddiWithProcessingFalse());
        final AddiRecord preprocessed = preprocessor.execute(addiRecord, trackingId);
        assertThat("AddiRecord.metadata is changed", preprocessed.getMetaData(), not(addiRecord.getMetaData()));
        assertThat("AddiRecord.metadata has processing tag", hasSinkProcessingElement(preprocessed), is(false));
        assertThat("AddiRecord.metadata has trackingId attribute", getTrackingId(preprocessed), is(trackingId));
        assertThat("AddiRecord.content is unchanged", preprocessed.getContentData(), is(addiRecord.getContentData()));
    }

    @Test
    public void execute_processingTagWithEncodeAs2709SetToTrue_returnsUpdatedMetadataWithUpdatedContent() {
        final AddiRecord addiRecord = toAddiRecord(getValidAddiWithProcessingTrueAndValidMarcXContentData());
        final AddiRecord preprocessed = preprocessor.execute(addiRecord, trackingId);
        assertThat("AddiRecord.metadata is changed", preprocessed.getMetaData(), not(addiRecord.getMetaData()));
        assertThat("AddiRecord.metadata has processing tag", hasSinkProcessingElement(preprocessed), is(false));
        assertThat("AddiRecord.metadata has trackingId attribute", getTrackingId(preprocessed), is(trackingId));
        assertThat("AddiRecord.content is 2709 encoded", preprocessed.getContentData(), is(to2709(addiRecord.getContentData())));
    }

    @Test
    public void execute_processingTagWithEncodeAs2709SetToTrueAndInvalidRecordContent_throws() {
        final AddiRecord addiRecord = toAddiRecord(getValidAddiWithProcessingTrueAndInvalidMarcXContentData());
        assertThat(() -> preprocessor.execute(addiRecord, null), isThrowing(IllegalArgumentException.class));
    }

    @Test
    public void execute_trackingIdIsNull_returnsUpdatedMetadataWithoutTrackingId() throws IOException, SAXException {
        final AddiRecord addiRecord = toAddiRecord(getValidAddiWithoutProcessing());
        final AddiRecord preprocessed = preprocessor.execute(addiRecord, null);
        assertThat("AddiRecord.metadata is changed", preprocessed.getMetaData(), not(addiRecord.getMetaData()));
        assertThat("AddiRecord.metadata has trackingId attribute", getTrackingId(preprocessed), is(nullValue()));
    }

    private boolean hasSinkProcessingElement(AddiRecord addiRecord) {
        try {
            return addiContext.getEsReferenceData(addiRecord).sinkDirectives != null;
        } catch (AddiException e) {
            throw new IllegalStateException(e);
        }
    }

    private String getTrackingId(AddiRecord addiRecord) {
        try {
            final EsReferenceData esReferenceData = addiContext.getEsReferenceData(addiRecord);
            if (esReferenceData.esDirectives != null) {
                return esReferenceData.esDirectives.trackingId;
            }
            return null;
        } catch (AddiException e) {
            throw new IllegalStateException(e);
        }
    }

    private byte[] to2709(byte[] bytes) {
        try {
            return Iso2709Packer.create2709FromMarcXChangeRecord(JaxpUtil.toDocument(bytes), new DanMarc2Charset());
        } catch (IOException | SAXException e) {
            throw new IllegalStateException(e);
        }
    }

    public static AddiRecord toAddiRecord(byte[] data) {
        final AddiReader addiReader = new AddiReader(new ByteArrayInputStream(data));
        try {
            return addiReader.getNextRecord();
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static byte[] getValidAddiWithoutProcessing() {
        return ("131\n" +
                "<es:referencedata xmlns:es=\"http://oss.dbc.dk/ns/es\">" +
                "<es:info format=\"basis\" language=\"dan\" submitter=\"870970\"/>" +
                "</es:referencedata>\n1\nb\n").getBytes(StandardCharsets.UTF_8);
    }

    public static byte[] getValidAddiWithProcessingFalse() {
        return ("236\n" +
                "<es:referencedata xmlns:es=\"http://oss.dbc.dk/ns/es\">" +
                "<es:info format=\"basis\" language=\"dan\" submitter=\"870970\"/>" +
                "<dataio:sink-processing xmlns:dataio=\"dk.dbc.dataio.processing\" encodeAs2709=\"false\" charset=\"danmarc2\"/>" +
                "</es:referencedata>\n1\nb\n").getBytes(StandardCharsets.UTF_8);
    }

    public static byte[] getValidAddiWithProcessingTrueAndValidMarcXContentData() {
        return ("235\n" +
                "<es:referencedata xmlns:es=\"http://oss.dbc.dk/ns/es\">" +
                "<es:info format=\"basis\" language=\"dan\" submitter=\"870970\"/>" +
                "<dataio:sink-processing xmlns:dataio=\"dk.dbc.dataio.processing\" encodeAs2709=\"true\" charset=\"danmarc2\"/>" +
                "</es:referencedata>" +
                "\n506\n" +
                "<marcx:record xmlns:marcx='info:lc/xmlns/marcxchange-v1'>" +
                "<marcx:leader>00000n    2200000   4500</marcx:leader>" +
                "<marcx:datafield tag='100' ind1='0' ind2='0'>" +
                "<marcx:subfield code='a'>field1</marcx:subfield>" +
                "<marcx:subfield code='b'/>" +
                "<marcx:subfield code='d'>Field2</marcx:subfield>" +
                "</marcx:datafield><marcx:datafield tag='101' ind1='1' ind2='2'>" +
                "<marcx:subfield code='h'>est</marcx:subfield>" +
                "<marcx:subfield code='k'>o</marcx:subfield>" +
                "<marcx:subfield code='G'>ris</marcx:subfield>" +
                "</marcx:datafield></marcx:record>\n").getBytes(StandardCharsets.UTF_8);
    }

    public static byte[] getValidAddiWithProcessingTrueAndInvalidMarcXContentData() {
        return ("235\n" +
                "<es:referencedata xmlns:es=\"http://oss.dbc.dk/ns/es\">" +
                "<es:info format=\"basis\" language=\"dan\" submitter=\"870970\"/>" +
                "<dataio:sink-processing xmlns:dataio=\"dk.dbc.dataio.processing\" encodeAs2709=\"true\" charset=\"danmarc2\"/>" +
                "</es:referencedata>" +
                "\n238\n" +
                "<marcx:collection xmlns:marcx=\"info:lc/xmlns/marcxchange-v1\">" +
                "<marcx:record format=\"danMARC2\"><marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"245\">" +
                "<marcx:subfield code=\"a\">title1</marcx:subfield></marcx:datafield>" +
                "</marcx:record>" +
                "</marcx:collection>" +
                "\n").getBytes(StandardCharsets.UTF_8);
    }

}
