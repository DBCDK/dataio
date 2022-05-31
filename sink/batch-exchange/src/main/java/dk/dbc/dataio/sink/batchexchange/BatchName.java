package dk.dbc.dataio.sink.batchexchange;

import dk.dbc.dataio.commons.types.Chunk;

import java.util.StringTokenizer;

/**
 * Simple batch-exchange batch name abstraction
 */
public class BatchName {
    private final long jobId;
    private final long chunkId;

    BatchName(long jobId, long chunkId) {
        this.jobId = jobId;
        this.chunkId = chunkId;
    }

    public long getJobId() {
        return jobId;
    }

    public long getChunkId() {
        return chunkId;
    }

    @Override
    public String toString() {
        return String.format("%d-%d", jobId, chunkId);
    }

    /**
     * Creates new batch name from chunk
     *
     * @param chunk chunk for which a batch name is to be generated
     * @return BatchName object
     */
    public static BatchName fromChunk(Chunk chunk) {
        return new BatchName(chunk.getJobId(), chunk.getChunkId());
    }

    /**
     * Creates new batch name from given string which must be formatted as [JOB_ID]-[CHUNK_ID]
     *
     * @param name name to be parsed
     * @return BatchName object
     * @throws IllegalArgumentException if given invalid name string
     */
    public static BatchName fromString(String name) throws IllegalArgumentException {
        final StringTokenizer tokenizer = new StringTokenizer(name, "-");
        if (tokenizer.countTokens() != 2) {
            throw new IllegalArgumentException("Name does not match [JOB_ID]-[CHUNK_ID] pattern: " + name);
        }
        try {
            return new BatchName(
                    Long.parseLong(tokenizer.nextToken()),
                    Long.parseLong(tokenizer.nextToken()));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid batch name: " + name, e);
        }
    }
}
