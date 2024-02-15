package dk.dbc.dataio.jobstore.service.dependencytracking;

import java.time.Instant;
import java.util.Set;

public interface DependencyTrackingRO {
    TrackingKey getKey();

    int getSinkId();

    ChunkSchedulingStatus getStatus();

    Set<TrackingKey> getWaitingOn();

    Set<String> getMatchKeys();

    Integer[] getHashes();

    int getSubmitterNumber();

    int getPriority();

    Instant getLastModified();

    int getRetries();
}
