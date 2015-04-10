package dk.dbc.dataio.jobprocessor.ejb;

import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.ExternalChunk;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;
import dk.dbc.dataio.commons.types.jms.JmsConstants;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.commons.utils.jobstore.ejb.JobStoreServiceConnectorBean;
import dk.dbc.dataio.commons.utils.service.Base64Util;
import dk.dbc.dataio.commons.utils.test.jms.MockedJmsMessageDrivenContext;
import dk.dbc.dataio.commons.utils.test.jms.MockedJmsProducer;
import dk.dbc.dataio.commons.utils.test.jms.MockedJmsTextMessage;
import dk.dbc.dataio.commons.utils.test.model.ChunkItemBuilder;
import dk.dbc.dataio.commons.utils.test.model.ExternalChunkBuilder;
import dk.dbc.dataio.commons.utils.test.model.SinkBuilder;
import dk.dbc.dataio.commons.utils.test.model.SupplementaryProcessDataBuilder;
import dk.dbc.dataio.jobprocessor.exception.JobProcessorException;
import dk.dbc.dataio.jobstore.types.ResourceBundle;
import dk.dbc.dataio.jsonb.JSONBContext;
import dk.dbc.dataio.jsonb.JSONBException;
import dk.dbc.dataio.jsonb.ejb.JSONBBean;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import javax.ejb.MessageDrivenContext;
import javax.jms.ConnectionFactory;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import java.util.Arrays;
import java.util.Iterator;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JobStoreMessageConsumerBeanTest {
    private JobStoreServiceConnectorBean jobStoreServiceConnectorBean = mock(JobStoreServiceConnectorBean.class);
    private JobStoreServiceConnector jobStoreServiceConnector = mock(JobStoreServiceConnector.class);
    private ConnectionFactory jmsConnectionFactory = mock(ConnectionFactory.class);
    private JMSContext jmsContext = mock(JMSContext.class);
    private MockedJmsProducer jmsProducer = new MockedJmsProducer();
    private JSONBBean jsonBinding = new JSONBBean();

    @Before
    public void setupMocks() {
        when(jobStoreServiceConnectorBean.getConnector()).thenReturn(jobStoreServiceConnector);
        when(jmsConnectionFactory.createContext()).thenReturn(jmsContext);
        when(jmsContext.createProducer()).thenReturn(jmsProducer);
    }

    @After
    public void clearMocks() {
        jmsProducer.clearMessages();
    }

    @Test
    public void onMessage_messageArgPayloadIsInvalidNewJob_noTransactionRollback() throws JMSException {
        final TestableJobStoreMessageConsumerBean jobStoreMessageConsumerBean = getInitializedBean();
        final MockedJmsTextMessage textMessage = new MockedJmsTextMessage();
        textMessage.setStringProperty(JmsConstants.PAYLOAD_PROPERTY_NAME, JmsConstants.CHUNK_PAYLOAD_TYPE);
        textMessage.setText("{'invalid': 'instance'}");
        jobStoreMessageConsumerBean.onMessage(textMessage);
        assertThat("RollbackOnly", jobStoreMessageConsumerBean.getMessageDrivenContext().getRollbackOnly(), is(false));
    }

    @Test
    public void handleConsumedMessage_messageArgPayloadIsInvalidNewJob_throws() throws JobProcessorException, JMSException, InvalidMessageException {
        final ConsumedMessage consumedMessage = new ConsumedMessage("id", JmsConstants.CHUNK_PAYLOAD_TYPE, "{'invalid': 'instance'}");
        final TestableJobStoreMessageConsumerBean jobStoreMessageConsumerBean = getInitializedBean();
        try {
            jobStoreMessageConsumerBean.handleConsumedMessage(consumedMessage);
            fail("No exception thrown");
        } catch (InvalidMessageException e) {
        }
    }

    @Test
    public void handleConsumedMessage_messagePayloadCanNotBeUnmarshalledToJson_throws() throws JobProcessorException, InvalidMessageException {
        final ConsumedMessage message = new ConsumedMessage("id", JmsConstants.CHUNK_PAYLOAD_TYPE, "invalid");
        final JobStoreMessageConsumerBean jobStoreMessageConsumerBean = getInitializedBean();
        try {
            jobStoreMessageConsumerBean.handleConsumedMessage(message);
            fail("No exception thrown");
        } catch (InvalidMessageException e) {
        }
    }

    @Test
    public void handleConsumedMessage_messageChunkIsOfIncorrectType_throws() throws JobProcessorException, InvalidMessageException, JMSException, JSONBException {
        final ChunkItem item = new ChunkItemBuilder().setData("This is some data").setStatus(ChunkItem.Status.SUCCESS).build();
        // The Chunk-type 'processed' is not allowed in the JobProcessor, only 'partitioned' is allowed.
        final ExternalChunk chunk = new ExternalChunkBuilder(ExternalChunk.Type.PROCESSED).setItems(Arrays.asList(item)).build();
        final String jsonChunk = new JSONBContext().marshall(chunk);

        final JobStoreMessageConsumerBean jobStoreMessageConsumerBean = getInitializedBean();
        final ConsumedMessage message = new ConsumedMessage("id", JmsConstants.CHUNK_PAYLOAD_TYPE, jsonChunk);
        try {
            jobStoreMessageConsumerBean.handleConsumedMessage(message);
            fail("No exception thrown");
        } catch (InvalidMessageException e) {
        }
    }

    @Test
    public void handleConsumedMessage_happyPath() throws Exception {
        when(jmsContext.createTextMessage(any(String.class)))
                .thenAnswer(new Answer<MockedJmsTextMessage>() {
                    @Override
                    public MockedJmsTextMessage answer(InvocationOnMock invocation) throws Throwable {
                        final Object[] args = invocation.getArguments();
                        final MockedJmsTextMessage message = new MockedJmsTextMessage();
                        message.setText((String) args[0]);
                        return message;
                    }
                })
                .thenAnswer(new Answer<MockedJmsTextMessage>() {
                    @Override
                    public MockedJmsTextMessage answer(InvocationOnMock invocation) throws Throwable {
                        final Object[] args = invocation.getArguments();
                        final MockedJmsTextMessage message = new MockedJmsTextMessage();
                        message.setText((String) args[0]);
                        return message;
                    }
                });

        final ChunkProcessorBeanTest jsFactory = new ChunkProcessorBeanTest();
        final Flow flow = jsFactory.getFlow(new ChunkProcessorBeanTest.ScriptWrapper(jsFactory.javaScriptReturnUpperCase,
                jsFactory.getJavaScript(jsFactory.getJavaScriptReturnUpperCaseFunction())));
        final Sink sink = new SinkBuilder().build();

        final ResourceBundle resourceBundle = new ResourceBundle(flow, sink, new SupplementaryProcessDataBuilder().build());
        when(jobStoreServiceConnector.getResourceBundle(anyInt())).thenReturn(resourceBundle);

        final ChunkItem item = new ChunkItemBuilder().setData(Base64Util.base64encode("This is some data")).setStatus(ChunkItem.Status.SUCCESS).build();
        final ExternalChunk chunk = new ExternalChunkBuilder(ExternalChunk.Type.PARTITIONED).setItems(Arrays.asList(item)).build();
        final String jsonChunk = new JSONBContext().marshall(chunk);

        final JobStoreMessageConsumerBean jobStoreMessageConsumerBean = getInitializedBean();
        final ConsumedMessage message = new ConsumedMessage("id", JmsConstants.CHUNK_PAYLOAD_TYPE, jsonChunk);
        jobStoreMessageConsumerBean.handleConsumedMessage(message);

        assertThat("Number of JMS messages", jmsProducer.messages.size(), is(2));
        assertChunk(chunk, assertProcessorMessageForJobStore(jmsProducer.messages.pop()));
        assertChunk(chunk, assertProcessorMessageForSink(jmsProducer.messages.pop(), sink.getContent().getResource()));
    }

    private ExternalChunk assertProcessorMessageForJobStore(MockedJmsTextMessage message) throws JMSException, JSONBException {
        assertThat("processor JMS msg", message, is(notNullValue()));
        assertThat("processor JMS msg source", message.getStringProperty(JmsConstants.SOURCE_PROPERTY_NAME), is(JmsConstants.PROCESSOR_SOURCE_VALUE));
        assertThat("processor JMS msg payload", message.getStringProperty(JmsConstants.PAYLOAD_PROPERTY_NAME), is(JmsConstants.CHUNK_PAYLOAD_TYPE));
        return jsonBinding.getContext().unmarshall(message.getText(), ExternalChunk.class);
    }

    private ExternalChunk assertProcessorMessageForSink(MockedJmsTextMessage message, String resource) throws JMSException, JSONBException {
        assertThat("sink JMS msg", message, is(notNullValue()));
        assertThat("sink JMS msg source", message.getStringProperty(JmsConstants.SOURCE_PROPERTY_NAME), is(JmsConstants.PROCESSOR_SOURCE_VALUE));
        assertThat("sink JMS msg payload", message.getStringProperty(JmsConstants.PAYLOAD_PROPERTY_NAME), is(JmsConstants.CHUNK_PAYLOAD_TYPE));
        assertThat("sink JMS msg resource", message.getStringProperty(JmsConstants.RESOURCE_PROPERTY_NAME), is(resource));
        return jsonBinding.getContext().unmarshall(message.getText(), ExternalChunk.class);
    }

    private void assertChunk(ExternalChunk in, ExternalChunk out) {
        assertThat("chunk type", out.getType(), is(ExternalChunk.Type.PROCESSED));
        assertThat("chunk jobId", out.getJobId(), is(in.getJobId()));
        assertThat("chunk chunkId", out.getChunkId(), is(in.getChunkId()));
        assertThat("chunk size", out.size(), is(in.size()));
        final Iterator<ChunkItem> inIterator = in.iterator();
        for (ChunkItem item : out) {
            assertThat("chunk item data", Base64Util.base64decode(item.getData()), is(Base64Util.base64decode(inIterator.next().getData()).toUpperCase()));
        }
    }

    private TestableJobStoreMessageConsumerBean getInitializedBean() {
        final TestableJobStoreMessageConsumerBean jobStoreMessageConsumerBean = new TestableJobStoreMessageConsumerBean();
        jobStoreMessageConsumerBean.setMessageDrivenContext(new MockedJmsMessageDrivenContext());
        jsonBinding.initialiseContext();
        jobStoreMessageConsumerBean.jsonBinding = jsonBinding;
        jobStoreMessageConsumerBean.jobStoreServiceConnector = jobStoreServiceConnectorBean;
        final ChunkProcessorBean chunkProcessorBean = new ChunkProcessorBean();
        chunkProcessorBean.jsonBinding = jsonBinding;
        jobStoreMessageConsumerBean.chunkProcessor = chunkProcessorBean;
        final JobStoreMessageProducerBean jobStoreMessageProducerBean = new JobStoreMessageProducerBean();
        jobStoreMessageProducerBean.jobStoreQueueConnectionFactory = jmsConnectionFactory;
        jobStoreMessageProducerBean.jsonBinding = jsonBinding;
        jobStoreMessageConsumerBean.jobStoreMessageProducer = jobStoreMessageProducerBean;
        final SinkMessageProducerBean sinkMessageProducerBean = new SinkMessageProducerBean();
        sinkMessageProducerBean.sinksQueueConnectionFactory = jmsConnectionFactory;
        sinkMessageProducerBean.jsonBinding = jsonBinding;
        jobStoreMessageConsumerBean.sinkMessageProducer = sinkMessageProducerBean;
        return jobStoreMessageConsumerBean;
    }

    private static class TestableJobStoreMessageConsumerBean extends JobStoreMessageConsumerBean {
        public MessageDrivenContext getMessageDrivenContext() {
            return messageDrivenContext;
        }
        public void setMessageDrivenContext(MessageDrivenContext messageDrivenContext) {
            this.messageDrivenContext = messageDrivenContext;
        }
    }
}


