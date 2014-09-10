package dk.dbc.dataio.gui.server;

import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorUnexpectedStatusCodeException;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.FlowBinder;
import dk.dbc.dataio.commons.types.FlowComponent;
import dk.dbc.dataio.commons.types.FlowComponentContent;
import dk.dbc.dataio.commons.types.FlowContent;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.SinkContent;
import dk.dbc.dataio.commons.types.Submitter;
import dk.dbc.dataio.commons.types.SubmitterContent;
import dk.dbc.dataio.commons.types.rest.FlowStoreServiceConstants;
import dk.dbc.dataio.commons.utils.httpclient.HttpClient;
import dk.dbc.dataio.commons.utils.service.ServiceUtil;
import dk.dbc.dataio.commons.utils.test.model.FlowBinderBuilder;
import dk.dbc.dataio.commons.utils.test.model.FlowBuilder;
import dk.dbc.dataio.commons.utils.test.model.FlowComponentBuilder;
import dk.dbc.dataio.commons.utils.test.model.FlowComponentContentBuilder;
import dk.dbc.dataio.commons.utils.test.model.FlowContentBuilder;
import dk.dbc.dataio.commons.utils.test.model.SinkBuilder;
import dk.dbc.dataio.commons.utils.test.model.SubmitterBuilder;
import dk.dbc.dataio.gui.client.exceptions.ProxyError;
import dk.dbc.dataio.gui.client.exceptions.ProxyException;
import dk.dbc.dataio.gui.client.pages.flow.modify.FlowModel;
import dk.dbc.dataio.gui.client.pages.sink.modify.SinkModel;
import dk.dbc.dataio.gui.client.pages.submitter.modify.SubmitterModel;
import org.glassfish.jersey.client.ClientConfig;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.naming.NamingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
        HttpClient.class,
        ServiceUtil.class
})
public class FlowStoreProxyImplTest {
    private final String flowStoreServiceUrl = "http://dataio/flow-service";
    private final Client client = mock(Client.class);
    private final static long ID = 1L;

    @Before
    public void setup() throws Exception {
        mockStatic(ServiceUtil.class);
        mockStatic(HttpClient.class);
        when(ServiceUtil.getFlowStoreServiceEndpoint()).thenReturn(flowStoreServiceUrl);
        when(HttpClient.newClient(any(ClientConfig.class))).thenReturn(client);
    }

    @Test
    public void noArgs_flowStoreProxyConstructorFlowStoreService_EndpointCanNotBeLookedUp_throws() throws Exception{
        when(ServiceUtil.getFlowStoreServiceEndpoint()).thenThrow(new NamingException());
        try{
            new FlowStoreProxyImpl();
            fail();
        }catch (NamingException e){
        }
    }

    @Test
    public void oneArg_flowStoreProxyConstructorFlowStoreService_EndpointCanNotBeLookedUp_throws1() throws Exception{
        final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        when(ServiceUtil.getFlowStoreServiceEndpoint()).thenThrow(new NamingException());
        try{
            new FlowStoreProxyImpl(flowStoreServiceConnector);
            fail();
        }catch (NamingException e){
        }
    }

    private void createSink_genericTestImplForHttpErrors(int errorCodeToReturn, ProxyError expectedError, String expectedErrorName) throws Exception {
        final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl(flowStoreServiceConnector);

        when(flowStoreServiceConnector.createSink(any(SinkContent.class)))
                .thenThrow(new FlowStoreServiceConnectorUnexpectedStatusCodeException("DIED", errorCodeToReturn));
        try {
            flowStoreProxy.createSink(getDefaultSinkModel(0, 0));
            fail("No " + expectedErrorName + " error was thrown by createSink()");
        } catch (ProxyException e) {
            assertThat(e.getErrorCode(), is(expectedError));
        }
    }

    private void createSink_testForProxyError(SinkModel model, Exception exception, ProxyError expectedError, String expectedErrorName) throws Exception {
        final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl(flowStoreServiceConnector);

        when(flowStoreServiceConnector.createSink(any(SinkContent.class)))
                .thenThrow(exception);

        try {
            flowStoreProxy.createSink(model);
            fail("No " + expectedErrorName + " error was thrown by createSink()");
        } catch (ProxyException e) {
            assertThat(e.getErrorCode(), is(expectedError));
        }
    }

    /*
    * Test createSink
    */

    @Test
    public void createSink_remoteServiceReturnsHttpStatusCreated_returnsSinkModelEntity() throws Exception {
        final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl(flowStoreServiceConnector);
        final Sink sink = new SinkBuilder().build();
        when(flowStoreServiceConnector.createSink(any(SinkContent.class))).thenReturn(sink);

        try {
            final SinkModel createdModel  = flowStoreProxy.createSink(getDefaultSinkModel(0, 0));
            assertNotNull(createdModel);
        } catch (ProxyException e) {
            fail("Unexpected error when calling: createSink()");
        }
    }

    @Test
    public void createSink_remoteServiceReturnsHttpStatusInternalServerError_throws() throws Exception {
        createSink_genericTestImplForHttpErrors(500, ProxyError.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR");
    }

    @Test
    public void createSink_remoteServiceReturnsHttpStatusNotAcceptable_throws() throws Exception {
        createSink_genericTestImplForHttpErrors(406, ProxyError.NOT_ACCEPTABLE, "NOT_ACCEPTABLE");
    }

    @Test
    public void createSink_throwsIllegalArgumentException() throws Exception {
        IllegalArgumentException illegalArgumentException = new IllegalArgumentException("DIED");
        SinkModel model = getDefaultSinkModel(0, 0);
        model.setSinkName("");
        createSink_testForProxyError(model, illegalArgumentException, ProxyError.MODEL_MAPPER_EMPTY_FIELDS, "MODEL_MAPPER_EMPTY_FIELDS");
    }


    /*
    * Test getSink
    */

    @Test
    public void getSink_remoteServiceReturnsHttpStatusOk_returnsSinkModelEntity() throws Exception {
        final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl(flowStoreServiceConnector);
        final Sink sink = new SinkBuilder().setId(ID).build();
        when(flowStoreServiceConnector.getSink(eq(ID))).thenReturn(sink);

        try {
            final SinkModel retrievedModel = flowStoreProxy.getSink(sink.getId());
            assertNotNull(retrievedModel);
        } catch (ProxyException e) {
            fail("Unexpected error when calling: getSink()");
        }
    }

    @Test
    public void getSink_remoteServiceReturnsHttpStatusInternalServerError_throws() throws Exception {
        getSink_genericTestImplForHttpErrors(500, ProxyError.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR");
    }

    @Test
    public void getSink_remoteServiceReturnsHttpStatusNotFound_throws() throws Exception {
        getSink_genericTestImplForHttpErrors(404, ProxyError.ENTITY_NOT_FOUND, "ENTITY_NOT_FOUND");
    }

    private void getSink_genericTestImplForHttpErrors(int errorCodeToReturn, ProxyError expectedError, String expectedErrorName) throws Exception {
        final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl(flowStoreServiceConnector);
        when(flowStoreServiceConnector.getSink(eq(ID))).thenThrow(new FlowStoreServiceConnectorUnexpectedStatusCodeException("DIED", errorCodeToReturn));

        try {
            flowStoreProxy.getSink(ID);
            fail("No " + expectedErrorName + " error was thrown by getSink()");
        } catch (ProxyException e) {
            assertThat(e.getErrorCode(), is(expectedError));
        }
    }

    /*
     * Test findAllSinks
     */
    @Test
    public void findAllSinks_remoteServiceReturnsHttpStatusInternalServerError_throws() throws Exception {
        final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl(flowStoreServiceConnector);
        when(flowStoreServiceConnector.findAllSinks()).thenThrow(new FlowStoreServiceConnectorUnexpectedStatusCodeException("DIED", 500));
        try {
            flowStoreProxy.findAllSinks();
            fail("No INTERNAL_SERVER_ERROR was thrown by findAllSinks()");
        } catch (ProxyException e) {
            assertThat(e.getErrorCode(), is(ProxyError.INTERNAL_SERVER_ERROR));
        }
    }

    @Test
    public void findAllSinks_remoteServiceReturnsHttpStatusOk_returnsListOfSinkEntity() throws Exception {

        final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl(flowStoreServiceConnector);
        final Sink sink = new SinkBuilder().setId(ID).build();

        when(flowStoreServiceConnector.findAllSinks()).thenReturn(Arrays.asList(sink));
        try {
            final List<Sink> allSinks  = flowStoreProxy.findAllSinks();
            assertNotNull(allSinks);
            assertThat(allSinks.size(), is(1));
            assertThat(allSinks.get(0).getId(), is(sink.getId()));
        } catch (ProxyException e) {
            fail("Unexpected error when calling: findAllSinks()");
        }
    }

    /*
     * Test updateSink
     */
    @Test
    public void updateSink_remoteServiceReturnsHttpStatusOk_returnsSinkModelEntity() throws Exception {
        final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl(flowStoreServiceConnector);
        final Sink sink = new SinkBuilder().setId(ID).setVersion(1).build();
        SinkModel model = getDefaultSinkModel(sink.getId(), sink.getVersion());

        when(flowStoreServiceConnector.updateSink(any(SinkContent.class), (eq(sink.getId())), (eq(sink.getVersion()))))
                .thenReturn(sink);
        try {
            final SinkModel updatedModel = flowStoreProxy.updateSink(model);
            assertNotNull(updatedModel);
        } catch (ProxyException e) {
            fail("Unexpected error when calling: updateSink()");
        }
    }

    @Test
    public void updateSink_remoteServiceReturnsHttpStatusNotFound_throws() throws Exception {
        updateSink_genericTestImplForHttpErrors(404, ProxyError.ENTITY_NOT_FOUND, "ENTITY_NOT_FOUND");
    }

    @Test
    public void updateSink_remoteServiceReturnsHttpStatusNotAcceptable_throws() throws Exception {
        updateSink_genericTestImplForHttpErrors(406, ProxyError.NOT_ACCEPTABLE, "NOT_ACCEPTABLE");
    }

    @Test
    public void updateSink_remoteServiceReturnsHttpStatusConflict_throws() throws Exception {
        updateSink_genericTestImplForHttpErrors(409, ProxyError.CONFLICT_ERROR, "CONFLICT_ERROR");
    }

    @Test
    public void updateSink_remoteServiceReturnsHttpStatusInternalServerError_throws() throws Exception {
        updateSink_genericTestImplForHttpErrors(500, ProxyError.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR");
    }

    @Test
    public void updateSink_throwsIllegalArgumentException() throws Exception {
        IllegalArgumentException illegalArgumentException = new IllegalArgumentException("DIED");
        SinkModel model = getDefaultSinkModel(1, 1);
        model.setResourceName("");
        updateSink_testForProxyError(model, illegalArgumentException, ProxyError.MODEL_MAPPER_EMPTY_FIELDS, "MODEL_MAPPER_EMPTY_FIELDS");
    }

    private SinkModel getDefaultSinkModel(long id, long version) {
        return new SinkModel(id, version, "sinkName", "resourceName");
    }

    private void updateSink_genericTestImplForHttpErrors(int errorCodeToReturn, ProxyError expectedError, String expectedErrorName) throws Exception {
        final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl(flowStoreServiceConnector);
        final Sink sink = new SinkBuilder().setId(ID).setVersion(1).build();
        SinkModel model = getDefaultSinkModel(sink.getId(), sink.getVersion());

        when(flowStoreServiceConnector.updateSink(any(SinkContent.class), (eq(sink.getId())), (eq(sink.getVersion()))))
                .thenThrow(new FlowStoreServiceConnectorUnexpectedStatusCodeException("DIED", errorCodeToReturn));
        try {
            flowStoreProxy.updateSink(model);
            fail("No " + expectedErrorName + " error was thrown by updateSink()");
        } catch (ProxyException e) {
            assertThat(e.getErrorCode(), is(expectedError));
        }
    }

    private void updateSink_testForProxyError(SinkModel model, Exception exception, ProxyError expectedError, String expectedErrorName) throws Exception {
        final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl(flowStoreServiceConnector);
        final Sink sink = new SinkBuilder().setId(ID).setVersion(1).build();

        when(flowStoreServiceConnector.updateSink(any(SinkContent.class), (eq(sink.getId())), (eq(sink.getVersion()))))
                .thenThrow(exception);

        try {
            flowStoreProxy.updateSink(model);
            fail("No " + expectedErrorName + " error was thrown by updateSink()");
        } catch (ProxyException e) {
            assertThat(e.getErrorCode(), is(expectedError));
        }
    }

    /*
     * Test updateSubmitter
     */
    @Test
    public void updateSubmitter_remoteServiceReturnsHttpStatusOk_returnsModelEntity() throws Exception {
        final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl(flowStoreServiceConnector);
        final Submitter submitter = new SubmitterBuilder().setId(ID).setVersion(1).build();
        SubmitterModel model = getDefaultSubmitterModel(submitter.getId(), submitter.getVersion());

        when(flowStoreServiceConnector.updateSubmitter(any(SubmitterContent.class), (eq(submitter.getId())), (eq(submitter.getVersion()))))
                .thenReturn(submitter);
        try {
            final SubmitterModel updatedModel = flowStoreProxy.updateSubmitter(model);
            assertNotNull(updatedModel);
        } catch (ProxyException e) {
            fail("Unexpected error when calling: updateSubmitter()");
        }
    }

    @Test
    public void updateSubmitter_remoteServiceReturnsHttpStatusNotFound_throws() throws Exception {
        updateSubmitter_genericTestImplForHttpErrors(404, ProxyError.ENTITY_NOT_FOUND, "ENTITY_NOT_FOUND");
    }

    @Test
    public void updateSubmitter_remoteServiceReturnsHttpStatusNotAcceptable_throws() throws Exception {
        updateSubmitter_genericTestImplForHttpErrors(406, ProxyError.NOT_ACCEPTABLE, "NOT_ACCEPTABLE");
    }

    @Test
    public void updateSubmitter_remoteServiceReturnsHttpStatusConflict_throws() throws Exception {
        updateSubmitter_genericTestImplForHttpErrors(409, ProxyError.CONFLICT_ERROR, "CONFLICT_ERROR");
    }

    @Test
    public void updateSubmitter_remoteServiceReturnsHttpStatusInternalServerError_throws() throws Exception {
        updateSubmitter_genericTestImplForHttpErrors(500, ProxyError.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR");
    }

    @Test
    public void updateSubmitter_throwsIllegalArgumentException() throws Exception {
        IllegalArgumentException illegalArgumentException = new IllegalArgumentException("DIED");
        SubmitterModel model = new SubmitterModel(1, 1, "42", "" , "test");
        updateSubmitter_testForProxyError(model, illegalArgumentException, ProxyError.MODEL_MAPPER_EMPTY_FIELDS, "MODEL_MAPPER_EMPTY_FIELDS");
    }

    private SubmitterModel getDefaultSubmitterModel(long id, long version) {
        return new SubmitterModel(id, version, "1", "submitterName", "submitterDescription");
    }

    private void updateSubmitter_genericTestImplForHttpErrors(int errorCodeToReturn, ProxyError expectedError, String expectedErrorName) throws Exception {
        final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl(flowStoreServiceConnector);
        final Submitter submitter = new SubmitterBuilder().setId(ID).setVersion(1).build();
        SubmitterModel model = getDefaultSubmitterModel(submitter.getId(), submitter.getVersion());

        when(flowStoreServiceConnector.updateSubmitter(any(SubmitterContent.class), (eq(submitter.getId())), (eq(submitter.getVersion()))))
                .thenThrow(new FlowStoreServiceConnectorUnexpectedStatusCodeException("DIED", errorCodeToReturn));
        try {
            flowStoreProxy.updateSubmitter(model);
            fail("No " + expectedErrorName + " error was thrown by updateSubmitter()");
        } catch (ProxyException e) {
            assertThat(e.getErrorCode(), is(expectedError));
        }
    }

    private void updateSubmitter_testForProxyError(SubmitterModel model, Exception exception, ProxyError expectedError, String expectedErrorName) throws Exception {
        final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl(flowStoreServiceConnector);
        final Submitter submitter = new SubmitterBuilder().setId(ID).setVersion(1).build();

        when(flowStoreServiceConnector.updateSubmitter(any(SubmitterContent.class), (eq(submitter.getId())), (eq(submitter.getVersion()))))
                .thenThrow(exception);

        try {
            flowStoreProxy.updateSubmitter(model);
            fail("No " + expectedErrorName + " error was thrown by updateSubmitter()");
        } catch (ProxyException e) {
            assertThat(e.getErrorCode(), is(expectedError));
        }
    }

    private void createSubmitter_genericTestImplForHttpErrors(int errorCodeToReturn, ProxyError expectedError, String expectedErrorName) throws Exception {
        final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl(flowStoreServiceConnector);

        when(flowStoreServiceConnector.createSubmitter(any(SubmitterContent.class)))
                .thenThrow(new FlowStoreServiceConnectorUnexpectedStatusCodeException("DIED", errorCodeToReturn));
        try {
            flowStoreProxy.createSubmitter(getDefaultSubmitterModel(0, 0));
            fail("No " + expectedErrorName + " error was thrown by createSubmitter()");
        } catch (ProxyException e) {
            assertThat(e.getErrorCode(), is(expectedError));
        }
    }

    private void createSubmitter_testForProxyError(SubmitterModel model, Exception exception, ProxyError expectedError, String expectedErrorName) throws Exception {
        final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl(flowStoreServiceConnector);

        when(flowStoreServiceConnector.createSubmitter(any(SubmitterContent.class)))
                .thenThrow(exception);

        try {
            flowStoreProxy.createSubmitter(model);
            fail("No " + expectedErrorName + " error was thrown by createSubmitter()");
        } catch (ProxyException e) {
            assertThat(e.getErrorCode(), is(expectedError));
        }
    }

  /*
   * Test createSubmitter
   */
    @Test
    public void createSubmitter_remoteServiceReturnsHttpStatusCreated_returnsSubmitterEntity() throws Exception {
        final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl(flowStoreServiceConnector);

        final Submitter submitter = new SubmitterBuilder().build();
        when(flowStoreServiceConnector.createSubmitter(any(SubmitterContent.class))).thenReturn(submitter);

        try {
            final SubmitterModel createdModel  = flowStoreProxy.createSubmitter(getDefaultSubmitterModel(0, 0));
            assertNotNull(createdModel);
        } catch (ProxyException e) {
            fail("Unexpected error when calling: createSubmitter()");
        }
    }

    @Test
    public void createSubmitter_remoteServiceReturnsHttpStatusInternalServerError_throws() throws Exception {
        createSubmitter_genericTestImplForHttpErrors(500, ProxyError.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR");
    }

    @Test
    public void createSubmitter_remoteServiceReturnsHttpStatusNotAcceptable_throws() throws Exception {
        createSubmitter_genericTestImplForHttpErrors(406, ProxyError.NOT_ACCEPTABLE, "NOT_ACCEPTABLE");
    }

    @Test
    public void createSubmitter_throwsIllegalArgumentException() throws Exception {
        IllegalArgumentException illegalArgumentException = new IllegalArgumentException("DIED");
        SubmitterModel model = new SubmitterModel(1, 1, "42", "" , "test");
        createSubmitter_testForProxyError(model, illegalArgumentException, ProxyError.MODEL_MAPPER_EMPTY_FIELDS, "MODEL_MAPPER_EMPTY_FIELDS");
    }


    /*
    * Test getSubmitter
    */

    @Test
    public void getSubmitter_remoteServiceReturnsHttpStatusOk_returnsSubmitterEntity() throws Exception {
        final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl(flowStoreServiceConnector);
        final Submitter submitter = new SubmitterBuilder().setId(ID).build();
        when(flowStoreServiceConnector.getSubmitter(eq(ID))).thenReturn(submitter);

        try {
            final SubmitterModel retrievedModel = flowStoreProxy.getSubmitter(submitter.getId());
            assertNotNull(retrievedModel);
        } catch (ProxyException e) {
            fail("Unexpected error when calling: getSubmitter()");
        }
    }

    @Test
    public void getSubmitter_remoteServiceReturnsHttpStatusInternalServerError_throws() throws Exception {
        getSubmitter_genericTestImplForHttpErrors(500, ProxyError.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR");
    }

    @Test
    public void getSubmitter_remoteServiceReturnsHttpStatusNotFound_throws() throws Exception {
        getSubmitter_genericTestImplForHttpErrors(404, ProxyError.ENTITY_NOT_FOUND, "ENTITY_NOT_FOUND");
    }

    private void getSubmitter_genericTestImplForHttpErrors(int errorCodeToReturn, ProxyError expectedError, String expectedErrorName) throws Exception {
        final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl(flowStoreServiceConnector);
        when(flowStoreServiceConnector.getSubmitter(eq(ID))).thenThrow(new FlowStoreServiceConnectorUnexpectedStatusCodeException("DIED", errorCodeToReturn));

        try {
            flowStoreProxy.getSubmitter(ID);
            fail("No " + expectedErrorName + " error was thrown by getSubmitter()");
        } catch (ProxyException e) {
            assertThat(e.getErrorCode(), is(expectedError));
        }
    }

    /*
     * Test findAllSubmitters
     */
    @Test
    public void findAllSubmitters_remoteServiceReturnsHttpStatusInternalServerError_throws() throws Exception {
        final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl(flowStoreServiceConnector);
        when(flowStoreServiceConnector.findAllSubmitters()).thenThrow(new FlowStoreServiceConnectorUnexpectedStatusCodeException("DIED", 500));
        try {
            flowStoreProxy.findAllSubmitters();
            fail("No INTERNAL_SERVER_ERROR was thrown by findAllSubmitters()");
        } catch (ProxyException e) {
            assertThat(e.getErrorCode(), is(ProxyError.INTERNAL_SERVER_ERROR));
        }
    }

    @Test
    public void findAllSubmitters_remoteServiceReturnsHttpStatusOk_returnsListOfSubmitterEntity() throws Exception {

        final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl(flowStoreServiceConnector);
        final Submitter submitter = new SubmitterBuilder().setId(1L).build();

        when(flowStoreServiceConnector.findAllSubmitters()).thenReturn(Arrays.asList(submitter));
        try {
            final List<Submitter> allSubmitters  = flowStoreProxy.findAllSubmitters();
            assertNotNull(allSubmitters);
            assertThat(allSubmitters.size(), is(1));
            assertThat(allSubmitters.get(0).getId(), is(submitter.getId()));
        } catch (ProxyException e) {
            fail("Unexpected error when calling: findAllSubmitters()");
        }
    }

    /*
    * Test createFlow
    */
    @Test
    public void createFlow_remoteServiceReturnsHttpStatusInternalServerError_throws() throws Exception {
        final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl(flowStoreServiceConnector);
        FlowContent flowContent = new FlowContentBuilder().build();
        when(flowStoreServiceConnector.createFlow(eq(flowContent))).
                thenThrow(new FlowStoreServiceConnectorUnexpectedStatusCodeException("DIED", 500));
        try {
            flowStoreProxy.createFlow(flowContent);
            fail("No INTERNAL_SERVER_ERROR error was thrown by createFlow()");
        } catch (ProxyException e) {
            assertThat(e.getErrorCode(), is(ProxyError.INTERNAL_SERVER_ERROR));
        }
    }

    @Test
    public void createFlow_remoteServiceReturnsHttpStatusNotAcceptable_throws() throws Exception {
        final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl(flowStoreServiceConnector);
        FlowContent flowContent = new FlowContentBuilder().build();
        when(flowStoreServiceConnector.createFlow(eq(flowContent))).
                thenThrow(new FlowStoreServiceConnectorUnexpectedStatusCodeException("DIED", 406));
        try {
            flowStoreProxy.createFlow(flowContent);
            fail("No NOT_ACCEPTABLE error was thrown by createFlow()");
        } catch (ProxyException e) {
            assertThat(e.getErrorCode(), is(ProxyError.NOT_ACCEPTABLE));
        }
    }

    @Test
    public void createFlow_remoteServiceReturnsHttpStatusCreated_returnsFlowEntity() throws Exception {
        final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl(flowStoreServiceConnector);
        final FlowContent flowContent = new FlowContentBuilder().build();
        final Flow flow = new FlowBuilder().build();
        when(flowStoreServiceConnector.createFlow(flowContent)).thenReturn(flow);

        try {
            final Flow createdFlow  = flowStoreProxy.createFlow(flowContent);
            assertNotNull(createdFlow);
        } catch (ProxyException e) {
            fail("Unexpected error when calling: createFlow()");
        }
    }

    /*
    * Test findAllFlows
    */
    @Test
    public void findAllFlows_remoteServiceReturnsHttpStatusInternalServerError_throws() throws Exception {
        final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl(flowStoreServiceConnector);
        when(flowStoreServiceConnector.findAllFlows()).thenThrow(new FlowStoreServiceConnectorUnexpectedStatusCodeException("DIED", 500));
        try {
            flowStoreProxy.findAllFlows();
            fail("No INTERNAL_SERVER_ERROR was thrown by findAllFlows()");
        } catch (ProxyException e) {
            assertThat(e.getErrorCode(), is(ProxyError.INTERNAL_SERVER_ERROR));
        }
    }

    @Test
    public void findAllFlows_remoteServiceReturnsHttpStatusOk_returnsListOfFlowEntity() throws Exception {

        final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl(flowStoreServiceConnector);
        final Flow flow = new FlowBuilder().setId(ID).build();

        when(flowStoreServiceConnector.findAllFlows()).thenReturn(Arrays.asList(flow));
        try {
            final List<Flow> allFlows  = flowStoreProxy.findAllFlows();
            assertNotNull(allFlows);
            assertThat(allFlows.size(), is(1));
            assertThat(allFlows.get(0).getId(), is(flow.getId()));
        } catch (ProxyException e) {
            fail("Unexpected error when calling: findAllFlows()");
        }
    }


    /*
     * Test findAllFlowBinders
     */
    @Test
    public void findAllFlowBinders_remoteServiceReturnsHttpStatusInternalServerError_throws() throws Exception {
        when(HttpClient.doGet(any(Client.class), eq(flowStoreServiceUrl), eq(FlowStoreServiceConstants.FLOW_BINDERS)))
                .thenReturn(new MockedHttpClientResponse<String>(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), ""));

        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl();
        try {
            flowStoreProxy.findAllFlowBinders();
            fail();
        } catch (ProxyException e) {
            assertThat(e.getErrorCode(), is(ProxyError.INTERNAL_SERVER_ERROR));
        }
    }

    @Test
    public void findAllFlowBinders_remoteServiceReturnsHttpStatusOk_returnsListOfSubmitterEntity() throws Exception {
        final FlowBinder flowBinder = new FlowBinderBuilder().setId(666).build();
        when(HttpClient.doGet(any(Client.class), eq(flowStoreServiceUrl), eq(FlowStoreServiceConstants.FLOW_BINDERS)))
                .thenReturn(new MockedHttpClientResponse<List<FlowBinder>>(Response.Status.OK.getStatusCode(), Arrays.asList(flowBinder)));

        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl();
        final List<FlowBinder> allFlowBinders = flowStoreProxy.findAllFlowBinders();
        assertThat(allFlowBinders.size(), is(1));
        assertThat(allFlowBinders.get(0).getId(), is(flowBinder.getId()));
    }

    /*
    * Test getFlowComponent
    */
    @Test
    public void getFlowComponent_remoteServiceReturnsHttpStatusInternalServerError_throws() throws Exception {
        final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl(flowStoreServiceConnector);
        when(flowStoreServiceConnector.getFlowComponent(eq(ID))).thenThrow(new FlowStoreServiceConnectorUnexpectedStatusCodeException("DIED", 500));
        try {
            flowStoreProxy.getFlowComponent(ID);
            fail("No INTERNAL_SERVER_ERROR was thrown by getFlowComponent()");
        } catch (ProxyException e) {
            assertThat(e.getErrorCode(), is(ProxyError.INTERNAL_SERVER_ERROR));
        }
    }

    @Test
    public void getFlowComponent_remoteServiceReturnsHttpStatusOk_returnsFlowComponentEntity() throws Exception {
        final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl(flowStoreServiceConnector);
        final FlowComponent flowComponent = new FlowComponentBuilder().setId(ID).build();
        when(flowStoreServiceConnector.getFlowComponent(eq(ID))).thenReturn(flowComponent);

        try {
            final FlowComponent retrievedFlowComponent  = flowStoreProxy.getFlowComponent(flowComponent.getId());
            assertNotNull(retrievedFlowComponent);
        } catch (ProxyException e) {
            fail("Unexpected error when calling: getFlowComponent()");
        }
    }

    @Test
    public void getFlowComponent_remoteServiceReturnsHttpStatusNotFound_throws() throws Exception {
        final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl(flowStoreServiceConnector);
        when(flowStoreServiceConnector.getFlowComponent(eq(ID))).thenThrow(new FlowStoreServiceConnectorUnexpectedStatusCodeException("DIED", 404));

        try {
            flowStoreProxy.getFlowComponent(ID);
            fail("No ENTITY_NOT_FOUND error was thrown by getFlowComponent()");
        } catch (ProxyException e) {
            assertThat(e.getErrorCode(), is(ProxyError.ENTITY_NOT_FOUND));
        }
    }

    /*
     * Test refreshFlow
     */
    @Test
    public void refreshFlowComponents_remoteServiceReturnsHttpStatusInternalServerError_throws() throws Exception {
        final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl(flowStoreServiceConnector);
        final Flow flow = new FlowBuilder().setId(ID).setVersion(1L).build();
        when(flowStoreServiceConnector.refreshFlowComponents((eq(flow.getId())), (eq(flow.getVersion()))))
                .thenThrow(new FlowStoreServiceConnectorUnexpectedStatusCodeException("DIED", 500));
        try {
            flowStoreProxy.refreshFlowComponentsOld(flow.getId(), flow.getVersion());
            fail("No INTERNAL_SERVER_ERROR was thrown by refreshFlowComponents()");
        } catch (ProxyException e) {
            assertThat(e.getErrorCode(), is(ProxyError.INTERNAL_SERVER_ERROR));
        }
    }

    @Test
    public void refreshFlowComponents_remoteServiceReturnsHttpStatusConflict_throws() throws Exception {
        final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl(flowStoreServiceConnector);
        final Flow flow = new FlowBuilder().setId(ID).setVersion(1L).build();
        when(flowStoreServiceConnector.refreshFlowComponents((eq(flow.getId())), (eq(flow.getVersion()))))
                .thenThrow(new FlowStoreServiceConnectorUnexpectedStatusCodeException("DIED", 409));
        try {
            flowStoreProxy.refreshFlowComponentsOld(flow.getId(), flow.getVersion());
            fail("No CONFLICT_ERROR was thrown by refreshFlowComponents()");
        } catch (ProxyException e) {
            assertThat(e.getErrorCode(), is(ProxyError.CONFLICT_ERROR));
        }
    }

    @Test
    public void refreshFlowComponents_remoteServiceReturnsHttpStatusOk_returnsFlowEntity() throws Exception {
        final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl(flowStoreServiceConnector);
        final Flow flow = new FlowBuilder().setId(ID).setVersion(1L).build();
        when(flowStoreServiceConnector.refreshFlowComponents((eq(flow.getId())), (eq(flow.getVersion()))))
                .thenReturn(flow);

        try {
            final Flow updatedFlow = flowStoreProxy.refreshFlowComponentsOld(flow.getId(), flow.getVersion());
            assertNotNull(updatedFlow);
        } catch (ProxyException e) {
            fail("Unexpected error when calling: refreshFlowComponents");
        }
    }

    /*
     * Test updateFlow
     */
    @Test
    public void updateFlow_remoteServiceReturnsHttpStatusOk_returnsFlowModelEntity() throws Exception {
        final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl(flowStoreServiceConnector);
        final Flow flow = new FlowBuilder().setId(ID).setVersion(1).build();
        FlowModel model = getDefaultFlowModel(flow.getId(), flow.getVersion());

        when(flowStoreServiceConnector.getFlow(eq(flow.getId()))).thenReturn(flow);
        when(flowStoreServiceConnector.updateFlow(any(FlowContent.class), (eq(flow.getId())), (eq(flow.getVersion()))))
                .thenReturn(flow);
        try {
            final FlowModel updatedModel = flowStoreProxy.updateFlow(model);
            assertNotNull(updatedModel);
        } catch (ProxyException e) {
            fail("Unexpected error when calling: updateFlow()");
        }
    }

    @Test
    public void updateFlow_remoteServiceReturnsHttpStatusNotFound_throws() throws Exception {
        updateFlow_genericTestImplForHttpErrors(404, ProxyError.ENTITY_NOT_FOUND, "ENTITY_NOT_FOUND");
    }

    @Test
    public void updateFlow_remoteServiceReturnsHttpStatusNotAcceptable_throws() throws Exception {
        updateFlow_genericTestImplForHttpErrors(406, ProxyError.NOT_ACCEPTABLE, "NOT_ACCEPTABLE");
    }

    @Test
    public void updateFlow_remoteServiceReturnsHttpStatusConflict_throws() throws Exception {
        updateFlow_genericTestImplForHttpErrors(409, ProxyError.CONFLICT_ERROR, "CONFLICT_ERROR");
    }

    @Test
    public void updateFlow_remoteServiceReturnsHttpStatusInternalServerError_throws() throws Exception {
        updateFlow_genericTestImplForHttpErrors(500, ProxyError.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR");
    }

    @Test
    public void updateFlow_throwsIllegalArgumentException() throws Exception {
        IllegalArgumentException illegalArgumentException = new IllegalArgumentException("DIED");
        FlowModel model = getDefaultFlowModel(1, 1);
        model.setFlowName("");
        updateFlow_testForProxyError(model, illegalArgumentException, ProxyError.MODEL_MAPPER_EMPTY_FIELDS, "MODEL_MAPPER_EMPTY_FIELDS");
    }

    private FlowModel getDefaultFlowModel(long id, long version) {
        Map<String, String> flowComponents = new HashMap<String, String>();
        flowComponents.put("1", "FlowComponentName");
        return new FlowModel(id, version, "flowName", "flowDescription", flowComponents);
    }

    private void updateFlow_genericTestImplForHttpErrors(int errorCodeToReturn, ProxyError expectedError, String expectedErrorName) throws Exception {
        final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl(flowStoreServiceConnector);
        final Flow flow = new FlowBuilder().setId(ID).setVersion(1).build();
        FlowModel model = getDefaultFlowModel(flow.getId(), flow.getVersion());
        when(flowStoreServiceConnector.getFlow(eq(flow.getId()))).thenReturn(flow);
        when(flowStoreServiceConnector.updateFlow(any(FlowContent.class), (eq(flow.getId())), (eq(flow.getVersion()))))
                .thenThrow(new FlowStoreServiceConnectorUnexpectedStatusCodeException("DIED", errorCodeToReturn));
        try {
            flowStoreProxy.updateFlow(model);
            fail("No " + expectedErrorName + " error was thrown by updateFlow()");
        } catch (ProxyException e) {
            assertThat(e.getErrorCode(), is(expectedError));
        }
    }

    private void updateFlow_testForProxyError(FlowModel model, Exception exception, ProxyError expectedError, String expectedErrorName) throws Exception {
        final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl(flowStoreServiceConnector);
        final Flow flow = new FlowBuilder().setId(ID).setVersion(1).build();

        when(flowStoreServiceConnector.updateFlow(any(FlowContent.class), (eq(flow.getId())), (eq(flow.getVersion()))))
                .thenThrow(exception);

        try {
            flowStoreProxy.updateFlow(model);
            fail("No " + expectedErrorName + " error was thrown by updateFlow()");
        } catch (ProxyException e) {
            assertThat(e.getErrorCode(), is(expectedError));
        }
    }

    /*
     * Test createFlowComponent
     */
    @Test
    public void createFlowComponent_remoteServiceReturnsHttpStatusInternalServerError_throws() throws Exception {
        final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl(flowStoreServiceConnector);
        FlowComponentContent flowComponentContent = new FlowComponentContentBuilder().build();
        when(flowStoreServiceConnector.createFlowComponent(eq(flowComponentContent))).
                thenThrow(new FlowStoreServiceConnectorUnexpectedStatusCodeException("DIED", 500));
        try {
            flowStoreProxy.createFlowComponent(flowComponentContent);
            fail("No INTERNAL_SERVER_ERROR error was thrown by createFlowComponent()");
        } catch (ProxyException e) {
            assertThat(e.getErrorCode(), is(ProxyError.INTERNAL_SERVER_ERROR));
        }
    }

    @Test
    public void createFlowComponent_remoteServiceReturnsHttpStatusNotAcceptable_throws() throws Exception {
        final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl(flowStoreServiceConnector);
        FlowComponentContent flowComponentContent = new FlowComponentContentBuilder().build();
        when(flowStoreServiceConnector.createFlowComponent(eq(flowComponentContent))).
                thenThrow(new FlowStoreServiceConnectorUnexpectedStatusCodeException("DIED", 406));
        try {
            flowStoreProxy.createFlowComponent(flowComponentContent);
            fail("No NOT_ACCEPTABLE error was thrown by createFlowComponent()");
        } catch (ProxyException e) {
            assertThat(e.getErrorCode(), is(ProxyError.NOT_ACCEPTABLE));
        }
    }

    @Test
    public void createFlowComponent_remoteServiceReturnsHttpStatusCreated_returnsFlowComponentEntity() throws Exception {
        final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl(flowStoreServiceConnector);
        final FlowComponentContent flowComponentContent = new FlowComponentContentBuilder().build();
        final FlowComponent flowComponent = new FlowComponentBuilder().build();
        when(flowStoreServiceConnector.createFlowComponent(flowComponentContent)).thenReturn(flowComponent);

        try {
            final FlowComponent createdFlowComponent  = flowStoreProxy.createFlowComponent(flowComponentContent);
            assertNotNull(createdFlowComponent);
        } catch (ProxyException e) {
            fail("Unexpected error when calling: createFlowComponent()");
        }
    }

    /*
     * Test findAllFlowComponents
     */
    @Test
    public void findAllFlowComponents_remoteServiceReturnsHttpStatusInternalServerError_throws() throws Exception {
        final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl(flowStoreServiceConnector);
        when(flowStoreServiceConnector.findAllFlowComponents()).thenThrow(new FlowStoreServiceConnectorUnexpectedStatusCodeException("DIED", 500));
        try {
            flowStoreProxy.findAllFlowComponents();
            fail("No INTERNAL_SERVER_ERROR was thrown by findAllFlowComponenst()");
        } catch (ProxyException e) {
            assertThat(e.getErrorCode(), is(ProxyError.INTERNAL_SERVER_ERROR));
        }
    }

    @Test
    public void findAllFlowComponents_remoteServiceReturnsHttpStatusOk_returnsListOfFlowComponentEntity() throws Exception {

        final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl(flowStoreServiceConnector);
        final FlowComponent flowComponent = new FlowComponentBuilder().setId(1L).build();

        when(flowStoreServiceConnector.findAllFlowComponents()).thenReturn(Arrays.asList(flowComponent));
        try {
            final List<FlowComponent> allFlowComponents  = flowStoreProxy.findAllFlowComponents();
            assertNotNull(allFlowComponents);
            assertThat(allFlowComponents.size(), is(1));
            assertThat(allFlowComponents.get(0).getId(), is(flowComponent.getId()));
        } catch (ProxyException e) {
            fail("Unexpected error when calling: findAllFlowComponents()");
        }
    }

    /*
     * Test updateFlowComponent
     */
    @Test
    public void updateFlowComponent_remoteServiceReturnsHttpStatusInternalServerError_throws() throws Exception {
        final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl(flowStoreServiceConnector);
        final FlowComponent flowComponent = new FlowComponentBuilder().setId(ID).setVersion(1L).build();
        when(flowStoreServiceConnector.updateFlowComponent(eq(flowComponent.getContent()), (eq(flowComponent.getId())), (eq(flowComponent.getVersion()))))
                .thenThrow(new FlowStoreServiceConnectorUnexpectedStatusCodeException("DIED", 500));
        try {
            flowStoreProxy.updateFlowComponent(flowComponent.getContent(), flowComponent.getId(), flowComponent.getVersion());
            fail("No INTERNAL_SERVER_ERROR was thrown by updateFlowComponent()");
        } catch (ProxyException e) {
            assertThat(e.getErrorCode(), is(ProxyError.INTERNAL_SERVER_ERROR));
        }
    }

    @Test
    public void updateFlowComponent_remoteServiceReturnsHttpStatusConflict_throws() throws Exception {
        final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl(flowStoreServiceConnector);
        final FlowComponent flowComponent = new FlowComponentBuilder().setId(ID).setVersion(1L).build();
        when(flowStoreServiceConnector.updateFlowComponent(eq(flowComponent.getContent()),(eq(flowComponent.getId())),(eq(flowComponent.getVersion()))))
                .thenThrow(new FlowStoreServiceConnectorUnexpectedStatusCodeException("DIED", 409));
        try {
            flowStoreProxy.updateFlowComponent(flowComponent.getContent(), flowComponent.getId(), flowComponent.getVersion());
            fail("No CONFLICT_ERROR was thrown by updateFlowComponent()");
        } catch (ProxyException e) {
            assertThat(e.getErrorCode(), is(ProxyError.CONFLICT_ERROR));
        }
    }

    @Test
    public void updateFlowComponent_remoteServiceReturnsHttpStatusNotAcceptable_throws() throws Exception {
        final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl(flowStoreServiceConnector);
        final FlowComponent flowComponent = new FlowComponentBuilder().setId(ID).setVersion(1L).build();
        when(flowStoreServiceConnector.updateFlowComponent(eq(flowComponent.getContent()), (eq(flowComponent.getId())), (eq(flowComponent.getVersion()))))
                .thenThrow(new FlowStoreServiceConnectorUnexpectedStatusCodeException("DIED", 406));
        try {
            flowStoreProxy.updateFlowComponent(flowComponent.getContent(), flowComponent.getId(), flowComponent.getVersion());
            fail("No NOT_ACCEPTABLE error was thrown by updateFlowComponent()");
        } catch (ProxyException e) {
            assertThat(e.getErrorCode(), is(ProxyError.NOT_ACCEPTABLE));
        }
    }

    @Test
    public void updateFlowComponent_remoteServiceReturnsHttpStatusOk_returnsFlowComponentEntity() throws Exception {
        final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl(flowStoreServiceConnector);
        final FlowComponent flowComponent = new FlowComponentBuilder().setId(ID).setVersion(1L).build();
        when(flowStoreServiceConnector.updateFlowComponent(eq(flowComponent.getContent()), (eq(flowComponent.getId())), (eq(flowComponent.getVersion()))))
                .thenReturn(flowComponent);

        try {
            final FlowComponent updatedFlowComponent = flowStoreProxy.updateFlowComponent(flowComponent.getContent(), flowComponent.getId(), flowComponent.getVersion());
            assertNotNull(updatedFlowComponent);
        } catch (ProxyException e) {
            fail("Unexpected error when calling: updateFlowComponent()");
        }
    }
}
