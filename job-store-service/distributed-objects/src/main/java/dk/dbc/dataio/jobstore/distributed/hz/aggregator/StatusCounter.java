package dk.dbc.dataio.jobstore.distributed.hz.aggregator;

import com.hazelcast.aggregation.Aggregator;
import dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus;
import dk.dbc.dataio.jobstore.distributed.DependencyTracking;
import dk.dbc.dataio.jobstore.distributed.TrackingKey;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class StatusCounter implements Aggregator<Map.Entry<TrackingKey, DependencyTracking>, Map<Integer, Map<ChunkSchedulingStatus, Integer>>> {
    private final Map<Integer, Map<ChunkSchedulingStatus, Integer>> map = new HashMap<>();
    private final Set<Integer> sinkFilter;

    public StatusCounter(Set<Integer> sinkFilter) {
        this.sinkFilter = Objects.requireNonNull(sinkFilter);
    }

    @Override
    public void accumulate(Map.Entry<TrackingKey, DependencyTracking> entry) {
        DependencyTracking dt = entry.getValue();
        if(sinkFilter.isEmpty() || sinkFilter.contains(dt.getSinkId())) {
            Map<ChunkSchedulingStatus, Integer> sinkStatus = map.computeIfAbsent(dt.getSinkId(), k -> new EnumMap<>(ChunkSchedulingStatus.class));
            sinkStatus.merge(entry.getValue().getStatus(), 1, Integer::sum);
        }
    }

    @Override
    public void combine(Aggregator aggregator) {
        StatusCounter oCounter = (StatusCounter) aggregator;
        for (Map.Entry<Integer, Map<ChunkSchedulingStatus, Integer>> oMap : oCounter.map.entrySet()) {
            map.merge(oMap.getKey(), oMap.getValue(), this::mergeSink);
        }
    }

    private Map<ChunkSchedulingStatus, Integer> mergeSink(Map<ChunkSchedulingStatus, Integer> map1, Map<ChunkSchedulingStatus, Integer> map2) {
        EnumMap<ChunkSchedulingStatus, Integer> result = new EnumMap<>(ChunkSchedulingStatus.class);
        Arrays.stream(ChunkSchedulingStatus.values()).forEach(cs -> result.put(cs, map1.getOrDefault(cs, 0) + map2.getOrDefault(cs, 0)));
        return result;
    }

    @Override
    public Map<Integer, Map<ChunkSchedulingStatus, Integer>> aggregate() {
        return map;
    }
}
