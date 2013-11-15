package dk.dbc.dataio.sink.es;

import dk.dbc.dataio.sink.InvalidMessageSinkException;
import org.junit.Test;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.TextMessage;
import java.util.Enumeration;

/**
 * EsMessageProcessorBean unit tests.
 *
 * The test methods of this class uses the following naming convention:
 *
 *  unitOfWork_stateUnderTest_expectedBehavior
 */
public class EsMessageProcessorBeanTest {
    @Test
    public void onMessage_messageArgIsNull_messageDrivenContextIsNotAccessed() {
        // We utilize the fact that since no MessageDrivenContext has been injected
        // any access would throw java.lang.NullPointerException
        getInitializedBean().onMessage(null);
    }

    @Test(expected = InvalidMessageSinkException.class)
    public void validateMessage_messageArgIsNull_throws() throws InvalidMessageSinkException {
        getInitializedBean().validateMessage(null);
    }

    @Test(expected = InvalidMessageSinkException.class)
    public void validateMessage_messageArgIsNotOfTypeTextMessage_throws() throws InvalidMessageSinkException {
        getInitializedBean().validateMessage(new NotTextMessage());
    }

    @Test(expected = InvalidMessageSinkException.class)
    public void validateMessage_messageArgPayloadIsNull_throws() throws InvalidMessageSinkException, JMSException {
        final MockedTextMessage textMessage = new MockedTextMessage();
        textMessage.setText(null);
        getInitializedBean().validateMessage(textMessage);
    }

    @Test(expected = InvalidMessageSinkException.class)
    public void validateMessage_messageArgPayloadIsEmpty_throws() throws InvalidMessageSinkException, JMSException {
        final MockedTextMessage textMessage = new MockedTextMessage();
        textMessage.setText("");
        getInitializedBean().validateMessage(textMessage);
    }

    @Test(expected = InvalidMessageSinkException.class)
    public void validateMessage_messageArgPayloadIsInvalidChunkResultJson_throws() throws InvalidMessageSinkException, JMSException {
        final MockedTextMessage textMessage = new MockedTextMessage();
        textMessage.setText("{'jobId': 42}");
        getInitializedBean().validateMessage(textMessage);
    }

    @Test(expected = InvalidMessageSinkException.class)
    public void validateMessage_messageArgPayloadTriggersDefaultConstructorWhenUnmarshalling_throws() throws InvalidMessageSinkException, JMSException {
        final MockedTextMessage textMessage = new MockedTextMessage();
        textMessage.setText("{}");
        getInitializedBean().validateMessage(textMessage);
    }

    private static EsMessageProcessorBean getInitializedBean() {
        return new EsMessageProcessorBean();
    }

    private static class MockedTextMessage implements TextMessage {
        private String payload;
        @Override
        public void setText(String s) throws JMSException {
            payload = s;
        }

        @Override
        public String getText() throws JMSException {
            return payload;
        }

        @Override public String getJMSMessageID() throws JMSException { return null; }
        @Override public void setJMSMessageID(String s) throws JMSException { }
        @Override public long getJMSTimestamp() throws JMSException { return 0; }
        @Override public void setJMSTimestamp(long l) throws JMSException { }
        @Override public byte[] getJMSCorrelationIDAsBytes() throws JMSException { return new byte[0]; }
        @Override public void setJMSCorrelationIDAsBytes(byte[] bytes) throws JMSException { }
        @Override public void setJMSCorrelationID(String s) throws JMSException { }
        @Override public String getJMSCorrelationID() throws JMSException { return null; }
        @Override public Destination getJMSReplyTo() throws JMSException { return null; }
        @Override public void setJMSReplyTo(Destination destination) throws JMSException { }
        @Override public Destination getJMSDestination() throws JMSException { return null; }
        @Override public void setJMSDestination(Destination destination) throws JMSException { }
        @Override public int getJMSDeliveryMode() throws JMSException { return 0; }
        @Override public void setJMSDeliveryMode(int i) throws JMSException { }
        @Override public boolean getJMSRedelivered() throws JMSException { return false; }
        @Override public void setJMSRedelivered(boolean b) throws JMSException { }
        @Override public String getJMSType() throws JMSException { return null; }
        @Override public void setJMSType(String s) throws JMSException { }
        @Override public long getJMSExpiration() throws JMSException { return 0; }
        @Override public void setJMSExpiration(long l) throws JMSException { }
        @Override public long getJMSDeliveryTime() throws JMSException { return 0; }
        @Override public void setJMSDeliveryTime(long l) throws JMSException { }
        @Override public int getJMSPriority() throws JMSException { return 0; }
        @Override public void setJMSPriority(int i) throws JMSException { }
        @Override public void clearProperties() throws JMSException { }
        @Override public boolean propertyExists(String s) throws JMSException { return false; }
        @Override public boolean getBooleanProperty(String s) throws JMSException { return false; }
        @Override public byte getByteProperty(String s) throws JMSException { return 0; }
        @Override public short getShortProperty(String s) throws JMSException { return 0; }
        @Override public int getIntProperty(String s) throws JMSException { return 0; }
        @Override public long getLongProperty(String s) throws JMSException { return 0; }
        @Override public float getFloatProperty(String s) throws JMSException { return 0; }
        @Override public double getDoubleProperty(String s) throws JMSException { return 0; }
        @Override public String getStringProperty(String s) throws JMSException { return null; }
        @Override public Object getObjectProperty(String s) throws JMSException { return null; }
        @Override public Enumeration getPropertyNames() throws JMSException { return null; }
        @Override public void setBooleanProperty(String s, boolean b) throws JMSException { }
        @Override public void setByteProperty(String s, byte b) throws JMSException { }
        @Override public void setShortProperty(String s, short i) throws JMSException { }
        @Override public void setIntProperty(String s, int i) throws JMSException { }
        @Override public void setLongProperty(String s, long l) throws JMSException { }
        @Override public void setFloatProperty(String s, float v) throws JMSException { }
        @Override public void setDoubleProperty(String s, double v) throws JMSException { }
        @Override public void setStringProperty(String s, String s2) throws JMSException { }
        @Override public void setObjectProperty(String s, Object o) throws JMSException { }
        @Override public void acknowledge() throws JMSException { }
        @Override public void clearBody() throws JMSException { }
        @Override public <T> T getBody(Class<T> tClass) throws JMSException { return null; }
        @Override public boolean isBodyAssignableTo(Class aClass) throws JMSException { return false; }
    }

    private static class NotTextMessage implements Message {
        @Override public String getJMSMessageID() throws JMSException { return null; }
        @Override public void setJMSMessageID(String s) throws JMSException { }
        @Override public long getJMSTimestamp() throws JMSException { return 0; }
        @Override public void setJMSTimestamp(long l) throws JMSException { }
        @Override public byte[] getJMSCorrelationIDAsBytes() throws JMSException { return new byte[0]; }
        @Override public void setJMSCorrelationIDAsBytes(byte[] bytes) throws JMSException { }
        @Override public void setJMSCorrelationID(String s) throws JMSException { }
        @Override public String getJMSCorrelationID() throws JMSException { return null; }
        @Override public Destination getJMSReplyTo() throws JMSException { return null; }
        @Override public void setJMSReplyTo(Destination destination) throws JMSException { }
        @Override public Destination getJMSDestination() throws JMSException { return null; }
        @Override public void setJMSDestination(Destination destination) throws JMSException { }
        @Override public int getJMSDeliveryMode() throws JMSException { return 0; }
        @Override public void setJMSDeliveryMode(int i) throws JMSException { }
        @Override public boolean getJMSRedelivered() throws JMSException { return false; }
        @Override public void setJMSRedelivered(boolean b) throws JMSException { }
        @Override public String getJMSType() throws JMSException { return null; }
        @Override public void setJMSType(String s) throws JMSException { }
        @Override public long getJMSExpiration() throws JMSException { return 0; }
        @Override public void setJMSExpiration(long l) throws JMSException { }
        @Override public long getJMSDeliveryTime() throws JMSException { return 0; }
        @Override public void setJMSDeliveryTime(long l) throws JMSException { }
        @Override public int getJMSPriority() throws JMSException { return 0; }
        @Override public void setJMSPriority(int i) throws JMSException { }
        @Override public void clearProperties() throws JMSException { }
        @Override public boolean propertyExists(String s) throws JMSException { return false; }
        @Override public boolean getBooleanProperty(String s) throws JMSException { return false; }
        @Override public byte getByteProperty(String s) throws JMSException { return 0; }
        @Override public short getShortProperty(String s) throws JMSException { return 0; }
        @Override public int getIntProperty(String s) throws JMSException { return 0; }
        @Override public long getLongProperty(String s) throws JMSException { return 0; }
        @Override public float getFloatProperty(String s) throws JMSException { return 0; }
        @Override public double getDoubleProperty(String s) throws JMSException { return 0; }
        @Override public String getStringProperty(String s) throws JMSException { return null; }
        @Override public Object getObjectProperty(String s) throws JMSException { return null; }
        @Override public Enumeration getPropertyNames() throws JMSException { return null; }
        @Override public void setBooleanProperty(String s, boolean b) throws JMSException { }
        @Override public void setByteProperty(String s, byte b) throws JMSException { }
        @Override public void setShortProperty(String s, short i) throws JMSException { }
        @Override public void setIntProperty(String s, int i) throws JMSException { }
        @Override public void setLongProperty(String s, long l) throws JMSException { }
        @Override public void setFloatProperty(String s, float v) throws JMSException { }
        @Override public void setDoubleProperty(String s, double v) throws JMSException { }
        @Override public void setStringProperty(String s, String s2) throws JMSException { }
        @Override public void setObjectProperty(String s, Object o) throws JMSException { }
        @Override public void acknowledge() throws JMSException { }
        @Override public void clearBody() throws JMSException { }
        @Override public <T> T getBody(Class<T> tClass) throws JMSException { return null; }
        @Override public boolean isBodyAssignableTo(Class aClass) throws JMSException { return false; }
    }
}
