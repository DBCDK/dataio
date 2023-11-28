package dk.dbc.dataio.gatekeeper.operation;

import dk.dbc.dataio.commons.types.Constants;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorUnexpectedStatusCodeException;
import dk.dbc.dataio.jobstore.types.AddNotificationRequest;
import dk.dbc.dataio.jobstore.types.InvalidTransfileNotificationContext;
import dk.dbc.dataio.jobstore.types.Notification;
import dk.dbc.httpclient.HttpClient;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.LinkedList;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings("Duplicates")
public class CreateInvalidTransfileNotificationOperationTest {
    private final MockedJobStoreServiceConnector jobStoreServiceConnector = new MockedJobStoreServiceConnector();
    private final Path workingDir = Paths.get("wd");
    private final String transfileName = "123456.001.trans";
    private final String transfileCauseForInvalidation = "cause";

    @TempDir
    public Path testFolder;

    @BeforeEach
    public void setupMocks() {
        jobStoreServiceConnector.addNotificationRequests.clear();
    }

    @Test
    public void constructor_jobStoreServiceConnectorArgIsNull_throws() {
        assertThrows(NullPointerException.class, () -> new CreateInvalidTransfileNotificationOperation(null, workingDir, transfileName, transfileCauseForInvalidation));
    }

    @Test
    public void constructor_workingDirArgIsNull_throws() {
        assertThrows(NullPointerException.class, () -> new CreateInvalidTransfileNotificationOperation(jobStoreServiceConnector, null, transfileName, transfileCauseForInvalidation));
    }

    @Test
    public void constructor_transfileNameArgIsNull_throws() {
        assertThrows(NullPointerException.class, () -> new CreateInvalidTransfileNotificationOperation(jobStoreServiceConnector, workingDir, null, transfileCauseForInvalidation));
    }

    @Test
    public void constructor_transfileNameArgIsEmpty_throws() {
        assertThrows(IllegalArgumentException.class, () -> new CreateInvalidTransfileNotificationOperation(jobStoreServiceConnector, workingDir, " ", transfileCauseForInvalidation));
    }

    @Test
    public void constructor_allArgsAreValid_returnsNewInstance() {
        CreateInvalidTransfileNotificationOperation operation = new CreateInvalidTransfileNotificationOperation(
                jobStoreServiceConnector, workingDir, transfileName, transfileCauseForInvalidation);
        assertThat("instance", operation, is(notNullValue()));
        assertThat("getJobStoreServiceConnector()", operation.getJobStoreServiceConnector(), is(jobStoreServiceConnector));
        assertThat("getWorkingDir()", operation.getWorkingDir(), is(workingDir));
        assertThat("getTransfileName()", operation.getTransfileName(), is(transfileName));
        assertThat("getTransfileName()", operation.getCauseForInvalidation(), is(transfileCauseForInvalidation));
    }

    @Test
    public void execute_jobStoreServiceConnectorThrows_throws() throws JobStoreServiceConnectorException, IOException, OperationExecutionException {
        Exception exception = new JobStoreServiceConnectorUnexpectedStatusCodeException(
                "DIED", Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        JobStoreServiceConnector jobStoreServiceConnector = mock(JobStoreServiceConnector.class);
        when(jobStoreServiceConnector.addNotification(any(AddNotificationRequest.class))).thenThrow(exception);

        CreateInvalidTransfileNotificationOperation operation = new CreateInvalidTransfileNotificationOperation(
                jobStoreServiceConnector, workingDir, transfileName, transfileCauseForInvalidation);
        try {
            operation.execute();
            fail("No OperationExecutionException thrown");
        } catch (OperationExecutionException e) {
            assertThat(e.getCause(), is(exception));
        }
    }

    @Test
    public void execute_noTransfileContent_issuesRequest() throws OperationExecutionException {
        CreateInvalidTransfileNotificationOperation operation = new CreateInvalidTransfileNotificationOperation(
                jobStoreServiceConnector, workingDir, transfileName, transfileCauseForInvalidation);
        operation.execute();

        assertThat("Number of requests created", jobStoreServiceConnector.addNotificationRequests.size(), is(1));
        AddNotificationRequest request = jobStoreServiceConnector.addNotificationRequests.remove();
        assertThat("Notification destination", request.getDestinationEmail(), is(Constants.MISSING_FIELD_VALUE));
        assertThat("Notification type", request.getNotificationType(), is(Notification.Type.INVALID_TRANSFILE));
        InvalidTransfileNotificationContext context = (InvalidTransfileNotificationContext) request.getContext();
        assertThat("Notification context", context, is(notNullValue()));
        assertThat("Context transfile name", context.getTransfileName(), is(transfileName));
        assertThat("Context transfile invalidation cause", context.getCause(), is(transfileCauseForInvalidation));
    }

    @Test
    public void execute_transfileHasSmallLetterM_issuesRequest() throws OperationExecutionException, IOException {
        final String destination = "example@compny.com";
        Path transfile = Files.createFile(testFolder.resolve(transfileName));
        appendToFile(transfile, "f=test.dat,m=" + System.lineSeparator());
        appendToFile(transfile, "m=" + destination);

        CreateInvalidTransfileNotificationOperation operation = new CreateInvalidTransfileNotificationOperation(jobStoreServiceConnector, testFolder, transfileName, transfileCauseForInvalidation);
        operation.execute();

        assertThat("Number of requests created", jobStoreServiceConnector.addNotificationRequests.size(), is(1));
        AddNotificationRequest request = jobStoreServiceConnector.addNotificationRequests.remove();
        assertThat("Notification destination", request.getDestinationEmail(), is(destination));
    }

    @Test
    public void execute_transfileHasOnlyCapitalLetterM_issuesRequest() throws OperationExecutionException, IOException {
        final String destination = "example@compny.com";
        Path transfile = Files.createFile(testFolder.resolve(transfileName));
        appendToFile(transfile, "f=test.dat,m=" + System.lineSeparator());
        appendToFile(transfile, "M=" + destination);

        CreateInvalidTransfileNotificationOperation operation = new CreateInvalidTransfileNotificationOperation(
                jobStoreServiceConnector, testFolder, transfileName, transfileCauseForInvalidation);
        operation.execute();

        assertThat("Number of requests created", jobStoreServiceConnector.addNotificationRequests.size(), is(1));
        AddNotificationRequest request = jobStoreServiceConnector.addNotificationRequests.remove();
        assertThat("Notification destination", request.getDestinationEmail(), is(destination));
    }

    private Path appendToFile(Path file, String content) {
        try {
            return Files.write(file, content.getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static class MockedJobStoreServiceConnector extends JobStoreServiceConnector {
        public final LinkedList<AddNotificationRequest> addNotificationRequests;

        public MockedJobStoreServiceConnector() {
            super(HttpClient.newClient(), "baseurl");
            addNotificationRequests = new LinkedList<>();
        }

        @Override
        public Notification addNotification(AddNotificationRequest request) {
            addNotificationRequests.add(request);
            return null;
        }
    }
}
