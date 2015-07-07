package dk.dbc.dataio.flowstore.ejb;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import dk.dbc.dataio.commons.types.FlowComponentContent;
import dk.dbc.dataio.commons.types.JavaScript;
import dk.dbc.dataio.commons.types.exceptions.ReferencedEntityNotFoundException;
import dk.dbc.dataio.commons.utils.json.JsonException;
import dk.dbc.dataio.commons.utils.json.JsonUtil;
import dk.dbc.dataio.commons.utils.test.json.FlowComponentContentJsonBuilder;
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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
        JsonUtil.class,})
public class FlowComponentsBeanTest {

    private static final EntityManager ENTITY_MANAGER = mock(EntityManager.class);

    @Test
    public void flowComponentsBean_validConstructor_newInstance() {
        final FlowComponentsBean flowComponentsBean = newFlowComponentsBeanWithMockedEntityManager();
        assertThat(flowComponentsBean, is(notNullValue()));
    }

    @Test(expected = NullPointerException.class)
    public void createFlowComponent_nullFlowComponentContent_throws() throws JsonException {
        newFlowComponentsBeanWithMockedEntityManager().createComponent(null, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void createFlowComponent_emptyFlowComponentContent_throws() throws JsonException {
        newFlowComponentsBeanWithMockedEntityManager().createComponent(null, "");
    }

    @Test(expected = JsonException.class)
    public void createFlowComponent_invalidJSON_throwsJsonException() throws JsonException {
        newFlowComponentsBeanWithMockedEntityManager().createComponent(null, "invalid Json");
    }

    @Test
    public void getFlowComponent_noFlowComponentFound_returnsResponseWithHttpStatusNotFound() throws JsonException {
        final FlowComponentsBean flowComponentsBean = newFlowComponentsBeanWithMockedEntityManager();
        when(ENTITY_MANAGER.find(eq(FlowComponent.class), any())).thenReturn(null);
        Response response = flowComponentsBean.getFlowComponent(1L);
        assertThat(response.getStatus(), is(Response.Status.NOT_FOUND.getStatusCode()));
    }

    @Test
    public void getFlowComponent_flowComponentFound_returnsResponseWithHttpStatusOK() throws JsonException {
        final FlowComponent flowComponent = new FlowComponent();
        flowComponent.setContent(new FlowComponentContentJsonBuilder().setName("testFlowComponent").build());
        final FlowComponentsBean flowComponentsBean = newFlowComponentsBeanWithMockedEntityManager();
        when(ENTITY_MANAGER.find(eq(FlowComponent.class), any())).thenReturn(flowComponent);

        Response response = flowComponentsBean.getFlowComponent(1L);
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.hasEntity(), is(true));
        JsonNode entityNode = JsonUtil.getJsonRoot((String) response.getEntity());
        assertThat(entityNode.get("content").get("name").textValue(), is("testFlowComponent"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void findAllFlowComponents_noFlowComponentsFound_returnsResponseWithHttpStatusOk() throws JsonException {
        final TypedQuery query = mock(TypedQuery.class);
        when(ENTITY_MANAGER.createNamedQuery(FlowComponent.QUERY_FIND_ALL, FlowComponent.class)).thenReturn(query);
        when(query.getResultList()).thenReturn(Arrays.asList());

        final FlowComponentsBean flowComponentsBean = newFlowComponentsBeanWithMockedEntityManager();
        final Response response = flowComponentsBean.findAllComponents();
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.hasEntity(), is(true));
        final ArrayNode entityNode = (ArrayNode) JsonUtil.getJsonRoot((String) response.getEntity());
        assertThat(entityNode.size(), is(0));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void findAllFlowComponents_FlowComponentsFound_returnsResponseWithHttpStatusOk() throws JsonException {

        final String nameFlowComponentA = "A";
        final FlowComponent flowComponentA = new FlowComponent();
        flowComponentA.setContent(new FlowComponentContentJsonBuilder()
                .setInvocationJavascriptName("invocationJavascriptName")
                .setInvocationMethod("invocationMethod")
                .setJavascripts(new ArrayList<String>())
                .setName(nameFlowComponentA)
                .setSvnProjectForInvocationJavascript("svnProjectForInvocationJavascript")
                .setSvnRevision(1L)
                .build());
        final String nameFlowComponentB = "B";
        final FlowComponent flowComponentB = new FlowComponent();
        flowComponentB.setContent(new FlowComponentContentJsonBuilder()
                .setInvocationJavascriptName("invocationJavascriptName")
                .setInvocationMethod("invocationMethod")
                .setJavascripts(new ArrayList<String>())
                .setName(nameFlowComponentB)
                .setSvnProjectForInvocationJavascript("svnProjectForInvocationJavascript")
                .setSvnRevision(1L)
                .build());

        final TypedQuery query = mock(TypedQuery.class);
        when(ENTITY_MANAGER.createNamedQuery(FlowComponent.QUERY_FIND_ALL, FlowComponent.class)).thenReturn(query);
        when(query.getResultList()).thenReturn(Arrays.asList(flowComponentA, flowComponentB));

        final FlowComponentsBean flowComponentsBean = newFlowComponentsBeanWithMockedEntityManager();
        final Response response = flowComponentsBean.findAllComponents();
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.hasEntity(), is(true));
        final ArrayNode entityNode = (ArrayNode) JsonUtil.getJsonRoot((String) response.getEntity());
        assertThat(entityNode.size(), is(2));
        assertThat(entityNode.get(0).get("content").get("name").textValue(), is(nameFlowComponentA));
        assertThat(entityNode.get(1).get("content").get("name").textValue(), is(nameFlowComponentB));
    }

    @Test
    public void CreateFlowComponent_flowComponentCreated_returnsResponseWithHttpStatusOk_returnsSink() throws JsonException, ReferencedEntityNotFoundException {
        final UriInfo uriInfo = mock(UriInfo.class);
        final UriBuilder uriBuilder = mock(UriBuilder.class);
        final FlowComponentContent flowComponentContent = new FlowComponentContent("CreateContentName", "svnProjectForInvocationJavascript", 1L, "invocationJavascriptName", new ArrayList<JavaScript>(), "invocationMethod", "RequireCach");
        final String flowComponentContentString = new FlowComponentContentJsonBuilder().setName("CreateContentName").build();
        final FlowComponentsBean flowComponentsBean = newFlowComponentsBeanWithMockedEntityManager();

        when(uriInfo.getAbsolutePathBuilder()).thenReturn(uriBuilder);
        when(uriBuilder.path(anyString())).thenReturn(uriBuilder);
        mockStatic(JsonUtil.class);
        when(JsonUtil.fromJson(flowComponentContentString, FlowComponentContent.class)).thenReturn(flowComponentContent);
        when(JsonUtil.toJson(any(FlowComponent.class))).thenReturn("flowComponent");

        final Response response = flowComponentsBean.createComponent(uriInfo, flowComponentContentString);

        assertThat(response.getStatus(), is(Response.Status.CREATED.getStatusCode()));
        assertThat(response.hasEntity(), is(true));
    }

    @Test(expected = NullPointerException.class)
    public void updateFlowComponent_nullFlowComponentContent_throws() throws JsonException, ReferencedEntityNotFoundException {
        newFlowComponentsBeanWithMockedEntityManager().updateFlowComponent(null, null, 0L, 0L);
    }

    @Test(expected = IllegalArgumentException.class)
    public void updateFlowComponent_emptyFlowComponentContent_throws() throws JsonException, ReferencedEntityNotFoundException {
        newFlowComponentsBeanWithMockedEntityManager().updateFlowComponent(null, "", 0L, 0L);
    }

    @Test
    public void updateFlowComponent_flowComponentNotFound_throwsException() throws JsonException, ReferencedEntityNotFoundException {
        final FlowComponentsBean flowComponentsBean = newFlowComponentsBeanWithMockedEntityManager();
        when(ENTITY_MANAGER.find(eq(FlowComponent.class), any())).thenReturn(null);

        final String flowComponentContent = new FlowComponentContentJsonBuilder().setName("UpdateContentName").build();
        final Response response = flowComponentsBean.updateFlowComponent(null, flowComponentContent, 123L, 4321L);
        assertThat(response.getStatus(), is(Response.Status.NOT_FOUND.getStatusCode()));
    }

    @Test
    public void updateFlowComponent_flowComponentFound_returnsResponseWithHttpStatusOk_returnsFlowComponent() throws JsonException, ReferencedEntityNotFoundException {
        final FlowComponent flowComponent = mock(FlowComponent.class);
        final UriInfo uriInfo = mock(UriInfo.class);
        final UriBuilder uriBuilder = mock(UriBuilder.class);
        final FlowComponentsBean flowComponentsBean = newFlowComponentsBeanWithMockedEntityManager();

        when(uriInfo.getAbsolutePathBuilder()).thenReturn(uriBuilder);
        when(uriBuilder.path(anyString())).thenReturn(uriBuilder);
        mockStatic(JsonUtil.class);
        when(JsonUtil.toJson(flowComponent)).thenReturn("test");
        when(ENTITY_MANAGER.find(eq(FlowComponent.class), any())).thenReturn(flowComponent);

        final String flowComponentContent = new FlowComponentContentJsonBuilder().setName("UpdateContentName").build();
        final Response response = flowComponentsBean.updateFlowComponent(uriInfo, flowComponentContent, 123L, 4321L);

        verify(flowComponent).setContent(flowComponentContent);
        verify(flowComponent).setVersion(4321L);
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.hasEntity(), is(true));
    }

    @Test
    public void updateNext_flowComponentNotFound_throwsException() throws JsonException, ReferencedEntityNotFoundException {
        final FlowComponentsBean flowComponentsBean = newFlowComponentsBeanWithMockedEntityManager();
        when(ENTITY_MANAGER.find(eq(FlowComponent.class), any())).thenReturn(null);

        final String flowComponentContent = new FlowComponentContentJsonBuilder().setName("UpdateContentName").build();
        final Response response = flowComponentsBean.updateNext(null, flowComponentContent, 123L, 4321L);
        assertThat(response.getStatus(), is(Response.Status.NOT_FOUND.getStatusCode()));
    }


    @Test
    public void updateNext_nextIsNull_returnsResponseWithHttpStatusOk() throws JsonException, ReferencedEntityNotFoundException {
        final FlowComponent flowComponent = mock(FlowComponent.class);
        final UriInfo uriInfo = mock(UriInfo.class);
        final UriBuilder uriBuilder = mock(UriBuilder.class);
        final FlowComponentsBean flowComponentsBean = newFlowComponentsBeanWithMockedEntityManager();

        when(uriInfo.getAbsolutePathBuilder()).thenReturn(uriBuilder);
        when(uriBuilder.path(anyString())).thenReturn(uriBuilder);
        mockStatic(JsonUtil.class);
        when(JsonUtil.toJson(flowComponent)).thenReturn("test");
        when(ENTITY_MANAGER.find(eq(FlowComponent.class), any())).thenReturn(flowComponent);

        final Response response = flowComponentsBean.updateNext(uriInfo, null, 123L, 4321L);

        verify(flowComponent).setNext(null);
        verify(flowComponent).setVersion(4321L);
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.hasEntity(), is(true));
    }

    @Test
    public void updateNext_nextIsNotNull_returnsResponseWithHttpStatusOk_returnsFlowComponent() throws JsonException, ReferencedEntityNotFoundException {
        final FlowComponent flowComponent = mock(FlowComponent.class);
        final UriInfo uriInfo = mock(UriInfo.class);
        final UriBuilder uriBuilder = mock(UriBuilder.class);
        final FlowComponentsBean flowComponentsBean = newFlowComponentsBeanWithMockedEntityManager();

        when(uriInfo.getAbsolutePathBuilder()).thenReturn(uriBuilder);
        when(uriBuilder.path(anyString())).thenReturn(uriBuilder);
        mockStatic(JsonUtil.class);
        when(JsonUtil.toJson(flowComponent)).thenReturn("test");
        when(ENTITY_MANAGER.find(eq(FlowComponent.class), any())).thenReturn(flowComponent);

        final String flowComponentContent = new FlowComponentContentJsonBuilder().build();
        final Response response = flowComponentsBean.updateNext(uriInfo, flowComponentContent, 123L, 4321L);

        verify(flowComponent).setNext(flowComponentContent);
        verify(flowComponent).setVersion(4321L);
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.hasEntity(), is(true));
    }

    public static FlowComponentsBean newFlowComponentsBeanWithMockedEntityManager() {
        final FlowComponentsBean flowComponentsBean = new FlowComponentsBean();
        flowComponentsBean.entityManager = ENTITY_MANAGER;
        return flowComponentsBean;
    }

}
