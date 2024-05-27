package dk.dbc.dataio.jobstore.distributed.hz.processor;

import com.hazelcast.map.EntryProcessor;
import dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus;

import java.util.Map;
import java.util.Objects;

public class UpdateCounter implements EntryProcessor<Integer, Map<ChunkSchedulingStatus, Integer>, Void> {
    public final Map<ChunkSchedulingStatus, Integer> deltas;

    public UpdateCounter(Map<ChunkSchedulingStatus, Integer> deltas) {
        this.deltas = Objects.requireNonNull(deltas);
    }

    public UpdateCounter(ChunkSchedulingStatus status, int delta) {
        this(Map.of(Objects.requireNonNull(status), delta));
    }

    @Override
    public Void process(Map.Entry<Integer, Map<ChunkSchedulingStatus, Integer>> entry) {
        Map<ChunkSchedulingStatus, Integer> current = entry.getValue();
        if(current == null) return null;
        deltas.forEach((k, v) -> current.merge(k, v, Integer::sum));
        entry.setValue(current);
        return null;
    }
}
