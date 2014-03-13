package dk.dbc.dataio.gui.server;

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
import dk.dbc.dataio.gui.client.exceptions.ProxyError;
import dk.dbc.dataio.gui.client.exceptions.ProxyException;
import dk.dbc.dataio.gui.client.proxies.FlowStoreProxy;
import org.glassfish.jersey.client.ClientConfig;

import javax.servlet.ServletException;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import java.util.List;

public class FlowStoreProxyImpl implements FlowStoreProxy {
    Client client = null;

    public FlowStoreProxyImpl() {
        final ClientConfig clientConfig = new ClientConfig().register(new Jackson2xFeature());
        client = HttpClient.newClient(clientConfig);
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
    public void createFlowComponent(FlowComponentContent flowComponentContent) throws NullPointerException, ProxyException {
        final Response response;
        try {
            response = HttpClient.doPostWithJson(client, flowComponentContent,
                    ServletUtil.getFlowStoreServiceEndpoint(), FlowStoreServiceConstants.FLOW_COMPONENTS);
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
    public void createSubmitter(SubmitterContent submitterContent) throws NullPointerException, IllegalStateException, ProxyException {
        final Response response;
        try {
            response = HttpClient.doPostWithJson(client, submitterContent,
                    ServletUtil.getFlowStoreServiceEndpoint(), FlowStoreServiceConstants.SUBMITTERS);
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
    public void createSink(SinkContent sinkContent) throws NullPointerException, ProxyException {
        final Response response;
        try {
            response = HttpClient.doPostWithJson(client, sinkContent,
                    ServletUtil.getFlowStoreServiceEndpoint(), FlowStoreServiceConstants.SINKS);
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
    public List<FlowComponent> findAllComponents() throws ProxyException {
        final Response response;
        final List<FlowComponent> result;
        try {
            response = HttpClient.doGet(client, ServletUtil.getFlowStoreServiceEndpoint(), FlowStoreServiceConstants.FLOW_COMPONENTS);
        } catch (ServletException e) {
            throw new ProxyException(ProxyError.SERVICE_NOT_FOUND, e);
        }
        try {
            assertStatusCode(response, Response.Status.OK);
            result = response.readEntity(new GenericType<List<FlowComponent>>() { });
        } finally {
            response.close();
        }
        return result;
    }

    @Override
    public List<Submitter> findAllSubmitters() throws ProxyException {
        final Response response;
        final List<Submitter> result;
        try {
            response = HttpClient.doGet(client, ServletUtil.getFlowStoreServiceEndpoint(), FlowStoreServiceConstants.SUBMITTERS);
        } catch (ServletException e) {
            throw new ProxyException(ProxyError.SERVICE_NOT_FOUND, e);
        }
        try {
            assertStatusCode(response, Response.Status.OK);
            result = response.readEntity(new GenericType<List<Submitter>>() { });
        } finally {
            response.close();
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
        final Response response;
        final List<Sink> result;
        try {
            response = HttpClient.doGet(client, ServletUtil.getFlowStoreServiceEndpoint(), FlowStoreServiceConstants.SINKS);
        } catch (ServletException e) {
            throw new ProxyException(ProxyError.SERVICE_NOT_FOUND, e);
        }
        try {
            assertStatusCode(response, Response.Status.OK);
            result = response.readEntity(new GenericType<List<Sink>>() { });
        } finally {
            response.close();
        }
        return result;
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
                default:
                    errorCode = ProxyError.INTERNAL_SERVER_ERROR;
            }
            throw new ProxyException(errorCode, response.readEntity(String.class));
        }
    }

}
