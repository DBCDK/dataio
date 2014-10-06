package dk.dbc.dataio.common.utils.flowstore;

import dk.dbc.dataio.commons.types.FlowBinder;
import dk.dbc.dataio.commons.types.FlowBinderContent;
import dk.dbc.dataio.commons.types.rest.FlowStoreServiceConstants;
import dk.dbc.dataio.commons.utils.httpclient.HttpClient;
import dk.dbc.dataio.commons.utils.json.JsonException;
import dk.dbc.dataio.commons.utils.test.model.FlowBinderBuilder;
import dk.dbc.dataio.commons.utils.test.model.FlowBinderContentBuilder;
import dk.dbc.dataio.commons.utils.test.rest.MockedResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorTestHelper.CLIENT;
import static dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorTestHelper.FLOW_STORE_URL;
import static dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorTestHelper.newFlowStoreServiceConnector;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
        HttpClient.class,})
public class FlowStoreServiceConnector_FlowBinders_Test {

    @Before
    public void setup() throws Exception {
        mockStatic(HttpClient.class);
    }

    // **************************************** create flow binder tests ****************************************
    @Test(expected = NullPointerException.class)
    public void createFlowBinder_flowBinderContentArgIsNull_throws() throws FlowStoreServiceConnectorException {
        final FlowStoreServiceConnector instance = newFlowStoreServiceConnector();
        instance.createFlowBinder(null);
    }

    @Test
    public void createFlowBinder_flowBinderIsCreated_returnsFlowBinder() throws FlowStoreServiceConnectorException, JsonException {
        final FlowBinder expectedFlowBinder = new FlowBinderBuilder().build();
        final FlowBinder flowBinder = createFlowBinder_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.CREATED.getStatusCode(), expectedFlowBinder);
        assertThat(flowBinder, is(notNullValue()));
        assertThat(flowBinder.getId(), is(expectedFlowBinder.getId()));
    }

    @Test(expected = FlowStoreServiceConnectorException.class)
    public void createFlowBinder_responseWithUnexpectedStatusCode_throws() throws FlowStoreServiceConnectorException {
        createFlowBinder_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), "");
    }

    @Test(expected = FlowStoreServiceConnectorException.class)
    public void createFlowBinder_responseWithNullEntity_throws() throws FlowStoreServiceConnectorException {
        createFlowBinder_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.CREATED.getStatusCode(), null);
    }

    @Test(expected = FlowStoreServiceConnectorUnexpectedStatusCodeException.class)
     public void createFlowBinder_responseWithPrimaryKeyViolation_throws() throws FlowStoreServiceConnectorException {
        createFlowBinder_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.NOT_ACCEPTABLE.getStatusCode(), "");
    }

    @Test(expected = FlowStoreServiceConnectorUnexpectedStatusCodeException.class)
    public void createFlowBinder_responseWithPreconditionFailed_throws() throws FlowStoreServiceConnectorException {
        createFlowBinder_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.PRECONDITION_FAILED.getStatusCode(), "");
    }

    // Helper method
    private FlowBinder createFlowBinder_mockedHttpWithSpecifiedReturnErrorCode(int statusCode, Object returnValue) throws FlowStoreServiceConnectorException {
        final FlowBinderContent flowBinderContent = new FlowBinderContentBuilder().build();
        when(HttpClient.doPostWithJson(CLIENT, flowBinderContent, FLOW_STORE_URL, FlowStoreServiceConstants.FLOW_BINDERS))
                .thenReturn(new MockedResponse<>(statusCode, returnValue));
        final FlowStoreServiceConnector instance = newFlowStoreServiceConnector();
        return instance.createFlowBinder(flowBinderContent);
    }

    // *************************************** find all flow binders tests **************************************
    @Test
    public void findAllFlowBinders_flowBindersRetrieved_returnsFlowBinder() throws FlowStoreServiceConnectorException {
        final FlowBinderContent flowBinderContentA = new FlowBinderContentBuilder().setName("a").build();
        final FlowBinderContent flowBinderContentB = new FlowBinderContentBuilder().setName("b").build();
        final FlowBinder expectedFlowBinderResultA = new FlowBinderBuilder().setContent(flowBinderContentA).build();
        final FlowBinder expectedFlowBinderResultB = new FlowBinderBuilder().setContent(flowBinderContentB).build();
        List<FlowBinder> expectedFlowBinderResultList = Arrays.asList(expectedFlowBinderResultA, expectedFlowBinderResultB);
        final List<FlowBinder> flowBinderResultList = findAllFlowBinders_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.OK.getStatusCode(), expectedFlowBinderResultList);

        assertThat(flowBinderResultList, not(nullValue()));
        assertFalse(flowBinderResultList.isEmpty());
        assertThat(flowBinderResultList.size(), is(2));
        assertThat(flowBinderResultList.get(0), is(notNullValue()));
        assertThat(flowBinderResultList.get(1), is(notNullValue()));
    }

    @Test
    public void findAllFlowBinders_noResults() throws FlowStoreServiceConnectorException {
        List<FlowBinder> flowBinderResultList = findAllFlowBinders_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.OK.getStatusCode(), new ArrayList<FlowBinder>());
        assertThat(flowBinderResultList, is(notNullValue()));
        assertThat(flowBinderResultList.size(), is(0));
    }

    @Test(expected = FlowStoreServiceConnectorException.class)
    public void findAllFlowBinders_responseWithUnexpectedStatusCode_throws() throws FlowStoreServiceConnectorException {
        findAllFlowBinders_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), "");
    }

    @Test(expected = FlowStoreServiceConnectorException.class)
    public void findAllFlowBinders_noListReturned() throws FlowStoreServiceConnectorException {
        findAllFlowBinders_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.OK.getStatusCode(), null);
    }

    @Test(expected = FlowStoreServiceConnectorUnexpectedStatusCodeException.class)
    public void findAllFlowBinders_responseWithNotFound_throws() throws FlowStoreServiceConnectorException {
        findAllFlowBinders_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.NOT_FOUND.getStatusCode(), null);
    }

    // Helper method
    private List<FlowBinder> findAllFlowBinders_mockedHttpWithSpecifiedReturnErrorCode(int statusCode, Object returnValue) throws FlowStoreServiceConnectorException {
        when(HttpClient.doGet(CLIENT, FLOW_STORE_URL, FlowStoreServiceConstants.FLOW_BINDERS))
                .thenReturn(new MockedResponse<>(statusCode, returnValue));
        final FlowStoreServiceConnector instance = newFlowStoreServiceConnector();
        return instance.findAllFlowBinders();
    }
}
