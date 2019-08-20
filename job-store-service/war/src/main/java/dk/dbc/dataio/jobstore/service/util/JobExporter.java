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
import dk.dbc.dataio.commons.types.Diagnostic;
import dk.dbc.dataio.jobstore.service.entity.ItemEntity;
import dk.dbc.dataio.jobstore.service.entity.ItemListQuery;
import dk.dbc.dataio.jobstore.service.entity.ListQuery;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import dk.dbc.dataio.jobstore.types.RecordInfo;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * This class is responsible for job exports.
 *
 * This class is not thread safe.
 */
public class JobExporter {
    private static final Logger LOGGER = LoggerFactory.getLogger(JobExporter.class);

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
     * @return export as {@link FailedItemsContent} in which chunk items are ordered by ascending
     * chunk ids and item ids respectively
     * @throws JobStoreException on general failure to write content
     */
    public FailedItemsContent exportFailedItemsContent(int jobId, List<State.Phase> fromPhases, ChunkItem.Type asType,
                                                       Charset encodedAs) throws JobStoreException {
        if (asType == null) {
            asType = getExportType(jobId, fromPhases);
        }

        LOGGER.info("Exporting failed items for job {} from phases {} as {} encoded as {}",
                jobId, fromPhases, asType, encodedAs);

        final JobExportQuery exportQuery = new JobExportQuery(entityManager, jobId)
                .where(new ListFilter<>(phaseToPhaseFailedCriteriaField(fromPhases.get(0))));
        fromPhases.stream().skip(1).forEach(
                phase -> exportQuery.or(new ListFilter<>(phaseToPhaseFailedCriteriaField(phase))));

        boolean hasFatalItems = false;
        final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (JobExport<ItemEntity> export = exportQuery.execute(item -> item)) {
            for (ItemEntity item : export) {
                try {
                    final ExportableFailedItem exportableFailedItem = ExportableFailedItem.of(item);
                    buffer.write(exportFailedItem(exportableFailedItem, asType, encodedAs));
                    if (exportableFailedItem.hasFatalDiagnostic()) {
                        hasFatalItems = true;
                    }
                } catch (IOException e) {
                    final String message = String.format(
                            "Exception caught during export of failed items for job %d chunk %d item %d",
                            item.getKey().getJobId(), item.getKey().getChunkId(), item.getKey().getId());
                    throw new JobStoreException(message, e);
                }
            }
        }
        return new FailedItemsContent(buffer, hasFatalItems);
    }

    /**
     * Exports bibliographic record IDs from all items in a job
     * @param jobId id of job to be exported
     * @return export of bibliographic record IDs (may contain null values)
     */
    public JobExport<RecordInfo> exportItemsRecordInfo(int jobId) {
        return extractRecordInfo(new JobExportQuery(entityManager, jobId));
    }

    /**
     * Exports bibliographic record IDs from failed items in a job
     * @param jobId id of job to be exported
     * @return export of bibliographic record IDs (may contain null values)
     */
    public JobExport<RecordInfo> exportFailedItemsRecordInfo(int jobId) {
        return extractRecordInfo(new JobExportQuery(entityManager, jobId)
                .where(new ListFilter<>(phaseToPhaseFailedCriteriaField(State.Phase.PARTITIONING)))
                .or(new ListFilter<>(phaseToPhaseFailedCriteriaField(State.Phase.PROCESSING)))
                .or(new ListFilter<>(phaseToPhaseFailedCriteriaField(State.Phase.DELIVERING))));
    }

    private JobExport<RecordInfo> extractRecordInfo(JobExportQuery exportQuery) {
        return exportQuery.execute(item ->  {
            try {
                return item.getRecordInfo();
            } catch (RuntimeException e) {
                LOGGER.error(String.format("extractRecordInfo(): extraction unsuccessful for item %s",
                        item.getKey()), e);
            }
            return null;
        });
    }

    /**
     * Exports keys from all items in a job
     * @param jobId id of job to be exported
     * @return export of {@link ItemEntity.Key}
     */
    public JobExport<ItemEntity.Key> exportItemsKeys(int jobId) {
        return new JobExportQuery(entityManager, jobId).execute(ItemEntity::getKey);
    }

    /**
     * Exports keys from failed items in a job
     * @param jobId id of job to be exported
     * @return export of {@link ItemEntity.Key}
     */
    public JobExport<ItemEntity.Key> exportFailedItemsKeys(int jobId) {
        return new JobExportQuery(entityManager, jobId)
                .where(new ListFilter<>(phaseToPhaseFailedCriteriaField(State.Phase.PARTITIONING)))
                .or(new ListFilter<>(phaseToPhaseFailedCriteriaField(State.Phase.PROCESSING)))
                .or(new ListFilter<>(phaseToPhaseFailedCriteriaField(State.Phase.DELIVERING)))
                .execute(ItemEntity::getKey);
    }

    /**
     * Exports datafile position from all items in a job
     * @param jobId id of job to be exported
     * @return export of {@link Integer} positions (may contain null values)
     */
    public JobExport<Integer> exportItemsPositionsInDatafile(int jobId) {
        return new JobExportQuery(entityManager, jobId).execute(ItemEntity::getPositionInDatafile);
    }

    /**
     * Exports datafile position from failed items in a job
     * @param jobId id of job to be exported
     * @return export of {@link Integer} positions (may contain null values)
     */
    public JobExport<Integer> exportFailedItemsPositionsInDatafile(int jobId) {
        return new JobExportQuery(entityManager, jobId)
                .where(new ListFilter<>(phaseToPhaseFailedCriteriaField(State.Phase.PARTITIONING)))
                .or(new ListFilter<>(phaseToPhaseFailedCriteriaField(State.Phase.PROCESSING)))
                .or(new ListFilter<>(phaseToPhaseFailedCriteriaField(State.Phase.DELIVERING)))
                .execute(ItemEntity::getPositionInDatafile);
    }

    byte[] exportFailedItem(ExportableFailedItem item, ChunkItem.Type asType, Charset encodedAs) {
        try {
            return chunkItemExporter.export(item.getChunkItem(), asType, encodedAs,
                    item.getDiagnostics());
        } catch (JobStoreException e) {
            LOGGER.error(String.format("Export unsuccessful for item %s failed in phase %s",
                    item.getKey(), item.getFailedPhase()), e);
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

    ChunkItem.Type getExportType(int jobId, List<State.Phase> fromPhases) {
        if (fromPhases.contains(State.Phase.PARTITIONING)) {
            // If something is wrong with the input records,
            // BYTES is the only sane choice.
            return ChunkItem.Type.BYTES;
        }
        // Test the type of the first partitioned item
        // (NOTE: This approach may miss the opportunity
        // to set the type to LINEFORMAT if the
        // first item failed during partitioning. Should
        // perhaps be replaced by a query for the first
        // successfully partitioned item!)
        final ItemEntity itemEntity = entityManager.find(ItemEntity.class,
                new ItemEntity.Key(jobId, 0, (short) 0));
        if (itemEntity != null) {
            final List<ChunkItem.Type> typeList = itemEntity.getPartitioningOutcome().getType();
            // Test last type entry
            if (ChunkItem.Type.MARCXCHANGE == typeList.get(typeList.size() - 1)) {
                return ChunkItem.Type.LINEFORMAT;
            }
        }
        // Everything can be exported as bytes.
        return ChunkItem.Type.BYTES;
    }

    @FunctionalInterface
    private interface ItemEntityConverter<V> {
        V from(ItemEntity itemEntity);
    }

    private static class JobExportQuery {
        private final EntityManager entityManager;
        private final ItemListCriteria itemListCriteria;

        JobExportQuery(EntityManager entityManager, int jobId) {
            this.entityManager = entityManager;
            itemListCriteria = new ItemListCriteria()
                .orderBy(new ListOrderBy<>(ItemListCriteria.Field.CHUNK_ID, ListOrderBy.Sort.ASC))
                .orderBy(new ListOrderBy<>(ItemListCriteria.Field.ITEM_ID, ListOrderBy.Sort.ASC))
                .where(new ListFilter<>(ItemListCriteria.Field.JOB_ID, ListFilter.Op.EQUAL, jobId));
        }

        public JobExportQuery where(ListFilter<ItemListCriteria.Field> filter) {
            itemListCriteria.where(filter);
            return this;
        }

        public JobExportQuery or(ListFilter<ItemListCriteria.Field> filter) {
            itemListCriteria.or(filter);
            return this;
        }

        public <V> JobExport<V> execute(ItemEntityConverter<V> converter) {
            return new JobExport<>(this, converter);
        }
    }

    public static class JobExport<V> implements Iterable<V>, AutoCloseable {
        private final ListQuery<ItemListCriteria, ItemListCriteria.Field, ItemEntity>.ResultSet resultSet;
        private final Iterator<ItemEntity> resultSetIterator;
        private final ItemEntityConverter<V> converter;

        JobExport(JobExportQuery query, ItemEntityConverter<V> converter) {
            this.resultSet = new ItemListQuery(query.entityManager).stream(query.itemListCriteria);
            this.resultSetIterator = resultSet.iterator();
            this.converter = converter;
        }

        @Override
        public Iterator<V> iterator() {
            return new Iterator<V>() {
                @Override
                public boolean hasNext() {
                    return resultSetIterator.hasNext();
                }

                @Override
                public V next() {
                    return converter.from(resultSetIterator.next());
                }
            };
        }

        @Override
        public void close() {
            if (resultSet != null) {
                resultSet.close();
            }
        }
    }

    public static class FailedItemsContent {
        private final ByteArrayOutputStream content;
        private final boolean hasFatalItems;

        public FailedItemsContent(ByteArrayOutputStream content, boolean hasFatalItems) {
            this.content = content;
            this.hasFatalItems = hasFatalItems;
        }

        public ByteArrayOutputStream getContent() {
            return content;
        }

        public boolean hasFatalItems() {
            return hasFatalItems;
        }
    }

    static class ExportableFailedItem {
        private final ItemEntity.Key key;
        private final State.Phase failedPhase;
        private final ChunkItem chunkItem;
        private final List<Diagnostic> diagnostics;

        static ExportableFailedItem of(ItemEntity itemEntity) {
            final State.Phase failedPhase = itemEntity.getFailedPhase().get();
            final ChunkItem chunkItem = getExportableChunkItemForFailedPhase(itemEntity, failedPhase);
            final List<Diagnostic> diagnostics = getDiagnosticsForFailedPhase(itemEntity, failedPhase);
            return new ExportableFailedItem(itemEntity.getKey(), failedPhase, chunkItem, diagnostics);
        }

        private static ChunkItem getExportableChunkItemForFailedPhase(ItemEntity itemEntity, State.Phase failedPhase) {
            switch (failedPhase) {
                case PARTITIONING: return itemEntity.getPartitioningOutcome();
                case PROCESSING:   return itemEntity.getPartitioningOutcome();
                case DELIVERING:   return itemEntity.getProcessingOutcome();
                default: throw new IllegalStateException("Unknown phase " + failedPhase);
            }
        }

        private static List<Diagnostic> getDiagnosticsForFailedPhase(ItemEntity itemEntity, State.Phase failedPhase) {
            final ChunkItem chunkItem = itemEntity.getChunkItemForPhase(failedPhase);
            if (chunkItem == null) {
                return Collections.emptyList();
            }
            final ArrayList<Diagnostic> diagnostics = chunkItem.getDiagnostics();
            if (diagnostics == null) {
                return Collections.emptyList();
            }
            return diagnostics;
        }

        private ExportableFailedItem(ItemEntity.Key key, State.Phase failedPhase,
                                     ChunkItem chunkItem, List<Diagnostic> diagnostics) {
            this.key = key;
            this.failedPhase = failedPhase;
            this.chunkItem = chunkItem;
            this.diagnostics = diagnostics;
        }

        public ItemEntity.Key getKey() {
            return key;
        }

        State.Phase getFailedPhase() {
            return failedPhase;
        }

        ChunkItem getChunkItem() {
            return chunkItem;
        }

        List<Diagnostic> getDiagnostics() {
            return diagnostics;
        }

        boolean hasFatalDiagnostic() {
            return diagnostics.stream()
                    .anyMatch(diag -> diag.getLevel() == Diagnostic.Level.FATAL);
        }
    }
}
