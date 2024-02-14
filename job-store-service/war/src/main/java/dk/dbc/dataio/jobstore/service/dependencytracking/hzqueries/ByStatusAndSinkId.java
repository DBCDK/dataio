package dk.dbc.dataio.jobstore.service.dependencytracking.hzqueries;

import com.hazelcast.query.Predicate;
import dk.dbc.dataio.jobstore.service.entity.DependencyTracking;

import java.util.Map;

public class ByStatusAndSinkId implements Predicate<DependencyTracking.Key, DependencyTracking> {
    private final int sinkId;
    private final DependencyTracking.ChunkSchedulingStatus status;

    public ByStatusAndSinkId(int sinkId, DependencyTracking.ChunkSchedulingStatus status) {
        this.sinkId = sinkId;
        this.status = status;
    }

    @Override
    public boolean apply(Map.Entry<DependencyTracking.Key, DependencyTracking> entry) {
        DependencyTracking value = entry.getValue();
        return value.getSinkid() == sinkId && value.getStatus() == status;
    }
}
