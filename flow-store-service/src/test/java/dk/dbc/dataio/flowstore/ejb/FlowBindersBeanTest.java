package dk.dbc.dataio.flowstore.ejb;

import dk.dbc.dataio.commons.types.json.mixins.MixIns;
import dk.dbc.dataio.commons.utils.json.JsonException;
import dk.dbc.dataio.commons.utils.json.JsonUtil;
import dk.dbc.dataio.commons.utils.test.json.FlowBinderJsonBuilder;
import dk.dbc.dataio.flowstore.entity.FlowBinder;
import java.util.Arrays;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.ws.rs.core.Response;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import org.junit.Test;
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

        String flowStr = new FlowBinderJsonBuilder().build();
        dk.dbc.dataio.commons.types.FlowBinder flowBinder = JsonUtil.fromJson(flowStr, dk.dbc.dataio.commons.types.FlowBinder.class, MixIns.getMixIns());

        when(query.getResultList()).thenReturn(Arrays.asList(flowBinder));
        Response response = fbb.findAllFlowBinders();
        assertThat(response.getStatus(), is(200));
    }
}