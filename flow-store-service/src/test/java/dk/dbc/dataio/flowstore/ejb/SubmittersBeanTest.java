package dk.dbc.dataio.flowstore.ejb;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.commons.utils.test.json.SubmitterContentJsonBuilder;
import dk.dbc.dataio.flowstore.entity.Submitter;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SubmittersBeanTest {

    private static final EntityManager ENTITY_MANAGER = mock(EntityManager.class);
    private static final Long DEFAULT_TEST_ID = 23L;
    private static final Long DEFAULT_TEST_VERSION = 4L;
    private static final String DEFAULT_TEST_ETAG_VALUE = Long.toString(DEFAULT_TEST_VERSION);

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
    public void submittersBean_validConstructor_newInstance() {
        SubmittersBean submittersBean = newSubmittersBeanWithMockedEntityManager();
        assertThat(submittersBean, is(notNullValue()));
    }

    @Test
    public void createSubmitter_nullSubmitterContent_throws() {
        assertThrows(NullPointerException.class, () -> newSubmittersBeanWithMockedEntityManager().createSubmitter(null, null));
    }

    @Test
    public void createSubmitter_emptySubmitterContent_throws() {
        assertThrows(IllegalArgumentException.class, () -> newSubmittersBeanWithMockedEntityManager().createSubmitter(null, ""));
    }

    @Test
    public void createSubmitter_invalidJsonInSubmitterContent_throws() {
        assertThrows(JSONBException.class, () -> newSubmittersBeanWithMockedEntityManager().createSubmitter(null, "Invalid JSON"));
    }

    @Test
    public void getSubmitter_submitterFound_returnsResponseWithHttpStatusOK() throws JSONBException {
        SubmittersBean submittersBean = newSubmittersBeanWithMockedEntityManager();
        Submitter submitter = new Submitter();
        final String submitterName = "testSubmitter";
        final Long submitterNumber = 555555L;
        submitter.setContent(new SubmitterContentJsonBuilder().setName(submitterName).setNumber(submitterNumber).build());
        submitter.setVersion(DEFAULT_TEST_VERSION);

        when(ENTITY_MANAGER.find(eq(Submitter.class), any())).thenReturn(submitter);

        Response response = submittersBean.getSubmitter(DEFAULT_TEST_ID);
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.hasEntity(), is(true));
        JsonNode entityNode = jsonbContext.getJsonTree((String) response.getEntity());
        assertThat(entityNode.get("content").get("name").textValue(), is(submitterName));
        assertThat(entityNode.get("content").get("number").longValue(), is(submitterNumber));
        assertThat(response.getEntityTag().getValue(), is(DEFAULT_TEST_ETAG_VALUE));
    }

    @Test
    public void getSubmitter_noSubmitterFound_returnsResponseWithHttpStatusNotFound() throws JSONBException {
        SubmittersBean submittersBean = newSubmittersBeanWithMockedEntityManager();

        when(ENTITY_MANAGER.find(eq(Submitter.class), any())).thenReturn(null);

        Response response = submittersBean.getSubmitter(DEFAULT_TEST_ID);
        assertThat(response.getStatus(), is(Response.Status.NOT_FOUND.getStatusCode()));
        assertThat(response.getEntityTag(), is(nullValue()));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void getSubmitterBySubmitterNumber_submitterFound_returnsResponseWithHttpStatusOK() throws JSONBException {
        SubmittersBean submittersBean = newSubmittersBeanWithMockedEntityManager();
        Submitter submitter = new Submitter();
        final Long submitterNumber = 463725L;
        submitter.setContent(new SubmitterContentJsonBuilder().setNumber(submitterNumber).build());
        submitter.setVersion(DEFAULT_TEST_VERSION);

        TypedQuery<Submitter> query = mock(TypedQuery.class);

        when(ENTITY_MANAGER.createNamedQuery(eq(Submitter.QUERY_FIND_BY_CONTENT), eq(Submitter.class))).thenReturn(query);
        when(query.getResultList()).thenReturn(Collections.singletonList(submitter));
        when(query.getSingleResult()).thenReturn(submitter);

        Response response = submittersBean.getSubmitterBySubmitterNumber(submitterNumber);
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.hasEntity(), is(true));
        JsonNode entityNode = jsonbContext.getJsonTree((String) response.getEntity());
        assertThat(entityNode.get("content").get("number").longValue(), is(submitterNumber));
        assertThat(response.getEntityTag().getValue(), is(DEFAULT_TEST_ETAG_VALUE));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void getSubmitterBySubmitterNumber_noSubmitterFound_returnsResponseWithHttpStatusNotFound() throws JSONBException {
        SubmittersBean submittersBean = newSubmittersBeanWithMockedEntityManager();
        final Long submitterNumber = 463725L;
        TypedQuery<Submitter> query = mock(TypedQuery.class);
        when(ENTITY_MANAGER.createNamedQuery(eq(Submitter.QUERY_FIND_BY_CONTENT), eq(Submitter.class))).thenReturn(query);
        when(query.getResultList()).thenReturn(new ArrayList<>());

        Response response = submittersBean.getSubmitterBySubmitterNumber(submitterNumber);
        assertThat(response.getStatus(), is(Response.Status.NOT_FOUND.getStatusCode()));
        assertThat(response.getEntityTag(), is(nullValue()));
    }


    @Test
    public void updateSubmitter_nullSubmitterContent_throws() throws JSONBException {
        assertThrows(NullPointerException.class, () -> newSubmittersBeanWithMockedEntityManager().updateSubmitter(null, 1L, 1L));
    }

    @Test
    public void updateSubmitter_emptySubmitterContent_throws() throws JSONBException {
        assertThrows(IllegalArgumentException.class, () -> newSubmittersBeanWithMockedEntityManager().updateSubmitter("", 1L, 1L));
    }

    @Test
    public void updateSubmitter_submitterNotFound_returnsResponseWithHttpStatusNotFound() throws JSONBException {
        String submitterContent = new SubmitterContentJsonBuilder().setName("UpdateContentName").build();
        SubmittersBean submittersBean = newSubmittersBeanWithMockedEntityManager();

        when(ENTITY_MANAGER.find(eq(Submitter.class), any())).thenReturn(null);

        Response response = submittersBean.updateSubmitter(submitterContent, DEFAULT_TEST_ID, DEFAULT_TEST_VERSION);
        assertThat(response.getStatus(), is(Response.Status.NOT_FOUND.getStatusCode()));
    }

    @Test
    public void updateSubmitter_submitterFound_returnsResponseWithHttpStatusOk_returnsSubmitter() throws JSONBException {
        Submitter submitter = mock(Submitter.class);
        SubmittersBean submittersBean = newSubmittersBeanWithMockedEntityManager();
        String submitterContent = new SubmitterContentJsonBuilder().build();
        submittersBean.jsonbContext = mock(JSONBContext.class);

        when(submittersBean.jsonbContext.marshall(submitter)).thenReturn("test");
        when(ENTITY_MANAGER.find(eq(Submitter.class), any())).thenReturn(submitter);
        when(submitter.getVersion()).thenReturn(DEFAULT_TEST_VERSION);

        Response response = submittersBean.updateSubmitter(submitterContent, DEFAULT_TEST_ID, DEFAULT_TEST_VERSION);
        verify(submitter).setContent(submitterContent);
        verify(submitter).setVersion(DEFAULT_TEST_VERSION);
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.hasEntity(), is(true));
        assertThat(response.getEntityTag().getValue(), is(DEFAULT_TEST_ETAG_VALUE));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void findAllSubmitters_noSubmittersFound_returnsResponseWithHttpStatusOkAndSubmitterEntity() throws JSONBException {
        SubmittersBean submittersBean = newSubmittersBeanWithMockedEntityManager();
        TypedQuery query = mock(TypedQuery.class);

        when(ENTITY_MANAGER.createNamedQuery(Submitter.QUERY_FIND_ALL, Submitter.class)).thenReturn(query);
        when(query.getResultList()).thenReturn(Collections.emptyList());

        Response response = submittersBean.findAllSubmitters();
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.hasEntity(), is(true));
        ArrayNode entityNode = (ArrayNode) jsonbContext.getJsonTree((String) response.getEntity());
        assertThat(entityNode.size(), is(0));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void findAllSubmitters_submittersFound_returnsResponseWithHttpStatusOkAndSubmitterEntities() throws JSONBException {
        SubmittersBean submittersBean = newSubmittersBeanWithMockedEntityManager();
        TypedQuery query = mock(TypedQuery.class);
        final String nameSubmitterA = "A";
        Submitter submitterA = new Submitter();
        submitterA.setContent(new SubmitterContentJsonBuilder()
                .setName(nameSubmitterA)
                .build());
        final String nameSubmitterB = "B";
        Submitter submitterB = new Submitter();
        submitterB.setContent(new SubmitterContentJsonBuilder()
                .setName(nameSubmitterB)
                .build());

        when(ENTITY_MANAGER.createNamedQuery(Submitter.QUERY_FIND_ALL, Submitter.class)).thenReturn(query);
        when(query.getResultList()).thenReturn(Arrays.asList(submitterA, submitterB));

        Response response = submittersBean.findAllSubmitters();
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.hasEntity(), is(true));
        ArrayNode entityNode = (ArrayNode) jsonbContext.getJsonTree((String) response.getEntity());
        assertThat(entityNode.size(), is(2));
        assertThat(entityNode.get(0).get("content").get("name").textValue(), is(nameSubmitterA));
        assertThat(entityNode.get(1).get("content").get("name").textValue(), is(nameSubmitterB));
    }

    public static SubmittersBean newSubmittersBeanWithMockedEntityManager() {
        SubmittersBean submittersBean = new SubmittersBean();
        submittersBean.entityManager = ENTITY_MANAGER;
        return submittersBean;
    }
}
