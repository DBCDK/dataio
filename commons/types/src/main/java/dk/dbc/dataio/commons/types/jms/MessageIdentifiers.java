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
     * item's own ID.
     * <p>
     * {@link JMSHeader#trackingId} stays the chunk level tracking ID, the same value the
     * chunk message carries today, so log lines for an item can still be correlated with
     * the rest of its chunk across components.
     *
     * @param message message to add headers to
     * @param jobId   ID of the job containing the item
     * @param chunkId ID of the chunk containing the item
     * @param itemId  ID of the item within its chunk
     * @throws JMSException when unable to set a message property
     */
    default void addItemIdentifiers(Message message, int jobId, long chunkId, short itemId) throws JMSException {
        addIdentifiers(message, jobId, chunkId);
        JMSHeader.itemId.addHeader(message, itemId);
    }
}
