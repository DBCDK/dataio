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
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.oss.ns.catalogingupdate.UpdateRecordResponse;
import dk.dbc.oss.ns.catalogingupdate.UpdateRecordResult;
import org.xmlunit.matchers.CompareMatcher;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;


public class AbstractOpenUpdateSinkTestBase {
    public static final String UPDATE_RECORD_RESULT_WITH_VALIDATION_ERROR = "/updateRecordResult.validationError.xml";
    public static final String UPDATE_RECORD_RESULT_OK = "/updateRecordResult.ok.xml";
    public static final String MARC_EXCHANGE_WEBSERVICE_FAIL = "/870970.fail.xml";
    public static final String MARC_EXCHANGE_WEBSERVICE_OK = "/870970.ok.xml";

    public static final String DBC_TRACKING_ID = "wiremock-test";

    protected static CompareMatcher isEquivalentTo(Object control) {
        return CompareMatcher.isSimilarTo(control)
                .throwComparisonFailure()
                .normalizeWhitespace()
                .ignoreComments();
    }

    protected String getMetaXml(String template, String submitter) {
        return "<es:referencedata xmlns:es=\"http://oss.dbc.dk/ns/es\">" +
                "<es:info format=\"basis\" language=\"dan\" DBCTrackingId=\"" + DBC_TRACKING_ID + "\" submitter=\"" + submitter + "\"/>" +
                "<dataio:sink-update-template xmlns:dataio=\"dk.dbc.dataio.processing\" updateTemplate=\"" + template + "\"/>" +
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

    protected AddiRecord newAddiRecord(String meta, String content) {
        return new AddiRecord(
                meta.trim().getBytes(StandardCharsets.UTF_8),
                content.trim().getBytes(StandardCharsets.UTF_8));
    }

    protected byte[] addiToBytes(AddiRecord... addiRecords) {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            for (AddiRecord addiRecord : addiRecords) {
                baos.write(addiRecord.getBytes());
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return baos.toByteArray();
    }

    protected UpdateRecordResult getWebserviceResultValidatedOk() throws JAXBException {
        return unmarshalUpdateRecordResponse(readTestRecord(UPDATE_RECORD_RESULT_OK)).getUpdateRecordResult();
    }

    protected UpdateRecordResult getWebserviceResultWithValidationErrors() throws JAXBException {
        return unmarshalUpdateRecordResponse(readTestRecord(UPDATE_RECORD_RESULT_WITH_VALIDATION_ERROR)).getUpdateRecordResult();
    }

    protected byte[] readTestRecord(String resourceName) {
        try {
            final URL url = AbstractOpenUpdateSinkTestBase.class.getResource(resourceName);
            return Files.readAllBytes(Paths.get(url.toURI()));
        } catch (IOException | URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    protected static UpdateRecordResponse unmarshalUpdateRecordResponse(byte[] xmlResponseToUnmarshal) throws JAXBException {
        final Unmarshaller unmarshaller = JAXBContext.newInstance(UpdateRecordResponse.class).createUnmarshaller();
        final StringReader reader = new StringReader(StringUtil.asString(xmlResponseToUnmarshal));
        return (UpdateRecordResponse) unmarshaller.unmarshal(reader);
    }
}
