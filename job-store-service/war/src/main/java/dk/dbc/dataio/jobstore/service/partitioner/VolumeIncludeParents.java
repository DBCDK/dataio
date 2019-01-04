/*
 * DataIO - Data IO
 *
 * Copyright (C) 2018 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
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

import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.jobstore.service.entity.ReorderedItemEntity;
import dk.dbc.dataio.jobstore.types.MarcRecordInfo;
import dk.dbc.marc.binding.MarcRecord;
import dk.dbc.marc.reader.MarcReaderException;
import dk.dbc.marc.reader.MarcXchangeV1Reader;
import dk.dbc.marc.writer.MarcXchangeV1Writer;

import javax.persistence.EntityManager;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The responsibility of this class is to ensure that volume records are
 * returned with their parents and possibly grandparents.
 *
 * This class assumes it is called in a transactional context with regards to
 * the given entity manager.
 *
 * This class is not thread safe.
 */
public class VolumeIncludeParents extends JobItemReorderer {
    public VolumeIncludeParents(int jobId, EntityManager entityManager) {
        super(jobId, entityManager);
    }

    @Override
    DataPartitionerResult getReorderedItem() {
        DataPartitionerResult partitionerResult = DataPartitionerResult.EMPTY;

        final ReorderedItemEntity reorderedItemEntity = getNextItemFromDatabase().orElse(null);
        if (reorderedItemEntity != null) {
            if (reorderedItemEntity.getChunkItem().getStatus() == ChunkItem.Status.SUCCESS
                    && reorderedItemEntity.getRecordInfo().isVolume()) {
                /* If the next item returned from the scratchpad is a volume
                   also include any parent records found in the scratchpad
                   in the result. */
                partitionerResult = resolveParents(reorderedItemEntity);
            } else {
                partitionerResult = new DataPartitionerResult(reorderedItemEntity.getChunkItem(),
                        reorderedItemEntity.getRecordInfo(), reorderedItemEntity.getPositionInDatafile());
            }
            entityManager.remove(reorderedItemEntity);
            numberOfItems--;
        }

        return partitionerResult;
    }

    @Override
    SortOrder getReorderedItemSortOrder(MarcRecordInfo recordInfo) {
        /* Ensure that all volumes are processed before
           section and head records are removed from the
           scratchpad. */
        switch (recordInfo.getType()) {
            case VOLUME:  return SortOrder.VOLUME_DELETE;
            case SECTION: return SortOrder.SECTION_DELETE;
            default:      return SortOrder.HEAD_DELETE;
        }
    }

    @Override
    public Boolean addCollectionWrapper() {
        return Boolean.TRUE;
    }

    /* Resolves the given item into a result also including any
       parents found in the scratchpad. */
    private DataPartitionerResult resolveParents(ReorderedItemEntity volume) {
        final List<ChunkItem> collection = new ArrayList<>(3);
        collection.add(volume.getChunkItem());
        final ReorderedItemEntity parent = getParent(volume).orElse(null);
        if (parent != null) {
            collection.add(parent.getChunkItem());
            if (parent.getRecordInfo().isSection()) {
                final ReorderedItemEntity grandparent = getParent(parent).orElse(null);
                if (grandparent != null) {
                    collection.add(grandparent.getChunkItem());
                }
            }
        }
        return new DataPartitionerResult(reduce(collection),
                volume.getRecordInfo(), volume.getPositionInDatafile());
    }

    private Optional<ReorderedItemEntity> getParent(ReorderedItemEntity child) {
        if (!child.getRecordInfo().hasParentRelation()) {
            return Optional.empty();
        }
        return entityManager.createNamedQuery(ReorderedItemEntity.QUERY_GET_PARENT, ReorderedItemEntity.class)
                .setParameter(1, jobId)
                .setParameter(2, String.format("{\"id\": \"%s\"}",
                        child.getRecordInfo().getParentRelation()))
                .getResultList()
                .stream()
                .findFirst();
    }

    private ChunkItem reduce(List<ChunkItem> collection) {
        try {
            final List<MarcRecord> marcRecords = new ArrayList<>(collection.size());
            for (ChunkItem chunkItem : collection) {
                if (chunkItem.getStatus() != ChunkItem.Status.SUCCESS) {
                    continue;
                }
                final MarcXchangeV1Reader reader = new MarcXchangeV1Reader(
                        new ByteArrayInputStream(chunkItem.getData()), chunkItem.getEncoding());
                marcRecords.add(reader.read());
            }
            final MarcXchangeV1Writer writer = new MarcXchangeV1Writer();
            final ChunkItem volume = collection.get(0);
            return ChunkItem.successfulChunkItem()
                        .withType(volume.getType().toArray(new ChunkItem.Type[0]))
                        .withData(writer.writeCollection(marcRecords, volume.getEncoding()));
        } catch (MarcReaderException e) {
            throw new RuntimeException("An error occurred while reducing MARC records", e);
        }
    }
}
