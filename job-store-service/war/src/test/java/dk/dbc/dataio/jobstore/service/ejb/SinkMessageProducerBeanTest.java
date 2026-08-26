package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.types.Priority;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.jms.JMSHeader;
import dk.dbc.dataio.commons.utils.test.jms.MockedJmsTextMessage;
import dk.dbc.dataio.commons.utils.test.model.ChunkItemBuilder;
import dk.dbc.dataio.commons.utils.test.model.SinkBuilder;
import dk.dbc.dataio.jobstore.service.entity.ItemEntity;
import dk.dbc.dataio.jobstore.service.entity.JobEntity;
import dk.dbc.dataio.jobstore.service.entity.SinkCacheEntity;
import dk.dbc.dataio.jobstore.types.FlowStoreReference;
import dk.dbc.dataio.jobstore.types.FlowStoreReferences;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import dk.dbc.dataio.jobstore.types.MarcRecordInfo;
import dk.dbc.dataio.jobstore.types.RecordInfo;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSContext;
import jakarta.jms.JMSException;
import jakarta.jms.JMSProducer;
import jakarta.jms.Message;
import jakarta.jms.Queue;
import jakarta.jms.TextMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static dk.dbc.commons.testutil.Assert.assertThat;
import static dk.dbc.commons.testutil.Assert.isThrowing;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SinkMessageProducerBeanTest {
    private static final String JMSX_GROUP_ID = "JMSXGroupID";
    private static final int JOB_ID = 42;
    private static final int CHUNK_ID = 3;
    private static final long SUBMITTER_ID = 870970;

    private final ConnectionFactory jmsConnectionFactory = mock(ConnectionFactory.class);
    private final JMSContext jmsContext = mock(JMSContext.class);
    private final JMSProducer jmsProducer = mock(JMSProducer.class);
    private final SinkCacheEntity sinkCacheEntity = mock(SinkCacheEntity.class);
    private final JSONBContext jsonbContext = new JSONBContext();

    private final Sink sink = new SinkBuilder().build();
    private final FlowStoreReferences flowStoreReferences = new FlowStoreReferences();

    {
        flowStoreReferences.setReference(FlowStoreReferences.Elements.SINK,
                new FlowStoreReference(sink.getId(), sink.getVersion(), sink.getContent().getName()));
        flowStoreReferences.setReference(FlowStoreReferences.Elements.FLOW_BINDER,
                new FlowStoreReference(42, 1, "test-binder"));
    }

    private final JobEntity jobEntity = new JobEntity();

    {
        jobEntity.setCachedSink(sinkCacheEntity);
        jobEntity.setFlowStoreReferences(flowStoreReferences);
        jobEntity.setSpecification(new JobSpecification().withSubmitterId(SUBMITTER_ID));
    }

    private final SinkMessageProducerBean sinkMessageProducerBean = getInitializedBean();

    @BeforeEach
    void setupExpectations() {
        when(jmsConnectionFactory.createContext()).thenReturn(jmsContext);
        when(jmsContext.createProducer()).thenReturn(jmsProducer);
        when(jmsContext.createQueue(any(String.class))).thenReturn(mock(Queue.class));
        when(sinkCacheEntity.getSink()).thenReturn(sink);
        when(jmsContext.createTextMessage(any(String.class)))
                .thenAnswer(invocation -> new MockedJmsTextMessage(invocation.getArgument(0)));
    }

    @Test
    void send_itemsArgIsNull_throws() {
        assertThat(() -> sinkMessageProducerBean.send(null, jobEntity, Priority.NORMAL.getValue()),
                isThrowing(NullPointerException.class));
    }

    @Test
    void send_jobEntityArgIsNull_throws() {
        assertThat(() -> sinkMessageProducerBean.send(items(item(0, recordInfo("record-0"))), null,
                        Priority.NORMAL.getValue()),
                isThrowing(NullPointerException.class));
    }

    @Test
    void send_setsMessagePriority() throws JobStoreException {
        sinkMessageProducerBean.send(items(item(0, recordInfo("record-0"))), jobEntity, Priority.NORMAL.getValue());
        verify(jmsProducer).setPriority(Priority.NORMAL.getValue());
    }

    @Test
    void send_sendsOneMessagePerItemInAscendingItemIdOrder() throws JobStoreException, JMSException {
        sinkMessageProducerBean.send(items(
                item(0, recordInfo("record-0")),
                item(1, recordInfo("record-1")),
                item(2, recordInfo("record-2"))), jobEntity, Priority.NORMAL.getValue());

        List<Message> sent = capturedMessages(3);
        assertThat("number of messages sent", sent.size(), is(3));
        for (short itemId = 0; itemId < sent.size(); itemId++) {
            assertThat("itemId of message " + itemId,
                    sent.get(itemId).getObjectProperty(JMSHeader.itemId.name), is(itemId));
        }
    }

    @Test
    void send_itemHasNoProcessingOutcome_throwsBeforeSendingAnything() {
        ItemEntity withoutOutcome = new ItemEntity()
                .withKey(new ItemEntity.Key(JOB_ID, CHUNK_ID, (short) 1))
                .withRecordInfo(recordInfo("record-1"));

        assertThat(() -> sinkMessageProducerBean.send(
                        items(item(0, recordInfo("record-0")), withoutOutcome), jobEntity, Priority.NORMAL.getValue()),
                isThrowing(JobStoreException.class));
        verify(jmsProducer, times(0)).send(any(Queue.class), any(Message.class));
    }

    @Test
    void createItemMessage_setsFullHeaderSet() throws JMSException, JSONBException {
        TextMessage message = createItemMessage(item(1, recordInfo("record-1")));

        FlowStoreReference sinkReference = flowStoreReferences.getReference(FlowStoreReferences.Elements.SINK);
        FlowStoreReference flowBinderReference = flowStoreReferences.getReference(FlowStoreReferences.Elements.FLOW_BINDER);
        assertThat("payload", message.getObjectProperty(JMSHeader.payload.name), is(JMSHeader.ITEM_PAYLOAD_TYPE));
        assertThat("jobId", message.getObjectProperty(JMSHeader.jobId.name), is(JOB_ID));
        assertThat("chunkId", message.getObjectProperty(JMSHeader.chunkId.name), is((long) CHUNK_ID));
        assertThat("trackingId", message.getObjectProperty(JMSHeader.trackingId.name), is(JOB_ID + "/" + CHUNK_ID));
        assertThat("itemId", message.getObjectProperty(JMSHeader.itemId.name), is((short) 1));
        assertThat("sinkId", message.getObjectProperty(JMSHeader.sinkId.name), is(sinkReference.getId()));
        assertThat("sinkVersion", message.getObjectProperty(JMSHeader.sinkVersion.name), is(sinkReference.getVersion()));
        assertThat("flowBinderId", message.getObjectProperty(JMSHeader.flowBinderId.name), is(flowBinderReference.getId()));
        assertThat("flowBinderVersion", message.getObjectProperty(JMSHeader.flowBinderVersion.name), is(flowBinderReference.getVersion()));
        assertThat("recordKey", message.getObjectProperty(JMSHeader.recordKey.name), is(SUBMITTER_ID + ":record-1"));
        assertThat("JMSXGroupID", message.getObjectProperty(JMSX_GROUP_ID), is("record-1"));
    }

    @Test
    void createItemMessage_jobHasNoFlowBinderReference_omitsFlowBinderHeaders() throws JMSException, JSONBException {
        FlowStoreReferences withoutFlowBinder = new FlowStoreReferences();
        withoutFlowBinder.setReference(FlowStoreReferences.Elements.SINK,
                new FlowStoreReference(sink.getId(), sink.getVersion(), sink.getContent().getName()));

        TextMessage message = sinkMessageProducerBean.createItemMessage(
                jmsContext, item(0, recordInfo("record-0")), withoutFlowBinder, SUBMITTER_ID);

        assertThat("flowBinderId", message.getObjectProperty(JMSHeader.flowBinderId.name), is(nullValue()));
        assertThat("flowBinderVersion", message.getObjectProperty(JMSHeader.flowBinderVersion.name), is(nullValue()));
    }

    @Test
    void createItemMessage_bodyIsTheItemProcessingOutcome() throws JMSException, JSONBException {
        ItemEntity item = item(0, recordInfo("record-0"));
        TextMessage message = createItemMessage(item);

        ChunkItem unmarshalled = jsonbContext.unmarshall(message.getText(), ChunkItem.class);
        assertThat("item id", unmarshalled.getId(), is(item.getProcessingOutcome().getId()));
        assertThat("item data", new String(unmarshalled.getData()), is("item 0"));
    }

    @Test
    void createItemMessage_recordIdNeedsNormalisation_recordKeyCarriesTheNormalisedForm() throws JMSException, JSONBException {
        TextMessage message = createItemMessage(item(0, recordInfo(" 4 2 ")));

        assertThat("recordKey", message.getObjectProperty(JMSHeader.recordKey.name), is(SUBMITTER_ID + ":42"));
    }

    @Test
    void createItemMessage_recordInfoIsNull_omitsRecordKeyAndGroupId() throws JMSException, JSONBException {
        TextMessage message = createItemMessage(item(0, null));

        assertThat("recordKey", message.getObjectProperty(JMSHeader.recordKey.name), is(nullValue()));
        assertThat("JMSXGroupID", message.getObjectProperty(JMSX_GROUP_ID), is(nullValue()));
    }

    @Test
    void createItemMessage_terminationItemHasNoRecordId_omitsRecordKeyAndGroupId() throws JMSException, JSONBException {
        TextMessage message = createItemMessage(item(0, new RecordInfo(null)));

        assertThat("recordKey", message.getObjectProperty(JMSHeader.recordKey.name), is(nullValue()));
        assertThat("JMSXGroupID", message.getObjectProperty(JMSX_GROUP_ID), is(nullValue()));
    }

    @Test
    void createItemMessage_hierarchyRecord_groupIdIsTheSharedHierarchyKey() throws JMSException, JSONBException {
        RecordInfo volume = new MarcRecordInfo("volume-1", MarcRecordInfo.RecordType.VOLUME, false, "head-1");
        TextMessage message = createItemMessage(item(0, volume));

        assertThat("JMSXGroupID", message.getObjectProperty(JMSX_GROUP_ID),
                is(MarcRecordInfo.HIERARCHY_CORRELATION_KEY));
        assertThat("recordKey", message.getObjectProperty(JMSHeader.recordKey.name),
                is(SUBMITTER_ID + ":volume-1"));
    }

    @Test
    void sendAbort_keepsAbortPayloadType() throws JobStoreException, JMSException {
        when(jmsContext.createTextMessage()).thenReturn(new MockedJmsTextMessage());

        sinkMessageProducerBean.sendAbort(jobEntity);

        Message message = capturedMessages(1).get(0);
        assertThat("payload", message.getObjectProperty(JMSHeader.payload.name), is(JMSHeader.ABORT_PAYLOAD_TYPE));
        assertThat("abortId", message.getObjectProperty(JMSHeader.abortId.name), is(jobEntity.getId()));
    }

    private TextMessage createItemMessage(ItemEntity item) throws JMSException, JSONBException {
        return sinkMessageProducerBean.createItemMessage(jmsContext, item, flowStoreReferences, SUBMITTER_ID);
    }

    private List<Message> capturedMessages(int expectedNumberOfMessages) {
        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(jmsProducer, times(expectedNumberOfMessages)).send(any(Queue.class), captor.capture());
        return captor.getAllValues();
    }

    private List<ItemEntity> items(ItemEntity... items) {
        return List.of(items);
    }

    private ItemEntity item(int itemId, RecordInfo recordInfo) {
        return new ItemEntity()
                .withKey(new ItemEntity.Key(JOB_ID, CHUNK_ID, (short) itemId))
                .withProcessingOutcome(new ChunkItemBuilder().setId(itemId).setData("item " + itemId).build())
                .withRecordInfo(recordInfo);
    }

    private RecordInfo recordInfo(String id) {
        return new RecordInfo(id);
    }

    private SinkMessageProducerBean getInitializedBean() {
        SinkMessageProducerBean sinkMessageProducerBean = new SinkMessageProducerBean();
        sinkMessageProducerBean.connectionFactory = jmsConnectionFactory;
        return sinkMessageProducerBean;
    }
}
