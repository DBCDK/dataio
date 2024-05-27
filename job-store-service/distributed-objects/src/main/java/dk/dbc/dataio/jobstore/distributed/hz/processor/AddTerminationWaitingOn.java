package dk.dbc.dataio.jobstore.distributed.hz.processor;

import com.hazelcast.map.EntryProcessor;
import dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus;
import dk.dbc.dataio.jobstore.distributed.DependencyTracking;
import dk.dbc.dataio.jobstore.distributed.StatusChangeEvent;
import dk.dbc.dataio.jobstore.distributed.TrackingKey;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus.BLOCKED;
import static dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus.READY_FOR_DELIVERY;

public class AddTerminationWaitingOn implements EntryProcessor<TrackingKey, DependencyTracking, StatusChangeEvent> {
    private static final long serialVersionUID = 1L;
    private final Set<TrackingKey> keys;

    public AddTerminationWaitingOn(Set<TrackingKey> keys) {
        this.keys = Objects.requireNonNull(keys);
    }

    @Override
    public StatusChangeEvent process(Map.Entry<TrackingKey, DependencyTracking> entry) {
        Set<TrackingKey> waitingOn = entry.getValue().getWaitingOn();
        waitingOn.addAll(keys);
        ChunkSchedulingStatus oldStatus = entry.getValue().getStatus();
        ChunkSchedulingStatus newStatus = waitingOn.isEmpty() ? READY_FOR_DELIVERY : BLOCKED;
        if(oldStatus == newStatus) return null;
        entry.getValue().setStatus(newStatus);
        entry.setValue(entry.getValue());
        return new StatusChangeEvent(entry.getValue().getSinkId(), oldStatus, newStatus);
    }
}
