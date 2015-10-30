package dk.dbc.dataio.jobstore.service.param;

import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.types.RecordSplitterConstants;
import dk.dbc.dataio.commons.utils.test.model.JobSpecificationBuilder;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnector;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnectorException;
import dk.dbc.dataio.jobstore.service.entity.JobEntity;
import dk.dbc.dataio.jobstore.types.Diagnostic;
import org.junit.Test;

import java.io.InputStream;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PartitioningParamTest extends ParamBaseTest {

    private final FileStoreServiceConnector mockedFileStoreServiceConnector = mock(FileStoreServiceConnector.class);
    private final static boolean NOT_DO_SEQUENCE_ANALYSIS = false;
    private final static RecordSplitterConstants.RecordSplitter XML_RECORD_SPLITTER = RecordSplitterConstants.RecordSplitter.XML;

    @Test
    public void constructor_jobEntityArgIsNull_throws() {
        try {
            new PartitioningParam(null, mockedFileStoreServiceConnector, NOT_DO_SEQUENCE_ANALYSIS, XML_RECORD_SPLITTER);
            fail("No exception thrown");
        } catch (NullPointerException e) {
        }
    }
    @Test
    public void constructor_fileStoreServiceConnectorArgIsNull_throws() {
        try {
            new PartitioningParam(new JobEntity(), null, NOT_DO_SEQUENCE_ANALYSIS, XML_RECORD_SPLITTER);
            fail("No exception thrown");
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void constructor_allArgsAreValid_returnsPartitioningParam() {

        final PartitioningParam partitioningParam = constructPartitioningParam(false, jobSpecification);
        assertThat(partitioningParam, is(notNullValue()));

        assertThat(partitioningParam.getDiagnostics(), is(notNullValue()));
        assertThat(partitioningParam.getDiagnostics().size(), is(0));
    }

    @Test
    public void extractDataFileIdFromURN_invalidUrn_diagnosticLevelFatalAddedForUrnAndDataFileInputStream() throws FlowStoreServiceConnectorException, FileStoreServiceConnectorException {
        when(mockedFileStoreServiceConnector.getFile(null)).thenThrow(new NullPointerException());

        final PartitioningParam partitioningParam = constructPartitioningParam(false, new JobSpecificationBuilder().setDataFile(DATA_FILE_ID).build());

        final List<Diagnostic> diagnostics = partitioningParam.getDiagnostics();

        assertThat(diagnostics.size(), is(1));
        assertThat(diagnostics.get(0).getLevel(), is(Diagnostic.Level.FATAL));
        assertThat(diagnostics.get(0).getMessage().contains(partitioningParam.getJobEntity().getSpecification().getDataFile()), is(true));
        assertThat(partitioningParam.getDataFileId(), is(nullValue()));
        assertThat(partitioningParam.getDataFileInputStream(), is(nullValue()));
    }

    @Test
    public void extractDataFileIdFromURN_validUrnAndFileFound_dataFileIdAndDataFileInputStreamAndDataPartitionerSet() throws FlowStoreServiceConnectorException, FileStoreServiceConnectorException {

        InputStream mockedInputStream = mock(InputStream.class);
        when(mockedFileStoreServiceConnector.getFile(anyString())).thenReturn(mockedInputStream);

        final PartitioningParam partitioningParam = constructPartitioningParam(true, jobSpecification);

        final List<Diagnostic> diagnostics = partitioningParam.getDiagnostics();
        assertThat(diagnostics.size(), is(0));
        assertThat(partitioningParam.getDataFileId(), is(notNullValue()));
        assertThat(partitioningParam.getDataFileInputStream(), is(notNullValue()));
        assertThat(partitioningParam.getDataPartitioner(), is(notNullValue()));

    }

    @Test
    public void newDataFileInputStream_fileNotFound_diagnosticLevelFatalAddedForInputStream() throws FlowStoreServiceConnectorException, FileStoreServiceConnectorException {

        when(mockedFileStoreServiceConnector.getFile(anyString())).thenThrow(new FileStoreServiceConnectorException(ERROR_MESSAGE));

        final PartitioningParam partitioningParam = constructPartitioningParam(true, jobSpecification);

        final List<Diagnostic> diagnostics = partitioningParam.getDiagnostics();
        assertThat(diagnostics.size(), is(1));
        assertThat(diagnostics.get(0).getLevel(), is(Diagnostic.Level.FATAL));
        assertThat(diagnostics.get(0).getMessage().contains(jobSpecification.getDataFile()), is(true));
    }

    @Test
    public void addJobParam_allReachableParametersSet_expectedValuesReturnedThroughGetters() throws FlowStoreServiceConnectorException, FileStoreServiceConnectorException {

        when(mockedFileStoreServiceConnector.getFile(anyString())).thenReturn(mock(InputStream.class));

        final PartitioningParam partitioningParam = constructPartitioningParam(true, jobSpecification);

        assertThat(partitioningParam.getSequenceAnalyserKeyGenerator(), is(notNullValue()));
        assertThat(partitioningParam.getDataPartitioner(), is(notNullValue()));
        assertThat(partitioningParam.getDiagnostics().size(), is(0));
        assertThat(partitioningParam.getDataFileId(), is(DATA_FILE_ID));
    }

    /*
     * private methods
     */

    private PartitioningParam constructPartitioningParam(boolean isDataFileInputStreamMocked, JobSpecification jobSpecification){
        final PartitioningParam partitioningParam;
        final JobEntity jobEntity;

        if(isDataFileInputStreamMocked) {
            jobEntity = new JobEntity();
            jobEntity.setSpecification(jobSpecification);
            partitioningParam = new PartitioningParam(jobEntity, mockedFileStoreServiceConnector, NOT_DO_SEQUENCE_ANALYSIS, XML_RECORD_SPLITTER);
            partitioningParam.dataFileInputStream = mock(InputStream.class);
        } else {

            jobEntity = new JobEntity();
            jobEntity.setSpecification(jobSpecification);
            partitioningParam = new PartitioningParam(jobEntity, mockedFileStoreServiceConnector, NOT_DO_SEQUENCE_ANALYSIS, XML_RECORD_SPLITTER);
        }
        return partitioningParam;
    }
}
