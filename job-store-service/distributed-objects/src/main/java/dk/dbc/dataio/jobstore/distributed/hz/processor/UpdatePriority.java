package dk.dbc.dataio.jobstore.distributed.hz.processor;

import com.hazelcast.map.EntryProcessor;
import dk.dbc.dataio.jobstore.distributed.DependencyTracking;
import dk.dbc.dataio.jobstore.distributed.TrackingKey;

import java.util.Map;
import java.util.Set;

public class UpdatePriority implements EntryProcessor<TrackingKey, DependencyTracking, Set<TrackingKey>> {
    private static final long serialVersionUID = 1L;
    final int priority;

    public UpdatePriority(int priority) {
        this.priority = priority;
    }


    @Override
    public Set<TrackingKey> process(Map.Entry<TrackingKey, DependencyTracking> entry) {
        DependencyTracking dt = entry.getValue();
        if(dt == null) return null;
        dt.setPriority(priority);
        entry.setValue(dt);
        return dt.getWaitingOn();
    }
}
