package dk.dbc.dataio.jobstore.service.param;

import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.commons.partioner.DanMarc2LineFormatDataPartitioner;
import dk.dbc.dataio.commons.partioner.DanMarc2LineFormatReorderingDataPartitioner;
import dk.dbc.dataio.commons.partioner.DataPartitioner;
import dk.dbc.dataio.commons.partioner.IncludeFilterDataPartitioner;
import dk.dbc.dataio.commons.partioner.Iso2709DataPartitioner;
import dk.dbc.dataio.commons.partioner.Iso2709ReorderingDataPartitioner;
import dk.dbc.dataio.commons.partioner.VolumeAfterParents;
import dk.dbc.dataio.commons.partioner.entity.ReorderedItemEntity;
import dk.dbc.dataio.commons.types.Diagnostic;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.types.RecordSplitter;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.SinkContent;
import dk.dbc.dataio.commons.types.Submitter;
import dk.dbc.dataio.commons.utils.test.model.SinkBuilder;
import dk.dbc.dataio.commons.utils.test.model.SinkContentBuilder;
import dk.dbc.dataio.commons.utils.test.model.SubmitterBuilder;
import dk.dbc.dataio.commons.utils.test.model.SubmitterContentBuilder;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnector;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnectorException;
import dk.dbc.dataio.jobstore.service.entity.JobEntity;
import dk.dbc.dataio.jobstore.service.entity.SinkCacheEntity;
import dk.dbc.dataio.jobstore.types.FlowStoreReference;
import dk.dbc.dataio.jobstore.types.FlowStoreReferences;
import dk.dbc.dataio.jobstore.types.State;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.BitSet;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PartitioningParamTest extends ParamBaseTest {
    private final InputStream inputStream = mock(InputStream.class);
    private final FileStoreServiceConnector fileStoreServiceConnector = mock(FileStoreServiceConnector.class);
    private final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
    private final EntityManager entityManager = mock(EntityManager.class);
    private final TypedQuery typedQuery = mock(TypedQuery.class);
    private final RecordSplitter dataPartitionerType = RecordSplitter.XML;
    private final Submitter expected_submitter = new SubmitterBuilder().build();

    @BeforeEach
    public void setupJobSpecification() {
        jobSpecification.withCharset("latin1");
    }

    @BeforeEach
    public void setupMocks() throws FlowStoreServiceConnectorException, FileStoreServiceConnectorException, IOException {
        when(flowStoreServiceConnector.getSubmitter(anyLong())).thenReturn(expected_submitter);
        when(fileStoreServiceConnector.getFile(anyString())).thenReturn(inputStream);
        when(inputStream.read(any(), anyInt(), anyInt())).thenReturn(32);  // Return space
        when(entityManager.createNamedQuery(ReorderedItemEntity.GET_ITEMS_COUNT_BY_JOBID_QUERY_NAME, Long.class))
                .thenReturn(typedQuery);
        when(typedQuery.setParameter(any(String.class), anyInt())).thenReturn(typedQuery);
        when(typedQuery.getSingleResult()).thenReturn(0L);
    }

    @Test
    public void constructor_allArgsAreValid_returnsPartitioningParam() {
        PartitioningParam partitioningParam = newPartitioningParam(newJobEntity(jobSpecification));
        assertThat(partitioningParam, is(notNullValue()));
        assertThat(partitioningParam.getDiagnostics(), is(notNullValue()));
        assertThat(partitioningParam.getDiagnostics().size(), is(0));
        assertThat(partitioningParam.isPreviewOnly(), is(false));
    }

    @Test
    public void extractDataFileIdFromURN_invalidUrn_diagnosticLevelFatalAddedForUrnAndDataFileInputStream() throws FileStoreServiceConnectorException {
        when(fileStoreServiceConnector.getFile(null)).thenThrow(new NullPointerException());
        jobSpecification.withDataFile("invalid_urn");

        PartitioningParam partitioningParam = newPartitioningParam(newJobEntity(jobSpecification));

        List<Diagnostic> diagnostics = partitioningParam.getDiagnostics();
        assertThat(diagnostics.size(), is(1));
        assertThat(diagnostics.get(0).getLevel(), is(Diagnostic.Level.FATAL));
        assertThat(diagnostics.get(0).getMessage().contains(partitioningParam.getJobEntity().getSpecification().getDataFile()), is(true));
        assertThat(partitioningParam.getDataFileId(), is(nullValue()));
        assertThat(partitioningParam.getDataFileInputStream(), is(nullValue()));
    }

    @Test
    public void extractDataFileIdFromURN_validUrnAndFileFound_dataFileIdAndDataFileInputStreamAndDataPartitionerSet() {
        PartitioningParam partitioningParam = newPartitioningParam(newJobEntity(jobSpecification));

        List<Diagnostic> diagnostics = partitioningParam.getDiagnostics();
        assertThat(diagnostics.size(), is(0));
        assertThat(partitioningParam.getDataFileId(), is(notNullValue()));
        assertThat(partitioningParam.getDataFileInputStream(), is(notNullValue()));
        assertThat(partitioningParam.getDataPartitioner(), is(notNullValue()));
    }

    @Test
    public void newDataFileInputStream_fileNotFound_diagnosticLevelFatalAddedForInputStream() throws FileStoreServiceConnectorException {
        when(fileStoreServiceConnector.getFile(anyString())).thenThrow(new FileStoreServiceConnectorException(ERROR_MESSAGE));
        PartitioningParam partitioningParam = newPartitioningParam(newJobEntity(jobSpecification));

        List<Diagnostic> diagnostics = partitioningParam.getDiagnostics();
        assertThat(diagnostics.size(), is(1));
        assertThat(diagnostics.get(0).getLevel(), is(Diagnostic.Level.FATAL));
        assertThat(diagnostics.get(0).getMessage().contains(jobSpecification.getDataFile()), is(true));
    }

    @Test
    public void addJobParam_allReachableParametersSet_expectedValuesReturnedThroughGetters() throws FileStoreServiceConnectorException {
        when(fileStoreServiceConnector.getFile(anyString())).thenReturn(mock(InputStream.class));
        PartitioningParam partitioningParam = newPartitioningParam(newJobEntity(jobSpecification));

        assertThat(partitioningParam.getKeyGenerator(), is(notNullValue()));
        assertThat(partitioningParam.getDataPartitioner(), is(notNullValue()));
        assertThat(partitioningParam.getDiagnostics().size(), is(0));
        assertThat(partitioningParam.getDataFileId(), is(DATA_FILE_ID));
    }

    @Test
    public void typeOfDataPartitioner_lineFormat() {
        JobEntity jobEntity = newJobEntity(jobSpecification);
        PartitioningParam partitioningParam = newPartitioningParamForDanMarc2LineFormat(jobEntity);
        DataPartitioner dataPartitioner = partitioningParam.getDataPartitioner();
        assertThat("Class", dataPartitioner instanceof DanMarc2LineFormatDataPartitioner,
                is(true));
        assertThat("Reordering", dataPartitioner instanceof DanMarc2LineFormatReorderingDataPartitioner,
                is(false));
    }

    @Test
    public void typeOfDataPartitioner_lineFormatWhenAncestryTransfileIsSet() {
        JobSpecification.Ancestry ancestry = new JobSpecification.Ancestry().withTransfile("file");
        JobEntity jobEntity = newJobEntity(jobSpecification.withAncestry(ancestry));
        PartitioningParam partitioningParam = newPartitioningParamForDanMarc2LineFormat(jobEntity);
        DataPartitioner dataPartitioner = partitioningParam.getDataPartitioner();
        assertThat("Class", dataPartitioner instanceof DanMarc2LineFormatDataPartitioner,
                is(true));
        assertThat("Reordering", dataPartitioner instanceof DanMarc2LineFormatReorderingDataPartitioner,
                is(true));
        DanMarc2LineFormatReorderingDataPartitioner lineFormatDataPartitioner =
                (DanMarc2LineFormatReorderingDataPartitioner) dataPartitioner;
        assertThat("Reordering variant", lineFormatDataPartitioner.getReorderer() instanceof VolumeAfterParents,
                is(true));
    }

    @Test
    public void typeOfDataPartitioner_iso2709() {
        JobEntity jobEntity = newJobEntity(jobSpecification);
        PartitioningParam partitioningParam = newPartitioningParamForIso2709(jobEntity);
        DataPartitioner dataPartitioner = partitioningParam.getDataPartitioner();
        assertThat("Class", dataPartitioner instanceof Iso2709DataPartitioner,
                is(true));
        assertThat("Reordering", dataPartitioner instanceof Iso2709ReorderingDataPartitioner,
                is(false));
    }

    @Test
    public void typeOfDataPartitioner_iso2709WhenAncestryTransfileIsSet() {
        JobSpecification.Ancestry ancestry = new JobSpecification.Ancestry().withTransfile("file");
        JobEntity jobEntity = newJobEntity(jobSpecification.withAncestry(ancestry));
        PartitioningParam partitioningParam = newPartitioningParamForIso2709(jobEntity);
        DataPartitioner dataPartitioner = partitioningParam.getDataPartitioner();
        assertThat("Class", dataPartitioner instanceof Iso2709DataPartitioner,
                is(true));
        assertThat("Reordering", dataPartitioner instanceof Iso2709ReorderingDataPartitioner,
                is(true));
        Iso2709ReorderingDataPartitioner iso2709DataPartitioner =
                (Iso2709ReorderingDataPartitioner) dataPartitioner;
        assertThat("Reordering variant", iso2709DataPartitioner.getReorderer() instanceof VolumeAfterParents,
                is(true));
    }

    @Test
    public void typeOfDataPartitioner_whenIncludeFilterIsNotNull() {
        JobEntity jobEntity = newJobEntity(jobSpecification);
        PartitioningParam param = new PartitioningParam(jobEntity,
                fileStoreServiceConnector, flowStoreServiceConnector,
                entityManager, RecordSplitter.ISO2709,
                new BitSet());
        assertThat("IncludeFilterDataPartitioner",
                param.getDataPartitioner() instanceof IncludeFilterDataPartitioner, is(true));
        assertThat("wrapping",
                ((IncludeFilterDataPartitioner) param.getDataPartitioner()).getWrappedDataPartitioner()
                        instanceof Iso2709DataPartitioner, is(true));
    }

    @Test
    public void isPreviewOnly_whenSubmitterIsEnabled_isFalse() {
        JobEntity jobEntity = newJobEntity(jobSpecification.withType(JobSpecification.Type.PERSISTENT));
        PartitioningParam partitioningParam = newPartitioningParam(jobEntity);
        assertThat(partitioningParam.isPreviewOnly(), is(false));
    }

    @Test
    public void isPreviewOnly_whenSubmitterIsDisabled_isTrue() throws FlowStoreServiceConnectorException {
        when(flowStoreServiceConnector.getSubmitter(anyLong())).thenReturn(new SubmitterBuilder().setContent(new SubmitterContentBuilder().setEnabled(false).build()).build());
        JobEntity jobEntity = newJobEntity(jobSpecification.withType(JobSpecification.Type.PERSISTENT));
        PartitioningParam partitioningParam = newPartitioningParam(jobEntity);
        assertThat(partitioningParam.isPreviewOnly(), is(true));
    }

    private PartitioningParam newPartitioningParam(JobEntity jobEntity) {
        return new PartitioningParam(jobEntity, fileStoreServiceConnector, flowStoreServiceConnector, entityManager, dataPartitionerType);
    }

    private PartitioningParam newPartitioningParamForDanMarc2LineFormat(JobEntity jobEntity) {
        return new PartitioningParam(jobEntity, fileStoreServiceConnector, flowStoreServiceConnector, entityManager,
                RecordSplitter.DANMARC2_LINE_FORMAT);
    }

    private PartitioningParam newPartitioningParamForIso2709(JobEntity jobEntity) {
        return new PartitioningParam(jobEntity, fileStoreServiceConnector, flowStoreServiceConnector, entityManager,
                RecordSplitter.ISO2709);
    }

    private JobEntity newJobEntity(JobSpecification jobSpecification) {
        JobEntity jobEntity = new JobEntity();
        jobEntity.setSpecification(jobSpecification);
        jobEntity.setState(new State());
        FlowStoreReference flowStoreReference = new FlowStoreReference(
                expected_submitter.getId(), expected_submitter.getVersion(),
                expected_submitter.getContent().getName());
        FlowStoreReferences flowStoreReferences = new FlowStoreReferences();
        flowStoreReferences.setReference(FlowStoreReferences.Elements.SUBMITTER, flowStoreReference);
        jobEntity.setFlowStoreReferences(flowStoreReferences);
        jobEntity.setCachedSink(newSinkCacheEntity(SinkContent.SinkType.DUMMY));
        return jobEntity;
    }

    private static SinkCacheEntity newSinkCacheEntity(SinkContent.SinkType sinkType) {
        Sink sink = new SinkBuilder()
                .setContent(new SinkContentBuilder()
                        .setSinkType(sinkType)
                        .build())
                .build();
        SinkCacheEntity sinkCacheEntity = mock(SinkCacheEntity.class);
        when(sinkCacheEntity.getSink()).thenReturn(sink);
        return sinkCacheEntity;
    }
}
