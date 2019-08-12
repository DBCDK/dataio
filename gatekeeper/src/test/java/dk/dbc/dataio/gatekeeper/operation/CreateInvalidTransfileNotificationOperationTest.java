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

package dk.dbc.dataio.gatekeeper.operation;

import dk.dbc.dataio.commons.types.Constants;
import dk.dbc.httpclient.HttpClient;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorUnexpectedStatusCodeException;
import dk.dbc.dataio.jobstore.types.AddNotificationRequest;
import dk.dbc.dataio.jobstore.types.InvalidTransfileNotificationContext;
import dk.dbc.dataio.jobstore.types.Notification;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import javax.ws.rs.core.Response;
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
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings("Duplicates")
public class CreateInvalidTransfileNotificationOperationTest {
    private final MockedJobStoreServiceConnector jobStoreServiceConnector = new MockedJobStoreServiceConnector();
    private final Path workingDir = Paths.get("wd");
    private final String transfileName = "123456.001.trans";
    private final String transfileCauseForInvalidation = "cause";

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    @Before
    public void setupMocks() {
        jobStoreServiceConnector.addNotificationRequests.clear();
    }

    @Test(expected = NullPointerException.class)
    public void constructor_jobStoreServiceConnectorArgIsNull_throws() {
        new CreateInvalidTransfileNotificationOperation(null, workingDir, transfileName, transfileCauseForInvalidation);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_workingDirArgIsNull_throws() {
        new CreateInvalidTransfileNotificationOperation(jobStoreServiceConnector, null, transfileName, transfileCauseForInvalidation);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_transfileNameArgIsNull_throws() {
        new CreateInvalidTransfileNotificationOperation(jobStoreServiceConnector, workingDir, null, transfileCauseForInvalidation);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_transfileNameArgIsEmpty_throws() {
        new CreateInvalidTransfileNotificationOperation(jobStoreServiceConnector, workingDir, " ", transfileCauseForInvalidation);
    }

    @Test
    public void constructor_allArgsAreValid_returnsNewInstance() {
        final CreateInvalidTransfileNotificationOperation operation = new CreateInvalidTransfileNotificationOperation(
                jobStoreServiceConnector, workingDir, transfileName, transfileCauseForInvalidation);
        assertThat("instance", operation, is(notNullValue()));
        assertThat("getJobStoreServiceConnector()", operation.getJobStoreServiceConnector(), is((JobStoreServiceConnector) jobStoreServiceConnector));
        assertThat("getWorkingDir()", operation.getWorkingDir(), is(workingDir));
        assertThat("getTransfileName()", operation.getTransfileName(), is(transfileName));
        assertThat("getTransfileName()", operation.getCauseForInvalidation(), is(transfileCauseForInvalidation));
    }

    @Test
    public void execute_jobStoreServiceConnectorThrows_throws() throws JobStoreServiceConnectorException, IOException, OperationExecutionException {
        final Exception exception = new JobStoreServiceConnectorUnexpectedStatusCodeException(
                "DIED", Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        final JobStoreServiceConnector jobStoreServiceConnector = mock(JobStoreServiceConnector.class);
        when(jobStoreServiceConnector.addNotification(any(AddNotificationRequest.class))).thenThrow(exception);

        final CreateInvalidTransfileNotificationOperation operation = new CreateInvalidTransfileNotificationOperation(
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
        final CreateInvalidTransfileNotificationOperation operation = new CreateInvalidTransfileNotificationOperation(
                jobStoreServiceConnector, workingDir, transfileName, transfileCauseForInvalidation);
        operation.execute();

        assertThat("Number of requests created", jobStoreServiceConnector.addNotificationRequests.size(), is(1));
        final AddNotificationRequest request = jobStoreServiceConnector.addNotificationRequests.remove();
        assertThat("Notification destination", request.getDestinationEmail(), is(Constants.MISSING_FIELD_VALUE));
        assertThat("Notification type", request.getNotificationType(), is(Notification.Type.INVALID_TRANSFILE));
        final InvalidTransfileNotificationContext context = (InvalidTransfileNotificationContext) request.getContext();
        assertThat("Notification context", context, is(notNullValue()));
        assertThat("Context transfile name", context.getTransfileName(), is(transfileName));
        assertThat("Context transfile invalidation cause", context.getCause(), is(transfileCauseForInvalidation));
    }

    @Test
    public void execute_transfileHasSmallLetterM_issuesRequest() throws OperationExecutionException, IOException {
        final String destination = "example@compny.com";
        final Path transfile = testFolder.newFile(transfileName).toPath();
        appendToFile(transfile, "f=test.dat,m=" + System.lineSeparator());
        appendToFile(transfile, "m=" + destination);

        final CreateInvalidTransfileNotificationOperation operation = new CreateInvalidTransfileNotificationOperation(
                jobStoreServiceConnector, testFolder.getRoot().toPath(), transfileName, transfileCauseForInvalidation);
        operation.execute();

        assertThat("Number of requests created", jobStoreServiceConnector.addNotificationRequests.size(), is(1));
        final AddNotificationRequest request = jobStoreServiceConnector.addNotificationRequests.remove();
        assertThat("Notification destination", request.getDestinationEmail(), is(destination));
    }

    @Test
    public void execute_transfileHasOnlyCapitalLetterM_issuesRequest() throws OperationExecutionException, IOException {
        final String destination = "example@compny.com";
        final Path transfile = testFolder.newFile(transfileName).toPath();
        appendToFile(transfile, "f=test.dat,m=" + System.lineSeparator());
        appendToFile(transfile, "M=" + destination);

        final CreateInvalidTransfileNotificationOperation operation = new CreateInvalidTransfileNotificationOperation(
                jobStoreServiceConnector, testFolder.getRoot().toPath(), transfileName, transfileCauseForInvalidation);
        operation.execute();

        assertThat("Number of requests created", jobStoreServiceConnector.addNotificationRequests.size(), is(1));
        final AddNotificationRequest request = jobStoreServiceConnector.addNotificationRequests.remove();
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