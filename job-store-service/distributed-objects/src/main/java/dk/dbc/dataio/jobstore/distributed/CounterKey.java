package dk.dbc.dataio.jobstore.distributed;

import java.io.Serializable;

public class CounterKey implements Serializable {
    public final int sinkId;
    public final ChunkSchedulingStatus status;

    public CounterKey(int sinkId, ChunkSchedulingStatus status) {
        this.sinkId = sinkId;
        this.status = status;
    }

    public CounterKey(DependencyTracking dt) {
        this(dt.getSinkId(), dt.getStatus());
    }
}
