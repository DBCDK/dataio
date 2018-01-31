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
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.commons.utils.test.model.ChunkItemBuilder;
import dk.dbc.dataio.commons.utils.test.model.DiagnosticBuilder;
import dk.dbc.dataio.jobstore.service.entity.ItemEntity;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import dk.dbc.dataio.jobstore.types.State;
import dk.dbc.dataio.jobstore.types.StateChange;
import dk.dbc.dataio.jobstore.types.criteria.ItemListCriteria;
import org.junit.Before;
import org.junit.Test;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JobExporterTest {
    private final Query query = mock(Query.class);
    private final EntityManager entityManager = mock(EntityManager.class);
    private final ChunkItemExporter chunkItemExporter = mock(ChunkItemExporter.class);
    private final ItemEntity.Key itemEntityKey = new ItemEntity.Key(1, 2, (short) 3);
    private final JobExporter jobExporter = new JobExporter(entityManager);
    {
        jobExporter.chunkItemExporter = chunkItemExporter;
    }

    @Before
    public void setupMocks() {
        when(entityManager.createNativeQuery(any(String.class), eq(ItemEntity.class))).thenReturn(query);
    }

    @Test
    public void phaseToPhaseFailedCriteriaField_phaseArgIsPartitioning_returnsField() {
        assertThat(jobExporter.phaseToPhaseFailedCriteriaField(State.Phase.PARTITIONING), is(ItemListCriteria.Field.PARTITIONING_FAILED));
    }

    @Test
    public void phaseToPhaseFailedCriteriaField_phaseArgIsProcessing_returnsField() {
        assertThat(jobExporter.phaseToPhaseFailedCriteriaField(State.Phase.PROCESSING), is(ItemListCriteria.Field.PROCESSING_FAILED));
    }

    @Test
    public void phaseToPhaseFailedCriteriaField_phaseArgIsDelivering_returnsField() {
        assertThat(jobExporter.phaseToPhaseFailedCriteriaField(State.Phase.DELIVERING), is(ItemListCriteria.Field.DELIVERY_FAILED));
    }

    @Test
    public void getExportableChunkItemForFailedPhase_partitioningPhaseFailed_returnsChunkItemFromPartitioningPhase() {
        final String expectedChunkItemData = "partitioning outcome";
        final ItemEntity itemEntity = createItemEntity();
        setItemEntityDataForPhase(itemEntity, State.Phase.PARTITIONING, expectedChunkItemData);
        failItemEntityForPhase(itemEntity, State.Phase.PARTITIONING);

        final JobExporter.ExportableFailedItem exportableFailedItem =
                JobExporter.ExportableFailedItem.of(itemEntity);
        assertThat(StringUtil.asString(exportableFailedItem.getChunkItem().getData()), is(expectedChunkItemData));
    }

    @Test
    public void getExportableChunkItemForFailedPhase_processingPhaseFailed_returnsChunkItemFromPartitioningPhase() {
        final String expectedChunkItemData = "partitioning outcome";
        final ItemEntity itemEntity = createItemEntity();
        setItemEntityDataForPhase(itemEntity, State.Phase.PARTITIONING, expectedChunkItemData);
        failItemEntityForPhase(itemEntity, State.Phase.PROCESSING);

        final JobExporter.ExportableFailedItem exportableFailedItem =
                JobExporter.ExportableFailedItem.of(itemEntity);
        assertThat(StringUtil.asString(exportableFailedItem.getChunkItem().getData()), is(expectedChunkItemData));
    }

    @Test
    public void getExportableChunkItemForFailedPhase_deliveringPhaseFailed_returnsChunkItemFromProcessingPhase() {
        final String expectedChunkItemData = "processing outcome";
        final ItemEntity itemEntity = createItemEntity();
        setItemEntityDataForPhase(itemEntity, State.Phase.PROCESSING, expectedChunkItemData);
        failItemEntityForPhase(itemEntity, State.Phase.DELIVERING);

        final JobExporter.ExportableFailedItem exportableFailedItem =
                JobExporter.ExportableFailedItem.of(itemEntity);
        assertThat(StringUtil.asString(exportableFailedItem.getChunkItem().getData()), is(expectedChunkItemData));
    }

    @Test
    public void getDiagnosticsForFailedPhase_entityHasFailedPhase_returnsDiagnosticsForPhase() {
        final ItemEntity itemEntity = createItemEntity();
        setItemEntityDataForPhase(itemEntity, State.Phase.PROCESSING, "data");
        failItemEntityForPhase(itemEntity, State.Phase.PROCESSING);
        final ChunkItem chunkItem = itemEntity.getChunkItemForPhase(State.Phase.PROCESSING);
        chunkItem.appendDiagnostics(new DiagnosticBuilder().build());

        final JobExporter.ExportableFailedItem exportableFailedItem =
                JobExporter.ExportableFailedItem.of(itemEntity);
        assertThat(exportableFailedItem.getDiagnostics(), is(chunkItem.getDiagnostics()));
    }

    @Test
    public void exportFailedItem() throws JobStoreException {
        when(chunkItemExporter.export(any(ChunkItem.class), eq(ChunkItem.Type.STRING), eq(StandardCharsets.UTF_8), anyListOf(Diagnostic.class)))
                .thenAnswer(invocation -> {
                    final Object[] args = invocation.getArguments();
                    final ChunkItem chunkItem = (ChunkItem) args[0];
                    return chunkItem.getData();
                });

        final String expectedExport = "partitioning outcome";
        final ItemEntity itemEntity = createItemEntity();
        setItemEntityDataForPhase(itemEntity, State.Phase.PARTITIONING, expectedExport);
        failItemEntityForPhase(itemEntity, State.Phase.PROCESSING);

        final byte[] export = jobExporter.exportFailedItem(JobExporter.ExportableFailedItem.of(itemEntity), ChunkItem.Type.STRING, StandardCharsets.UTF_8);
        assertThat(StringUtil.asString(export), is(expectedExport));
    }

    @Test
    public void exportFailedItem_chunkItemExporterThrows_returnsEmptyByteArray() throws JobStoreException {
        when(chunkItemExporter.export(any(ChunkItem.class), eq(ChunkItem.Type.STRING), eq(StandardCharsets.UTF_8), anyListOf(Diagnostic.class)))
                .thenThrow(new JobStoreException("Died"));

        final ItemEntity itemEntity = createItemEntity();
        setItemEntityDataForPhase(itemEntity, State.Phase.PARTITIONING, "partitioning outcome");
        failItemEntityForPhase(itemEntity, State.Phase.PROCESSING);

        final byte[] export = jobExporter.exportFailedItem(JobExporter.ExportableFailedItem.of(itemEntity), ChunkItem.Type.STRING, StandardCharsets.UTF_8);
        assertThat(export, is(new byte[0]));
    }

    private ItemEntity createItemEntity() {
        final ItemEntity itemEntity = new ItemEntity();
        itemEntity.setKey(itemEntityKey);
        itemEntity.setState(new State());
        return itemEntity;
    }

    private void setItemEntityDataForPhase(ItemEntity itemEntity, State.Phase phase, String data) {
        switch (phase) {
            case PARTITIONING: itemEntity.setPartitioningOutcome(new ChunkItemBuilder().setData(data).build()); break;
            case PROCESSING:   itemEntity.setProcessingOutcome(new ChunkItemBuilder().setData(data).build()); break;
            case DELIVERING:   itemEntity.setDeliveringOutcome(new ChunkItemBuilder().setData(data).build()); break;
            default: throw new IllegalStateException("Unknown phase " + phase);
        }
    }

    private void failItemEntityForPhase(ItemEntity itemEntity, State.Phase phase) {
        final StateChange stateChange = new StateChange()
                .setPhase(phase)
                .incFailed(1);
        itemEntity.getState().updateState(stateChange);
    }
}