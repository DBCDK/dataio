package dk.dbc.dataio.sink.openupdate;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.utils.lang.ResourceReader;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.commons.utils.test.model.ChunkItemBuilder;
import dk.dbc.dataio.sink.openupdate.connector.OpenUpdateServiceConnector;
import dk.dbc.oss.ns.catalogingupdate.BibliographicRecord;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.ws.WebServiceException;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.metrics.SimpleTimer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashSet;

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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@WireMockTest
public class ChunkItemProcessorTest extends AbstractOpenUpdateSinkTestBase {
    private final AddiRecordPreprocessor addiRecordPreprocessor = new AddiRecordPreprocessor();
    private final OpenUpdateServiceConnector mockedOpenUpdateServiceConnector = mock(OpenUpdateServiceConnector.class);
    private static final String WIREDENDPOINTURL = "/UpdateService/2.0";
    private OpenUpdateServiceConnector wiredOpenUpdateServiceConnector;
    private final UpdateRecordResultMarshaller updateRecordResultMarshaller = new UpdateRecordResultMarshaller();
    private final UpdateRecordErrorInterpreter updateRecordErrorInterpreter = new UpdateRecordErrorInterpreter();
    private final String submitter = "870970";
    private final String updateTemplate = "bog";
    private final String queueProvider = "queue";
    private final SimpleTimer mockedTimer = mock(SimpleTimer.class);

    private final AddiRecord addiRecord = newAddiRecord(
            getMetaXml(updateTemplate, submitter),
            StringUtil.asString(readTestRecord(MARC_EXCHANGE_WEBSERVICE_OK), StandardCharsets.UTF_8));

    private final ChunkItem chunkItemWithMultipleAddiRecords = buildChunkItemWithMultipleValidAddiRecords(addiRecord);

    @BeforeEach
    public void setupMocks(WireMockRuntimeInfo wireMockRuntimeInfo) {
        wiredOpenUpdateServiceConnector = new OpenUpdateServiceConnector(wireMockRuntimeInfo.getHttpBaseUrl() +  WIREDENDPOINTURL);
        doNothing().when(mockedTimer).update(any(Duration.class));
    }

    @Test
    public void processForQueueProvider_OK() throws JAXBException {
        // Expectations
        when(mockedOpenUpdateServiceConnector.updateRecord(anyString(), anyString(), any(BibliographicRecord.class), anyString()))
                .thenReturn(getWebserviceResultValidatedOk());

        // Subject Under Test
        ChunkItem chunkItemForDelivery = newChunkItemProcessor().processForQueueProvider(queueProvider);

        // Verification
        assertNotNull(chunkItemForDelivery);
        String chunkItemDataAsString = asString(chunkItemForDelivery.getData());
        assertEquals(ChunkItem.Status.SUCCESS, chunkItemForDelivery.getStatus(), "Expected status OK");
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
        ChunkItem chunkItemForDelivery = newChunkItemProcessor().processForQueueProvider(queueProvider);

        // Verification
        assertNotNull(chunkItemForDelivery);
        String chunkItemDataAsString = asString(chunkItemForDelivery.getData());
        assertEquals(ChunkItem.Status.FAILURE, chunkItemForDelivery.getStatus(), "Expected status FAILURE");
        assertTrue(chunkItemDataAsString.contains("message"));
        assertTrue(chunkItemDataAsString.contains("e01 00 *a"));
        assertEquals(3, StringUtils.countMatches(chunkItemDataAsString, "<message>"));
        assertThat(chunkItemForDelivery.getDiagnostics(), is(notNullValue()));
        assertThat(chunkItemForDelivery.getDiagnostics().size(), is(3));
        assertThat(chunkItemForDelivery.getTrackingId(), is(DBC_TRACKING_ID));
    }

    @Test
    public void processForQueueProvider_stackTrace() {
        // Expectations
        when(mockedOpenUpdateServiceConnector.updateRecord(anyString(), anyString(), any(BibliographicRecord.class), anyString()))
                .thenThrow(new WebServiceException());

        // Subject Under Test
        ChunkItem chunkItemForDelivery = newChunkItemProcessor().processForQueueProvider(queueProvider);

        // Verification
        testChunkItemForDelivery(chunkItemForDelivery);
    }

    @Test
    public void processForQueueProvider_chunkItemContainsMultipleAddiRecords_OK() throws JAXBException {
        // Expectations
        when(mockedOpenUpdateServiceConnector.updateRecord(anyString(), anyString(), any(BibliographicRecord.class), anyString()))
                .thenReturn(getWebserviceResultValidatedOk());

        // Subject Under Test
        ChunkItem chunkItemForDelivery = newChunkItemProcessor().processForQueueProvider(queueProvider);

        // Verification
        assertNotNull(chunkItemForDelivery);
        String chunkItemDataAsString = asString(chunkItemForDelivery.getData());
        assertEquals(ChunkItem.Status.SUCCESS, chunkItemForDelivery.getStatus(), "Expected status OK");
        assertFalse(chunkItemDataAsString.contains("errorMessages"));
        assertTrue(chunkItemDataAsString.contains("OK"));
        assertFalse(chunkItemDataAsString.contains("e01 00 *a"));
        assertThat(chunkItemForDelivery.getDiagnostics(), is(nullValue()));
        verify(mockedOpenUpdateServiceConnector, times(3)).updateRecord(anyString(), anyString(), any(BibliographicRecord.class), anyString());
        assertThat(chunkItemForDelivery.getTrackingId(), is(DBC_TRACKING_ID));
    }

    @Test
    public void processForQueueProvider_emptyDiagnosticsReturnsChunkItemWithStatusSuccess() throws JAXBException {
        byte[] updateRecordResponse = (
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
        ChunkItem chunkItem = newChunkItemProcessor().processForQueueProvider(queueProvider);

        assertThat("ChunkItem status", chunkItem.getStatus(), is(ChunkItem.Status.SUCCESS));
    }

    @Test
    public void processForQueueProvider_http_error_404() {
        stubFor(post(urlEqualTo(WIREDENDPOINTURL)).willReturn(aResponse().withStatus(404)));

        ChunkItemProcessor chunkItemProcessor = newWiredChunkItemProcessor();
        ChunkItem chunkItemForDelivery = chunkItemProcessor.processForQueueProvider(queueProvider);
        testChunkItemForDelivery(chunkItemForDelivery);
    }

    @Test
    public void processForQueueProvider_http_error_502() {
        stubFor(post(urlEqualTo(WIREDENDPOINTURL)).willReturn(aResponse().withStatus(502)));

        ChunkItemProcessor chunkItemProcessor = newWiredChunkItemProcessor();
        ChunkItem chunkItemForDelivery = chunkItemProcessor.processForQueueProvider(queueProvider);
        testChunkItemForDelivery(chunkItemForDelivery);
    }

    @Test
    public void processForQueueProvider_http_error_503() {
        stubFor(post(urlEqualTo(WIREDENDPOINTURL)).willReturn(aResponse().withStatus(503)));

        ChunkItemProcessor chunkItemProcessor = newWiredChunkItemProcessor();
        ChunkItem chunkItemForDelivery = chunkItemProcessor.processForQueueProvider(queueProvider);
        testChunkItemForDelivery(chunkItemForDelivery);
    }

    @Test
    public void processForQueueProvider_OK_after_http_error_503() {
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
        for (int i = 0; i < retries; i++) {
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
        ChunkItem chunkItemForDelivery = chunkItemProcessor.processForQueueProvider(queueProvider);
        assertNotNull(chunkItemForDelivery);
        String chunkItemDataAsString = asString(chunkItemForDelivery.getData());
        assertEquals(ChunkItem.Status.SUCCESS, chunkItemForDelivery.getStatus(), "Expected status OK");
        assertFalse(chunkItemDataAsString.contains("errorMessages"));
        assertTrue(chunkItemDataAsString.contains("OK"));
        assertFalse(chunkItemDataAsString.contains("e01 00 *a"));
        assertThat(chunkItemForDelivery.getDiagnostics(), is(nullValue()));
        assertThat(chunkItemForDelivery.getTrackingId(), is(DBC_TRACKING_ID));
    }

    @Test
    public void processForQueueProvider_successWhenAllValidationErrorsAreIgnorable() {
        byte[] failedBody = ResourceReader.getResourceAsByteArray(
                ChunkItemProcessorTest.class, "UpdateService-2.0-response_FAILED.xml");

        stubFor(post(urlEqualTo(WIREDENDPOINTURL)).willReturn(aResponse().withStatus(200).withBody(failedBody)));

        HashSet<String> ignoredValidationErrors = new HashSet<>();
        ignoredValidationErrors.add("er ikke et gyldigt faustnummer");
        UpdateRecordErrorInterpreter updateRecordErrorInterpreter =
                new UpdateRecordErrorInterpreter(ignoredValidationErrors);

        ChunkItemProcessor chunkItemProcessor = newWiredChunkItemProcessor(updateRecordErrorInterpreter);
        ChunkItem result = chunkItemProcessor.processForQueueProvider(queueProvider);

        assertThat(result.getStatus(), is(SUCCESS));
    }

    private ChunkItemProcessor newChunkItemProcessor() {
        return new ChunkItemProcessor(
                chunkItemWithMultipleAddiRecords,
                addiRecordPreprocessor,
                mockedOpenUpdateServiceConnector,
                updateRecordResultMarshaller,
                updateRecordErrorInterpreter);
    }

    private ChunkItemProcessor newWiredChunkItemProcessor() {
        return newWiredChunkItemProcessor(updateRecordErrorInterpreter);
    }

    private ChunkItemProcessor newWiredChunkItemProcessor(UpdateRecordErrorInterpreter updateRecordErrorInterpreter) {
        ChunkItemProcessor chunkItemProcessor =
                new ChunkItemProcessor(
                        chunkItemWithMultipleAddiRecords,
                        addiRecordPreprocessor,
                        wiredOpenUpdateServiceConnector,
                        updateRecordResultMarshaller,
                        updateRecordErrorInterpreter);
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
        assertEquals(ChunkItem.Status.FAILURE, chunkItemForDelivery.getStatus(), "Expected status FAILURE");
        assertTrue(chunkItemDataAsString.contains("FAILED_STACKTRACE"));
        assertFalse(chunkItemDataAsString.contains("e01 00 *a"));
        assertThat(chunkItemForDelivery.getDiagnostics().size(), is(1));
        assertThat(chunkItemForDelivery.getTrackingId(), is(DBC_TRACKING_ID));
    }
}
