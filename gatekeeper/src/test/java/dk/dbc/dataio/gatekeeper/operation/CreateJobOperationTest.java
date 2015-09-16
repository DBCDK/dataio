package dk.dbc.dataio.gatekeeper.operation;

import dk.dbc.dataio.commons.types.Constants;
import dk.dbc.dataio.commons.types.FileStoreUrn;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorUnexpectedStatusCodeException;
import dk.dbc.dataio.commons.utils.jobstore.MockedJobStoreServiceConnector;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnector;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnectorException;
import dk.dbc.dataio.jobstore.test.types.JobInfoSnapshotBuilder;
import dk.dbc.dataio.jobstore.types.JobInputStream;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CreateJobOperationTest {
    private final MockedJobStoreServiceConnector jobStoreServiceConnector = new MockedJobStoreServiceConnector();
    private final FileStoreServiceConnector fileStoreServiceConnector = mock(FileStoreServiceConnector.class);
    private final String fileStoreId = "42";
    private final String transfileName = "123456.001.trans";

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    @Before
    public void setupMocks() throws FileStoreServiceConnectorException {
        when(fileStoreServiceConnector.addFile(any(InputStream.class))).thenReturn(fileStoreId);
        jobStoreServiceConnector.jobInfoSnapshots.clear();
        jobStoreServiceConnector.jobInfoSnapshots.add(new JobInfoSnapshotBuilder().build());
    }

    @Test(expected = NullPointerException.class)
    public void constructor_jobStoreServiceConnectorArgIsNull_throws() {
        new CreateJobOperation(null, fileStoreServiceConnector, Paths.get("wd"), transfileName, "foo");
    }

    @Test(expected = NullPointerException.class)
    public void constructor_fileStoreServiceConnectorArgIsNull_throws() {
        new CreateJobOperation(jobStoreServiceConnector, null, Paths.get("wd"), transfileName, "foo");
    }

    @Test(expected = NullPointerException.class)
    public void constructor_workingDirArgIsNull_throws() {
        new CreateJobOperation(jobStoreServiceConnector, fileStoreServiceConnector, null, transfileName, "foo");
    }

    @Test(expected = NullPointerException.class)
    public void constructor_transfileNameArgIsNull_throws() {
        new CreateJobOperation(jobStoreServiceConnector, fileStoreServiceConnector, Paths.get("wd"), null, "foo");
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_transfileNameArgIsEmpty_throws() {
        new CreateJobOperation(jobStoreServiceConnector, fileStoreServiceConnector, Paths.get("wd"), "  ", "foo");
    }

    @Test(expected = NullPointerException.class)
    public void constructor_transfileDataArgIsNull_throws() {
        new CreateJobOperation(jobStoreServiceConnector, fileStoreServiceConnector, Paths.get("wd"), transfileName, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_transfileDataArgIsEmpty_throws() {
        new CreateJobOperation(jobStoreServiceConnector, fileStoreServiceConnector, Paths.get("wd"), transfileName, "  ");
    }

    @Test
    public void constructor_allArgsAreValid_returnsNewInstance() {
        final Path workingDir = Paths.get("wd");
        final String transfileData = "foo";
        final CreateJobOperation createJobOperation = new CreateJobOperation(
                jobStoreServiceConnector, fileStoreServiceConnector, workingDir, transfileName, transfileData);
        assertThat("instance", createJobOperation, is(notNullValue()));
        assertThat("getJobStoreServiceConnector()", createJobOperation.getJobStoreServiceConnector(), is((JobStoreServiceConnector) jobStoreServiceConnector));
        assertThat("getFileStoreServiceConnector()", createJobOperation.getFileStoreServiceConnector(), is(fileStoreServiceConnector));
        assertThat("getWorkingDir()", createJobOperation.getWorkingDir(), is(workingDir));
        assertThat("getTransfileData()", createJobOperation.getTransfileData(), is(transfileData));
    }

    @Test
    public void execute_datafileIsUndefined_createsJobWithMissingDataFilePlaceHolder() throws OperationExecutionException, FileStoreServiceConnectorException {
        final Path workingDir = Paths.get("wd");
        final String transfileData = "foo";
        final CreateJobOperation createJobOperation = new CreateJobOperation(
                jobStoreServiceConnector, fileStoreServiceConnector, workingDir, transfileName, transfileData);
        createJobOperation.execute();

        final JobInputStream jobInputStream = jobStoreServiceConnector.jobInputStreams.remove();
        assertThat(jobInputStream.getJobSpecification().getDataFile(), is(Constants.MISSING_FIELD_VALUE));

        verify(fileStoreServiceConnector, times(0)).addFile(any(InputStream.class));
    }

    @Test
    public void execute_datafileDoesNotExist_createsJobWithOriginalDataFileName() throws OperationExecutionException, FileStoreServiceConnectorException {
        final String transfileData = "f=123456.file";
        final CreateJobOperation createJobOperation = new CreateJobOperation(
                jobStoreServiceConnector, fileStoreServiceConnector, testFolder.getRoot().toPath(), transfileName, transfileData);
        createJobOperation.execute();

        final JobInputStream jobInputStream = jobStoreServiceConnector.jobInputStreams.remove();
        assertThat(jobInputStream.getJobSpecification().getDataFile(), is("123456.file"));

        verify(fileStoreServiceConnector, times(0)).addFile(any(InputStream.class));
    }

    @Test
    public void execute_dataFileReadFails_throws() throws OperationExecutionException, FileStoreServiceConnectorException, IOException {
        testFolder.newFolder("123456.file");

        final String transfileData = "f=123456.file";
        final CreateJobOperation createJobOperation = new CreateJobOperation(
                jobStoreServiceConnector, fileStoreServiceConnector, testFolder.getRoot().toPath(), transfileName, transfileData);
        try {
            createJobOperation.execute();
            fail("No OperationExecutionException thrown");
        } catch (OperationExecutionException e) {
            assertThat(e.getCause() instanceof IOException, is(true));
        }
    }

    @Test
    public void execute_fileStoreServiceConnectorThrows_throws() throws OperationExecutionException, FileStoreServiceConnectorException, IOException {
        final Exception exception = new FileStoreServiceConnectorException("DIED");
        when(fileStoreServiceConnector.addFile(any(InputStream.class))).thenThrow(exception);
        testFolder.newFile("123456.file");

        final String transfileData = "f=123456.file";
        final CreateJobOperation createJobOperation = new CreateJobOperation(
                jobStoreServiceConnector, fileStoreServiceConnector, testFolder.getRoot().toPath(), transfileName, transfileData);
        try {
            createJobOperation.execute();
            fail("No OperationExecutionException thrown");
        } catch (OperationExecutionException e) {
            assertThat((Exception) e.getCause(), is(exception));
        }
    }

    @Test
    public void execute_dataFileExists_uploadsToFileStore()
            throws IOException, OperationExecutionException, FileStoreServiceConnectorException, URISyntaxException {
        testFolder.newFile("123456.file");
        final String transfileData = "f=123456.file";
        final CreateJobOperation createJobOperation = new CreateJobOperation(
                jobStoreServiceConnector, fileStoreServiceConnector, testFolder.getRoot().toPath(), transfileName, transfileData);
        createJobOperation.execute();

        final JobInputStream jobInputStream = jobStoreServiceConnector.jobInputStreams.remove();
        assertThat(jobInputStream.getJobSpecification().getDataFile(), is(FileStoreUrn.create(fileStoreId).toString()));

        verify(fileStoreServiceConnector, times(1)).addFile(any(InputStream.class));
    }

    @Test
    public void execute_jobStoreServiceConnectorThrows_removesFileFromFileStoreAndThrows()
            throws IOException, OperationExecutionException, JobStoreServiceConnectorException, FileStoreServiceConnectorException {
        final Exception exception = new JobStoreServiceConnectorException("DIED");
        final JobStoreServiceConnector jssc = mock(JobStoreServiceConnector.class);
        when(jssc.addJob(any(JobInputStream.class))).thenThrow(exception);

        testFolder.newFile("123456.file");
        final String transfileData = "f=123456.file";
        final CreateJobOperation createJobOperation = new CreateJobOperation(
                jssc, fileStoreServiceConnector, testFolder.getRoot().toPath(), transfileName, transfileData);
        try {
            createJobOperation.execute();
            fail("No OperationExecutionException thrown");
        } catch (OperationExecutionException e) {
            assertThat((Exception) e.getCause(), is(exception));
        }

        verify(fileStoreServiceConnector).deleteFile(fileStoreId);
    }

    @Test
    public void execute_jobStoreServiceConnectorThrowsWithInternalServerError_throws()
            throws IOException, OperationExecutionException, JobStoreServiceConnectorException, FileStoreServiceConnectorException {
        final Exception exception = new JobStoreServiceConnectorUnexpectedStatusCodeException(
                "DIED", Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        final JobStoreServiceConnector jssc = mock(JobStoreServiceConnector.class);
        when(jssc.addJob(any(JobInputStream.class))).thenThrow(exception);

        testFolder.newFile("123456.file");
        final String transfileData = "f=123456.file";
        final CreateJobOperation createJobOperation = new CreateJobOperation(
                jssc, fileStoreServiceConnector, testFolder.getRoot().toPath(), transfileName, transfileData);
        try {
            createJobOperation.execute();
            fail("No OperationExecutionException thrown");
        } catch (OperationExecutionException e) {
            assertThat((Exception) e.getCause(), is(exception));
        }

        verify(fileStoreServiceConnector, times(0)).deleteFile(fileStoreId);
    }
}