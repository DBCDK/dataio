/*
 * DataIO - Data IO
 * Copyright (C) 2015 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
 * Denmark. CVR: 15149043
 *
 * This file is part of DataIO.
 *
 * DataIO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DataIO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DataIO.  If not, see <http://www.gnu.org/licenses/>.
 */

package dk.dbc.dataio.gui.server;

import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorUnexpectedStatusCodeException;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.FlowBinder;
import dk.dbc.dataio.commons.types.FlowBinderContent;
import dk.dbc.dataio.commons.types.FlowComponent;
import dk.dbc.dataio.commons.types.FlowComponentContent;
import dk.dbc.dataio.commons.types.FlowContent;
import dk.dbc.dataio.commons.types.GatekeeperDestination;
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
import dk.dbc.dataio.commons.utils.test.model.FlowComponentContentBuilder;
import dk.dbc.dataio.commons.utils.test.model.GatekeeperDestinationBuilder;
import dk.dbc.dataio.commons.utils.test.model.SinkBuilder;
import dk.dbc.dataio.commons.utils.test.model.SubmitterBuilder;
import dk.dbc.dataio.gui.client.exceptions.ProxyError;
import dk.dbc.dataio.gui.client.exceptions.ProxyException;
import dk.dbc.dataio.gui.client.model.FlowBinderModel;
import dk.dbc.dataio.gui.client.model.FlowComponentModel;
import dk.dbc.dataio.gui.client.model.FlowModel;
import dk.dbc.dataio.gui.client.model.SinkModel;
import dk.dbc.dataio.gui.client.model.SubmitterModel;
import dk.dbc.dataio.gui.client.modelBuilders.FlowBinderModelBuilder;
import dk.dbc.dataio.gui.client.modelBuilders.FlowComponentModelBuilder;
import dk.dbc.dataio.gui.client.modelBuilders.FlowModelBuilder;
import dk.dbc.dataio.gui.client.modelBuilders.SinkModelBuilder;
import dk.dbc.dataio.gui.client.modelBuilders.SubmitterModelBuilder;
import dk.dbc.dataio.gui.client.proxies.JavaScriptProjectFetcher;
import dk.dbc.dataio.gui.server.modelmappers.FlowComponentModelMapper;
import dk.dbc.dataio.harvester.types.OLDRRHarvesterConfig;
import dk.dbc.dataio.harvester.types.RRHarvesterConfig;
import dk.dbc.dataio.harvester.types.UshHarvesterProperties;
import dk.dbc.dataio.harvester.types.UshSolrHarvesterConfig;
import org.glassfish.jersey.client.ClientConfig;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.naming.NamingException;
import javax.ws.rs.client.Client;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.doThrow;
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
    private FlowBinderContent defaultFlowBinderContent = new FlowBinderContentBuilder()
            .setFlowId(DEFAULT_FLOW_ID)
            .setSubmitterIds(Collections.singletonList(DEFAULT_SUBMITTER_ID))
            .setSinkId(DEFAULT_SINK_ID)
            .build();

    @Before
    public void setup() throws Exception {
        mockStatic(ServiceUtil.class);
        mockStatic(HttpClient.class);
        String flowStoreServiceUrl = "http://dataio/flow-service";
        String ushHarvesterEndpoint = "http://dataio/ush-harvester-endpoint";
        when(ServiceUtil.getFlowStoreServiceEndpoint()).thenReturn(flowStoreServiceUrl);
        when(ServiceUtil.getUshHarvesterEndpoint()).thenReturn(ushHarvesterEndpoint);
        when(HttpClient.newClient(any(ClientConfig.class))).thenReturn(client);
    }


    // Constructor tests

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


    // Flows tests

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
            final FlowModel createdModel = flowStoreProxy.createFlow(new FlowModelBuilder().setId(0).setVersion(0).build());
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
        FlowModel model = new FlowModelBuilder().setId(0).setVersion(0).setName("").build();
        createFlow_testForProxyError(model, illegalArgumentException, ProxyError.MODEL_MAPPER_INVALID_FIELD_VALUE, "MODEL_MAPPER_INVALID_FIELD_VALUE");
    }

    private void createFlow_genericTestImplForHttpErrors(int errorCodeToReturn, ProxyError expectedError, String expectedErrorName) throws Exception {
        final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl(flowStoreServiceConnector);

        when(flowStoreServiceConnector.createFlow(any(FlowContent.class)))
                .thenThrow(new FlowStoreServiceConnectorUnexpectedStatusCodeException("DIED", errorCodeToReturn));
        try {
            flowStoreProxy.createFlow(new FlowModelBuilder().setId(0).setVersion(0).build());
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
        List<FlowComponentModel> flowComponentModels = new ArrayList<>();
        for (FlowComponent flowComponent : flow.getContent().getComponents()) {
            flowComponentModels.add(FlowComponentModelMapper.toModel(flowComponent));
        }
        return new FlowModelBuilder().setId(flow.getId()).setVersion(flow.getVersion()).setName(flow.getContent().getName()).setDescription(flow.getContent().getDescription()).setComponents(flowComponentModels).build();
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
     * Test deleteFlow
     */

    @Test
    public void deleteFlow_remoteServiceReturnsHttpStatusNoContent() throws Exception {
        final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl(flowStoreServiceConnector);

        try {
            flowStoreProxy.deleteFlow(ID, 1L);
        } catch (ProxyException e) {
            fail("Unexpected error when calling: deleteFlow()");
        }
    }

    @Test
    public void deleteFlow_remoteServiceReturnsHttpStatusNotFound_throws() throws Exception {
        deleteFlow_genericTestImplForHttpErrors(404, ProxyError.ENTITY_NOT_FOUND, "ENTITY_NOT_FOUND");
    }

    @Test
    public void deleteFlow_remoteServiceReturnsHttpStatusConflict_throws() throws Exception {
        deleteFlow_genericTestImplForHttpErrors(409, ProxyError.CONFLICT_ERROR, "CONFLICT_ERROR");
    }

    @Test
    public void deleteFlow_remoteServiceReturnsHttpStatusInternalServerError_throws() throws Exception {
        deleteFlow_genericTestImplForHttpErrors(500, ProxyError.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR");
    }

    private void deleteFlow_genericTestImplForHttpErrors(int errorCodeToReturn, ProxyError expectedError, String expectedErrorName) throws Exception {
        final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl(flowStoreServiceConnector);

        doThrow(new FlowStoreServiceConnectorUnexpectedStatusCodeException("msg", errorCodeToReturn)).when(flowStoreServiceConnector).deleteFlow(eq(ID), (eq(1L)));
        try {
            flowStoreProxy.deleteFlow(ID, 1);
            fail("No " + expectedErrorName + " error was thrown by deleteFlow()");
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


    // Flow Components tests

    /*
     * Test createFlowComponent
     */

    @Test
    public void createFlowComponent_remoteServiceReturnsHttpStatusCreated_returnsFlowComponentModelEntity() throws Throwable {
        final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        final JavaScriptProjectFetcherImpl javaScriptProjectFetcher = mock(JavaScriptProjectFetcherImpl.class);
        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl(flowStoreServiceConnector, javaScriptProjectFetcher);
        final FlowComponent flowComponent = new FlowComponentBuilder().setNext(new FlowComponentContentBuilder().build()).build();
        final FlowComponentModel model = new FlowComponentModelBuilder().setSvnNext(String.valueOf(flowComponent.getNext().getSvnRevision())).build();

        when(javaScriptProjectFetcher.fetchRequiredJavaScript(
                anyString(),
                anyLong(),
                anyString(),
                anyString()))
                .thenReturn(getDefaultJavaScripts());

        when(flowStoreServiceConnector.createFlowComponent(any(FlowComponentContent.class))).thenReturn(flowComponent);
        when(flowStoreServiceConnector.updateNext(any(FlowComponentContent.class), eq(flowComponent.getId()), (eq(flowComponent.getVersion())))).thenReturn(flowComponent);

        try {
            final FlowComponentModel createdModel = flowStoreProxy.createFlowComponent(model);
            assertNotNull(createdModel);
        } catch (ProxyException e) {
            fail("Unexpected error when calling: createFlowComponent()");
        }
    }

    @Test
    public void createFlowComponent_remoteServiceReturnsHttpStatusInternalServerError_throws() throws Throwable {
        createFlowComponent_genericTestImplForHttpErrors(500, ProxyError.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR");
    }

    @Test
    public void createFlowComponent_remoteServiceReturnsHttpStatusNotAcceptable_throws() throws Throwable {
        createFlowComponent_genericTestImplForHttpErrors(406, ProxyError.NOT_ACCEPTABLE, "NOT_ACCEPTABLE");
    }

    @Test
    public void createFlowComponent_throwsIllegalArgumentException() throws Throwable {
        IllegalArgumentException illegalArgumentException = new IllegalArgumentException("DIED");
        FlowComponentModel model = new FlowComponentModelBuilder().build();
        model.setName("");
        createFlowComponent_testForProxyError(model, illegalArgumentException, ProxyError.MODEL_MAPPER_INVALID_FIELD_VALUE, "MODEL_MAPPER_INVALID_FIELD_VALUE");
    }

    private void createFlowComponent_genericTestImplForHttpErrors(int errorCodeToReturn, ProxyError expectedError, String expectedErrorName) throws Throwable {
        final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        final JavaScriptProjectFetcherImpl javaScriptProjectFetcher = mock(JavaScriptProjectFetcherImpl.class);
        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl(flowStoreServiceConnector, javaScriptProjectFetcher);

        FlowComponentModel model = new FlowComponentModelBuilder().build();
        when(javaScriptProjectFetcher.fetchRequiredJavaScript(
                model.getSvnProject(),
                Long.valueOf(model.getSvnRevision()),
                model.getInvocationJavascript(),
                model.getInvocationMethod()))
                .thenReturn(getDefaultJavaScripts());

        when(flowStoreServiceConnector.createFlowComponent(any(FlowComponentContent.class)))
                .thenThrow(new FlowStoreServiceConnectorUnexpectedStatusCodeException("DIED", errorCodeToReturn));
        try {
            flowStoreProxy.createFlowComponent(new FlowComponentModelBuilder().build());
            fail("No " + expectedErrorName + " error was thrown by createFlowComponent()");
        } catch (ProxyException e) {
            assertThat(e.getErrorCode(), is(expectedError));
        }
    }

    private void createFlowComponent_testForProxyError(FlowComponentModel model, Exception exception, ProxyError expectedError, String expectedErrorName) throws Throwable {
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
     * Test updateFlowComponent
     */

    @Test
    public void updateFlowComponent_remoteServiceReturnsHttpStatusOk_returnsFlowComponentModelEntity() throws Throwable {
        final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        final JavaScriptProjectFetcherImpl javaScriptProjectFetcher = mock(JavaScriptProjectFetcherImpl.class);
        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl(flowStoreServiceConnector, javaScriptProjectFetcher);

        final FlowComponent flowComponent = new FlowComponentBuilder().setId(ID).setVersion(1L).setNext(new FlowComponentContentBuilder().build()).build();
        final FlowComponentModel model = new FlowComponentModelBuilder().setSvnNext(String.valueOf(flowComponent.getNext().getSvnRevision())).build();

        when(flowStoreServiceConnector.getFlowComponent(any(Long.class))).thenReturn(flowComponent);
        when(flowStoreServiceConnector.updateFlowComponent(any(FlowComponentContent.class), (eq(model.getId())), (eq(model.getVersion()))))
                .thenReturn(flowComponent);
        when(flowStoreServiceConnector.updateNext(any(FlowComponentContent.class), eq(flowComponent.getId()), eq(flowComponent.getVersion()))).thenReturn(flowComponent);
        when(javaScriptProjectFetcher.fetchRequiredJavaScript(
                anyString(),
                anyLong(),
                anyString(),
                anyString()))
                .thenReturn(getDefaultJavaScripts());
        try {
            final FlowComponentModel updatedModel = flowStoreProxy.updateFlowComponent(model);
            assertNotNull(updatedModel);
        } catch (ProxyException e) {
            fail("Unexpected error when calling: updateFlowComponent()");
        }
    }

    @Test
    public void updateFlowComponent_remoteServiceReturnsHttpStatusNotAcceptable_throws() throws Throwable {
        updateFlowComponent_genericTestImplForHttpErrors(406, ProxyError.NOT_ACCEPTABLE, "NOT_ACCEPTABLE");
    }

    @Test
    public void updateFlowComponent_remoteServiceReturnsHttpStatusConflict_throws() throws Throwable {
        updateFlowComponent_genericTestImplForHttpErrors(409, ProxyError.CONFLICT_ERROR, "CONFLICT_ERROR");
    }

    @Test
    public void updateFlowComponent_remoteServiceReturnsHttpStatusInternalServerError_throws() throws Throwable {
        updateFlowComponent_genericTestImplForHttpErrors(500, ProxyError.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR");
    }

    @Test
    public void updateFlowComponent_throwsIllegalArgumentException() throws Throwable {
        IllegalArgumentException illegalArgumentException = new IllegalArgumentException("DIED");
        FlowComponentModel model = new FlowComponentModelBuilder().build();
        model.setName("");
        updateFlowComponent_testForProxyError(model, illegalArgumentException, ProxyError.MODEL_MAPPER_INVALID_FIELD_VALUE, "MODEL_MAPPER_INVALID_FIELD_VALUE");
    }


    private void updateFlowComponent_genericTestImplForHttpErrors(int errorCodeToReturn, ProxyError expectedError, String expectedErrorName) throws Throwable {
        final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        final JavaScriptProjectFetcherImpl javaScriptProjectFetcher = mock(JavaScriptProjectFetcherImpl.class);
        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl(flowStoreServiceConnector, javaScriptProjectFetcher);
        final FlowComponent flowComponent = new FlowComponentBuilder().setId(ID).setVersion(1L).build();

        FlowComponentModel model = new FlowComponentModelBuilder().build();
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

    private void updateFlowComponent_testForProxyError(FlowComponentModel model, Exception exception, ProxyError expectedError, String expectedErrorName) throws Throwable {
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

    /*
     * Test refreshFlowComponents
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
     * Test deleteFlow
     */

    @Test
    public void deleteFlowComponent_remoteServiceReturnsHttpStatusNoContent() throws Exception {
        final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl(flowStoreServiceConnector);

        try {
            flowStoreProxy.deleteFlowComponent(ID, 1L);
        } catch (ProxyException e) {
            fail("Unexpected error when calling: deleteFlowComponent()");
        }
    }

    @Test
    public void deleteFlowComponent_remoteServiceReturnsHttpStatusNotFound_throws() throws Exception {
        deleteFlowComponent_genericTestImplForHttpErrors(404, ProxyError.ENTITY_NOT_FOUND, "ENTITY_NOT_FOUND");
    }

    @Test
    public void deleteFlowComponent_remoteServiceReturnsHttpStatusConflict_throws() throws Exception {
        deleteFlowComponent_genericTestImplForHttpErrors(409, ProxyError.CONFLICT_ERROR, "CONFLICT_ERROR");
    }

    @Test
    public void deleteFlowComponent_remoteServiceReturnsHttpStatusInternalServerError_throws() throws Exception {
        deleteFlowComponent_genericTestImplForHttpErrors(500, ProxyError.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR");
    }

    private void deleteFlowComponent_genericTestImplForHttpErrors(int errorCodeToReturn, ProxyError expectedError, String expectedErrorName) throws Exception {
        final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl(flowStoreServiceConnector);

        doThrow(new FlowStoreServiceConnectorUnexpectedStatusCodeException("msg", errorCodeToReturn)).when(flowStoreServiceConnector).deleteFlowComponent(eq(ID), (eq(1L)));
        try {
            flowStoreProxy.deleteFlowComponent(ID, 1);
            fail("No " + expectedErrorName + " error was thrown by deleteFlowComponent()");
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


    // Flow Binders tests

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

    /*
     * Test updateFlowBinder
     */

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
        FlowModel flowModel = new FlowModelBuilder().setId(id).setVersion(version).build();
        SinkModel sinkModel = new SinkModelBuilder().setId(id).setVersion(version).build();
        SubmitterModel submitterModel = new SubmitterModelBuilder().setId(id).setVersion(version).build();
        return new FlowBinderModelBuilder().setId(1).setVersion(1).setFlowModel(flowModel).setSubmitterModels(Collections.singletonList(submitterModel)).setSinkModel(sinkModel).build();
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
     * Test deleteFlowBinder
     */

    @Test
    public void deleteFlowBinder_remoteServiceReturnsHttpStatusNoContent() throws Exception {
        final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl(flowStoreServiceConnector);

        try {
            flowStoreProxy.deleteFlowBinder(ID, 1L);
        } catch (ProxyException e) {
            fail("Unexpected error when calling: deleteFlowBinder()");
        }
    }

    @Test
    public void deleteFlowBinder_remoteServiceReturnsHttpStatusNotFound_throws() throws Exception {
        deleteFlowBinder_genericTestImplForHttpErrors(404, ProxyError.ENTITY_NOT_FOUND, "ENTITY_NOT_FOUND");
    }

    @Test
    public void deleteFlowBinder_remoteServiceReturnsHttpStatusConflict_throws() throws Exception {
        deleteFlowBinder_genericTestImplForHttpErrors(409, ProxyError.CONFLICT_ERROR, "CONFLICT_ERROR");
    }

    @Test
    public void deleteFlowBinder_remoteServiceReturnsHttpStatusInternalServerError_throws() throws Exception {
        deleteFlowBinder_genericTestImplForHttpErrors(500, ProxyError.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR");
    }

    private void deleteFlowBinder_genericTestImplForHttpErrors(int errorCodeToReturn, ProxyError expectedError, String expectedErrorName) throws Exception {
        final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl(flowStoreServiceConnector);

        doThrow(new FlowStoreServiceConnectorUnexpectedStatusCodeException("msg", errorCodeToReturn)).when(flowStoreServiceConnector).deleteFlowBinder(eq(ID), (eq(1L)));
        try {
            flowStoreProxy.deleteFlowBinder(ID, 1);
            fail("No " + expectedErrorName + " error was thrown by deleteFlowBinder()");
        } catch (ProxyException e) {
            assertThat(e.getErrorCode(), is(expectedError));
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
        assertThat(model.getRecordSplitter(), is(defaultFlowBinderContent.getRecordSplitter().name()));
        assertThat(model.getFlowModel().getId(), is(defaultFlow.getId()));
        assertThat(model.getFlowModel().getFlowName(), is(defaultFlow.getContent().getName()));
        assertThat(model.getSubmitterModels().size(), is(1));
        assertThat(model.getSubmitterModels().get(0).getId(), is(DEFAULT_SUBMITTER_ID));
        assertThat(model.getSubmitterModels().get(0).getName(), is(defaultSubmitter.getContent().getName()));
        assertThat(model.getSinkModel().getId(), is(DEFAULT_SINK_ID));
        assertThat(model.getSinkModel().getSinkName(), is(defaultSink.getContent().getName()));
    }


    // Submitters tests

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

    /*
     * Test deleteSubmitter
     */

    @Test
    public void deleteSubmitter_remoteServiceReturnsHttpStatusNoContent() throws Exception {
        final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl(flowStoreServiceConnector);

        try {
            flowStoreProxy.deleteSubmitter(ID, 1L);
        } catch (ProxyException e) {
            fail("Unexpected error when calling: deleteSubmitter()");
        }
    }

    @Test
    public void deleteSubmitter_remoteServiceReturnsHttpStatusNotFound_throws() throws Exception {
        deleteSubmitter_genericTestImplForHttpErrors(404, ProxyError.ENTITY_NOT_FOUND, "ENTITY_NOT_FOUND");
    }

    @Test
    public void deleteSubmitter_remoteServiceReturnsHttpStatusConflict_throws() throws Exception {
        deleteSubmitter_genericTestImplForHttpErrors(409, ProxyError.CONFLICT_ERROR, "CONFLICT_ERROR");
    }

    @Test
    public void deleteSubmitter_remoteServiceReturnsHttpStatusInternalServerError_throws() throws Exception {
        deleteSubmitter_genericTestImplForHttpErrors(500, ProxyError.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR");
    }

    private void deleteSubmitter_genericTestImplForHttpErrors(int errorCodeToReturn, ProxyError expectedError, String expectedErrorName) throws Exception {
        final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl(flowStoreServiceConnector);

        doThrow(new FlowStoreServiceConnectorUnexpectedStatusCodeException("msg", errorCodeToReturn)).when(flowStoreServiceConnector).deleteSubmitter(eq(ID), (eq(1L)));
        try {
            flowStoreProxy.deleteSubmitter(ID, 1);
            fail("No " + expectedErrorName + " error was thrown by deleteSubmitter()");
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


    // Sinks tests

    /*
     * Test createSinks
     */

    @Test
    public void createSink_remoteServiceReturnsHttpStatusCreated_returnsSinkModelEntity() throws Exception {
        final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl(flowStoreServiceConnector);
        final Sink sink = new SinkBuilder().build();
        when(flowStoreServiceConnector.createSink(any(SinkContent.class))).thenReturn(sink);

        try {
            final SinkModel createdModel = flowStoreProxy.createSink(new SinkModelBuilder().setId(0).setVersion(0).build());
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
        SinkModel model = new SinkModelBuilder().setId(0).setVersion(0).setName("").build();
        createSink_testForProxyError(model, illegalArgumentException, ProxyError.MODEL_MAPPER_INVALID_FIELD_VALUE, "MODEL_MAPPER_INVALID_FIELD_VALUE");
    }

    private void createSink_genericTestImplForHttpErrors(int errorCodeToReturn, ProxyError expectedError, String expectedErrorName) throws Exception {
        final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl(flowStoreServiceConnector);

        when(flowStoreServiceConnector.createSink(any(SinkContent.class)))
                .thenThrow(new FlowStoreServiceConnectorUnexpectedStatusCodeException("DIED", errorCodeToReturn));
        try {
            flowStoreProxy.createSink(new SinkModelBuilder().setId(0).setVersion(0).build());
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
     * Test updateSink
     */

    @Test
    public void updateSink_remoteServiceReturnsHttpStatusOk_returnsSinkModelEntity() throws Exception {
        final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl(flowStoreServiceConnector);
        final Sink sink = new SinkBuilder().setId(ID).setVersion(1).build();
        SinkModel model = new SinkModelBuilder().setId(sink.getId()).setVersion(sink.getVersion()).build();

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
        SinkModel model = new SinkModelBuilder().setResource("").build();
        updateSink_testForProxyError(model, illegalArgumentException, ProxyError.MODEL_MAPPER_INVALID_FIELD_VALUE, "MODEL_MAPPER_INVALID_FIELD_VALUE");
    }

    private void updateSink_genericTestImplForHttpErrors(int errorCodeToReturn, ProxyError expectedError, String expectedErrorName) throws Exception {
        final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl(flowStoreServiceConnector);
        final Sink sink = new SinkBuilder().setId(ID).setVersion(1).build();
        SinkModel model = new SinkModelBuilder().setId(sink.getId()).setVersion(sink.getVersion()).build();

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
     * Test deleteSink
     */

    @Test
    public void deleteSink_remoteServiceReturnsHttpStatusNoContent() throws Exception {
        final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl(flowStoreServiceConnector);

        try {
            flowStoreProxy.deleteSink(ID, 1L);
        } catch (ProxyException e) {
            fail("Unexpected error when calling: deleteSink()");
        }
    }

    @Test
    public void deleteSink_remoteServiceReturnsHttpStatusNotFound_throws() throws Exception {
        deleteSink_genericTestImplForHttpErrors(404, ProxyError.ENTITY_NOT_FOUND, "ENTITY_NOT_FOUND");
    }

    @Test
    public void deleteSink_remoteServiceReturnsHttpStatusConflict_throws() throws Exception {
        deleteSink_genericTestImplForHttpErrors(409, ProxyError.CONFLICT_ERROR, "CONFLICT_ERROR");
    }

    @Test
    public void deleteSink_remoteServiceReturnsHttpStatusInternalServerError_throws() throws Exception {
        deleteSink_genericTestImplForHttpErrors(500, ProxyError.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR");
    }

    private void deleteSink_genericTestImplForHttpErrors(int errorCodeToReturn, ProxyError expectedError, String expectedErrorName) throws Exception {
        final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl(flowStoreServiceConnector);

        doThrow(new FlowStoreServiceConnectorUnexpectedStatusCodeException("msg", errorCodeToReturn)).when(flowStoreServiceConnector).deleteSink(eq(ID), (eq(1L)));
        try {
            flowStoreProxy.deleteSink(ID, 1);
            fail("No " + expectedErrorName + " error was thrown by deleteSink()");
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


    // Harvesters tests

    /*
     * Test createHarvesterConfig
     */

    @Test(expected = ProxyException.class)
    public void createHarvesterConfig_throwExceptionOnCorrectSubtype_exceptionIsThrown() throws Exception {
        final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl(flowStoreServiceConnector);
        // Now do emulate a TypeNotPresentException, which will be caught in the Proxy and a new ProxyException will be thrown
        when(flowStoreServiceConnector.createHarvesterConfig(any(RRHarvesterConfig.class), eq(RRHarvesterConfig.class))).thenThrow(new TypeNotPresentException("RRHarvesterConfig", new Throwable()));

        flowStoreProxy.createRRHarvesterConfig(new RRHarvesterConfig(1L, 1L, new RRHarvesterConfig.Content().withId("content-id")));
    }

    @Test
    public void createHarvesterConfig_remoteServiceReturnsHttpStatusCreated_returnsRRHarvesterConfigEntity() throws Exception {
        final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl(flowStoreServiceConnector);
        final RRHarvesterConfig config = new RRHarvesterConfig(123L, 234L, new RRHarvesterConfig.Content().withId("created-content-id"));
        final OLDRRHarvesterConfig oldConfig = new OLDRRHarvesterConfig(124L, 235L, new OLDRRHarvesterConfig.Content().withId("old-created-content-id"));
        when(flowStoreServiceConnector.createHarvesterConfig(any(RRHarvesterConfig.class), eq(RRHarvesterConfig.class))).thenReturn(config);
        when(flowStoreServiceConnector.createHarvesterConfig(any(RRHarvesterConfig.class), eq(OLDRRHarvesterConfig.class))).thenReturn(oldConfig);
        try {
            final RRHarvesterConfig createdConfig = flowStoreProxy.createRRHarvesterConfig(new RRHarvesterConfig(345L, 456L, new RRHarvesterConfig.Content().withId("content-id")));
            assertNotNull(createdConfig);
            assertThat(createdConfig.getContent().getId(), is("created-content-id"));
        } catch (ProxyException e) {
            fail("Unexpected error when calling: createHarvesterConfig()");
        }
    }


    @Test
    public void createHarvesterConfig_remoteServiceReturnsHttpStatusInternalServerError_throws() throws Exception {
        createHarvesterConfig_genericTestImplForHttpErrors(500, ProxyError.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR");
    }

    @Test
    public void createHarvesterConfig_remoteServiceReturnsHttpStatusNotAcceptable_throws() throws Exception {
        createHarvesterConfig_genericTestImplForHttpErrors(406, ProxyError.NOT_ACCEPTABLE, "NOT_ACCEPTABLE");
    }

    @Test
    public void createHarvesterConfig_throwsIllegalArgumentException() throws Exception {
        IllegalArgumentException illegalArgumentException = new IllegalArgumentException("DIED");
        final RRHarvesterConfig config = new RRHarvesterConfig(123L, 234L, new RRHarvesterConfig.Content().withId("created-content-id"));
        createHarvesterConfig_testForProxyError(config, illegalArgumentException, ProxyError.MODEL_MAPPER_INVALID_FIELD_VALUE, "MODEL_MAPPER_INVALID_FIELD_VALUE");
    }

    private void createHarvesterConfig_genericTestImplForHttpErrors(int errorCodeToReturn, ProxyError expectedError, String expectedErrorName) throws Exception {
        final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl(flowStoreServiceConnector);
        when(flowStoreServiceConnector.createHarvesterConfig(any(RRHarvesterConfig.class), eq(RRHarvesterConfig.class)))
                .thenThrow(new FlowStoreServiceConnectorUnexpectedStatusCodeException("DIED", errorCodeToReturn));

        try {
            flowStoreProxy.createRRHarvesterConfig(new RRHarvesterConfig(345L, 456L, new RRHarvesterConfig.Content().withId("content-id")));
            fail("No " + expectedErrorName + " error was thrown by createHarvesterConfig()");
        } catch (ProxyException e) {
            assertThat(e.getErrorCode(), is(expectedError));
        }
    }

    private void createHarvesterConfig_testForProxyError(RRHarvesterConfig config, Exception exception, ProxyError expectedError, String expectedErrorName) throws Exception {
        final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl(flowStoreServiceConnector);
        when(flowStoreServiceConnector.createHarvesterConfig(any(RRHarvesterConfig.class), eq(RRHarvesterConfig.class))).thenThrow(exception);

        try {
            flowStoreProxy.createRRHarvesterConfig(config);
            fail("No " + expectedErrorName + " error was thrown by createHarvesterConfig()");
        } catch (ProxyException e) {
            assertThat(e.getErrorCode(), is(expectedError));
        }
    }

    /*
     * Test updateHarvesterConfig
     */

    @Test(expected = ProxyException.class)
    public void updateHarvesterConfig_throwExceptionOnCorrectSubtype_exceptionIsThrown() throws Exception {
        final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl(flowStoreServiceConnector);
        // Now do emulate a TypeNotPresentException, which will be caught in the Proxy and a new ProxyException will be thrown
        when(flowStoreServiceConnector.updateHarvesterConfig(any(UshSolrHarvesterConfig.class))).thenThrow(new TypeNotPresentException("UshSolrHarvesterConfig", new Throwable()));

        flowStoreProxy.updateHarvesterConfig(new UshSolrHarvesterConfig(2L, 2L, new UshSolrHarvesterConfig.Content().withName("ush-name")));
    }

    @Test
    public void updateHarvesterConfig_remoteServiceReturnsHttpStatusOk_returnsRRHarvesterConfigEntity() throws Exception {
        final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl(flowStoreServiceConnector);
        final RRHarvesterConfig config = new RRHarvesterConfig(123L, 234L, new RRHarvesterConfig.Content().withId("created-content-id"));
        when(flowStoreServiceConnector.updateHarvesterConfig(any(RRHarvesterConfig.class))).thenReturn(config);

        try {
            final RRHarvesterConfig updatedConfig = (RRHarvesterConfig) flowStoreProxy.updateHarvesterConfig(new RRHarvesterConfig(1, 2, new RRHarvesterConfig.Content().withId("content-id")));
            assertNotNull(updatedConfig);
            assertThat(updatedConfig, is(config));
        } catch (ProxyException e) {
            fail("Unexpected error when calling: updateHarvesterConfig()");
        }
    }

    @Test
    public void updateHarvesterConfig_remoteServiceReturnsHttpStatusOkWithUshConfigAndValidUshProperties_returnsUshHarvesterConfigEntityUpdatesNullProperties() throws Exception {
        final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl(flowStoreServiceConnector);
        final UshSolrHarvesterConfig config = new UshSolrHarvesterConfig(123L, 234L, new UshSolrHarvesterConfig.Content().
                withName("updated-content-name").
                withUshHarvesterProperties(new UshHarvesterProperties().withId(567))
        );
        when(flowStoreServiceConnector.updateHarvesterConfig(any(UshSolrHarvesterConfig.class))).thenReturn(config);

        try {
            final UshSolrHarvesterConfig updatedConfig = (UshSolrHarvesterConfig) flowStoreProxy.updateHarvesterConfig(
                    new UshSolrHarvesterConfig(124L, 235L, new UshSolrHarvesterConfig.Content().
                            withName("content-name").
                            withUshHarvesterProperties(new UshHarvesterProperties().withId(568))
                    )
            );
            ArgumentCaptor<UshSolrHarvesterConfig> argument = ArgumentCaptor.forClass(UshSolrHarvesterConfig.class);
            verify(flowStoreServiceConnector).updateHarvesterConfig(argument.capture());
            assertThat(argument.getValue().getContent().getName(), is("content-name"));
            assertThat(argument.getValue().getContent().getUshHarvesterProperties(), is(nullValue()));
            assertNotNull(updatedConfig);
            assertThat(updatedConfig.getContent().getName(), is("updated-content-name"));
            assertThat(updatedConfig, is(config));
        } catch (ProxyException e) {
            fail("Unexpected error when calling: updateHarvesterConfig()");
        }
    }

    @Test
    public void updateHarvesterConfig_remoteServiceReturnsHttpStatusNotFound_throws() throws Exception {
        updateHarvesterConfig_genericTestImplForHttpErrors(404, ProxyError.ENTITY_NOT_FOUND, "ENTITY_NOT_FOUND");
    }

    @Test
    public void updateHarvesterConfig_remoteServiceReturnsHttpStatusNotAcceptable_throws() throws Exception {
        updateHarvesterConfig_genericTestImplForHttpErrors(406, ProxyError.NOT_ACCEPTABLE, "NOT_ACCEPTABLE");
    }

    @Test
    public void updateHarvesterConfig_remoteServiceReturnsHttpStatusConflict_throws() throws Exception {
        updateHarvesterConfig_genericTestImplForHttpErrors(409, ProxyError.CONFLICT_ERROR, "CONFLICT_ERROR");
    }

    @Test
    public void updateHarvesterConfig_remoteServiceReturnsHttpStatusInternalServerError_throws() throws Exception {
        updateHarvesterConfig_genericTestImplForHttpErrors(500, ProxyError.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR");
    }

    @Test
    public void updateHarvesterConfig_throwsIllegalArgumentException() throws Exception {
        IllegalArgumentException illegalArgumentException = new IllegalArgumentException("DIED");
        RRHarvesterConfig config = new RRHarvesterConfig(1, 2, new RRHarvesterConfig.Content().withId("content-id"));
        updateHarvesterConfig_testForProxyError(config, illegalArgumentException, ProxyError.MODEL_MAPPER_INVALID_FIELD_VALUE, "MODEL_MAPPER_INVALID_FIELD_VALUE");
    }

    private void updateHarvesterConfig_genericTestImplForHttpErrors(int errorCodeToReturn, ProxyError expectedError, String expectedErrorName) throws Exception {
        final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl(flowStoreServiceConnector);
        when(flowStoreServiceConnector.updateHarvesterConfig(any(RRHarvesterConfig.class)))
                .thenThrow(new FlowStoreServiceConnectorUnexpectedStatusCodeException("DIED", errorCodeToReturn));
        try {
            flowStoreProxy.updateHarvesterConfig(new RRHarvesterConfig(1, 2, new RRHarvesterConfig.Content().withId("content-id")));
            fail("No " + expectedErrorName + " error was thrown by updateHarvesterConfig()");
        } catch (ProxyException e) {
            assertThat(e.getErrorCode(), is(expectedError));
        }
    }

    private void updateHarvesterConfig_testForProxyError(RRHarvesterConfig config, Exception exception, ProxyError expectedError, String expectedErrorName) throws Exception {
        final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl(flowStoreServiceConnector);
        when(flowStoreServiceConnector.updateHarvesterConfig(any(RRHarvesterConfig.class))).thenThrow(exception);
        try {
            flowStoreProxy.updateHarvesterConfig(config);
            fail("No " + expectedErrorName + " error was thrown by updateHarvesterConfig()");
        } catch (ProxyException e) {
            assertThat(e.getErrorCode(), is(expectedError));
        }
    }


    /*
     * Test deleteHarvesterConfig
     */

    @Test
    public void deleteHarvesterConfig_remoteServiceReturnsHttpStatusNoContent() throws Exception {
        final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl(flowStoreServiceConnector);

        try {
            flowStoreProxy.deleteHarvesterConfig(ID, 1L);
        } catch (ProxyException e) {
            fail("Unexpected error when calling: deleteHarvesterConfig()");
        }
    }

    @Test
    public void deleteHarvesterConfig_remoteServiceReturnsHttpStatusNotFound_throws() throws Exception {
        deleteHarvesterConfig_genericTestImplForHttpErrors(404, ProxyError.ENTITY_NOT_FOUND, "ENTITY_NOT_FOUND");
    }

    @Test
    public void deleteHarvesterConfig_remoteServiceReturnsHttpStatusConflict_throws() throws Exception {
        deleteHarvesterConfig_genericTestImplForHttpErrors(409, ProxyError.CONFLICT_ERROR, "CONFLICT_ERROR");
    }

    @Test
    public void deleteHarvesterConfig_remoteServiceReturnsHttpStatusInternalServerError_throws() throws Exception {
        deleteHarvesterConfig_genericTestImplForHttpErrors(500, ProxyError.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR");
    }

    private void deleteHarvesterConfig_genericTestImplForHttpErrors(int errorCodeToReturn, ProxyError expectedError, String expectedErrorName) throws Exception {
        final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl(flowStoreServiceConnector);

        doThrow(new FlowStoreServiceConnectorUnexpectedStatusCodeException("msg", errorCodeToReturn)).when(flowStoreServiceConnector).deleteHarvesterConfig(eq(ID), (eq(1L)));
        try {
            flowStoreProxy.deleteHarvesterConfig(ID, 1);
            fail("No " + expectedErrorName + " error was thrown by deleteHarvesterConfig()");
        } catch (ProxyException e) {
            assertThat(e.getErrorCode(), is(expectedError));
        }
    }


  /*
     * Test findAllRRHarvesterConfigs
     */

    @Test
    public void findAllRRHarvesterConfigs_remoteServiceReturnsHttpStatusInternalServerError_throws() throws Exception {
        final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl(flowStoreServiceConnector);

        when(flowStoreServiceConnector.findHarvesterConfigsByType(OLDRRHarvesterConfig.class)).thenThrow(new FlowStoreServiceConnectorUnexpectedStatusCodeException("DIED", 500));
        try {
            flowStoreProxy.findAllRRHarvesterConfigs();
            fail("No INTERNAL_SERVER_ERROR was thrown by findAllHarvesterRrConfigs()");
        } catch (ProxyException e) {
            assertThat(e.getErrorCode(), is(ProxyError.INTERNAL_SERVER_ERROR));
        }
    }

    @Test
    public void findAllRRHarvesterConfigs_remoteServiceReturnsHttpStatusOk_returnsHarvesterRrConfigs() throws Exception {

        final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl(flowStoreServiceConnector);
        final List<RRHarvesterConfig> rrHarvesterConfigs = new ArrayList<>();
        rrHarvesterConfigs.add( new RRHarvesterConfig(1,1,
                new RRHarvesterConfig.Content().withId("Id-1")
        ) );

        final List<OLDRRHarvesterConfig> oldRrHarvesterConfigs = new ArrayList<>();
        oldRrHarvesterConfigs.add( new OLDRRHarvesterConfig(2,1,
                new OLDRRHarvesterConfig.Content().withId("Id-2")
        ) );

        when(flowStoreServiceConnector.findHarvesterConfigsByType(RRHarvesterConfig.class)).thenReturn(rrHarvesterConfigs);
        when(flowStoreServiceConnector.findHarvesterConfigsByType(OLDRRHarvesterConfig.class)).thenReturn(oldRrHarvesterConfigs);

        final List<RRHarvesterConfig> result = flowStoreProxy.findAllRRHarvesterConfigs();

        assertNotNull(result);
        assertThat(result.size(), is(2));
        for ( RRHarvesterConfig entry: result ) {
            switch(entry.getContent().getId()) {
                case "Id-1":
                case "Id-2":
                    break;
                default:
                    fail("RawRepoHarvesterConfig contains an unexpected entry");
            }
        }
    }


    /*
     * Test findAllUshSolrHarvesterConfigs
     */

    @Test
    public void findAllUshSolrHarvesterConfigs_remoteFlowStoreServiceReturnsHttpStatusInternalServerError_throws() throws Exception {
        final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl(flowStoreServiceConnector);
        when(flowStoreServiceConnector.findHarvesterConfigsByType(UshSolrHarvesterConfig.class)).thenThrow(new FlowStoreServiceConnectorUnexpectedStatusCodeException("DIED", 500));

        try {
            flowStoreProxy.findAllUshSolrHarvesterConfigs();
            fail("No INTERNAL_SERVER_ERROR was thrown by findAllHarvesterUshConfigs()");
        } catch (ProxyException e) {
            assertThat(e.getErrorCode(), is(ProxyError.INTERNAL_SERVER_ERROR));
        }
    }

    @Test
    public void findAllUshSolrHarvesterConfigs_remoteServiceReturnsHttpStatusOk_returnsHarvesterUshConfigs() throws Exception {
        final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl(flowStoreServiceConnector);

        final List<UshSolrHarvesterConfig> ushHarvesterConfigs = new ArrayList<>();
        ushHarvesterConfigs.add(
                new UshSolrHarvesterConfig(1, 1,
                        new UshSolrHarvesterConfig.Content().
                                withUshHarvesterJobId(10).
                                withUshHarvesterProperties(
                                        new UshHarvesterProperties().
                                                withId(10).
                                                withName("Ush Property Name")))
        );
        when(flowStoreServiceConnector.findHarvesterConfigsByType(UshSolrHarvesterConfig.class)).thenReturn(ushHarvesterConfigs);

        final List<UshSolrHarvesterConfig> result = flowStoreProxy.findAllUshSolrHarvesterConfigs();

        assertThat(result.size(), is(1));
        assertThat(result.get(0).getContent().getUshHarvesterJobId(), is(10));
        assertThat(result.get(0).getContent().getUshHarvesterProperties().getId(), is(10));
        assertThat(result.get(0).getContent().getUshHarvesterProperties().getName(), is("Ush Property Name"));
    }

    /*
     * Test getUshSolrHarvesterConfig
     */

    @Test
    public void getUshSolrHarvesterConfig_remoteServiceReturnsHttpStatusOk_returnsRRHarvesterConfig() throws Exception {
        final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl(flowStoreServiceConnector);
        final UshSolrHarvesterConfig harvesterConfig = new UshSolrHarvesterConfig(11L, 22L, new UshSolrHarvesterConfig.Content().withName("UshName"));
        when(flowStoreServiceConnector.getHarvesterConfig(11, UshSolrHarvesterConfig.class)).thenReturn(harvesterConfig);

        try {
            final UshSolrHarvesterConfig retrievedConfig = flowStoreProxy.getUshSolrHarvesterConfig(11);
            assertNotNull(retrievedConfig);
            assertThat(retrievedConfig.getId(), is(11L));
            assertThat(retrievedConfig.getContent().getName(), is("UshName"));
        } catch (ProxyException e) {
            fail("Unexpected error when calling: getUshSolrHarvesterConfig()");
        }
    }

    @Test
    public void getUshSolrHarvesterConfig_remoteServiceReturnsHttpStatusInternalServerError_throws() throws Exception {
        getUshSolrHarvesterConfig_genericTestImplForHttpErrors(500, ProxyError.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR");
    }

    @Test
    public void getUshSolrHarvesterConfig_remoteServiceReturnsHttpStatusNotFound_throws() throws Exception {
        getUshSolrHarvesterConfig_genericTestImplForHttpErrors(404, ProxyError.ENTITY_NOT_FOUND, "ENTITY_NOT_FOUND");
    }

    private void getUshSolrHarvesterConfig_genericTestImplForHttpErrors(int errorCodeToReturn, ProxyError expectedError, String expectedErrorName) throws Exception {
        final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl(flowStoreServiceConnector);
        when(flowStoreServiceConnector.getHarvesterConfig(6543, UshSolrHarvesterConfig.class)).thenThrow(new FlowStoreServiceConnectorUnexpectedStatusCodeException("DIED", errorCodeToReturn));

        try {
            flowStoreProxy.getUshSolrHarvesterConfig(6543);
            fail("No " + expectedErrorName + " error was thrown by getUshSolrHarvesterConfig()");
        } catch (ProxyException e) {
            assertThat(e.getErrorCode(), is(expectedError));
        }
    }


    /*
     * Test getRRHarvesterConfig
     */

    @Test
    public void getRRHarvesterConfig_remoteServiceReturnsHttpStatusOk_returnsRRHarvesterConfig() throws Exception {
        final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl(flowStoreServiceConnector);
        final RRHarvesterConfig harvesterConfig = new RRHarvesterConfig(11L, 22L, new RRHarvesterConfig.Content().withId("54321"));
        when(flowStoreServiceConnector.getHarvesterConfig(11, RRHarvesterConfig.class)).thenReturn(harvesterConfig);

        try {
            final RRHarvesterConfig retrievedConfig = flowStoreProxy.getRRHarvesterConfig(11);
            assertNotNull(retrievedConfig);
            assertThat(retrievedConfig.getId(), is(11L));
            assertThat(retrievedConfig.getContent().getId(), is("54321"));
        } catch (ProxyException e) {
            fail("Unexpected error when calling: getRRHarvesterConfig()");
        }
    }

    @Test
    public void getRRHarvesterConfig_remoteServiceReturnsHttpStatusInternalServerError_throws() throws Exception {
        getRRHarvesterConfig_genericTestImplForHttpErrors(500, ProxyError.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR");
    }

    @Test
    public void getRRHarvesterConfig_remoteServiceReturnsHttpStatusNotFound_throws() throws Exception {
        getRRHarvesterConfig_genericTestImplForHttpErrors(404, ProxyError.ENTITY_NOT_FOUND, "ENTITY_NOT_FOUND");
    }

    private void getRRHarvesterConfig_genericTestImplForHttpErrors(int errorCodeToReturn, ProxyError expectedError, String expectedErrorName) throws Exception {
        final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl(flowStoreServiceConnector);
        when(flowStoreServiceConnector.getHarvesterConfig(6543, RRHarvesterConfig.class)).thenThrow(new FlowStoreServiceConnectorUnexpectedStatusCodeException("DIED", errorCodeToReturn));

        try {
            flowStoreProxy.getRRHarvesterConfig(6543);
            fail("No " + expectedErrorName + " error was thrown by getRRHarvesterConfig()");
        } catch (ProxyException e) {
            assertThat(e.getErrorCode(), is(expectedError));
        }
    }



    // Gatekeeper destination tests

    /*
     * Test createGatekeeperDestination
     */

    @Test
    public void createGatekeeperDestination_remoteServiceReturnsHttpStatusCreated_returnsCreatedGatekeeperDestination() throws Exception {
        final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl(flowStoreServiceConnector);
        final GatekeeperDestination destination = new GatekeeperDestinationBuilder().build();
        when(flowStoreServiceConnector.createGatekeeperDestination(any(GatekeeperDestination.class))).thenReturn(destination);
        try {
            final GatekeeperDestination createdDestination = flowStoreProxy.createGatekeeperDestination(destination);
            assertNotNull(createdDestination);
        } catch (ProxyException e) {
            fail("Unexpected error when calling: createGatekeeperDestination()");
        }
    }

    @Test
    public void createGatekeeperDestination_remoteServiceReturnsHttpStatusInternalServerError_throws() throws Exception {
        createGatekeeperDestination_genericTestImplForHttpErrors(500, ProxyError.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR");
    }

    @Test
    public void createGatekeeperDestination_remoteServiceReturnsHttpStatusNotAcceptable_throws() throws Exception {
        createGatekeeperDestination_genericTestImplForHttpErrors(406, ProxyError.NOT_ACCEPTABLE, "NOT_ACCEPTABLE");
    }

    @Test
    public void createGatekeeperDestination_throwsIllegalArgumentException() throws Exception {
        IllegalArgumentException illegalArgumentException = new IllegalArgumentException("DIED");
        createGatekeeperDestination_testForProxyError(new GatekeeperDestinationBuilder().build(),
                illegalArgumentException, ProxyError.MODEL_MAPPER_INVALID_FIELD_VALUE, "MODEL_MAPPER_INVALID_FIELD_VALUE");
    }

    private void createGatekeeperDestination_genericTestImplForHttpErrors(int errorCodeToReturn, ProxyError expectedError, String expectedErrorName) throws Exception {
        final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl(flowStoreServiceConnector);

        when(flowStoreServiceConnector.createGatekeeperDestination(any(GatekeeperDestination.class)))
                .thenThrow(new FlowStoreServiceConnectorUnexpectedStatusCodeException("DIED", errorCodeToReturn));
        try {
            flowStoreProxy.createGatekeeperDestination(new GatekeeperDestinationBuilder().build());
            fail("No " + expectedErrorName + " error was thrown by createGatekeeperDestination()");
        } catch (ProxyException e) {
            assertThat(e.getErrorCode(), is(expectedError));
        }
    }

    private void createGatekeeperDestination_testForProxyError(GatekeeperDestination destination, Exception exception, ProxyError expectedError, String expectedErrorName) throws Exception {
        final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl(flowStoreServiceConnector);

        when(flowStoreServiceConnector.createGatekeeperDestination(any(GatekeeperDestination.class)))
                .thenThrow(exception);
        try {
            flowStoreProxy.createGatekeeperDestination(destination);
            fail("No " + expectedErrorName + " error was thrown by createGatekeeperDestination()");
        } catch (ProxyException e) {
            assertThat(e.getErrorCode(), is(expectedError));
        }
    }

    /*
     * Test findAllGatekeeperDestinations
     */

    @Test
    public void findAllGatekeeperDestinations_remoteServiceReturnsHttpStatusInternalServerError_throws() throws Exception {
        final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl(flowStoreServiceConnector);
        when(flowStoreServiceConnector.findAllGatekeeperDestinations()).thenThrow(new FlowStoreServiceConnectorUnexpectedStatusCodeException("DIED", 500));
        try {
            flowStoreProxy.findAllGatekeeperDestinations();
            fail("No INTERNAL_SERVER_ERROR was thrown by findAllGatekeeperDestinations()");
        } catch (ProxyException e) {
            assertThat(e.getErrorCode(), is(ProxyError.INTERNAL_SERVER_ERROR));
        }
    }

    @Test
    public void findAllGatekeeperDestinations_remoteServiceReturnsHttpStatusOk_returnsListOfGatekeeperDestinations() throws Exception {

        final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl(flowStoreServiceConnector);
        final GatekeeperDestination gatekeeperDestination = new GatekeeperDestinationBuilder().build();

        when(flowStoreServiceConnector.findAllGatekeeperDestinations()).thenReturn(Collections.singletonList(gatekeeperDestination));
        try {
            final List<GatekeeperDestination> allGatekeepers = flowStoreProxy.findAllGatekeeperDestinations();
            assertNotNull(allGatekeepers);
            assertThat(allGatekeepers.size(), is(1));
            assertThat(allGatekeepers.get(0).getId(), is(gatekeeperDestination.getId()));
        } catch (ProxyException e) {
            fail("Unexpected error when calling: findAllGatekeeperDestinations()");
        }
    }


    /*
     * Test deleteGatekeeperDestination
     */

    @Test
    public void deleteGatekeeperDestination_remoteServiceReturnsHttpStatusNoContent() throws Exception {
        final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl(flowStoreServiceConnector);

        try {
            flowStoreProxy.deleteGatekeeperDestination(ID);
        } catch (ProxyException e) {
            fail("Unexpected error when calling: deleteGatekeeperDestination()");
        }
    }

    @Test
    public void deleteGatekeeperDestination_remoteServiceReturnsHttpStatusNotFound_throws() throws Exception {
        deleteGatekeeperDestination_genericTestImplForHttpErrors(404, ProxyError.ENTITY_NOT_FOUND, "ENTITY_NOT_FOUND");
    }

    @Test
    public void deleteGatekeeperDestination_remoteServiceReturnsHttpStatusConflict_throws() throws Exception {
        deleteGatekeeperDestination_genericTestImplForHttpErrors(409, ProxyError.CONFLICT_ERROR, "CONFLICT_ERROR");
    }

    @Test
    public void deleteGatekeeperDestination_remoteServiceReturnsHttpStatusInternalServerError_throws() throws Exception {
        deleteGatekeeperDestination_genericTestImplForHttpErrors(500, ProxyError.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR");
    }

    private void deleteGatekeeperDestination_genericTestImplForHttpErrors(int errorCodeToReturn, ProxyError expectedError, String expectedErrorName) throws Exception {
        final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl(flowStoreServiceConnector);

        doThrow(new FlowStoreServiceConnectorUnexpectedStatusCodeException("msg", errorCodeToReturn)).when(flowStoreServiceConnector).deleteGatekeeperDestination(eq(ID));
        try {
            flowStoreProxy.deleteGatekeeperDestination(ID);
            fail("No " + expectedErrorName + " error was thrown by deleteGatekeeperDestination()");
        } catch (ProxyException e) {
            assertThat(e.getErrorCode(), is(expectedError));
        }
    }


    /*
     * Test updateGatekeeperDestination
     */

    @Test
    public void updateGatekeeperDestination_remoteServiceReturnsHttpStatusOk_returnsGatekeeperDestination() throws Exception {
        final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl(flowStoreServiceConnector);
        final GatekeeperDestination initialGatekeeperDestination = new GatekeeperDestinationBuilder().setId(ID).build();
        final GatekeeperDestination newGatekeeperDestination = new GatekeeperDestinationBuilder().setId(ID).build();
        when(flowStoreServiceConnector.updateGatekeeperDestination(initialGatekeeperDestination)).thenReturn(newGatekeeperDestination);
        try {
            final GatekeeperDestination updatedGatekeeperDestination = flowStoreProxy.updateGatekeeperDestination(newGatekeeperDestination);
            assertNotNull(updatedGatekeeperDestination);
            assertThat(updatedGatekeeperDestination, is(newGatekeeperDestination));
        } catch (ProxyException e) {
            fail("Unexpected error when calling: updatedGatekeeperDestination()");
        }
    }

    @Test
    public void updateGatekeeperDestination_remoteServiceReturnsHttpStatusNotFound_throws() throws Exception {
        updateGatekeeperDestination_genericTestImplForHttpErrors(404, ProxyError.ENTITY_NOT_FOUND, "ENTITY_NOT_FOUND");
    }

    @Test
    public void updateGatekeeperDestination_remoteServiceReturnsHttpStatusNotAcceptable_throws() throws Exception {
        updateGatekeeperDestination_genericTestImplForHttpErrors(406, ProxyError.NOT_ACCEPTABLE, "NOT_ACCEPTABLE");
    }

    @Test
    public void updateGatekeeperDestination_remoteServiceReturnsHttpStatusConflict_throws() throws Exception {
        updateGatekeeperDestination_genericTestImplForHttpErrors(409, ProxyError.CONFLICT_ERROR, "CONFLICT_ERROR");
    }

    @Test
    public void updateGatekeeperDestination_remoteServiceReturnsHttpStatusInternalServerError_throws() throws Exception {
        updateGatekeeperDestination_genericTestImplForHttpErrors(500, ProxyError.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR");
    }

    @Test
    public void updateGatekeeperDestination_throwsIllegalArgumentException() throws Exception {
        IllegalArgumentException illegalArgumentException = new IllegalArgumentException("DIED");
        final GatekeeperDestination gatekeeperDestination = new GatekeeperDestinationBuilder().setId(ID).build();
        updateGatekeeperDestination_testForProxyError(gatekeeperDestination, illegalArgumentException, ProxyError.MODEL_MAPPER_INVALID_FIELD_VALUE, "MODEL_MAPPER_INVALID_FIELD_VALUE");
    }

    private void updateGatekeeperDestination_genericTestImplForHttpErrors(int errorCodeToReturn, ProxyError expectedError, String expectedErrorName) throws Exception {
        final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl(flowStoreServiceConnector);
        final GatekeeperDestination gatekeeperDestination = new GatekeeperDestinationBuilder().setId(ID).build();

        when(flowStoreServiceConnector.updateGatekeeperDestination(gatekeeperDestination))
                .thenThrow(new FlowStoreServiceConnectorUnexpectedStatusCodeException("DIED", errorCodeToReturn));
        try {
            flowStoreProxy.updateGatekeeperDestination(gatekeeperDestination);
            fail("No " + expectedErrorName + " error was thrown by updateGatekeeperDestination()");
        } catch (ProxyException e) {
            assertThat(e.getErrorCode(), is(expectedError));
        }
    }

    private void updateGatekeeperDestination_testForProxyError(GatekeeperDestination gatekeeperDestination, Exception exception, ProxyError expectedError, String expectedErrorName) throws Exception {
        final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl(flowStoreServiceConnector);

        when(flowStoreServiceConnector.updateGatekeeperDestination(gatekeeperDestination)).thenThrow(exception);

        try {
            flowStoreProxy.updateGatekeeperDestination(gatekeeperDestination);
            fail("No " + expectedErrorName + " error was thrown by updateGatekeeperDestination()");
        } catch (ProxyException e) {
            assertThat(e.getErrorCode(), is(expectedError));
        }
    }



    // Private methods

    private JavaScriptProjectFetcher.fetchRequiredJavaScriptResult getDefaultJavaScripts() {
        List<JavaScript> javaScripts = new ArrayList<>(2);
        javaScripts.add(new JavaScript("javascript1", "javaScriptName1"));
        javaScripts.add(new JavaScript("javascript2", "javaScriptName2"));
        return new JavaScriptProjectFetcher.fetchRequiredJavaScriptResult(javaScripts, null);
    }
}
