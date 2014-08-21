package dk.dbc.dataio.flowstore.ejb;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import dk.dbc.dataio.commons.types.exceptions.ReferencedEntityNotFoundException;
import dk.dbc.dataio.commons.utils.json.JsonException;
import dk.dbc.dataio.commons.utils.json.JsonUtil;
import dk.dbc.dataio.commons.utils.test.json.SubmitterContentJsonBuilder;
import static dk.dbc.dataio.flowstore.ejb.SubmittersBeanTest.newSubmittersBeanWithMockedEntityManager;
import dk.dbc.dataio.flowstore.entity.Submitter;
import dk.dbc.dataio.flowstore.util.ServiceUtil;
import java.util.Arrays;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Matchers.anyString;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
    JsonUtil.class,
    ServiceUtil.class})
public class SubmittersBeanTest {

    private static final EntityManager ENTITY_MANAGER = mock(EntityManager.class);
    private static final Long DEFAULT_TEST_ID = 23L;
    private static final Long DEFAULT_TEST_VERSION = 4L;
    private static final String DEFAULT_TEST_ETAG_VALUE = Long.toString(DEFAULT_TEST_VERSION);

    @Test
    public void submittersBean_validConstructor_newInstance() {
        final SubmittersBean submittersBean = newSubmittersBeanWithMockedEntityManager();
        assertThat(submittersBean, is(notNullValue()));
    }

    @Test(expected = NullPointerException.class)
    public void createSubmitter_nullSubmitterContent_throws() throws JsonException {
        newSubmittersBeanWithMockedEntityManager().createSubmitter(null, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void createSubmitter_emptySubmitterContent_throws() throws JsonException {
        newSubmittersBeanWithMockedEntityManager().createSubmitter(null, "");
    }

    @Test(expected = JsonException.class)
    public void createSubmitter_invalidJsonInSubmitterContent_throws() throws JsonException {
        newSubmittersBeanWithMockedEntityManager().createSubmitter(null, "Invalid JSON");
    }

    @Test
    public void createSubmitter_submitterCreated_returnsResponseWithHttpStatusOk_returnsSubmitter() throws JsonException, ReferencedEntityNotFoundException {
        final Long VERSION = 41L;
        final String ETAG_VALUE = "41";
        final UriInfo uriInfo = mock(UriInfo.class);
        final UriBuilder uriBuilder = mock(UriBuilder.class);
        final String submitterContent = new SubmitterContentJsonBuilder().build();
        final SubmittersBean submittersBean = newSubmittersBeanWithMockedEntityManager();
        final Submitter submitter = new Submitter();
        submitter.setVersion(VERSION);

        mockStatic(JsonUtil.class);
        mockStatic(ServiceUtil.class);
        when(uriInfo.getAbsolutePathBuilder()).thenReturn(uriBuilder);
        when(uriBuilder.path(anyString())).thenReturn(uriBuilder);
        when(JsonUtil.toJson(any(Submitter.class))).thenReturn("submitter");
        when(ServiceUtil.saveAsVersionedEntity(ENTITY_MANAGER, Submitter.class, submitterContent)).thenReturn(submitter);

        final Response response = submittersBean.createSubmitter(uriInfo, submitterContent);

        assertThat(response.getStatus(), is(Response.Status.CREATED.getStatusCode()));
        assertThat(response.hasEntity(), is(true));
        assertThat(response.getEntityTag().getValue(), is(ETAG_VALUE));
    }

    /*
    @Test
    public void getSubmitter_submitterFound_returnsResponseWithHttpStatusOK() throws JsonException {
        final SubmittersBean submittersBean = newSubmittersBeanWithMockedEntityManager();
        final Submitter submitter = new Submitter();
        final String submitterName = "testSubmitter";
        final Long submitterNumber = 555555L;
        submitter.setContent(new SubmitterContentJsonBuilder().setName(submitterName).setNumber(submitterNumber).build());
        submitter.setVersion(DEFAULT_TEST_VERSION);

        when(ENTITY_MANAGER.find(eq(Submitter.class), any())).thenReturn(submitter);

        Response response = submittersBean.getSubmitter(DEFAULT_TEST_ID);
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.hasEntity(), is(true));
        JsonNode entityNode = JsonUtil.getJsonRoot((String)response.getEntity());
        assertThat(entityNode.get("content").get("name").textValue(), is(submitterName));
        assertThat(entityNode.get("content").get("number").longValue(), is(submitterNumber));
        assertThat(response.getEntityTag().getValue(), is(DEFAULT_TEST_ETAG_VALUE));
    }

    @Test
    public void getSubmitter_noSubmitterFound_returnsResponseWithHttpStatusNotFound() throws JsonException {
        final SubmittersBean submittersBean = newSubmittersBeanWithMockedEntityManager();

        when(ENTITY_MANAGER.find(eq(Submitter.class), any())).thenReturn(null);

        Response response = submittersBean.getSubmitter(DEFAULT_TEST_ID);
        assertThat(response.getStatus(), is(Response.Status.NOT_FOUND.getStatusCode()));
        assertThat(response.getEntityTag(), is(nullValue()));
    }
    */

    @Test(expected = NullPointerException.class)
    public void updateSubmitter_nullSubmitterContent_throws() throws JsonException {
        newSubmittersBeanWithMockedEntityManager().updateSubmitter(null, 1L, 1L);
    }

    @Test(expected = IllegalArgumentException.class)
    public void updateSubmitter_emptySubmitterContent_throws() throws JsonException {
        newSubmittersBeanWithMockedEntityManager().updateSubmitter("", 1L, 1L);
    }

    @Test
    public void updateSubmitter_submitterNotFound_returnsResponseWithHttpStatusNotFound() throws JsonException, ReferencedEntityNotFoundException {
        final String submitterContent = new SubmitterContentJsonBuilder().setName("UpdateContentName").build();
        final SubmittersBean submittersBean = newSubmittersBeanWithMockedEntityManager();

        when(ENTITY_MANAGER.find(eq(Submitter.class), any())).thenReturn(null);

        final Response response = submittersBean.updateSubmitter(submitterContent, DEFAULT_TEST_ID, DEFAULT_TEST_VERSION);
        assertThat(response.getStatus(), is(Response.Status.NOT_FOUND.getStatusCode()));
    }

    @Test
    public void updateSubmitter_submitterFound_returnsResponseWithHttpStatusOk_returnsSubmitter() throws JsonException, ReferencedEntityNotFoundException {
        final Submitter submitter = mock(Submitter.class);
        final SubmittersBean submittersBean = newSubmittersBeanWithMockedEntityManager();
        final String submitterContent = new SubmitterContentJsonBuilder().build();

        mockStatic(JsonUtil.class);
        when(JsonUtil.toJson(submitter)).thenReturn("test");
        when(ENTITY_MANAGER.find(eq(Submitter.class), any())).thenReturn(submitter);
        when(submitter.getVersion()).thenReturn(DEFAULT_TEST_VERSION);

        final Response response = submittersBean.updateSubmitter(submitterContent, DEFAULT_TEST_ID, DEFAULT_TEST_VERSION);
        verify(submitter).setContent(submitterContent);
        verify(submitter).setVersion(DEFAULT_TEST_VERSION);
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.hasEntity(), is(true));
        assertThat(response.getEntityTag().getValue(), is(DEFAULT_TEST_ETAG_VALUE));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void findAllSubmitters_noSubmittersFound_returnsResponseWithHttpStatusOkAndSubmitterEntity() throws JsonException {
        final SubmittersBean submittersBean = newSubmittersBeanWithMockedEntityManager();
        final TypedQuery query = mock(TypedQuery.class);

        when(ENTITY_MANAGER.createNamedQuery(Submitter.QUERY_FIND_ALL, Submitter.class)).thenReturn(query);
        when(query.getResultList()).thenReturn(Arrays.asList());

        final Response response = submittersBean.findAllSubmitters();
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.hasEntity(), is(true));
        final ArrayNode entityNode = (ArrayNode) JsonUtil.getJsonRoot((String) response.getEntity());
        assertThat(entityNode.size(), is(0));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void findAllSubmitters_submittersFound_returnsResponseWithHttpStatusOkAndSubmitterEntities() throws JsonException {
        final SubmittersBean submittersBean = newSubmittersBeanWithMockedEntityManager();
        final TypedQuery query = mock(TypedQuery.class);
        final String nameSubmitterA = "A";
        final Submitter submitterA = new Submitter();
        submitterA.setContent(new SubmitterContentJsonBuilder()
                .setName(nameSubmitterA)
                .build());
        final String nameSubmitterB = "B";
        final Submitter submitterB = new Submitter();
        submitterB.setContent(new SubmitterContentJsonBuilder()
                .setName(nameSubmitterB)
                .build());

        when(ENTITY_MANAGER.createNamedQuery(Submitter.QUERY_FIND_ALL, Submitter.class)).thenReturn(query);
        when(query.getResultList()).thenReturn(Arrays.asList(submitterA, submitterB));

        final Response response = submittersBean.findAllSubmitters();
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.hasEntity(), is(true));
        final ArrayNode entityNode = (ArrayNode) JsonUtil.getJsonRoot((String) response.getEntity());
        assertThat(entityNode.size(), is(2));
        assertThat(entityNode.get(0).get("content").get("name").textValue(), is(nameSubmitterA));
        assertThat(entityNode.get(1).get("content").get("name").textValue(), is(nameSubmitterB));
    }

    public static SubmittersBean newSubmittersBeanWithMockedEntityManager() {
        final SubmittersBean submittersBean = new SubmittersBean();
        submittersBean.entityManager = ENTITY_MANAGER;
        return submittersBean;
    }
}
