package dk.dbc.dataio.jobstore.distributed.hz.aggregator;

import com.hazelcast.aggregation.Aggregator;
import dk.dbc.dataio.jobstore.distributed.DependencyTracking;
import dk.dbc.dataio.jobstore.distributed.TrackingKey;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class JobCounter implements Aggregator<Map.Entry<TrackingKey, DependencyTracking>, Integer[]> {
    private Set<Integer> jobs = new HashSet<>();
    private int chunkCount = 0;
    private final int sinkId;

    public JobCounter(int sinkId) {
        this.sinkId = sinkId;
    }

    @Override
    public void accumulate(Map.Entry<TrackingKey, DependencyTracking> entry) {
        DependencyTracking dt = entry.getValue();
        if(dt.getSinkId() == sinkId) {
            jobs.add(dt.getKey().getJobId());
            chunkCount++;
        }
    }

    @Override
    public void combine(Aggregator aggregator) {
        JobCounter other = getClass().cast(aggregator);
        jobs.addAll(other.jobs);
        chunkCount += other.chunkCount;
    }

    @Override

    public Integer[] aggregate() {
        return new Integer[] {jobs.size(), chunkCount};
    }
}
