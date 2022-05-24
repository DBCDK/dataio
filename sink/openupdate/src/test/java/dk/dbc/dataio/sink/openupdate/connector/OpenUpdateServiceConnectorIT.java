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
import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.dataio.sink.openupdate.AbstractOpenUpdateSinkTestBase;
import dk.dbc.dataio.sink.openupdate.AddiRecordPreprocessor;
import dk.dbc.oss.ns.catalogingupdate.BibliographicRecord;
import dk.dbc.oss.ns.catalogingupdate.UpdateRecordResult;
import dk.dbc.oss.ns.catalogingupdate.UpdateStatusEnum;
import org.junit.Rule;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class OpenUpdateServiceConnectorIT extends AbstractOpenUpdateSinkTestBase {
    private static final String WIREMOCK_PORT = System.getProperty("wiremock.port", "8998");

    private final String groupId = "010100";
    private final String updateTemplate = "dbc";
    private final String queueProvider = "queue";

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(Integer.valueOf(WIREMOCK_PORT));

    private final String updateServiceEndpoint = "http://localhost:" + WIREMOCK_PORT + "/UpdateService/2.0";

    OpenUpdateServiceConnector openUpdateServiceConnector = new OpenUpdateServiceConnector(updateServiceEndpoint);

    /*
    // To enable debug on wiremock:
    @Before
    public void debugWireMock() {
        wireMockRule.addMockServiceRequestListener((request, response) -> {
            System.out.println("URL Requested => " + request.getAbsoluteUrl());
            System.out.println("Request Body => " + request.getBodyAsString());
            System.out.println("Request Headers => " + request.getAllHeaderKeys());
            System.out.println("Response Status => " + response.getStatus());
            System.out.println("Response Body => " + response.getBodyAsString());
        });
    }
    */

    @Test
    public void updateRecord_ok() {
        final UpdateRecordResult updateRecordResult = getUpdateRecordOkResult();

        // Verification
        assertThat("UpdateRecordResult", updateRecordResult, is(notNullValue()));
        assertThat("status", updateRecordResult.getUpdateStatus(), is(UpdateStatusEnum.OK));
        assertThat("no messages", updateRecordResult.getMessages(), is(nullValue()));
    }

    @Test
    public void updateRecord_fail() {
        final UpdateRecordResult updateRecordResult = getUpdateRecordFailedResult();

        // Verification
        assertThat("UpdateRecordResult", updateRecordResult, is(notNullValue()));
        assertThat("status", updateRecordResult.getUpdateStatus(), is(UpdateStatusEnum.FAILED));
        assertThat("messages", updateRecordResult.getMessages().getMessageEntry().size(), is(4));
    }

    void recordUpdateRecordRequests() {
        getUpdateRecordOkResult();
        getUpdateRecordFailedResult();
    }

    private UpdateRecordResult getUpdateRecordOkResult() {
        final AddiRecord addiRecord = new AddiRecord(
                getMetaXml(updateTemplate, groupId).getBytes(StandardCharsets.UTF_8),
                readTestRecord(MARC_EXCHANGE_WEBSERVICE_OK));
        final BibliographicRecord bibliographicRecord = getBibliographicRecord(queueProvider, addiRecord);
        return openUpdateServiceConnector.updateRecord(groupId, updateTemplate, bibliographicRecord, DBC_TRACKING_ID);
    }

    private UpdateRecordResult getUpdateRecordFailedResult() {
        final AddiRecord addiRecord = new AddiRecord(
                getMetaXml(updateTemplate, groupId).getBytes(StandardCharsets.UTF_8),
                readTestRecord(MARC_EXCHANGE_WEBSERVICE_FAIL));
        final BibliographicRecord bibliographicRecord = getBibliographicRecord(queueProvider, addiRecord);
        return openUpdateServiceConnector.updateRecord(groupId, updateTemplate, bibliographicRecord, DBC_TRACKING_ID);
    }

    private BibliographicRecord getBibliographicRecord(String queueProvider, AddiRecord addiRecord) {
        final AddiRecordPreprocessor.Result preprocessorResult = new AddiRecordPreprocessor().preprocess(addiRecord, queueProvider);
        return preprocessorResult.getBibliographicRecord();
    }
}
