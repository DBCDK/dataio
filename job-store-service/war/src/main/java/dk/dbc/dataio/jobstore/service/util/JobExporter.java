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

import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.jobstore.service.entity.ItemEntity;
import dk.dbc.dataio.jobstore.service.entity.ItemListQuery;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import dk.dbc.dataio.jobstore.types.State;
import dk.dbc.dataio.jobstore.types.criteria.ItemListCriteria;
import dk.dbc.dataio.jobstore.types.criteria.ListFilter;
import dk.dbc.dataio.jobstore.types.criteria.ListOrderBy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;

/**
 * This class is responsible for job exports.
 *
 * This class is not thread safe.
 */
public class JobExporter {
    private static final Logger LOGGER = LoggerFactory.getLogger(JobExporter.class);
    int MAX_NUMBER_OF_ITEMS_PER_QUERY = 1000;

    private final EntityManager entityManager;

    public JobExporter(EntityManager entityManager) throws NullPointerException {
        this.entityManager = entityManager;
    }

    ChunkItemExporter chunkItemExporter = new ChunkItemExporter();

    /**
     * Exports from a job all chunk items which have failed in specific phases
     * @param jobId id of job from which failed chunk items are to be exported
     * @param fromPhases list of phases from which failed chunk items are to be exported
     * @param asType type of export
     * @param encodedAs export encoding
     * @return export as ByteArrayOutputStream in which chunk items are ordered by ascending
     * chunk ids and item ids respectively
     * @throws JobStoreException on general failure to write output stream
     */
    public ByteArrayOutputStream exportFailedItems(int jobId, List<State.Phase> fromPhases, ChunkItem.Type asType, Charset encodedAs) throws JobStoreException {
        LOGGER.info("Exporting failed items for job {} from phases {} as {} encoded as {}", jobId, fromPhases, asType, encodedAs);
        final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        final ItemListCriteria itemListCriteria = new ItemListCriteria()
                .limit(MAX_NUMBER_OF_ITEMS_PER_QUERY)
                .orderBy(new ListOrderBy<>(ItemListCriteria.Field.CHUNK_ID, ListOrderBy.Sort.ASC))
                .orderBy(new ListOrderBy<>(ItemListCriteria.Field.ITEM_ID, ListOrderBy.Sort.ASC))
                .where(new ListFilter<>(ItemListCriteria.Field.JOB_ID, ListFilter.Op.EQUAL, jobId))
                .where(new ListFilter<>(phaseToPhaseFailedCriteriaField(fromPhases.get(0))));
        fromPhases.stream().skip(1).forEach(
                phase -> itemListCriteria.or(new ListFilter<>(phaseToPhaseFailedCriteriaField(phase))));

        int offset = 0;
        int numberOfItemsFound;
        do {
            itemListCriteria.offset(offset);

            final ItemListQuery itemListQuery = new ItemListQuery(entityManager);
            final List<ItemEntity> items = itemListQuery.execute(itemListCriteria);

            numberOfItemsFound = items.size();
            if (numberOfItemsFound > 0) {
                offset += numberOfItemsFound;
                for (ItemEntity item : items) {
                    try {
                        buffer.write(exportFailedItem(item, asType, encodedAs));
                    } catch (IOException e) {
                        final String message = String.format("Exception caught during export of failed items for job %d chunk %d item %d",
                                item.getKey().getJobId(), item.getKey().getChunkId(), item.getKey().getId());
                        throw new JobStoreException(message, e);
                    }
                }
            }
        } while (numberOfItemsFound == MAX_NUMBER_OF_ITEMS_PER_QUERY);

        return buffer;
    }

    byte[] exportFailedItem(ItemEntity item, ChunkItem.Type asType, Charset encodedAs) {
        final State.Phase failedPhase = item.getFailedPhase().get();
        final ChunkItem chunkItem = getExportableChunkItemForFailedPhase(item, item.getFailedPhase().get());
        try {
            return chunkItemExporter.export(chunkItem, asType, encodedAs);
        } catch (JobStoreException e) {
            LOGGER.error(String.format("Export unsuccessful for item %s failed in phase %s",
                    item.getKey(), failedPhase), e);
            return new byte[0];
        }
    }

    ItemListCriteria.Field phaseToPhaseFailedCriteriaField(State.Phase phase) {
        switch (phase) {
            case PARTITIONING: return ItemListCriteria.Field.PARTITIONING_FAILED;
            case PROCESSING:   return ItemListCriteria.Field.PROCESSING_FAILED;
            case DELIVERING:   return ItemListCriteria.Field.DELIVERY_FAILED;
            default: throw new IllegalStateException("Unknown phase " + phase);
        }
    }

    ChunkItem getExportableChunkItemForFailedPhase(ItemEntity entity, State.Phase failedPhase) {
        switch (failedPhase) {
            case PARTITIONING: return entity.getPartitioningOutcome();
            case PROCESSING:   return entity.getPartitioningOutcome();
            case DELIVERING:   return entity.getProcessingOutcome();
            default: throw new IllegalStateException("Unknown phase " + failedPhase);
        }
    }
}
