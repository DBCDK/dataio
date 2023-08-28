package dk.dbc.dataio.commons.utils.test.jms;

import jakarta.jms.JMSException;
import jakarta.jms.ObjectMessage;

import java.io.Serializable;

public class MockedJmsObjectMessage extends MockedJmsMessage implements ObjectMessage {
    private Serializable object;

    @Override
    public Serializable getObject() throws JMSException {
        return this.object;
    }

    @Override
    public void setObject(Serializable object) throws JMSException {
        this.object = object;
    }
}
