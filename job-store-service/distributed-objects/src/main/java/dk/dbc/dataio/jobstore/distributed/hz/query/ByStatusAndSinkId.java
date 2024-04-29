package dk.dbc.dataio.jobstore.distributed.hz.query;

import com.hazelcast.query.Predicate;
import dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus;
import dk.dbc.dataio.jobstore.distributed.DependencyTracking;
import dk.dbc.dataio.jobstore.distributed.TrackingKey;

import java.util.Map;
import java.util.Objects;

public class ByStatusAndSinkId implements Predicate<TrackingKey, DependencyTracking> {
    private final Integer sinkId;
    private final ChunkSchedulingStatus status;

    public ByStatusAndSinkId(Integer sinkId, ChunkSchedulingStatus status) {
        this.sinkId = sinkId;
        this.status = Objects.requireNonNull(status);
    }

    @Override
    public boolean apply(Map.Entry<TrackingKey, DependencyTracking> entry) {
        DependencyTracking value = entry.getValue();
        return (sinkId == null || value.getSinkId() == sinkId) && value.getStatus() == status;
    }
}
