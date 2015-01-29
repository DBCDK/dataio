package dk.dbc.dataio.commons.utils.service;

import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.ExternalChunk;
import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;
import dk.dbc.dataio.commons.types.exceptions.ServiceException;
import dk.dbc.dataio.commons.utils.json.JsonException;
import dk.dbc.dataio.commons.utils.json.JsonUtil;
import dk.dbc.dataio.commons.utils.test.jms.MockedJmsMessageDrivenContext;
import dk.dbc.dataio.commons.utils.test.model.ExternalChunkBuilder;
import org.junit.Test;

import javax.ejb.MessageDrivenContext;
import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import org.junit.Before;

public class AbstractSinkMessageConsumerBeanTest {
    private static final String MESSAGE_ID = "id";
    private static final String PAYLOAD_TYPE = "ChunkResult";
    private String PAYLOAD;
    
    @Before
    public void setup() throws JsonException {
        ExternalChunk processedChunk = new ExternalChunkBuilder(ExternalChunk.Type.PROCESSED).build();
        PAYLOAD = JsonUtil.toJson(processedChunk);
    }

    @Test(expected = NullPointerException.class)
    public void unmarshallPayload_consumedMessageArgIsNull_throws() throws InvalidMessageException {
        getInitializedBean().unmarshallPayload(null);
    }

    @Test(expected = InvalidMessageException.class)
    public void unmarshallPayload_consumedMessageArgContainsUnexpectedPayloadType_throws() throws InvalidMessageException {
        final ConsumedMessage consumedMessage = new ConsumedMessage(MESSAGE_ID, "notExpectedPayloadType", PAYLOAD);
        getInitializedBean().unmarshallPayload(consumedMessage);
    }

    @Test(expected = InvalidMessageException.class)
    public void unmarshallPayload_consumedMessageArgPayloadCanNotBeUnmarshalled_throws() throws InvalidMessageException {
        final ConsumedMessage consumedMessage = new ConsumedMessage(MESSAGE_ID, PAYLOAD_TYPE, "payload");
        getInitializedBean().unmarshallPayload(consumedMessage);
    }

    @Test(expected = InvalidMessageException.class)
    public void unmarshallPayload_consumedMessageArgPayloadIsEmptyChunkResult_throws() throws InvalidMessageException, JsonException {
        ExternalChunk processedChunk = new ExternalChunkBuilder(ExternalChunk.Type.PROCESSED).setItems(Collections.<ChunkItem>emptyList()).build();
        final String emptyChunkResult = JsonUtil.toJson(processedChunk);
        final ConsumedMessage consumedMessage = new ConsumedMessage(MESSAGE_ID, PAYLOAD_TYPE, emptyChunkResult);
        getInitializedBean().unmarshallPayload(consumedMessage);
    }

    @Test
    public void unmarshallPayload_consumedMessageArgIsValid_returnsChunkResultInstance() throws InvalidMessageException {
        final ConsumedMessage consumedMessage = new ConsumedMessage(MESSAGE_ID, PAYLOAD_TYPE, PAYLOAD);
        final ExternalChunk processedChunk = getInitializedBean().unmarshallPayload(consumedMessage);
        assertThat(processedChunk, is(notNullValue()));
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