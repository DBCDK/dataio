package dk.dbc.dataio.flowstore.ejb;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.commons.types.FlowContent;
import dk.dbc.dataio.commons.types.FlowView;
import dk.dbc.dataio.commons.types.exceptions.ReferencedEntityNotFoundException;
import dk.dbc.dataio.commons.utils.test.json.FlowContentJsonBuilder;
import dk.dbc.dataio.commons.utils.test.model.FlowComponentBuilder;
import dk.dbc.dataio.commons.utils.test.model.FlowContentBuilder;
import dk.dbc.dataio.flowstore.entity.Flow;
import dk.dbc.dataio.flowstore.entity.FlowComponent;
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
import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class FlowsBeanTest {
    private static final EntityManager ENTITY_MANAGER = mock(EntityManager.class);

    private static final Long DEFAULT_TEST_ID = 23L;
    private static final Long DEFAULT_TEST_VERSION = 4L;

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
    public void flowsBean_validConstructor_newInstance() {
        FlowsBean flowsBean = newFlowsBeanWithMockedEntityManager();
        assertThat(flowsBean, is(notNullValue()));
    }

    @Test
    public void createFlow_nullFlowContent_throws() throws JSONBException {
        assertThrows(NullPointerException.class, () -> newFlowsBeanWithMockedEntityManager().createFlow(null, null));
    }

    @Test
    public void createFlow_emptyFlowContent_throws() throws JSONBException {
        assertThrows(IllegalArgumentException.class, () -> newFlowsBeanWithMockedEntityManager().createFlow(null, ""));
    }

    @Test
    public void createFlow_invalidJSON_throwsJsonException() throws JSONBException {
        assertThrows(JSONBException.class, () -> newFlowsBeanWithMockedEntityManager().createFlow(mockedUriInfo, "invalid Json"));
    }

    @Test
    public void getFlow_noFlowFound_returnsResponseWithHttpStatusNotFound() throws JSONBException {
        FlowsBean flowsBean = newFlowsBeanWithMockedEntityManager();
        when(ENTITY_MANAGER.find(eq(Flow.class), any())).thenReturn(null);
        Response response = flowsBean.getFlow(1L);
        assertThat(response.getStatus(), is(Response.Status.NOT_FOUND.getStatusCode()));
    }

    @Test
    public void getFlow_flowFound_returnsResponseWithHttpStatusOK() throws JSONBException {
        Flow flow = new Flow();
        flow.setContent(new FlowContentJsonBuilder().setName("testFlow").build());
        FlowsBean flowsBean = newFlowsBeanWithMockedEntityManager();
        when(ENTITY_MANAGER.find(eq(Flow.class), any())).thenReturn(flow);

        Response response = flowsBean.getFlow(1L);
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.hasEntity(), is(true));
        JsonNode entityNode = jsonbContext.getJsonTree((String) response.getEntity());
        assertThat(entityNode.get("content").get("name").textValue(), is("testFlow"));
    }

    @Test
    public void findFlows_findFlowByNameFlowFound_returnsResponseWithHttpStatusOK() throws JSONBException {
        Flow flow = new Flow();
        flow.setContent(new FlowContentJsonBuilder().setName("testFlow").build());
        FlowsBean flowsBean = newFlowsBeanWithMockedEntityManager();
        TypedQuery<Flow> query = mock(TypedQuery.class);

        when(ENTITY_MANAGER.createNamedQuery(Flow.QUERY_FIND_BY_NAME, Flow.class)).thenReturn(query);
        when(query.setParameter(1, "testFlow")).thenReturn(query);
        when(query.getResultList()).thenReturn(Collections.singletonList(flow));

        Response response = flowsBean.findFlows("testFlow");
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.hasEntity(), is(true));
        ArrayNode entityNode = (ArrayNode) jsonbContext.getJsonTree((String) response.getEntity());
        assertThat(entityNode.size(), is(1));
        assertThat(entityNode.get(0).get("content").get("name").textValue(), is("testFlow"));
    }

    @Test
    public void findFlows_findFlowByNameFlowNotFound_returnsResponseWithHttpStatusNotFound() throws JSONBException {
        Flow flow = new Flow();
        flow.setContent(new FlowContentJsonBuilder().setName("testFlow").build());
        FlowsBean flowsBean = newFlowsBeanWithMockedEntityManager();
        TypedQuery<Flow> query = mock(TypedQuery.class);

        when(ENTITY_MANAGER.createNamedQuery(Flow.QUERY_FIND_BY_NAME, Flow.class)).thenReturn(query);
        when(query.setParameter(1, "testFlow")).thenReturn(query);
        when(query.getResultList()).thenReturn(Collections.emptyList());

        Response response = flowsBean.findFlows("testFlow");
        assertThat(response.getStatus(), is(Response.Status.NOT_FOUND.getStatusCode()));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void findFlows_findAllFlowsFlowsNotFound_returnsResponseWithHttpStatusOk() throws JSONBException {
        TypedQuery query = mock(TypedQuery.class);
        when(ENTITY_MANAGER.createNamedQuery(Flow.QUERY_FIND_ALL, String.class)).thenReturn(query);
        when(query.getResultList()).thenReturn(Collections.emptyList());

        FlowsBean flowsBean = newFlowsBeanWithMockedEntityManager();
        Response response = flowsBean.findFlows(null);
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.hasEntity(), is(true));
        ArrayNode entityNode = (ArrayNode) jsonbContext.getJsonTree((String) response.getEntity());
        assertThat(entityNode.size(), is(0));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void findFlows_findAllFlowsFlowsFound_returnsResponseWithHttpStatusOk() throws JSONBException {

        final String nameFlowA = "A";
        FlowView flowA = new FlowView()
                .withId(1)
                .withName(nameFlowA)
                .withDescription("Flow A description");
        final String nameFlowB = "B";
        FlowView flowB = new FlowView()
                .withId(2)
                .withName(nameFlowB)
                .withDescription("Flow B description");

        TypedQuery query = mock(TypedQuery.class);

        when(ENTITY_MANAGER.createNamedQuery(Flow.QUERY_FIND_ALL, String.class)).thenReturn(query);
        when(query.getResultList()).thenReturn(Arrays.asList(jsonbContext.marshall(flowA), jsonbContext.marshall(flowB)));

        FlowsBean flowsBean = newFlowsBeanWithMockedEntityManager();

        Response response = flowsBean.findFlows(null);

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.hasEntity(), is(true));
        ArrayNode entityNode = (ArrayNode) jsonbContext.getJsonTree((String) response.getEntity());
        assertThat(entityNode.size(), is(2));
        assertThat(entityNode.get(0).get("name").textValue(), is(nameFlowA));
        assertThat(entityNode.get(1).get("name").textValue(), is(nameFlowB));
    }

    @Test
    public void CreateFlow_flowCreated_returnsResponseWithHttpStatusOk_returnsFlow() throws JSONBException {

        String flowContentString = new FlowContentJsonBuilder().setName("CreateContentName").build();
        FlowsBean flowsBean = newFlowsBeanWithMockedEntityManager();

        Response response = flowsBean.createFlow(mockedUriInfo, flowContentString);

        assertThat(response.getStatus(), is(Response.Status.CREATED.getStatusCode()));
        assertThat(response.hasEntity(), is(true));
    }

    @Test
    public void refreshFlowComponents_flowComponentNotFound_throwsException() throws JSONBException {
        FlowsBean flowsBean = newFlowsBeanWithMockedEntityManager();
        flowsBean.jsonbContext = mock(JSONBContext.class);
        Flow flow = mock(Flow.class);

        when(ENTITY_MANAGER.find(eq(Flow.class), any())).thenReturn(flow);
        when(flow.getContent()).thenReturn("{}");

        dk.dbc.dataio.commons.types.FlowComponent flowComponent = new FlowComponentBuilder().build();
        FlowContent flowContent = new FlowContentBuilder()
                .setComponents(Collections.singletonList(flowComponent))
                .build();

        when(flowsBean.jsonbContext.unmarshall(anyString(), eq(FlowContent.class))).thenReturn(flowContent);
        when(ENTITY_MANAGER.find(eq(FlowComponent.class), any())).thenReturn(null);

        String flowContentJSON = new JSONBContext().marshall(flowContent);
        assertThrows(ReferencedEntityNotFoundException.class, () -> flowsBean.updateFlow(flowContentJSON, null, 123L, 4321L, true));
    }

    @Test
    public void refreshFlowComponents_flowNotFound_throwsException() throws JSONBException, ReferencedEntityNotFoundException {
        FlowsBean flowsBean = newFlowsBeanWithMockedEntityManager();

        when(ENTITY_MANAGER.find(eq(Flow.class), any())).thenReturn(null);

        Response response = flowsBean.updateFlow(createEmptyFlowContentJSON(), null, 123L, 4321L, true);
        assertThat(response.getStatus(), is(Response.Status.NOT_FOUND.getStatusCode()));
    }

    @Test
    public void refreshFlowComponents_flowFound_returnsResponseWithHttpStatusOk_returnsFlow() throws JSONBException, ReferencedEntityNotFoundException {
        Flow flow = mock(Flow.class);
        FlowsBean flowsBean = newFlowsBeanWithMockedEntityManager();
        flowsBean.jsonbContext = mock(JSONBContext.class);

        when(flowsBean.jsonbContext.marshall(flow)).thenReturn("test");

        dk.dbc.dataio.commons.types.FlowComponent flowComponent = new FlowComponentBuilder().build();
        FlowContent flowContent = new FlowContentBuilder()
                .setComponents(Collections.singletonList(flowComponent))
                .build();

        when(flowsBean.jsonbContext.unmarshall(anyString(), eq(FlowContent.class))).thenReturn(flowContent);
        when(flowsBean.jsonbContext.unmarshall(anyString(), eq(dk.dbc.dataio.commons.types.FlowComponent.class))).thenReturn(flowComponent);
        when(flowsBean.jsonbContext.marshall(eq(flowComponent))).thenReturn("test");

        FlowComponent persistedFlowComponent = mock(FlowComponent.class);
        when(ENTITY_MANAGER.find(eq(dk.dbc.dataio.flowstore.entity.FlowComponent.class), any())).thenReturn(persistedFlowComponent);
        when(persistedFlowComponent.getVersion()).thenReturn(flowComponent.getVersion() + 1);
        when(ENTITY_MANAGER.find(eq(Flow.class), any())).thenReturn(flow);
        when(flow.getContent()).thenReturn("{}");

        Response response = flowsBean.updateFlow(createEmptyFlowContentJSON(), mockedUriInfo, DEFAULT_TEST_ID, DEFAULT_TEST_VERSION, true);

        verify(flow).setContent(flowsBean.jsonbContext.marshall(flowContent));

        // Verifying that the private method invoked is: updateFlowComponentsInFlowToLatestVersion.
        // The other method: updateFlowContent does not invoke flow.getContent().
        verify(flow, times(1)).getContent();
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.hasEntity(), is(true));
    }

    @Test
    public void updateFlow_nullFlowContent_throws() {
        assertThrows(NullPointerException.class, () -> newFlowsBeanWithMockedEntityManager().updateFlow(null, null, DEFAULT_TEST_ID, DEFAULT_TEST_VERSION, false));
    }

    @Test
    public void updateFlow_emptyFlowContent_throws() {
        assertThrows(IllegalArgumentException.class, () -> newFlowsBeanWithMockedEntityManager().updateFlow("", null, DEFAULT_TEST_ID, DEFAULT_TEST_VERSION, false));
    }

    @Test
    public void updateFlow_flowNotFound_returnsResponseWithHttpStatusNotFound() throws JSONBException, ReferencedEntityNotFoundException {
        String flowContent = new FlowContentJsonBuilder().setName("UpdateContentName").build();
        FlowsBean flowsBean = newFlowsBeanWithMockedEntityManager();

        when(ENTITY_MANAGER.find(eq(Flow.class), any())).thenReturn(null);

        Response response = flowsBean.updateFlow(flowContent, null, DEFAULT_TEST_ID, DEFAULT_TEST_VERSION, false);
        assertThat(response.getStatus(), is(Response.Status.NOT_FOUND.getStatusCode()));
    }

    @Test
    public void deleteFlow_flowNotFound_returnsResponseWithHttpStatusNotFound() {
        FlowsBean flowsBean = newFlowsBeanWithMockedEntityManager();
        when(ENTITY_MANAGER.find(eq(Flow.class), any())).thenReturn(null);

        Response response = flowsBean.deleteFlow(12L, 1L);
        assertThat(response.getStatus(), is(Response.Status.NOT_FOUND.getStatusCode()));
    }

    @Test
    public void deleteFlow_flowFound_returnsNoContentHttpResponse() {
        Flow flow = mock(Flow.class);
        FlowsBean flowsBean = newFlowsBeanWithMockedEntityManager();

        when(ENTITY_MANAGER.find(eq(Flow.class), any())).thenReturn(flow);
        when(ENTITY_MANAGER.merge(any(Flow.class))).thenReturn(flow);

        Response response = flowsBean.deleteFlow(12L, 1L);

        verify(ENTITY_MANAGER).remove(flow);
        assertThat(response.getStatus(), is(Response.Status.NO_CONTENT.getStatusCode()));
    }

    private String createEmptyFlowContentJSON() {
        try {
            dk.dbc.dataio.commons.types.FlowComponent flowComponent = new FlowComponentBuilder().build();
            FlowContent flowContent = new FlowContentBuilder()
                    .setComponents(Collections.singletonList(flowComponent))
                    .build();
            return jsonbContext.marshall(flowContent);
        } catch (JSONBException e) {
            fail("Internal error, should not happen");
        }
        return "";
    }

    public static FlowsBean newFlowsBeanWithMockedEntityManager() {
        FlowsBean flowsBean = new FlowsBean() {
            protected FlowsBean self() {
                return this;
            }
        };
        flowsBean.entityManager = ENTITY_MANAGER;
        return flowsBean;
    }
}
