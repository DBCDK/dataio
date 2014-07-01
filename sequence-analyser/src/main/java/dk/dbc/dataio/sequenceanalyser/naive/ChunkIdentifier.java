package dk.dbc.dataio.sequenceanalyser.naive;

public class ChunkIdentifier {
    public final long jobId;
    public final long chunkId;

    public ChunkIdentifier(long jobId, long chunkId) {
        this.jobId = jobId;
        this.chunkId = chunkId;
    }
}
