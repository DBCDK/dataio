package dk.dbc.dataio.jobstore.distributed.hz.query;

import com.hazelcast.query.Predicate;
import dk.dbc.dataio.jobstore.distributed.DependencyTracking;
import dk.dbc.dataio.jobstore.distributed.TrackingKey;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class JobChunksWaitForKey implements Predicate<TrackingKey, DependencyTracking> {
    private static final long serialVersionUID = 1L;
    private final int sinkId;
    private final int jobId;
    private final Set<String> matchKeys;

    public JobChunksWaitForKey(int sinkId, int jobId, Set<String> matchKeys) {
        this.sinkId = sinkId;
        this.jobId = jobId;
        this.matchKeys = Objects.requireNonNull(matchKeys);
    }

    @Override
    public boolean apply(Map.Entry<TrackingKey, DependencyTracking> entry) {
        DependencyTracking value = entry.getValue();
        return value.getSinkId() == sinkId &&
                (value.getKey().getJobId() == jobId ||
                matchKeys.stream().anyMatch(value.getMatchKeys()::contains));
    }
}
