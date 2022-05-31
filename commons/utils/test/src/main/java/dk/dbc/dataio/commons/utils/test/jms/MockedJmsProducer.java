package dk.dbc.dataio.commons.utils.test.jms;

import javax.jms.CompletionListener;
import javax.jms.Destination;
import javax.jms.JMSProducer;
import javax.jms.Message;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

public class MockedJmsProducer implements JMSProducer {
    public LinkedList<MockedJmsTextMessage> messages = new LinkedList<>();

    public void clearMessages() {
        messages.clear();
    }

    @Override
    public JMSProducer send(Destination destination, Message message) {
        messages.add((MockedJmsTextMessage) message);
        return this;
    }

    @Override
    public JMSProducer send(Destination destination, String s) {
        return null;
    }

    @Override
    public JMSProducer send(Destination destination, Map<String, Object> map) {
        return null;
    }

    @Override
    public JMSProducer send(Destination destination, byte[] bytes) {
        return null;
    }

    @Override
    public JMSProducer send(Destination destination, Serializable serializable) {
        return null;
    }

    @Override
    public JMSProducer setDisableMessageID(boolean b) {
        return null;
    }

    @Override
    public boolean getDisableMessageID() {
        return false;
    }

    @Override
    public JMSProducer setDisableMessageTimestamp(boolean b) {
        return null;
    }

    @Override
    public boolean getDisableMessageTimestamp() {
        return false;
    }

    @Override
    public JMSProducer setDeliveryMode(int i) {
        return null;
    }

    @Override
    public int getDeliveryMode() {
        return 0;
    }

    @Override
    public JMSProducer setPriority(int i) {
        return null;
    }

    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    public JMSProducer setTimeToLive(long l) {
        return null;
    }

    @Override
    public long getTimeToLive() {
        return 0;
    }

    @Override
    public JMSProducer setDeliveryDelay(long l) {
        return null;
    }

    @Override
    public long getDeliveryDelay() {
        return 0;
    }

    @Override
    public JMSProducer setAsync(CompletionListener completionListener) {
        return null;
    }

    @Override
    public CompletionListener getAsync() {
        return null;
    }

    @Override
    public JMSProducer setProperty(String s, boolean b) {
        return null;
    }

    @Override
    public JMSProducer setProperty(String s, byte b) {
        return null;
    }

    @Override
    public JMSProducer setProperty(String s, short i) {
        return null;
    }

    @Override
    public JMSProducer setProperty(String s, int i) {
        return null;
    }

    @Override
    public JMSProducer setProperty(String s, long l) {
        return null;
    }

    @Override
    public JMSProducer setProperty(String s, float v) {
        return null;
    }

    @Override
    public JMSProducer setProperty(String s, double v) {
        return null;
    }

    @Override
    public JMSProducer setProperty(String s, String s1) {
        return null;
    }

    @Override
    public JMSProducer setProperty(String s, Object o) {
        return null;
    }

    @Override
    public JMSProducer clearProperties() {
        return null;
    }

    @Override
    public boolean propertyExists(String s) {
        return false;
    }

    @Override
    public boolean getBooleanProperty(String s) {
        return false;
    }

    @Override
    public byte getByteProperty(String s) {
        return 0;
    }

    @Override
    public short getShortProperty(String s) {
        return 0;
    }

    @Override
    public int getIntProperty(String s) {
        return 0;
    }

    @Override
    public long getLongProperty(String s) {
        return 0;
    }

    @Override
    public float getFloatProperty(String s) {
        return 0;
    }

    @Override
    public double getDoubleProperty(String s) {
        return 0;
    }

    @Override
    public String getStringProperty(String s) {
        return null;
    }

    @Override
    public Object getObjectProperty(String s) {
        return null;
    }

    @Override
    public Set<String> getPropertyNames() {
        return null;
    }

    @Override
    public JMSProducer setJMSCorrelationIDAsBytes(byte[] bytes) {
        return null;
    }

    @Override
    public byte[] getJMSCorrelationIDAsBytes() {
        return new byte[0];
    }

    @Override
    public JMSProducer setJMSCorrelationID(String s) {
        return null;
    }

    @Override
    public String getJMSCorrelationID() {
        return null;
    }

    @Override
    public JMSProducer setJMSType(String s) {
        return null;
    }

    @Override
    public String getJMSType() {
        return null;
    }

    @Override
    public JMSProducer setJMSReplyTo(Destination destination) {
        return null;
    }

    @Override
    public Destination getJMSReplyTo() {
        return null;
    }
}
