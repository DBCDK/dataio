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
import dk.dbc.dataio.commons.types.FileStoreUrn;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorUnexpectedStatusCodeException;
import dk.dbc.dataio.commons.utils.jobstore.MockedJobStoreServiceConnector;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnector;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnectorException;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.jobstore.types.JobInputStream;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

import static dk.dbc.commons.testutil.Assert.assertThat;
import static dk.dbc.commons.testutil.Assert.isThrowing;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
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

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    @Before
    public void setupMocks() throws FileStoreServiceConnectorException {
        when(fileStoreServiceConnector.addFile(any(InputStream.class))).thenReturn(fileStoreId);
        jobStoreServiceConnector.jobInfoSnapshots.clear();
        jobStoreServiceConnector.jobInfoSnapshots.add(new JobInfoSnapshot());
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
    public void constructor_allArgsAreValid_returnsNewInstance() throws IOException {
        final Path workingDir = testFolder.getRoot().toPath();

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
    public void execute_datafileIsUndefined_createsJobWithMissingDataFilePlaceHolder() throws OperationExecutionException, FileStoreServiceConnectorException, IOException {
        testFolder.newFile(transfileName);
        final String transfileData = "foo";
        final CreateJobOperation createJobOperation = new CreateJobOperation(
                jobStoreServiceConnector, fileStoreServiceConnector, testFolder.getRoot().toPath(), transfileName, transfileData);

        createJobOperation.execute();

        final JobInputStream jobInputStream = jobStoreServiceConnector.jobInputStreams.remove();
        assertThat(jobInputStream.getJobSpecification().getDataFile(), is(Constants.MISSING_FIELD_VALUE));

        verify(fileStoreServiceConnector, times(0)).addFile(any(InputStream.class));
    }

    @Test
    public void execute_datafileDoesNotExist_createsJobWithOriginalDataFileName() throws OperationExecutionException, FileStoreServiceConnectorException, IOException {
        testFolder.newFile(transfileName);
        final String transfileData = "f=123456.file";
        final CreateJobOperation createJobOperation = new CreateJobOperation(
                jobStoreServiceConnector, fileStoreServiceConnector, testFolder.getRoot().toPath(), transfileName, transfileData);

        createJobOperation.execute();

        final JobInputStream jobInputStream = jobStoreServiceConnector.jobInputStreams.remove();
        assertThat(jobInputStream.getJobSpecification().getAncestry().getDatafile(), is("123456.file"));
        assertThat(jobInputStream.getJobSpecification().getDataFile(), is(Constants.MISSING_FIELD_VALUE));

        verify(fileStoreServiceConnector, times(0)).addFile(any(InputStream.class));
    }

    @Test
    public void execute_dataFileReadFails_throws() throws OperationExecutionException, FileStoreServiceConnectorException, IOException {
        testFolder.newFile(transfileName);
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
        testFolder.newFile(transfileName);
        testFolder.newFile("123456.file");

        final Exception exception = new FileStoreServiceConnectorException("DIED");
        when(fileStoreServiceConnector.addFile(any(InputStream.class))).thenThrow(exception);

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
    public void execute_dataFileExists_uploadsToFileStore() throws IOException, OperationExecutionException, FileStoreServiceConnectorException {
        testFolder.newFile(transfileName);
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
    public void execute_jobStoreServiceConnectorThrows_removesFileFromFileStoreAndThrows() throws IOException, OperationExecutionException, JobStoreServiceConnectorException, FileStoreServiceConnectorException {
        testFolder.newFile(transfileName);
        testFolder.newFile("123456.file");

        final Exception exception = new JobStoreServiceConnectorException("DIED");
        final JobStoreServiceConnector jssc = mock(JobStoreServiceConnector.class);
        when(jssc.addJob(any(JobInputStream.class))).thenThrow(exception);

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
    public void execute_jobStoreServiceConnectorThrowsWithInternalServerError_throws() throws IOException, OperationExecutionException, JobStoreServiceConnectorException, FileStoreServiceConnectorException {
        testFolder.newFile(transfileName);
        testFolder.newFile("123456.file");

        final Exception exception = new JobStoreServiceConnectorUnexpectedStatusCodeException(
                "DIED", Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        final JobStoreServiceConnector jssc = mock(JobStoreServiceConnector.class);
        when(jssc.addJob(any(JobInputStream.class))).thenThrow(exception);

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

    @Test
    public void execute_transfileDoesNotExist_throws() throws OperationExecutionException {
        final CreateJobOperation createJobOperation = new CreateJobOperation(
                jobStoreServiceConnector, fileStoreServiceConnector, testFolder.getRoot().toPath(), transfileName, "foo");

        assertThat(createJobOperation::execute, isThrowing(OperationExecutionException.class));
    }
}
