package dk.dbc.dataio.common.utils.flowstore;

import static dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorTestHelper.CLIENT;
import static dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorTestHelper.FLOW_STORE_URL;
import static dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorTestHelper.ID;
import static dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorTestHelper.VERSION;
import static dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorTestHelper.newFlowStoreServiceConnector;
import dk.dbc.dataio.commons.types.FlowComponent;
import dk.dbc.dataio.commons.types.FlowComponentContent;
import dk.dbc.dataio.commons.types.rest.FlowStoreServiceConstants;
import dk.dbc.dataio.commons.utils.httpclient.HttpClient;
import dk.dbc.dataio.commons.utils.json.JsonException;
import dk.dbc.dataio.commons.utils.test.model.FlowComponentBuilder;
import dk.dbc.dataio.commons.utils.test.model.FlowComponentContentBuilder;
import dk.dbc.dataio.commons.utils.test.rest.MockedResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ws.rs.core.Response;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import static org.mockito.Matchers.anyVararg;
import static org.mockito.Matchers.eq;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
    HttpClient.class,})
public class FlowStoreServiceConnector_FlowComponents_Test {

    @Before
    public void setup() throws Exception {
        mockStatic(HttpClient.class);
        when(HttpClient.interpolatePathVariables(eq(FlowStoreServiceConstants.FLOW_COMPONENT_CONTENT), Matchers.<Map<String, String>>any()))
                .thenReturn("path");
    }

    // ************************************* create flow component tests *************************************
    @Test(expected = NullPointerException.class)
    public void createFlowComponent_flowComponentContentArgIsNull_throws() throws FlowStoreServiceConnectorException {
        final FlowStoreServiceConnector instance = newFlowStoreServiceConnector();
        instance.createFlowComponent(null);
    }

    @Test
    public void createFlowComponent_flowComponentIsCreated_returnsFlowComponent() throws FlowStoreServiceConnectorException, JsonException {
        final FlowComponent expectedFlowComponent = new FlowComponentBuilder().build();
        final FlowComponent flowComponent
                = createFlowComponent_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.CREATED.getStatusCode(), expectedFlowComponent);

        assertThat(flowComponent, is(notNullValue()));
        assertThat(flowComponent.getId(), is(expectedFlowComponent.getId()));
    }

    @Test(expected = FlowStoreServiceConnectorException.class)
    public void createFlowComponent_responseWithUnexpectedStatusCode_throws() throws FlowStoreServiceConnectorException {
        createFlowComponent_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), "");
    }

    @Test(expected = FlowStoreServiceConnectorException.class)
    public void createFlowComponent_responseWithNullEntity_throws() throws FlowStoreServiceConnectorException {
        createFlowComponent_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.CREATED.getStatusCode(), null);
    }

    @Test(expected = FlowStoreServiceConnectorUnexpectedStatusCodeException.class)
    public void createFlowComponent_responseWithPrimaryKeyViolation_throws() throws FlowStoreServiceConnectorException {
        createFlowComponent_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.NOT_ACCEPTABLE.getStatusCode(), "");
    }

    // Helper method
    private FlowComponent createFlowComponent_mockedHttpWithSpecifiedReturnErrorCode(int statusCode, Object returnValue) throws FlowStoreServiceConnectorException {
        final FlowComponentContent flowComponentContent = new FlowComponentContentBuilder().build();
        when(HttpClient.doPostWithJson(CLIENT, flowComponentContent, FLOW_STORE_URL, FlowStoreServiceConstants.FLOW_COMPONENTS))
                .thenReturn(new MockedResponse<>(statusCode, returnValue));
        final FlowStoreServiceConnector instance = newFlowStoreServiceConnector();
        return instance.createFlowComponent(flowComponentContent);
    }

    // *************************************** get flow component tests **************************************
    @Test
    public void getFlowComponent_flowComponentRetrieved_returnsFlowComponent() throws FlowStoreServiceConnectorException {
        final FlowComponent expectedFlowComponentResult = new FlowComponentBuilder().build();

        final FlowComponent flowComponentResult
                = getFlowComponent_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.OK.getStatusCode(), expectedFlowComponentResult, ID);

        assertThat(flowComponentResult, is(notNullValue()));
        assertThat(flowComponentResult.getId(), is(expectedFlowComponentResult.getId()));
    }

    @Test(expected = FlowStoreServiceConnectorException.class)
    public void getFlowComponent_responseWithUnexpectedStatusCode_throws() throws FlowStoreServiceConnectorException {
        getFlowComponent_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), "", ID);
    }

    @Test(expected = FlowStoreServiceConnectorException.class)
    public void getFlowComponent_responseWithNullEntity_throws() throws FlowStoreServiceConnectorException {
        getFlowComponent_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.OK.getStatusCode(), null, ID);
    }

    @Test(expected = FlowStoreServiceConnectorUnexpectedStatusCodeException.class)
    public void getFlowComponent_responseWithNotFound_throws() throws FlowStoreServiceConnectorException {
        getFlowComponent_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.NOT_FOUND.getStatusCode(), null, ID);
    }

    // Helper method
    private FlowComponent getFlowComponent_mockedHttpWithSpecifiedReturnErrorCode(int statusCode, Object returnValue, long id) throws FlowStoreServiceConnectorException {
        when(HttpClient.doGet(eq(CLIENT), eq(FLOW_STORE_URL), (String) anyVararg()))
                .thenReturn(new MockedResponse<>(statusCode, returnValue));
        final FlowStoreServiceConnector instance = newFlowStoreServiceConnector();
        return instance.getFlowComponent(id);
    }

    // ************************************** update flow component tests **************************************
    @Test
    public void updateFlowComponent_flowComponentIsUpdated_returnsFlowComponent() throws FlowStoreServiceConnectorException, JsonException {
        final FlowComponent flowComponentToUpdate = new FlowComponentBuilder().build();

        FlowComponent updatedFlowComponent
                = updateFlowComponent_mockedHttpWithSpecifiedReturnErrorCode(
                        Response.Status.OK.getStatusCode(),
                        flowComponentToUpdate,
                        flowComponentToUpdate.getId(),
                        flowComponentToUpdate.getVersion());

        assertThat(updatedFlowComponent, is(notNullValue()));
        assertThat(updatedFlowComponent.getContent(), is(notNullValue()));
        assertThat(updatedFlowComponent.getId(), is(flowComponentToUpdate.getId()));
    }

    @Test(expected = FlowStoreServiceConnectorException.class)
    public void updateFlowComponent_responseWithUnexpectedStatusCode_throws() throws FlowStoreServiceConnectorException {
        updateFlowComponent_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), "", ID, VERSION);
    }

    @Test(expected = FlowStoreServiceConnectorUnexpectedStatusCodeException.class)
    public void updateFlowComponent_responseWithPrimaryKeyViolation_throws() throws FlowStoreServiceConnectorException {
        updateFlowComponent_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.NOT_ACCEPTABLE.getStatusCode(), "", ID, VERSION);
    }

    @Test(expected = FlowStoreServiceConnectorUnexpectedStatusCodeException.class)
    public void updateFlowComponent_responseWithMultipleUpdatesConflict_throws() throws FlowStoreServiceConnectorException {
        updateFlowComponent_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.CONFLICT.getStatusCode(), "", ID, VERSION);
    }

    // Helper method
    private FlowComponent updateFlowComponent_mockedHttpWithSpecifiedReturnErrorCode(int statusCode, Object returnValue, long id, long version) throws FlowStoreServiceConnectorException {
        final FlowComponentContent flowComponentContent = new FlowComponentContentBuilder().build();
        final Map<String, String> headers = new HashMap<>(1);
        headers.put(FlowStoreServiceConstants.IF_MATCH_HEADER, "1");
        when(HttpClient.doPostWithJson(CLIENT, headers, flowComponentContent, FLOW_STORE_URL, "path"))
                .thenReturn(new MockedResponse<>(statusCode, returnValue));
        final FlowStoreServiceConnector instance = newFlowStoreServiceConnector();
        return instance.updateFlowComponent(flowComponentContent, id, version);
    }

    // *********************************** find all flow components tests ***********************************
    @Test
    public void findAllFlowComponents_flowComponentsRetrieved_returnsflowComponents() throws FlowStoreServiceConnectorException {

        final FlowComponentContent flowComponentContentA = new FlowComponentContentBuilder().setName("a").build();
        final FlowComponentContent flowComponentContentB = new FlowComponentContentBuilder().setName("b").build();
        final FlowComponent expectedFlowComponentResultA = new FlowComponentBuilder().setContent(flowComponentContentA).build();
        final FlowComponent expectedFlowComponentResultB = new FlowComponentBuilder().setContent(flowComponentContentB).build();

        List<FlowComponent> expectedFlowComponentResultList = new ArrayList<>();
        expectedFlowComponentResultList.add(expectedFlowComponentResultA);
        expectedFlowComponentResultList.add(expectedFlowComponentResultB);

        final List<FlowComponent> flowComponentResultList
                = getAllFlowComponents_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.OK.getStatusCode(), expectedFlowComponentResultList);

        assertNotNull(flowComponentResultList);
        assertFalse(flowComponentResultList.isEmpty());
        assertThat(flowComponentResultList.size(), is(2));

        for (FlowComponent flowComponent : flowComponentResultList) {
            assertThat(flowComponent, is(notNullValue()));
        }
    }

    @Test
    public void findAllFlowComponents_noResults() throws FlowStoreServiceConnectorException {
        List<FlowComponent> flowComponentResultList
                = getAllFlowComponents_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.OK.getStatusCode(), new ArrayList<FlowComponent>());
        assertThat(flowComponentResultList, is(notNullValue()));
        assertThat(flowComponentResultList.size(), is(0));
    }

    @Test(expected = FlowStoreServiceConnectorException.class)
    public void findAllFlowComponents_noListReturned() throws FlowStoreServiceConnectorException {
        getAllFlowComponents_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.OK.getStatusCode(), null);
    }

    @Test(expected = FlowStoreServiceConnectorException.class)
    public void findAllFlowComponents_responseWithUnexpectedStatusCode_throws() throws FlowStoreServiceConnectorException {
        getAllFlowComponents_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), "");
    }

    @Test(expected = FlowStoreServiceConnectorUnexpectedStatusCodeException.class)
    public void findAllFlowComponents_responseWithNotFound_throws() throws FlowStoreServiceConnectorException {
        getAllFlowComponents_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.NOT_FOUND.getStatusCode(), null);
    }

    // Helper method
    private List<FlowComponent> getAllFlowComponents_mockedHttpWithSpecifiedReturnErrorCode(int statusCode, Object returnValue) throws FlowStoreServiceConnectorException {
        when(HttpClient.doGet(eq(CLIENT), eq(FLOW_STORE_URL), (String) anyVararg()))
                .thenReturn(new MockedResponse<>(statusCode, returnValue));
        final FlowStoreServiceConnector instance = newFlowStoreServiceConnector();
        return instance.findAllFlowComponents();
    }
}
