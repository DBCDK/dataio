package dk.dbc.dataio.jobstore.service.entity;

import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.utils.test.model.ChunkItemBuilder;
import dk.dbc.dataio.jobstore.test.types.WorkflowNoteBuilder;
import dk.dbc.dataio.jobstore.types.ItemInfoSnapshot;
import dk.dbc.dataio.jobstore.types.RecordInfo;
import dk.dbc.dataio.jobstore.types.State;
import org.junit.Test;

import java.sql.Timestamp;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.fail;

public class ItemEntityTest {
    private final static ItemEntity.Key KEY = new ItemEntity.Key(2, 1, (short) 0);

    @Test
    public void getChunkItemForPhase_phaseArgIsNull_throws() {
        final ItemEntity entity = new ItemEntity();
        entity.setKey(KEY);
        entity.setPartitioningOutcome(new ChunkItemBuilder().build());
        final State state = new State();
        state.getPhase(State.Phase.PARTITIONING).withSucceeded(1);
        entity.setState(state);
        try {
            entity.getChunkItemForPhase(null);
            fail("No Exception thrown");
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void getChunkItemForPhase_calledWithPartitioningPhase_returnsChunkItem() {
        final ChunkItem expectedChunkItem = new ChunkItemBuilder().setId(KEY.getId()).setStatus(ChunkItem.Status.SUCCESS).build();
        final ItemEntity entity = new ItemEntity();
        entity.setKey(KEY);
        entity.setPartitioningOutcome(expectedChunkItem);
        final State state = new State();
        state.getPhase(State.Phase.PARTITIONING).withSucceeded(1);
        entity.setState(state);
        assertThat(entity.getChunkItemForPhase(State.Phase.PARTITIONING), is(expectedChunkItem));
    }

    @Test
    public void getChunkItemForPhase_calledWithProcessingPhase_returnsChunkItem() {
        final ChunkItem expectedChunkItem = new ChunkItemBuilder().setId(KEY.getId()).setStatus(ChunkItem.Status.FAILURE).build();
        final ItemEntity entity = new ItemEntity();
        entity.setKey(KEY);
        entity.setProcessingOutcome(expectedChunkItem);
        final State state = new State();
        state.getPhase(State.Phase.PROCESSING).withFailed(1);
        entity.setState(state);
        assertThat(entity.getChunkItemForPhase(State.Phase.PROCESSING), is(expectedChunkItem));
    }

    @Test
    public void getChunkItemForPhase_calledWithDeliveringPhase_returnsChunkItem() {
        final ChunkItem expectedChunkItem = new ChunkItemBuilder().setId(KEY.getId()).setStatus(ChunkItem.Status.IGNORE).build();
        final ItemEntity entity = new ItemEntity();
        entity.setKey(KEY);
        entity.setDeliveringOutcome(expectedChunkItem);
        final State state = new State();
        state.getPhase(State.Phase.DELIVERING).withIgnored(1);
        entity.setState(state);
        assertThat(entity.getChunkItemForPhase(State.Phase.DELIVERING), is(expectedChunkItem));
    }

    @Test
    public void toItemInfoSnapshot() {
        final ItemEntity itemEntity = getItemEntity();
        final ItemInfoSnapshot itemInfoSnapshot = itemEntity.toItemInfoSnapshot();
        assertThat(itemInfoSnapshot, is(notNullValue()));
        assertItemInfoSnapshotEquals(itemInfoSnapshot, itemEntity);
        assertThat(itemInfoSnapshot.getItemNumber(), is(24));
    }

    @Test
    public void getZeroBasedIndex() {
        assertThat("0 index", new ItemEntity.Key(42, 0, (short) 0).getZeroBasedIndex(), is(0));
        assertThat("9 index", new ItemEntity.Key(42, 0, (short) 9).getZeroBasedIndex(), is(9));
        assertThat("10 index", new ItemEntity.Key(42, 1, (short) 0).getZeroBasedIndex(), is(10));
        assertThat("19 index", new ItemEntity.Key(42, 1, (short) 9).getZeroBasedIndex(), is(19));
        assertThat("20 index", new ItemEntity.Key(42, 2, (short) 0).getZeroBasedIndex(), is(20));
    }

    @Test
    public void getOneBasedIndex() {
        assertThat("1 index", new ItemEntity.Key(42, 0, (short) 0).getOneBasedIndex(), is(1));
        assertThat("10 index", new ItemEntity.Key(42, 0, (short) 9).getOneBasedIndex(), is(10));
        assertThat("11 index", new ItemEntity.Key(42, 1, (short) 0).getOneBasedIndex(), is(11));
        assertThat("20 index", new ItemEntity.Key(42, 1, (short) 9).getOneBasedIndex(), is(20));
        assertThat("21 index", new ItemEntity.Key(42, 2, (short) 0).getOneBasedIndex(), is(21));
    }

    private ItemEntity getItemEntity() {
        ItemEntity itemEntity = new ItemEntity();
        itemEntity.setKey(new ItemEntity.Key(1, 2, (short) 3));
        itemEntity.setState(new State());
        itemEntity.setPartitioningOutcome(new ChunkItemBuilder().setData("PartitioningData").build());
        itemEntity.setProcessingOutcome(new ChunkItemBuilder().setData("ProcessingData").build());
        itemEntity.setDeliveringOutcome(new ChunkItemBuilder().setData("DeliveringData").build());
        itemEntity.setTimeOfCompletion(new Timestamp(System.currentTimeMillis()));
        itemEntity.setWorkflowNote(new WorkflowNoteBuilder().build());
        itemEntity.setRecordInfo(new RecordInfo("42"));
        return itemEntity;
    }

    private void assertItemInfoSnapshotEquals(ItemInfoSnapshot itemInfoSnapshot, ItemEntity itemEntity) {
        assertThat(itemInfoSnapshot.getItemId(), is(itemEntity.getKey().getId()));
        assertThat(itemInfoSnapshot.getChunkId(), is(itemEntity.getKey().getChunkId()));
        assertThat(itemInfoSnapshot.getJobId(), is(itemEntity.getKey().getJobId()));
        assertThat(itemInfoSnapshot.getState(), is(itemEntity.getState()));
        assertThat(itemInfoSnapshot.getTimeOfCompletion().getTime(), is(itemEntity.getTimeOfCompletion().getTime()));
        assertThat(itemInfoSnapshot.getWorkflowNote(), is(itemEntity.getWorkflowNote()));
        assertThat(itemInfoSnapshot.getRecordInfo(), is(itemEntity.getRecordInfo()));
        assertThat(itemInfoSnapshot.getTrackingId(), is(itemEntity.getPartitioningOutcome().getTrackingId()));
    }
}
