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

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.utils.lang.ResourceReader;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.commons.utils.test.model.ChunkItemBuilder;
import dk.dbc.dataio.sink.openupdate.connector.OpenUpdateServiceConnector;
import dk.dbc.oss.ns.catalogingupdate.BibliographicRecord;
import org.apache.commons.lang.StringUtils;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.Meter;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.Tag;
import org.eclipse.microprofile.metrics.Timer;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import javax.xml.bind.JAXBException;
import javax.xml.ws.WebServiceException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static dk.dbc.dataio.commons.types.ChunkItem.Status.SUCCESS;
import static dk.dbc.dataio.commons.utils.lang.StringUtil.asString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ChunkItemProcessorTest extends AbstractOpenUpdateSinkTestBase {
    private AddiRecordPreprocessor addiRecordPreprocessor = new AddiRecordPreprocessor();
    private OpenUpdateServiceConnector mockedOpenUpdateServiceConnector = mock(OpenUpdateServiceConnector.class);
    private static final String WIREDENDPOINTURL = "/UpdateService/2.0";
    private OpenUpdateServiceConnector wiredOpenUpdateServiceConnector =
            new OpenUpdateServiceConnector(String.format(
                    "http://localhost:%s%s", WIREMOCK_PORT, WIREDENDPOINTURL), "", "");
    private final UpdateRecordResultMarshaller updateRecordResultMarshaller = new UpdateRecordResultMarshaller();
    private final String submitter = "870970";
    private final String updateTemplate = "bog";
    private final String queueProvider = "queue";

    private MetricRegistry mockedMetricRegistry = mock(MetricRegistry.class);
    private final Meter mockedMeter = mock(Meter.class);
    private final Timer mockedTimer = mock(Timer.class);

    private final AddiRecord addiRecord = newAddiRecord(
            getMetaXml(updateTemplate, submitter),
            StringUtil.asString(readTestRecord(MARC_EXCHANGE_WEBSERVICE_OK), StandardCharsets.UTF_8));

    private final ChunkItem chunkItem = new ChunkItemBuilder()
            .setData(addiRecord.getBytes())
            .setStatus(SUCCESS).build();

    private static final String WIREMOCK_PORT = getWiremockPort();
    // needed for intellij:
    private static String getWiremockPort() {
        final String defaultPort = "8998";
        String wiremockPort = System.getProperty("wiremock.port", defaultPort);
        if(wiremockPort == null || wiremockPort.equals("")) {
            wiremockPort = defaultPort;
        }
        return wiremockPort;
    }
    @Rule
    public WireMockRule wireMockRule = new WireMockRule(Integer.valueOf(WIREMOCK_PORT));

    private final ChunkItem chunkItemWithMultipleAddiRecords = buildChunkItemWithMultipleValidAddiRecords(addiRecord);

    @Before
    public void setupMocks() {
        when(mockedMetricRegistry.meter(any(Metadata.class), any(Tag.class), any(Tag.class))).thenReturn(mockedMeter);
        doNothing().when(mockedMeter).mark();
        when(mockedMetricRegistry.timer(any(Metadata.class), any(Tag.class), any(Tag.class))).thenReturn(mockedTimer);
        doNothing().when(mockedTimer).update(anyLong(), any(TimeUnit.class));
    }

    @Test(expected = NullPointerException.class)
    public void constructor_addiRecordsForItemArgIsNull_throws() {
        new ChunkItemProcessor(null, addiRecordPreprocessor, mockedOpenUpdateServiceConnector, updateRecordResultMarshaller, mockedMetricRegistry);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_addiRecordPreprocessorArgIsNull_throws() {
        new ChunkItemProcessor(chunkItem, null, mockedOpenUpdateServiceConnector, updateRecordResultMarshaller, mockedMetricRegistry);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_openUpdateServiceConnectorArgIsNull_throws() {
        new ChunkItemProcessor(chunkItem, addiRecordPreprocessor, null, updateRecordResultMarshaller, mockedMetricRegistry);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_updateRecordResultMarshallerArgIsNull_throws() {
        new ChunkItemProcessor(chunkItem, addiRecordPreprocessor, mockedOpenUpdateServiceConnector, null, mockedMetricRegistry);
    }

    @Test
    public void processForQueueProvider_OK() throws JAXBException {
        // Expectations
        when(mockedOpenUpdateServiceConnector.updateRecord(anyString(), anyString(), any(BibliographicRecord.class), anyString()))
                .thenReturn(getWebserviceResultValidatedOk());

        // Subject Under Test
        final ChunkItem chunkItemForDelivery = newChunkItemProcessor().processForQueueProvider(queueProvider);

        // Verification
        assertNotNull(chunkItemForDelivery);
        String chunkItemDataAsString = asString(chunkItemForDelivery.getData());
        assertEquals("Expected status OK", chunkItemForDelivery.getStatus(), ChunkItem.Status.SUCCESS);
        assertFalse(chunkItemDataAsString.contains("errorMessages"));
        assertFalse(chunkItemDataAsString.contains("e01 00 *a"));
        assertTrue(asString(chunkItemForDelivery.getData()).contains("OK"));
        assertThat(chunkItemForDelivery.getDiagnostics(), is(nullValue()));
        assertThat(chunkItemForDelivery.getTrackingId(), is(DBC_TRACKING_ID));
    }

    @Test
    public void processForQueueProvider_validationError() throws JAXBException {
        // Expectations
        when(mockedOpenUpdateServiceConnector.updateRecord(anyString(), anyString(), any(BibliographicRecord.class), anyString()))
                .thenReturn(getWebserviceResultWithValidationErrors());

        // Subject Under Test
        final ChunkItem chunkItemForDelivery = newChunkItemProcessor().processForQueueProvider(queueProvider);

        // Verification
        assertNotNull(chunkItemForDelivery);
        String chunkItemDataAsString = asString(chunkItemForDelivery.getData());
        assertEquals("Expected status FAILURE", chunkItemForDelivery.getStatus(), ChunkItem.Status.FAILURE);
        assertTrue(chunkItemDataAsString.contains("message"));
        assertTrue(chunkItemDataAsString.contains("e01 00 *a"));
        assertTrue(StringUtils.countMatches(chunkItemDataAsString, "<message>") == 3);
        assertThat(chunkItemForDelivery.getDiagnostics(), is(notNullValue()));
        assertThat(chunkItemForDelivery.getDiagnostics().size(), is(3));
        assertThat(chunkItemForDelivery.getTrackingId(), is(DBC_TRACKING_ID));
    }

    @Test
    public void processForQueueProvider_stackTrace() throws JAXBException {
        // Expectations
        when(mockedOpenUpdateServiceConnector.updateRecord(anyString(), anyString(), any(BibliographicRecord.class), anyString()))
                .thenThrow(new WebServiceException());

        // Subject Under Test
        final ChunkItem chunkItemForDelivery = newChunkItemProcessor().processForQueueProvider(queueProvider);

        // Verification
        testChunkItemForDelivery(chunkItemForDelivery);
    }

    @Test
    public void processForQueueProvider_chunkItemContainsMultipleAddiRecords_OK() throws JAXBException {
        // Expectations
        when(mockedOpenUpdateServiceConnector.updateRecord(anyString(), anyString(), any(BibliographicRecord.class), anyString()))
                .thenReturn(getWebserviceResultValidatedOk());

        // Subject Under Test
        final ChunkItem chunkItemForDelivery = newChunkItemProcessor().processForQueueProvider(queueProvider);

        // Verification
        assertNotNull(chunkItemForDelivery);
        String chunkItemDataAsString = asString(chunkItemForDelivery.getData());
        assertEquals("Expected status OK", chunkItemForDelivery.getStatus(), ChunkItem.Status.SUCCESS);
        assertFalse(chunkItemDataAsString.contains("errorMessages"));
        assertTrue(chunkItemDataAsString.contains("OK"));
        assertFalse(chunkItemDataAsString.contains("e01 00 *a"));
        assertThat(chunkItemForDelivery.getDiagnostics(), is(nullValue()));
        verify(mockedOpenUpdateServiceConnector, times(3)).updateRecord(anyString(), anyString(), any(BibliographicRecord.class), anyString());
        assertThat(chunkItemForDelivery.getTrackingId(), is(DBC_TRACKING_ID));
    }

    @Test
    public void processForQueueProvider_emptyDiagnosticsReturnsChunkItemWithStatusSuccess() throws JAXBException {
        final byte[] updateRecordResponse = (
          "<updateRecordResponse xmlns=\"http://oss.dbc.dk/ns/catalogingUpdate\">" +
            "<updateRecordResult>" +
              "<updateStatus>failed_update_internal_error</updateStatus>" +
              "<validateInstance>" +
                "<validateEntry>" +
                  "<warningOrError>error</warningOrError>" +
                  "<message>Posten kan ikke slettes, da den ikke findes</message>" +
                "</validateEntry>" +
              "</validateInstance>" +
            "</updateRecordResult>" +
          "</updateRecordResponse>").getBytes(StandardCharsets.UTF_8);

        // Expectations
        when(mockedOpenUpdateServiceConnector.updateRecord(anyString(), anyString(), any(BibliographicRecord.class), anyString()))
                .thenReturn(unmarshalUpdateRecordResponse(updateRecordResponse).getUpdateRecordResult());

        // subject under test
        final ChunkItem chunkItem = newChunkItemProcessor().processForQueueProvider(queueProvider);

        assertThat("ChunkItem status", chunkItem.getStatus(), is(ChunkItem.Status.SUCCESS));
    }

    @Test
    public void processForQueueProvider_http_error_404() throws JAXBException {
        stubFor(post(urlEqualTo(WIREDENDPOINTURL)).willReturn(aResponse().withStatus(404)));

        ChunkItemProcessor chunkItemProcessor = newWiredChunkItemProcessor();
        final ChunkItem chunkItemForDelivery = chunkItemProcessor.processForQueueProvider(queueProvider);
        testChunkItemForDelivery(chunkItemForDelivery);
    }

    @Test
    public void processForQueueProvider_http_error_502() throws JAXBException {
        stubFor(post(urlEqualTo(WIREDENDPOINTURL)).willReturn(aResponse().withStatus(502)));

        ChunkItemProcessor chunkItemProcessor = newWiredChunkItemProcessor();
        final ChunkItem chunkItemForDelivery = chunkItemProcessor.processForQueueProvider(queueProvider);
        testChunkItemForDelivery(chunkItemForDelivery);
    }

    @Test
    public void processForQueueProvider_http_error_503() throws JAXBException {
        stubFor(post(urlEqualTo(WIREDENDPOINTURL)).willReturn(aResponse().withStatus(503)));

        ChunkItemProcessor chunkItemProcessor = newWiredChunkItemProcessor();
        final ChunkItem chunkItemForDelivery = chunkItemProcessor.processForQueueProvider(queueProvider);
        testChunkItemForDelivery(chunkItemForDelivery);
    }

    @Test
    public void processForQueueProvider_OK_after_http_error_503() throws JAXBException {
        String scenarioName = "OK after 503";
        String currentState = "call";
        byte[] okBody = ResourceReader.getResourceAsByteArray(
                ChunkItemProcessorTest.class, "UpdateService-2.0-response_OK.xml");
        int retries = 5;
        stubFor(post(urlEqualTo(WIREDENDPOINTURL))
                .inScenario(scenarioName)
                .whenScenarioStateIs(STARTED)
                .willReturn(aResponse()
                        .withStatus(503))
                .willSetStateTo(currentState));
        for(int i = 0; i < retries; i++) {
            // it doesn't matter what the string is, it should just differ from the current one
            String nextState = currentState + i;
            stubFor(post(urlEqualTo(WIREDENDPOINTURL))
                    .inScenario(scenarioName)
                    .whenScenarioStateIs(currentState)
                    .willReturn(aResponse().withStatus(503))
                    .willSetStateTo(nextState));
            currentState = nextState;
        }
        stubFor(post(urlEqualTo(WIREDENDPOINTURL))
                .inScenario(scenarioName)
                .whenScenarioStateIs(currentState)
                .willReturn(aResponse().withStatus(200).withBody(okBody)));

        ChunkItemProcessor chunkItemProcessor = newWiredChunkItemProcessor();
        final ChunkItem chunkItemForDelivery = chunkItemProcessor.processForQueueProvider(queueProvider);
        assertNotNull(chunkItemForDelivery);
        String chunkItemDataAsString = asString(chunkItemForDelivery.getData());
        assertEquals("Expected status OK", chunkItemForDelivery.getStatus(), ChunkItem.Status.SUCCESS);
        assertFalse(chunkItemDataAsString.contains("errorMessages"));
        assertTrue(chunkItemDataAsString.contains("OK"));
        assertFalse(chunkItemDataAsString.contains("e01 00 *a"));
        assertThat(chunkItemForDelivery.getDiagnostics(), is(nullValue()));
        assertThat(chunkItemForDelivery.getTrackingId(), is(DBC_TRACKING_ID));
    }

    /*
     * Private methods
     */

    private ChunkItemProcessor newChunkItemProcessor() {
        return new ChunkItemProcessor(
                chunkItemWithMultipleAddiRecords,
                addiRecordPreprocessor,
                mockedOpenUpdateServiceConnector,
                updateRecordResultMarshaller,
                mockedMetricRegistry);
    }

    private ChunkItemProcessor newWiredChunkItemProcessor() {
        ChunkItemProcessor chunkItemProcessor =
                new ChunkItemProcessor(
                        chunkItemWithMultipleAddiRecords,
                        addiRecordPreprocessor,
                        wiredOpenUpdateServiceConnector,
                        updateRecordResultMarshaller,
                        mockedMetricRegistry);
        chunkItemProcessor.retrySleepMillis = 0;
        return chunkItemProcessor;
    }

    private ChunkItem buildChunkItemWithMultipleValidAddiRecords(AddiRecord addiRecord) {
        return new ChunkItemBuilder()
                .setData(addiToBytes(addiRecord, addiRecord, addiRecord))
                .setTrackingId(DBC_TRACKING_ID)
                .setStatus(SUCCESS)
                .build();
    }

    private void testChunkItemForDelivery(ChunkItem chunkItemForDelivery) {
        assertNotNull(chunkItemForDelivery);
        String chunkItemDataAsString = asString(chunkItemForDelivery.getData());
        assertEquals("Expected status FAILURE", chunkItemForDelivery.getStatus(), ChunkItem.Status.FAILURE);
        assertTrue(chunkItemDataAsString.contains("FAILED_STACKTRACE"));
        assertFalse(chunkItemDataAsString.contains("e01 00 *a"));
        assertThat(chunkItemForDelivery.getDiagnostics().size(), is(1));
        assertThat(chunkItemForDelivery.getTrackingId(), is(DBC_TRACKING_ID));
    }
}
