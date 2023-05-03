package dk.dbc.dataio.commons.utils.test.jms;

import javax.jms.JMSException;
import javax.jms.TextMessage;

public class MockedJmsTextMessage extends MockedJmsMessage implements TextMessage {
    private String payload;

    public MockedJmsTextMessage() {
        try {
            setIntProperty("JMSXDeliveryCount", 1);
        } catch (JMSException e) {
            throw new RuntimeException(e);
        }
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
