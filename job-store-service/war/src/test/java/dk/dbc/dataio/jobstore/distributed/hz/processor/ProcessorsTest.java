package dk.dbc.dataio.jobstore.distributed.hz.processor;

import com.hazelcast.map.impl.MapEntrySimple;
import dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus;
import dk.dbc.dataio.jobstore.distributed.DependencyTracking;
import dk.dbc.dataio.jobstore.distributed.StatusChangeEvent;
import dk.dbc.dataio.jobstore.distributed.TrackingKey;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus.BLOCKED;
import static dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus.QUEUED_FOR_DELIVERY;
import static dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus.QUEUED_FOR_PROCESSING;
import static dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus.READY_FOR_DELIVERY;
import static dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus.READY_FOR_PROCESSING;

public class ProcessorsTest {
    @Test
    public void addTerminationWaitingOn() {
        Set<TrackingKey> adds = Set.of(new TrackingKey(5, 0), new TrackingKey(5, 1));
        AddTerminationWaitingOn processor = new AddTerminationWaitingOn(adds);
        DependencyTracking dt = new DependencyTracking(new TrackingKey(1, 1), 0, 0).setWaitingOn(Set.of());
        MapEntrySimple<TrackingKey, DependencyTracking> entry = new MapEntrySimple<>(dt.getKey(), dt);
        StatusChangeEvent event = processor.process(entry);
        Assertions.assertTrue(entry.isModified(), "Entry should have been modified and must call setValue()");
        Assertions.assertEquals(new StatusChangeEvent(0, READY_FOR_PROCESSING, BLOCKED), event,
                "Entry should have been modified and must return the matching status change");
        MapEntrySimple<TrackingKey, DependencyTracking> sameEntry = new MapEntrySimple<>(dt.getKey(), dt);
        StatusChangeEvent noEvent = processor.process(sameEntry);
        Assertions.assertFalse(sameEntry.isModified(), "Processing causes no change");
        Assertions.assertNull(noEvent, "A null must be returned");
    }

    @Test
    public void updateCounter() {
        UpdateCounter update = new UpdateCounter(Map.of(BLOCKED, 2, READY_FOR_DELIVERY, -1));
        Map<ChunkSchedulingStatus, Integer> sinkStatus = Map.of(
                READY_FOR_PROCESSING, 1,
                QUEUED_FOR_PROCESSING, 2,
                BLOCKED, 3,
                READY_FOR_DELIVERY, 4,
                QUEUED_FOR_DELIVERY, 5);
        MapEntrySimple<Integer, Map<ChunkSchedulingStatus, Integer>> entry = new MapEntrySimple<>(0, new HashMap<>(sinkStatus));
        update.process(entry);
        Map<ChunkSchedulingStatus, Integer> expected = Map.of(
                READY_FOR_PROCESSING, 1,
                QUEUED_FOR_PROCESSING, 2,
                BLOCKED, 5,
                READY_FOR_DELIVERY, 3,
                QUEUED_FOR_DELIVERY, 5);
        Assertions.assertEquals(entry.getValue(), expected);
        Assertions.assertTrue(entry.isModified());
    }
}
