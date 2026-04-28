package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.JavaScriptEngine;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.types.Priority;
import dk.dbc.dataio.commons.types.jms.JMSHeader;
import dk.dbc.dataio.commons.utils.test.jms.MockedJmsTextMessage;
import dk.dbc.dataio.commons.utils.test.model.ChunkBuilder;
import dk.dbc.dataio.commons.utils.test.model.FlowBuilder;
import dk.dbc.dataio.commons.utils.test.model.FlowContentBuilder;
import dk.dbc.dataio.jobstore.service.entity.FlowCacheEntity;
import dk.dbc.dataio.jobstore.service.entity.JobEntity;
import dk.dbc.dataio.jobstore.test.types.FlowStoreReferenceBuilder;
import dk.dbc.dataio.jobstore.types.FlowStoreReference;
import dk.dbc.dataio.jobstore.types.FlowStoreReferences;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSContext;
import jakarta.jms.JMSException;
import jakarta.jms.JMSProducer;
import jakarta.jms.JMSRuntimeException;
import jakarta.jms.Message;
import jakarta.jms.Queue;
import jakarta.jms.TextMessage;
import net.jodah.failsafe.RetryPolicy;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.Duration;

import static dk.dbc.commons.testutil.Assert.assertThat;
import static dk.dbc.commons.testutil.Assert.isThrowing;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class JobProcessorMessageProducerBeanTest {
    private final JMSContext jmsContext = mock(JMSContext.class);
    private final JMSProducer jmsProducer = mock(JMSProducer.class);
    private final ConnectionFactory jmsConnectionFactory = mock(ConnectionFactory.class);
    private final JobProcessorMessageProducerBean jobProcessorMessageProducerBean = getInitializedBean();

    public JobProcessorMessageProducerBeanTest() throws Exception {
    }

    @BeforeEach
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

    @Test
    public void sendRetryAndFail() throws Exception {
        when(jmsProducer.send(any(Queue.class), any(Message.class))).thenThrow(new JMSRuntimeException("Argh"));
        JobProcessorMessageProducerBean producerBean = getInitializedBean();
        JobEntity jobEntity = buildJobEntity();
        Chunk chunk = new ChunkBuilder(Chunk.Type.PARTITIONED).setJobId(jobEntity.getId()).build();
        Assert.assertThrows(JMSRuntimeException.class, () -> producerBean.send(chunk, jobEntity, 1));
        verify(jmsProducer, times(4)).send(any(Queue.class), any(Message.class));
    }

    @Test
    public void resolveProcessorQueue_noCachedFlow_returnsNashornQueue() throws Exception {
        JobProcessorMessageProducerBean bean = beanWithQueues("nashorn-queue", "graaljs-queue");
        JobEntity job = new JobEntity();
        assertThat(bean.resolveProcessorQueue(job), is("nashorn-queue"));
    }

    @Test
    public void resolveProcessorQueue_nashornFlow_returnsNashornQueue() throws Exception {
        JobProcessorMessageProducerBean bean = beanWithQueues("nashorn-queue", "graaljs-queue");
        JobEntity job = new JobEntity();
        job.setCachedFlow(flowCacheEntityWithEngine(JavaScriptEngine.NASHORN));
        assertThat(bean.resolveProcessorQueue(job), is("nashorn-queue"));
    }

    @Test
    public void resolveProcessorQueue_graaljsFlow_returnsGraaljsQueue() throws Exception {
        JobProcessorMessageProducerBean bean = beanWithQueues("nashorn-queue", "graaljs-queue");
        JobEntity job = new JobEntity();
        job.setCachedFlow(flowCacheEntityWithEngine(JavaScriptEngine.GRAALJS));
        assertThat(bean.resolveProcessorQueue(job), is("graaljs-queue"));
    }

    private static JobProcessorMessageProducerBean beanWithQueues(String nashornQueue, String graaljsQueue) throws Exception {
        JobProcessorMessageProducerBean bean = new JobProcessorMessageProducerBean(
                new RetryPolicy<>().withDelay(Duration.ofMillis(1)).withMaxRetries(0));
        setField(bean, "nashornQueue", nashornQueue);
        setField(bean, "graaljsQueue", graaljsQueue);
        return bean;
    }

    private static FlowCacheEntity flowCacheEntityWithEngine(JavaScriptEngine engine) throws Exception {
        FlowCacheEntity entity = new FlowCacheEntity();
        setField(entity, "flow", new FlowBuilder()
                .setContent(new FlowContentBuilder().setEngine(engine).build())
                .build());
        return entity;
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private JobProcessorMessageProducerBean getInitializedBean() throws Exception {
        RetryPolicy<Object> retryPolicy = new RetryPolicy<>().withDelay(Duration.ofMillis(1)).withMaxRetries(3);
        JobProcessorMessageProducerBean bean = new JobProcessorMessageProducerBean(retryPolicy);
        bean.connectionFactory = jmsConnectionFactory;
        bean.jsonbContext = new JSONBContext();
        setField(bean, "nashornQueue", "processor::business");
        setField(bean, "graaljsQueue", "processor-graaljs::processor-graaljs");
        bean.queueNameFromJob = bean::resolveProcessorQueue;
        return bean;
    }

    private JobEntity buildJobEntity() {
        JobEntity jobEntity = new JobEntity();
        jobEntity.setSpecification(new JobSpecification().withType(JobSpecification.Type.TEST));
        jobEntity.setFlowStoreReferences(buildFlowStoreReferences());
        return jobEntity;
    }

    private FlowStoreReferences buildFlowStoreReferences() {
        FlowStoreReferences flowStoreReferences = new FlowStoreReferences();
        flowStoreReferences.setReference(FlowStoreReferences.Elements.FLOW, new FlowStoreReferenceBuilder().build());
        return flowStoreReferences;
    }
}
