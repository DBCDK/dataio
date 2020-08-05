/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnector;
import dk.dbc.dataio.filestore.service.connector.ejb.FileStoreServiceConnectorBean;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import dk.dbc.dataio.jobstore.types.State;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.Response;
import java.net.URISyntaxException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JobsExportsBeanTest {
    private JobsExportsBean jobsExportsBean;

    @Before
    public void setup() {
        jobsExportsBean = new JobsExportsBean();
        jobsExportsBean.jobStoreRepository = mock(PgJobStoreRepository.class);
        jobsExportsBean.fileStoreServiceConnectorBean = mock(FileStoreServiceConnectorBean.class);
        when(jobsExportsBean.fileStoreServiceConnectorBean.getConnector()).thenReturn(mock(FileStoreServiceConnector.class));
    }

    @Test
    public void exportItemsPartitioned() throws URISyntaxException, JobStoreException {
        final int jobId = 42;
        final String fileStoreUrl = "http://filestore/files/1";
        when(jobsExportsBean.jobStoreRepository.exportItemsToFileStore(
                jobId, State.Phase.PARTITIONING, jobsExportsBean.fileStoreServiceConnectorBean.getConnector()))
                .thenReturn(fileStoreUrl);

        final Response response = jobsExportsBean.exportItemsPartitioned(jobId);
        assertThat("Response status", response.getStatus(), is(Response.Status.SEE_OTHER.getStatusCode()));
        assertThat("Redirect location", response.getHeaderString("Location"), is(fileStoreUrl));
    }

    @Test
    public void exportItemsPartitioned_jobNotFound() throws URISyntaxException, JobStoreException {
        final int jobId = 42;
        when(jobsExportsBean.jobStoreRepository.exportItemsToFileStore(
                jobId, State.Phase.PARTITIONING, jobsExportsBean.fileStoreServiceConnectorBean.getConnector()))
                .thenThrow(new JobStoreException("job not found"));

        when(jobsExportsBean.jobStoreRepository.jobExists(jobId)).thenReturn(false);

        final Response response = jobsExportsBean.exportItemsPartitioned(jobId);
        assertThat("Response status", response.getStatus(), is(Response.Status.NO_CONTENT.getStatusCode()));
    }

    @Test(expected = JobStoreException.class)
    public void exportItemsPartitioned_internalServerError() throws URISyntaxException, JobStoreException {
        final int jobId = 42;
        when(jobsExportsBean.jobStoreRepository.exportItemsToFileStore(
                jobId, State.Phase.PARTITIONING, jobsExportsBean.fileStoreServiceConnectorBean.getConnector()))
                .thenThrow(new JobStoreException("died"));

        when(jobsExportsBean.jobStoreRepository.jobExists(jobId)).thenReturn(true);

        jobsExportsBean.exportItemsPartitioned(jobId);
    }

    @Test
    public void exportItemsProcessed() throws URISyntaxException, JobStoreException {
        final int jobId = 42;
        final String fileStoreUrl = "http://filestore/files/1";
        when(jobsExportsBean.jobStoreRepository.exportItemsToFileStore(
                jobId, State.Phase.PROCESSING, jobsExportsBean.fileStoreServiceConnectorBean.getConnector()))
                .thenReturn(fileStoreUrl);

        final Response response = jobsExportsBean.exportItemsProcessed(jobId);
        assertThat("Response status", response.getStatus(), is(Response.Status.SEE_OTHER.getStatusCode()));
        assertThat("Redirect location", response.getHeaderString("Location"), is(fileStoreUrl));
    }

    @Test
    public void exportItemsProcessed_jobNotFound() throws URISyntaxException, JobStoreException {
        final int jobId = 42;
        when(jobsExportsBean.jobStoreRepository.exportItemsToFileStore(
                jobId, State.Phase.PROCESSING, jobsExportsBean.fileStoreServiceConnectorBean.getConnector()))
                .thenThrow(new JobStoreException("job not found"));

        when(jobsExportsBean.jobStoreRepository.jobExists(jobId)).thenReturn(false);

        final Response response = jobsExportsBean.exportItemsProcessed(jobId);
        assertThat("Response status", response.getStatus(), is(Response.Status.NO_CONTENT.getStatusCode()));
    }

    @Test(expected = JobStoreException.class)
    public void exportItemsProcessed_internalServerError() throws URISyntaxException, JobStoreException {
        final int jobId = 42;
        when(jobsExportsBean.jobStoreRepository.exportItemsToFileStore(
                jobId, State.Phase.PROCESSING, jobsExportsBean.fileStoreServiceConnectorBean.getConnector()))
                .thenThrow(new JobStoreException("died"));

        when(jobsExportsBean.jobStoreRepository.jobExists(jobId)).thenReturn(true);

        jobsExportsBean.exportItemsProcessed(jobId);
    }

    @Test
    public void exportItemsDelivered() throws URISyntaxException, JobStoreException {
        final int jobId = 42;
        final String fileStoreUrl = "http://filestore/files/1";
        when(jobsExportsBean.jobStoreRepository.exportItemsToFileStore(
                jobId, State.Phase.DELIVERING, jobsExportsBean.fileStoreServiceConnectorBean.getConnector()))
                .thenReturn(fileStoreUrl);

        final Response response = jobsExportsBean.exportItemsDelivered(jobId);
        assertThat("Response status", response.getStatus(), is(Response.Status.SEE_OTHER.getStatusCode()));
        assertThat("Redirect location", response.getHeaderString("Location"), is(fileStoreUrl));
    }

    @Test
    public void exportItemsDelivered_jobNotFound() throws URISyntaxException, JobStoreException {
        final int jobId = 42;
        when(jobsExportsBean.jobStoreRepository.exportItemsToFileStore(
                jobId, State.Phase.DELIVERING, jobsExportsBean.fileStoreServiceConnectorBean.getConnector()))
                .thenThrow(new JobStoreException("job not found"));

        when(jobsExportsBean.jobStoreRepository.jobExists(jobId)).thenReturn(false);

        final Response response = jobsExportsBean.exportItemsDelivered(jobId);
        assertThat("Response status", response.getStatus(), is(Response.Status.NO_CONTENT.getStatusCode()));
    }

    @Test(expected = JobStoreException.class)
    public void exportItemsDelivered_internalServerError() throws URISyntaxException, JobStoreException {
        final int jobId = 42;
        when(jobsExportsBean.jobStoreRepository.exportItemsToFileStore(
                jobId, State.Phase.DELIVERING, jobsExportsBean.fileStoreServiceConnectorBean.getConnector()))
                .thenThrow(new JobStoreException("died"));

        when(jobsExportsBean.jobStoreRepository.jobExists(jobId)).thenReturn(true);

        jobsExportsBean.exportItemsDelivered(jobId);
    }
}