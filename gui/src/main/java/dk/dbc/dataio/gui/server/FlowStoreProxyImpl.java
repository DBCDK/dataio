package dk.dbc.dataio.gui.server;

import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.FlowBinderContent;
import dk.dbc.dataio.commons.types.FlowComponent;
import dk.dbc.dataio.commons.types.FlowComponentContent;
import dk.dbc.dataio.commons.types.FlowContent;
import dk.dbc.dataio.commons.types.FlowStoreServiceEntryPoint;
import dk.dbc.dataio.commons.types.Submitter;
import dk.dbc.dataio.commons.types.SubmitterContent;
import dk.dbc.dataio.commons.utils.httpclient.HttpClient;
import dk.dbc.dataio.gui.client.exceptions.FlowStoreProxyError;
import dk.dbc.dataio.gui.client.exceptions.FlowStoreProxyException;
import dk.dbc.dataio.gui.client.proxies.FlowStoreProxy;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import java.util.List;

public class FlowStoreProxyImpl implements FlowStoreProxy {

    private static final Logger log = LoggerFactory.getLogger(FlowStoreProxyImpl.class);
    Client client = null;

    public FlowStoreProxyImpl() {
        final ClientConfig clientConfig = new ClientConfig().register(new JacksonFeature());
        client = HttpClient.newClient(clientConfig);
    }

    @Override
    public void createFlow(FlowContent flowContent) throws NullPointerException, FlowStoreProxyException {
        final Response response;
        try {
            response = HttpClient.doPostWithJson(client, flowContent,
                    ServletUtil.getFlowStoreServiceEndpoint(), FlowStoreServiceEntryPoint.FLOWS);
        } catch (ServletException e) {
            throw new FlowStoreProxyException(FlowStoreProxyError.SERVICE_NOT_FOUND, e);
        }
        try {
            assertStatusCode(response, Response.Status.CREATED);
        } finally {
            response.close();
        }
    }

    @Override
    public void createFlowComponent(FlowComponentContent flowComponentContent) throws NullPointerException, FlowStoreProxyException {
        final Response response;
        try {
            response = HttpClient.doPostWithJson(client, flowComponentContent,
                    ServletUtil.getFlowStoreServiceEndpoint(), FlowStoreServiceEntryPoint.FLOW_COMPONENTS);
        } catch (ServletException e) {
            throw new FlowStoreProxyException(FlowStoreProxyError.SERVICE_NOT_FOUND, e);
        }
        try {
            assertStatusCode(response, Response.Status.CREATED);
        } finally {
            response.close();
        }
    }

    @Override
    public void createSubmitter(SubmitterContent submitterContent) throws NullPointerException, IllegalStateException, FlowStoreProxyException {
        final Response response;
        try {
            response = HttpClient.doPostWithJson(client, submitterContent,
                    ServletUtil.getFlowStoreServiceEndpoint(), FlowStoreServiceEntryPoint.SUBMITTERS);
        } catch (ServletException e) {
            throw new FlowStoreProxyException(FlowStoreProxyError.SERVICE_NOT_FOUND, e);
        }
        try {
            assertStatusCode(response, Response.Status.CREATED);
        } finally {
            response.close();
        }
    }

    @Override
    public void createFlowBinder(FlowBinderContent flowBinderContent) throws NullPointerException, FlowStoreProxyException {
        final Response response;
        try {
            response = HttpClient.doPostWithJson(client, flowBinderContent,
                    ServletUtil.getFlowStoreServiceEndpoint(), FlowStoreServiceEntryPoint.FLOW_BINDERS);
        } catch (ServletException e) {
            throw new FlowStoreProxyException(FlowStoreProxyError.SERVICE_NOT_FOUND, e);
        }
        try {
            assertStatusCode(response, Response.Status.CREATED);
        } finally {
            response.close();
        }
    }

    @Override
    public List<FlowComponent> findAllComponents() throws FlowStoreProxyException {
        final Response response;
        final List<FlowComponent> result;
        try {
            response = HttpClient.doGet(client, ServletUtil.getFlowStoreServiceEndpoint(), FlowStoreServiceEntryPoint.FLOW_COMPONENTS);
        } catch (ServletException e) {
            throw new FlowStoreProxyException(FlowStoreProxyError.SERVICE_NOT_FOUND, e);
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
    public List<Submitter> findAllSubmitters() throws FlowStoreProxyException {
        final Response response;
        final List<Submitter> result;
        try {
            response = HttpClient.doGet(client, ServletUtil.getFlowStoreServiceEndpoint(), FlowStoreServiceEntryPoint.SUBMITTERS);
        } catch (ServletException e) {
            throw new FlowStoreProxyException(FlowStoreProxyError.SERVICE_NOT_FOUND, e);
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
    public List<Flow> findAllFlows() throws FlowStoreProxyException {
        final Response response;
        final List<Flow> result;
        try {
            response = HttpClient.doGet(client, ServletUtil.getFlowStoreServiceEndpoint(), FlowStoreServiceEntryPoint.FLOWS);
        } catch (ServletException e) {
            throw new FlowStoreProxyException(FlowStoreProxyError.SERVICE_NOT_FOUND, e);
        }
        try {
            assertStatusCode(response, Response.Status.OK);
            result = response.readEntity(new GenericType<List<Flow>>() { });
        } finally {
            response.close();
        }
        return result;
    }

    public void close() {
        HttpClient.closeClient(client);
    }

    private void assertStatusCode(Response response, Response.Status expectedStatus) throws FlowStoreProxyException {
        final Response.Status status = Response.Status.fromStatusCode(response.getStatus());
        if (status != expectedStatus) {
            final FlowStoreProxyError errorCode;
            switch (status) {
                case BAD_REQUEST: errorCode = FlowStoreProxyError.BAD_REQUEST;
                    break;
                case NOT_ACCEPTABLE: errorCode = FlowStoreProxyError.NOT_ACCEPTABLE;
                    break;
                case PRECONDITION_FAILED: errorCode = FlowStoreProxyError.ENTITY_NOT_FOUND;
                    break;
                default:
                    errorCode = FlowStoreProxyError.INTERNAL_SERVER_ERROR;
            }
            throw new FlowStoreProxyException(errorCode, response.readEntity(String.class));
        }
    }

}
