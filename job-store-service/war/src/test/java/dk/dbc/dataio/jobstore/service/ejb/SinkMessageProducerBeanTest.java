package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.Priority;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.jms.JmsConstants;
import dk.dbc.dataio.commons.utils.test.jms.MockedJmsTextMessage;
import dk.dbc.dataio.commons.utils.test.model.ChunkBuilder;
import dk.dbc.dataio.commons.utils.test.model.SinkBuilder;
import dk.dbc.dataio.jobstore.service.entity.JobEntity;
import dk.dbc.dataio.jobstore.service.entity.SinkCacheEntity;
import dk.dbc.dataio.jobstore.types.FlowStoreReference;
import dk.dbc.dataio.jobstore.types.FlowStoreReferences;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSContext;
import jakarta.jms.JMSException;
import jakarta.jms.JMSProducer;
import jakarta.jms.TextMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static dk.dbc.commons.testutil.Assert.assertThat;
import static dk.dbc.commons.testutil.Assert.isThrowing;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SinkMessageProducerBeanTest {
    private ConnectionFactory jmsConnectionFactory = mock(ConnectionFactory.class);
    private JMSContext jmsContext = mock(JMSContext.class);
    private final JMSProducer jmsProducer = mock(JMSProducer.class);
    private final SinkCacheEntity sinkCacheEntity = mock(SinkCacheEntity.class);

    private final Chunk chunk = new ChunkBuilder(Chunk.Type.PROCESSED).build();
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
    }

    private final SinkMessageProducerBean sinkMessageProducerBean = getInitializedBean();

    @BeforeEach
    public void setupExpectations() {
        when(jmsConnectionFactory.createContext()).thenReturn(jmsContext);
        when(jmsContext.createProducer()).thenReturn(jmsProducer);
        when(sinkCacheEntity.getSink()).thenReturn(sink);
        when(jmsContext.createTextMessage(any(String.class))).thenReturn(new MockedJmsTextMessage());
    }

    @Test
    public void send_chunkArgIsNull_throws() {
        assertThat(() -> sinkMessageProducerBean.send(null, jobEntity, Priority.NORMAL.getValue()),
                isThrowing(NullPointerException.class));
    }

    @Test
    public void send_jobEntityArgIsNull_throws() {
        assertThat(() -> sinkMessageProducerBean.send(chunk, null, Priority.NORMAL.getValue()),
                isThrowing(NullPointerException.class));
    }

    @Test
    public void send_setsMessagePriority() throws JobStoreException {
        sinkMessageProducerBean.send(chunk, jobEntity, Priority.NORMAL.getValue());
        verify(jmsProducer).setPriority(Priority.NORMAL.getValue());
    }

    @Test
    public void createMessage_chunkArgIsValid_returnsMessageWithHeaderProperties() throws JMSException, JSONBException {
        // Subject Under Test
        TextMessage message = sinkMessageProducerBean.createMessage(jmsContext, chunk, flowStoreReferences);

        // Verifications
        FlowStoreReference sinkReference = flowStoreReferences.getReference(FlowStoreReferences.Elements.SINK);
        FlowStoreReference flowBinderReference = flowStoreReferences.getReference(FlowStoreReferences.Elements.FLOW_BINDER);
        assertThat("Message payload property", message.getStringProperty(JmsConstants.PAYLOAD_PROPERTY_NAME), is(JmsConstants.CHUNK_PAYLOAD_TYPE));
        assertThat("Message id property", message.getLongProperty(JmsConstants.SINK_ID_PROPERTY_NAME), is(sinkReference.getId()));
        assertThat("Message version property", message.getLongProperty(JmsConstants.SINK_VERSION_PROPERTY_NAME), is(sinkReference.getVersion()));
        assertThat("Message flowBinderId property", message.getLongProperty(JmsConstants.FLOW_BINDER_ID_PROPERTY_NAME), is(flowBinderReference.getId()));
        assertThat("Message flowBinderVersion property", message.getLongProperty(JmsConstants.FLOW_BINDER_VERSION_PROPERTY_NAME), is(flowBinderReference.getVersion()));
    }

    private SinkMessageProducerBean getInitializedBean() {
        SinkMessageProducerBean sinkMessageProducerBean = new SinkMessageProducerBean();
        sinkMessageProducerBean.connectionFactory = jmsConnectionFactory;
        return sinkMessageProducerBean;
    }
}
