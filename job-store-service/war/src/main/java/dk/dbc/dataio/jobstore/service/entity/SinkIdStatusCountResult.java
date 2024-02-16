package dk.dbc.dataio.jobstore.service.entity;

import dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus;

/**
 * Created by ja7 on 19-07-16.
 * <p>
 * Result Class for NamedQuery used by bootStrapcode
 */

public class SinkIdStatusCountResult {
    public int sinkId;
    public ChunkSchedulingStatus status;
    public int count;

    static final ChunkSchedulingStatusConverter converter = new ChunkSchedulingStatusConverter();

    public SinkIdStatusCountResult(Integer sinkId, Integer status, Long count) {
        this.sinkId = sinkId;
        this.status = converter.convertToEntityAttribute(status);
        this.count = Math.toIntExact(count);
    }

    public SinkIdStatusCountResult(int sinkId, ChunkSchedulingStatus status, int count) {
        this.sinkId = sinkId;
        this.status = status;
        this.count = count;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SinkIdStatusCountResult that = (SinkIdStatusCountResult) o;

        if (sinkId != that.sinkId) return false;
        if (count != that.count) return false;
        return status == that.status;

    }

    @Override
    public int hashCode() {
        int result = sinkId;
        result = 31 * result + (status != null ? status.hashCode() : 0);
        result = 31 * result + (int) (count ^ (count >>> 32));
        return result;
    }

    public String toString() {
        return "SinkIdStatusCountResult{" +
                "sinkId=" + sinkId +
                ", status=" + status +
                ", count=" + count +
                '}';
    }
}
