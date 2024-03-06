package dk.dbc.dataio.jobstore.distributed;

import java.io.Serializable;
import java.util.Map;

public class StatusChangeEvent implements Serializable {
    public final int sinkId;
    public final ChunkSchedulingStatus oldStatus;
    public final ChunkSchedulingStatus newStatus;

    public StatusChangeEvent(int sinkId, ChunkSchedulingStatus oldStatus, ChunkSchedulingStatus newStatus) {
        this.sinkId = sinkId;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
    }

    public void apply(Map<Integer, JobSchedulerSinkStatus> sinkStatusMap) {
        JobSchedulerSinkStatus sinkStatus = sinkStatusMap.get(sinkId);
        if(oldStatus != null) oldStatus.decSinkStatusCount(sinkStatus);
        if(newStatus != null) newStatus.incSinkStatusCount(sinkStatus);
    }
}
