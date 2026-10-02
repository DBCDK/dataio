package dk.dbc.dataio.sink.holdingsitems;

import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;
import dk.dbc.dataio.commons.types.jms.JMSHeader;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.jobstore.types.ItemDeliveryResult;
import dk.dbc.dataio.jobstore.types.Watermark;
import dk.dbc.dataio.jse.artemis.common.service.ServiceHub;
import dk.dbc.solrdocstore.connector.SolrDocStoreConnector;
import dk.dbc.solrdocstore.connector.SolrDocStoreConnectorException;
import dk.dbc.solrdocstore.connector.SolrDocStoreConnectorUnexpectedStatusCodeException;
import dk.dbc.solrdocstore.connector.model.HoldingsItems;
import dk.dbc.solrdocstore.connector.model.IndexKeys;
import dk.dbc.solrdocstore.connector.model.Status;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MessageConsumerTest {
    private static final int JOB_ID = 42;
    private static final int CHUNK_ID = 7;
    private static final short ITEM_ID = 3;
    private static final long SINK_ID = 15;
    private static final String RECORD_KEY = "123456:id1";
    private static final String TRACKING_ID = "rr:1223io:12534";

    private final JSONBContext jsonbContext = new JSONBContext();
    private final SolrDocStoreConnector solrDocStoreConnector = mock(SolrDocStoreConnector.class);
    private final JobStoreServiceConnector jobStoreServiceConnector = mock(JobStoreServiceConnector.class);
    private final MessageConsumer messageConsumer = newMessageConsumer();

    private final HoldingsItems holdingsItemsOK1 = holdingsItems("id1__1");
    private final HoldingsItems holdingsItemsFail = holdingsItems("id1__2");
    private final HoldingsItems holdingsItemsOK2 = holdingsItems("id1__3");

    @Test
    void successItem_isDelivered() throws JSONBException, SolrDocStoreConnectorException {
        acceptHoldings(holdingsItemsOK1);

        ItemDeliveryResult result = messageConsumer.deliverItem(
                itemMessage(), item(ChunkItem.Status.SUCCESS, addiRecord(holdingsItemsOK1)));

        assertThat("verdict", result.status(), is(ItemDeliveryResult.Status.DELIVERED));
        assertThat("outcome status", result.chunkItem().getStatus(), is(ChunkItem.Status.SUCCESS));
        assertThat("outcome data", StringUtil.asString(result.chunkItem().getData()),
                is("id1__1:123456 consumer service response - ok\n"));
        assertThat("outcome tracking id", result.chunkItem().getTrackingId(), is(TRACKING_ID));
    }

    /**
     * IGNORED rather than DELIVERED is what keeps an item this sink did not send counted as
     * ignored in the delivering phase, and leaves the record's delivery watermark where it was.
     */
    @Test
    void failureItem_isIgnored() throws JSONBException, SolrDocStoreConnectorException {
        ItemDeliveryResult result = messageConsumer.deliverItem(
                itemMessage(), item(ChunkItem.Status.FAILURE, new byte[0]));

        assertThat("verdict", result.status(), is(ItemDeliveryResult.Status.IGNORED));
        assertThat("outcome status", result.chunkItem().getStatus(), is(ChunkItem.Status.IGNORE));
        assertThat("outcome data", StringUtil.asString(result.chunkItem().getData()),
                is("Failed by processor"));
    }

    @Test
    void ignoreItem_isIgnored() throws JSONBException, SolrDocStoreConnectorException {
        ItemDeliveryResult result = messageConsumer.deliverItem(
                itemMessage(), item(ChunkItem.Status.IGNORE, new byte[0]));

        assertThat("verdict", result.status(), is(ItemDeliveryResult.Status.IGNORED));
        assertThat("outcome status", result.chunkItem().getStatus(), is(ChunkItem.Status.IGNORE));
        assertThat("outcome data", StringUtil.asString(result.chunkItem().getData()),
                is("Ignored by processor"));
    }

    @Test
    void invalidItemData_isFailed() throws JSONBException, SolrDocStoreConnectorException {
        AddiRecord addiRecord = new AddiRecord(new byte[0], StringUtil.asBytes("not json"));

        ItemDeliveryResult result = messageConsumer.deliverItem(
                itemMessage(), item(ChunkItem.Status.SUCCESS, addiRecord.getBytes()));

        assertThat("verdict", result.status(), is(ItemDeliveryResult.Status.FAILED));
        assertThat("outcome status", result.chunkItem().getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat("outcome diagnostics", result.chunkItem().getDiagnostics(), is(notNullValue()));
    }

    @Test
    void rejectedHoldings_isFailed() throws JSONBException, SolrDocStoreConnectorException {
        acceptHoldings(holdingsItemsOK1);
        acceptHoldings(holdingsItemsOK2);
        rejectHoldings(holdingsItemsFail);

        ItemDeliveryResult result = messageConsumer.deliverItem(itemMessage(),
                item(ChunkItem.Status.SUCCESS, addiRecord(holdingsItemsOK1, holdingsItemsFail, holdingsItemsOK2)));

        assertThat("verdict", result.status(), is(ItemDeliveryResult.Status.FAILED));
        assertThat("outcome status", result.chunkItem().getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat("outcome data", StringUtil.asString(result.chunkItem().getData()),
                is("id1__1:123456 consumer service response - ok\n"
                        + "id1__2:123456 consumer service response - error\n"
                        + "id1__3:123456 consumer service response - ok\n"));
    }

    /**
     * A connector failure is thrown rather than reported, which has the item redelivered instead of
     * counted as failed.
     */
    @Test
    void connectorFailure_isThrown() throws JSONBException, SolrDocStoreConnectorException {
        when(solrDocStoreConnector.setHoldings(eq(holdingsItemsOK1)))
                .thenThrow(new SolrDocStoreConnectorException("connector failure"));

        ChunkItem item = item(ChunkItem.Status.SUCCESS, addiRecord(holdingsItemsOK1));

        assertThrows(SolrDocStoreConnectorException.class,
                () -> messageConsumer.deliverItem(itemMessage(), item));
    }

    /**
     * This sink keeps the delivery watermark rather than opting out of it, which is observable only
     * as the lookup being made.
     */
    @Test
    void watermarkIsConsulted() throws InvalidMessageException, JobStoreServiceConnectorException,
            JSONBException, SolrDocStoreConnectorException {
        acceptHoldings(holdingsItemsOK1);
        when(jobStoreServiceConnector.getWatermark(anyInt(), anyString())).thenReturn(Optional.<Watermark>empty());

        messageConsumer.handleConsumedMessage(itemMessage());

        verify(jobStoreServiceConnector).getWatermark((int) SINK_ID, RECORD_KEY);
    }

    private MessageConsumer newMessageConsumer() {
        HoldingsItemsUnmarshaller holdingsItemsUnmarshaller = new HoldingsItemsUnmarshaller(solrDocStoreConnector);
        ServiceHub hub = new ServiceHub.Builder()
                .withJobStoreServiceConnector(jobStoreServiceConnector)
                .test();
        return new MessageConsumer(hub, solrDocStoreConnector, holdingsItemsUnmarshaller);
    }

    private HoldingsItems holdingsItems(String bibliographicRecordId) {
        IndexKeys indexKeys = new IndexKeys();
        indexKeys.put("k1", Collections.singletonList("v1"));
        List<IndexKeys> indexKeysList = new ArrayList<>();
        indexKeysList.add(indexKeys);

        HoldingsItems holdingsItems = new HoldingsItems();
        holdingsItems.setAgencyId(123456);
        holdingsItems.setBibliographicRecordId(bibliographicRecordId);
        holdingsItems.setIndexKeys(indexKeysList);
        return holdingsItems;
    }

    private void acceptHoldings(HoldingsItems holdingsItems) throws SolrDocStoreConnectorException {
        Status ok = new Status();
        ok.setOk(true);
        ok.setText("ok");
        when(solrDocStoreConnector.setHoldings(eq(holdingsItems))).thenReturn(ok);
    }

    private void rejectHoldings(HoldingsItems holdingsItems) throws SolrDocStoreConnectorException {
        Status error = new Status();
        error.setOk(false);
        error.setText("error");
        SolrDocStoreConnectorUnexpectedStatusCodeException internalServerError =
                new SolrDocStoreConnectorUnexpectedStatusCodeException("internal server error", 500);
        internalServerError.setStatus(error);
        when(solrDocStoreConnector.setHoldings(eq(holdingsItems))).thenThrow(internalServerError);
    }

    private byte[] addiRecord(HoldingsItems... holdingsItems) throws JSONBException {
        return new AddiRecord(new byte[0],
                StringUtil.asBytes(jsonbContext.marshall(Arrays.asList(holdingsItems)))).getBytes();
    }

    private ChunkItem item(ChunkItem.Status status, byte[] data) {
        return new ChunkItem()
                .withId(ITEM_ID)
                .withStatus(status)
                .withType(ChunkItem.Type.ADDI)
                .withTrackingId(TRACKING_ID)
                .withData(data);
    }

    private ConsumedMessage itemMessage() throws JSONBException {
        Map<String, Object> headers = new HashMap<>();
        headers.put(JMSHeader.payload.name, JMSHeader.ITEM_PAYLOAD_TYPE);
        headers.put(JMSHeader.jobId.name, JOB_ID);
        headers.put(JMSHeader.chunkId.name, (long) CHUNK_ID);
        headers.put(JMSHeader.itemId.name, ITEM_ID);
        headers.put(JMSHeader.sinkId.name, SINK_ID);
        headers.put(JMSHeader.recordKey.name, RECORD_KEY);
        return new ConsumedMessage("id", headers,
                jsonbContext.marshall(item(ChunkItem.Status.SUCCESS, addiRecord(holdingsItemsOK1))));
    }
}
