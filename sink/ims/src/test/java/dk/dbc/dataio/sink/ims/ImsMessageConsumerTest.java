package dk.dbc.dataio.sink.ims;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.ImsSinkConfig;
import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;
import dk.dbc.dataio.commons.types.jms.JMSHeader;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.jobstore.types.ItemDeliveryResult;
import dk.dbc.dataio.jobstore.types.Watermark;
import dk.dbc.dataio.jse.artemis.common.service.ServiceHub;
import dk.dbc.dataio.sink.ims.connector.ImsServiceConnector;
import dk.dbc.dataio.sink.ims.connector.ImsServiceConnectorTest;
import net.jodah.failsafe.RetryPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@WireMockTest
class ImsMessageConsumerTest {
    private static final int JOB_ID = 42;
    private static final int CHUNK_ID = 7;
    private static final short ITEM_ID = 3;
    private static final long SINK_ID = 15;
    private static final long SINK_VERSION = 1;
    private static final String RECORD_KEY = "870970:12345678";
    private static final String TRACKING_ID = "{12345678:870970}-42-7-3";

    private final JSONBContext jsonbContext = new JSONBContext();
    private final JobStoreServiceConnector jobStoreServiceConnector = mock(JobStoreServiceConnector.class);
    private final ImsConfig imsConfig = mock(ImsConfig.class);
    private final ImsMessageConsumer consumer = new ImsMessageConsumer(
            new ServiceHub.Builder().withJobStoreServiceConnector(jobStoreServiceConnector).test(), imsConfig) {
        @Override
        ImsServiceConnector createConnector(ImsSinkConfig imsSinkConfig) {
            // without this the unreachable-IMS test sits through the connector's minute of retries
            return super.createConnector(imsSinkConfig).withRetryPolicy(new RetryPolicy<>().withMaxRetries(0));
        }
    };

    @BeforeEach
    void setupMocks(WireMockRuntimeInfo wireMockRuntimeInfo) {
        when(imsConfig.getConfig(any(ConsumedMessage.class)))
                .thenReturn(new ImsSinkConfig().withEndpoint(wireMockRuntimeInfo.getHttpBaseUrl()));
    }

    @Test
    void successItem_isDelivered() {
        stubResponse(result("ok", "Request received!", String.valueOf(ITEM_ID)));

        ItemDeliveryResult result = consumer.deliverItem(itemMessage(), successItem());

        assertThat("verdict", result.status(), is(ItemDeliveryResult.Status.DELIVERED));
        assertThat("outcome status", result.chunkItem().getStatus(), is(ChunkItem.Status.SUCCESS));
        assertThat("outcome data", StringUtil.asString(result.chunkItem().getData()),
                is("[Status: ok], [Message: Request received!]"));
        assertThat("tracking id", result.chunkItem().getTrackingId(), is(TRACKING_ID));
    }

    /**
     * IGNORED rather than DELIVERED is what keeps an item this sink did not send counted as
     * ignored in the delivering phase, and leaves the record's delivery watermark where it was.
     */
    @Test
    void failureItem_isIgnored() {
        ItemDeliveryResult result = consumer.deliverItem(itemMessage(), item(ChunkItem.Status.FAILURE, "data"));

        assertThat("verdict", result.status(), is(ItemDeliveryResult.Status.IGNORED));
        assertThat("outcome status", result.chunkItem().getStatus(), is(ChunkItem.Status.IGNORE));
        assertThat("outcome data", StringUtil.asString(result.chunkItem().getData()), is("Failed by processor"));
        verify(0, postRequestedFor(urlPathMatching("/")));
    }

    @Test
    void ignoreItem_isIgnored() {
        ItemDeliveryResult result = consumer.deliverItem(itemMessage(), item(ChunkItem.Status.IGNORE, "data"));

        assertThat("verdict", result.status(), is(ItemDeliveryResult.Status.IGNORED));
        assertThat("outcome status", result.chunkItem().getStatus(), is(ChunkItem.Status.IGNORE));
        assertThat("outcome data", StringUtil.asString(result.chunkItem().getData()), is("Ignored by processor"));
        verify(0, postRequestedFor(urlPathMatching("/")));
    }

    @Test
    void invalidCollection_isFailed() {
        ItemDeliveryResult result = consumer.deliverItem(itemMessage(), item(ChunkItem.Status.SUCCESS, "not a collection"));

        assertThat("verdict", result.status(), is(ItemDeliveryResult.Status.FAILED));
        assertThat("outcome status", result.chunkItem().getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat("outcome data", StringUtil.asString(result.chunkItem().getData()),
                is("Error occurred while unmarshalling JAXBElement"));
        assertThat("diagnostics", result.chunkItem().getDiagnostics().size(), is(1));
        verify(0, postRequestedFor(urlPathMatching("/")));
    }

    @Test
    void rejectedByIms_isFailed() {
        stubResponse(result("update_failed_invalid_record", "Missing leader", String.valueOf(ITEM_ID)));

        ItemDeliveryResult result = consumer.deliverItem(itemMessage(), successItem());

        assertThat("verdict", result.status(), is(ItemDeliveryResult.Status.FAILED));
        assertThat("outcome status", result.chunkItem().getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat("outcome data", StringUtil.asString(result.chunkItem().getData()),
                is("[Status: update_failed_invalid_record], [Message: Missing leader]"));
        assertThat("diagnostics", result.chunkItem().getDiagnostics().size(), is(1));
    }

    @Test
    void nullRecordIdInResult_isFailed() {
        stubResponse(result("update_failed_invalid_record", "Missing leader", null));

        ItemDeliveryResult result = consumer.deliverItem(itemMessage(), successItem());

        assertThat("verdict", result.status(), is(ItemDeliveryResult.Status.FAILED));
        assertThat("outcome data", StringUtil.asString(result.chunkItem().getData()),
                is("Item failed due to webservice returning updateMarcXchangeResult with record id null."
                        + " -> [Status: update_failed_invalid_record], [Message: Missing leader]"));
    }

    @Test
    void noResults_isFailed() {
        stubResponse("");

        ItemDeliveryResult result = consumer.deliverItem(itemMessage(), successItem());

        assertThat("verdict", result.status(), is(ItemDeliveryResult.Status.FAILED));
        assertThat("outcome data", StringUtil.asString(result.chunkItem().getData()),
                is("Item failed due to webservice returning 0 updateMarcXchangeResults when 1 was expected."));
    }

    @Test
    void multipleResults_isFailed() {
        stubResponse(result("ok", "Request received!", String.valueOf(ITEM_ID))
                + result("ok", "Request received!", "9"));

        ItemDeliveryResult result = consumer.deliverItem(itemMessage(), successItem());

        assertThat("verdict", result.status(), is(ItemDeliveryResult.Status.FAILED));
        assertThat("outcome data", StringUtil.asString(result.chunkItem().getData()),
                is("Item failed due to webservice returning 2 updateMarcXchangeResults when 1 was expected."));
    }

    /**
     * A request carries one record, so an answer naming another record is the service answering a
     * question that was not asked, and the item is failed rather than given that answer's outcome.
     */
    @Test
    void resultForAnotherRecordId_isFailed() {
        stubResponse(result("ok", "Request received!", "9"));

        ItemDeliveryResult result = consumer.deliverItem(itemMessage(), successItem());

        assertThat("verdict", result.status(), is(ItemDeliveryResult.Status.FAILED));
        assertThat("outcome data", StringUtil.asString(result.chunkItem().getData()),
                is("Item failed due to webservice returning updateMarcXchangeResult for record id 9 when 3 was expected."
                        + " -> [Status: ok], [Message: Request received!]"));
    }

    /**
     * An unreachable IMS parks the item for redelivery rather than failing it. The connector has
     * already exhausted its own retries by the time one reaches here, so the failure is an outage
     * rather than a rejected record.
     */
    @Test
    void webServiceException_isThrown() {
        stubFor(post(urlPathMatching("/")).willReturn(aResponse().withStatus(500)));

        assertThrows(RuntimeException.class, () -> consumer.deliverItem(itemMessage(), successItem()));
    }

    /**
     * One request carries one item, so the chunk it belongs to no longer identifies it.
     */
    @Test
    void requestCarriesItemTrackingId() {
        stubResponse(result("ok", "Request received!", String.valueOf(ITEM_ID)));

        consumer.deliverItem(itemMessage(), successItem());

        verify(postRequestedFor(urlPathMatching("/"))
                .withRequestBody(containing(">42-7-3<")));
    }

    /**
     * This sink keeps the delivery watermark rather than opting out of it, which is observable
     * only as the lookup being made.
     */
    @Test
    void watermarkIsConsulted() throws InvalidMessageException, JobStoreServiceConnectorException {
        stubResponse(result("ok", "Request received!", String.valueOf(ITEM_ID)));
        when(jobStoreServiceConnector.getWatermark(anyInt(), anyString())).thenReturn(Optional.<Watermark>empty());

        consumer.handleConsumedMessage(itemMessage());

        Mockito.verify(jobStoreServiceConnector).getWatermark((int) SINK_ID, RECORD_KEY);
    }

    private ChunkItem successItem() {
        return item(ChunkItem.Status.SUCCESS, ImsServiceConnectorTest.ImsServiceRequestResponse.MARCXCHANGE_710100_OK);
    }

    private ChunkItem item(ChunkItem.Status status, String data) {
        return new ChunkItem()
                .withId(ITEM_ID)
                .withStatus(status)
                .withType(ChunkItem.Type.STRING)
                .withTrackingId(TRACKING_ID)
                .withData(data);
    }

    private ConsumedMessage itemMessage() {
        Map<String, Object> headers = new HashMap<>();
        headers.put(JMSHeader.payload.name, JMSHeader.ITEM_PAYLOAD_TYPE);
        headers.put(JMSHeader.jobId.name, JOB_ID);
        headers.put(JMSHeader.chunkId.name, (long) CHUNK_ID);
        headers.put(JMSHeader.itemId.name, ITEM_ID);
        headers.put(JMSHeader.sinkId.name, SINK_ID);
        headers.put(JMSHeader.sinkVersion.name, SINK_VERSION);
        headers.put(JMSHeader.recordKey.name, RECORD_KEY);
        try {
            return new ConsumedMessage("id", headers, jsonbContext.marshall(successItem()));
        } catch (JSONBException e) {
            throw new IllegalStateException(e);
        }
    }

    private String result(String status, String message, String recordId) {
        return "<updateMarcXchangeResult>"
                + "<updateMarcXchangeStatus>" + status + "</updateMarcXchangeStatus>"
                + "<updateMarcXchangeMessage>" + message + "</updateMarcXchangeMessage>"
                + (recordId == null ? "" : "<MarcXchangeRecordId>" + recordId + "</MarcXchangeRecordId>")
                + "</updateMarcXchangeResult>";
    }

    private void stubResponse(String results) {
        stubFor(post(urlPathMatching("/")).willReturn(aResponse()
                .withStatus(200)
                .withBody("<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"><soap:Body>"
                        + "<updateMarcXchangeResponse xmlns=\"http://oss.dbc.dk/ns/updateMarcXchange\">"
                        + results
                        + "</updateMarcXchangeResponse></soap:Body></soap:Envelope>")));
    }
}
