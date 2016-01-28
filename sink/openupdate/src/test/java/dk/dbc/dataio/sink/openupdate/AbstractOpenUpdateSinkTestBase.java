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
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.oss.ns.catalogingupdate.UpdateRecordResponse;
import dk.dbc.oss.ns.catalogingupdate.UpdateRecordResult;
import org.xmlunit.matchers.CompareMatcher;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;


public class AbstractOpenUpdateSinkTestBase {
    private static final String UPDATE_RECORD_RESULT_WITH_VALIDATION_ERROR = "/updateRecordResult.validationError.xml";
    private static final String UPDATE_RECORD_RESULT_OK = "/updateRecordResult.ok.xml";
    private static final String MARC_EXCHANGE_WEBSERVICE_OK = "/870970.ok.xml";
    private static final String MARC_EXCHANGE_WEBSERVICE_VALIDATION_ERRORS = "/820040.validationError.xml";
    public static final String ES_INFO_SUBMITTER_ATTRIBUTE_VALUE = "870970";
    public static final String UPDATE_TEMPLATE_ATTRIBUTE_VALUE = "bog";
    public static final String DBC_TRACKING_ID_VALUE = "dataio-tracking-id";

    protected static CompareMatcher isEquivalentTo(Object control) {
        return CompareMatcher.isSimilarTo(control)
                .throwComparisonFailure()
                .normalizeWhitespace()
                .ignoreComments();
    }

    protected String getMetaXml() {
        return "<es:referencedata xmlns:es=\"http://oss.dbc.dk/ns/es\">" +
                "<es:info format=\"basis\" language=\"dan\" DBCTrackingId=\"" + DBC_TRACKING_ID_VALUE + "\" submitter=\"" + ES_INFO_SUBMITTER_ATTRIBUTE_VALUE + "\"/>" +
                "<dataio:sink-update-template xmlns:dataio=\"dk.dbc.dataio.processing\" updateTemplate=\"" + UPDATE_TEMPLATE_ATTRIBUTE_VALUE + "\"/>" +
                "</es:referencedata>";
    }

    protected String getInvalidMetaXml(String referenceDataChildren) {
        return  "<es:referencedata xmlns:es=\"http://oss.dbc.dk/ns/es\">" +
                    referenceDataChildren +
                "</es:referencedata>";
    }

    protected String getContentXml() {
        return "<marcx:collection xmlns:marcx=\"info:lc/xmlns/marcxchange-v1\">" +
                "<marcx:record format=\"danMARC2\">" +
                "<marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"245\">" +
                "<marcx:subfield code=\"a\">field1</marcx:subfield>" +
                "</marcx:datafield></marcx:record></marcx:collection>";
    }

    protected String getSimpleMarcExchangeRecord() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n" +
                "    <marcx:record xmlns:marcx=\"info:lc/xmlns/marcxchange-v1\">\n" +
                "        <marcx:leader>00000cape 22000003 4500</marcx:leader>\n" +
                "        <marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"001\">\n" +
                "            <marcx:subfield code=\"a\">x7845232</marcx:subfield>\n" +
                "            <marcx:subfield code=\"d\">19900326</marcx:subfield>\n" +
                "            <marcx:subfield code=\"f\">a</marcx:subfield>\n" +
                "            <marcx:subfield code=\"o\">d</marcx:subfield>\n" +
                "        </marcx:datafield>\n" +
                "    </marcx:record>";
    }

    protected AddiRecord toAddiRecord(byte[] data) {
        final AddiReader addiReader = new AddiReader(new ByteArrayInputStream(data));
        try {
            return addiReader.getNextRecord();
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    protected byte[] getAddi(String metaXml, String contentXml) {
        return new AddiRecordWrapper(metaXml, contentXml).getAddiRecordAsString().getBytes();
    }
    protected byte[] getAddi(List<AddiRecordWrapper> addiRecords) {
        return addiRecords.stream().map(AddiRecordWrapper::getAddiRecordAsString)
                .collect(Collectors.joining(System.lineSeparator())).getBytes();
    }

    protected UpdateRecordResult getWebserviceResultValidatedOk() throws JAXBException {
        return unmarshalUpdateRecordResponse(readTestRecord(UPDATE_RECORD_RESULT_OK)).getUpdateRecordResult();
    }

    protected UpdateRecordResult getWebserviceResultWithValidationErrors() throws JAXBException {
        return unmarshalUpdateRecordResponse(readTestRecord(UPDATE_RECORD_RESULT_WITH_VALIDATION_ERROR)).getUpdateRecordResult();
    }

    protected String getMarcExchangeValidatedOkByWebservice() {
        return StringUtil.asString(readTestRecord(MARC_EXCHANGE_WEBSERVICE_OK), StandardCharsets.UTF_8);
    }

    protected String getMarcExchangeCausingWebserviceValidationErrors() {
        return StringUtil.asString(readTestRecord(MARC_EXCHANGE_WEBSERVICE_VALIDATION_ERRORS), StandardCharsets.UTF_8);
    }

    protected byte[] readTestRecord(String resourceName) {
        try {
            final URL url = AbstractOpenUpdateSinkTestBase.class.getResource(resourceName);
            final Path resPath;
            resPath = Paths.get(url.toURI());
            return Files.readAllBytes(resPath);
        } catch (IOException | URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    /*
     * Private methods
     */

    private static UpdateRecordResponse unmarshalUpdateRecordResponse(byte[] xmlResponseToUnmarshal) throws JAXBException {
        final Unmarshaller unmarshaller = JAXBContext.newInstance(UpdateRecordResponse.class).createUnmarshaller();
        StringReader reader = new StringReader(StringUtil.asString(xmlResponseToUnmarshal));
        return (UpdateRecordResponse) unmarshaller.unmarshal(reader);
    }

    /*
     * Protected classes
     */

    protected class AddiRecordWrapper {
        private String metaXml;
        private String contentXml;

        public AddiRecordWrapper(String metaXml, String contentXml) {
            this.metaXml = metaXml;
            this.contentXml = contentXml;
        }

        public String getMetaXml() {return this.metaXml;}
        public String getContentXml() {return this.contentXml;}

        private byte[] getMetaXmlAsBytes() {return this.metaXml.trim().getBytes();}
        private byte[] getContentXmlAsBytes() {return this.contentXml.trim().getBytes();}

        public String getAddiRecordAsString() {
            return this.getMetaXmlAsBytes().length
                    + System.lineSeparator()
                    + this.getMetaXml()
                    + System.lineSeparator()
                    + this.getContentXmlAsBytes().length
                    + System.lineSeparator()
                    + this.getContentXml();
        }
    }
}
