package dk.dbc.dataio.jobstore.distributed.hz.processor;

import com.hazelcast.map.EntryProcessor;
import dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus;

import java.util.Map;

public class UpdateCounterProcessor implements EntryProcessor<Integer, Map<ChunkSchedulingStatus, Integer>, Void> {
    private final Map<ChunkSchedulingStatus, Integer> deltas;

    public UpdateCounterProcessor(Map<ChunkSchedulingStatus, Integer> deltas) {
        this.deltas = deltas;
    }

    public UpdateCounterProcessor(ChunkSchedulingStatus status, int delta) {
        this(Map.of(status, delta));
    }

    @Override
    public Void process(Map.Entry<Integer, Map<ChunkSchedulingStatus, Integer>> entry) {
        Map<ChunkSchedulingStatus, Integer> current = entry.getValue();
        deltas.forEach((k, v) -> current.merge(k, v, Integer::sum));
        return null;
    }
}
