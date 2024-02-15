package dk.dbc.dataio.jobstore.service.dependencytracking.hzqueries;

import com.hazelcast.query.Predicate;
import dk.dbc.dataio.jobstore.service.dependencytracking.ChunkSchedulingStatus;
import dk.dbc.dataio.jobstore.service.dependencytracking.DependencyTracking;
import dk.dbc.dataio.jobstore.service.dependencytracking.TrackingKey;

import java.util.Map;
import java.util.Objects;

public class ByStatusAndSinkId implements Predicate<TrackingKey, DependencyTracking> {
    private final int sinkId;
    private final ChunkSchedulingStatus status;

    public ByStatusAndSinkId(int sinkId, ChunkSchedulingStatus status) {
        this.sinkId = sinkId;
        this.status = Objects.requireNonNull(status);
    }

    @Override
    public boolean apply(Map.Entry<TrackingKey, DependencyTracking> entry) {
        DependencyTracking value = entry.getValue();
        return value.getSinkId() == sinkId && value.getStatus() == status;
    }
}
