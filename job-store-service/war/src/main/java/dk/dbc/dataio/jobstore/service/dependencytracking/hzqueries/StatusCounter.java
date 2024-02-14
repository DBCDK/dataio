package dk.dbc.dataio.jobstore.service.dependencytracking.hzqueries;

import com.hazelcast.aggregation.Aggregator;
import dk.dbc.dataio.jobstore.service.ejb.JobSchedulerSinkStatus;
import dk.dbc.dataio.jobstore.service.entity.DependencyTracking;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class StatusCounter implements Aggregator<Map.Entry<DependencyTracking.Key, DependencyTracking>, Map<Long, JobSchedulerSinkStatus>> {
    private final Map<Long, JobSchedulerSinkStatus> map = new HashMap<>();
    private final Set<Long> sinkFilter;

    public StatusCounter(Set<Long> sinkFilter) {
        this.sinkFilter = sinkFilter;
    }

    @Override
    public void accumulate(Map.Entry<DependencyTracking.Key, DependencyTracking> entry) {
        DependencyTracking dt = entry.getValue();
        if(sinkFilter.isEmpty() || sinkFilter.contains((long)dt.getSinkid())) {
            JobSchedulerSinkStatus sinkStatus = map.computeIfAbsent((long)dt.getSinkid(), id -> new JobSchedulerSinkStatus());
            dt.getStatus().countSinkStatus(sinkStatus);
        }
    }

    @Override
    public void combine(Aggregator aggregator) {
        StatusCounter other = getClass().cast(aggregator);
        for (Map.Entry<Long, JobSchedulerSinkStatus> entry : other.map.entrySet()) {
            JobSchedulerSinkStatus sinkStatus = map.computeIfAbsent(entry.getKey(), k -> new JobSchedulerSinkStatus());
            sinkStatus.mergeCounters(entry.getValue());
        }
    }

    @Override

    public Map<Long, JobSchedulerSinkStatus> aggregate() {
        return map;
    }
}
