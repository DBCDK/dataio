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

public class AddWaitingOnProcessor implements EntryProcessor<TrackingKey, DependencyTracking, StatusChangeEvent> {
    private final Set<TrackingKey> keys;

    public AddWaitingOnProcessor(Set<TrackingKey> keys) {
        this.keys = Objects.requireNonNull(keys);
        if(keys.isEmpty()) throw new IllegalArgumentException("There should be at least one key added");
    }

    @Override
    public StatusChangeEvent process(Map.Entry<TrackingKey, DependencyTracking> entry) {
        Set<TrackingKey> waitingOn = entry.getValue().getWaitingOn();
        keys.forEach(waitingOn::add);
        ChunkSchedulingStatus oldStatus = entry.getValue().getStatus();
        entry.getValue().setStatus(BLOCKED);
        return oldStatus == BLOCKED ? null : new StatusChangeEvent(entry.getValue().getSinkId(), oldStatus, BLOCKED);
    }
}
