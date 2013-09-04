package dk.dbc.dataio.gui.server;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.GenericType;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.json.JSONConfiguration;
import dk.dbc.dataio.engine.Flow;
import dk.dbc.dataio.engine.FlowComponent;
import dk.dbc.dataio.engine.FlowComponentContent;
import dk.dbc.dataio.engine.FlowContent;
import dk.dbc.dataio.gui.client.proxies.FlowStoreProxy;
import org.codehaus.jackson.jaxrs.JacksonJsonProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
import java.util.List;

public class FlowStoreProxyImpl implements FlowStoreProxy {

    private static final Logger log = LoggerFactory.getLogger(FlowStoreProxyImpl.class);
    private static final String FLOWS_ENTRY_POINT = "flows";
    private static final String COMPONENTS_ENTRY_POINT = "components";
    private final WebResource webResource;

    /**
     * Class constructor
     *
     * @param serviceEndpoint base URL of flow store web-service to be proxied
     */
    public FlowStoreProxyImpl(String serviceEndpoint) {
        final ClientConfig clientConfig = new DefaultClientConfig();
        clientConfig.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);

        // force client to use Jackson JAX-RS provider (one in org.codehaus.jackson.jaxrs)
        clientConfig.getClasses().add(JacksonJsonProvider.class);

        final Client httpClient = Client.create(clientConfig);

        webResource = httpClient.resource(serviceEndpoint);
    }

    @Override
    public void createFlow(FlowContent flowContent) throws NullPointerException, IllegalStateException {
        final ClientResponse response = webResource.path(FLOWS_ENTRY_POINT).type(MediaType.APPLICATION_JSON).post(ClientResponse.class, flowContent);
        if (response.getClientResponseStatus() != ClientResponse.Status.CREATED) {
            throw new IllegalStateException(response.getEntity(String.class));
        }
    }

    @Override
    public void createFlowComponent(FlowComponentContent flowComponentContent) throws NullPointerException, IllegalStateException {
        final ClientResponse response = webResource.path(COMPONENTS_ENTRY_POINT).type(MediaType.APPLICATION_JSON).post(ClientResponse.class, flowComponentContent);
        if (response.getClientResponseStatus() != ClientResponse.Status.CREATED) {
            throw new IllegalStateException(response.getEntity(String.class));
        }
    }

    @Override
    public List<FlowComponent> findAllComponents() {
        final ClientResponse response = webResource.path(COMPONENTS_ENTRY_POINT).get(ClientResponse.class);
        if (response.getClientResponseStatus() != ClientResponse.Status.OK) {
            throw new IllegalStateException(response.getEntity(String.class));
        }
        return response.getEntity(new GenericType<List<FlowComponent>>() { });
    }

    @Override
    public List<Flow> findAllFlows() throws Exception {
        log.info("Find All Flows");
        try {
            final ClientResponse response = webResource.path(FLOWS_ENTRY_POINT).get(ClientResponse.class);
            if (response.getClientResponseStatus() != ClientResponse.Status.OK) {
                throw new IllegalStateException(response.getEntity(String.class));
            }
            return response.getEntity(new GenericType<List<Flow>>() {
            });
        } catch (Exception ex) {
            log.error("Exception caught while retrieving all flows", ex);
            throw ex;
        }
    }
}
