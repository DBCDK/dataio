package dk.dbc.dataio.jobstore.service;

import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.utils.test.model.JobSpecificationBuilder;
import dk.dbc.dataio.jobstore.service.ejb.JobsBean;
import dk.dbc.dataio.jobstore.types.JobInputStream;
import dk.dbc.dataio.jsonb.JSONBContext;
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
    private final static long PART_NUMBER = 2535678;
    private static UriInfo mockedUriInfo;
    private static UriBuilder mockedUriBuilder;

    private static JSONBContext jsonbContext;
    private static JobsBean jobsBean;

    @Before
    public void setup() {
        jsonbContext = new JSONBContext();
        jobsBean = new JobsBean();
        mockedUriInfo = mock(UriInfo.class);
        mockedUriBuilder = mock(UriBuilder.class);
    }

    @Test
    public void jobsBean_validConstructor_newInstance() throws Exception {
        URI uri = new URI(LOCATION);
        JobSpecification jobSpecification = new JobSpecificationBuilder().build();
        JobInputStream jobInputStream = new JobInputStream(jobSpecification, false, PART_NUMBER);
        String jobInputStreamData = jsonbContext.marshall(jobInputStream);
        when(mockedUriInfo.getAbsolutePathBuilder()).thenReturn(mockedUriBuilder);
        when(mockedUriBuilder.path(anyString())).thenReturn(mockedUriBuilder);
        when(mockedUriBuilder.build()).thenReturn(uri);

        final Response response = jobsBean.addJob(mockedUriInfo, jobInputStreamData);
        assertThat(response.getStatus(), is(Response.Status.CREATED.getStatusCode()));
        assertThat(response.hasEntity(), is(true));
        assertThat(response.getLocation(), is(uri));
        assertThat(response.getLocation().toString(), is(LOCATION));
    }

}
