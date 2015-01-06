package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.utils.test.model.JobSpecificationBuilder;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.jobstore.types.JobInputStream;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import dk.dbc.dataio.jsonb.JSONBContext;
import dk.dbc.dataio.jsonb.ejb.JSONBBean;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.net.URI;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JobsBeanTest {
    private final static String LOCATION = "helloWorld";
    private final static int PART_NUMBER = 2535678;
    private UriInfo mockedUriInfo;
    private UriBuilder mockedUriBuilder;
    private JobsBean jobsBean;
    private JSONBContext jsonbContext;


    @Before
    public void setup() {
        initializeJobsBean();
        jsonbContext = new JSONBContext();

        mockedUriInfo = mock(UriInfo.class);
        mockedUriBuilder = mock(UriBuilder.class);
        jobsBean.jobStoreBean = mock(JobStoreBean.class);

        when(mockedUriInfo.getAbsolutePathBuilder()).thenReturn(mockedUriBuilder);
        when(mockedUriBuilder.path(anyString())).thenReturn(mockedUriBuilder);
    }

    @Test(expected = JobStoreException.class)
    public void addJob_addAndScheduleJobFailure_throwsJobStoreException() throws Exception{
        final URI uri = new URI(LOCATION);
        final JobSpecification jobSpecification = new JobSpecificationBuilder().build();
        final JobInputStream jobInputStream = new JobInputStream(jobSpecification, false, PART_NUMBER);
        final String jobInputStreamJson = jsonbContext.marshall(jobInputStream);

        when(mockedUriBuilder.build()).thenReturn(uri);
        when(jobsBean.jobStoreBean.addAndScheduleJob(any(JobInputStream.class))).thenThrow(new JobStoreException("Error"));

        jobsBean.addJob(mockedUriInfo, jobInputStreamJson);
    }

    @Test
    public void addJob_marshallingFailure_returnsResponseWithHttpStatusBadRequest() throws Exception{
        final String jobInputStreamJsonString = jsonbContext.marshall("invalid JSON");
        final Response response = jobsBean.addJob(mockedUriInfo, jobInputStreamJsonString);

        assertThat(response.hasEntity(), is(true));
        assertThat(response.getStatusInfo().getStatusCode(), is(Response.Status.BAD_REQUEST.getStatusCode()));
    }

    @Test
    public void addJob_returnsResponseWithHttpStatusCreated_returnsJobOverview() throws Exception {
        final URI uri = new URI(LOCATION);
        final JobSpecification jobSpecification = new JobSpecificationBuilder().build();
        final JobInputStream jobInputStream = new JobInputStream(jobSpecification, false, PART_NUMBER);
        final String jobInputStreamJson = jsonbContext.marshall(jobInputStream);
        final JobInfoSnapshot jobInfoSnapshot = getJobOverview();

        when(mockedUriBuilder.build()).thenReturn(uri);
        when(jobsBean.jobStoreBean.addAndScheduleJob(any(JobInputStream.class))).thenReturn(jobInfoSnapshot);

        final Response response = jobsBean.addJob(mockedUriInfo, jobInputStreamJson);

        assertThat(response.getStatus(), is(Response.Status.CREATED.getStatusCode()));
        assertThat(response.getLocation().toString(), is(LOCATION));
        assertThat(response.hasEntity(), is(true));

        JobInfoSnapshot returnedJobInfoSnapshot = jsonbContext.unmarshall((String) response.getEntity(), JobInfoSnapshot.class);
        assertThat(returnedJobInfoSnapshot, not(nullValue()));
        assertJobSpecificationEquals(returnedJobInfoSnapshot.getSpecification(), jobSpecification);
    }

    /*
     Private methods
    */

    private void assertJobSpecificationEquals(JobSpecification jobSpecification1, JobSpecification jobSpecification2) {
        assertThat(jobSpecification1.getFormat(), is(jobSpecification2.getFormat()));
        assertThat(jobSpecification1.getCharset(), is(jobSpecification2.getCharset()));
        assertThat(jobSpecification1.getDataFile(), is(jobSpecification2.getDataFile()));
        assertThat(jobSpecification1.getDestination(), is(jobSpecification2.getDestination()));
        assertThat(jobSpecification1.getMailForNotificationAboutProcessing(), is(jobSpecification2.getMailForNotificationAboutProcessing()));
        assertThat(jobSpecification1.getMailForNotificationAboutVerification(), is(jobSpecification2.getMailForNotificationAboutVerification()));
        assertThat(jobSpecification1.getPackaging(), is(jobSpecification2.getPackaging()));
        assertThat(jobSpecification1.getResultmailInitials(), is(jobSpecification2.getResultmailInitials()));
        assertThat(jobSpecification1.getSubmitterId(), is(jobSpecification2.getSubmitterId()));
    }

    private void initializeJobsBean() {
        jobsBean = new JobsBean();
        jobsBean.jsonbBean = new JSONBBean();
        jobsBean.jsonbBean.initialiseContext();
    }

    private JobInfoSnapshot getJobOverview() {
        JobInfoSnapshot jobOverview = new JobInfoSnapshot();
        jobOverview.setSinkName("sinkName");
        jobOverview.setFlowName("flowName");
        jobOverview.setSpecification(new JobSpecificationBuilder().build());
        return jobOverview;
    }

}
