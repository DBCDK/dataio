package dk.dbc.dataio.flowstore.ejb;

import com.fasterxml.jackson.databind.JsonNode;
import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.commons.types.FlowComponentContent;
import dk.dbc.dataio.commons.types.JavaScript;
import dk.dbc.dataio.commons.utils.test.json.FlowComponentContentJsonBuilder;
import dk.dbc.dataio.flowstore.entity.FlowComponent;
import jakarta.persistence.EntityManager;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class FlowComponentsBeanTest {

    private static final EntityManager ENTITY_MANAGER = mock(EntityManager.class);

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
    public void flowComponentsBean_validConstructor_newInstance() {
        FlowComponentsBean flowComponentsBean = newFlowComponentsBeanWithMockedEntityManager();
        assertThat(flowComponentsBean, is(notNullValue()));
    }

    @Test
    public void createFlowComponent_nullFlowComponentContent_throws() {
        assertThrows(NullPointerException.class, () -> newFlowComponentsBeanWithMockedEntityManager().createComponent(null, null));
    }

    @Test
    public void createFlowComponent_emptyFlowComponentContent_throws() throws JSONBException {
        assertThrows(IllegalArgumentException.class, () -> newFlowComponentsBeanWithMockedEntityManager().createComponent(null, ""));
    }

    @Test
    public void createFlowComponent_invalidJSON_throwsJsonException() throws JSONBException {
        assertThrows(JSONBException.class, () -> newFlowComponentsBeanWithMockedEntityManager().createComponent(null, "invalid Json"));
    }

    @Test
    public void getFlowComponent_noFlowComponentFound_returnsResponseWithHttpStatusNotFound() throws JSONBException {
        FlowComponentsBean flowComponentsBean = newFlowComponentsBeanWithMockedEntityManager();
        when(ENTITY_MANAGER.find(eq(FlowComponent.class), any())).thenReturn(null);
        Response response = flowComponentsBean.getFlowComponent(1L);
        assertThat(response.getStatus(), is(Response.Status.NOT_FOUND.getStatusCode()));
    }

    @Test
    public void getFlowComponent_flowComponentFound_returnsResponseWithHttpStatusOK() throws JSONBException {
        FlowComponent flowComponent = new FlowComponent();
        flowComponent.setContent(new FlowComponentContentJsonBuilder().setName("testFlowComponent").build());
        FlowComponentsBean flowComponentsBean = newFlowComponentsBeanWithMockedEntityManager();
        when(ENTITY_MANAGER.find(eq(FlowComponent.class), any())).thenReturn(flowComponent);

        Response response = flowComponentsBean.getFlowComponent(1L);
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.hasEntity(), is(true));
        JsonNode entityNode = jsonbContext.getJsonTree((String) response.getEntity());
        assertThat(entityNode.get("content").get("name").textValue(), is("testFlowComponent"));
    }

    @Test
    public void CreateFlowComponent_flowComponentCreated_returnsResponseWithHttpStatusOk_returnsSink() throws JSONBException {
        UriInfo uriInfo = mock(UriInfo.class);
        UriBuilder uriBuilder = mock(UriBuilder.class);
        FlowComponentContent flowComponentContent = new FlowComponentContent("CreateContentName", "svnProjectForInvocationJavascript", 1L, "invocationJavascriptName", new ArrayList<JavaScript>(), "invocationMethod", "RequireCach");
        String flowComponentContentString = new FlowComponentContentJsonBuilder().setName("CreateContentName").build();
        FlowComponentsBean flowComponentsBean = newFlowComponentsBeanWithMockedEntityManager();

        when(uriInfo.getAbsolutePathBuilder()).thenReturn(uriBuilder);
        when(uriBuilder.path(anyString())).thenReturn(uriBuilder);
        JSONBContext mockedJSONBContext = mock(JSONBContext.class);
        when(mockedJSONBContext.unmarshall(flowComponentContentString, FlowComponentContent.class)).thenReturn(flowComponentContent);
        when(mockedJSONBContext.marshall(any(FlowComponent.class))).thenReturn("flowComponent");

        Response response = flowComponentsBean.createComponent(uriInfo, flowComponentContentString);

        assertThat(response.getStatus(), is(Response.Status.CREATED.getStatusCode()));
        assertThat(response.hasEntity(), is(true));
    }

    @Test
    public void updateFlowComponent_nullFlowComponentContent_throws() {
        assertThrows(NullPointerException.class, () -> newFlowComponentsBeanWithMockedEntityManager().updateFlowComponent(null, null, 0L, 0L));
    }

    @Test
    public void updateFlowComponent_emptyFlowComponentContent_throws() {
        assertThrows(IllegalArgumentException.class, () -> newFlowComponentsBeanWithMockedEntityManager().updateFlowComponent(null, "", 0L, 0L));
    }

    @Test
    public void updateFlowComponent_flowComponentNotFound_throwsException() throws JSONBException {
        FlowComponentsBean flowComponentsBean = newFlowComponentsBeanWithMockedEntityManager();
        when(ENTITY_MANAGER.find(eq(FlowComponent.class), any())).thenReturn(null);

        String flowComponentContent = new FlowComponentContentJsonBuilder().setName("UpdateContentName").build();
        Response response = flowComponentsBean.updateFlowComponent(null, flowComponentContent, 123L, 4321L);
        assertThat(response.getStatus(), is(Response.Status.NOT_FOUND.getStatusCode()));
    }

    @Test
    public void updateFlowComponent_flowComponentFound_returnsResponseWithHttpStatusOk_returnsFlowComponent() throws JSONBException {
        FlowComponent flowComponent = mock(FlowComponent.class);
        FlowComponentsBean flowComponentsBean = newFlowComponentsBeanWithMockedEntityManager();
        flowComponentsBean.jsonbContext = mock(JSONBContext.class);
        when(flowComponentsBean.jsonbContext.marshall(flowComponent)).thenReturn("test");
        when(ENTITY_MANAGER.find(eq(FlowComponent.class), any())).thenReturn(flowComponent);

        String flowComponentContent = new FlowComponentContentJsonBuilder().setName("UpdateContentName").build();
        Response response = flowComponentsBean.updateFlowComponent(mockedUriInfo, flowComponentContent, 123L, 4321L);

        verify(flowComponent).setContent(flowComponentContent);
        verify(flowComponent).setVersion(4321L);
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.hasEntity(), is(true));
    }

    @Test
    public void updateNext_flowComponentNotFound_throwsException() throws JSONBException {
        FlowComponentsBean flowComponentsBean = newFlowComponentsBeanWithMockedEntityManager();
        when(ENTITY_MANAGER.find(eq(FlowComponent.class), any())).thenReturn(null);

        String flowComponentContent = new FlowComponentContentJsonBuilder().setName("UpdateContentName").build();
        Response response = flowComponentsBean.updateNext(null, flowComponentContent, 123L, 4321L);
        assertThat(response.getStatus(), is(Response.Status.NOT_FOUND.getStatusCode()));
    }


    @Test
    public void updateNext_nextIsNull_returnsResponseWithHttpStatusOk() throws JSONBException {
        FlowComponent flowComponent = mock(FlowComponent.class);
        FlowComponentsBean flowComponentsBean = newFlowComponentsBeanWithMockedEntityManager();

        flowComponentsBean.jsonbContext = mock(JSONBContext.class);
        when(flowComponentsBean.jsonbContext.marshall(flowComponent)).thenReturn("test");
        when(ENTITY_MANAGER.find(eq(FlowComponent.class), any())).thenReturn(flowComponent);

        Response response = flowComponentsBean.updateNext(mockedUriInfo, null, 123L, 4321L);

        verify(flowComponent).setNext(null);
        verify(flowComponent).setVersion(4321L);
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.hasEntity(), is(true));
    }

    @Test
    public void updateNext_nextIsNotNull_returnsResponseWithHttpStatusOk_returnsFlowComponent() throws JSONBException {
        FlowComponent flowComponent = mock(FlowComponent.class);
        FlowComponentsBean flowComponentsBean = newFlowComponentsBeanWithMockedEntityManager();

        flowComponentsBean.jsonbContext = mock(JSONBContext.class);
        when(flowComponentsBean.jsonbContext.marshall(flowComponent)).thenReturn("test");
        when(ENTITY_MANAGER.find(eq(FlowComponent.class), any())).thenReturn(flowComponent);

        String flowComponentContent = new FlowComponentContentJsonBuilder().build();
        Response response = flowComponentsBean.updateNext(mockedUriInfo, flowComponentContent, 123L, 4321L);

        verify(flowComponent).setNext(flowComponentContent);
        verify(flowComponent).setVersion(4321L);
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.hasEntity(), is(true));
    }

    public static FlowComponentsBean newFlowComponentsBeanWithMockedEntityManager() {
        FlowComponentsBean flowComponentsBean = new FlowComponentsBean();
        flowComponentsBean.entityManager = ENTITY_MANAGER;
        return flowComponentsBean;
    }

}
