package dk.dbc.dataio.jobstore.distributed.hzqueries;

import com.hazelcast.aggregation.Aggregator;
import dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus;
import dk.dbc.dataio.jobstore.distributed.DependencyTracking;
import dk.dbc.dataio.jobstore.distributed.TrackingKey;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class BlockedCounter implements Aggregator<Map.Entry<TrackingKey, DependencyTracking>, Map<Integer, Integer>> {
    private final Map<Integer, AtomicInteger> map = new HashMap<>();
    private final ChunkSchedulingStatus status;

    public BlockedCounter(ChunkSchedulingStatus status) {
        this.status = Objects.requireNonNull(status);
    }

    @Override
    public void accumulate(Map.Entry<TrackingKey, DependencyTracking> entry) {
        DependencyTracking dt = entry.getValue();
        if(dt.getStatus() == status) {
            map.computeIfAbsent(dt.getSinkId(), id -> new AtomicInteger(0)).incrementAndGet();
        }
    }

    @Override
    public void combine(Aggregator aggregator) {
        BlockedCounter other = getClass().cast(aggregator);
        for (Map.Entry<Integer, AtomicInteger> entry : other.map.entrySet()) {
            map.computeIfAbsent(entry.getKey(), k -> new AtomicInteger(0)).addAndGet(entry.getValue().get());
        }
    }

    @Override

    public Map<Integer, Integer> aggregate() {
        return map.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get()));
    }
}
