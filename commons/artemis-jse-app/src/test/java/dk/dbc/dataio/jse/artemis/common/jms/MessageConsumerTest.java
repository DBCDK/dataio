package dk.dbc.dataio.jse.artemis.common.jms;

import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;
import dk.dbc.dataio.commons.types.jms.JMSHeader;
import dk.dbc.dataio.commons.utils.test.jms.MockedJmsTextMessage;
import dk.dbc.dataio.jse.artemis.common.service.ZombieWatch;
import jakarta.jms.JMSException;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class MessageConsumerTest {
    private static final String RECORD_KEY = "870970:42";
    private static final short ITEM_ID = 3;

    private final MessageConsumer messageConsumer = new TestMessageConsumer();

    @Test
    void itemHeadersAreCarriedIntoConsumedMessage() throws JMSException, InvalidMessageException {
        MockedJmsTextMessage message = new MockedJmsTextMessage("payload");
        JMSHeader.payload.addHeader(message, JMSHeader.CHUNK_PAYLOAD_TYPE);
        JMSHeader.itemId.addHeader(message, ITEM_ID);
        JMSHeader.recordKey.addHeader(message, RECORD_KEY);

        ConsumedMessage consumedMessage = messageConsumer.validateMessage(message);

        assertThat(JMSHeader.itemId.getHeader(consumedMessage, Short.class), is(ITEM_ID));
        assertThat(JMSHeader.recordKey.getHeader(consumedMessage, String.class), is(RECORD_KEY));
    }

    private static class TestMessageConsumer implements MessageConsumer {
        @Override
        public void handleConsumedMessage(ConsumedMessage consumedMessage) {
        }

        @Override
        public String getQueue() {
            return "queue";
        }

        @Override
        public String getAddress() {
            return "address";
        }

        @Override
        public ZombieWatch getZombieWatch() {
            return null;
        }
    }
}
