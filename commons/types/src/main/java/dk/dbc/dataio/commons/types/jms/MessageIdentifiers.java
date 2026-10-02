package dk.dbc.dataio.commons.types.jms;

import dk.dbc.dataio.commons.types.Chunk;
import jakarta.jms.JMSException;
import jakarta.jms.Message;

public interface MessageIdentifiers {
    default void addIdentifiers(Message message, Chunk chunk) throws JMSException {
        addIdentifiers(message, chunk.getJobId(), chunk.getChunkId());
    }

    /**
     * Adds the chunk level identifiers.
     * <p>
     * Note the property types: {@link JMSHeader#jobId} is an int property and
     * {@link JMSHeader#chunkId} a long property. Consumers read them back through
     * {@code getObjectProperty} and unbox them, so the types have to stay exactly as they
     * are on the chunk message.
     *
     * @param message message to add headers to
     * @param jobId   ID of the job
     * @param chunkId ID of the chunk within its job
     * @throws JMSException when unable to set a message property
     */
    default void addIdentifiers(Message message, int jobId, long chunkId) throws JMSException {
        JMSHeader.jobId.addHeader(message, jobId);
        JMSHeader.chunkId.addHeader(message, chunkId);
        JMSHeader.trackingId.addHeader(message, jobId + "/" + chunkId);
    }

    /**
     * Adds the identifiers of a single item, being the chunk level identifiers plus the
     * item's own ID and tracking ID.
     * <p>
     * {@link JMSHeader#trackingId} is the item's own tracking ID, generated for the record
     * at partitioning time and carried through processing, rather than the chunk level
     * {@code jobId/chunkId}. It is what identifies the record in log lines from every
     * component handling it, where the chunk level value only restates the two numbers the
     * message already carries as properties of their own.
     * <p>
     * An item reaching this point without a tracking ID falls back to
     * {@code jobId/chunkId/itemId}, which at least names the item the log line is about.
     * job-store assigns a tracking ID to every item it partitions and both job processors
     * carry it onto the processing outcome, so the fallback covers a processed chunk that
     * crossed a service boundary without one.
     *
     * @param message    message to add headers to
     * @param jobId      ID of the job containing the item
     * @param chunkId    ID of the chunk containing the item
     * @param itemId     ID of the item within its chunk
     * @param trackingId tracking ID of the item, may be null or empty
     * @throws JMSException when unable to set a message property
     */
    default void addItemIdentifiers(Message message, int jobId, long chunkId, short itemId, String trackingId)
            throws JMSException {
        JMSHeader.jobId.addHeader(message, jobId);
        JMSHeader.chunkId.addHeader(message, chunkId);
        JMSHeader.itemId.addHeader(message, itemId);
        JMSHeader.trackingId.addHeader(message, trackingId == null || trackingId.isBlank()
                ? jobId + "/" + chunkId + "/" + itemId
                : trackingId);
    }
}
