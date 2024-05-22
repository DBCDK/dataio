package dk.dbc.dataio.jobstore.distributed;

import java.util.Map;
import java.util.Objects;

public class StatusChangeEvent {
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StatusChangeEvent that = (StatusChangeEvent) o;
        return sinkId == that.sinkId && oldStatus == that.oldStatus && newStatus == that.newStatus;
    }

    @Override
    public int hashCode() {
        return Objects.hash(sinkId, oldStatus, newStatus);
    }

    @Override
    public String toString() {
        return "StatusChangeEvent{" +
                "sinkId=" + sinkId +
                ", oldStatus=" + oldStatus +
                ", newStatus=" + newStatus +
                '}';
    }
}
