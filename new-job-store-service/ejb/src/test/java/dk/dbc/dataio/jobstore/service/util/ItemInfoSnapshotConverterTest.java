package dk.dbc.dataio.jobstore.service.util;

import dk.dbc.dataio.jobstore.service.entity.ItemEntity;
import dk.dbc.dataio.jobstore.types.ItemData;
import dk.dbc.dataio.jobstore.types.ItemInfoSnapshot;
import dk.dbc.dataio.jobstore.types.State;
import org.junit.Test;

import java.nio.charset.Charset;
import java.sql.Timestamp;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;

public class ItemInfoSnapshotConverterTest {

    @Test
    public void toItemInfoSnapShot_itemEntityInput_itemInfoSnapshotReturned() {
        ItemEntity itemEntity = getItemEntity();

        // Convert to ItemInfoSnapshot to view information regarding the item from one exact moment in time (now)
        ItemInfoSnapshot itemInfoSnapshot = ItemInfoSnapshotConverter.toItemInfoSnapshot(itemEntity);

        assertThat(itemInfoSnapshot, not(nullValue()));
        assertItemInfoSnapshotEquals(itemInfoSnapshot, itemEntity);
        assertThat(itemInfoSnapshot.getItemNumber(), is(23));
    }

     /*
     * Private methods
     */

    private ItemEntity getItemEntity() {
        ItemEntity itemEntity = new ItemEntity();
        itemEntity.setKey(new ItemEntity.Key(1, 2, (short) 3));
        itemEntity.setState(new State());
        itemEntity.setPartitioningOutcome(new ItemData("PartitioningData", Charset.defaultCharset()));
        itemEntity.setProcessingOutcome(new ItemData("ProcessingData", Charset.defaultCharset()));
        itemEntity.setDeliveringOutcome(new ItemData("DeliveringData", Charset.defaultCharset()));
        itemEntity.setTimeOfCompletion(new Timestamp(System.currentTimeMillis()));
        return itemEntity;
    }

    private void assertItemInfoSnapshotEquals(ItemInfoSnapshot itemInfoSnapshot, ItemEntity itemEntity) {
        assertThat(itemInfoSnapshot.getState(), is(itemEntity.getState()));
        assertThat(itemInfoSnapshot.getItemId(), is(itemEntity.getKey().getId()));
        assertThat(itemInfoSnapshot.getChunkId(), is(itemEntity.getKey().getChunkId()));
        assertThat(itemInfoSnapshot.getJobId(), is(itemEntity.getKey().getJobId()));
        assertThat(itemInfoSnapshot.getState(), is(itemEntity.getState()));
        assertThat(itemInfoSnapshot.getTimeOfCompletion().getTime(), is(itemEntity.getTimeOfCompletion().getTime()));
    }
}
