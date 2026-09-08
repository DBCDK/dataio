package dk.dbc.dataio.jobstore.distributed.hz.processor;

import com.hazelcast.map.impl.MapEntrySimple;
import dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus.QUEUED_FOR_DELIVERY;
import static dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus.QUEUED_FOR_PROCESSING;
import static dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus.READY_FOR_DELIVERY;
import static dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus.READY_FOR_PROCESSING;
import static dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus.SCHEDULED_FOR_DELIVERY;

public class ProcessorsTest {
    @Test
    public void updateCounter() {
        UpdateCounter update = new UpdateCounter(Map.of(SCHEDULED_FOR_DELIVERY, 2, READY_FOR_DELIVERY, -1));
        Map<ChunkSchedulingStatus, Integer> sinkStatus = Map.of(
                READY_FOR_PROCESSING, 1,
                QUEUED_FOR_PROCESSING, 2,
                SCHEDULED_FOR_DELIVERY, 3,
                READY_FOR_DELIVERY, 4,
                QUEUED_FOR_DELIVERY, 5);
        MapEntrySimple<Integer, Map<ChunkSchedulingStatus, Integer>> entry = new MapEntrySimple<>(0, new HashMap<>(sinkStatus));
        update.process(entry);
        Map<ChunkSchedulingStatus, Integer> expected = Map.of(
                READY_FOR_PROCESSING, 1,
                QUEUED_FOR_PROCESSING, 2,
                SCHEDULED_FOR_DELIVERY, 5,
                READY_FOR_DELIVERY, 3,
                QUEUED_FOR_DELIVERY, 5);
        Assertions.assertEquals(entry.getValue(), expected);
        Assertions.assertTrue(entry.isModified());
    }
}
