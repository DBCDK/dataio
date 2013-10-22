package dk.dbc.dataio.flowstore.ejb;

import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.json.mixins.MixIns;
import dk.dbc.dataio.commons.utils.json.JsonException;
import dk.dbc.dataio.commons.utils.json.JsonUtil;
import dk.dbc.dataio.flowstore.entity.FlowBinder;
import dk.dbc.dataio.integrationtest.ITUtil;
import java.util.Arrays;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.ws.rs.core.Response;
import org.junit.Test;
import static org.junit.Assert.assertThat;
import static org.hamcrest.CoreMatchers.is;

import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FlowBindersBeanTest {

    private static Logger log = LoggerFactory.getLogger(FlowBindersBeanTest.class);

    @Test(expected = NullPointerException.class)
    public void getFlow_nullPackagingParameter_throws() throws JsonException {
        FlowBindersBean fbb = new FlowBindersBean();
        fbb.getFlow(null, "nmalbum", "utf8", 654321L, "someDestination");
    }

    @Test(expected = IllegalArgumentException.class)
    public void getFlow_emptyPackagingParameter_throws() throws JsonException {
        FlowBindersBean fbb = new FlowBindersBean();
        fbb.getFlow("", "nmalbum", "utf8", 654321L, "someDestination");
    }

    @Test(expected = NullPointerException.class)
    public void getFlow_nullFormatParameter_throws() throws JsonException {
        FlowBindersBean fbb = new FlowBindersBean();
        fbb.getFlow("xml", null, "utf8", 654321L, "someDestination");
    }

    @Test(expected = IllegalArgumentException.class)
    public void getFlow_emptyFormatParameter_throws() throws JsonException {
        FlowBindersBean fbb = new FlowBindersBean();
        fbb.getFlow("xml", "", "utf8", 654321L, "someDestination");
    }

    @Test(expected = NullPointerException.class)
    public void getFlow_nullCharsetParameter_throws() throws JsonException {
        FlowBindersBean fbb = new FlowBindersBean();
        fbb.getFlow("xml", "nmalbum", null, 654321L, "someDestination");
    }

    @Test(expected = IllegalArgumentException.class)
    public void getFlow_emptyCharsetParameter_throws() throws JsonException {
        FlowBindersBean fbb = new FlowBindersBean();
        fbb.getFlow("xml", "nmalbum", "", 654321L, "someDestination");
    }

    @Test(expected = NullPointerException.class)
    public void getFlow_nullSubmitterParameter_throws() throws JsonException {
        FlowBindersBean fbb = new FlowBindersBean();
        fbb.getFlow("xml", "nmalbum", "utf8", null, "someDestination");
    }

    @Test(expected = NullPointerException.class)
    public void getFlow_nullDestinationParameter_throws() throws JsonException {
        FlowBindersBean fbb = new FlowBindersBean();
        fbb.getFlow("xml", "nmalbum", "utf8", 654321L, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void getFlow_emptyDestinationParameter_throws() throws JsonException {
        FlowBindersBean fbb = new FlowBindersBean();
        fbb.getFlow("xml", "nmalbum", "utf8", 654321L, "");
    }

    @Test
    public void getFlow_validParametersButNoMatchingFlow_returnsResponseWithFlowAndHTTP404() throws JsonException {
        FlowBindersBean fbb = new FlowBindersBean();
        EntityManager entityManager = mock(EntityManager.class);
        fbb.entityManager = entityManager;
        Query query = mock(Query.class);
        when(entityManager.createNamedQuery(FlowBinder.QUERY_FIND_FLOW)).thenReturn(query);

        when(query.getResultList()).thenReturn(Arrays.asList());
        Response response = fbb.getFlow("xml", "nmalbum", "utf8", 654321L, "someDestination");
        assertThat(response.getStatus(), is(404));
    }

    @Test
    public void getFlow_validParametersAndMatchingFlow_returnsResponseWithFlowAndHTTP200() throws JsonException {
        FlowBindersBean fbb = new FlowBindersBean();
        EntityManager entityManager = mock(EntityManager.class);
        fbb.entityManager = entityManager;
        Query query = mock(Query.class);
        when(entityManager.createNamedQuery(FlowBinder.QUERY_FIND_FLOW)).thenReturn(query);

        String flowStr = new ITUtil.FlowJsonBuilder().build();
        Flow flow = JsonUtil.fromJson(flowStr, Flow.class, MixIns.getMixIns());

        when(query.getResultList()).thenReturn(Arrays.asList(flow));
        Response response = fbb.getFlow("xml", "nmalbum", "utf8", 654321L, "someDestination");
        assertThat(response.getStatus(), is(200));
    }
}