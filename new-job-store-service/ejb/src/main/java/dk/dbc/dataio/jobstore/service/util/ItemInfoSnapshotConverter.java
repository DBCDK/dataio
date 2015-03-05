package dk.dbc.dataio.jobstore.service.util;

import dk.dbc.dataio.jobstore.service.entity.ItemEntity;
import dk.dbc.dataio.jobstore.types.ItemInfoSnapshot;

import java.sql.Timestamp;
import java.util.Date;

public class ItemInfoSnapshotConverter {

    private static final int CHUNK_SIZE = 10;

    /**
     * Private Constructor prevents instantiation of this static class
     */
    private ItemInfoSnapshotConverter() {}

    /**
     *
     * Maps information from an item entity to a ItemInfoSnapshot
     *
     * @param itemEntity the item entity
     * @return ItemInfoSnapshot containing information about the item from one exact moment in time (now)
     */
    public static ItemInfoSnapshot toItemInfoSnapshot(ItemEntity itemEntity) {
        return new ItemInfoSnapshot(
                calculateItemNumber(itemEntity.getKey()),
                itemEntity.getKey().getId(),
                itemEntity.getKey().getChunkId(),
                itemEntity.getKey().getJobId(),
                toDate(itemEntity.getTimeOfCreation()),
                toDate(itemEntity.getTimeOfLastModification()),
                toDate(itemEntity.getTimeOfCompletion()),
                itemEntity.getState());
    }

    private static int calculateItemNumber(ItemEntity.Key key) {
        return key.getChunkId() * CHUNK_SIZE + key.getId();
    }


    /**
     * Converts a java.sql.Timestamp to a java.util.Date
     *
     * @param timestamp to convert
     * @return new Date representation of the timestamp, null if the timestamp is null
     */
    private static Date toDate(Timestamp timestamp) {
        if (timestamp != null) {
            return new Date(timestamp.getTime());

        } else {
            return null;
        }
    }

}
