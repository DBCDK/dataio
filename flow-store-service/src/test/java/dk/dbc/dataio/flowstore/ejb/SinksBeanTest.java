package dk.dbc.dataio.flowstore.ejb;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.commons.utils.test.json.SinkContentJsonBuilder;
import dk.dbc.dataio.flowstore.entity.SinkEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SinksBeanTest {
    private static final EntityManager ENTITY_MANAGER = mock(EntityManager.class);
    private static final long ID = 123;
    private static final long VERSION = 41L;
    private static final String ETAG_VALUE = Long.toString(VERSION);

    private JSONBContext jsonbContext;
    private UriInfo mockedUriInfo;

    @BeforeEach
    public void setup() throws URISyntaxException {
        jsonbContext = new JSONBContext();

        mockedUriInfo = mock(UriInfo.class);
        UriBuilder mockedUriBuilder = mock(UriBuilder.class);

        when(mockedUriInfo.getAbsolutePathBuilder()).thenReturn(mockedUriBuilder);
        when(mockedUriBuilder.path(anyString())).thenReturn(mockedUriBuilder);
        when(mockedUriBuilder.build()).thenReturn(new URI("location"));
    }

    @Test
    public void sinksBean_validConstructor_newInstance() {
        SinksBean sinksBean = newSinksBeanWithMockedEntityManager();
        assertThat(sinksBean, is(notNullValue()));
    }

    @Test
    public void createSink_nullSinkContent_throws() {
        assertThrows(NullPointerException.class, () -> newSinksBeanWithMockedEntityManager().createSink(null, null));
    }

    @Test
    public void createSink_emptySinkContent_throws() {
        assertThrows(IllegalArgumentException.class, () -> newSinksBeanWithMockedEntityManager().createSink(null, ""));
    }

    @Test
    public void createSink_invalidJSON_throwsJsonException() throws JSONBException {
        assertThrows(JSONBException.class, () -> newSinksBeanWithMockedEntityManager().createSink(null, "invalid Json"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void findAllSinks_noSinksFound_returnsResponseWithHttpStatusOkAndSinkEntity() throws JSONBException {
        SinksBean sinksBean = newSinksBeanWithMockedEntityManager();
        TypedQuery<SinkEntity> query = mock(TypedQuery.class);

        when(ENTITY_MANAGER.createNamedQuery(SinkEntity.QUERY_FIND_ALL, SinkEntity.class)).thenReturn(query);
        when(query.getResultList()).thenReturn(Collections.emptyList());

        Response response = sinksBean.findAllSinks();
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.hasEntity(), is(true));
        ArrayNode entityNode = (ArrayNode) jsonbContext.getJsonTree((String) response.getEntity());
        assertThat(entityNode.size(), is(0));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void findAllSinks_sinksFound_returnsResponseWithHttpStatusOkAndSinkEntities() throws JSONBException {
        SinksBean sinksBean = newSinksBeanWithMockedEntityManager();
        TypedQuery<SinkEntity> query = mock(TypedQuery.class);
        final String nameSinkA = "A";
        SinkEntity sinkEntityA = new SinkEntity();
        sinkEntityA.setContent(new SinkContentJsonBuilder()
                .setName(nameSinkA)
                .build());
        final String nameSinkB = "B";
        SinkEntity sinkEntityB = new SinkEntity();
        sinkEntityB.setContent(new SinkContentJsonBuilder()
                .setName(nameSinkB)
                .build());

        when(ENTITY_MANAGER.createNamedQuery(SinkEntity.QUERY_FIND_ALL, SinkEntity.class)).thenReturn(query);
        when(query.getResultList()).thenReturn(Arrays.asList(sinkEntityA, sinkEntityB));

        Response response = sinksBean.findAllSinks();
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.hasEntity(), is(true));
        ArrayNode entityNode = (ArrayNode) jsonbContext.getJsonTree((String) response.getEntity());
        assertThat(entityNode.size(), is(2));
        assertThat(entityNode.get(0).get("content").get("name").textValue(), is(nameSinkA));
        assertThat(entityNode.get(1).get("content").get("name").textValue(), is(nameSinkB));
    }

    @Test
    public void getSink_noSinkFound_returnsResponseWithHttpStatusNotFound() throws JSONBException {
        SinksBean sinksBean = newSinksBeanWithMockedEntityManager();

        when(ENTITY_MANAGER.find(eq(SinkEntity.class), any())).thenReturn(null);

        Response response = sinksBean.getSink(ID);
        assertThat(response.getStatus(), is(Response.Status.NOT_FOUND.getStatusCode()));
        assertThat(response.getEntityTag(), is(nullValue()));
    }

    @Test
    public void getSink_sinkFound_returnsResponseWithHttpStatusOK() throws JSONBException {
        SinksBean sinksBean = newSinksBeanWithMockedEntityManager();
        SinkEntity sinkEntity = new SinkEntity();
        final String sinkName = "testSink";
        sinkEntity.setContent(new SinkContentJsonBuilder().setName(sinkName).build());
        sinkEntity.setVersion(VERSION);

        when(ENTITY_MANAGER.find(eq(SinkEntity.class), any())).thenReturn(sinkEntity);

        Response response = sinksBean.getSink(ID);
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.hasEntity(), is(true));
        JsonNode entityNode = jsonbContext.getJsonTree((String) response.getEntity());
        assertThat(entityNode.get("content").get("name").textValue(), is(sinkName));
        assertThat(response.getEntityTag().getValue(), is(ETAG_VALUE));
    }

    @Test
    public void updateSink_nullSinkContent_throws() {
        assertThrows(NullPointerException.class, () -> newSinksBeanWithMockedEntityManager().updateSink(null, ID, VERSION));
    }

    @Test
    public void updateSink_emptySinkContent_throws() {
        assertThrows(IllegalArgumentException.class, () -> newSinksBeanWithMockedEntityManager().updateSink("", ID, VERSION));
    }

    @Test
    public void updateSink_sinkNotFound_returnsResponseWithHttpStatusNotFound() throws JSONBException {
        String sinkContent = new SinkContentJsonBuilder().setName("UpdateContentName").build();
        SinksBean sinksBean = newSinksBeanWithMockedEntityManager();

        when(ENTITY_MANAGER.find(eq(SinkEntity.class), any())).thenReturn(null);

        Response response = sinksBean.updateSink(sinkContent, ID, VERSION);
        assertThat(response.getStatus(), is(Response.Status.NOT_FOUND.getStatusCode()));
    }

    @Test
    public void updateSink_sinkFound_returnsResponseWithHttpStatusOk_returnsSink() throws JSONBException {
        SinkEntity sinkEntity = mock(SinkEntity.class);
        SinksBean sinksBean = newSinksBeanWithMockedEntityManager();
        String sinkContent = new SinkContentJsonBuilder().build();

        sinksBean.jsonbContext = mock(JSONBContext.class);
        when(sinksBean.jsonbContext.marshall(sinkEntity)).thenReturn("test");
        when(ENTITY_MANAGER.find(eq(SinkEntity.class), any())).thenReturn(sinkEntity);
        when(sinkEntity.getVersion()).thenReturn(VERSION);

        Response response = sinksBean.updateSink(sinkContent, ID, VERSION);
        verify(sinkEntity).setContent(sinkContent);
        verify(sinkEntity).setVersion(VERSION);
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.hasEntity(), is(true));
        assertThat(response.getEntityTag().getValue(), is(ETAG_VALUE));
    }


    @Test
    public void deleteSink_sinkNotFound_returnsResponseWithHttpStatusNotFound() {
        SinksBean sinksBean = newSinksBeanWithMockedEntityManager();
        when(ENTITY_MANAGER.find(eq(SinkEntity.class), any())).thenReturn(null);

        Response response = sinksBean.deleteSink(ID, VERSION);
        assertThat(response.getStatus(), is(Response.Status.NOT_FOUND.getStatusCode()));
    }

    @Test
    public void deleteSink_sinkFound_returnsNoContentHttpResponse() {
        SinkEntity sinkEntity = mock(SinkEntity.class);
        SinksBean sinksBean = newSinksBeanWithMockedEntityManager();

        when(ENTITY_MANAGER.find(eq(SinkEntity.class), any())).thenReturn(sinkEntity);
        when(ENTITY_MANAGER.merge(any(SinkEntity.class))).thenReturn(sinkEntity);

        Response response = sinksBean.deleteSink(ID, VERSION);

        verify(sinkEntity).setVersion(VERSION);
        verify(ENTITY_MANAGER).remove(sinkEntity);
        assertThat(response.getStatus(), is(Response.Status.NO_CONTENT.getStatusCode()));
    }

    @Test
    public void createSink_sinkCreated_returnsResponseWithHttpStatusOk_returnsSink() throws JSONBException {
        String sinkContent = new SinkContentJsonBuilder().build();
        SinksBean sinksBean = newSinksBeanWithMockedEntityManager();

        doAnswer(invocationOnMock -> {
            Object[] args = invocationOnMock.getArguments();
            SinkEntity sinkEntity = (SinkEntity) args[0];
            sinkEntity.setVersion(VERSION);
            return null;
        }).when(ENTITY_MANAGER).persist(any(SinkEntity.class));

        Response response = sinksBean.createSink(mockedUriInfo, sinkContent);

        assertThat(response.getStatus(), is(Response.Status.CREATED.getStatusCode()));
        assertThat(response.hasEntity(), is(true));
        assertThat(response.getEntityTag().getValue(), is(ETAG_VALUE));
    }

    public static SinksBean newSinksBeanWithMockedEntityManager() {
        SinksBean sinksBean = new SinksBean();
        sinksBean.entityManager = ENTITY_MANAGER;
        return sinksBean;
    }
}
