package dk.dbc.dataio.jobstore.distributed.hz.query;

import com.hazelcast.query.Predicate;
import dk.dbc.dataio.jobstore.distributed.DependencyTracking;
import dk.dbc.dataio.jobstore.distributed.TrackingKey;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class WaitForKey implements Predicate<TrackingKey, DependencyTracking> {
    private final int sinkId;
    private final int jobId;
    private final Set<String> matchKeys;

    public WaitForKey(int sinkId, int jobId, Set<String> matchKeys) {
        this.sinkId = sinkId;
        this.jobId = jobId;
        this.matchKeys = Objects.requireNonNull(matchKeys);
    }

    @Override
    public boolean apply(Map.Entry<TrackingKey, DependencyTracking> entry) {
        DependencyTracking value = entry.getValue();
        return value.getSinkId() == sinkId &&
                (value.getKey().getJobId() == jobId ||
                matchKeys.stream().limit(1).anyMatch(value.getMatchKeys()::contains)); // Todo JEGA: Not entirely sure if this is correct, must test
    }
    // SELECT jobId, chunkId FROM dependencyTracking WHERE sinkId=? AND (jobId=? or matchKeys @> '["?"]' ) ORDER BY jobId, chunkId FOR NO KEY UPDATE
}
