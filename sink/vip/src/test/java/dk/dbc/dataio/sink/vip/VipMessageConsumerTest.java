package dk.dbc.dataio.sink.vip;

import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.VipSinkConfig;
import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;
import dk.dbc.dataio.commons.types.jms.JMSHeader;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.jobstore.types.ItemDeliveryResult;
import dk.dbc.dataio.jobstore.types.Watermark;
import dk.dbc.dataio.jse.artemis.common.service.ServiceHub;
import dk.dbc.dataio.sink.vip.connector.VipCoreConnector;
import dk.dbc.dataio.sink.vip.connector.VipCoreConnectorException;
import dk.dbc.dataio.sink.vip.connector.VipCoreConnectorUnexpectedStatusCodeException;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.StringContains.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// TODO: 24-04-19 Add wiremock request/response tests when VIP-CORE service is available

class VipMessageConsumerTest {
    private static final int JOB_ID = 42;
    private static final int CHUNK_ID = 7;
    private static final short ITEM_ID = 3;
    private static final long SINK_ID = 15;
    private static final String RECORD_KEY = "870970:12345678";
    private static final String TRACKING_ID = "rr:1223io:12534";

    private final JSONBContext jsonbContext = new JSONBContext();
    private final JobStoreServiceConnector jobStoreServiceConnector = mock(JobStoreServiceConnector.class);
    private final VipCoreConnector vipCoreConnector = mock(VipCoreConnector.class);
    private final VipSinkConfig config = new VipSinkConfig().withEndpoint("http://vip-core");
    private final ConfigBean configBean = mock(ConfigBean.class);
    private final VipMessageConsumer consumer = new VipMessageConsumer(
            new ServiceHub.Builder().withJobStoreServiceConnector(jobStoreServiceConnector).test(),
            configBean, config, vipCoreConnector);

    @Test
    void successItem_isDelivered() throws VipCoreConnectorException {
        givenConfig();

        ItemDeliveryResult result = consumer.deliverItem(itemMessage(), item(ChunkItem.Status.SUCCESS,
                addiRecords(addiRecord("bibbas", "{\"id\":1}"))));

        assertThat("verdict", result.status(), is(ItemDeliveryResult.Status.DELIVERED));
        assertThat("outcome status", result.chunkItem().getStatus(), is(ChunkItem.Status.SUCCESS));
        assertThat("outcome data", StringUtil.asString(result.chunkItem().getData()), is("Loaded"));
        verify(vipCoreConnector).vipload("bibbas", "{\"id\":1}");
    }

    /**
     * IGNORED rather than DELIVERED is what keeps an item this sink did not send counted as ignored
     * in the delivering phase, as it was when whole chunks were delivered, and what leaves the
     * record's delivery watermark where it was.
     */
    @Test
    void failureItem_isIgnored() {
        givenConfig();

        ItemDeliveryResult result = consumer.deliverItem(itemMessage(), item(ChunkItem.Status.FAILURE, "data"));

        assertThat("verdict", result.status(), is(ItemDeliveryResult.Status.IGNORED));
        assertThat("outcome status", result.chunkItem().getStatus(), is(ChunkItem.Status.IGNORE));
        assertThat("outcome data", StringUtil.asString(result.chunkItem().getData()), is("Failed by processor"));
    }

    @Test
    void ignoreItem_isIgnored() {
        givenConfig();

        ItemDeliveryResult result = consumer.deliverItem(itemMessage(), item(ChunkItem.Status.IGNORE, "data"));

        assertThat("verdict", result.status(), is(ItemDeliveryResult.Status.IGNORED));
        assertThat("outcome status", result.chunkItem().getStatus(), is(ChunkItem.Status.IGNORE));
        assertThat("outcome data", StringUtil.asString(result.chunkItem().getData()), is("Ignored by processor"));
    }

    @Test
    void invalidAddiMetadata_isFailed() {
        givenConfig();

        AddiRecord invalidMetadata = new AddiRecord(StringUtil.asBytes("not JSON"), StringUtil.asBytes("{}"));

        ItemDeliveryResult result = consumer.deliverItem(itemMessage(),
                item(ChunkItem.Status.SUCCESS, addiRecords(invalidMetadata)));

        assertThat("verdict", result.status(), is(ItemDeliveryResult.Status.FAILED));
        assertThat("outcome status", result.chunkItem().getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat("outcome diagnostics", result.chunkItem().getDiagnostics().size(), is(1));
    }

    /**
     * A rejection is reported as failed rather than thrown, since the connector has already
     * exhausted its own retry policy by the time it reports one.
     */
    @Test
    void rejectedByVipCore_isFailed() throws VipCoreConnectorException {
        givenConfig();
        VipCoreConnectorUnexpectedStatusCodeException rejected =
                new VipCoreConnectorUnexpectedStatusCodeException("VIP-CORE said no", 400);
        rejected.setError(error("record is not valid"));
        doThrow(rejected).when(vipCoreConnector).vipload(anyString(), anyString());

        ItemDeliveryResult result = consumer.deliverItem(itemMessage(), item(ChunkItem.Status.SUCCESS,
                addiRecords(addiRecord("bibbas", "{\"id\":1}"))));

        assertThat("verdict", result.status(), is(ItemDeliveryResult.Status.FAILED));
        assertThat("outcome status", result.chunkItem().getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat("outcome data names the status code", StringUtil.asString(result.chunkItem().getData()),
                containsString("VIP-CORE said no"));
        assertThat("outcome data names what VIP-CORE said", StringUtil.asString(result.chunkItem().getData()),
                containsString("record is not valid"));
    }

    @Test
    void multipleRecords_areAllUploaded() throws VipCoreConnectorException {
        givenConfig();

        ItemDeliveryResult result = consumer.deliverItem(itemMessage(), item(ChunkItem.Status.SUCCESS,
                addiRecords(addiRecord("bibbas", "{\"id\":1}"), addiRecord("bibbas", "{\"id\":2}"))));

        assertThat("verdict", result.status(), is(ItemDeliveryResult.Status.DELIVERED));
        InOrder inOrder = inOrder(vipCoreConnector);
        inOrder.verify(vipCoreConnector).vipload("bibbas", "{\"id\":1}");
        inOrder.verify(vipCoreConnector).vipload("bibbas", "{\"id\":2}");
    }

    /**
     * An item rejected halfway leaves its earlier records at VIP-CORE and its record's delivery
     * watermark where it was, so that a later version of the record is free to reach VIP-CORE.
     */
    @Test
    void secondRecordRejected_isFailed() throws VipCoreConnectorException {
        givenConfig();
        doThrow(new VipCoreConnectorUnexpectedStatusCodeException("VIP-CORE said no", 400))
                .when(vipCoreConnector).vipload("bibbas", "{\"id\":2}");

        ItemDeliveryResult result = consumer.deliverItem(itemMessage(), item(ChunkItem.Status.SUCCESS,
                addiRecords(addiRecord("bibbas", "{\"id\":1}"), addiRecord("bibbas", "{\"id\":2}"))));

        assertThat("verdict", result.status(), is(ItemDeliveryResult.Status.FAILED));
        verify(vipCoreConnector).vipload("bibbas", "{\"id\":1}");
    }

    /**
     * This sink keeps the delivery watermark rather than opting out of it, which is observable only
     * as the lookup being made.
     */
    @Test
    void watermarkIsConsulted() throws InvalidMessageException, JobStoreServiceConnectorException {
        givenConfig();
        when(jobStoreServiceConnector.getWatermark(anyInt(), anyString())).thenReturn(Optional.<Watermark>empty());

        consumer.handleConsumedMessage(itemMessage());

        verify(jobStoreServiceConnector).getWatermark((int) SINK_ID, RECORD_KEY);
    }

    private void givenConfig() {
        when(configBean.getConfig(any(ConsumedMessage.class))).thenReturn(config);
    }

    private VipCoreConnector.Error error(String message) {
        try {
            return jsonbContext.unmarshall("{\"message\":\"" + message + "\"}", VipCoreConnector.Error.class);
        } catch (JSONBException e) {
            throw new IllegalStateException(e);
        }
    }

    private static AddiRecord addiRecord(String metadataFormat, String content) {
        return new AddiRecord(StringUtil.asBytes("{\"format\":\"" + metadataFormat + "\"}"),
                StringUtil.asBytes(content));
    }

    private static byte[] addiRecords(AddiRecord... addiRecords) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try {
            for (AddiRecord addiRecord : addiRecords) {
                bytes.write(addiRecord.getBytes());
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return bytes.toByteArray();
    }

    private ChunkItem item(ChunkItem.Status status, String data) {
        return item(status, StringUtil.asBytes(data));
    }

    private ChunkItem item(ChunkItem.Status status, byte[] data) {
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
        headers.put(JMSHeader.sinkVersion.name, 1L);
        headers.put(JMSHeader.recordKey.name, RECORD_KEY);
        try {
            return new ConsumedMessage("id", headers,
                    jsonbContext.marshall(item(ChunkItem.Status.IGNORE, "processing outcome")));
        } catch (JSONBException e) {
            throw new IllegalStateException(e);
        }
    }
}
