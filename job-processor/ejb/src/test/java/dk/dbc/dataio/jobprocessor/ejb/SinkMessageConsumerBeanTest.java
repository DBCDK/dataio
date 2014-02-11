package dk.dbc.dataio.jobprocessor.ejb;

import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.SinkChunkResult;
import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;
import dk.dbc.dataio.commons.types.jms.JmsConstants;
import dk.dbc.dataio.commons.utils.test.jms.MockedJmsMessageDrivenContext;
import dk.dbc.dataio.commons.utils.test.jms.MockedJmsTextMessage;
import dk.dbc.dataio.commons.utils.test.json.SinkChunkResultJsonBuilder;
import dk.dbc.dataio.jobprocessor.exception.JobProcessorException;
import org.junit.Test;

import javax.ejb.MessageDrivenContext;
import javax.jms.JMSException;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.powermock.api.mockito.PowerMockito.doThrow;
import static org.powermock.api.mockito.PowerMockito.mock;

public class SinkMessageConsumerBeanTest {
    private JobStoreMessageProducerBean jobStoreMessageProducer = mock(JobStoreMessageProducerBean.class);

    @Test
    public void onMessage_messageArgPayloadIsInvalidSinkResult_noTransactionRollback() throws JMSException {
        final TestableSinkMessageConsumerBean sinkMessageConsumerBean = getInitializedBean();
        final MockedJmsTextMessage textMessage = new MockedJmsTextMessage();
        textMessage.setStringProperty(JmsConstants.PAYLOAD_PROPERTY_NAME, JmsConstants.SINK_RESULT_PAYLOAD_TYPE);
        textMessage.setText("{'invalid': 'instance'}");
        sinkMessageConsumerBean.onMessage(textMessage);
        assertThat(sinkMessageConsumerBean.getMessageDrivenContext().getRollbackOnly(), is(false));
    }

    @Test(expected = InvalidMessageException.class)
    public void handleConsumedMessage_messageArgPayloadIsInvalidSinkResult_throws() throws JobProcessorException, JMSException, InvalidMessageException {
        final ConsumedMessage consumedMessage = new ConsumedMessage("id", JmsConstants.SINK_RESULT_PAYLOAD_TYPE, "{'invalid': 'instance'}");
        getInitializedBean().handleConsumedMessage(consumedMessage);
    }

    @Test
    public void onMessage_messageArgPayloadIsValidSinkResult_handled() throws JMSException {
        final TestableSinkMessageConsumerBean sinkMessageConsumerBean = getInitializedBean();
        final MockedJmsTextMessage textMessage = new MockedJmsTextMessage();
        textMessage.setStringProperty(JmsConstants.PAYLOAD_PROPERTY_NAME, JmsConstants.SINK_RESULT_PAYLOAD_TYPE);
        textMessage.setText(new SinkChunkResultJsonBuilder().build());
        sinkMessageConsumerBean.onMessage(textMessage);
        assertThat(sinkMessageConsumerBean.getMessageDrivenContext().getRollbackOnly(), is(false));
    }

    @Test
    public void onMessage_handlingThrowsJobProcessorException_transactionRollback() throws JMSException, JobProcessorException {
        doThrow(new JobProcessorException("JobProcessorException")).when(jobStoreMessageProducer).send(any(SinkChunkResult.class));
        final TestableSinkMessageConsumerBean sinkMessageConsumerBean = getInitializedBean();
        final MockedJmsTextMessage textMessage = new MockedJmsTextMessage();
        textMessage.setStringProperty(JmsConstants.PAYLOAD_PROPERTY_NAME, JmsConstants.SINK_RESULT_PAYLOAD_TYPE);
        textMessage.setText(new SinkChunkResultJsonBuilder().build());
        sinkMessageConsumerBean.onMessage(textMessage);
        assertThat(sinkMessageConsumerBean.getMessageDrivenContext().getRollbackOnly(), is(true));
    }

    private TestableSinkMessageConsumerBean getInitializedBean() {
        final TestableSinkMessageConsumerBean sinkMessageConsumerBeanBean = new TestableSinkMessageConsumerBean();
        sinkMessageConsumerBeanBean.setMessageDrivenContext(new MockedJmsMessageDrivenContext());
        sinkMessageConsumerBeanBean.jobStoreMessageProducer = jobStoreMessageProducer;
        return sinkMessageConsumerBeanBean;
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


