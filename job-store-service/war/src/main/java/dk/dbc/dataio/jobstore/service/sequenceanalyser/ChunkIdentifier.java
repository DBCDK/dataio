package dk.dbc.dataio.jobstore.service.sequenceanalyser;

import dk.dbc.dataio.sequenceanalyser.CollisionDetectionElementIdentifier;

public class ChunkIdentifier implements CollisionDetectionElementIdentifier {
    public final long jobId;
    public final long chunkId;

    public ChunkIdentifier(long jobId, long chunkId) {
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
        return "["+jobId+", "+chunkId+ "]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ChunkIdentifier that = (ChunkIdentifier) o;

        if (chunkId != that.chunkId) {
            return false;
        }
        if (jobId != that.jobId) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = (int) (jobId ^ (jobId >>> 32));
        result = 31 * result + (int) (chunkId ^ (chunkId >>> 32));
        return result;
    }
}
