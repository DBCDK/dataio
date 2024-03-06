package dk.dbc.dataio.jobstore.distributed.hz.processor;

import com.hazelcast.map.EntryProcessor;
import dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus;
import dk.dbc.dataio.jobstore.distributed.DependencyTracking;
import dk.dbc.dataio.jobstore.distributed.StatusChangeEvent;
import dk.dbc.dataio.jobstore.distributed.TrackingKey;

import java.util.Map;

public class UpdateStatusProcessor implements EntryProcessor<TrackingKey, DependencyTracking, StatusChangeEvent> {
    ChunkSchedulingStatus schedulingStatus;

    public UpdateStatusProcessor(ChunkSchedulingStatus schedulingStatus) {
        this.schedulingStatus = schedulingStatus;
    }

    @Override
    public StatusChangeEvent process(Map.Entry<TrackingKey, DependencyTracking> entry) {
        StatusChangeEvent event = new StatusChangeEvent(entry.getValue().getSinkId(), entry.getValue().getStatus(), schedulingStatus);
        entry.getValue().setStatus(schedulingStatus);
        return event;
    }
}
