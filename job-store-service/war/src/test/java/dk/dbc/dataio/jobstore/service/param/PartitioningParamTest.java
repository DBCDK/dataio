package dk.dbc.dataio.jobstore.service.param;

import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.commons.types.Diagnostic;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.types.RecordSplitterConstants;
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
import dk.dbc.dataio.jobstore.service.entity.ReorderedItemEntity;
import dk.dbc.dataio.jobstore.service.entity.SinkCacheEntity;
import dk.dbc.dataio.jobstore.service.partitioner.DanMarc2LineFormatDataPartitioner;
import dk.dbc.dataio.jobstore.service.partitioner.DanMarc2LineFormatReorderingDataPartitioner;
import dk.dbc.dataio.jobstore.service.partitioner.DataPartitioner;
import dk.dbc.dataio.jobstore.service.partitioner.IncludeFilterDataPartitioner;
import dk.dbc.dataio.jobstore.service.partitioner.Iso2709DataPartitioner;
import dk.dbc.dataio.jobstore.service.partitioner.Iso2709ReorderingDataPartitioner;
import dk.dbc.dataio.jobstore.service.partitioner.VolumeAfterParents;
import dk.dbc.dataio.jobstore.types.FlowStoreReference;
import dk.dbc.dataio.jobstore.types.FlowStoreReferences;
import dk.dbc.dataio.jobstore.types.State;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.junit.Before;
import org.junit.Test;

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
    private final RecordSplitterConstants.RecordSplitter dataPartitionerType = RecordSplitterConstants.RecordSplitter.XML;
    private final Submitter expected_submitter = new SubmitterBuilder().build();

    @Before
    public void setupJobSpecification() {
        jobSpecification.withCharset("latin1");
    }

    @Before
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
        final PartitioningParam partitioningParam = newPartitioningParam(newJobEntity(jobSpecification));
        assertThat(partitioningParam, is(notNullValue()));
        assertThat(partitioningParam.getDiagnostics(), is(notNullValue()));
        assertThat(partitioningParam.getDiagnostics().size(), is(0));
        assertThat(partitioningParam.isPreviewOnly(), is(false));
    }

    @Test
    public void extractDataFileIdFromURN_invalidUrn_diagnosticLevelFatalAddedForUrnAndDataFileInputStream() throws FileStoreServiceConnectorException {
        when(fileStoreServiceConnector.getFile(null)).thenThrow(new NullPointerException());
        jobSpecification.withDataFile("invalid_urn");

        final PartitioningParam partitioningParam = newPartitioningParam(newJobEntity(jobSpecification));

        final List<Diagnostic> diagnostics = partitioningParam.getDiagnostics();
        assertThat(diagnostics.size(), is(1));
        assertThat(diagnostics.get(0).getLevel(), is(Diagnostic.Level.FATAL));
        assertThat(diagnostics.get(0).getMessage().contains(partitioningParam.getJobEntity().getSpecification().getDataFile()), is(true));
        assertThat(partitioningParam.getDataFileId(), is(nullValue()));
        assertThat(partitioningParam.getDataFileInputStream(), is(nullValue()));
    }

    @Test
    public void extractDataFileIdFromURN_validUrnAndFileFound_dataFileIdAndDataFileInputStreamAndDataPartitionerSet() {
        final PartitioningParam partitioningParam = newPartitioningParam(newJobEntity(jobSpecification));

        final List<Diagnostic> diagnostics = partitioningParam.getDiagnostics();
        assertThat(diagnostics.size(), is(0));
        assertThat(partitioningParam.getDataFileId(), is(notNullValue()));
        assertThat(partitioningParam.getDataFileInputStream(), is(notNullValue()));
        assertThat(partitioningParam.getDataPartitioner(), is(notNullValue()));
    }

    @Test
    public void newDataFileInputStream_fileNotFound_diagnosticLevelFatalAddedForInputStream() throws FileStoreServiceConnectorException {
        when(fileStoreServiceConnector.getFile(anyString())).thenThrow(new FileStoreServiceConnectorException(ERROR_MESSAGE));
        final PartitioningParam partitioningParam = newPartitioningParam(newJobEntity(jobSpecification));

        final List<Diagnostic> diagnostics = partitioningParam.getDiagnostics();
        assertThat(diagnostics.size(), is(1));
        assertThat(diagnostics.get(0).getLevel(), is(Diagnostic.Level.FATAL));
        assertThat(diagnostics.get(0).getMessage().contains(jobSpecification.getDataFile()), is(true));
    }

    @Test
    public void addJobParam_allReachableParametersSet_expectedValuesReturnedThroughGetters() throws FileStoreServiceConnectorException {
        when(fileStoreServiceConnector.getFile(anyString())).thenReturn(mock(InputStream.class));
        final PartitioningParam partitioningParam = newPartitioningParam(newJobEntity(jobSpecification));

        assertThat(partitioningParam.getKeyGenerator(), is(notNullValue()));
        assertThat(partitioningParam.getDataPartitioner(), is(notNullValue()));
        assertThat(partitioningParam.getDiagnostics().size(), is(0));
        assertThat(partitioningParam.getDataFileId(), is(DATA_FILE_ID));
    }

    @Test
    public void typeOfDataPartitioner_lineFormat() {
        final JobEntity jobEntity = newJobEntity(jobSpecification);
        final PartitioningParam partitioningParam = newPartitioningParamForDanMarc2LineFormat(jobEntity);
        final DataPartitioner dataPartitioner = partitioningParam.getDataPartitioner();
        assertThat("Class", dataPartitioner instanceof DanMarc2LineFormatDataPartitioner,
                is(true));
        assertThat("Reordering", dataPartitioner instanceof DanMarc2LineFormatReorderingDataPartitioner,
                is(false));
    }

    @Test
    public void typeOfDataPartitioner_lineFormatWhenAncestryTransfileIsSet() {
        final JobSpecification.Ancestry ancestry = new JobSpecification.Ancestry().withTransfile("file");
        final JobEntity jobEntity = newJobEntity(jobSpecification.withAncestry(ancestry));
        final PartitioningParam partitioningParam = newPartitioningParamForDanMarc2LineFormat(jobEntity);
        final DataPartitioner dataPartitioner = partitioningParam.getDataPartitioner();
        assertThat("Class", dataPartitioner instanceof DanMarc2LineFormatDataPartitioner,
                is(true));
        assertThat("Reordering", dataPartitioner instanceof DanMarc2LineFormatReorderingDataPartitioner,
                is(true));
        final DanMarc2LineFormatReorderingDataPartitioner lineFormatDataPartitioner =
                (DanMarc2LineFormatReorderingDataPartitioner) dataPartitioner;
        assertThat("Reordering variant", lineFormatDataPartitioner.getReorderer() instanceof VolumeAfterParents,
                is(true));
    }

    @Test
    public void typeOfDataPartitioner_iso2709() {
        final JobEntity jobEntity = newJobEntity(jobSpecification);
        final PartitioningParam partitioningParam = newPartitioningParamForIso2709(jobEntity);
        final DataPartitioner dataPartitioner = partitioningParam.getDataPartitioner();
        assertThat("Class", dataPartitioner instanceof Iso2709DataPartitioner,
                is(true));
        assertThat("Reordering", dataPartitioner instanceof Iso2709ReorderingDataPartitioner,
                is(false));
    }

    @Test
    public void typeOfDataPartitioner_iso2709WhenAncestryTransfileIsSet() {
        final JobSpecification.Ancestry ancestry = new JobSpecification.Ancestry().withTransfile("file");
        final JobEntity jobEntity = newJobEntity(jobSpecification.withAncestry(ancestry));
        final PartitioningParam partitioningParam = newPartitioningParamForIso2709(jobEntity);
        final DataPartitioner dataPartitioner = partitioningParam.getDataPartitioner();
        assertThat("Class", dataPartitioner instanceof Iso2709DataPartitioner,
                is(true));
        assertThat("Reordering", dataPartitioner instanceof Iso2709ReorderingDataPartitioner,
                is(true));
        final Iso2709ReorderingDataPartitioner iso2709DataPartitioner =
                (Iso2709ReorderingDataPartitioner) dataPartitioner;
        assertThat("Reordering variant", iso2709DataPartitioner.getReorderer() instanceof VolumeAfterParents,
                is(true));
    }

    @Test
    public void typeOfDataPartitioner_whenIncludeFilterIsNotNull() {
        final JobEntity jobEntity = newJobEntity(jobSpecification);
        final PartitioningParam param = new PartitioningParam(jobEntity,
                fileStoreServiceConnector, flowStoreServiceConnector,
                entityManager, RecordSplitterConstants.RecordSplitter.ISO2709,
                new BitSet());
        assertThat("IncludeFilterDataPartitioner",
                param.getDataPartitioner() instanceof IncludeFilterDataPartitioner, is(true));
        assertThat("wrapping",
                ((IncludeFilterDataPartitioner) param.getDataPartitioner()).getWrappedDataPartitioner()
                        instanceof Iso2709DataPartitioner, is(true));
    }

    @Test
    public void isPreviewOnly_whenTypeAcctest_isFalse() throws FlowStoreServiceConnectorException {
        when(flowStoreServiceConnector.getSubmitter(anyLong())).thenReturn(new SubmitterBuilder().setContent(new SubmitterContentBuilder().setEnabled(false).build()).build());
        final JobEntity jobEntity = newJobEntity(jobSpecification.withType(JobSpecification.Type.ACCTEST));
        final PartitioningParam partitioningParam = newPartitioningParam(jobEntity);
        assertThat(partitioningParam.isPreviewOnly(), is(false));
    }

    @Test
    public void isPreviewOnly_whenSubmitterIsEnabled_isFalse() {
        final JobEntity jobEntity = newJobEntity(jobSpecification.withType(JobSpecification.Type.PERSISTENT));
        final PartitioningParam partitioningParam = newPartitioningParam(jobEntity);
        assertThat(partitioningParam.isPreviewOnly(), is(false));
    }

    @Test
    public void isPreviewOnly_whenSubmitterIsDisabled_isTrue() throws FlowStoreServiceConnectorException {
        when(flowStoreServiceConnector.getSubmitter(anyLong())).thenReturn(new SubmitterBuilder().setContent(new SubmitterContentBuilder().setEnabled(false).build()).build());
        final JobEntity jobEntity = newJobEntity(jobSpecification.withType(JobSpecification.Type.PERSISTENT));
        final PartitioningParam partitioningParam = newPartitioningParam(jobEntity);
        assertThat(partitioningParam.isPreviewOnly(), is(true));
    }

    private PartitioningParam newPartitioningParam(JobEntity jobEntity) {
        return new PartitioningParam(jobEntity, fileStoreServiceConnector, flowStoreServiceConnector, entityManager, dataPartitionerType);
    }

    private PartitioningParam newPartitioningParamForDanMarc2LineFormat(JobEntity jobEntity) {
        return new PartitioningParam(jobEntity, fileStoreServiceConnector, flowStoreServiceConnector, entityManager,
                RecordSplitterConstants.RecordSplitter.DANMARC2_LINE_FORMAT);
    }

    private PartitioningParam newPartitioningParamForIso2709(JobEntity jobEntity) {
        return new PartitioningParam(jobEntity, fileStoreServiceConnector, flowStoreServiceConnector, entityManager,
                RecordSplitterConstants.RecordSplitter.ISO2709);
    }

    private JobEntity newJobEntity(JobSpecification jobSpecification) {
        final JobEntity jobEntity = new JobEntity();
        jobEntity.setSpecification(jobSpecification);
        jobEntity.setState(new State());
        final FlowStoreReference flowStoreReference = new FlowStoreReference(
                expected_submitter.getId(), expected_submitter.getVersion(),
                expected_submitter.getContent().getName());
        final FlowStoreReferences flowStoreReferences = new FlowStoreReferences();
        flowStoreReferences.setReference(FlowStoreReferences.Elements.SUBMITTER, flowStoreReference);
        jobEntity.setFlowStoreReferences(flowStoreReferences);
        jobEntity.setCachedSink(newSinkCacheEntity(SinkContent.SinkType.DUMMY));
        return jobEntity;
    }

    private static SinkCacheEntity newSinkCacheEntity(SinkContent.SinkType sinkType) {
        final Sink sink = new SinkBuilder()
                .setContent(new SinkContentBuilder()
                        .setSinkType(sinkType)
                        .build())
                .build();
        final SinkCacheEntity sinkCacheEntity = mock(SinkCacheEntity.class);
        when(sinkCacheEntity.getSink()).thenReturn(sink);
        return sinkCacheEntity;
    }
}
