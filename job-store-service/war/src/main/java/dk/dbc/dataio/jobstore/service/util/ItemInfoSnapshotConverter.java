/*
 * DataIO - Data IO
 * Copyright (C) 2015 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
 * Denmark. CVR: 15149043
 *
 * This file is part of DataIO.
 *
 * DataIO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DataIO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DataIO.  If not, see <http://www.gnu.org/licenses/>.
 */

package dk.dbc.dataio.jobstore.service.util;

import dk.dbc.dataio.jobstore.service.entity.ItemEntity;
import dk.dbc.dataio.jobstore.types.ItemInfoSnapshot;

import java.sql.Timestamp;
import java.util.Date;

public class ItemInfoSnapshotConverter {

    private static final int CHUNK_SIZE = 10; //TODO - this solution only works as long as we have a fixed chunk size of 10

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
                itemEntity.getState(),
                itemEntity.getWorkflowNote(),
                itemEntity.getRecordInfo(),
                itemEntity.getPartitioningOutcome() == null ? null : itemEntity.getPartitioningOutcome().getTrackingId());
    }

    private static int calculateItemNumber(ItemEntity.Key key) {
        // + 1 because we want to show the first item with number 1 although we are using a 0 index
        return key.getChunkId() * CHUNK_SIZE + key.getId() + 1;
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
