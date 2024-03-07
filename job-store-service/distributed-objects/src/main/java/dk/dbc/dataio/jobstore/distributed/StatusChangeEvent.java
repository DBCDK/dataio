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

    public void apply(Map<ChunkSchedulingStatus, Integer> diff) {
        diff.merge(oldStatus, -1, Integer::sum);
        diff.merge(newStatus, 1, Integer::sum);
    }
}
