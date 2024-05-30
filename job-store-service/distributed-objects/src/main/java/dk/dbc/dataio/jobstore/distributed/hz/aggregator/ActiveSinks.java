package dk.dbc.dataio.jobstore.distributed.hz.aggregator;

import com.hazelcast.aggregation.Aggregator;
import dk.dbc.dataio.jobstore.distributed.DependencyTracking;
import dk.dbc.dataio.jobstore.distributed.TrackingKey;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ActiveSinks implements Aggregator<Map.Entry<TrackingKey, DependencyTracking>, Set<Integer>> {
    private final Set<Integer> sinks = new HashSet<>();

    @Override
    public void accumulate(Map.Entry<TrackingKey, DependencyTracking> entry) {
        sinks.add(entry.getValue().getSinkId());
    }

    @Override
    public void combine(Aggregator aggregator) {
        ActiveSinks oActiveSinks = (ActiveSinks) aggregator;
        sinks.addAll(oActiveSinks.sinks);
    }

    @Override
    public Set<Integer> aggregate() {
        return sinks;
    }
}
