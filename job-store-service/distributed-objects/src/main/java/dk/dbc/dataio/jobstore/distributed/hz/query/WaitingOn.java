package dk.dbc.dataio.jobstore.distributed.hz.query;

import com.hazelcast.query.Predicate;
import dk.dbc.dataio.jobstore.distributed.DependencyTracking;
import dk.dbc.dataio.jobstore.distributed.TrackingKey;

import java.util.Map;
import java.util.Objects;

public class WaitingOn implements Predicate<TrackingKey, DependencyTracking> {
    private static final long serialVersionUID = 1L;
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
