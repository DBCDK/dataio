package dk.dbc.dataio.commons.types.jms;

import dk.dbc.dataio.commons.types.Chunk;

import javax.jms.JMSException;
import javax.jms.Message;

public interface MessageIdentifiers {
    default void addIdentifiers(Message message, Chunk chunk) throws JMSException {
        JMSHeader.jobId.addHeader(message, chunk.getJobId());
        JMSHeader.chunkId.addHeader(message, chunk.getChunkId());
        JMSHeader.trackingId.addHeader(message, chunk.getTrackingId());
    }
}
