package dk.dbc.dataio.jobprocessor.ejb;

import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.ExternalChunk;
import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;
import dk.dbc.dataio.commons.types.jms.JmsConstants;
import dk.dbc.dataio.commons.utils.test.jms.MockedJmsMessageDrivenContext;
import dk.dbc.dataio.commons.utils.test.jms.MockedJmsTextMessage;
import dk.dbc.dataio.commons.utils.test.model.ChunkItemBuilder;
import dk.dbc.dataio.commons.utils.test.model.ExternalChunkBuilder;
import dk.dbc.dataio.jobprocessor.exception.JobProcessorException;
import dk.dbc.dataio.jsonb.JSONBContext;
import dk.dbc.dataio.jsonb.JSONBException;
import org.junit.Test;

import javax.ejb.MessageDrivenContext;
import javax.jms.JMSException;
import java.util.Arrays;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.powermock.api.mockito.PowerMockito.doThrow;
import static org.powermock.api.mockito.PowerMockito.mock;

public class SinkMessageConsumerBeanTest {
    private JobStoreMessageProducerBean jobStoreMessageProducer = mock(JobStoreMessageProducerBean.class);

    @Test
    public void onMessage_messageArgPayloadIsInvalidSinkResult_noTransactionRollback() throws JMSException {
        final TestableSinkMessageConsumerBean sinkMessageConsumerBean = getInitializedBean();
        final MockedJmsTextMessage textMessage = new MockedJmsTextMessage();
        textMessage.setStringProperty(JmsConstants.PAYLOAD_PROPERTY_NAME, JmsConstants.CHUNK_PAYLOAD_TYPE);
        textMessage.setText("{'invalid': 'instance'}");
        sinkMessageConsumerBean.onMessage(textMessage);
        assertThat("RollbackOnly", sinkMessageConsumerBean.getMessageDrivenContext().getRollbackOnly(), is(false));
    }

    @Test
    public void handleConsumedMessage_messageArgPayloadIsInvalidSinkResult_throws() throws JobProcessorException, JMSException, InvalidMessageException {
        final ConsumedMessage consumedMessage = new ConsumedMessage("id", JmsConstants.CHUNK_PAYLOAD_TYPE, "{'invalid': 'instance'}");
        final TestableSinkMessageConsumerBean sinkMessageConsumerBean = getInitializedBean();
        try {
            sinkMessageConsumerBean.handleConsumedMessage(consumedMessage);
            fail("No exception thrown");
        } catch (InvalidMessageException e) {
        }
    }
    
    @Test
    public void handleConsumedMessage_messageChunkIsOfIncorrectType_throws() throws JobProcessorException, InvalidMessageException, JSONBException {
        final ChunkItem item = new ChunkItemBuilder().setData("This is some data").setStatus(ChunkItem.Status.SUCCESS).build();
        // The Chunk-type 'processed' is not allowed in the JobProcessor, only 'delivered' is allowed.
        final ExternalChunk chunk = new ExternalChunkBuilder(ExternalChunk.Type.PROCESSED).setItems(Arrays.asList(item)).build();
        final String jsonChunk = new JSONBContext().marshall(chunk);

        final ConsumedMessage message = new ConsumedMessage("id", JmsConstants.CHUNK_PAYLOAD_TYPE, jsonChunk);
        final SinkMessageConsumerBean sinkMessageConsumerBean = getInitializedBean();
        try {
            sinkMessageConsumerBean.handleConsumedMessage(message);
            fail("No exception thrown");
        } catch (InvalidMessageException e) {
        }
    }

    @Test
    public void onMessage_messageArgPayloadIsValidSinkResult_handled() throws JMSException, JSONBException {
        final TestableSinkMessageConsumerBean sinkMessageConsumerBean = getInitializedBean();
        final MockedJmsTextMessage textMessage = new MockedJmsTextMessage();
        textMessage.setStringProperty(JmsConstants.PAYLOAD_PROPERTY_NAME, JmsConstants.CHUNK_PAYLOAD_TYPE);
        final ExternalChunk deliveredChunk = new ExternalChunkBuilder(ExternalChunk.Type.DELIVERED).build();
        final String deliveredChunkJson = new JSONBContext().marshall(deliveredChunk);
        textMessage.setText(deliveredChunkJson);
        sinkMessageConsumerBean.onMessage(textMessage);
        assertThat("RollbackOnly", sinkMessageConsumerBean.getMessageDrivenContext().getRollbackOnly(), is(false));
    }

    @Test
    public void onMessage_handlingThrowsJobProcessorException_transactionRollback() throws JMSException, JobProcessorException, JSONBException {
        doThrow(new JobProcessorException("JobProcessorException")).when(jobStoreMessageProducer).sendSink(any(ExternalChunk.class));
        final TestableSinkMessageConsumerBean sinkMessageConsumerBean = getInitializedBean();
        final MockedJmsTextMessage textMessage = new MockedJmsTextMessage();
        textMessage.setStringProperty(JmsConstants.PAYLOAD_PROPERTY_NAME, JmsConstants.CHUNK_PAYLOAD_TYPE);
        final ExternalChunk deliveredChunk = new ExternalChunkBuilder(ExternalChunk.Type.DELIVERED).build();
        final String deliveredChunkJson = new JSONBContext().marshall(deliveredChunk);
        textMessage.setText(deliveredChunkJson);
        try {
            sinkMessageConsumerBean.onMessage(textMessage);
            fail("No exception thrown");
        } catch (IllegalStateException e) {
        }
        assertThat("RollbackOnly", sinkMessageConsumerBean.getMessageDrivenContext().getRollbackOnly(), is(false));
    }

    private TestableSinkMessageConsumerBean getInitializedBean() {
        final TestableSinkMessageConsumerBean sinkMessageConsumerBean = new TestableSinkMessageConsumerBean();
        sinkMessageConsumerBean.setMessageDrivenContext(new MockedJmsMessageDrivenContext());
        sinkMessageConsumerBean.jobStoreMessageProducer = jobStoreMessageProducer;
        return sinkMessageConsumerBean;
    }

    private static class TestableSinkMessageConsumerBean extends SinkMessageConsumerBean {
        public MessageDrivenContext getMessageDrivenContext() {
            return messageDrivenContext;
        }
        public void setMessageDrivenContext(MessageDrivenContext messageDrivenContext) {
            this.messageDrivenContext = messageDrivenContext;
        }
    }
}


