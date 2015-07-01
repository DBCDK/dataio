package dk.dbc.dataio.gui.server;

import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorUnexpectedStatusCodeException;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.FlowBinder;
import dk.dbc.dataio.commons.types.FlowBinderContent;
import dk.dbc.dataio.commons.types.FlowComponent;
import dk.dbc.dataio.commons.types.FlowComponentContent;
import dk.dbc.dataio.commons.types.FlowContent;
import dk.dbc.dataio.commons.types.JavaScript;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.SinkContent;
import dk.dbc.dataio.commons.types.Submitter;
import dk.dbc.dataio.commons.types.SubmitterContent;
import dk.dbc.dataio.commons.utils.httpclient.HttpClient;
import dk.dbc.dataio.commons.utils.service.ServiceUtil;
import dk.dbc.dataio.commons.utils.test.model.FlowBinderBuilder;
import dk.dbc.dataio.commons.utils.test.model.FlowBinderContentBuilder;
import dk.dbc.dataio.commons.utils.test.model.FlowBuilder;
import dk.dbc.dataio.commons.utils.test.model.FlowComponentBuilder;
import dk.dbc.dataio.commons.utils.test.model.SinkBuilder;
import dk.dbc.dataio.commons.utils.test.model.SubmitterBuilder;
import dk.dbc.dataio.gui.client.exceptions.ProxyError;
import dk.dbc.dataio.gui.client.exceptions.ProxyException;
import dk.dbc.dataio.gui.client.model.FlowBinderModel;
import dk.dbc.dataio.gui.client.model.FlowComponentModel;
import dk.dbc.dataio.gui.client.model.FlowModel;
import dk.dbc.dataio.gui.client.model.SinkModel;
import dk.dbc.dataio.gui.client.model.SubmitterModel;
import dk.dbc.dataio.gui.client.modelBuilders.SubmitterModelBuilder;
import dk.dbc.dataio.gui.client.proxies.JavaScriptProjectFetcher;
import dk.dbc.dataio.gui.server.modelmappers.FlowComponentModelMapper;
import org.glassfish.jersey.client.ClientConfig;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.naming.NamingException;
import javax.ws.rs.client.Client;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
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
    private final Client client = mock(Client.class);
    private final static long ID = 1L;

    private final long DEFAULT_FLOW_BINDER_ID = 11L;
    private final long DEFAULT_FLOW_BINDER_VERSION = 2002L;
    private final long DEFAULT_FLOW_ID = 22L;
    private final long DEFAULT_SUBMITTER_ID = 33L;
    private final long DEFAULT_SINK_ID = 44L;

    private Flow defaultFlow = new FlowBuilder().setId(DEFAULT_FLOW_ID).build();
    private Submitter defaultSubmitter = new SubmitterBuilder().setId(DEFAULT_SUBMITTER_ID).build();
    private Sink defaultSink = new SinkBuilder().setId(DEFAULT_SINK_ID).build();
    private FlowBinderContent defaultFlowBinderContent = new FlowBinderContent(
            "flow binder content", "description", "packaging", "format", "charset", "destination", "record splitter", true,
            DEFAULT_FLOW_ID,
            Collections.singletonList(DEFAULT_SUBMITTER_ID),
            DEFAULT_SINK_ID);

    @Before
    public void setup() throws Exception {
        mockStatic(ServiceUtil.class);
        mockStatic(HttpClient.class);
        String flowStoreServiceUrl = "http://dataio/flow-service";
        when(ServiceUtil.getFlowStoreServiceEndpoint()).thenReturn(flowStoreServiceUrl);
        when(HttpClient.newClient(any(ClientConfig.class))).thenReturn(client);
    }

    @Test
    public void noArgs_flowStoreProxyConstructorFlowStoreService_EndpointCanNotBeLookedUp_throws() throws Exception {
        when(ServiceUtil.getFlowStoreServiceEndpoint()).thenThrow(new NamingException());
        try {
            new FlowStoreProxyImpl();
            fail();
        } catch (NamingException e) {
        }
    }

    @Test
    public void oneArg_flowStoreProxyConstructorFlowStoreService_EndpointCanNotBeLookedUp_throws1() throws Exception {
        final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        when(ServiceUtil.getFlowStoreServiceEndpoint()).thenThrow(new NamingException());
        try {
            new FlowStoreProxyImpl(flowStoreServiceConnector);
            fail();
        } catch (NamingException e) {
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
            final SinkModel createdModel = flowStoreProxy.createSink(getDefaultSinkModel(0, 0));
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
        createSink_testForProxyError(model, illegalArgumentException, ProxyError.MODEL_MAPPER_INVALID_FIELD_VALUE, "MODEL_MAPPER_INVALID_FIELD_VALUE");
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

        when(flowStoreServiceConnector.findAllSinks()).thenReturn(Collections.singletonList(sink));
        try {
            final List<SinkModel> allSinks = flowStoreProxy.findAllSinks();
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
        updateSink_testForProxyError(model, illegalArgumentException, ProxyError.MODEL_MAPPER_INVALID_FIELD_VALUE, "MODEL_MAPPER_INVALID_FIELD_VALUE");
    }

    private SinkModel getDefaultSinkModel(long id, long version) {
        return new SinkModel(id, version, "sinkName", "resourceName", "description");
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
        SubmitterModel model = new SubmitterModelBuilder().setId(submitter.getId()).setVersion(submitter.getVersion()).build();

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
        SubmitterModel model = new SubmitterModelBuilder().setName("").build();
        updateSubmitter_testForProxyError(model, illegalArgumentException, ProxyError.MODEL_MAPPER_INVALID_FIELD_VALUE, "MODEL_MAPPER_INVALID_FIELD_VALUE");
    }

    private void updateSubmitter_genericTestImplForHttpErrors(int errorCodeToReturn, ProxyError expectedError, String expectedErrorName) throws Exception {
        final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl(flowStoreServiceConnector);
        final Submitter submitter = new SubmitterBuilder().setId(ID).setVersion(1).build();
        SubmitterModel model = new SubmitterModelBuilder().setId(submitter.getId()).setVersion(submitter.getVersion()).build();

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
            flowStoreProxy.createSubmitter(new SubmitterModelBuilder().build());
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
            final SubmitterModel createdModel = flowStoreProxy.createSubmitter(new SubmitterModelBuilder().build());
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
        SubmitterModel model = new SubmitterModelBuilder().setName("").build();
        createSubmitter_testForProxyError(model, illegalArgumentException, ProxyError.MODEL_MAPPER_INVALID_FIELD_VALUE, "MODEL_MAPPER_INVALID_FIELD_VALUE");
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

        when(flowStoreServiceConnector.findAllSubmitters()).thenReturn(Collections.singletonList(submitter));
        try {
            final List<SubmitterModel> allSubmitters = flowStoreProxy.findAllSubmitters();
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
    public void createFlow_remoteServiceReturnsHttpStatusCreated_returnsFlowModelEntity() throws Exception {
        final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl(flowStoreServiceConnector);
        final Flow flow = new FlowBuilder().build();
        when(flowStoreServiceConnector.createFlow(any(FlowContent.class))).thenReturn(flow);

        try {
            final FlowModel createdModel = flowStoreProxy.createFlow(getDefaultFlowModel(0, 0));
            assertNotNull(createdModel);
        } catch (ProxyException e) {
            fail("Unexpected error when calling: createFlow()");
        }
    }

    @Test
    public void createFlow_remoteServiceReturnsHttpStatusInternalServerError_throws() throws Exception {
        createFlow_genericTestImplForHttpErrors(500, ProxyError.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR");
    }

    @Test
    public void createFlow_remoteServiceReturnsHttpStatusNotAcceptable_throws() throws Exception {
        createFlow_genericTestImplForHttpErrors(406, ProxyError.NOT_ACCEPTABLE, "NOT_ACCEPTABLE");
    }

    @Test
    public void createFlow_throwsIllegalArgumentException() throws Exception {
        IllegalArgumentException illegalArgumentException = new IllegalArgumentException("DIED");
        FlowModel model = getDefaultFlowModel(0, 0);
        model.setFlowName("");
        createFlow_testForProxyError(model, illegalArgumentException, ProxyError.MODEL_MAPPER_INVALID_FIELD_VALUE, "MODEL_MAPPER_INVALID_FIELD_VALUE");
    }


    private FlowModel getDefaultFlowModel(long id, long version) {
        FlowComponentModel flowComponentModel =
                new FlowComponentModel(1, 1, "FlowComponentName", "svnProject", "1233", "invocationJavaScript", "invocationMethod", Collections.singletonList("JavaScriptModuleName"), "description");
        return new FlowModel(id, version, "FlowName", "description", Collections.singletonList(flowComponentModel));
    }

    private void createFlow_genericTestImplForHttpErrors(int errorCodeToReturn, ProxyError expectedError, String expectedErrorName) throws Exception {
        final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl(flowStoreServiceConnector);

        when(flowStoreServiceConnector.createFlow(any(FlowContent.class)))
                .thenThrow(new FlowStoreServiceConnectorUnexpectedStatusCodeException("DIED", errorCodeToReturn));
        try {
            flowStoreProxy.createFlow(getDefaultFlowModel(0, 0));
            fail("No " + expectedErrorName + " error was thrown by createFlow()");
        } catch (ProxyException e) {
            assertThat(e.getErrorCode(), is(expectedError));
        }
    }

    private void createFlow_testForProxyError(FlowModel model, Exception exception, ProxyError expectedError, String expectedErrorName) throws Exception {
        final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl(flowStoreServiceConnector);

        when(flowStoreServiceConnector.createFlow(any(FlowContent.class)))
                .thenThrow(exception);

        try {
            flowStoreProxy.createFlow(model);
            fail("No " + expectedErrorName + " error was thrown by createFlow()");
        } catch (ProxyException e) {
            assertThat(e.getErrorCode(), is(expectedError));
        }
    }

    /*
    * Test getFlow
    */

    @Test
    public void getFlow_remoteServiceReturnsHttpStatusOk_returnsFlowModelEntity() throws Exception {
        final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl(flowStoreServiceConnector);
        final Flow flow = new FlowBuilder().setId(ID).build();
        when(flowStoreServiceConnector.getFlow(eq(ID))).thenReturn(flow);

        try {
            final FlowModel retrievedModel = flowStoreProxy.getFlow(flow.getId());
            assertNotNull(retrievedModel);
        } catch (ProxyException e) {
            fail("Unexpected error when calling: getFlow()");
        }
    }

    @Test
    public void getFlow_remoteServiceReturnsHttpStatusInternalServerError_throws() throws Exception {
        getFlow_genericTestImplForHttpErrors(500, ProxyError.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR");
    }

    @Test
    public void getFlow_remoteServiceReturnsHttpStatusNotFound_throws() throws Exception {
        getFlow_genericTestImplForHttpErrors(404, ProxyError.ENTITY_NOT_FOUND, "ENTITY_NOT_FOUND");
    }

    private void getFlow_genericTestImplForHttpErrors(int errorCodeToReturn, ProxyError expectedError, String expectedErrorName) throws Exception {
        final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl(flowStoreServiceConnector);
        when(flowStoreServiceConnector.getFlow(eq(ID))).thenThrow(new FlowStoreServiceConnectorUnexpectedStatusCodeException("DIED", errorCodeToReturn));

        try {
            flowStoreProxy.getFlow(ID);
            fail("No " + expectedErrorName + " error was thrown by getFlow()");
        } catch (ProxyException e) {
            assertThat(e.getErrorCode(), is(expectedError));
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

        when(flowStoreServiceConnector.findAllFlows()).thenReturn(Collections.singletonList(flow));
        try {
            final List<FlowModel> allFlows = flowStoreProxy.findAllFlows();
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
        final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl(flowStoreServiceConnector);
        when(flowStoreServiceConnector.findAllFlowBinders()).thenThrow(new FlowStoreServiceConnectorUnexpectedStatusCodeException("DIED", 500));
        try {
            flowStoreProxy.findAllFlowBinders();
            fail("No INTERNAL_SERVER_ERROR was thrown by findAllFlowBinders()");
        } catch (ProxyException e) {
            assertThat(e.getErrorCode(), is(ProxyError.INTERNAL_SERVER_ERROR));
        }
    }

    @Test
    public void findAllFlowBinders_remoteServiceReturnsHttpStatusOk_returnsListOfFlowBinderEntity() throws Exception {

        final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl(flowStoreServiceConnector);
        final FlowBinder flowBinder = new FlowBinderBuilder().setId(1L).build();

        when(flowStoreServiceConnector.findAllFlowBinders()).thenReturn(Collections.singletonList(flowBinder));
        when(flowStoreServiceConnector.getFlow(anyLong())).thenReturn(defaultFlow);
        when(flowStoreServiceConnector.getSubmitter(anyLong())).thenReturn(defaultSubmitter);
        when(flowStoreServiceConnector.getSink(anyLong())).thenReturn(defaultSink);
        try {
            final List<FlowBinderModel> allFlowBinderModels = flowStoreProxy.findAllFlowBinders();
            assertNotNull(allFlowBinderModels);
            assertThat(allFlowBinderModels.size(), is(1));
            FlowBinderModel model = allFlowBinderModels.get(0);
            assertThat(model.getId(), is(flowBinder.getId()));
            assertThat(model.getName(), is(flowBinder.getContent().getName()));
            assertThat(model.getFlowModel().getId(), is(DEFAULT_FLOW_ID));
            assertThat(model.getSubmitterModels().get(0).getId(), is(DEFAULT_SUBMITTER_ID));
            assertThat(model.getSinkModel().getId(), is(DEFAULT_SINK_ID));
        } catch (ProxyException e) {
            fail("Unexpected error when calling: findAllFlowBinders()");
        }
    }


    /*
     * Test getFlowBinder
     */
    @Test(expected = NullPointerException.class)
    public void getFlowBinder_getFlowBinderReturnsNull_throws() throws Exception {
        final long FLOW_BINDER_ID = 1L;
        final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl(flowStoreServiceConnector);
        when(flowStoreServiceConnector.getFlowBinder(FLOW_BINDER_ID)).thenReturn(null);

        flowStoreProxy.getFlowBinder(FLOW_BINDER_ID);
    }

    @Test
    public void getFlowBinder_getFlowBinderReturnsHttpStatusInternalServerError_throws() throws Exception {
        final long FLOW_BINDER_ID = 1L;
        final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl(flowStoreServiceConnector);
        when(flowStoreServiceConnector.getFlowBinder(FLOW_BINDER_ID)).thenThrow(new FlowStoreServiceConnectorUnexpectedStatusCodeException("DIED", 500));

        try {
            flowStoreProxy.getFlowBinder(FLOW_BINDER_ID);
            fail("No INTERNAL_SERVER_ERROR was thrown by getFlowBinder()");
        } catch (ProxyException e) {
            assertThat(e.getErrorCode(), is(ProxyError.INTERNAL_SERVER_ERROR));
        }
    }

    @Test(expected = NullPointerException.class)
    public void getFlowBinder_getFlowReturnsNull_throws() throws Exception {
        final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl(flowStoreServiceConnector);
        when(flowStoreServiceConnector.getFlow(DEFAULT_FLOW_ID)).thenReturn(null);
        when(flowStoreServiceConnector.getSubmitter(DEFAULT_SUBMITTER_ID)).thenReturn(defaultSubmitter);
        when(flowStoreServiceConnector.getSink(DEFAULT_SINK_ID)).thenReturn(defaultSink);
        FlowBinder flowBinder = new FlowBinder(DEFAULT_FLOW_BINDER_ID, DEFAULT_FLOW_BINDER_VERSION, defaultFlowBinderContent);
        when(flowStoreServiceConnector.getFlowBinder(DEFAULT_FLOW_BINDER_ID)).thenReturn(flowBinder);

        flowStoreProxy.getFlowBinder(DEFAULT_FLOW_BINDER_ID);
    }

    @Test
    public void getFlowBinder_getFlowReturnsHttpStatusInternalServerError_throws() throws Exception {
        final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl(flowStoreServiceConnector);
        when(flowStoreServiceConnector.getFlow(DEFAULT_FLOW_ID)).thenThrow(new FlowStoreServiceConnectorUnexpectedStatusCodeException("DIED", 500));
        when(flowStoreServiceConnector.getSubmitter(DEFAULT_SUBMITTER_ID)).thenReturn(defaultSubmitter);
        when(flowStoreServiceConnector.getSink(DEFAULT_SINK_ID)).thenReturn(defaultSink);
        FlowBinder flowBinder = new FlowBinder(DEFAULT_FLOW_BINDER_ID, DEFAULT_FLOW_BINDER_VERSION, defaultFlowBinderContent);
        when(flowStoreServiceConnector.getFlowBinder(DEFAULT_FLOW_BINDER_ID)).thenReturn(flowBinder);

        try {
            flowStoreProxy.getFlowBinder(DEFAULT_FLOW_BINDER_ID);
            fail("No INTERNAL_SERVER_ERROR was thrown by getFlowBinder()");
        } catch (ProxyException e) {
            assertThat(e.getErrorCode(), is(ProxyError.INTERNAL_SERVER_ERROR));
        }
    }

    @Test(expected = NullPointerException.class)
    public void getFlowBinder_getSubmitterReturnsNull_throws() throws Exception {
        final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl(flowStoreServiceConnector);
        when(flowStoreServiceConnector.getFlow(DEFAULT_FLOW_ID)).thenReturn(defaultFlow);
        when(flowStoreServiceConnector.getSubmitter(DEFAULT_SUBMITTER_ID)).thenReturn(null);
        when(flowStoreServiceConnector.getSink(DEFAULT_SINK_ID)).thenReturn(defaultSink);
        FlowBinder flowBinder = new FlowBinder(DEFAULT_FLOW_BINDER_ID, DEFAULT_FLOW_BINDER_VERSION, defaultFlowBinderContent);
        when(flowStoreServiceConnector.getFlowBinder(DEFAULT_FLOW_BINDER_ID)).thenReturn(flowBinder);

        flowStoreProxy.getFlowBinder(DEFAULT_FLOW_BINDER_ID);
    }

    @Test
    public void getFlowBinder_getSubmitterReturnsHttpStatusInternalServerError_throws() throws Exception {
        final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl(flowStoreServiceConnector);
        when(flowStoreServiceConnector.getFlow(DEFAULT_FLOW_ID)).thenReturn(defaultFlow);
        when(flowStoreServiceConnector.getSubmitter(DEFAULT_SUBMITTER_ID)).thenThrow(new FlowStoreServiceConnectorUnexpectedStatusCodeException("DIED", 500));
        when(flowStoreServiceConnector.getSink(DEFAULT_SINK_ID)).thenReturn(defaultSink);
        FlowBinder flowBinder = new FlowBinder(DEFAULT_FLOW_BINDER_ID, DEFAULT_FLOW_BINDER_VERSION, defaultFlowBinderContent);
        when(flowStoreServiceConnector.getFlowBinder(DEFAULT_FLOW_BINDER_ID)).thenReturn(flowBinder);

        try {
            flowStoreProxy.getFlowBinder(DEFAULT_FLOW_BINDER_ID);
            fail("No INTERNAL_SERVER_ERROR was thrown by getFlowBinder()");
        } catch (ProxyException e) {
            assertThat(e.getErrorCode(), is(ProxyError.INTERNAL_SERVER_ERROR));
        }
    }

    @Test(expected = NullPointerException.class)
    public void getFlowBinder_getSinkReturnsNull_throws() throws Exception {
        final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl(flowStoreServiceConnector);
        when(flowStoreServiceConnector.getFlow(DEFAULT_FLOW_ID)).thenReturn(defaultFlow);
        when(flowStoreServiceConnector.getSubmitter(DEFAULT_SUBMITTER_ID)).thenReturn(defaultSubmitter);
        when(flowStoreServiceConnector.getSink(DEFAULT_SINK_ID)).thenReturn(null);
        FlowBinder flowBinder = new FlowBinder(DEFAULT_FLOW_BINDER_ID, DEFAULT_FLOW_BINDER_VERSION, defaultFlowBinderContent);
        when(flowStoreServiceConnector.getFlowBinder(DEFAULT_FLOW_BINDER_ID)).thenReturn(flowBinder);

        flowStoreProxy.getFlowBinder(DEFAULT_FLOW_BINDER_ID);
    }

    @Test
    public void getFlowBinder_getSinkReturnsHttpStatusInternalServerError_throws() throws Exception {
        final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl(flowStoreServiceConnector);
        when(flowStoreServiceConnector.getFlow(DEFAULT_FLOW_ID)).thenReturn(defaultFlow);
        when(flowStoreServiceConnector.getSubmitter(DEFAULT_SUBMITTER_ID)).thenReturn(defaultSubmitter);
        when(flowStoreServiceConnector.getSink(DEFAULT_SINK_ID)).thenThrow(new FlowStoreServiceConnectorUnexpectedStatusCodeException("DIED", 500));
        FlowBinder flowBinder = new FlowBinder(DEFAULT_FLOW_BINDER_ID, DEFAULT_FLOW_BINDER_VERSION, defaultFlowBinderContent);
        when(flowStoreServiceConnector.getFlowBinder(DEFAULT_FLOW_BINDER_ID)).thenReturn(flowBinder);

        try {
            flowStoreProxy.getFlowBinder(DEFAULT_FLOW_BINDER_ID);
            fail("No INTERNAL_SERVER_ERROR was thrown by getFlowBinder()");
        } catch (ProxyException e) {
            assertThat(e.getErrorCode(), is(ProxyError.INTERNAL_SERVER_ERROR));
        }
    }

    @Test
    public void getFlowBinder_allRemoteServicesReturnsHttpStatusOk_returnsFlowBinderModel() throws Exception {
        final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl(flowStoreServiceConnector);
        when(flowStoreServiceConnector.getFlow(DEFAULT_FLOW_ID)).thenReturn(defaultFlow);
        when(flowStoreServiceConnector.getSubmitter(DEFAULT_SUBMITTER_ID)).thenReturn(defaultSubmitter);
        when(flowStoreServiceConnector.getSink(DEFAULT_SINK_ID)).thenReturn(defaultSink);
        FlowBinder flowBinder = new FlowBinder(DEFAULT_FLOW_BINDER_ID, DEFAULT_FLOW_BINDER_VERSION, defaultFlowBinderContent);
        when(flowStoreServiceConnector.getFlowBinder(DEFAULT_FLOW_BINDER_ID)).thenReturn(flowBinder);

        FlowBinderModel model = flowStoreProxy.getFlowBinder(DEFAULT_FLOW_BINDER_ID);

        assertThat(model.getId(), is(DEFAULT_FLOW_BINDER_ID));
        assertThat(model.getVersion(), is(DEFAULT_FLOW_BINDER_VERSION));
        assertThat(model.getName(), is(defaultFlowBinderContent.getName()));
        assertThat(model.getDescription(), is(defaultFlowBinderContent.getDescription()));
        assertThat(model.getPackaging(), is(defaultFlowBinderContent.getPackaging()));
        assertThat(model.getFormat(), is(defaultFlowBinderContent.getFormat()));
        assertThat(model.getCharset(), is(defaultFlowBinderContent.getCharset()));
        assertThat(model.getDestination(), is(defaultFlowBinderContent.getDestination()));
        assertThat(model.getRecordSplitter(), is(defaultFlowBinderContent.getRecordSplitter()));
        assertThat(model.getFlowModel().getId(), is(defaultFlow.getId()));
        assertThat(model.getFlowModel().getFlowName(), is(defaultFlow.getContent().getName()));
        assertThat(model.getSubmitterModels().size(), is(1));
        assertThat(model.getSubmitterModels().get(0).getId(), is(DEFAULT_SUBMITTER_ID));
        assertThat(model.getSubmitterModels().get(0).getName(), is(defaultSubmitter.getContent().getName()));
        assertThat(model.getSinkModel().getId(), is(DEFAULT_SINK_ID));
        assertThat(model.getSinkModel().getSinkName(), is(defaultSink.getContent().getName()));
    }

    /*
    * Test createFlowBinder
    */

    @Test
    public void createFlowBinder_remoteServiceReturnsHttpStatusCreated_returnsFlowBinderEntity() throws Exception {
        final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl(flowStoreServiceConnector);
        final FlowBinderContent flowBinderContent = new FlowBinderContentBuilder().build();
        final FlowBinder flowBinder = new FlowBinderBuilder().setContent(flowBinderContent).build();
        final Flow updatedFlow = new FlowBuilder().setId(123L).setVersion(456L).build();
        final Sink updatedSink = new SinkBuilder().setId(43L).setVersion(876L).build();
        final Submitter updatedSubmitter = new SubmitterBuilder().setId(8487L).setVersion(848L).build();

        when(flowStoreServiceConnector.createFlowBinder(any(FlowBinderContent.class))).thenReturn(flowBinder);
        when(flowStoreServiceConnector.getFlow(anyLong())).thenReturn(updatedFlow);
        when(flowStoreServiceConnector.getSink(anyLong())).thenReturn(updatedSink);
        when(flowStoreServiceConnector.getSubmitter(anyLong())).thenReturn(updatedSubmitter);

        final FlowBinderModel createdFlowBinder = flowStoreProxy.createFlowBinder(getDefaultFlowBinderModel(121L, 212L));
        assertNotNull(createdFlowBinder);
        assertThat(createdFlowBinder.getFlowModel().getId(), is(updatedFlow.getId()));
        assertThat(createdFlowBinder.getSinkModel().getId(), is(updatedSink.getId()));
        assertThat(createdFlowBinder.getSubmitterModels().size(), is(1));
        assertThat(createdFlowBinder.getSubmitterModels().get(0).getId(), is(updatedSubmitter.getId()));
    }

    @Test
    public void createFlowBinder_remoteServiceReturnsHttpStatusInternalServerError_throws() throws Exception {
        createFlowBinder_genericTestImplForHttpErrors(500, ProxyError.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR");
    }

    @Test
    public void createFlowBinder_remoteServiceReturnsHttpStatusNotAcceptable_throws() throws Exception {
        createFlowBinder_genericTestImplForHttpErrors(406, ProxyError.NOT_ACCEPTABLE, "NOT_ACCEPTABLE");
    }

    @Test
    public void createFlowBinder_remoteServiceReturnsHttpStatusBadRequest_throws() throws Exception {
        createFlowBinder_genericTestImplForHttpErrors(400, ProxyError.BAD_REQUEST, "BAD_REQUEST");
    }

    @Test
    public void createFlowBinder_remoteServiceReturnsHttpStatusPreConditionFailed_throws() throws Exception {
        createFlowBinder_genericTestImplForHttpErrors(412, ProxyError.PRECONDITION_FAILED, "PRECONDITION_FAILED");
    }

    private void createFlowBinder_genericTestImplForHttpErrors(int errorCodeToReturn, ProxyError expectedError, String expectedErrorName) throws Exception {
        final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl(flowStoreServiceConnector);

        when(flowStoreServiceConnector.createFlowBinder(any(FlowBinderContent.class)))
                .thenThrow(new FlowStoreServiceConnectorUnexpectedStatusCodeException("DIED", errorCodeToReturn));
        try {
            flowStoreProxy.createFlowBinder(getDefaultFlowBinderModel(443L, 554L));
            fail("No " + expectedErrorName + " error was thrown by createFlowBinder()");
        } catch (ProxyException e) {
            assertThat(e.getErrorCode(), is(expectedError));
        }
    }

    //****************************
    @Test
    public void updateFlowBinder_remoteServiceReturnsHttpStatusOk_returnsFlowBinderModelEntity() throws Exception {
        final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl(flowStoreServiceConnector);
        final FlowBinder flowBinder = new FlowBinderBuilder().setId(ID).setVersion(1).build();
        final Submitter submitter = new SubmitterBuilder().setId(ID).setVersion(1).build();
        final Flow flow = new FlowBuilder().setId(ID).setVersion(1).build();
        final Sink sink = new SinkBuilder().setId(ID).setVersion(1).build();
        FlowBinderModel model = getDefaultFlowBinderModel(flowBinder.getId(), flowBinder.getVersion());

        when(flowStoreServiceConnector.updateFlowBinder(any(FlowBinderContent.class), (eq(flowBinder.getId())), (eq(flowBinder.getVersion()))))
                .thenReturn(flowBinder);
        when(flowStoreServiceConnector.getSubmitter(anyLong())).thenReturn(submitter);
        when(flowStoreServiceConnector.getFlow(anyLong())).thenReturn(flow);
        when(flowStoreServiceConnector.getSink(anyLong())).thenReturn(sink);
        try {
            final FlowBinderModel updatedModel = flowStoreProxy.updateFlowBinder(model);
            assertNotNull(updatedModel);
        } catch (ProxyException e) {
            fail("Unexpected error when calling: updateFlowBinder()");
        }
    }

    @Test
    public void updateFlowBinder_remoteServiceReturnsHttpStatusNotFound_throws() throws Exception {
        updateFlowBinder_genericTestImplForHttpErrors(404, ProxyError.ENTITY_NOT_FOUND, "ENTITY_NOT_FOUND");
    }

    @Test
    public void updateFlowBinder_remoteServiceReturnsHttpStatusNotAcceptable_throws() throws Exception {
        updateFlowBinder_genericTestImplForHttpErrors(406, ProxyError.NOT_ACCEPTABLE, "NOT_ACCEPTABLE");
    }

    @Test
    public void updateFlowBinder_remoteServiceReturnsHttpStatusConflict_throws() throws Exception {
        updateFlowBinder_genericTestImplForHttpErrors(409, ProxyError.CONFLICT_ERROR, "CONFLICT_ERROR");
    }

    @Test
    public void updateFlowBinder_remoteServiceReturnsHttpStatusInternalServerError_throws() throws Exception {
        updateFlowBinder_genericTestImplForHttpErrors(500, ProxyError.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR");
    }

    @Test
    public void updateFlowBinder_throwsIllegalArgumentException() throws Exception {
        IllegalArgumentException illegalArgumentException = new IllegalArgumentException("DIED");
        FlowBinderModel model = getDefaultFlowBinderModel(1, 1);
        model.setSinkModel(new SinkModel());
        updateFlowBinder_testForProxyError(model, illegalArgumentException, ProxyError.MODEL_MAPPER_INVALID_FIELD_VALUE, "MODEL_MAPPER_INVALID_FIELD_VALUE");
    }

    private FlowBinderModel getDefaultFlowBinderModel(long id, long version) {
        FlowModel flowModel = getDefaultFlowModel(id, version);
        SinkModel sinkModel = getDefaultSinkModel(id, version);
        SubmitterModel submitterModel = new SubmitterModelBuilder().setId(id).setVersion(version).build();

        return new FlowBinderModel(
                1,
                1,
                "flowBinderName",
                "flowBinderDescription",
                "flowBinderPackaging",
                "format",
                "charset",
                "destination",
                "recordSplitter",
                true,
                flowModel,
                Collections.singletonList(submitterModel),
                sinkModel);
    }

    private void updateFlowBinder_genericTestImplForHttpErrors(int errorCodeToReturn, ProxyError expectedError, String expectedErrorName) throws Exception {
        final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl(flowStoreServiceConnector);
        final FlowBinder flowBinder = new FlowBinderBuilder().setId(ID).setVersion(1).build();

        FlowBinderModel model = getDefaultFlowBinderModel(flowBinder.getId(), flowBinder.getVersion());

        when(flowStoreServiceConnector.updateFlowBinder(any(FlowBinderContent.class), (eq(flowBinder.getId())), (eq(flowBinder.getVersion()))))
                .thenThrow(new FlowStoreServiceConnectorUnexpectedStatusCodeException("DIED", errorCodeToReturn));
        try {
            flowStoreProxy.updateFlowBinder(model);
            fail("No " + expectedErrorName + " error was thrown by updateFlowBinder()");
        } catch (ProxyException e) {
            assertThat(e.getErrorCode(), is(expectedError));
        }
    }

    private void updateFlowBinder_testForProxyError(FlowBinderModel model, Exception exception, ProxyError expectedError, String expectedErrorName) throws Exception {
        final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl(flowStoreServiceConnector);
        final FlowBinder flowBinder = new FlowBinderBuilder().setId(ID).setVersion(1).build();

        when(flowStoreServiceConnector.updateFlowBinder(any(FlowBinderContent.class), (eq(flowBinder.getId())), (eq(flowBinder.getVersion()))))
                .thenThrow(exception);

        try {
            flowStoreProxy.updateFlowBinder(model);
            fail("No " + expectedErrorName + " error was thrown by updateFlowBinder()");
        } catch (ProxyException e) {
            assertThat(e.getErrorCode(), is(expectedError));
        }
    }

    /*
    * Test getFlowComponent
    */

    @Test
    public void getFlowComponent_remoteServiceReturnsHttpStatusOk_returnsFlowComponentModelEntity() throws Exception {
        final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        final JavaScriptProjectFetcherImpl javaScriptProjectFetcher = mock(JavaScriptProjectFetcherImpl.class);
        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl(flowStoreServiceConnector, javaScriptProjectFetcher);
        final FlowComponent flowComponent = new FlowComponentBuilder().setId(ID).build();
        when(flowStoreServiceConnector.getFlowComponent(eq(ID))).thenReturn(flowComponent);

        try {
            final FlowComponentModel retrievedModel = flowStoreProxy.getFlowComponent(flowComponent.getId());
            assertNotNull(retrievedModel);
        } catch (ProxyException e) {
            fail("Unexpected error when calling: getFlowComponentModel()");
        }
    }

    @Test
    public void getFlowComponent_remoteServiceReturnsHttpStatusInternalServerError_throws() throws Exception {
        getFlow_genericTestImplForHttpErrors(500, ProxyError.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR");
    }

    @Test
    public void getFlowComponent_remoteServiceReturnsHttpStatusNotFound_throws() throws Exception {
        getFlowComponent_genericTestImplForHttpErrors(404, ProxyError.ENTITY_NOT_FOUND, "ENTITY_NOT_FOUND");
    }

    private void getFlowComponent_genericTestImplForHttpErrors(int errorCodeToReturn, ProxyError expectedError, String expectedErrorName) throws Exception {
        final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        final JavaScriptProjectFetcherImpl javaScriptProjectFetcher = mock(JavaScriptProjectFetcherImpl.class);
        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl(flowStoreServiceConnector, javaScriptProjectFetcher);
        when(flowStoreServiceConnector.getFlowComponent(eq(ID))).thenThrow(new FlowStoreServiceConnectorUnexpectedStatusCodeException("DIED", errorCodeToReturn));

        try {
            flowStoreProxy.getFlowComponent(ID);
            fail("No " + expectedErrorName + " error was thrown by getFlowComponent()");
        } catch (ProxyException e) {
            assertThat(e.getErrorCode(), is(expectedError));
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
            flowStoreProxy.refreshFlowComponents(flow.getId(), flow.getVersion());
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
            flowStoreProxy.refreshFlowComponents(flow.getId(), flow.getVersion());
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
            final FlowModel updatedFlow = flowStoreProxy.refreshFlowComponents(flow.getId(), flow.getVersion());
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
        FlowModel model = getDefaultFlowModel(flow);

        when(flowStoreServiceConnector.getFlow(any(Long.class))).thenReturn(flow);
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
        FlowModel model = getDefaultFlowModel(new FlowBuilder().build());
        model.setFlowName("");
        updateFlow_testForProxyError(model, illegalArgumentException, ProxyError.MODEL_MAPPER_INVALID_FIELD_VALUE, "MODEL_MAPPER_INVALID_FIELD_VALUE");
    }


    private void updateFlow_genericTestImplForHttpErrors(int errorCodeToReturn, ProxyError expectedError, String expectedErrorName) throws Exception {
        final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl(flowStoreServiceConnector);
        final Flow flow = new FlowBuilder().setId(ID).setVersion(1).build();
        FlowModel model = getDefaultFlowModel(flow);
        when(flowStoreServiceConnector.getFlow(any(Long.class))).thenReturn(flow);
        when(flowStoreServiceConnector.updateFlow(any(FlowContent.class), (eq(flow.getId())), (eq(flow.getVersion()))))
                .thenThrow(new FlowStoreServiceConnectorUnexpectedStatusCodeException("DIED", errorCodeToReturn));
        try {
            flowStoreProxy.updateFlow(model);
            fail("No " + expectedErrorName + " error was thrown by updateFlow()");
        } catch (ProxyException e) {
            assertThat(e.getErrorCode(), is(expectedError));
        }
    }

    private FlowModel getDefaultFlowModel(Flow flow) {
        List<FlowComponentModel> flowComponentModels = new ArrayList<FlowComponentModel>();
        for (FlowComponent flowComponent : flow.getContent().getComponents()) {
            flowComponentModels.add(FlowComponentModelMapper.toModel(flowComponent));
        }
        return new FlowModel(flow.getId(), flow.getVersion(), flow.getContent().getName(), flow.getContent().getDescription(), flowComponentModels);
    }


    private void updateFlow_testForProxyError(FlowModel model, Exception exception, ProxyError expectedError, String expectedErrorName) throws Exception {
        final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl(flowStoreServiceConnector);
        final Flow flow = new FlowBuilder().setId(ID).setVersion(1).build();

        when(flowStoreServiceConnector.getFlow(any(Long.class))).thenReturn(flow);
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
    public void createFlowComponent_remoteServiceReturnsHttpStatusCreated_returnsFlowComponentModelEntity() throws Exception {
        final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        final JavaScriptProjectFetcherImpl javaScriptProjectFetcher = mock(JavaScriptProjectFetcherImpl.class);
        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl(flowStoreServiceConnector, javaScriptProjectFetcher);
        final FlowComponent flowComponent = new FlowComponentBuilder().build();
        final FlowComponentModel model = getDefaultFlowComponentModel();

        when(flowStoreServiceConnector.createFlowComponent(any(FlowComponentContent.class))).thenReturn(flowComponent);
        when(javaScriptProjectFetcher.fetchRequiredJavaScript(
                model.getSvnProject(),
                Long.valueOf(model.getSvnRevision()),
                model.getInvocationJavascript(),
                model.getInvocationMethod()))
                .thenReturn(getDefaultJavaScripts());
        try {
            final FlowComponentModel createdModel = flowStoreProxy.createFlowComponent(getDefaultFlowComponentModel());
            assertNotNull(createdModel);
        } catch (ProxyException e) {
            fail("Unexpected error when calling: createFlowComponent()");
        }
    }

    @Test
    public void createFlowComponent_remoteServiceReturnsHttpStatusInternalServerError_throws() throws Exception {
        createFlowComponent_genericTestImplForHttpErrors(500, ProxyError.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR");
    }

    @Test
    public void createFlowComponent_remoteServiceReturnsHttpStatusNotAcceptable_throws() throws Exception {
        createFlowComponent_genericTestImplForHttpErrors(406, ProxyError.NOT_ACCEPTABLE, "NOT_ACCEPTABLE");
    }

    @Test
    public void createFlowComponent_throwsIllegalArgumentException() throws Exception {
        IllegalArgumentException illegalArgumentException = new IllegalArgumentException("DIED");
        FlowComponentModel model = getDefaultFlowComponentModel();
        model.setName("");
        createFlowComponent_testForProxyError(model, illegalArgumentException, ProxyError.MODEL_MAPPER_INVALID_FIELD_VALUE, "MODEL_MAPPER_INVALID_FIELD_VALUE");
    }

    private void createFlowComponent_genericTestImplForHttpErrors(int errorCodeToReturn, ProxyError expectedError, String expectedErrorName) throws Exception {
        final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        final JavaScriptProjectFetcherImpl javaScriptProjectFetcher = mock(JavaScriptProjectFetcherImpl.class);
        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl(flowStoreServiceConnector, javaScriptProjectFetcher);

        FlowComponentModel model = getDefaultFlowComponentModel();
        when(javaScriptProjectFetcher.fetchRequiredJavaScript(
                model.getSvnProject(),
                Long.valueOf(model.getSvnRevision()),
                model.getInvocationJavascript(),
                model.getInvocationMethod()))
                .thenReturn(getDefaultJavaScripts());

        when(flowStoreServiceConnector.createFlowComponent(any(FlowComponentContent.class)))
                .thenThrow(new FlowStoreServiceConnectorUnexpectedStatusCodeException("DIED", errorCodeToReturn));
        try {
            flowStoreProxy.createFlowComponent(getDefaultFlowComponentModel());
            fail("No " + expectedErrorName + " error was thrown by createFlowComponent()");
        } catch (ProxyException e) {
            assertThat(e.getErrorCode(), is(expectedError));
        }
    }

    private void createFlowComponent_testForProxyError(FlowComponentModel model, Exception exception, ProxyError expectedError, String expectedErrorName) throws Exception {
        final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        final JavaScriptProjectFetcherImpl javaScriptProjectFetcher = mock(JavaScriptProjectFetcherImpl.class);
        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl(flowStoreServiceConnector, javaScriptProjectFetcher);

        when(flowStoreServiceConnector.createFlowComponent(any(FlowComponentContent.class)))
                .thenThrow(exception);

        try {
            flowStoreProxy.createFlowComponent(model);
            fail("No " + expectedErrorName + " error was thrown by createFlowComponent()");
        } catch (ProxyException e) {
            assertThat(e.getErrorCode(), is(expectedError));
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
            fail("No INTERNAL_SERVER_ERROR was thrown by findAllFlowComponents()");
        } catch (ProxyException e) {
            assertThat(e.getErrorCode(), is(ProxyError.INTERNAL_SERVER_ERROR));
        }
    }

    @Test
    public void findAllFlowComponents_remoteServiceReturnsHttpStatusOk_returnsListOfFlowComponentEntity() throws Exception {

        final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl(flowStoreServiceConnector);
        final FlowComponent flowComponent = new FlowComponentBuilder().setId(1L).build();

        when(flowStoreServiceConnector.findAllFlowComponents()).thenReturn(Collections.singletonList(flowComponent));
        try {
            final List<FlowComponentModel> allFlowComponents = flowStoreProxy.findAllFlowComponents();
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
    public void updateFlowComponent_remoteServiceReturnsHttpStatusOk_returnsFlowComponentModelEntity() throws Exception {
        final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        final JavaScriptProjectFetcherImpl javaScriptProjectFetcher = mock(JavaScriptProjectFetcherImpl.class);
        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl(flowStoreServiceConnector, javaScriptProjectFetcher);

        final FlowComponent flowComponent = new FlowComponentBuilder().setId(ID).setVersion(1L).build();
        FlowComponentModel model = getDefaultFlowComponentModel();

        when(flowStoreServiceConnector.getFlowComponent(any(Long.class))).thenReturn(flowComponent);
        when(flowStoreServiceConnector.updateFlowComponent(any(FlowComponentContent.class), (eq(flowComponent.getId())), (eq(flowComponent.getVersion()))))
                .thenReturn(flowComponent);
        when(javaScriptProjectFetcher.fetchRequiredJavaScript(
                model.getSvnProject(),
                Long.valueOf(model.getSvnRevision()),
                model.getInvocationJavascript(),
                model.getInvocationMethod()))
                .thenReturn(getDefaultJavaScripts());
        try {
            final FlowComponentModel updatedModel = flowStoreProxy.updateFlowComponent(model);
            assertNotNull(updatedModel);
        } catch (ProxyException e) {
            fail("Unexpected error when calling: updateFlowComponent()");
        }
    }

    @Test
    public void updateFlowComponent_remoteServiceReturnsHttpStatusNotAcceptable_throws() throws Exception {
        updateFlowComponent_genericTestImplForHttpErrors(406, ProxyError.NOT_ACCEPTABLE, "NOT_ACCEPTABLE");
    }

    @Test
    public void updateFlowComponent_remoteServiceReturnsHttpStatusConflict_throws() throws Exception {
        updateFlowComponent_genericTestImplForHttpErrors(409, ProxyError.CONFLICT_ERROR, "CONFLICT_ERROR");
    }

    @Test
    public void updateFlowComponent_remoteServiceReturnsHttpStatusInternalServerError_throws() throws Exception {
        updateFlowComponent_genericTestImplForHttpErrors(500, ProxyError.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR");
    }

    @Test
    public void updateFlowComponent_throwsIllegalArgumentException() throws Exception {
        IllegalArgumentException illegalArgumentException = new IllegalArgumentException("DIED");
        FlowComponentModel model = getDefaultFlowComponentModel();
        model.setName("");
        updateFlowComponent_testForProxyError(model, illegalArgumentException, ProxyError.MODEL_MAPPER_INVALID_FIELD_VALUE, "MODEL_MAPPER_INVALID_FIELD_VALUE");
    }


    private void updateFlowComponent_genericTestImplForHttpErrors(int errorCodeToReturn, ProxyError expectedError, String expectedErrorName) throws Exception {
        final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        final JavaScriptProjectFetcherImpl javaScriptProjectFetcher = mock(JavaScriptProjectFetcherImpl.class);
        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl(flowStoreServiceConnector, javaScriptProjectFetcher);
        final FlowComponent flowComponent = new FlowComponentBuilder().setId(ID).setVersion(1L).build();

        FlowComponentModel model = getDefaultFlowComponentModel();
        when(javaScriptProjectFetcher.fetchRequiredJavaScript(
                model.getSvnProject(),
                Long.valueOf(model.getSvnRevision()),
                model.getInvocationJavascript(),
                model.getInvocationMethod()))
                .thenReturn(getDefaultJavaScripts());

        when(flowStoreServiceConnector.getFlowComponent(any(Long.class))).thenReturn(flowComponent);
        when(flowStoreServiceConnector.updateFlowComponent(any(FlowComponentContent.class), (eq(flowComponent.getId())), (eq(flowComponent.getVersion()))))
                .thenThrow(new FlowStoreServiceConnectorUnexpectedStatusCodeException("DIED", errorCodeToReturn));
        try {
            flowStoreProxy.updateFlowComponent(model);
            fail("No " + expectedErrorName + " error was thrown by updateFlowComponent()");
        } catch (ProxyException e) {
            assertThat(e.getErrorCode(), is(expectedError));
        }
    }

    private void updateFlowComponent_testForProxyError(FlowComponentModel model, Exception exception, ProxyError expectedError, String expectedErrorName) throws Exception {
        final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        final JavaScriptProjectFetcherImpl javaScriptProjectFetcher = mock(JavaScriptProjectFetcherImpl.class);
        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl(flowStoreServiceConnector, javaScriptProjectFetcher);
        final FlowComponent flowComponent = new FlowComponentBuilder().setId(ID).setVersion(1L).build();

        when(flowStoreServiceConnector.getFlowComponent(any(Long.class))).thenReturn(flowComponent);
        when(flowStoreServiceConnector.updateFlowComponent(any(FlowComponentContent.class), (eq(flowComponent.getId())), (eq(flowComponent.getVersion()))))
                .thenThrow(exception);

        try {
            flowStoreProxy.updateFlowComponent(model);
            fail("No " + expectedErrorName + " error was thrown by updateFlowComponent()");
        } catch (ProxyException e) {
            assertThat(e.getErrorCode(), is(expectedError));
        }
    }

    private JavaScriptProjectFetcher.fetchRequiredJavaScriptResult getDefaultJavaScripts() {
        List<JavaScript> javaScripts = new ArrayList<JavaScript>(2);
        javaScripts.add(new JavaScript("javascript1", "javaScriptName1"));
        javaScripts.add(new JavaScript("javascript2", "javaScriptName2"));
        return new JavaScriptProjectFetcher.fetchRequiredJavaScriptResult(javaScripts, null);
    }

    private FlowComponentModel getDefaultFlowComponentModel() {
        List<String> javaScriptModules = new ArrayList<String>();
        javaScriptModules.add("javaScriptName1");
        javaScriptModules.add("javaScriptName2");

        return new FlowComponentModel(ID, 1, "name", "project", "45", "invocationJavaScript", "invocationMethod", javaScriptModules, "description");
    }
}
