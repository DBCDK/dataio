package dk.dbc.dataio.jobstore.distributed.hzqueries;

import com.hazelcast.query.Predicate;
import dk.dbc.dataio.jobstore.distributed.DependencyTracking;
import dk.dbc.dataio.jobstore.distributed.TrackingKey;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class WaitForKey implements Predicate<TrackingKey, DependencyTracking> {
    private final int sinkId;
    private final int submitterNumber;
    private final Set<String> matchKeys;

    public WaitForKey(int sinkId, int submitterNumber, Set<String> matchKeys) {
        this.sinkId = sinkId;
        this.submitterNumber = submitterNumber;
        this.matchKeys = Objects.requireNonNull(matchKeys);
    }

    @Override
    public boolean apply(Map.Entry<TrackingKey, DependencyTracking> entry) {
        DependencyTracking value = entry.getValue();
        return value.getSinkId() == sinkId &&
                value.getSubmitter() == submitterNumber &&
                value.getMatchKeys().stream().limit(1).anyMatch(matchKeys::contains); // Todo JEGA: Not entirely sure if this is correct, must test
    }
}
