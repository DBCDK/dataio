package dk.dbc.dataio.jobstore.ejb;

import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.FlowStoreServiceEntryPoint;
import dk.dbc.dataio.commons.types.exceptions.ReferencedEntityNotFoundException;
import dk.dbc.dataio.commons.types.json.mixins.MixIns;
import dk.dbc.dataio.commons.utils.httpclient.HttpClient;
import dk.dbc.dataio.commons.utils.json.JsonException;
import dk.dbc.dataio.commons.utils.json.JsonUtil;
import dk.dbc.dataio.commons.utils.service.ServiceUtil;
import dk.dbc.dataio.integrationtest.ITUtil;
import dk.dbc.dataio.jobstore.types.Job;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.ejb.EJBException;
import javax.naming.NamingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.lang.annotation.Annotation;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

/**
  * JobsBean unit tests
  * <p>
  * The test methods of this class uses the following naming convention:
  *
  *  unitOfWork_stateUnderTest_expectedBehavior
  */
@RunWith(PowerMockRunner.class)
@PrepareForTest({
        HttpClient.class,
        ServiceUtil.class,
        dk.dbc.dataio.jobstore.JobStoreBean.class,
        UriInfo.class
})
public class JobsBeanTest {
    private final String flowStoreUrl = "http://dataio/flow-store";
    private final UriInfo uriInfo = mock(UriInfo.class);
    private final dk.dbc.dataio.jobstore.JobStoreBean jobStore = mock(dk.dbc.dataio.jobstore.JobStoreBean.class);

    @Before
    public void setup() throws Exception {
        mockStatic(ServiceUtil.class);
        mockStatic(HttpClient.class);
        when(ServiceUtil.getFlowStoreServiceEndpoint())
                .thenReturn(flowStoreUrl);
    }

    @Test(expected = NullPointerException.class)
    public void createJob_jobSpecDataIsNull_throws() throws Exception {
        final JobsBean jobsBean = new JobsBean();
        jobsBean.createJob(uriInfo, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void createJob_jobSpecDataIsEmpty_throws() throws Exception {
        final JobsBean jobsBean = new JobsBean();
        jobsBean.createJob(uriInfo, "");
    }

    @Test(expected = JsonException.class)
    public void createJob_jobSpecDataIsInvalidJson_throws() throws Exception {
        final JobsBean jobsBean = new JobsBean();
        jobsBean.createJob(uriInfo, "{");
    }

    @Test(expected = JsonException.class)
    public void createJob_jobSpecDataIsInvalidJobSpecification_throws() throws Exception {
        final JobsBean jobsBean = new JobsBean();
        jobsBean.createJob(uriInfo, "{\"name\": \"name\"}");
    }

    @Test(expected = EJBException.class)
    public void createJob_jndiLookupThrowsNamingException_throws() throws Exception {
        final String jobSpecData = new ITUtil.JobSpecificationJsonBuilder().build();
        when(ServiceUtil.getFlowStoreServiceEndpoint())
                .thenThrow(new NamingException());

        final JobsBean jobsBean = new JobsBean();
        jobsBean.createJob(uriInfo, jobSpecData);
    }

    @Test(expected = ReferencedEntityNotFoundException.class)
    public void createJob_noFlowCanBeFound_throws() throws Exception {
        final long flowId = 42L;
        final String jobSpecData = new ITUtil.JobSpecificationJsonBuilder()
                .setFlowId(flowId)
                .build();

        when(HttpClient.doGet(any(Client.class), eq(flowStoreUrl), eq(FlowStoreServiceEntryPoint.FLOWS), eq(Long.toString(flowId))))
                .thenReturn(new MockedResponse<>(Response.Status.NOT_FOUND.getStatusCode(), ""));

        final JobsBean jobsBean = new JobsBean();
        jobsBean.createJob(uriInfo, jobSpecData);
    }

    @Test(expected = EJBException.class)
    public void createJob_jobStoreThrowsJobStoreException_throwsEJBException() throws Exception {
        final long flowId = 42L;
        final String flowData = new ITUtil.FlowJsonBuilder()
                .setId(flowId)
                .build();
        final String jobSpecData = new ITUtil.JobSpecificationJsonBuilder()
                .setFlowId(flowId)
                .build();
        final Job job = new Job(1, Paths.get("file"), JsonUtil.fromJson(flowData, Flow.class, MixIns.getMixIns()));

        when(HttpClient.doGet(any(Client.class), eq(flowStoreUrl), eq(FlowStoreServiceEntryPoint.FLOWS), eq(Long.toString(flowId))))
                .thenReturn(new MockedResponse<>(Response.Status.OK.getStatusCode(), flowData));
        when(jobStore.createJob(any(Path.class), any(Flow.class)))
                .thenThrow(new JobStoreException("die"));

        final JobsBean jobsBean = new JobsBean();
        jobsBean.jobStore = jobStore;
        jobsBean.createJob(uriInfo, jobSpecData);
    }

    @Test
    public void createJob_jobIsCreated_returnsStatusCreatedResponse() throws Exception {
        final long flowId = 42L;
        final String flowData = new ITUtil.FlowJsonBuilder()
                .setId(flowId)
                .build();
        final String jobSpecData = new ITUtil.JobSpecificationJsonBuilder()
                .setFlowId(flowId)
                .build();
        final Job job = new Job(1, Paths.get("file"), JsonUtil.fromJson(flowData, Flow.class, MixIns.getMixIns()));

        when(HttpClient.doGet(any(Client.class), eq(flowStoreUrl), eq(FlowStoreServiceEntryPoint.FLOWS), eq(Long.toString(flowId))))
                .thenReturn(new MockedResponse<>(Response.Status.OK.getStatusCode(), flowData));
        when(jobStore.createJob(any(Path.class), any(Flow.class)))
                .thenReturn(job);

        final JobsBean jobsBean = new JobsBean();
        jobsBean.jobStore = jobStore;
        final Response response = jobsBean.createJob(uriInfo, jobSpecData);
        assertThat(response.getStatus(), is(Response.Status.CREATED.getStatusCode()));
    }

    class MockedResponse<T> extends Response {
        private final int mockedStatus;
        private final T mockedEntity;

        public MockedResponse(int status, T entity) {
            mockedStatus = status;
            mockedEntity = entity;
        }

        @Override
        public int getStatus() {
            return mockedStatus;
        }

        @Override
        public StatusType getStatusInfo() {
            return null;
        }

        @Override
        public Object getEntity() {
            return null;
        }

        @Override
        public <T> T readEntity(Class<T> tClass) {
            return (T) mockedEntity;
        }

        @Override
        public <T> T readEntity(GenericType<T> tGenericType) {
            return null;
        }

        @Override
        public <T> T readEntity(Class<T> tClass, Annotation[] annotations) {
            return null;
        }

        @Override
        public <T> T readEntity(GenericType<T> tGenericType, Annotation[] annotations) {
            return null;
        }

        @Override
        public boolean hasEntity() {
            return false;
        }

        @Override
        public boolean bufferEntity() {
            return false;
        }

        @Override
        public void close() {
        }

        @Override
        public MediaType getMediaType() {
            return null;
        }

        @Override
        public Locale getLanguage() {
            return null;
        }

        @Override
        public int getLength() {
            return 0;
        }

        @Override
        public Set<String> getAllowedMethods() {
            return null;
        }

        @Override
        public Map<String, NewCookie> getCookies() {
            return null;
        }

        @Override
        public EntityTag getEntityTag() {
            return null;
        }

        @Override
        public Date getDate() {
            return null;
        }

        @Override
        public Date getLastModified() {
            return null;
        }

        @Override
        public URI getLocation() {
            return null;
        }

        @Override
        public Set<Link> getLinks() {
            return null;
        }

        @Override
        public boolean hasLink(String s) {
            return false;
        }

        @Override
        public Link getLink(String s) {
            return null;
        }

        @Override
        public Link.Builder getLinkBuilder(String s) {
            return null;
        }

        @Override
        public MultivaluedMap<String, Object> getMetadata() {
            return null;
        }

        @Override
        public MultivaluedMap<String, String> getStringHeaders() {
            return null;
        }

        @Override
        public String getHeaderString(String s) {
            return null;
        }
    }
}
