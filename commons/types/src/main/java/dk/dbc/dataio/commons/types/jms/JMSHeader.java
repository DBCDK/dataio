package dk.dbc.dataio.commons.types.jms;

import dk.dbc.dataio.commons.types.ConsumedMessage;
import jakarta.jms.JMSException;
import jakarta.jms.Message;

public enum JMSHeader {
    jobId,
    chunkId,
    /**
     * Identifies the individual item within its chunk, carried as a JMS short property to
     * match the {@code short} item id used throughout job-store ({@code ItemEntity.Key},
     * {@code Watermark}, the delivery endpoints and the job-store-service-connector).
     * <p>
     * Third element of the {@code (jobId, chunkId, itemId)} version tuple compared against
     * the delivery watermark before an item is delivered, and the {@code itemId} path
     * segment of {@code POST /jobs/{jobId}/chunks/{chunkId}/items/{itemId}/delivered}.
     * <p>
     * Absent on chunk level messages, so read it as a nullable {@code Short}.
     */
    itemId,
    /**
     * Delivery watermark key for the record carried by this item, composed once by
     * job-store as {@code <agencyId>:RecordInfo.getId()}, where agencyId is the job's
     * submitter number.
     * <p>
     * The value is opaque to sinks. Do not re-derive it from the delivered content and do
     * not split it on {@code :}. The id half is whitespace normalised by the
     * {@code RecordInfo} constructor, so a key re-derived from raw record bytes can differ
     * from the one job-store compares against, and stale delivery detection then fails
     * silently.
     * <p>
     * Absent for an item that has no record key, such as the termination marker item, and
     * absent on chunk level messages, so read it as a nullable {@code String}.
     */
    recordKey,
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

    public void addHeader(Message message, short value) throws JMSException {
        message.setShortProperty(name, value);
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
