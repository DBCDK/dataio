package dk.dbc.dataio.sink.dpf;

import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.DpfSinkConfig;
import dk.dbc.dataio.commons.types.FlowBinder;
import dk.dbc.dataio.commons.types.FlowBinderContent;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.SinkContent;
import dk.dbc.dataio.commons.types.jms.JMSHeader;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.commons.utils.test.model.FlowBinderBuilder;
import dk.dbc.dataio.commons.utils.test.model.FlowBinderContentBuilder;
import dk.dbc.dataio.commons.utils.test.model.SinkBuilder;
import dk.dbc.dataio.commons.utils.test.model.SinkContentBuilder;
import dk.dbc.dataio.jobstore.types.ItemDeliveryResult;
import dk.dbc.dataio.jse.artemis.common.service.ServiceHub;
import dk.dbc.dataio.sink.dpf.model.DpfRecord;
import dk.dbc.dataio.sink.dpf.model.ProcessingInstructions;
import dk.dbc.dataio.sink.dpf.transform.MarcRecordFactory;
import dk.dbc.marc.binding.DataField;
import dk.dbc.marc.binding.MarcRecord;
import dk.dbc.marc.binding.SubField;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class MessageConsumerTest {
    private static final int JOB_ID = 3;
    private static final long CHUNK_ID = 2L;
    private static final short ITEM_ID = 1;
    private static final long SINK_ID = 10L;
    private static final long FLOW_BINDER_ID = 11L;

    private final JSONBContext jsonbContext = new JSONBContext();
    private final FlowStoreServiceConnector flowStore = mock(FlowStoreServiceConnector.class);
    private final ServiceBroker serviceBroker = mock(ServiceBroker.class);

    private MessageConsumer messageConsumer;

    @BeforeEach
    void setupMocks() throws FlowStoreServiceConnectorException {
        when(flowStore.getSink(SINK_ID)).thenReturn(newSink());
        when(flowStore.getFlowBinder(FLOW_BINDER_ID)).thenReturn(newFlowBinder());
        messageConsumer = newMessageConsumer();
    }

    @Test
    void anItemTheProcessorFailedIsIgnored() {
        ItemDeliveryResult result = messageConsumer.deliverItem(
                newConsumedMessage(), ChunkItem.failedChunkItem().withId(ITEM_ID));

        assertThat("status", result.status(), is(ItemDeliveryResult.Status.IGNORED));
        assertThat("outcome status", result.chunkItem().getStatus(), is(ChunkItem.Status.IGNORE));
        assertThat("outcome data", StringUtil.asString(result.chunkItem().getData()),
                is("Failed by processor"));
        verifyNoInteractions(serviceBroker);
    }

    @Test
    void anItemTheProcessorIgnoredIsIgnored() {
        ItemDeliveryResult result = messageConsumer.deliverItem(
                newConsumedMessage(), ChunkItem.ignoredChunkItem().withId(ITEM_ID));

        assertThat("status", result.status(), is(ItemDeliveryResult.Status.IGNORED));
        assertThat("outcome status", result.chunkItem().getStatus(), is(ChunkItem.Status.IGNORE));
        assertThat("outcome data", StringUtil.asString(result.chunkItem().getData()),
                is("Ignored by processor"));
        verifyNoInteractions(serviceBroker);
    }

    @Test
    void anItemWhoseProcessingInstructionsCanNotBeReadIsFailed() {
        ChunkItem item = ChunkItem.successfulChunkItem().withId(ITEM_ID)
                .withData(new AddiRecord(StringUtil.asBytes("not JSON"), StringUtil.asBytes("{}")).getBytes());

        ItemDeliveryResult result = messageConsumer.deliverItem(newConsumedMessage(), item);

        assertThat("status", result.status(), is(ItemDeliveryResult.Status.FAILED));
        assertThat("outcome status", result.chunkItem().getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat("outcome diagnostics", result.chunkItem().getDiagnostics(), is(notNullValue()));
    }

    @Test
    void anItemWhoseRecordsReachedLobbyIsDelivered() {
        ItemDeliveryResult result = messageConsumer.deliverItem(
                newConsumedMessage(), newItem(newProcessingInstructions().withErrors(List.of("error"))));

        assertThat("status", result.status(), is(ItemDeliveryResult.Status.DELIVERED));
        assertThat("outcome status", result.chunkItem().getStatus(), is(ChunkItem.Status.SUCCESS));
    }

    @Test
    void anItemHoldingNoAddiRecordIsIgnored() {
        ChunkItem item = ChunkItem.successfulChunkItem().withId(ITEM_ID).withData(new byte[0]);

        ItemDeliveryResult result = messageConsumer.deliverItem(newConsumedMessage(), item);

        assertThat("status", result.status(), is(ItemDeliveryResult.Status.IGNORED));
        assertThat("outcome status", result.chunkItem().getStatus(), is(ChunkItem.Status.IGNORE));
        assertThat("outcome data", StringUtil.asString(result.chunkItem().getData()),
                is("No addi records could be read from the item"));
        verifyNoInteractions(serviceBroker);
    }

    /**
     * A record state none of the processor's three branches match leaves it with nothing to do, so
     * neither the update service nor lobby receives the record and the item must not advance its
     * delivery watermark.
     */
    @Test
    void anItemWhoseRecordStateGivesTheProcessorNothingToDoIsIgnored() {
        ItemDeliveryResult result = messageConsumer.deliverItem(
                newConsumedMessage(), newItem(newProcessingInstructions()));

        assertThat("status", result.status(), is(ItemDeliveryResult.Status.IGNORED));
        assertThat("outcome status", result.chunkItem().getStatus(), is(ChunkItem.Status.IGNORE));
        verifyNoInteractions(serviceBroker);
    }

    /**
     * The id the records of an item are processed under reaches lobby and the update service, so it
     * is asserted through the event log naming it rather than only through the delivery succeeding.
     */
    @Test
    void theRecordIdIsComposedFromTheMessageHeaders() {
        ItemDeliveryResult result = messageConsumer.deliverItem(
                newConsumedMessage(), newItem(newProcessingInstructions().withErrors(List.of("error"))));

        assertThat("outcome data", StringUtil.asString(result.chunkItem().getData()),
                is(JOB_ID + "." + CHUNK_ID + "." + ITEM_ID + "-1: Sent to lobby"));
    }

    private MessageConsumer newMessageConsumer() {
        ServiceHub hub = new ServiceHub.Builder()
                .withJobStoreServiceConnector(mock(JobStoreServiceConnector.class))
                .test();
        return new MessageConsumer(hub, serviceBroker, new ConfigBean(flowStore));
    }

    private ConsumedMessage newConsumedMessage() {
        Map<String, Object> headers = new HashMap<>();
        headers.put(JMSHeader.payload.name, JMSHeader.ITEM_PAYLOAD_TYPE);
        headers.put(JMSHeader.jobId.name, JOB_ID);
        headers.put(JMSHeader.chunkId.name, CHUNK_ID);
        headers.put(JMSHeader.itemId.name, ITEM_ID);
        headers.put(JMSHeader.sinkId.name, SINK_ID);
        headers.put(JMSHeader.sinkVersion.name, 1L);
        headers.put(JMSHeader.flowBinderId.name, FLOW_BINDER_ID);
        headers.put(JMSHeader.flowBinderVersion.name, 1L);
        return new ConsumedMessage("messageId", headers, "");
    }

    private ChunkItem newItem(ProcessingInstructions processingInstructions) {
        try {
            AddiRecord addiRecord = new AddiRecord(
                    StringUtil.asBytes(jsonbContext.marshall(processingInstructions)),
                    MarcRecordFactory.toMarcXchange(newMarcRecord()));
            return ChunkItem.successfulChunkItem().withId(ITEM_ID).withData(addiRecord.getBytes());
        } catch (JSONBException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Creates processing instructions leaving the record state unset, which is what makes an item
     * built from them alone give the processor nothing to do.
     */
    private ProcessingInstructions newProcessingInstructions() {
        return new ProcessingInstructions().withUpdateTemplate("dbcperiodica");
    }

    private MarcRecord newMarcRecord() {
        MarcRecord marcRecord = new MarcRecord();
        marcRecord.addField(new DataField("001", "00").addSubField(new SubField('a', "1234")));
        return marcRecord;
    }

    private Sink newSink() {
        SinkContent sinkContent = new SinkContentBuilder()
                .setSinkType(SinkContent.SinkType.DPF)
                .setSinkConfig(new DpfSinkConfig()
                        .withUpdateServiceUserId("userId")
                        .withUpdateServicePassword("password")
                        .withUpdateServiceAvailableQueueProviders(Collections.singletonList("dpf")))
                .build();
        return new SinkBuilder().setContent(sinkContent).build();
    }

    private FlowBinder newFlowBinder() {
        FlowBinderContent flowBinderContent = new FlowBinderContentBuilder()
                .setQueueProvider("queueProvider")
                .build();
        return new FlowBinderBuilder().setContent(flowBinderContent).build();
    }
}
