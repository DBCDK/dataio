package dk.dbc.dataio.jobstore.service.param;

import dk.dbc.dataio.commons.types.Diagnostic;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.types.RecordSplitterConstants;
import dk.dbc.dataio.commons.utils.test.model.JobSpecificationBuilder;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnector;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnectorException;
import dk.dbc.dataio.jobstore.service.entity.JobEntity;
import dk.dbc.dataio.jobstore.service.partitioner.DanMarc2LineFormatDataPartitioner;
import dk.dbc.dataio.jobstore.service.partitioner.DanMarc2LineFormatReorderingDataPartitioner;
import dk.dbc.dataio.jobstore.service.partitioner.DataPartitioner;
import dk.dbc.dataio.jobstore.service.partitioner.Iso2709DataPartitioner;
import dk.dbc.dataio.jobstore.service.partitioner.Iso2709ReorderingDataPartitioner;
import dk.dbc.dataio.jobstore.types.State;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;

import javax.persistence.EntityManager;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PartitioningParamTest extends ParamBaseTest {
    private final InputStream inputStream = mock(InputStream.class);
    private final FileStoreServiceConnector fileStoreServiceConnector = mock(FileStoreServiceConnector.class);
    private final EntityManager entityManager = mock(EntityManager.class);
    private final RecordSplitterConstants.RecordSplitter dataPartitionerType = RecordSplitterConstants.RecordSplitter.XML;

    @Before
    public void setFileStoreServiceConnectorExpectations() throws FileStoreServiceConnectorException {
        when(fileStoreServiceConnector.getFile(anyString())).thenReturn(inputStream);
    }

    @Before
    public void setInputStreamExpectations() throws IOException {
        when(inputStream.read(Matchers.<byte[]>any(), anyInt(), anyInt())).thenReturn(32);  // Return space
    }

    @Before
    public void setupJobSpecification() {
        jobSpecificationBuilder.setCharset("latin1");
    }

    @Test
    public void constructor_jobEntityArgIsNull_throws() {
        try {
            new PartitioningParam(null, fileStoreServiceConnector, entityManager, dataPartitionerType);
            fail("No exception thrown");
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void constructor_fileStoreServiceConnectorArgIsNull_throws() {
        try {
            new PartitioningParam(new JobEntity(), null, entityManager, dataPartitionerType);
            fail("No exception thrown");
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void constructor_entityManagerArgIsNull_throws() {
        try {
            new PartitioningParam(new JobEntity(), fileStoreServiceConnector, null, dataPartitionerType);
            fail("No exception thrown");
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void constructor_dataPartitionerTypeArgIsNull_throws() {
        try {
            new PartitioningParam(new JobEntity(), fileStoreServiceConnector, entityManager, null);
            fail("No exception thrown");
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void constructor_allArgsAreValid_returnsPartitioningParam() {
        final PartitioningParam partitioningParam = newPartitioningParam(getJobEntity(jobSpecification));
        assertThat(partitioningParam, is(notNullValue()));
        assertThat(partitioningParam.getDiagnostics(), is(notNullValue()));
        assertThat(partitioningParam.getDiagnostics().size(), is(0));
    }

    @Test
    public void extractDataFileIdFromURN_invalidUrn_diagnosticLevelFatalAddedForUrnAndDataFileInputStream() throws FileStoreServiceConnectorException {
        when(fileStoreServiceConnector.getFile(null)).thenThrow(new NullPointerException());

        final JobSpecification jobSpecification = new JobSpecificationBuilder().setDataFile("invalid_urn").build();

        final PartitioningParam partitioningParam = newPartitioningParam(getJobEntity(jobSpecification));

        final List<Diagnostic> diagnostics = partitioningParam.getDiagnostics();
        assertThat(diagnostics.size(), is(1));
        assertThat(diagnostics.get(0).getLevel(), is(Diagnostic.Level.FATAL));
        assertThat(diagnostics.get(0).getMessage().contains(partitioningParam.getJobEntity().getSpecification().getDataFile()), is(true));
        assertThat(partitioningParam.getDataFileId(), is(nullValue()));
        assertThat(partitioningParam.getDataFileInputStream(), is(nullValue()));
    }

    @Test
    public void extractDataFileIdFromURN_validUrnAndFileFound_dataFileIdAndDataFileInputStreamAndDataPartitionerSet() throws FileStoreServiceConnectorException {
        final InputStream mockedInputStream = mock(InputStream.class);
        when(fileStoreServiceConnector.getFile(anyString())).thenReturn(mockedInputStream);

        final PartitioningParam partitioningParam = newPartitioningParam(getJobEntity(jobSpecification));

        final List<Diagnostic> diagnostics = partitioningParam.getDiagnostics();
        assertThat(diagnostics.size(), is(0));
        assertThat(partitioningParam.getDataFileId(), is(notNullValue()));
        assertThat(partitioningParam.getDataFileInputStream(), is(notNullValue()));
        assertThat(partitioningParam.getDataPartitioner(), is(notNullValue()));
    }

    @Test
    public void newDataFileInputStream_fileNotFound_diagnosticLevelFatalAddedForInputStream() throws FileStoreServiceConnectorException {
        when(fileStoreServiceConnector.getFile(anyString())).thenThrow(new FileStoreServiceConnectorException(ERROR_MESSAGE));

        final PartitioningParam partitioningParam = newPartitioningParam(getJobEntity(jobSpecification));

        final List<Diagnostic> diagnostics = partitioningParam.getDiagnostics();
        assertThat(diagnostics.size(), is(1));
        assertThat(diagnostics.get(0).getLevel(), is(Diagnostic.Level.FATAL));
        assertThat(diagnostics.get(0).getMessage().contains(jobSpecification.getDataFile()), is(true));
    }

    @Test
    public void addJobParam_allReachableParametersSet_expectedValuesReturnedThroughGetters() throws FileStoreServiceConnectorException {
        when(fileStoreServiceConnector.getFile(anyString())).thenReturn(mock(InputStream.class));

        final PartitioningParam partitioningParam = newPartitioningParam(getJobEntity(jobSpecification));

        assertThat(partitioningParam.getSequenceAnalyserKeyGenerator(), is(notNullValue()));
        assertThat(partitioningParam.getDataPartitioner(), is(notNullValue()));
        assertThat(partitioningParam.getDiagnostics().size(), is(0));
        assertThat(partitioningParam.getDataFileId(), is(DATA_FILE_ID));
    }

    @Test
    public void typeOfDataPartitioner_whenJobSpecificationAncestryIsNull_isDanMarc2LineFormatDataPartitioner() {
        final JobEntity jobEntity = getJobEntity(jobSpecificationBuilder.setAncestry(null).build());
        final PartitioningParam partitioningParam = newPartitioningParamForDanMarc2LineFormat(jobEntity);
        final DataPartitioner dataPartitioner = partitioningParam.getDataPartitioner();
        assertThat("Class", dataPartitioner instanceof DanMarc2LineFormatDataPartitioner, is(true));
        assertThat("Reordering variant", dataPartitioner instanceof DanMarc2LineFormatReorderingDataPartitioner, is(false));
    }

    @Test
    public void typeOfDataPartitioner_whenJobSpecificationAncestryTransfileIsSet_isDanMarc2LineFormatReorderingDataPartitioner() {
        final JobSpecification.Ancestry ancestry = new JobSpecificationBuilder.AncestryBuilder().setTransfile("file").build();
        final JobEntity jobEntity = getJobEntity(jobSpecificationBuilder.setAncestry(ancestry).build());
        final PartitioningParam partitioningParam = newPartitioningParamForDanMarc2LineFormat(jobEntity);
        final DataPartitioner dataPartitioner = partitioningParam.getDataPartitioner();
        assertThat("Class", dataPartitioner instanceof DanMarc2LineFormatDataPartitioner, is(true));
        assertThat("Reordering variant", dataPartitioner instanceof DanMarc2LineFormatReorderingDataPartitioner, is(true));
    }

    @Test
    public void typeOfDataPartitioner_whenJobSpecificationAncestryIsNull_isIso2709DataPartitioner() {
        final JobEntity jobEntity = getJobEntity(jobSpecificationBuilder.setAncestry(null).build());
        final PartitioningParam partitioningParam = newPartitioningParamForIso2709(jobEntity);
        final DataPartitioner dataPartitioner = partitioningParam.getDataPartitioner();
        assertThat("Class", dataPartitioner instanceof Iso2709DataPartitioner, is(true));
        assertThat("Reordering variant", dataPartitioner instanceof Iso2709ReorderingDataPartitioner, is(false));
    }

    @Test
    public void typeOfDataPartitioner_whenJobSpecificationAncestryTransfileIsSet_isIso2709ReorderingDataPartitioner() {
        final JobSpecification.Ancestry ancestry = new JobSpecificationBuilder.AncestryBuilder().setTransfile("file").build();
        final JobEntity jobEntity = getJobEntity(jobSpecificationBuilder.setAncestry(ancestry).build());
        final PartitioningParam partitioningParam = newPartitioningParamForIso2709(jobEntity);
        final DataPartitioner dataPartitioner = partitioningParam.getDataPartitioner();
        assertThat("Class", dataPartitioner instanceof Iso2709DataPartitioner, is(true));
        assertThat("Reordering variant", dataPartitioner instanceof Iso2709ReorderingDataPartitioner, is(true));
    }

    private PartitioningParam newPartitioningParam(JobEntity jobEntity) {
        return new PartitioningParam(jobEntity, fileStoreServiceConnector, entityManager, dataPartitionerType);
    }

    private PartitioningParam newPartitioningParamForDanMarc2LineFormat(JobEntity jobEntity) {
        return new PartitioningParam(jobEntity, fileStoreServiceConnector, entityManager,
                RecordSplitterConstants.RecordSplitter.DANMARC2_LINE_FORMAT);
    }

    private PartitioningParam newPartitioningParamForIso2709(JobEntity jobEntity) {
        return new PartitioningParam(jobEntity, fileStoreServiceConnector, entityManager,
                RecordSplitterConstants.RecordSplitter.ISO2709);
    }

    private JobEntity getJobEntity(JobSpecification jobSpecification) {
        final JobEntity jobEntity = new JobEntity();
        jobEntity.setSpecification(jobSpecification);
        jobEntity.setState(new State());
        return jobEntity;
    }
}
