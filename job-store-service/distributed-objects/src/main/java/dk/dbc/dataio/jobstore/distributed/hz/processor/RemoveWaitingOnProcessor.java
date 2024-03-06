package dk.dbc.dataio.jobstore.distributed.hz.processor;

import com.hazelcast.map.EntryProcessor;
import com.hazelcast.query.Predicate;
import dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus;
import dk.dbc.dataio.jobstore.distributed.DependencyTracking;
import dk.dbc.dataio.jobstore.distributed.StatusChangeEvent;
import dk.dbc.dataio.jobstore.distributed.TrackingKey;

import java.util.Map;
import java.util.Set;

public class RemoveWaitingOnProcessor implements EntryProcessor<TrackingKey, DependencyTracking, StatusChangeEvent>, Predicate<TrackingKey, DependencyTracking> {
    private final TrackingKey key;

    public RemoveWaitingOnProcessor(TrackingKey key) {
        this.key = key;
    }

    @Override
    public StatusChangeEvent process(Map.Entry<TrackingKey, DependencyTracking> entry) {
        DependencyTracking dt = entry.getValue();
        dt.getWaitingOn().remove(key);
        if(dt.getStatus() == ChunkSchedulingStatus.BLOCKED && dt.getWaitingOn().isEmpty()) {
            dt.setStatus(ChunkSchedulingStatus.QUEUED_FOR_PROCESSING);
            return new StatusChangeEvent(dt.getSinkId(), ChunkSchedulingStatus.BLOCKED, ChunkSchedulingStatus.QUEUED_FOR_PROCESSING);
        }
        return null;
    }

    @Override
    public boolean apply(Map.Entry<TrackingKey, DependencyTracking> entry) {
        Set<TrackingKey> waitingOn = entry.getValue().getWaitingOn();
        return waitingOn != null && waitingOn.contains(key);
    }
}
