package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.utils.test.model.JobSpecificationBuilder;
import dk.dbc.dataio.jobstore.types.JobInputStream;
import dk.dbc.dataio.jsonb.JSONBContext;
import dk.dbc.dataio.jsonb.ejb.JSONBBean;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.net.URI;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JobsBeanTest {
    private final static String LOCATION = "helloWorld";
    private final static int PART_NUMBER = 2535678;
    private static UriInfo mockedUriInfo;
    private static UriBuilder mockedUriBuilder;
    private static JSONBContext jsonbContext;
    private static JobsBean jobsBean;

    @Before
    public void setup() {
        jsonbContext = new JSONBContext();
        jobsBean = newJobsBean();
        mockedUriInfo = mock(UriInfo.class);
        mockedUriBuilder = mock(UriBuilder.class);
    }

    @Test
    public void addJob_returnsResponseWithHttpStatusCreated_returnsJobInputStreamEntity() throws Exception {
        URI uri = new URI(LOCATION);
        JobSpecification jobSpecification = new JobSpecificationBuilder().build();
        JobInputStream jobInputStream = new JobInputStream(jobSpecification, false, PART_NUMBER);

        when(mockedUriInfo.getAbsolutePathBuilder()).thenReturn(mockedUriBuilder);
        when(mockedUriBuilder.path(anyString())).thenReturn(mockedUriBuilder);
        when(mockedUriBuilder.build()).thenReturn(uri);

        final Response response = jobsBean.addJob(mockedUriInfo, jsonbContext.marshall(jobInputStream));

        assertThat(response.getStatus(), is(Response.Status.CREATED.getStatusCode()));
        assertThat(response.getLocation().toString(), is(LOCATION));

        assertThat(response.hasEntity(), is(true));
        JobInputStream returnedJobInputStreamEntity
                = jsonbContext.unmarshall((String)response.getEntity(), JobInputStream.class);

        assertJobInputStreamEquals(returnedJobInputStreamEntity, jobInputStream);
    }

    /*
     Private methods
    */

    private JobsBean newJobsBean() {
        final JSONBBean jsonbBean = new JSONBBean();
        jsonbBean.initialiseContext();

        final JobsBean jobsBean = new JobsBean();
        jobsBean.jsonbBean = jsonbBean;

        return jobsBean;
    }

    private void assertJobInputStreamEquals(JobInputStream jobInputStream1, JobInputStream jobInputStream2) {
        assertThat(jobInputStream1.getIsEndOfJob(), is(jobInputStream2.getIsEndOfJob()));
        assertThat(jobInputStream1.getPartNumber(), is(jobInputStream2.getPartNumber()));
        assertJobSpecificationEquals(jobInputStream1.getJobSpecification(), jobInputStream2.getJobSpecification());
    }

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

}
