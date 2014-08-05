package dk.dbc.dataio.flowstore.ejb;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import dk.dbc.dataio.commons.types.SinkContent;
import dk.dbc.dataio.commons.types.exceptions.ReferencedEntityNotFoundException;
import dk.dbc.dataio.commons.types.json.mixins.MixIns;
import dk.dbc.dataio.commons.utils.json.JsonException;
import dk.dbc.dataio.commons.utils.json.JsonUtil;
import dk.dbc.dataio.commons.utils.test.json.SinkContentJsonBuilder;
import dk.dbc.dataio.flowstore.entity.Sink;
import dk.dbc.dataio.flowstore.util.ServiceUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.util.Arrays;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;


@RunWith(PowerMockRunner.class)
@PrepareForTest({
        JsonUtil.class,
        ServiceUtil.class})
public class SinksBeanTest {

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
        assertThat(response.getEntityTag(), is(nullValue()));
    }

    @Test
    public void getSink_sinkFound_returnsResponseWithHttpStatusOK() throws JsonException {
        final String SINK_NAME = "testSink";
        final String EXPECTED_ETAG_VALUE = "234";
        final Sink sink = new Sink();
        sink.setContent(new SinkContentJsonBuilder().setName(SINK_NAME).build());
        sink.setVersion(Long.parseLong(EXPECTED_ETAG_VALUE));
        final SinksBean sinksBean = newSinksBeanWithMockedEntityManager();
        when(ENTITY_MANAGER.find(eq(Sink.class), any())).thenReturn(sink);

        Response response = sinksBean.getSink(1L);
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.hasEntity(), is(true));
        JsonNode entityNode = JsonUtil.getJsonRoot((String)response.getEntity());
        assertThat(entityNode.get("content").get("name").textValue(), is(SINK_NAME));
        assertThat(response.getEntityTag().getValue(), is(EXPECTED_ETAG_VALUE));
    }

    @Test(expected = NullPointerException.class)
    public void updateSink_nullSinkContent_throws() throws JsonException, ReferencedEntityNotFoundException {
        newSinksBeanWithMockedEntityManager().updateSink(null, null, 0L, 0L);
    }

    @Test(expected = IllegalArgumentException.class)
    public void updateSink_emptySinkContent_throws() throws JsonException, ReferencedEntityNotFoundException {
        newSinksBeanWithMockedEntityManager().updateSink(null, "", 0L, 0L);
    }

    @Test
    public void updateSink_sinkNotFound_throwsException() throws JsonException, ReferencedEntityNotFoundException {
        final SinksBean sinksBean = newSinksBeanWithMockedEntityManager();
        when(ENTITY_MANAGER.find(eq(Sink.class), any())).thenReturn(null);

        final String sinkContent = new SinkContentJsonBuilder().setName("UpdateContentName").build();
        final Response response = sinksBean.updateSink(null, sinkContent, 123L, 4321L);
        assertThat(response.getStatus(), is(Response.Status.NOT_FOUND.getStatusCode()));
    }

    @Test
    public void updateSink_sinkFound_returnsResponseWithHttpStatusOk_returnsSink() throws JsonException, ReferencedEntityNotFoundException {
        final Long CURRENT_VERSION = 345L;
        final Long EXPECTED_ETAG_VALUE = CURRENT_VERSION + 1;
        final Sink sink = mock(Sink.class);
        final UriInfo uriInfo = mock(UriInfo.class);
        final UriBuilder uriBuilder = mock(UriBuilder.class);
        final SinksBean sinksBean = newSinksBeanWithMockedEntityManager();

        when(uriInfo.getAbsolutePathBuilder()).thenReturn(uriBuilder);
        when(uriBuilder.path(anyString())).thenReturn(uriBuilder);
        mockStatic(JsonUtil.class);
        when(JsonUtil.toJson(sink)).thenReturn("test");
        when(ENTITY_MANAGER.find(eq(Sink.class), any())).thenReturn(sink);
        when(sink.getVersion()).thenReturn(EXPECTED_ETAG_VALUE);

        final String sinkContent = new SinkContentJsonBuilder().setName("UpdateContentName").build();
        final Response response = sinksBean.updateSink(uriInfo, sinkContent, 123L, CURRENT_VERSION);

        verify(sink).setContent(sinkContent);
        verify(sink).setVersion(CURRENT_VERSION);
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.hasEntity(), is(true));
        assertThat(response.getEntityTag().getValue(), is(EXPECTED_ETAG_VALUE.toString()));
    }

    @Test
    public void CreateSink_sinkCreated_returnsResponseWithHttpStatusOk_returnsSink() throws JsonException, ReferencedEntityNotFoundException {
        final UriInfo uriInfo = mock(UriInfo.class);
        final UriBuilder uriBuilder = mock(UriBuilder.class);
        final SinkContent sinkContent = new SinkContent("CreateContentName", "Resource");
        final String sinkContentString = new SinkContentJsonBuilder().setName("CreateContentName").build();
        final SinksBean sinksBean = newSinksBeanWithMockedEntityManager();
        final Sink sink = new Sink();
        final String expectedETagValue = "123";
        sink.setVersion(Long.parseLong(expectedETagValue));

        when(uriInfo.getAbsolutePathBuilder()).thenReturn(uriBuilder);
        when(uriBuilder.path(anyString())).thenReturn(uriBuilder);
        mockStatic(JsonUtil.class);
        when(JsonUtil.fromJson(sinkContentString, SinkContent.class, MixIns.getMixIns())).thenReturn(sinkContent);
        when(JsonUtil.toJson(any(Sink.class))).thenReturn("sink");
        mockStatic(ServiceUtil.class);
        when(ServiceUtil.saveAsVersionedEntity(ENTITY_MANAGER, Sink.class, sinkContentString)).thenReturn(sink);

        final Response response = sinksBean.createSink(uriInfo, sinkContentString);

        assertThat(response.getStatus(), is(Response.Status.CREATED.getStatusCode()));
        assertThat(response.hasEntity(), is(true));
        assertThat(response.getEntityTag().getValue(), is(expectedETagValue));
    }

    public static SinksBean newSinksBeanWithMockedEntityManager() {
        final SinksBean sinksBean = new SinksBean();
        sinksBean.entityManager = ENTITY_MANAGER;
        return sinksBean;
    }
}