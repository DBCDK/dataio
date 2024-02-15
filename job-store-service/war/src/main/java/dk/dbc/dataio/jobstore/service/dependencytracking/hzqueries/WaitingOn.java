package dk.dbc.dataio.jobstore.service.dependencytracking.hzqueries;

import com.hazelcast.query.Predicate;
import dk.dbc.dataio.jobstore.service.dependencytracking.DependencyTracking;
import dk.dbc.dataio.jobstore.service.dependencytracking.TrackingKey;

import java.util.Map;
import java.util.Objects;

public class WaitingOn implements Predicate<TrackingKey, DependencyTracking> {
    private final int sinkId;
    private final TrackingKey key;

    public WaitingOn(int sinkId, TrackingKey key) {
        this.sinkId = sinkId;
        this.key = Objects.requireNonNull(key);
    }

    @Override
    public boolean apply(Map.Entry<TrackingKey, DependencyTracking> entry) {
        return entry.getValue().getSinkId() == sinkId && entry.getValue().getWaitingOn().contains(key);
    }
}
