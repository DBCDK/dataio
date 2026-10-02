package dk.dbc.dataio.sink.dmatdm3;

import dk.dbc.commons.addi.AddiReader;
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
import dk.dbc.dmat.service.connector.DMatServiceConnector;
import dk.dbc.dmat.service.connector.DMatServiceConnectorException;
import dk.dbc.dmat.service.dto.RecordData;
import dk.dbc.dmat.service.persistence.DMatRecord;
import dk.dbc.dmat.service.persistence.enums.EReolCode;
import dk.dbc.dmat.service.persistence.enums.Status;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DMatMessageConsumerTest {
    private static final int JOB_ID = 42;
    private static final int CHUNK_ID = 7;
    private static final short ITEM_ID = 3;
    private static final long SINK_ID = 15;
    private static final String RECORD_KEY = "150015:9788772196244";
    private static final String TRACKING_ID = "10.233.28.92-16794738-0-2";
    private static final String RECORD_REFERENCE = "33707b62-99d2-47a0-ad51-a7a35e53493c";
    private static final String DATESTAMP = "20150706T12:09:19Z";

    private final JSONBContext jsonbContext = new JSONBContext();
    private final DMatServiceConnector dMatServiceConnector = mock(DMatServiceConnector.class);
    private final JobStoreServiceConnector jobStoreServiceConnector = mock(JobStoreServiceConnector.class);
    private final DMatMessageConsumer consumer = new DMatMessageConsumer(
            new ServiceHub.Builder().withJobStoreServiceConnector(jobStoreServiceConnector).test(),
            dMatServiceConnector);

    @BeforeEach
    void setupMocks() throws DMatServiceConnectorException, JSONBException {
        when(dMatServiceConnector.upsertRecord(any(RecordData.class)))
                .thenReturn(new DMatRecord().withId(1).withStatus(Status.NEW));
    }

    @Test
    void successItem_isDelivered() throws IOException, DMatServiceConnectorException, JSONBException {
        ItemDeliveryResult result = consumer.deliverItem(itemMessage(),
                item(ChunkItem.Status.SUCCESS, readLocalFile("valid_recorddata_addi.json")));

        assertThat("verdict", result.status(), is(ItemDeliveryResult.Status.DELIVERED));
        assertThat("outcome status", result.chunkItem().getStatus(), is(ChunkItem.Status.SUCCESS));
        assertThat("outcome data", StringUtil.asString(result.chunkItem().getData()),
                is(JOB_ID + "." + CHUNK_ID + "." + ITEM_ID + ": "
                        + RECORD_REFERENCE + "@" + DATESTAMP + " => seqno 1 status NEW"));
        verify(dMatServiceConnector).upsertRecord(any(RecordData.class));
    }

    /**
     * IGNORED rather than DELIVERED is what keeps an item this sink did not send counted as ignored
     * in the delivering phase, as it was when whole chunks were delivered, and what leaves the
     * record's delivery watermark where it was.
     */
    @Test
    void failureItem_isIgnored() throws DMatServiceConnectorException, JSONBException {
        ItemDeliveryResult result = consumer.deliverItem(itemMessage(),
                item(ChunkItem.Status.FAILURE, StringUtil.asBytes("data")));

        assertThat("verdict", result.status(), is(ItemDeliveryResult.Status.IGNORED));
        assertThat("outcome status", result.chunkItem().getStatus(), is(ChunkItem.Status.IGNORE));
        assertThat("outcome data", StringUtil.asString(result.chunkItem().getData()),
                is("Failed by processor"));
        verify(dMatServiceConnector, never()).upsertRecord(any(RecordData.class));
    }

    @Test
    void ignoreItem_isIgnored() throws DMatServiceConnectorException, JSONBException {
        ItemDeliveryResult result = consumer.deliverItem(itemMessage(),
                item(ChunkItem.Status.IGNORE, StringUtil.asBytes("data")));

        assertThat("verdict", result.status(), is(ItemDeliveryResult.Status.IGNORED));
        assertThat("outcome status", result.chunkItem().getStatus(), is(ChunkItem.Status.IGNORE));
        assertThat("outcome data", StringUtil.asString(result.chunkItem().getData()),
                is("Ignored by processor"));
        verify(dMatServiceConnector, never()).upsertRecord(any(RecordData.class));
    }

    @Test
    void invalidAddi_isFailed() throws DMatServiceConnectorException, JSONBException {
        ItemDeliveryResult result = consumer.deliverItem(itemMessage(),
                item(ChunkItem.Status.SUCCESS, StringUtil.asBytes("invalid-chunk")));

        assertThat("verdict", result.status(), is(ItemDeliveryResult.Status.FAILED));
        assertThat("outcome status", result.chunkItem().getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat("outcome diagnostics", result.chunkItem().getDiagnostics(), is(notNullValue()));
        verify(dMatServiceConnector, never()).upsertRecord(any(RecordData.class));
    }

    /**
     * A record the sink itself rejects carries no diagnostic, which is what separates it from the
     * unexpected exception branch covered by {@link #invalidAddi_isFailed()}.
     */
    @Test
    void missingDatestamp_isFailed() throws IOException, DMatServiceConnectorException, JSONBException {
        ItemDeliveryResult result = consumer.deliverItem(itemMessage(),
                item(ChunkItem.Status.SUCCESS, readLocalFile("invalid_recorddata_addi_no_datestamp.json")));

        assertThat("verdict", result.status(), is(ItemDeliveryResult.Status.FAILED));
        assertThat("outcome status", result.chunkItem().getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat("outcome data", StringUtil.asString(result.chunkItem().getData()),
                is("Null or empty datestamp field. Record will fail"));
        assertThat("outcome diagnostics", result.chunkItem().getDiagnostics(), is(nullValue()));
        verify(dMatServiceConnector, never()).upsertRecord(any(RecordData.class));
    }

    @Test
    void missingRecordReference_isFailed() throws IOException, DMatServiceConnectorException, JSONBException {
        ItemDeliveryResult result = consumer.deliverItem(itemMessage(),
                item(ChunkItem.Status.SUCCESS, readLocalFile("invalid_recorddata_addi_no_recordreference.json")));

        assertThat("verdict", result.status(), is(ItemDeliveryResult.Status.FAILED));
        assertThat("outcome status", result.chunkItem().getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat("outcome data", StringUtil.asString(result.chunkItem().getData()),
                is("Null or empty record reference field. Record will fail"));
        assertThat("outcome diagnostics", result.chunkItem().getDiagnostics(), is(nullValue()));
        verify(dMatServiceConnector, never()).upsertRecord(any(RecordData.class));
    }

    /**
     * A DMat service the item never reached fails the item rather than having it redelivered, as it
     * did when whole chunks were delivered. A failed item advances no delivery watermark, so a later
     * version of the record is still free to reach DMat.
     */
    @Test
    void rejectedByDMat_isFailed() throws IOException, DMatServiceConnectorException, JSONBException {
        when(dMatServiceConnector.upsertRecord(any(RecordData.class)))
                .thenThrow(new DMatServiceConnectorException("DMat is down"));

        ItemDeliveryResult result = consumer.deliverItem(itemMessage(),
                item(ChunkItem.Status.SUCCESS, readLocalFile("valid_recorddata_addi.json")));

        assertThat("verdict", result.status(), is(ItemDeliveryResult.Status.FAILED));
        assertThat("outcome status", result.chunkItem().getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat("outcome diagnostics", result.chunkItem().getDiagnostics(), is(notNullValue()));
    }

    @Test
    void multipleRecords_areAllUpserted() throws IOException, DMatServiceConnectorException, JSONBException {
        byte[] validRecord = readLocalFile("valid_recorddata_addi.json");

        ItemDeliveryResult result = consumer.deliverItem(itemMessage(),
                item(ChunkItem.Status.SUCCESS, concat(validRecord, validRecord)));

        assertThat("verdict", result.status(), is(ItemDeliveryResult.Status.DELIVERED));
        assertThat("outcome data lines",
                StringUtil.asString(result.chunkItem().getData()).split("\n").length, is(2));
        verify(dMatServiceConnector, times(2)).upsertRecord(any(RecordData.class));
    }

    /**
     * The records preceding the rejected one stay in DMat, and the item is reported failed so that
     * the record's delivery watermark is left where it was rather than claiming a version as
     * delivered that only partly was.
     */
    @Test
    void secondRecordInvalid_isFailed() throws IOException, DMatServiceConnectorException, JSONBException {
        ItemDeliveryResult result = consumer.deliverItem(itemMessage(),
                item(ChunkItem.Status.SUCCESS, concat(
                        readLocalFile("valid_recorddata_addi.json"),
                        readLocalFile("invalid_recorddata_addi_no_datestamp.json"))));

        assertThat("verdict", result.status(), is(ItemDeliveryResult.Status.FAILED));
        assertThat("outcome data", StringUtil.asString(result.chunkItem().getData()),
                is("Null or empty datestamp field. Record will fail"));
        verify(dMatServiceConnector, times(1)).upsertRecord(any(RecordData.class));
    }

    /**
     * This sink keeps the delivery watermark rather than opting out of it, which is observable only
     * as the lookup being made.
     */
    @Test
    void watermarkIsConsulted() throws InvalidMessageException, JobStoreServiceConnectorException {
        when(jobStoreServiceConnector.getWatermark(anyInt(), anyString())).thenReturn(Optional.<Watermark>empty());

        consumer.handleConsumedMessage(itemMessage());

        verify(jobStoreServiceConnector).getWatermark((int) SINK_ID, RECORD_KEY);
    }

    /**
     * This test mimics a test in the dmat-service, to check that the RecordData object correctly
     * serializes and deserializes the incomming addi data, thus checking that we have the proper
     * versions of the dmat-connector (and by transitive dependency) the dmat-model
     */
    @Test
    void testRecordData() throws IOException, JSONBException {
        byte[] validRecordAddi = readLocalFile("valid_recorddata_addi.json");
        InputStream is = new ByteArrayInputStream(validRecordAddi);
        AddiReader rdr = new AddiReader(is);
        AddiRecord addiRecord = rdr.getNextRecord();

        RecordData record = RecordData.fromRaw(new String(addiRecord.getContentData()));

        assertThat("datestamp", record.getDatestamp(), is(DATESTAMP));
        assertThat("recordReference", record.getRecordReference(), is(RECORD_REFERENCE));
        assertThat("active", record.getActive(), is(true));
        assertThat("isbn13", record.getIsbn13(), is("9788772196244"));
        assertThat("productForm", record.getProductForm(), is("DIGITAL_DOWNLOAD"));
        assertThat("contentType", record.getContentType(), is("TEXT_EYE_READABLE"));
        assertThat("title.text", record.getTitle().getText(), is("Leadership pipeline"));
        assertThat("publisherName", record.getPublisherName(), is("Gyldendal"));
        assertThat("publishingDate", record.getPublishingDate(), is("20150202"));
        assertThat("lendingTypes", record.getLendingTypes(), is(notNullValue()));
        assertThat("lendingTypes", record.getLendingTypes().size(), is(1));
        assertThat("lendingTypes contains ERL", record.getLendingTypes().get(0), is(EReolCode.ERE));
    }

    private byte[] readLocalFile(String name) throws IOException {
        Path path = Paths.get(DMatMessageConsumerTest.class.getResource("/__files/" + name).getPath());
        return Files.readAllBytes(path);
    }

    private static byte[] concat(byte[]... addiRecords) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        for (byte[] addiRecord : addiRecords) {
            bytes.write(addiRecord);
        }
        return bytes.toByteArray();
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
        headers.put(JMSHeader.recordKey.name, RECORD_KEY);
        try {
            return new ConsumedMessage("id", headers,
                    jsonbContext.marshall(item(ChunkItem.Status.IGNORE, StringUtil.asBytes("processing outcome"))));
        } catch (JSONBException e) {
            throw new IllegalStateException(e);
        }
    }
}
