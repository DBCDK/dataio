package dk.dbc.dataio.flowstore.ejb;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import dk.dbc.dataio.commons.types.FlowContent;
import dk.dbc.dataio.commons.types.exceptions.ReferencedEntityNotFoundException;
import dk.dbc.dataio.commons.utils.json.JsonException;
import dk.dbc.dataio.commons.utils.json.JsonUtil;
import dk.dbc.dataio.commons.utils.test.json.FlowContentJsonBuilder;
import dk.dbc.dataio.commons.utils.test.model.FlowComponentBuilder;
import dk.dbc.dataio.commons.utils.test.model.FlowContentBuilder;
import dk.dbc.dataio.flowstore.entity.Flow;
import dk.dbc.dataio.flowstore.entity.FlowComponent;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;


@RunWith(PowerMockRunner.class)
@PrepareForTest({
        JsonUtil.class,})
public class FlowsBeanTest {

    private static final EntityManager ENTITY_MANAGER = mock(EntityManager.class);

    private static final Long DEFAULT_TEST_ID = 23L;
    private static final Long DEFAULT_TEST_VERSION = 4L;
    private static final String DEFAULT_TEST_ETAG_VALUE = Long.toString(DEFAULT_TEST_VERSION);

    @Test
    public void flowsBean_validConstructor_newInstance() {
        final FlowsBean flowsBean = newFlowsBeanWithMockedEntityManager();
        assertThat(flowsBean, is(notNullValue()));
    }

    @Test(expected = NullPointerException.class)
    public void createFlow_nullFlowContent_throws() throws JsonException {
        newFlowsBeanWithMockedEntityManager().createFlow(null, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void createFlow_emptyFlowContent_throws() throws JsonException {
        newFlowsBeanWithMockedEntityManager().createFlow(null, "");
    }

    @Test(expected = JsonException.class)
    public void createFlow_invalidJSON_throwsJsonException() throws JsonException {
        newFlowsBeanWithMockedEntityManager().createFlow(null, "invalid Json");
    }

    @Test
    public void getFlow_noFlowFound_returnsResponseWithHttpStatusNotFound() throws JsonException {
        final FlowsBean flowsBean = newFlowsBeanWithMockedEntityManager();
        when(ENTITY_MANAGER.find(eq(Flow.class), any())).thenReturn(null);
        Response response = flowsBean.getFlow(1L);
        assertThat(response.getStatus(), is(Response.Status.NOT_FOUND.getStatusCode()));
    }

    @Test
    public void getFlow_flowFound_returnsResponseWithHttpStatusOK() throws JsonException {
        final Flow flow = new Flow();
        flow.setContent(new FlowContentJsonBuilder().setName("testFlow").build());
        final FlowsBean flowsBean = newFlowsBeanWithMockedEntityManager();
        when(ENTITY_MANAGER.find(eq(Flow.class), any())).thenReturn(flow);

        Response response = flowsBean.getFlow(1L);
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.hasEntity(), is(true));
        JsonNode entityNode = JsonUtil.getJsonRoot((String)response.getEntity());
        assertThat(entityNode.get("content").get("name").textValue(), is("testFlow"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void findAllFlows_noFlowsFound_returnsResponseWithHttpStatusOk() throws JsonException {
        final TypedQuery query = mock(TypedQuery.class);
        when(ENTITY_MANAGER.createNamedQuery(Flow.QUERY_FIND_ALL, Flow.class)).thenReturn(query);
        when(query.getResultList()).thenReturn(Arrays.asList());

        final FlowsBean flowsBean = newFlowsBeanWithMockedEntityManager();
        final Response response = flowsBean.findAllFlows();
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.hasEntity(), is(true));
        final ArrayNode entityNode = (ArrayNode) JsonUtil.getJsonRoot((String) response.getEntity());
        assertThat(entityNode.size(), is(0));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void findAllFlows_FlowsFound_returnsResponseWithHttpStatusOk() throws JsonException {

        final String nameFlowA = "A";
        final Flow flowA = new Flow();
        flowA.setContent(new FlowContentJsonBuilder()
                .setName(nameFlowA)
                .setDescription("Flow A description")
                .build());
        final String nameFlowB = "B";
        final Flow flowB = new Flow();
        flowB.setContent(new FlowContentJsonBuilder()
                .setName(nameFlowB)
                .setDescription("Flow B description")
                .build());

        final TypedQuery query = mock(TypedQuery.class);
        when(ENTITY_MANAGER.createNamedQuery(Flow.QUERY_FIND_ALL, Flow.class)).thenReturn(query);
        when(query.getResultList()).thenReturn(Arrays.asList(flowA, flowB));

        final FlowsBean flowsBean = newFlowsBeanWithMockedEntityManager();
        final Response response = flowsBean.findAllFlows();
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.hasEntity(), is(true));
        final ArrayNode entityNode = (ArrayNode) JsonUtil.getJsonRoot((String) response.getEntity());
        assertThat(entityNode.size(), is(2));
        assertThat(entityNode.get(0).get("content").get("name").textValue(), is(nameFlowA));
        assertThat(entityNode.get(1).get("content").get("name").textValue(), is(nameFlowB));
    }

    @Test
    public void CreateFlow_flowCreated_returnsResponseWithHttpStatusOk_returnsFlow() throws JsonException, ReferencedEntityNotFoundException {
        final UriInfo uriInfo = mock(UriInfo.class);
        final UriBuilder uriBuilder = mock(UriBuilder.class);
        final FlowContent flowContent = new FlowContent("CreateContentName", "CreateDescription", new ArrayList<dk.dbc.dataio.commons.types.FlowComponent>());
        final String flowContentString = new FlowContentJsonBuilder().setName("CreateContentName").build();
        final FlowsBean flowsBean = newFlowsBeanWithMockedEntityManager();

        when(uriInfo.getAbsolutePathBuilder()).thenReturn(uriBuilder);
        when(uriBuilder.path(anyString())).thenReturn(uriBuilder);
        mockStatic(JsonUtil.class);
        when(JsonUtil.fromJson(flowContentString, FlowContent.class)).thenReturn(flowContent);
        when(JsonUtil.toJson(any(Flow.class))).thenReturn("flow");

        final Response response = flowsBean.createFlow(uriInfo, flowContentString);

        assertThat(response.getStatus(), is(Response.Status.CREATED.getStatusCode()));
        assertThat(response.hasEntity(), is(true));
    }

    @Test(expected = ReferencedEntityNotFoundException.class)
    public void refreshFlowComponents_flowComponentNotFound_throwsException() throws JsonException, ReferencedEntityNotFoundException {
        final FlowsBean flowsBean = newFlowsBeanWithMockedEntityManager();
        final Flow flow = mock(Flow.class);
        mockStatic(JsonUtil.class);
        when(ENTITY_MANAGER.find(eq(Flow.class), any())).thenReturn(flow);

        final dk.dbc.dataio.commons.types.FlowComponent flowComponent = new FlowComponentBuilder().build();
        final FlowContent flowContent = new FlowContentBuilder()
                .setComponents(Collections.singletonList(flowComponent))
                .build();

        when(JsonUtil.fromJson(anyString(), eq(FlowContent.class))).thenReturn(flowContent);
        when(ENTITY_MANAGER.find(eq(FlowComponent.class), any())).thenReturn(null);
        flowsBean.updateFlow(null, null, 123L, 4321L, true);
    }

    @Test
    public void refreshFlowComponents_flowNotFound_throwsException() throws JsonException, ReferencedEntityNotFoundException {
        final FlowsBean flowsBean = newFlowsBeanWithMockedEntityManager();
        when(ENTITY_MANAGER.find(eq(Flow.class), any())).thenReturn(null);
        final Response response = flowsBean.updateFlow(null, null, 123L, 4321L, true);
        assertThat(response.getStatus(), is(Response.Status.NOT_FOUND.getStatusCode()));
    }

    @Test
    public void refreshFlowComponents_flowFound_returnsResponseWithHttpStatusOk_returnsFlow() throws JsonException, ReferencedEntityNotFoundException {
        final Flow flow = mock(Flow.class);
        final UriInfo uriInfo = mock(UriInfo.class);
        final UriBuilder uriBuilder = mock(UriBuilder.class);
        final FlowsBean flowsBean = newFlowsBeanWithMockedEntityManager();

        when(uriInfo.getAbsolutePathBuilder()).thenReturn(uriBuilder);
        when(uriBuilder.path(anyString())).thenReturn(uriBuilder);
        mockStatic(JsonUtil.class);
        when(JsonUtil.toJson(flow)).thenReturn("test");

        final dk.dbc.dataio.commons.types.FlowComponent flowComponent = new FlowComponentBuilder().build();
        final FlowContent flowContent = new FlowContentBuilder()
                .setComponents(Collections.singletonList(flowComponent))
                .build();

        when(JsonUtil.fromJson(anyString(), eq(FlowContent.class))).thenReturn(flowContent);
        when(JsonUtil.fromJson(anyString(), eq(dk.dbc.dataio.commons.types.FlowComponent.class))).thenReturn(flowComponent);
        when(JsonUtil.toJson(eq(flowComponent))).thenReturn("test");

        when(ENTITY_MANAGER.find(eq(dk.dbc.dataio.flowstore.entity.FlowComponent.class), any())).thenReturn(new FlowComponent());
        when(ENTITY_MANAGER.find(eq(Flow.class), any())).thenReturn(flow);

        final Response response = flowsBean.updateFlow("", uriInfo, DEFAULT_TEST_ID, DEFAULT_TEST_VERSION, true);

        verify(flow).setContent(JsonUtil.toJson(flowContent));
        verify(flow).setVersion(DEFAULT_TEST_VERSION);

        // Verifying that the private method invoked is: updateFlowComponentsInFlowToLatestVersion.
        // The other method: updateFlowContent does not invoke flow.getContent().
        verify(flow, times(1)).getContent();
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.hasEntity(), is(true));
    }

    @Test(expected = NullPointerException.class)
    public void updateFlow_nullFlowContent_throws() throws JsonException, ReferencedEntityNotFoundException {
        newFlowsBeanWithMockedEntityManager().updateFlow(null, null, DEFAULT_TEST_ID, DEFAULT_TEST_VERSION, false);
    }

    @Test(expected = IllegalArgumentException.class)
    public void updateFlow_emptyFlowContent_throws() throws JsonException, ReferencedEntityNotFoundException {
        newFlowsBeanWithMockedEntityManager().updateFlow("", null, DEFAULT_TEST_ID, DEFAULT_TEST_VERSION, false);
    }

    @Test
    public void updateFlow_flowNotFound_returnsResponseWithHttpStatusNotFound() throws JsonException, ReferencedEntityNotFoundException {
        final String flowContent = new FlowContentJsonBuilder().setName("UpdateContentName").build();
        final FlowsBean flowsBean = newFlowsBeanWithMockedEntityManager();

        when(ENTITY_MANAGER.find(eq(Flow.class), any())).thenReturn(null);

        final Response response = flowsBean.updateFlow(flowContent, null, DEFAULT_TEST_ID, DEFAULT_TEST_VERSION, false);
        assertThat(response.getStatus(), is(Response.Status.NOT_FOUND.getStatusCode()));
    }

    @Test
    public void updateFlow_flowFound_returnsResponseWithHttpStatusOk_returnsFlow() throws JsonException, ReferencedEntityNotFoundException {
        final Flow flow = mock(Flow.class);
        final FlowsBean flowsBean = newFlowsBeanWithMockedEntityManager();
        final String flowContent = new FlowContentJsonBuilder().build();

        mockStatic(JsonUtil.class);
        when(JsonUtil.toJson(flow)).thenReturn("test");
        when(ENTITY_MANAGER.find(eq(Flow.class), any())).thenReturn(flow);
        when(flow.getVersion()).thenReturn(DEFAULT_TEST_VERSION);

        final Response response = flowsBean.updateFlow(flowContent, null, DEFAULT_TEST_ID, DEFAULT_TEST_VERSION, false);
        verify(flow).setContent(flowContent);
        verify(flow).setVersion(DEFAULT_TEST_VERSION);
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.hasEntity(), is(true));
        assertThat(response.getEntityTag().getValue(), is(DEFAULT_TEST_ETAG_VALUE));
    }

    @Test
    public void deleteFlow_flowNotFound_returnsResponseWithHttpStatusNotFound() throws JsonException, ReferencedEntityNotFoundException {
        final FlowsBean flowsBean = newFlowsBeanWithMockedEntityManager();
        when(ENTITY_MANAGER.find(eq(Flow.class), any())).thenReturn(null);

        final Response response = flowsBean.deleteFlow(12L, 1L);
        assertThat(response.getStatus(), is(Response.Status.NOT_FOUND.getStatusCode()));
    }

    @Test
    public void deleteFlow_flowFound_returnsNoContentHttpResponse() throws JsonException, ReferencedEntityNotFoundException {
        final Flow flow = mock(Flow.class);
        final FlowsBean flowsBean = newFlowsBeanWithMockedEntityManager();

        mockStatic(JsonUtil.class);
        when(JsonUtil.toJson(flow)).thenReturn("test");
        when(ENTITY_MANAGER.find(eq(Flow.class), any())).thenReturn(flow);
        when(ENTITY_MANAGER.merge(any(Flow.class))).thenReturn(flow);

        final Response response = flowsBean.deleteFlow(12L, 1L);

        verify(flow).setVersion(1L);
        verify(ENTITY_MANAGER).remove(flow);
        assertThat(response.getStatus(), is(Response.Status.NO_CONTENT.getStatusCode()));
    }


    public static FlowsBean newFlowsBeanWithMockedEntityManager() {
        final FlowsBean flowsBean = new FlowsBean();
        flowsBean.entityManager = ENTITY_MANAGER;
        return flowsBean;
    }
}
