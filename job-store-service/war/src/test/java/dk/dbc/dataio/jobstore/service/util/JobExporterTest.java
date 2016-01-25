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
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JobExporterTest {
    final Query query = mock(Query.class);
    final EntityManager entityManager = mock(EntityManager.class);
    final ChunkItemExporter chunkItemExporter = mock(ChunkItemExporter.class);
    final ItemEntity.Key itemEntityKey = new ItemEntity.Key(1, 2, (short) 3);
    final JobExporter jobExporter = new JobExporter(entityManager);

    {
        jobExporter.MAX_NUMBER_OF_ITEMS_PER_QUERY = 2;
        jobExporter.chunkItemExporter = chunkItemExporter;
    }

    @Before
    public void setupMocks() {
         when(entityManager.createNativeQuery(any(String.class), eq(ItemEntity.class)))
                 .thenReturn(query);
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

        final ChunkItem chunkItem = jobExporter.getExportableChunkItemForFailedPhase(itemEntity, State.Phase.PARTITIONING);
        assertThat(StringUtil.asString(chunkItem.getData()), is(expectedChunkItemData));
    }

    @Test
    public void getExportableChunkItemForFailedPhase_processingPhaseFailed_returnsChunkItemFromPartitioningPhase() {
        final String expectedChunkItemData = "partitioning outcome";
        final ItemEntity itemEntity = createItemEntity();
        setItemEntityDataForPhase(itemEntity, State.Phase.PARTITIONING, expectedChunkItemData);

        final ChunkItem chunkItem = jobExporter.getExportableChunkItemForFailedPhase(itemEntity, State.Phase.PROCESSING);
        assertThat(StringUtil.asString(chunkItem.getData()), is(expectedChunkItemData));
    }

    @Test
    public void getExportableChunkItemForFailedPhase_deliveringPhaseFailed_returnsChunkItemFromProcessingPhase() {
        final String expectedChunkItemData = "processing outcome";
        final ItemEntity itemEntity = createItemEntity();
        setItemEntityDataForPhase(itemEntity, State.Phase.PROCESSING, expectedChunkItemData);

        final ChunkItem chunkItem = jobExporter.getExportableChunkItemForFailedPhase(itemEntity, State.Phase.DELIVERING);
        assertThat(StringUtil.asString(chunkItem.getData()), is(expectedChunkItemData));
    }

    @Test
    public void getDiagnosticsForFailedPhase_entityHasNoFailedPhase_returnsEmptyList() {
        final ItemEntity itemEntity = createItemEntity();
        final List<Diagnostic> diagnostics = jobExporter.getDiagnosticsForFailedPhase(itemEntity, State.Phase.PARTITIONING);
        assertThat(diagnostics, is(Collections.emptyList()));
    }

    @Test
    public void getDiagnosticsForFailedPhase_entityHasFailedPhase_returnsDiagnosticsForPhase() {
        final ItemEntity itemEntity = createItemEntity();
        setItemEntityDataForPhase(itemEntity, State.Phase.PARTITIONING, "data");
        final ChunkItem chunkItem = itemEntity.getChunkItemForPhase(State.Phase.PARTITIONING);
        chunkItem.appendDiagnostics(new DiagnosticBuilder().build());

        final List<Diagnostic> diagnostics = jobExporter.getDiagnosticsForFailedPhase(itemEntity, State.Phase.PARTITIONING);
        assertThat(diagnostics, is(chunkItem.getDiagnostics()));
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

        final byte[] export = jobExporter.exportFailedItem(itemEntity, ChunkItem.Type.STRING, StandardCharsets.UTF_8);
        assertThat(StringUtil.asString(export), is(expectedExport));
    }

    @Test
    public void exportFailedItem_chunkItemExporterThrows_returnsEmptyByteArray() throws JobStoreException {
        when(chunkItemExporter.export(any(ChunkItem.class), eq(ChunkItem.Type.STRING), eq(StandardCharsets.UTF_8), anyListOf(Diagnostic.class)))
                .thenThrow(new JobStoreException("Died"));

        final ItemEntity itemEntity = createItemEntity();
        setItemEntityDataForPhase(itemEntity, State.Phase.PARTITIONING, "partitioning outcome");
        failItemEntityForPhase(itemEntity, State.Phase.PROCESSING);

        final byte[] export = jobExporter.exportFailedItem(itemEntity, ChunkItem.Type.STRING, StandardCharsets.UTF_8);
        assertThat(export, is(new byte[0]));
    }

    @Test
    public void exportFailedItems() throws JobStoreException {
        // Tests the 'while (numberOfItemsFound == MAX_NUMBER_OF_ITEMS_PER_QUERY)' loop

        // Force the first ChunkItemExporter.export() to throw to verify
        // that loop iteration continues.
        when(chunkItemExporter.export(any(ChunkItem.class), eq(ChunkItem.Type.STRING), eq(StandardCharsets.UTF_8), anyListOf(Diagnostic.class)))
                .thenThrow(new JobStoreException("Died"))
                .thenAnswer(invocation -> {
                    final Object[] args = invocation.getArguments();
                    final ChunkItem chunkItem = (ChunkItem) args[0];
                    return chunkItem.getData();
                });

        final ItemEntity itemEntity = createItemEntity();
        setItemEntityDataForPhase(itemEntity, State.Phase.PARTITIONING, "{record}");
        failItemEntityForPhase(itemEntity, State.Phase.PROCESSING);

        // Since MAX_NUMBER_OF_ITEMS_PER_QUERY for test purposes is set to 2,
        // the mocked responses below will result in 2 loop iterations
        when(query.getResultList())
                .thenReturn(Arrays.asList(itemEntity, itemEntity))
                .thenReturn(Collections.singletonList(itemEntity));

        final ByteArrayOutputStream export = jobExporter.exportFailedItems(42, Arrays.asList(State.Phase.PROCESSING, State.Phase.DELIVERING),
                ChunkItem.Type.STRING, StandardCharsets.UTF_8);
        assertThat(StringUtil.asString(export.toByteArray()), is("{record}{record}"));
    }

    @Test
    public void exportFailedItems_noItemsFound_returnsEmptyByteArrayOutputStream() throws JobStoreException {
        when(query.getResultList()).thenReturn(Collections.emptyList());

        final ByteArrayOutputStream export = jobExporter.exportFailedItems(42, Arrays.asList(State.Phase.PROCESSING, State.Phase.DELIVERING),
                ChunkItem.Type.STRING, StandardCharsets.UTF_8);
        assertThat(export, is(notNullValue()));
        assertThat(export.toByteArray(), is(new byte[0]));
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