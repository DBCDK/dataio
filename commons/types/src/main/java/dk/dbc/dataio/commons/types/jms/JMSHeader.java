package dk.dbc.dataio.commons.types.jms;

import dk.dbc.dataio.commons.types.ConsumedMessage;

import javax.jms.JMSException;
import javax.jms.Message;

public enum JMSHeader {
    jobId,
    chunkId,
    sink,
    trackingId,
    payload,
    flowId,
    additionalArgs,
    flowVersion,
    resource,
    sinkId("id"),
    sinkVersion("version"),
    flowBinderId,
    flowBinderVersion,
    abortId;

    public final String name;
    public static final String CHUNK_PAYLOAD_TYPE = "Chunk";
    public static final String ABORT_PAYLOAD_TYPE = "ABORT";

    JMSHeader() {
        name = name();
    }

    JMSHeader(String name) {
        this.name = name;
    }

    public void addHeader(Message message, String value) throws JMSException {
        message.setStringProperty(name, value);
    }

    public void addHeader(Message message, long value) throws JMSException {
        message.setLongProperty(name, value);
    }

    public void addHeader(Message message, int value) throws JMSException {
        message.setIntProperty(name, value);
    }

    public <T> T getHeader(Message message) throws JMSException {
        //noinspection unchecked
        return (T)message.getObjectProperty(name);
    }

    public <T> T getHeader(ConsumedMessage message, Class<T> clazz) {
        return message.getHeaderValue(name, clazz);
    }

    public <T> T getHeader(Message message, Class<T> clazz) throws JMSException {
        return clazz.cast(message.getObjectProperty(name));
    }
}
