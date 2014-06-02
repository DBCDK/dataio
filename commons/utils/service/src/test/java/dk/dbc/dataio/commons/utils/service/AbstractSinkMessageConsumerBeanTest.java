package dk.dbc.dataio.commons.utils.service;

import dk.dbc.dataio.commons.types.ChunkResult;
import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;
import dk.dbc.dataio.commons.types.exceptions.ServiceException;
import dk.dbc.dataio.commons.utils.test.jms.MockedJmsMessageDrivenContext;
import dk.dbc.dataio.commons.utils.test.json.ChunkResultJsonBuilder;
import org.junit.Test;

import javax.ejb.MessageDrivenContext;
import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

public class AbstractSinkMessageConsumerBeanTest {
    private static String messageId = "id";
    private static String payloadType = "ChunkResult";
    private static String payload = new ChunkResultJsonBuilder().build();

    @Test(expected = NullPointerException.class)
    public void unmarshallPayload_consumedMessageArgIsNull_throws() throws InvalidMessageException {
        getInitializedBean().unmarshallPayload(null);
    }

    @Test(expected = InvalidMessageException.class)
    public void unmarshallPayload_consumedMessageArgContainsUnexpectedPayloadType_throws() throws InvalidMessageException {
        final ConsumedMessage consumedMessage = new ConsumedMessage(messageId, "notExpectedPayloadType", payload);
        getInitializedBean().unmarshallPayload(consumedMessage);
    }

    @Test(expected = InvalidMessageException.class)
    public void unmarshallPayload_consumedMessageArgPayloadCanNotBeUnmarshalled_throws() throws InvalidMessageException {
        final ConsumedMessage consumedMessage = new ConsumedMessage(messageId, payloadType, "payload");
        getInitializedBean().unmarshallPayload(consumedMessage);
    }

    @Test(expected = InvalidMessageException.class)
    public void unmarshallPayload_consumedMessageArgPayloadIsEmptyChunkResult_throws() throws InvalidMessageException {
        final String emptyChunkResult = new ChunkResultJsonBuilder().setItems(Collections.<String>emptyList()).build();
        final ConsumedMessage consumedMessage = new ConsumedMessage(messageId, payloadType, emptyChunkResult);
        getInitializedBean().unmarshallPayload(consumedMessage);
    }

    @Test
    public void unmarshallPayload_consumedMessageArgIsValid_returnsChunkResultInstance() throws InvalidMessageException {
        final ConsumedMessage consumedMessage = new ConsumedMessage(messageId, payloadType, payload);
        final ChunkResult chunkResult = getInitializedBean().unmarshallPayload(consumedMessage);
        assertThat(chunkResult, is(notNullValue()));
    }

    private TestableMessageConsumerBean getInitializedBean() {
        final TestableMessageConsumerBean messageConsumerBean = new TestableMessageConsumerBean();
        messageConsumerBean.setMessageDrivenContext(new MockedJmsMessageDrivenContext());
        return messageConsumerBean;
    }

    private static class TestableMessageConsumerBean extends AbstractSinkMessageConsumerBean {
        public void setMessageDrivenContext(MessageDrivenContext messageDrivenContext) {
            this.messageDrivenContext = messageDrivenContext;
        }
        @Override
        public void handleConsumedMessage(ConsumedMessage consumedMessage) throws ServiceException, InvalidMessageException {
        }
    }
}