package dk.dbc.dataio.jobstore.service.dependencytracking.hzqueries;

import com.hazelcast.aggregation.Aggregator;
import dk.dbc.dataio.jobstore.service.entity.DependencyTracking;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class BlockedCounter implements Aggregator<Map.Entry<DependencyTracking.Key, DependencyTracking>, Map<Long, Integer>> {
    private final Map<Long, AtomicInteger> map = new HashMap<>();
    private final DependencyTracking.ChunkSchedulingStatus status;

    public BlockedCounter(DependencyTracking.ChunkSchedulingStatus status) {
        this.status = status;
    }

    @Override
    public void accumulate(Map.Entry<DependencyTracking.Key, DependencyTracking> entry) {
        DependencyTracking dt = entry.getValue();
        if(dt.getStatus() == status) {
            map.computeIfAbsent((long)dt.getSinkid(), id -> new AtomicInteger(0)).incrementAndGet();
        }
    }

    @Override
    public void combine(Aggregator aggregator) {
        BlockedCounter other = getClass().cast(aggregator);
        for (Map.Entry<Long, AtomicInteger> entry : other.map.entrySet()) {
            map.computeIfAbsent(entry.getKey(), k -> new AtomicInteger(0)).addAndGet(entry.getValue().get());
        }
    }

    @Override

    public Map<Long, Integer> aggregate() {
        return map.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get()));
    }
}
