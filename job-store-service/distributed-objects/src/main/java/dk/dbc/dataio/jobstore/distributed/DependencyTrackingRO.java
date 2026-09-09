package dk.dbc.dataio.jobstore.distributed;

import java.time.Instant;

public interface DependencyTrackingRO {
    TrackingKey getKey();

    int getSinkId();

    ChunkSchedulingStatus getStatus();

    int getSubmitter();

    boolean isTermination();

    boolean isGateOpen();

    int getPriority();

    Instant getLastModified();

    int getRetries();
}
