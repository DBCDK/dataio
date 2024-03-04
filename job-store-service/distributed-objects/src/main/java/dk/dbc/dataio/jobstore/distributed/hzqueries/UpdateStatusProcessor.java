package dk.dbc.dataio.jobstore.distributed.hzqueries;

import com.hazelcast.map.EntryProcessor;
import dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus;
import dk.dbc.dataio.jobstore.distributed.DependencyTracking;
import dk.dbc.dataio.jobstore.distributed.TrackingKey;

import java.util.Map;

public class UpdateStatusProcessor implements EntryProcessor<TrackingKey, DependencyTracking, Void> {
    ChunkSchedulingStatus schedulingStatus;

    public UpdateStatusProcessor(ChunkSchedulingStatus schedulingStatus) {
        this.schedulingStatus = schedulingStatus;
    }

    @Override
    public Void process(Map.Entry<TrackingKey, DependencyTracking> entry) {
        entry.getValue().setStatus(schedulingStatus);
        return null;
    }
}
