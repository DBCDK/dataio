package dk.dbc.dataio.flowstore.ejb;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import dk.dbc.dataio.commons.utils.json.JsonException;
import dk.dbc.dataio.commons.utils.json.JsonUtil;
import dk.dbc.dataio.commons.utils.test.json.SinkContentJsonBuilder;
import dk.dbc.dataio.flowstore.entity.Sink;
import org.junit.Test;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.ws.rs.core.Response;
import java.util.Arrays;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SinksBeanTest {
    Logger logger = LoggerFactory.getLogger(SinksBeanTest.class);

    private static final EntityManager ENTITY_MANAGER = mock(EntityManager.class);

    @Test
    public void sinksBean_validConstructor_newInstance() {
        final SinksBean sinksBean = newSinksBeanWithMockedEntityManager();
        assertThat(sinksBean, is(notNullValue()));
    }

    @Test(expected = NullPointerException.class)
    public void createSink_nullSinkContent_throws() throws JsonException {
        newSinksBeanWithMockedEntityManager().createSink(null, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void createSink_emptySinkContent_throws() throws JsonException {
        newSinksBeanWithMockedEntityManager().createSink(null, "");
    }

    @Test(expected = JsonException.class)
    public void createSink_invalidJSON_throwsJsonException() throws JsonException {
        newSinksBeanWithMockedEntityManager().createSink(null, "invalid Json");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void findAllSinks_noSinksFound_returnsResponseWithHttpStatusOk() throws JsonException {
        final TypedQuery query = mock(TypedQuery.class);
        when(ENTITY_MANAGER.createNamedQuery(Sink.QUERY_FIND_ALL, Sink.class)).thenReturn(query);
        when(query.getResultList()).thenReturn(Arrays.asList());

        final SinksBean sinksBean = newSinksBeanWithMockedEntityManager();
        final Response response = sinksBean.findAllSinks();
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.hasEntity(), is(true));
        final ArrayNode entityNode = (ArrayNode) JsonUtil.getJsonRoot((String) response.getEntity());
        assertThat(entityNode.size(), is(0));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void findAllSinks_sinksFound_returnsResponseWithHttpStatusOk() throws JsonException {
        final String nameSinkA = "A";
        final Sink sinkA = new Sink();
        sinkA.setContent(new SinkContentJsonBuilder()
            .setName(nameSinkA)
            .build());
        final String nameSinkB = "B";
        final Sink sinkB = new Sink();
        sinkB.setContent(new SinkContentJsonBuilder()
                .setName(nameSinkB)
                .build());

        final TypedQuery query = mock(TypedQuery.class);
        when(ENTITY_MANAGER.createNamedQuery(Sink.QUERY_FIND_ALL, Sink.class)).thenReturn(query);
        when(query.getResultList()).thenReturn(Arrays.asList(sinkA, sinkB));

        final SinksBean sinksBean = newSinksBeanWithMockedEntityManager();
        final Response response = sinksBean.findAllSinks();
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.hasEntity(), is(true));
        final ArrayNode entityNode = (ArrayNode) JsonUtil.getJsonRoot((String) response.getEntity());
        assertThat(entityNode.size(), is(2));
        assertThat(entityNode.get(0).get("content").get("name").textValue(), is(nameSinkA));
        assertThat(entityNode.get(1).get("content").get("name").textValue(), is(nameSinkB));
    }

    @Test
    public void getSink_noSinkFound_returnsResponseWithHttpStatusNotFound() throws JsonException {
        final SinksBean sinksBean = newSinksBeanWithMockedEntityManager();
        when(ENTITY_MANAGER.find(eq(Sink.class), any())).thenReturn(null);
        Response response = sinksBean.getSink(1L);
        assertThat(response.getStatus(), is(Response.Status.NOT_FOUND.getStatusCode()));
    }

    @Test
    public void getSink_sinkFound_returnsResponseWithHttpStatusOK() throws JsonException {
        final Sink sink = new Sink();
        sink.setContent(new SinkContentJsonBuilder().setName("testSink").build());
        final SinksBean sinksBean = newSinksBeanWithMockedEntityManager();
        when(ENTITY_MANAGER.find(eq(Sink.class), any())).thenReturn(sink);

        Response response = sinksBean.getSink(1L);
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.hasEntity(), is(true));
        JsonNode entityNode = JsonUtil.getJsonRoot((String)response.getEntity());
        assertThat(entityNode.get("content").get("name").textValue(), is("testSink"));
    }

    public static SinksBean newSinksBeanWithMockedEntityManager() {
        final SinksBean sinksBean = new SinksBean();
        sinksBean.entityManager = ENTITY_MANAGER;
        return sinksBean;
    }
}