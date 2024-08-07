package dk.dbc.dataio.jobstore.service.util;

import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.commons.utils.test.model.ChunkItemBuilder;
import dk.dbc.dataio.commons.utils.test.model.DiagnosticBuilder;
import dk.dbc.dataio.jobstore.service.entity.ItemEntity;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import dk.dbc.dataio.jobstore.types.State;
import dk.dbc.dataio.jobstore.types.StateChange;
import dk.dbc.dataio.jobstore.types.criteria.ItemListCriteria;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
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

    @BeforeEach
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
        when(chunkItemExporter.export(any(ChunkItem.class), eq(ChunkItem.Type.STRING), eq(StandardCharsets.UTF_8), anyList()))
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
        when(chunkItemExporter.export(any(ChunkItem.class), eq(ChunkItem.Type.STRING), eq(StandardCharsets.UTF_8), anyList()))
                .thenThrow(new JobStoreException("Died"));

        final ItemEntity itemEntity = createItemEntity();
        setItemEntityDataForPhase(itemEntity, State.Phase.PARTITIONING, "partitioning outcome");
        failItemEntityForPhase(itemEntity, State.Phase.PROCESSING);

        final byte[] export = jobExporter.exportFailedItem(JobExporter.ExportableFailedItem.of(itemEntity), ChunkItem.Type.STRING, StandardCharsets.UTF_8);
        assertThat(export, is(new byte[0]));
    }

    @Test
    public void getExportType_exportIncludesPartitioningPhase() {
        assertThat("single phase",
                jobExporter.getExportType(42, Collections.singletonList(State.Phase.PARTITIONING)),
                is(ChunkItem.Type.BYTES));
        assertThat("multiple phases",
                jobExporter.getExportType(42, Arrays.asList(
                        State.Phase.PROCESSING, State.Phase.PARTITIONING, State.Phase.DELIVERING)),
                is(ChunkItem.Type.BYTES));
    }

    @Test
    public void getExportType_itemEntityNotFound() {
        assertThat(jobExporter.getExportType(42, Collections.singletonList(State.Phase.PROCESSING)),
                is(ChunkItem.Type.BYTES));
    }

    @Test
    public void getExportType_itemEntityFoundWithoutMarcXchangeType() {
        final ItemEntity itemEntity = new ItemEntity();
        itemEntity.setPartitioningOutcome(ChunkItem.successfulChunkItem()
                .withType(ChunkItem.Type.STRING));
        when(entityManager.find(ItemEntity.class, new ItemEntity.Key(42, 0, (short) 0)))
                .thenReturn(itemEntity);

        assertThat(jobExporter.getExportType(42, Collections.singletonList(State.Phase.PROCESSING)),
                is(ChunkItem.Type.BYTES));
    }

    @Test
    public void getExportType_itemEntityFoundWithMarcXchangeType() {
        final ItemEntity itemEntity = new ItemEntity();
        itemEntity.setPartitioningOutcome(ChunkItem.successfulChunkItem()
                .withType(ChunkItem.Type.MARCXCHANGE));
        when(entityManager.find(ItemEntity.class, new ItemEntity.Key(42, 0, (short) 0)))
                .thenReturn(itemEntity);

        assertThat(jobExporter.getExportType(42, Collections.singletonList(State.Phase.PROCESSING)),
                is(ChunkItem.Type.LINEFORMAT));
    }

    private ItemEntity createItemEntity() {
        final ItemEntity itemEntity = new ItemEntity();
        itemEntity.setKey(itemEntityKey);
        itemEntity.setState(new State());
        return itemEntity;
    }

    private void setItemEntityDataForPhase(ItemEntity itemEntity, State.Phase phase, String data) {
        switch (phase) {
            case PARTITIONING:
                itemEntity.setPartitioningOutcome(new ChunkItemBuilder().setData(data).build());
                break;
            case PROCESSING:
                itemEntity.setProcessingOutcome(new ChunkItemBuilder().setData(data).build());
                break;
            case DELIVERING:
                itemEntity.setDeliveringOutcome(new ChunkItemBuilder().setData(data).build());
                break;
            default:
                throw new IllegalStateException("Unknown phase " + phase);
        }
    }

    private void failItemEntityForPhase(ItemEntity itemEntity, State.Phase phase) {
        final StateChange stateChange = new StateChange()
                .setPhase(phase)
                .incFailed(1);
        itemEntity.getState().updateState(stateChange);
    }
}
