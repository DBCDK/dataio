package dk.dbc.dataio.gui.server;

import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorUnexpectedStatusCodeException;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.FlowBinder;
import dk.dbc.dataio.commons.types.FlowBinderContent;
import dk.dbc.dataio.commons.types.FlowComponent;
import dk.dbc.dataio.commons.types.FlowComponentContent;
import dk.dbc.dataio.commons.types.FlowContent;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.SinkContent;
import dk.dbc.dataio.commons.types.Submitter;
import dk.dbc.dataio.commons.types.SubmitterContent;
import dk.dbc.dataio.commons.types.rest.FlowStoreServiceConstants;
import dk.dbc.dataio.commons.utils.httpclient.HttpClient;
import dk.dbc.dataio.commons.utils.jersey.jackson.Jackson2xFeature;
import dk.dbc.dataio.commons.utils.service.ServiceUtil;
import dk.dbc.dataio.gui.client.exceptions.ProxyError;
import dk.dbc.dataio.gui.client.exceptions.ProxyException;
import dk.dbc.dataio.gui.client.proxies.FlowStoreProxy;
import org.glassfish.jersey.client.ClientConfig;

import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import java.util.List;

public class FlowStoreProxyImpl implements FlowStoreProxy {
    final Client client;
    final String baseUrl;
    FlowStoreServiceConnector flowStoreServiceConnector;

    public FlowStoreProxyImpl() throws NamingException{
        final ClientConfig clientConfig = new ClientConfig().register(new Jackson2xFeature());
        client = HttpClient.newClient(clientConfig);
        baseUrl = ServiceUtil.getFlowStoreServiceEndpoint();
        flowStoreServiceConnector = new FlowStoreServiceConnector(client, baseUrl);
    }

    //This constructor is intended for test purpose only with reference to dependency injection.
    FlowStoreProxyImpl(FlowStoreServiceConnector flowStoreServiceConnector) throws NamingException{
        final ClientConfig clientConfig = new ClientConfig().register(new Jackson2xFeature());
        this.flowStoreServiceConnector = flowStoreServiceConnector;
        client = HttpClient.newClient(clientConfig);
        baseUrl = ServiceUtil.getFlowStoreServiceEndpoint();
    }

    @Override
    public void createFlow(FlowContent flowContent) throws NullPointerException, ProxyException {
        final Response response;
        try {
            response = HttpClient.doPostWithJson(client, flowContent,
                    ServletUtil.getFlowStoreServiceEndpoint(), FlowStoreServiceConstants.FLOWS);
        } catch (ServletException e) {
            throw new ProxyException(ProxyError.SERVICE_NOT_FOUND, e);
        }
        try {
            assertStatusCode(response, Response.Status.CREATED);
        } finally {
            response.close();
        }
    }

    @Override
    public FlowComponent createFlowComponent(FlowComponentContent flowComponentContent) throws NullPointerException, ProxyException {
        FlowComponent flowComponent;
        try {
            flowComponent = flowStoreServiceConnector.createFlowComponent(flowComponentContent);
        } catch (FlowStoreServiceConnectorUnexpectedStatusCodeException e){
            throw new ProxyException(translateToProxyError(e.getStatusCode()),e.getMessage());
        } catch (FlowStoreServiceConnectorException e) {
            throw new ProxyException(ProxyError.SERVICE_NOT_FOUND, e);
        }
        return flowComponent;
    }

    @Override
    public Submitter createSubmitter(SubmitterContent submitterContent) throws NullPointerException, ProxyException {
        Submitter submitter;
        try {
            submitter = flowStoreServiceConnector.createSubmitter(submitterContent);
        } catch (FlowStoreServiceConnectorUnexpectedStatusCodeException e){
            throw new ProxyException(translateToProxyError(e.getStatusCode()),e.getMessage());
        } catch (FlowStoreServiceConnectorException e) {
            throw new ProxyException(ProxyError.SERVICE_NOT_FOUND, e);
        }
        return submitter;
    }

    @Override
    public void createFlowBinder(FlowBinderContent flowBinderContent) throws NullPointerException, ProxyException {
        final Response response;
        try {
            response = HttpClient.doPostWithJson(client, flowBinderContent,
                    ServletUtil.getFlowStoreServiceEndpoint(), FlowStoreServiceConstants.FLOW_BINDERS);
        } catch (ServletException e) {
            throw new ProxyException(ProxyError.SERVICE_NOT_FOUND, e);
        }
        try {
            assertStatusCode(response, Response.Status.CREATED);
        } finally {
            response.close();
        }
    }

    @Override
    public Sink createSink(SinkContent sinkContent) throws NullPointerException, ProxyException {
        Sink sink;
        try {
            sink = flowStoreServiceConnector.createSink(sinkContent);
        } catch (FlowStoreServiceConnectorUnexpectedStatusCodeException e){
            throw new ProxyException(translateToProxyError(e.getStatusCode()),e.getMessage());
        } catch (FlowStoreServiceConnectorException e) {
            throw new ProxyException(ProxyError.SERVICE_NOT_FOUND, e);
        }
        return sink;
    }

    @Override
    public Sink updateSink(SinkContent sinkContent, Long id, Long version) throws NullPointerException, ProxyException {
        Sink sink;
        try {
            sink = flowStoreServiceConnector.updateSink(sinkContent, id, version);
        } catch (FlowStoreServiceConnectorUnexpectedStatusCodeException e){
            throw new ProxyException(translateToProxyError(e.getStatusCode()),e.getMessage());
        } catch (FlowStoreServiceConnectorException e) {
            throw new ProxyException(ProxyError.SERVICE_NOT_FOUND, e);
        }
        return sink;
    }

    @Override
    public List<FlowComponent> findAllFlowComponents() throws ProxyException {
        final List<FlowComponent> result;
        try {
            result = flowStoreServiceConnector.findAllFlowComponents();
        } catch (FlowStoreServiceConnectorUnexpectedStatusCodeException e){
            throw new ProxyException(translateToProxyError(e.getStatusCode()),e.getMessage());
        } catch (FlowStoreServiceConnectorException e) {
            throw new ProxyException(ProxyError.SERVICE_NOT_FOUND, e);
        }
        return result;
    }

    @Override
    public List<Submitter> findAllSubmitters() throws ProxyException {
        final List<Submitter> result;
        try {
            result = flowStoreServiceConnector.findAllSubmitters();
        } catch (FlowStoreServiceConnectorUnexpectedStatusCodeException e){
            throw new ProxyException(translateToProxyError(e.getStatusCode()),e.getMessage());
        } catch (FlowStoreServiceConnectorException e) {
            throw new ProxyException(ProxyError.SERVICE_NOT_FOUND, e);
        }
        return result;
    }

    @Override
    public List<Flow> findAllFlows() throws ProxyException {
        final Response response;
        final List<Flow> result;
        try {
            response = HttpClient.doGet(client, ServletUtil.getFlowStoreServiceEndpoint(), FlowStoreServiceConstants.FLOWS);
        } catch (ServletException e) {
            throw new ProxyException(ProxyError.SERVICE_NOT_FOUND, e);
        }
        try {
            assertStatusCode(response, Response.Status.OK);
            result = response.readEntity(new GenericType<List<Flow>>() { });
        } finally {
            response.close();
        }
        return result;
    }

    @Override
    public List<FlowBinder> findAllFlowBinders() throws ProxyException {
        final Response response;
        final List<FlowBinder> result;
        try {
            response = HttpClient.doGet(client, ServletUtil.getFlowStoreServiceEndpoint(), FlowStoreServiceConstants.FLOW_BINDERS);
        } catch (ServletException e) {
            throw new ProxyException(ProxyError.SERVICE_NOT_FOUND, e);
        }
        try {
            assertStatusCode(response, Response.Status.OK);
            result = response.readEntity(new GenericType<List<FlowBinder>>() { });
        } finally {
            response.close();
        }
        return result;
    }

    @Override
    public List<Sink> findAllSinks() throws ProxyException {
        final List<Sink> result;
        try {
            result = flowStoreServiceConnector.findAllSinks();
        } catch (FlowStoreServiceConnectorUnexpectedStatusCodeException e){
            throw new ProxyException(translateToProxyError(e.getStatusCode()),e.getMessage());
        } catch (FlowStoreServiceConnectorException e) {
            throw new ProxyException(ProxyError.SERVICE_NOT_FOUND, e);
        }
        return result;
    }

    @Override
    public Sink getSink(Long id) throws ProxyException {
        final Sink result;
        try {
            result = flowStoreServiceConnector.getSink(id);
        } catch (FlowStoreServiceConnectorUnexpectedStatusCodeException e){
            throw new ProxyException(translateToProxyError(e.getStatusCode()),e.getMessage());
        } catch (FlowStoreServiceConnectorException e) {
            throw new ProxyException(ProxyError.SERVICE_NOT_FOUND, e);
        }
        return result;
    }

    private ProxyError translateToProxyError(int statusCode)throws ProxyException {
        final Response.Status status = Response.Status.fromStatusCode(statusCode);

        final ProxyError errorCode;
        switch (status){
            case NOT_FOUND: errorCode = ProxyError.ENTITY_NOT_FOUND;
                break;
            case CONFLICT: errorCode = ProxyError.CONFLICT_ERROR;
                break;
            case NOT_ACCEPTABLE: errorCode = ProxyError.NOT_ACCEPTABLE;
                break;
            default:
                errorCode = ProxyError.INTERNAL_SERVER_ERROR;
        }
        return errorCode;
    }

    public void close() {
        HttpClient.closeClient(client);
    }

    private void assertStatusCode(Response response, Response.Status expectedStatus) throws ProxyException {
        final Response.Status status = Response.Status.fromStatusCode(response.getStatus());
        if (status != expectedStatus) {
            final ProxyError errorCode;
            switch (status) {
                case BAD_REQUEST: errorCode = ProxyError.BAD_REQUEST;
                    break;
                case NOT_ACCEPTABLE: errorCode = ProxyError.NOT_ACCEPTABLE;
                    break;
                case PRECONDITION_FAILED: errorCode = ProxyError.ENTITY_NOT_FOUND;
                    break;
                case CONFLICT: errorCode = ProxyError.CONFLICT_ERROR;
                    break;
                default:
                    errorCode = ProxyError.INTERNAL_SERVER_ERROR;
            }
            throw new ProxyException(errorCode, response.readEntity(String.class));
        }
    }

}
