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

package dk.dbc.dataio.jobstore.service.partitioner;

import dk.dbc.dataio.jobstore.service.entity.ReorderedItemEntity;
import dk.dbc.dataio.jobstore.types.MarcRecordInfo;

import javax.persistence.EntityManager;
import java.util.Optional;

/**
 * The responsibility of this class is to ensure the correct ordering of records taking part
 * in multi-level record structures.
 *
 * This class assumes it is called in a transactional context with regards to the given entity manager.
 *
 * This class is not thread safe.
 */
public class JobItemReorderer {
    public enum SortOrder {
        HEAD(1),
        SECTION(2),
        VOLUME(3),
        VOLUME_DELETE(4),
        SECTION_DELETE(5),
        HEAD_DELETE(6);

        private final int sortOrder;

        SortOrder(int sortOrder) {
            this.sortOrder = sortOrder;
        }

        public int getSortOrder() {
            return sortOrder;
        }
    }

    private final int jobId;
    private final EntityManager entityManager;

    private int itemsToBeReordered = 0;

    public JobItemReorderer(int jobId, EntityManager entityManager) {
        this.jobId = jobId;
        this.entityManager = entityManager;
    }

    /**
     * @return true if this Reorderer instance still contains items to be re-ordered, otherwise false
     */
    public boolean hasNext() {
        return itemsToBeReordered != 0;
    }

    /**
     * Returns the next DataPartitionerResult to be output
     *
     * If given result is empty (indicating that the caller has run out of results to be examined)
     *   then
     *     if this Reorderer instance still contains results to be re-ordered
     *       then
     *         the next result is retrieved from internal list and returned
     *       else
     *         an empty Optional indicating no more records is returned.
     *
     * If given result is not empty
     *   then
     *     if the result is part of multi-level record structure
     *       then
     *         the result is stored in internal list and an empty placeholder result is returned
     *       else
     *         the result is returned as-is (passthrough)
     *
     * @param partitionerResult current DataPartitionerResult to be examined
     *
     * @return the next DataPartitionerResult
     */
    public Optional<DataPartitionerResult> next(DataPartitionerResult partitionerResult) {
        Optional<DataPartitionerResult> next;
        if (partitionerResult.isEmpty()) {
            if (hasNext()) {
                next = Optional.of(getReorderedItem());
            } else {
                next = Optional.empty();
            }
        } else {
            if (mustBeReordered(partitionerResult)) {
                next = Optional.of(setReorderedItem(partitionerResult));
            } else {
                next = Optional.of(partitionerResult);
            }
        }
        return next;
    }

    private boolean mustBeReordered(DataPartitionerResult partitionerResult) {
        final MarcRecordInfo recordInfo = (MarcRecordInfo) partitionerResult.getRecordInfo();
        return recordInfo.isHead() || recordInfo.isSection() || recordInfo.isVolume();
    }

    /* Persists given DataPartitionerResult in internal list with the correct sort-order
       to be used for later retrieval. Returns empty DataPartitionerResult placeholder. */
    private DataPartitionerResult setReorderedItem(DataPartitionerResult partitionerResult) {
        final MarcRecordInfo recordInfo = (MarcRecordInfo) partitionerResult.getRecordInfo();
        final ReorderedItemEntity reorderedItemEntity = new ReorderedItemEntity();
        reorderedItemEntity.setKey(new ReorderedItemEntity.Key(jobId, itemsToBeReordered++));
        reorderedItemEntity.setChunkItem(partitionerResult.getChunkItem());
        reorderedItemEntity.setRecordInfo(recordInfo);
        reorderedItemEntity.setSortOrder(getReorderedItemSortOrder(recordInfo));
        entityManager.persist(reorderedItemEntity);
        return DataPartitionerResult.EMPTY;
    }

    /* Retrieves next DataPartitionerResult in line from internal list */
    private DataPartitionerResult getReorderedItem() {
        final ReorderedItemEntity reorderedItemEntity = entityManager
                .createNamedQuery(ReorderedItemEntity.NAMED_QUERY_GET_REORDERED_ITEM, ReorderedItemEntity.class)
                .setMaxResults(1)
                .getSingleResult();

        final DataPartitionerResult partitionerResult;
        if (reorderedItemEntity != null) {
            partitionerResult = new DataPartitionerResult(reorderedItemEntity.getChunkItem(), reorderedItemEntity.getRecordInfo());
            entityManager.remove(reorderedItemEntity);
        } else {
            partitionerResult = DataPartitionerResult.EMPTY;
        }
        itemsToBeReordered--;
        return partitionerResult;
    }

    private int getReorderedItemSortOrder(MarcRecordInfo recordInfo) {
        switch (recordInfo.getType()) {
            case VOLUME:
                if (recordInfo.isDelete()) {
                    return SortOrder.VOLUME_DELETE.getSortOrder();
                }
                return SortOrder.VOLUME.getSortOrder();
            case SECTION:
                if (recordInfo.isDelete()) {
                    return SortOrder.SECTION_DELETE.getSortOrder();
                }
                return SortOrder.SECTION.getSortOrder();
            default:
                if (recordInfo.isDelete()) {
                    return SortOrder.HEAD_DELETE.getSortOrder();
                }
                return SortOrder.HEAD.getSortOrder();
        }
    }
}
