package dk.dbc.dataio.sink.marcconv.jms;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.commons.conversion.ConversionParam;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;
import dk.dbc.dataio.commons.types.jms.JMSHeader;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.jobstore.types.criteria.JobListCriteria;
import dk.dbc.dataio.commons.utils.lang.ResourceReader;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.commons.utils.test.model.ChunkItemBuilder;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnector;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnectorException;
import dk.dbc.dataio.jobstore.types.ItemDeliveryResult;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.jse.artemis.common.service.ServiceHub;
import dk.dbc.dataio.sink.marcconv.IntegrationTest;
import dk.dbc.dataio.sink.marcconv.entity.ConversionBlock;
import dk.dbc.dataio.sink.marcconv.entity.StoredConversionParam;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MessageConsumerIT extends IntegrationTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final int JOB_ID = 42;
    private static final int CHUNK_ID = 3;
    private static final long SINK_ID = 1L;
    private static final String FILE_STORE_URL = "http://filestore";
    private static final String FILE_ID = "123456789";

    private final JSONBContext jsonbContext = new JSONBContext();
    private final JobStoreServiceConnector jobStoreServiceConnector = mock(JobStoreServiceConnector.class);
    private final FileStoreServiceConnector fileStoreServiceConnector = mock(FileStoreServiceConnector.class);

    private final ConversionParam conversionParam = new ConversionParam().withPackaging("iso").withEncoding("danmarc2");
    private final AddiRecord addiRecord1 = newAddiRecord(conversionParam, "test-record-1-danmarc2.marcxchange");
    private final byte[] isoRecord1 = ResourceReader.getResourceAsByteArray(MessageConsumerIT.class, "test-record-1-danmarc2.iso");
    private final AddiRecord addiRecord2 = newAddiRecord(conversionParam, "test-record-2-danmarc2.marcxchange");
    private final byte[] isoRecord2 = ResourceReader.getResourceAsByteArray(MessageConsumerIT.class, "test-record-2-danmarc2.iso");

    @Before
    public void setupMocks() throws FileStoreServiceConnectorException, JobStoreServiceConnectorException {
        when(jobStoreServiceConnector.listJobs(any(JobListCriteria.class)))
                .thenReturn(Collections.singletonList(new JobInfoSnapshot()
                        .withJobId(JOB_ID)
                        .withSpecification(new JobSpecification().withSubmitterId(870970))));
        when(fileStoreServiceConnector.searchByMetadata(any(), any()))
                .thenReturn(Collections.emptyList());
        when(fileStoreServiceConnector.addFile(any(InputStream.class)))
                .thenReturn(FILE_ID);
        when(fileStoreServiceConnector.getBaseUrl())
                .thenReturn(FILE_STORE_URL);
    }

    @Test
    public void itemFailedByProcessorIsNotConverted() {
        MessageConsumer messageConsumer = newMessageConsumer();
        ChunkItem item = new ChunkItemBuilder().setId(0L).setStatus(ChunkItem.Status.FAILURE).build();

        ChunkItem outcome = convert(messageConsumer, item, (short) 0);

        assertThat("outcome status", outcome.getStatus(), is(ChunkItem.Status.IGNORE));
        assertThat("outcome data", StringUtil.asString(outcome.getData()), is("Failed by processor"));
        assertThat("block written", findBlock(0), is(nullValue()));
    }

    @Test
    public void itemIgnoredByProcessorIsNotConverted() {
        MessageConsumer messageConsumer = newMessageConsumer();
        ChunkItem item = new ChunkItemBuilder().setId(1L).setStatus(ChunkItem.Status.IGNORE).build();

        ChunkItem outcome = convert(messageConsumer, item, (short) 1);

        assertThat("outcome status", outcome.getStatus(), is(ChunkItem.Status.IGNORE));
        assertThat("outcome data", StringUtil.asString(outcome.getData()), is("Ignored by processor"));
        assertThat("block written", findBlock(1), is(nullValue()));
    }

    @Test
    public void itemWithoutAddiContentIsFailed() {
        MessageConsumer messageConsumer = newMessageConsumer();
        ChunkItem item = new ChunkItemBuilder().setId(2L).setStatus(ChunkItem.Status.SUCCESS).build();

        ChunkItem outcome = convert(messageConsumer, item, (short) 2);

        assertThat("outcome status", outcome.getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat("outcome diagnostics", outcome.getDiagnostics(), is(notNullValue()));
        assertThat("block written", findBlock(2), is(nullValue()));
    }

    @Test
    public void eachItemOfAChunkIsConvertedIntoItsOwnBlock() {
        MessageConsumer messageConsumer = newMessageConsumer();
        ChunkItem item0 = new ChunkItemBuilder().setId(0L).setStatus(ChunkItem.Status.SUCCESS).setData(addiRecord1.getBytes()).build();
        ChunkItem item1 = new ChunkItemBuilder().setId(1L).setStatus(ChunkItem.Status.SUCCESS).setData(addiRecord2.getBytes()).build();

        ChunkItem outcome0 = convert(messageConsumer, item0, (short) 0);
        ChunkItem outcome1 = convert(messageConsumer, item1, (short) 1);

        assertThat("1st outcome status", outcome0.getStatus(), is(ChunkItem.Status.SUCCESS));
        assertThat("1st outcome data", StringUtil.asString(outcome0.getData()), is("Converted"));
        assertThat("2nd outcome status", outcome1.getStatus(), is(ChunkItem.Status.SUCCESS));

        assertThat("1st block bytes", findBlock(0).getBytes(), is(isoRecord1));
        assertThat("2nd block bytes", findBlock(1).getBytes(), is(isoRecord2));
    }

    @Test
    public void conversionParamIsStoredOnceForTheJob() {
        MessageConsumer messageConsumer = newMessageConsumer();
        ChunkItem item = new ChunkItemBuilder().setId(0L).setStatus(ChunkItem.Status.SUCCESS).setData(addiRecord1.getBytes()).build();

        convert(messageConsumer, item, (short) 0);

        StoredConversionParam storedConversionParam = env().getPersistenceContext().run(() ->
                env().getEntityManager().find(StoredConversionParam.class, JOB_ID));

        assertThat("StoredConversionParam", storedConversionParam, is(notNullValue()));
        assertThat("ConversionParam", storedConversionParam.getParam(), is(conversionParam));
    }

    @Test
    public void conversionParamAlreadyStoredIsKept() {
        // What another instance converting the same job leaves behind. The statement
        // itself is covered by StoredConversionParamIT; what this asserts is that an item
        // converts normally when the job's parameters are already someone else's.
        ConversionParam alreadyStored = new ConversionParam().withPackaging("iso").withEncoding("latin1");
        StoredConversionParam existing = new StoredConversionParam(JOB_ID);
        existing.setParam(alreadyStored);
        env().getPersistenceContext().run(() -> env().getEntityManager().persist(existing));

        MessageConsumer messageConsumer = newMessageConsumer();
        ChunkItem item = new ChunkItemBuilder().setId(0L).setStatus(ChunkItem.Status.SUCCESS).setData(addiRecord1.getBytes()).build();

        ChunkItem outcome = convert(messageConsumer, item, (short) 0);

        assertThat("outcome status", outcome.getStatus(), is(ChunkItem.Status.SUCCESS));
        assertThat("block bytes", findBlock(0).getBytes(), is(isoRecord1));

        StoredConversionParam storedConversionParam = env().getPersistenceContext().run(() ->
                env().getEntityManager().find(StoredConversionParam.class, JOB_ID));
        assertThat("ConversionParam", storedConversionParam.getParam(), is(alreadyStored));
    }

    @Test
    public void redeliveredItemOverwritesItsOwnBlockAndLeavesItsNeighbourAlone() {
        MessageConsumer messageConsumer = newMessageConsumer();
        ChunkItem neighbour = new ChunkItemBuilder().setId(0L).setStatus(ChunkItem.Status.SUCCESS).setData(addiRecord1.getBytes()).build();
        ChunkItem item = new ChunkItemBuilder().setId(1L).setStatus(ChunkItem.Status.SUCCESS).setData(addiRecord2.getBytes()).build();

        convert(messageConsumer, neighbour, (short) 0);

        ConversionBlock existingBlock = new ConversionBlock();
        existingBlock.setKey(new ConversionBlock.Key(JOB_ID, CHUNK_ID, 1));
        existingBlock.setBytes(StringUtil.asBytes("stale"));
        env().getPersistenceContext().run(() -> env().getEntityManager().persist(existingBlock));

        convert(messageConsumer, item, (short) 1);

        assertThat("redelivered block bytes", findBlock(1).getBytes(), is(isoRecord2));
        assertThat("neighbouring block bytes", findBlock(0).getBytes(), is(isoRecord1));
    }

    @Test
    public void dontStoreZeroLengthBlocks() {
        // The ISO2709 conversion can't handle the marcxchange slim format
        // and therefore produces no output.
        //
        // No block should be persisted.

        ConversionParam slimConversionParam = new ConversionParam().withPackaging("iso").withEncoding("utf8");
        AddiRecord addiRecord = newAddiRecord(slimConversionParam, "test-record-3-marc21.slim");
        MessageConsumer messageConsumer = newMessageConsumer();
        ChunkItem item = new ChunkItemBuilder().setId(0L).setStatus(ChunkItem.Status.SUCCESS).setData(addiRecord.getBytes()).build();

        ChunkItem outcome = convert(messageConsumer, item, (short) 0);

        assertThat("outcome status", outcome.getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat("block written", findBlock(0), is(nullValue()));
    }

    @Test
    public void deliveredDataItemIsConvertedAndCountedAsSucceeded() throws Exception {
        MessageConsumer messageConsumer = newMessageConsumer();
        ChunkItem item = new ChunkItemBuilder().setId(0L).setStatus(ChunkItem.Status.SUCCESS).setData(addiRecord1.getBytes()).build();

        ItemDeliveryResult result = messageConsumer.deliverItem(newItemMessage(item, (short) 0), item);

        assertThat("verdict", result.status(), is(ItemDeliveryResult.Status.DELIVERED));
        assertThat("outcome status", result.chunkItem().getStatus(), is(ChunkItem.Status.SUCCESS));
        assertThat("block bytes", findBlock(0).getBytes(), is(isoRecord1));
    }

    @Test
    public void itemPassedThroughIsCountedAsIgnored() throws Exception {
        MessageConsumer messageConsumer = newMessageConsumer();
        ChunkItem item = new ChunkItemBuilder().setId(0L).setStatus(ChunkItem.Status.IGNORE).build();

        ItemDeliveryResult result = messageConsumer.deliverItem(newItemMessage(item, (short) 0), item);

        assertThat("verdict", result.status(), is(ItemDeliveryResult.Status.IGNORED));
        assertThat("outcome status", result.chunkItem().getStatus(), is(ChunkItem.Status.IGNORE));
    }

    @Test
    public void unconvertibleItemIsCountedAsFailed() throws Exception {
        MessageConsumer messageConsumer = newMessageConsumer();
        ChunkItem item = new ChunkItemBuilder().setId(0L).setStatus(ChunkItem.Status.SUCCESS).build();

        ItemDeliveryResult result = messageConsumer.deliverItem(newItemMessage(item, (short) 0), item);

        assertThat("verdict", result.status(), is(ItemDeliveryResult.Status.FAILED));
        assertThat("outcome status", result.chunkItem().getStatus(), is(ChunkItem.Status.FAILURE));
    }

    @Test
    public void terminationItemUploadsTheJobsBlocks() throws Exception {
        MessageConsumer messageConsumer = newMessageConsumer();
        ChunkItem dataItem = new ChunkItemBuilder().setId(0L).setStatus(ChunkItem.Status.SUCCESS).setData(addiRecord1.getBytes()).build();
        messageConsumer.deliverItem(newItemMessage(dataItem, (short) 0), dataItem);

        ChunkItem terminationItem = new ChunkItem()
                .withId(1)
                .withStatus(ChunkItem.Status.SUCCESS)
                .withType(ChunkItem.Type.JOB_END)
                .withData("termination");

        ItemDeliveryResult result = messageConsumer.deliverItem(newItemMessage(terminationItem, (short) 1), terminationItem);

        assertThat("verdict", result.status(), is(ItemDeliveryResult.Status.DELIVERED));
        assertThat("outcome type", result.chunkItem().getType().getFirst(), is(ChunkItem.Type.JOB_END));
        assertThat("outcome data", StringUtil.asString(result.chunkItem().getData()),
                is(String.join("/", FILE_STORE_URL, "files", FILE_ID)));
        assertThat("blocks deleted", findBlock(0), is(nullValue()));
    }

    private ChunkItem convert(MessageConsumer messageConsumer, ChunkItem item, short itemId) {
        return env().getPersistenceContext().run(() ->
                messageConsumer.convertItem(item, JOB_ID, CHUNK_ID, itemId, env().getEntityManager()));
    }

    private ConversionBlock findBlock(int itemId) {
        return env().getPersistenceContext().run(() ->
                env().getEntityManager().find(ConversionBlock.class, new ConversionBlock.Key(JOB_ID, CHUNK_ID, itemId)));
    }

    private MessageConsumer newMessageConsumer() {
        ServiceHub hub = new ServiceHub.Builder().withJobStoreServiceConnector(jobStoreServiceConnector).test();
        return new MessageConsumer(hub, fileStoreServiceConnector, env().getEntityManagerFactory());
    }

    /**
     * Builds the message job-store dispatches per item, deliberately including the record
     * key this sink must not act on
     */
    private ConsumedMessage newItemMessage(ChunkItem item, short itemId) {
        Map<String, Object> headers = new HashMap<>();
        headers.put(JMSHeader.payload.name, JMSHeader.ITEM_PAYLOAD_TYPE);
        headers.put(JMSHeader.jobId.name, JOB_ID);
        headers.put(JMSHeader.chunkId.name, (long) CHUNK_ID);
        headers.put(JMSHeader.itemId.name, itemId);
        headers.put(JMSHeader.sinkId.name, SINK_ID);
        headers.put(JMSHeader.recordKey.name, "870970:record");
        try {
            return new ConsumedMessage("messageId", headers, jsonbContext.marshall(item));
        } catch (JSONBException e) {
            throw new IllegalStateException(e);
        }
    }

    private AddiRecord newAddiRecord(ConversionParam conversionParam, String resourceFile) {
        try {
            byte[] metadata = MAPPER.writeValueAsBytes(conversionParam);
            byte[] record = ResourceReader.getResourceAsByteArray(MessageConsumerIT.class, resourceFile);
            return new AddiRecord(metadata, record);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }
}
