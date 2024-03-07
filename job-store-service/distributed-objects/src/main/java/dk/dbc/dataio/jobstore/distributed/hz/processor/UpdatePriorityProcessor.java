package dk.dbc.dataio.jobstore.distributed.hz.processor;

import com.hazelcast.map.EntryProcessor;
import dk.dbc.dataio.jobstore.distributed.DependencyTracking;
import dk.dbc.dataio.jobstore.distributed.TrackingKey;

import java.util.Map;
import java.util.Set;

public class UpdatePriorityProcessor implements EntryProcessor<TrackingKey, DependencyTracking, Set<TrackingKey>> {
    final int priority;

    public UpdatePriorityProcessor(int priority) {
        this.priority = priority;
    }


    @Override
    public Set<TrackingKey> process(Map.Entry<TrackingKey, DependencyTracking> entry) {
        DependencyTracking dt = entry.getValue();
        dt.setPriority(priority);
        entry.setValue(dt);
        return dt.getWaitingOn();
    }
}
