package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.types.Priority;
import dk.dbc.dataio.commons.types.jms.JMSHeader;
import dk.dbc.dataio.commons.utils.test.jms.MockedJmsTextMessage;
import dk.dbc.dataio.commons.utils.test.model.ChunkBuilder;
import dk.dbc.dataio.jobstore.service.entity.JobEntity;
import dk.dbc.dataio.jobstore.test.types.FlowStoreReferenceBuilder;
import dk.dbc.dataio.jobstore.types.FlowStoreReference;
import dk.dbc.dataio.jobstore.types.FlowStoreReferences;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import org.junit.Before;
import org.junit.Test;

import javax.jms.ConnectionFactory;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.JMSProducer;
import javax.jms.Queue;
import javax.jms.TextMessage;

import static dk.dbc.commons.testutil.Assert.assertThat;
import static dk.dbc.commons.testutil.Assert.isThrowing;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class JobProcessorMessageProducerBeanTest {
    private final JMSContext jmsContext = mock(JMSContext.class);
    private final JMSProducer jmsProducer = mock(JMSProducer.class);
    private final ConnectionFactory jmsConnectionFactory = mock(ConnectionFactory.class);
    private final JobProcessorMessageProducerBean jobProcessorMessageProducerBean = getInitializedBean();

    @Before
    public void setupMocks() {
        when(jmsConnectionFactory.createContext()).thenReturn(jmsContext);
        when(jmsContext.createProducer()).thenReturn(jmsProducer);
        when(jmsContext.createQueue(any(String.class))).thenReturn(mock(Queue.class));
        when(jmsContext.createTextMessage(any(String.class))).thenReturn(new MockedJmsTextMessage());
    }

    @Test
    public void send_chunkArgIsNull_throws() {
        assertThat(() -> jobProcessorMessageProducerBean.send(null, new JobEntity(), Priority.NORMAL.getValue()),
                isThrowing(NullPointerException.class));
    }

    @Test
    public void send_jobEntityArgIsNull_throws() {
        assertThat(() -> jobProcessorMessageProducerBean.send(new ChunkBuilder(Chunk.Type.PROCESSED).build(), null, Priority.NORMAL.getValue()),
                isThrowing(NullPointerException.class));
    }

    @Test
    public void send_setsMessagePriority() throws JobStoreException {
        jobProcessorMessageProducerBean.send(new ChunkBuilder(Chunk.Type.PARTITIONED).build(),
                buildJobEntity(), Priority.NORMAL.getValue());
        verify(jmsProducer).setPriority(Priority.NORMAL.getValue());
    }

    @Test
    public void createMessage_chunkArgIsValid_returnsMessageWithHeaderProperties() throws JSONBException, JMSException {
        JobEntity jobEntity = buildJobEntity();

        // Subject under test
        Chunk chunk = new ChunkBuilder(Chunk.Type.PARTITIONED).setJobId(jobEntity.getId()).build();
        TextMessage message = jobProcessorMessageProducerBean.createMessage(jmsContext, chunk, jobEntity);

        // Verification
        assertThat(JMSHeader.payload.getHeader(message), is(JMSHeader.CHUNK_PAYLOAD_TYPE));

        FlowStoreReference flowReference = jobEntity.getFlowStoreReferences().getReference(FlowStoreReferences.Elements.FLOW);
        assertThat(JMSHeader.flowId.getHeader(message), is(flowReference.getId()));
        assertThat(JMSHeader.flowVersion.getHeader(message), is(flowReference.getVersion()));
        assertThat(JMSHeader.jobId.getHeader(message), is(jobEntity.getId()));
        assertThat(JMSHeader.chunkId.getHeader(message), is(chunk.getChunkId()));
        assertThat(JMSHeader.trackingId.getHeader(message), notNullValue());

        JobSpecification jobSpecification = jobEntity.getSpecification();
        assertThat(JMSHeader.additionalArgs.getHeader(message, String.class).contains(String.valueOf(jobSpecification.getSubmitterId())), is(true));
        assertThat(JMSHeader.additionalArgs.getHeader(message, String.class).contains(String.valueOf(jobSpecification.getFormat())), is(true));
    }

    private JobProcessorMessageProducerBean getInitializedBean() {
        JobProcessorMessageProducerBean jobProcessorMessageProducerBean = new JobProcessorMessageProducerBean();
        jobProcessorMessageProducerBean.connectionFactory = jmsConnectionFactory;
        jobProcessorMessageProducerBean.jsonbContext = new JSONBContext();
        return jobProcessorMessageProducerBean;
    }

    private JobEntity buildJobEntity() {
        JobEntity jobEntity = new JobEntity();
        jobEntity.setSpecification(new JobSpecification().withType(JobSpecification.Type.ACCTEST));
        jobEntity.setFlowStoreReferences(buildFlowStoreReferences());
        return jobEntity;
    }

    private FlowStoreReferences buildFlowStoreReferences() {
        FlowStoreReferences flowStoreReferences = new FlowStoreReferences();
        flowStoreReferences.setReference(FlowStoreReferences.Elements.FLOW, new FlowStoreReferenceBuilder().build());
        return flowStoreReferences;
    }
}
