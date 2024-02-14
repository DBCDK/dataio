package dk.dbc.dataio.jobstore.service.dependencytracking.hzqueries;

import com.hazelcast.query.Predicate;
import dk.dbc.dataio.jobstore.service.entity.DependencyTracking;

import java.util.Map;

public class WaitingOn implements Predicate<DependencyTracking.Key, DependencyTracking> {
    private final int sinkId;
    private final DependencyTracking.Key key;

    public WaitingOn(int sinkId, DependencyTracking.Key key) {
        this.sinkId = sinkId;
        this.key = key;
    }

    @Override
    public boolean apply(Map.Entry<DependencyTracking.Key, DependencyTracking> entry) {
        return entry.getValue().getSinkid() == sinkId && entry.getValue().getWaitingOn().contains(key);
    }
}
