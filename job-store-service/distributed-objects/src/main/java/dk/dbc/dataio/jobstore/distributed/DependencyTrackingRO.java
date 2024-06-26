package dk.dbc.dataio.jobstore.distributed;

import java.time.Instant;
import java.util.Set;

public interface DependencyTrackingRO {
    TrackingKey getKey();

    int getSinkId();

    ChunkSchedulingStatus getStatus();

    Set<TrackingKey> getWaitingOn();

    Set<String> getMatchKeys();

    Integer[] getHashes();

    int getSubmitter();

    int getPriority();

    Instant getLastModified();

    int getRetries();
}
