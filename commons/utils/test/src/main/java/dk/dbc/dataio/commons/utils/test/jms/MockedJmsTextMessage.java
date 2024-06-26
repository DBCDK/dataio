package dk.dbc.dataio.commons.utils.test.jms;

import jakarta.jms.JMSException;
import jakarta.jms.TextMessage;

public class MockedJmsTextMessage extends MockedJmsMessage implements TextMessage {
    private String payload;

    public MockedJmsTextMessage(String payload) {
        this.payload = payload;
        try {
            setIntProperty("JMSXDeliveryCount", 1);
        } catch (JMSException e) {
            throw new RuntimeException(e);
        }
    }

    public MockedJmsTextMessage() {
        this(null);
    }

    @Override
    public void setText(String payload) throws JMSException {
        this.payload = payload;
    }

    @Override
    public String getText() throws JMSException {
        return payload;
    }
}
