package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.jobstore.service.entity.ChunkEntity;
import dk.dbc.dataio.jobstore.service.entity.ItemEntity;
import dk.dbc.dataio.jobstore.service.entity.JobEntity;
import dk.dbc.dataio.jobstore.types.ItemDeliveryResult;
import dk.dbc.dataio.jobstore.types.ItemDeliveryResult.Status;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import dk.dbc.dataio.jobstore.types.SequenceAnalysisData;
import dk.dbc.dataio.jobstore.types.State;
import dk.dbc.dataio.jobstore.types.StateChange;
import jakarta.persistence.Query;
import types.TestableJobEntityBuilder;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import static dk.dbc.dataio.jobstore.types.State.Phase.DELIVERING;
import static dk.dbc.dataio.jobstore.types.State.Phase.PARTITIONING;
import static dk.dbc.dataio.jobstore.types.State.Phase.PROCESSING;
import static jakarta.persistence.LockModeType.PESSIMISTIC_WRITE;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PgJobStore_AddItemDeliveredTest extends PgJobStoreBaseTest {
    private static final int JOB_ID = 1;
    private static final int CHUNK_ID = 0;
    private static final short ITEM_ID = 0;
    private static final long SINK_ID = 42L;
    private static final String RECORD_KEY = "870970:12345678";

    @org.junit.Test
    public void addItemDelivered_itemNotFound_throwsJobStoreException() {
        when(entityManager.find(eq(ItemEntity.class), any(ItemEntity.Key.class))).thenReturn(null);

        PgJobStore pgJobStore = newPgJobStore(newPgJobStoreReposity());
        assertThrows(JobStoreException.class, () ->
                pgJobStore.addItemDelivered(JOB_ID, CHUNK_ID, ITEM_ID, new ItemDeliveryResult(
                        SINK_ID, RECORD_KEY, Status.DELIVERED, ChunkItem.successfulChunkItem().withId(ITEM_ID))));
    }

    @org.junit.Test
    public void addItemDelivered_alreadyDelivered_isIdempotentNoOp() throws JobStoreException {
        ItemEntity itemEntity = getItemEntity();
        itemEntity.setDeliveringOutcome(ChunkItem.successfulChunkItem().withId(ITEM_ID));
        when(entityManager.find(eq(ItemEntity.class), any(ItemEntity.Key.class))).thenReturn(itemEntity);
        // chunk not otherwise done: proves this replay does not apply a second delta
        when(entityManager.find(eq(ChunkEntity.class), any(ChunkEntity.Key.class))).thenReturn(getChunkEntity(2));

        PgJobStore pgJobStore = newPgJobStore(newPgJobStoreReposity());
        boolean chunkDeliveringDone = pgJobStore.addItemDelivered(
                JOB_ID, CHUNK_ID, ITEM_ID, new ItemDeliveryResult(
                        SINK_ID, RECORD_KEY, Status.DELIVERED, ChunkItem.successfulChunkItem().withId(ITEM_ID)));

        assertThat("chunk delivering done", chunkDeliveringDone, is(false));
        verify(entityManager, never()).find(eq(ChunkEntity.class), any(ChunkEntity.Key.class), eq(PESSIMISTIC_WRITE));
        verify(entityManager, never()).createNativeQuery(anyString());
    }

    @org.junit.Test
    public void addItemDelivered_alreadyDelivered_reportsChunkAlreadyDone() throws JobStoreException {
        ItemEntity itemEntity = getItemEntity();
        itemEntity.setDeliveringOutcome(ChunkItem.successfulChunkItem().withId(ITEM_ID));
        when(entityManager.find(eq(ItemEntity.class), any(ItemEntity.Key.class))).thenReturn(itemEntity);
        ChunkEntity chunkEntity = getChunkEntity(1);
        chunkEntity.getState().updateState(new StateChange().setPhase(DELIVERING)
                .setSucceeded(1).setBeginDate(new Date()).setEndDate(new Date()));
        when(entityManager.find(eq(ChunkEntity.class), any(ChunkEntity.Key.class))).thenReturn(chunkEntity);

        PgJobStore pgJobStore = newPgJobStore(newPgJobStoreReposity());
        boolean chunkDeliveringDone = pgJobStore.addItemDelivered(
                JOB_ID, CHUNK_ID, ITEM_ID, new ItemDeliveryResult(
                        SINK_ID, RECORD_KEY, Status.DELIVERED, ChunkItem.successfulChunkItem().withId(ITEM_ID)));

        assertThat("a redelivery after the chunk is already fully done still reports done, "
                + "so a crash between commit and the scheduler notification is self-healing",
                chunkDeliveringDone, is(true));
        verify(entityManager, never()).find(eq(ChunkEntity.class), any(ChunkEntity.Key.class), eq(PESSIMISTIC_WRITE));
        verify(entityManager, never()).createNativeQuery(anyString());
    }

    @org.junit.Test
    public void addItemDelivered_deliveredWithNullRecordKey_doesNotUpsertWatermark() throws JobStoreException {
        ItemEntity itemEntity = getItemEntity();
        ChunkEntity chunkEntity = getChunkEntity(1);
        JobEntity jobEntity = buildJobEntity(1);
        mockNativeQuery();

        when(entityManager.find(eq(ItemEntity.class), any(ItemEntity.Key.class))).thenReturn(itemEntity);
        when(entityManager.find(eq(ItemEntity.class), any(ItemEntity.Key.class), eq(PESSIMISTIC_WRITE))).thenReturn(itemEntity);
        when(entityManager.find(eq(ChunkEntity.class), any(ChunkEntity.Key.class), eq(PESSIMISTIC_WRITE))).thenReturn(chunkEntity);
        when(entityManager.find(eq(JobEntity.class), anyInt(), eq(PESSIMISTIC_WRITE))).thenReturn(jobEntity);

        PgJobStore pgJobStore = newPgJobStore(newPgJobStoreReposity());
        pgJobStore.addItemDelivered(JOB_ID, CHUNK_ID, ITEM_ID, new ItemDeliveryResult(
                SINK_ID, null, Status.DELIVERED, ChunkItem.successfulChunkItem().withId(ITEM_ID)));

        verify(entityManager, never()).createNativeQuery(anyString());
    }

    /**
     * The verdict feeds the counters, not the outcome item's status, so a sink reporting an
     * item it never sent has to say IGNORED for the item to be counted as ignored the way
     * the chunk-level path counted it, and for the record's watermark to stay where it is.
     * Reporting DELIVERED with an IGNORE item instead counts as succeeded and claims a
     * delivery that never happened.
     */
    @org.junit.Test
    public void addItemDelivered_ignored_countsAsIgnoredAndLeavesWatermarkUntouched() throws JobStoreException {
        ItemEntity itemEntity = getItemEntity();
        ChunkEntity chunkEntity = getChunkEntity(1);
        JobEntity jobEntity = buildJobEntity(1);
        mockNativeQuery();

        when(entityManager.find(eq(ItemEntity.class), any(ItemEntity.Key.class))).thenReturn(itemEntity);
        when(entityManager.find(eq(ItemEntity.class), any(ItemEntity.Key.class), eq(PESSIMISTIC_WRITE))).thenReturn(itemEntity);
        when(entityManager.find(eq(ChunkEntity.class), any(ChunkEntity.Key.class), eq(PESSIMISTIC_WRITE))).thenReturn(chunkEntity);
        when(entityManager.find(eq(JobEntity.class), anyInt(), eq(PESSIMISTIC_WRITE))).thenReturn(jobEntity);

        PgJobStore pgJobStore = newPgJobStore(newPgJobStoreReposity());
        pgJobStore.addItemDelivered(JOB_ID, CHUNK_ID, ITEM_ID, new ItemDeliveryResult(
                SINK_ID, RECORD_KEY, Status.IGNORED, ChunkItem.ignoredChunkItem().withId(ITEM_ID)));

        assertThat("item ignored in delivering",
                itemEntity.getState().getPhase(DELIVERING).getIgnored(), is(1));
        assertThat("item succeeded in delivering",
                itemEntity.getState().getPhase(DELIVERING).getSucceeded(), is(0));
        assertThat("chunk ignored in delivering",
                chunkEntity.getState().getPhase(DELIVERING).getIgnored(), is(1));
        assertThat("job ignored in delivering",
                jobEntity.getState().getPhase(DELIVERING).getIgnored(), is(1));
        verify(entityManager, never()).createNativeQuery(anyString());
    }

    @org.junit.Test
    public void addItemDelivered_terminationItemFailed_marksJobFatalError() throws JobStoreException {
        ItemEntity itemEntity = getItemEntity();
        itemEntity.setProcessingOutcome(ChunkItem.failedChunkItem().withId(ITEM_ID)
                .withData("Job termination item").withType(ChunkItem.Type.JOB_END));
        ChunkEntity chunkEntity = getChunkEntity(1);
        JobEntity jobEntity = buildJobEntity(1);
        mockNativeQuery();

        when(entityManager.find(eq(ItemEntity.class), any(ItemEntity.Key.class))).thenReturn(itemEntity);
        when(entityManager.find(eq(ItemEntity.class), any(ItemEntity.Key.class), eq(PESSIMISTIC_WRITE))).thenReturn(itemEntity);
        when(entityManager.find(eq(ChunkEntity.class), any(ChunkEntity.Key.class), eq(PESSIMISTIC_WRITE))).thenReturn(chunkEntity);
        when(entityManager.find(eq(JobEntity.class), anyInt(), eq(PESSIMISTIC_WRITE))).thenReturn(jobEntity);

        PgJobStore pgJobStore = newPgJobStore(newPgJobStoreReposity());
        boolean chunkDeliveringDone = pgJobStore.addItemDelivered(
                JOB_ID, CHUNK_ID, ITEM_ID, new ItemDeliveryResult(
                        SINK_ID, null, Status.FAILED, ChunkItem.failedChunkItem().withId(ITEM_ID)));

        assertThat("chunk delivering done", chunkDeliveringDone, is(true));
        assertThat("job fatal error", jobEntity.hasFatalError(), is(true));
        assertThat("job time of completion", jobEntity.getTimeOfCompletion(), is(notNullValue()));
    }

    private Query mockNativeQuery() {
        Query query = mock(Query.class);
        when(query.setParameter(anyInt(), any())).thenReturn(query);
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        return query;
    }

    private ItemEntity getItemEntity() {
        ItemEntity itemEntity = new ItemEntity();
        itemEntity.setKey(new ItemEntity.Key(JOB_ID, CHUNK_ID, ITEM_ID));
        itemEntity.setState(closedPhases(1, List.of(PARTITIONING, PROCESSING)));
        return itemEntity;
    }

    private ChunkEntity getChunkEntity(int numberOfItems) {
        ChunkEntity chunkEntity = new ChunkEntity();
        chunkEntity.setKey(new ChunkEntity.Key(CHUNK_ID, JOB_ID));
        chunkEntity.setNumberOfItems((short) numberOfItems);
        chunkEntity.setSequenceAnalysisData(new SequenceAnalysisData(Collections.emptySet()));
        chunkEntity.setState(closedPhases(numberOfItems, List.of(PARTITIONING, PROCESSING)));
        return chunkEntity;
    }

    private JobEntity buildJobEntity(int numberOfItems) {
        return new TestableJobEntityBuilder()
                .setNumberOfItems(numberOfItems)
                .setState(closedPhases(numberOfItems, List.of(PARTITIONING, PROCESSING)))
                .build();
    }

    private State closedPhases(int succeeded, List<State.Phase> phases) {
        State state = new State();
        StateChange stateChange = new StateChange();
        for (State.Phase phase : phases) {
            stateChange.setPhase(phase)
                    .setSucceeded(succeeded)
                    .setBeginDate(new Date())
                    .setEndDate(new Date());
            state.updateState(stateChange);
        }
        return state;
    }
}
