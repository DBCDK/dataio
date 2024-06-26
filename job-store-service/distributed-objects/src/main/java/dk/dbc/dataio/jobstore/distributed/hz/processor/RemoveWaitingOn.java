package dk.dbc.dataio.jobstore.distributed.hz.processor;

import com.hazelcast.map.EntryProcessor;
import com.hazelcast.query.Predicate;
import dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus;
import dk.dbc.dataio.jobstore.distributed.DependencyTracking;
import dk.dbc.dataio.jobstore.distributed.StatusChangeEvent;
import dk.dbc.dataio.jobstore.distributed.TrackingKey;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class RemoveWaitingOn implements EntryProcessor<TrackingKey, DependencyTracking, StatusChangeEvent>, Predicate<TrackingKey, DependencyTracking> {
    public final TrackingKey key;

    public RemoveWaitingOn(TrackingKey key) {
        this.key = Objects.requireNonNull(key);
    }

    @Override
    public StatusChangeEvent process(Map.Entry<TrackingKey, DependencyTracking> entry) {
        DependencyTracking dt = entry.getValue();
        dt.getWaitingOn().remove(key);
        if(dt.getStatus() == ChunkSchedulingStatus.BLOCKED && dt.getWaitingOn().isEmpty()) {
            dt.setStatus(ChunkSchedulingStatus.READY_FOR_DELIVERY);
            entry.setValue(dt);
            return new StatusChangeEvent(dt.getSinkId(), ChunkSchedulingStatus.BLOCKED, ChunkSchedulingStatus.READY_FOR_DELIVERY);
        }
        entry.setValue(dt);
        return null;
    }

    @Override
    public boolean apply(Map.Entry<TrackingKey, DependencyTracking> entry) {
        if(entry.getKey() == null) return false;
        Set<TrackingKey> waitingOn = entry.getValue().getWaitingOn();
        return waitingOn != null && waitingOn.contains(key);
    }
}
