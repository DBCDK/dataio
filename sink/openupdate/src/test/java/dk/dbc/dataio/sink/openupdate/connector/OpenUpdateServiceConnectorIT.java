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

package dk.dbc.dataio.sink.openupdate.connector;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import dk.dbc.commons.addi.AddiReader;
import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.dataio.sink.openupdate.AddiRecordPreprocessor;
import dk.dbc.dataio.sink.openupdate.AddiRecordPreprocessorTest;
import dk.dbc.oss.ns.catalogingupdate.BibliographicRecord;
import dk.dbc.oss.ns.catalogingupdate.CatalogingUpdateServices;
import dk.dbc.oss.ns.catalogingupdate.UpdateRecordResult;
import dk.dbc.oss.ns.catalogingupdate.UpdateStatusEnum;
import dk.dbc.oss.ns.catalogingupdate.ValidateEntry;
import dk.dbc.oss.ns.catalogingupdate.ValidateInstance;
import dk.dbc.oss.ns.catalogingupdate.ValidateWarningOrErrorEnum;
import org.junit.Rule;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.fail;

public class OpenUpdateServiceConnectorIT {
    private static final String GROUP_ID = "010100";
    private static final String INVALID_SCHEMA =  "fisk";
    private static final String expectedTrackingId = "efd0db60-87db-40c8-8b0f-2c164ce49dfc";
    private static final String SCHEMA_NAME = "dbc";
    private static final String FAILED_UPDATE_MARC = "/870970.failedUpdateInternalError.xml";
    private static final String VALIDATION_ERROR_MARC = "/820040.validationError.xml";
    private static final String VALIDATION_OK_MARC = "/870970.ok.xml";
    private static final String SYSTEM_PROPERTY = System.getProperty("wiremock.port", "8998");

    private final String wireMockEndpoint = "http://localhost:" + SYSTEM_PROPERTY + "/CatalogingUpdateServices/UpdateService";
    /*
    Remember to download wiremock jar file from this location:
    http://repo1.maven.org/maven2/com/github/tomakehurst/wiremock/1.57/wiremock-1.57-standalone.jar

    To record test run this in a command line:
    java -jar wiremock-1.57-standalone.jar --port 9999 --proxy-all="http://fbstest-i01:2080/" --record-mappings —verbose
    OBS use beneath wireMockEndpoint when recording instead of above!
    port 9999 for recording
    port 8998 for testing
    */
//    private String wireMockEndpoint = "http://localhost:" + System.getProperty("wiremock.port", "9999") +"/CatalogingUpdateServices/UpdateService";


    @Rule
    public WireMockRule wireMockRule = new WireMockRule(Integer.valueOf(SYSTEM_PROPERTY));

    @Test
    public void constructor1arg_endpointArgIsNull_throws() {
        try {
            new OpenUpdateServiceConnector(null);
            fail("No Exception thrown");
        } catch (NullPointerException e) { }
    }

    @Test
    public void constructor1arg_endpointArgIsEmpty_throws() {
        try {
            new OpenUpdateServiceConnector("");
            fail("No Exception thrown");
        } catch (IllegalArgumentException e) { }
    }

    @Test
    public void constructor2arg_servicesArgIsNull_throws() {
        try {
            new OpenUpdateServiceConnector(null, wireMockEndpoint);
            fail("No Exception thrown");
        } catch (NullPointerException e) { }
    }

    @Test
    public void constructor2arg_endpointArgIsNull_throws() {
        try {
            new OpenUpdateServiceConnector(new CatalogingUpdateServices(), null);
            fail("No Exception thrown");
        } catch (NullPointerException e) { }
    }

    @Test
    public void constructor2arg_endpointArgIsEmpty_throws() {
        try {
            new OpenUpdateServiceConnector(new CatalogingUpdateServices(), "");
            fail("No Exception thrown");
        } catch (IllegalArgumentException e) { }
    }

    @Test
    public void updateRecord_groupIdArgIsNull_throws() {
        final OpenUpdateServiceConnector connector = getConnector();
        try {
                callUpdateRecordOnConnector(connector, null, SCHEMA_NAME, new BibliographicRecord());
            fail("No Exception thrown");
        } catch (NullPointerException e) { }
    }

    @Test
    public void updateRecord_groupIdArgIsEmpty_throws() {
        final OpenUpdateServiceConnector connector = getConnector();
        try {
            callUpdateRecordOnConnector(connector, "", SCHEMA_NAME, new BibliographicRecord());
            fail("No Exception thrown");
        } catch (IllegalArgumentException e) { }
    }

    @Test
    public void updateRecord_schemaNameArgIsNull_throws() {
        final OpenUpdateServiceConnector connector = getConnector();
        try {
                callUpdateRecordOnConnector(connector, GROUP_ID, null, new BibliographicRecord());
            fail("No Exception thrown");
        } catch (NullPointerException e) { }
    }

    @Test
    public void updateRecord_schemaNameArgIsEmpty_throws() {
        final OpenUpdateServiceConnector connector = getConnector();
        try {
            callUpdateRecordOnConnector(connector, GROUP_ID, "", new BibliographicRecord());
            fail("No Exception thrown");
        } catch (IllegalArgumentException e) { }
    }

    @Test
    public void updateRecord_bibliographicalRecordIsNull_throws() {
        final OpenUpdateServiceConnector connector = getConnector();
        try {
            callUpdateRecordOnConnector(connector, GROUP_ID, SCHEMA_NAME, null);
            fail("No Exception thrown");
        } catch (NullPointerException e) { }
    }

    @Test
    public void updateRecord_callsServiceWithInvalidSchema_validationFailedWithInvalidSchema() throws IOException, URISyntaxException {
        final AddiRecord addiRecord = toAddiRecord(getAddi(getMeta(INVALID_SCHEMA), readTestRecord(FAILED_UPDATE_MARC)));
        final AddiRecordPreprocessor.Result preprocessingResult = new AddiRecordPreprocessor().preprocess(addiRecord);
        final OpenUpdateServiceConnector connector = getConnector();

        // Subject under test
        final UpdateRecordResult updateRecordResult = callUpdateRecordOnConnector(connector,
                preprocessingResult.getSubmitter(), preprocessingResult.getTemplate(), preprocessingResult.getBibliographicRecord());

        // Verification
        assertThat("UpdateRecordResult", updateRecordResult, not(nullValue()));
        assertThat("UpdateStatus", updateRecordResult.getUpdateStatus(), is(UpdateStatusEnum.FAILED_INVALID_SCHEMA));
        assertThat("ValidateInstance is null", updateRecordResult.getValidateInstance(), is(nullValue()));
    }

    @Test
    public void updateRecord_callsServiceWithInvalidMarc_validationFailedWithValidationError() throws IOException, URISyntaxException {
        final AddiRecord addiRecord = toAddiRecord(getAddi(getMeta(SCHEMA_NAME), readTestRecord(VALIDATION_ERROR_MARC)));
        final AddiRecordPreprocessor.Result preprocessingResult = new AddiRecordPreprocessor().preprocess(addiRecord);
        final OpenUpdateServiceConnector connector = getConnector();

        // Subject under test
        final UpdateRecordResult updateRecordResult = callUpdateRecordOnConnector(connector,
                preprocessingResult.getSubmitter(), preprocessingResult.getTemplate(), preprocessingResult.getBibliographicRecord());

        // Verification
        assertThat("UpdateRecordResult not null", updateRecordResult, not(nullValue()));
        assertThat("UpdateStatus", updateRecordResult.getUpdateStatus(), is(UpdateStatusEnum.VALIDATION_ERROR));

        final ValidateInstance validateInstance = updateRecordResult.getValidateInstance();
        assertThat("ValidateInstance not null", validateInstance, not(nullValue()));
        assertThat("Validate entries not null ", validateInstance.getValidateEntry(), not(nullValue()));

        for(ValidateEntry validateEntry : validateInstance.getValidateEntry()) {
            assertThat("Message not null", validateEntry.getMessage(), not(nullValue()));
            assertThat("Url not null", validateEntry.getUrlForDocumentation(), not(nullValue()));
            assertThat("ValidateWarningOrErrorEnum", validateEntry.getWarningOrError(), is(ValidateWarningOrErrorEnum.ERROR));
        }
    }

    @Test
    public void updateRecord_callsServiceWithInsufficientRights_validationFailedWithFailedUpdateInternalError() throws IOException, URISyntaxException {
        final AddiRecord addiRecord = toAddiRecord(getAddi(getMeta(SCHEMA_NAME), readTestRecord(FAILED_UPDATE_MARC)));
        final AddiRecordPreprocessor.Result preprocessingResult = new AddiRecordPreprocessor().preprocess(addiRecord);
        final OpenUpdateServiceConnector connector = getConnector();

        // Subject under test
        final UpdateRecordResult updateRecordResult = callUpdateRecordOnConnector(connector,
                preprocessingResult.getSubmitter(), preprocessingResult.getTemplate(), preprocessingResult.getBibliographicRecord());

        // Verification
        assertThat("UpdateRecordResult", updateRecordResult, not(nullValue()));
        assertThat("UpdateStatus", updateRecordResult.getUpdateStatus(), is(UpdateStatusEnum.FAILED_UPDATE_INTERNAL_ERROR));

        final ValidateInstance validateInstance = updateRecordResult.getValidateInstance();
        assertThat("ValidateInstance not null", validateInstance, not(nullValue()));
        assertThat("Validate entries not null ", validateInstance.getValidateEntry(), not(nullValue()));
        assertThat("Number of validate entries: ", validateInstance.getValidateEntry().size(), is(1));

        final ValidateEntry validateEntry = validateInstance.getValidateEntry().get(0);
        assertThat("ValidateWarningOrErrorEnum", validateEntry.getWarningOrError(), is(ValidateWarningOrErrorEnum.ERROR));
        assertThat("Message not null", validateEntry.getMessage(), not(nullValue()));
    }

    @Test
    public void updateRecord_callsServiceWithAllArgsAreValid_validationOk() throws IOException, URISyntaxException {
        final AddiRecord addiRecord = toAddiRecord(getAddi(getMeta(SCHEMA_NAME), readTestRecord(VALIDATION_OK_MARC)));
        final AddiRecordPreprocessor.Result preprocessingResult = new AddiRecordPreprocessor().preprocess(addiRecord);
        final OpenUpdateServiceConnector connector = getConnector();

        // Subject under test
        final UpdateRecordResult updateRecordResult = callUpdateRecordOnConnector(connector,
                preprocessingResult.getSubmitter(), preprocessingResult.getTemplate(), preprocessingResult.getBibliographicRecord());

        // Verification
        assertThat("UpdateRecordResult", updateRecordResult, not(nullValue()));
        assertThat("UpdateStatus", updateRecordResult.getUpdateStatus(), is(UpdateStatusEnum.OK));
        assertThat("validateInstance is null", updateRecordResult.getValidateInstance(), is(nullValue()));
    }

    /*
     * Private methods
     */
    private UpdateRecordResult callUpdateRecordOnConnector(OpenUpdateServiceConnector connector, String groupId, String template, BibliographicRecord bibliographicRecord) throws NullPointerException, IllegalArgumentException {
        return connector.updateRecord(groupId, template, bibliographicRecord, UUID.fromString(expectedTrackingId));
    }

    private OpenUpdateServiceConnector getConnector() {
        return new OpenUpdateServiceConnector(new CatalogingUpdateServices(), wireMockEndpoint);
    }

    private AddiRecord toAddiRecord(byte[] data) {
        final AddiReader addiReader = new AddiReader(new ByteArrayInputStream(data));
        try {
            return addiReader.getNextRecord();
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public String getMeta(String nodeValue) {
        return "<es:referencedata xmlns:es=\"http://oss.dbc.dk/ns/es\">" +
                "<es:info format=\"basis\" language=\"dan\" submitter=\"010100\"/>" +
                "<dataio:sink-update-template xmlns:dataio=\"dk.dbc.dataio.processing\"" +
                " updateTemplate=\"" + nodeValue + "\" charset=\"danmarc2\"/>" +
                "</es:referencedata>";
    }

    private byte[] getAddi(String metaXml, byte[] contentXml) {
        return (metaXml.trim().getBytes().length +
                System.lineSeparator() +
                metaXml +
                System.lineSeparator() +
                contentXml.length +
                System.lineSeparator() +
                new String(contentXml, StandardCharsets.UTF_8)).getBytes();
    }

    private static byte[] readTestRecord(String resourceName) throws IOException, URISyntaxException {
        final URL url = AddiRecordPreprocessorTest.class.getResource(resourceName);
        final Path resPath;
        resPath = Paths.get(url.toURI());
        return Files.readAllBytes(resPath);
    }
}
