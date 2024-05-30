package dk.dbc.dataio.jobstore.distributed.hz.processor;

import com.hazelcast.map.EntryProcessor;
import dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus;
import dk.dbc.dataio.jobstore.distributed.DependencyTracking;
import dk.dbc.dataio.jobstore.distributed.StatusChangeEvent;
import dk.dbc.dataio.jobstore.distributed.TrackingKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;

public class UpdateStatus implements EntryProcessor<TrackingKey, DependencyTracking, StatusChangeEvent> {
    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateStatus.class);
    public final boolean validateUpdate;
    public final ChunkSchedulingStatus schedulingStatus;

    public UpdateStatus(ChunkSchedulingStatus schedulingStatus) {
        this(schedulingStatus, false);
    }

    public UpdateStatus(ChunkSchedulingStatus schedulingStatus, boolean validateUpdate) {
        this.schedulingStatus = Objects.requireNonNull(schedulingStatus, "schedulingStatus cannot be null");
        this.validateUpdate = validateUpdate;
    }

    @Override
    public StatusChangeEvent process(Map.Entry<TrackingKey, DependencyTracking> entry) {
        DependencyTracking dt = entry.getValue();
        if(dt == null) return null;
        if(validateUpdate && dt.getStatus().isInvalidStatusChange(schedulingStatus)) return null;
        ChunkSchedulingStatus status = schedulingStatus;
        if(status == ChunkSchedulingStatus.READY_FOR_DELIVERY && !dt.getWaitingOn().isEmpty()) status = ChunkSchedulingStatus.BLOCKED;
        StatusChangeEvent event = new StatusChangeEvent(dt.getSinkId(), dt.getStatus(), status);
        dt.setStatus(status);
        entry.setValue(dt);
        LOGGER.debug("Status update on {} - {}", entry.getKey(), schedulingStatus);
        return event;
    }
}
