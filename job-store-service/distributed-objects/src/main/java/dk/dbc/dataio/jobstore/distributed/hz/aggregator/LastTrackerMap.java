package dk.dbc.dataio.jobstore.distributed.hz.aggregator;

import com.hazelcast.aggregation.Aggregator;
import dk.dbc.dataio.jobstore.distributed.DependencyTracking;
import dk.dbc.dataio.jobstore.distributed.TrackingKey;
import dk.dbc.dataio.jobstore.distributed.WaitFor;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

public class LastTrackerMap implements Aggregator<Map.Entry<TrackingKey, DependencyTracking>, Map<WaitFor, TrackingKey>> {
    Map<WaitFor, TrackingKey> trackers = new HashMap<>();

    @Override
    public void accumulate(Map.Entry<TrackingKey, DependencyTracking> entry) {
        Set<WaitFor> waitFor = entry.getValue().getWaitFor();
        for (WaitFor aFor : waitFor) {
            trackers.merge(aFor, entry.getKey(), this::merge);
        }
    }

    private TrackingKey merge(TrackingKey left, TrackingKey right) {
        if (left == null) return right;
        if (right == null) return left;
        return left.compareTo(right) >= 0 ? left : right;
    }

    @Override
    public void combine(Aggregator aggregator) {
        LastTrackerMap lastTracker = getClass().cast(aggregator);
        HashMap<WaitFor, TrackingKey> result = new HashMap<>();
        Stream.of(lastTracker.trackers.keySet(), trackers.keySet()).flatMap(Collection::stream)
                .forEach(wf -> result.put(wf, merge(trackers.get(wf), lastTracker.trackers.get(wf))));
        trackers = result;
    }

    @Override
    public Map<WaitFor, TrackingKey> aggregate() {
        return trackers;
    }
}
