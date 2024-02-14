package dk.dbc.dataio.jobstore.service.dependencytracking;

import dk.dbc.dataio.jobstore.service.entity.DependencyTracking;

import java.sql.Timestamp;
import java.util.Set;

public interface DependencyTrackingRO {
    DependencyTracking.Key getKey();

    int getSinkid();

    DependencyTracking.ChunkSchedulingStatus getStatus();

    Set<DependencyTracking.Key> getWaitingOn();

    Set<String> getMatchKeys();

    Integer[] getHashes();

    int getSubmitterNumber();

    int getPriority();

    Timestamp getLastModified();
}
