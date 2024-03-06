package dk.dbc.dataio.jobstore.distributed;

import java.io.Serializable;
import java.util.Map;

public class StatusChangeEvent implements Serializable {
    private int sinkId;
    private ChunkSchedulingStatus oldStatus;
    private ChunkSchedulingStatus newStatus;

    public StatusChangeEvent() {
    }

    public StatusChangeEvent(int sinkId, ChunkSchedulingStatus oldStatus, ChunkSchedulingStatus newStatus) {
        this.sinkId = sinkId;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
    }

    public int getSinkId() {
        return sinkId;
    }

    public ChunkSchedulingStatus getOldStatus() {
        return oldStatus;
    }

    public ChunkSchedulingStatus getNewStatus() {
        return newStatus;
    }

    public void apply(Map<Integer, JobSchedulerSinkStatus> sinkStatusMap) {
        JobSchedulerSinkStatus sinkStatus = sinkStatusMap.get(sinkId);
        if(oldStatus != null) oldStatus.decSinkStatusCount(sinkStatus);
        if(newStatus != null) newStatus.incSinkStatusCount(sinkStatus);
    }
}
