package dk.dbc.dataio.flowstore.ejb;

import com.fasterxml.jackson.databind.JsonNode;
import dk.dbc.dataio.commons.types.json.mixins.MixIns;
import dk.dbc.dataio.commons.utils.json.JsonException;
import dk.dbc.dataio.commons.utils.json.JsonUtil;
import dk.dbc.dataio.commons.utils.test.json.FlowBinderContentJsonBuilder;
import dk.dbc.dataio.commons.utils.test.json.FlowBinderJsonBuilder;
import dk.dbc.dataio.commons.utils.test.json.FlowContentJsonBuilder;
import dk.dbc.dataio.commons.utils.test.json.SinkContentJsonBuilder;
import dk.dbc.dataio.commons.utils.test.json.SubmitterContentJsonBuilder;
import dk.dbc.dataio.flowstore.entity.Flow;
import dk.dbc.dataio.flowstore.entity.FlowBinder;
import dk.dbc.dataio.flowstore.entity.Sink;
import dk.dbc.dataio.flowstore.entity.Submitter;
import org.junit.Test;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

public class FlowBindersBeanTest {

    @Test(expected = NullPointerException.class)
    public void getFlow_nullPackagingParameter_throws() throws JsonException {
        FlowBindersBean fbb = new FlowBindersBean();
        fbb.getFlowBinder(null, "nmalbum", "utf8", 654321L, "someDestination");
    }

    @Test(expected = IllegalArgumentException.class)
    public void getFlow_emptyPackagingParameter_throws() throws JsonException {
        FlowBindersBean fbb = new FlowBindersBean();
        fbb.getFlowBinder("", "nmalbum", "utf8", 654321L, "someDestination");
    }

    @Test(expected = NullPointerException.class)
    public void getFlow_nullFormatParameter_throws() throws JsonException {
        FlowBindersBean fbb = new FlowBindersBean();
        fbb.getFlowBinder("xml", null, "utf8", 654321L, "someDestination");
    }

    @Test(expected = IllegalArgumentException.class)
    public void getFlow_emptyFormatParameter_throws() throws JsonException {
        FlowBindersBean fbb = new FlowBindersBean();
        fbb.getFlowBinder("xml", "", "utf8", 654321L, "someDestination");
    }

    @Test(expected = NullPointerException.class)
    public void getFlow_nullCharsetParameter_throws() throws JsonException {
        FlowBindersBean fbb = new FlowBindersBean();
        fbb.getFlowBinder("xml", "nmalbum", null, 654321L, "someDestination");
    }

    @Test(expected = IllegalArgumentException.class)
    public void getFlow_emptyCharsetParameter_throws() throws JsonException {
        FlowBindersBean fbb = new FlowBindersBean();
        fbb.getFlowBinder("xml", "nmalbum", "", 654321L, "someDestination");
    }

    @Test(expected = NullPointerException.class)
    public void getFlow_nullSubmitterParameter_throws() throws JsonException {
        FlowBindersBean fbb = new FlowBindersBean();
        fbb.getFlowBinder("xml", "nmalbum", "utf8", null, "someDestination");
    }

    @Test(expected = NullPointerException.class)
    public void getFlow_nullDestinationParameter_throws() throws JsonException {
        FlowBindersBean fbb = new FlowBindersBean();
        fbb.getFlowBinder("xml", "nmalbum", "utf8", 654321L, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void getFlow_emptyDestinationParameter_throws() throws JsonException {
        FlowBindersBean fbb = new FlowBindersBean();
        fbb.getFlowBinder("xml", "nmalbum", "utf8", 654321L, "");
    }

    @Test
    public void getFlow_validParametersButNoMatchingFlow_returnsResponseWithFlowAndHTTP404() throws JsonException {
        FlowBindersBean fbb = new FlowBindersBean();
        EntityManager entityManager = mock(EntityManager.class);
        fbb.entityManager = entityManager;
        Query query = mock(Query.class);
        when(entityManager.createNamedQuery(FlowBinder.QUERY_FIND_FLOWBINDER)).thenReturn(query);

        when(query.getResultList()).thenReturn(Arrays.asList());
        Response response = fbb.getFlowBinder("xml", "nmalbum", "utf8", 654321L, "someDestination");
        assertThat(response.getStatus(), is(404));
    }

    @Test
    public void getFlow_validParametersAndMatchingFlow_returnsResponseWithFlowAndHTTP200() throws JsonException {
        FlowBindersBean fbb = new FlowBindersBean();
        EntityManager entityManager = mock(EntityManager.class);
        fbb.entityManager = entityManager;
        Query query = mock(Query.class);
        when(entityManager.createNamedQuery(FlowBinder.QUERY_FIND_FLOWBINDER)).thenReturn(query);

        String flowBinderStr = new FlowBinderJsonBuilder().build();
        dk.dbc.dataio.commons.types.FlowBinder flowBinder = JsonUtil.fromJson(flowBinderStr, dk.dbc.dataio.commons.types.FlowBinder.class, MixIns.getMixIns());

        when(query.getResultList()).thenReturn(Arrays.asList(flowBinder));
        Response response = fbb.getFlowBinder("xml", "nmalbum", "utf8", 654321L, "someDestination");
        assertThat(response.getStatus(), is(200));
    }

    @Test
    public void getAllFlowBinders_validParametersAndMatchingFlow_returnsResponseWithFlowAndHTTP200() throws JsonException {
        FlowBindersBean fbb = new FlowBindersBean();
        EntityManager entityManager = mock(EntityManager.class);
        fbb.entityManager = entityManager;
        TypedQuery<dk.dbc.dataio.commons.types.FlowBinder> query = mock(TypedQuery.class);
        when(entityManager.createNamedQuery(FlowBinder.QUERY_FIND_ALL, dk.dbc.dataio.commons.types.FlowBinder.class)).thenReturn(query);

        String flowBinderStr = new FlowBinderJsonBuilder().build();
        dk.dbc.dataio.commons.types.FlowBinder flowBinder = JsonUtil.fromJson(flowBinderStr, dk.dbc.dataio.commons.types.FlowBinder.class, MixIns.getMixIns());

        when(query.getResultList()).thenReturn(Arrays.asList(flowBinder));
        Response response = fbb.findAllFlowBinders();
        assertThat(response.getStatus(), is(200));
    }

    @Test
    public void getFlowBinder_flowBinderNotFound_returnsResponse404() throws JsonException {
        final long FLOW_BINDER_ID = 12L;
        FlowBindersBean bean = new FlowBindersBean();
        EntityManager mockedEntityManager = mock(EntityManager.class);
        bean.entityManager = mockedEntityManager;
        when(mockedEntityManager.find(FlowBinder.class, FLOW_BINDER_ID)).thenReturn(null);  // null => Not found

        Response response = bean.getFlowBinderById(FLOW_BINDER_ID);

        assertThat(response.getStatus(), is(404));
    }

    @Test
    public void getFlowBinder_flowBinderFound_returnsResponseWithFlowAndHTTP200() throws JsonException {
        final long FLOW_BINDER_ID = 12L;
        FlowBindersBean bean = new FlowBindersBean();
        EntityManager mockedEntityManager = mock(EntityManager.class);
        bean.entityManager = mockedEntityManager;
        FlowBinder flowBinder = testFlowBinder();
        when(mockedEntityManager.find(FlowBinder.class, FLOW_BINDER_ID)).thenReturn(flowBinder);

        Response response = bean.getFlowBinderById(FLOW_BINDER_ID);

        assertThat(response.getStatus(), is(200));
        assertThat(response.hasEntity(), is(true));
        JsonNode entity = JsonUtil.getJsonRoot((String) response.getEntity());
        assertThat(entity.get("version").asLong(), is(FLOW_BINDER_VERSION));
        assertThat(entity.get("content").get("flowId").asLong(), is(flowBinder.getFlowId()));
        assertTrue(flowBinder.getSubmitterIds().contains(entity.get("content").get("submitterIds").elements().next().asLong()));
        assertThat(entity.get("content").get("sinkId").asLong(), is((flowBinder.getSinkId())));
    }


    final long FLOW_BINDER_VERSION = 1245L;
    private FlowBinder testFlowBinder() throws JsonException {
        FlowBinder flowBinder = new FlowBinder();
        flowBinder.setVersion(FLOW_BINDER_VERSION);
        flowBinder.setFlow(testFlow());
        flowBinder.setSink(testSink());
        flowBinder.setContent(new FlowBinderContentJsonBuilder().build());
        flowBinder.setSubmitters(testSubmitters());
        return flowBinder;
    }

    final String TEST_FLOW_NAME = "Test flow name";
    private Flow testFlow() throws JsonException {
        final Flow flow = new Flow();
        flow.setContent(new FlowContentJsonBuilder().setName(TEST_FLOW_NAME).build());
        return flow;
    }

    private Sink testSink() throws JsonException {
        final Sink sink = new Sink();
        sink.setContent(new SinkContentJsonBuilder().build());
        return sink;
    }

    private Set<Submitter> testSubmitters() throws JsonException {
        Submitter submitter = new Submitter();
        submitter.setContent(new SubmitterContentJsonBuilder().build());
        return new HashSet<>(Arrays.asList(submitter));
    }

}