package dk.dbc.dataio.jobstore.distributed.hz.processor;

import com.hazelcast.map.EntryProcessor;
import dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus;
import dk.dbc.dataio.jobstore.distributed.DependencyTracking;
import dk.dbc.dataio.jobstore.distributed.StatusChangeEvent;
import dk.dbc.dataio.jobstore.distributed.TrackingKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class UpdateStatusProcessor implements EntryProcessor<TrackingKey, DependencyTracking, StatusChangeEvent> {
    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateStatusProcessor.class);
    private ChunkSchedulingStatus schedulingStatus;

    public UpdateStatusProcessor(ChunkSchedulingStatus schedulingStatus) {
        this.schedulingStatus = schedulingStatus;
    }

    @Override
    public StatusChangeEvent process(Map.Entry<TrackingKey, DependencyTracking> entry) {
        DependencyTracking dt = entry.getValue();
        StatusChangeEvent event = new StatusChangeEvent(dt.getSinkId(), dt.getStatus(), schedulingStatus);
        dt.setStatus(schedulingStatus);
        entry.setValue(dt);
        LOGGER.debug("Status update on {} - {}", entry.getKey(), schedulingStatus);

        return event;
    }
}
