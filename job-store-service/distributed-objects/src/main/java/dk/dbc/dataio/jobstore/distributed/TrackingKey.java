package dk.dbc.dataio.jobstore.distributed;

import java.io.Serializable;
import java.util.Objects;

public class TrackingKey implements Serializable {
    private static final long serialVersionUID = -5575195152198835462L;
    private int jobId;
    private int chunkId;

    public TrackingKey() {
    }

    public TrackingKey(int jobId, int chunkId) {
        this.jobId = jobId;
        this.chunkId = chunkId;
    }

    public int getChunkId() {
        return chunkId;
    }

    public int getJobId() {
        return jobId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TrackingKey key = (TrackingKey) o;
        return jobId == key.jobId &&
                chunkId == key.chunkId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(jobId, chunkId);
    }

    @Override
    public String toString() {
        return "DependencyTrackingEntity.Key{" +
                "jobId=" + jobId +
                ", chunkId=" + chunkId +
                '}';
    }

    public String toChunkIdentifier() {
        return jobId + "/" + chunkId;
    }
}
