package dk.dbc.dataio.gatekeeper.operation;

import dk.dbc.dataio.commons.types.Constants;
import dk.dbc.dataio.commons.types.FileStoreUrn;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorUnexpectedStatusCodeException;
import dk.dbc.dataio.commons.utils.jobstore.MockedJobStoreServiceConnector;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnector;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnectorException;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.jobstore.types.JobInputStream;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import static dk.dbc.commons.testutil.Assert.assertThat;
import static dk.dbc.commons.testutil.Assert.isThrowing;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CreateJobOperationTest {
    private final MockedJobStoreServiceConnector jobStoreServiceConnector = new MockedJobStoreServiceConnector();
    private final FileStoreServiceConnector fileStoreServiceConnector = mock(FileStoreServiceConnector.class);
    private final String fileStoreId = "42";
    private final String transfileName = "123456.001.trans";

    @TempDir
    public Path testFolder;

    @BeforeEach
    public void setupMocks() throws FileStoreServiceConnectorException {
        when(fileStoreServiceConnector.addFile(any(InputStream.class))).thenReturn(fileStoreId);
        jobStoreServiceConnector.jobInfoSnapshots.clear();
        jobStoreServiceConnector.jobInfoSnapshots.add(new JobInfoSnapshot());
    }

    @Test
    public void constructor_jobStoreServiceConnectorArgIsNull_throws() {
        assertThrows(NullPointerException.class, () -> new CreateJobOperation(null, fileStoreServiceConnector, Paths.get("wd"), transfileName, "foo"));
    }

    @Test
    public void constructor_fileStoreServiceConnectorArgIsNull_throws() {
        assertThrows(NullPointerException.class, () -> new CreateJobOperation(jobStoreServiceConnector, null, Paths.get("wd"), transfileName, "foo"));
    }

    @Test
    public void constructor_workingDirArgIsNull_throws() {
        assertThrows(NullPointerException.class, () -> new CreateJobOperation(jobStoreServiceConnector, fileStoreServiceConnector, null, transfileName, "foo"));
    }

    @Test
    public void constructor_transfileNameArgIsNull_throws() {
        assertThrows(NullPointerException.class, () -> new CreateJobOperation(jobStoreServiceConnector, fileStoreServiceConnector, Paths.get("wd"), null, "foo"));
    }

    @Test
    public void constructor_transfileNameArgIsEmpty_throws() {
        assertThrows(IllegalArgumentException.class, () -> new CreateJobOperation(jobStoreServiceConnector, fileStoreServiceConnector, Paths.get("wd"), "  ", "foo"));
    }

    @Test
    public void constructor_transfileDataArgIsNull_throws() {
        assertThrows(NullPointerException.class, () -> new CreateJobOperation(jobStoreServiceConnector, fileStoreServiceConnector, Paths.get("wd"), transfileName, null));
    }

    @Test
    public void constructor_transfileDataArgIsEmpty_throws() {
        assertThrows(IllegalArgumentException.class, () -> new CreateJobOperation(jobStoreServiceConnector, fileStoreServiceConnector, Paths.get("wd"), transfileName, "  "));
    }

    @Test
    public void constructor_allArgsAreValid_returnsNewInstance() throws IOException {
        Path workingDir = Files.createDirectory(testFolder.resolve(UUID.randomUUID().toString()));

        final String transfileData = "foo";
        CreateJobOperation createJobOperation = new CreateJobOperation(
                jobStoreServiceConnector, fileStoreServiceConnector, workingDir, transfileName, transfileData);
        assertThat("instance", createJobOperation, is(notNullValue()));
        assertThat("getJobStoreServiceConnector()", createJobOperation.getJobStoreServiceConnector(), is(jobStoreServiceConnector));
        assertThat("getFileStoreServiceConnector()", createJobOperation.getFileStoreServiceConnector(), is(fileStoreServiceConnector));
        assertThat("getWorkingDir()", createJobOperation.getWorkingDir(), is(workingDir));
        assertThat("getTransfileData()", createJobOperation.getTransfileData(), is(transfileData));
    }

    @Test
    public void execute_datafileIsUndefined_createsJobWithMissingDataFilePlaceHolder() throws OperationExecutionException, FileStoreServiceConnectorException, IOException {
        Files.createFile(testFolder.resolve(transfileName));
        final String transfileData = "foo";
        CreateJobOperation createJobOperation = new CreateJobOperation(
                jobStoreServiceConnector, fileStoreServiceConnector, testFolder, transfileName, transfileData);

        createJobOperation.execute();

        JobInputStream jobInputStream = jobStoreServiceConnector.jobInputStreams.remove();
        assertThat(jobInputStream.getJobSpecification().getDataFile(), is(Constants.MISSING_FIELD_VALUE));

        verify(fileStoreServiceConnector, times(0)).addFile(any(InputStream.class));
    }

    @Test
    public void execute_datafileDoesNotExist_createsJobWithOriginalDataFileName() throws OperationExecutionException, FileStoreServiceConnectorException, IOException {
        Files.createFile(testFolder.resolve(transfileName));
        final String transfileData = "f=123456.file";
        CreateJobOperation createJobOperation = new CreateJobOperation(
                jobStoreServiceConnector, fileStoreServiceConnector, testFolder, transfileName, transfileData);

        createJobOperation.execute();

        JobInputStream jobInputStream = jobStoreServiceConnector.jobInputStreams.remove();
        assertThat(jobInputStream.getJobSpecification().getAncestry().getDatafile(), is("123456.file"));
        assertThat(jobInputStream.getJobSpecification().getDataFile(), is(Constants.MISSING_FIELD_VALUE));

        verify(fileStoreServiceConnector, times(0)).addFile(any(InputStream.class));
    }

    @Test
    public void execute_dataFileReadFails_throws() throws IOException {
        Files.createFile(testFolder.resolve(transfileName));
        Files.createDirectory(testFolder.resolve("123456.file"));

        final String transfileData = "f=123456.file";
        CreateJobOperation createJobOperation = new CreateJobOperation(jobStoreServiceConnector, fileStoreServiceConnector, testFolder, transfileName, transfileData);
        try {
            createJobOperation.execute();
            Assertions.fail("No OperationExecutionException thrown");
        } catch (OperationExecutionException e) {
            assertThat(e.getCause() instanceof IOException, is(true));
        }
    }

    @Test
    public void execute_fileStoreServiceConnectorThrows_throws() throws FileStoreServiceConnectorException, IOException {
        Files.createFile(testFolder.resolve(transfileName));
        Files.createFile(testFolder.resolve("123456.file"));

        Exception exception = new FileStoreServiceConnectorException("DIED");
        when(fileStoreServiceConnector.addFile(any(InputStream.class))).thenThrow(exception);

        final String transfileData = "f=123456.file";
        CreateJobOperation createJobOperation = new CreateJobOperation(
                jobStoreServiceConnector, fileStoreServiceConnector, testFolder, transfileName, transfileData);
        try {
            createJobOperation.execute();
            Assertions.fail("No OperationExecutionException thrown");
        } catch (OperationExecutionException e) {
            assertThat((Exception) e.getCause(), is(exception));
        }
    }

    @Test
    public void execute_dataFileExists_uploadsToFileStore() throws IOException, OperationExecutionException, FileStoreServiceConnectorException {
        Files.createFile(testFolder.resolve(transfileName));
        Files.createFile(testFolder.resolve("123456.file"));

        final String transfileData = "f=123456.file";
        CreateJobOperation createJobOperation = new CreateJobOperation(
                jobStoreServiceConnector, fileStoreServiceConnector, testFolder, transfileName, transfileData);

        createJobOperation.execute();

        JobInputStream jobInputStream = jobStoreServiceConnector.jobInputStreams.remove();
        assertThat(jobInputStream.getJobSpecification().getDataFile(), is(FileStoreUrn.create(fileStoreId).toString()));

        verify(fileStoreServiceConnector, times(1)).addFile(any(InputStream.class));
    }

    @Test
    public void execute_jobStoreServiceConnectorThrows_removesFileFromFileStoreAndThrows() throws IOException, JobStoreServiceConnectorException, FileStoreServiceConnectorException {
        Files.createFile(testFolder.resolve(transfileName));
        Files.createFile(testFolder.resolve("123456.file"));

        Exception exception = new JobStoreServiceConnectorException("DIED");
        JobStoreServiceConnector jssc = mock(JobStoreServiceConnector.class);
        when(jssc.addJob(any(JobInputStream.class))).thenThrow(exception);

        final String transfileData = "f=123456.file";
        CreateJobOperation createJobOperation = new CreateJobOperation(
                jssc, fileStoreServiceConnector, testFolder, transfileName, transfileData);
        try {
            createJobOperation.execute();
            Assertions.fail("No OperationExecutionException thrown");
        } catch (OperationExecutionException e) {
            assertThat((Exception) e.getCause(), is(exception));
        }

        verify(fileStoreServiceConnector).deleteFile(fileStoreId);
    }

    @Test
    public void execute_jobStoreServiceConnectorThrowsWithInternalServerError_throws() throws IOException, JobStoreServiceConnectorException, FileStoreServiceConnectorException {
        Files.createFile(testFolder.resolve(transfileName));
        Files.createFile(testFolder.resolve("123456.file"));

        Exception exception = new JobStoreServiceConnectorUnexpectedStatusCodeException(
                "DIED", Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        JobStoreServiceConnector jssc = mock(JobStoreServiceConnector.class);
        when(jssc.addJob(any(JobInputStream.class))).thenThrow(exception);

        final String transfileData = "f=123456.file";
        CreateJobOperation createJobOperation = new CreateJobOperation(
                jssc, fileStoreServiceConnector, testFolder, transfileName, transfileData);
        try {
            createJobOperation.execute();
            Assertions.fail("No OperationExecutionException thrown");
        } catch (OperationExecutionException e) {
            assertThat((Exception) e.getCause(), is(exception));
        }

        verify(fileStoreServiceConnector, times(0)).deleteFile(fileStoreId);
    }

    @Test
    public void execute_transfileDoesNotExist_throws() {
        CreateJobOperation createJobOperation = new CreateJobOperation(
                jobStoreServiceConnector, fileStoreServiceConnector, testFolder, transfileName, "foo");

        assertThat(createJobOperation::execute, isThrowing(OperationExecutionException.class));
    }
}
