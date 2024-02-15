package dk.dbc.dataio.jobstore.service.dependencytracking.hzqueries;

import com.hazelcast.aggregation.Aggregator;
import dk.dbc.dataio.jobstore.service.dependencytracking.DependencyTracking;
import dk.dbc.dataio.jobstore.service.dependencytracking.TrackingKey;
import dk.dbc.dataio.jobstore.service.ejb.JobSchedulerSinkStatus;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class StatusCounter implements Aggregator<Map.Entry<TrackingKey, DependencyTracking>, Map<Integer, JobSchedulerSinkStatus>> {
    private final Map<Integer, JobSchedulerSinkStatus> map = new HashMap<>();
    private final Set<Integer> sinkFilter;

    public StatusCounter(Set<Integer> sinkFilter) {
        this.sinkFilter = Objects.requireNonNull(sinkFilter);
    }

    @Override
    public void accumulate(Map.Entry<TrackingKey, DependencyTracking> entry) {
        DependencyTracking dt = entry.getValue();
        if(sinkFilter.isEmpty() || sinkFilter.contains(dt.getSinkId())) {
            JobSchedulerSinkStatus sinkStatus = map.computeIfAbsent(dt.getSinkId(), id -> new JobSchedulerSinkStatus());
            dt.getStatus().incSinkStatusCount(sinkStatus);
        }
    }

    @Override
    public void combine(Aggregator aggregator) {
        StatusCounter other = getClass().cast(aggregator);
        for (Map.Entry<Integer, JobSchedulerSinkStatus> entry : other.map.entrySet()) {
            JobSchedulerSinkStatus sinkStatus = map.computeIfAbsent(entry.getKey(), k -> new JobSchedulerSinkStatus());
            sinkStatus.mergeCounters(entry.getValue());
        }
    }

    @Override

    public Map<Integer, JobSchedulerSinkStatus> aggregate() {
        return map;
    }
}
