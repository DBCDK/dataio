package dk.dbc.dataio.jobprocessor.ejb;

import dk.dbc.dataio.commons.types.SinkChunkResult;
import dk.dbc.dataio.commons.utils.test.jms.MockedJmsMessageDrivenContext;
import dk.dbc.dataio.commons.utils.test.jms.MockedJmsTextMessage;
import dk.dbc.dataio.commons.utils.test.jms.NotJmsTextMessage;
import dk.dbc.dataio.commons.utils.test.json.SinkChunkResultJsonBuilder;
import dk.dbc.dataio.jobprocessor.dto.ConsumedMessage;
import dk.dbc.dataio.jobprocessor.exception.InvalidMessageJobProcessorException;
import dk.dbc.dataio.jobprocessor.exception.JobProcessorException;
import org.junit.Test;

import javax.jms.JMSException;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.powermock.api.mockito.PowerMockito.doThrow;
import static org.powermock.api.mockito.PowerMockito.mock;

public class SinkMessageConsumerBeanTest {
    private JobStoreMessageProducerBean jobStoreMessageProducer = mock(JobStoreMessageProducerBean.class);

    @Test
    public void onMessage_messageArgIsNull_noTransactionRollback() {
        final SinkMessageConsumerBean sinkMessageConsumerBean = getInitializedBean();
        sinkMessageConsumerBean.onMessage(null);
        assertThat(sinkMessageConsumerBean.messageDrivenContext.getRollbackOnly(), is(false));
    }

    @Test(expected = InvalidMessageJobProcessorException.class)
    public void validateMessage_messageArgIsNull_throws() throws InvalidMessageJobProcessorException {
        getInitializedBean().validateMessage(null);
    }

    @Test
    public void onMessage_messageArgIsNotOfTypeTextMessage_noTransactionRollback() {
        final SinkMessageConsumerBean sinkMessageConsumerBean = getInitializedBean();
        sinkMessageConsumerBean.onMessage(new NotJmsTextMessage());
        assertThat(sinkMessageConsumerBean.messageDrivenContext.getRollbackOnly(), is(false));
    }

    @Test(expected = InvalidMessageJobProcessorException.class)
    public void validateMessage_messageArgIsNotOfTypeTextMessage_throws() throws InvalidMessageJobProcessorException {
        getInitializedBean().validateMessage(new NotJmsTextMessage());
    }

    @Test
    public void onMessage_messageArgPayloadIsNull_noTransactionRollback() throws JMSException {
        final SinkMessageConsumerBean sinkMessageConsumerBean = getInitializedBean();
        final MockedJmsTextMessage textMessage = new MockedJmsTextMessage();
        textMessage.setText(null);
        sinkMessageConsumerBean.onMessage(textMessage);
        assertThat(sinkMessageConsumerBean.messageDrivenContext.getRollbackOnly(), is(false));
    }

    @Test(expected = InvalidMessageJobProcessorException.class)
    public void validateMessage_messageArgPayloadIsNull_throws() throws InvalidMessageJobProcessorException, JMSException {
        final MockedJmsTextMessage textMessage = new MockedJmsTextMessage();
        textMessage.setText(null);
        getInitializedBean().validateMessage(textMessage);
    }

    @Test
    public void onMessage_messageArgPayloadIsEmpty_noTransactionRollback() throws JMSException {
        final SinkMessageConsumerBean sinkMessageConsumerBean = getInitializedBean();
        final MockedJmsTextMessage textMessage = new MockedJmsTextMessage();
        textMessage.setText("");
        sinkMessageConsumerBean.onMessage(textMessage);
        assertThat(sinkMessageConsumerBean.messageDrivenContext.getRollbackOnly(), is(false));
    }

    @Test(expected = InvalidMessageJobProcessorException.class)
    public void validateMessage_messageArgPayloadIsEmpty_throws() throws InvalidMessageJobProcessorException, JMSException {
        final MockedJmsTextMessage textMessage = new MockedJmsTextMessage();
        textMessage.setText("");
        getInitializedBean().validateMessage(textMessage);
    }

    @Test
    public void validateMessage_onValidMessage_returnsConsumedMessage() throws JMSException, InvalidMessageJobProcessorException {
        final String payload = "payload";
        final MockedJmsTextMessage textMessage = new MockedJmsTextMessage();
        textMessage.setText(payload);
        ConsumedMessage consumedMessage = getInitializedBean().validateMessage(textMessage);
        assertThat(consumedMessage.getMessageId(), is(MockedJmsTextMessage.DEFAULT_MESSAGE_ID));
        assertThat(consumedMessage.getMessagePayload(), is(payload));
    }

    @Test
    public void onMessage_messageArgPayloadIsInvalidSinkChunkResult_noTransactionRollback() throws JMSException {
        final SinkMessageConsumerBean sinkMessageConsumerBean = getInitializedBean();
        final MockedJmsTextMessage textMessage = new MockedJmsTextMessage();
        textMessage.setText("{'invalid': 'instance'}");
        sinkMessageConsumerBean.onMessage(textMessage);
        assertThat(sinkMessageConsumerBean.messageDrivenContext.getRollbackOnly(), is(false));
    }

    @Test(expected = InvalidMessageJobProcessorException.class)
    public void handleConsumedMessage_messageArgPayloadIsInvalidSinkChunkResult_throws() throws JobProcessorException, JMSException {
        final ConsumedMessage consumedMessage = new ConsumedMessage("id", "{'invalid': 'instance'}");
        getInitializedBean().handleConsumedMessage(consumedMessage);
    }

    @Test
    public void onMessage_messageArgPayloadIsValidSinkChunkResult_handled() throws JMSException {
        final SinkMessageConsumerBean sinkMessageConsumerBean = getInitializedBean();
        final MockedJmsTextMessage textMessage = new MockedJmsTextMessage();
        textMessage.setText(new SinkChunkResultJsonBuilder().build());
        sinkMessageConsumerBean.onMessage(textMessage);
        assertThat(sinkMessageConsumerBean.messageDrivenContext.getRollbackOnly(), is(false));
    }

    @Test
    public void onMessage_handlingThrowsJobProcessorException_transactionRollback() throws JMSException, JobProcessorException {
        doThrow(new JobProcessorException("JobProcessorException")).when(jobStoreMessageProducer).send(any(SinkChunkResult.class));
        final SinkMessageConsumerBean sinkMessageConsumerBean = getInitializedBean();
        final MockedJmsTextMessage textMessage = new MockedJmsTextMessage();
        textMessage.setText(new SinkChunkResultJsonBuilder().build());
        sinkMessageConsumerBean.onMessage(textMessage);
        assertThat(sinkMessageConsumerBean.messageDrivenContext.getRollbackOnly(), is(true));
    }

    private SinkMessageConsumerBean getInitializedBean() {
        final SinkMessageConsumerBean sinkMessageConsumerBean = new SinkMessageConsumerBean();
        sinkMessageConsumerBean.messageDrivenContext = new MockedJmsMessageDrivenContext();
        sinkMessageConsumerBean.jobStoreMessageProducer = jobStoreMessageProducer;
        return sinkMessageConsumerBean;
    }
}


