package dk.dbc.dataio.jobstore.distributed.hzqueries;

import com.hazelcast.map.EntryProcessor;
import com.hazelcast.query.Predicate;
import dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus;
import dk.dbc.dataio.jobstore.distributed.DependencyTracking;
import dk.dbc.dataio.jobstore.distributed.TrackingKey;

import java.util.Map;

public class RemoveWaitingOnProcessor implements EntryProcessor<TrackingKey, DependencyTracking, Boolean>, Predicate<TrackingKey, DependencyTracking> {
    private final TrackingKey key;

    public RemoveWaitingOnProcessor(TrackingKey key) {
        this.key = key;
    }

    @Override
    public Boolean process(Map.Entry<TrackingKey, DependencyTracking> entry) {
        DependencyTracking dt = entry.getValue();
        dt.getWaitingOn().remove(key);
        if(dt.getStatus() == ChunkSchedulingStatus.BLOCKED && dt.getWaitingOn().isEmpty()) {
            dt.setStatus(ChunkSchedulingStatus.QUEUED_FOR_PROCESSING);
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }

    @Override
    public boolean apply(Map.Entry<TrackingKey, DependencyTracking> entry) {
        return entry.getValue().getWaitingOn().contains(key);
    }
}
