package dk.dbc.dataio.commons.utils.test.jms;

import javax.jms.JMSException;
import javax.jms.ObjectMessage;
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
