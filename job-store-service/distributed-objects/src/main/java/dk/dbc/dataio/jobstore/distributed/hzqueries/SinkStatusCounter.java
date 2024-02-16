package dk.dbc.dataio.jobstore.distributed.hzqueries;

import com.hazelcast.aggregation.Aggregator;
import dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus;
import dk.dbc.dataio.jobstore.distributed.DependencyTracking;
import dk.dbc.dataio.jobstore.distributed.TrackingKey;

import java.util.Map;

public class SinkStatusCounter implements Aggregator<Map.Entry<TrackingKey, DependencyTracking>, Integer> {
    private int count = 0;
    private final int sinkId;
    private final ChunkSchedulingStatus status;

    public SinkStatusCounter(int sinkId, ChunkSchedulingStatus status) {
        this.sinkId = sinkId;
        this.status = status;
    }

    @Override
    public void accumulate(Map.Entry<TrackingKey, DependencyTracking> entry) {
        DependencyTracking dt = entry.getValue();
        if(dt.getSinkId() == sinkId && (status == null || dt.getStatus() == status)) count++;
    }

    @Override
    public void combine(Aggregator aggregator) {
        SinkStatusCounter other = getClass().cast(aggregator);
        count += other.count;
    }

    @Override

    public Integer aggregate() {
        return count;
    }
}
