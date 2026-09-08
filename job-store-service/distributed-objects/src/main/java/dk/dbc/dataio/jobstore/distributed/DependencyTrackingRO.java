package dk.dbc.dataio.jobstore.distributed;

import java.time.Instant;

public interface DependencyTrackingRO {
    TrackingKey getKey();

    int getSinkId();

    ChunkSchedulingStatus getStatus();

    int getSubmitter();

    boolean isTermination();

    int getPriority();

    Instant getLastModified();

    int getRetries();
}
