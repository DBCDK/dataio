package dk.dbc.dataio.gui.server;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.GenericType;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.json.JSONConfiguration;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import dk.dbc.dataio.engine.Flow;
import dk.dbc.dataio.engine.FlowComponent;
import dk.dbc.dataio.engine.FlowContent;
import dk.dbc.dataio.gui.client.proxies.FlowStoreProxy;
import org.codehaus.jackson.jaxrs.JacksonJsonProvider;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FlowStoreProxyImpl implements FlowStoreProxy {
    private static final String FLOWS_ENTRY_POINT = "flows";
    private static final String FLOWS_COMPONENTS_ENTRY_POINT = "components";

    private final WebResource webResource;
    
    private static final Logger log = LoggerFactory.getLogger(FlowStoreProxyImpl.class);

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
        if (response.getClientResponseStatus() == ClientResponse.Status.BAD_REQUEST) {
            throw new IllegalStateException(response.getEntity(String.class));
        }
    }

    @Override
    public void addFlowComponentToFlow(Flow flow, FlowComponent flowComponent) {
        final MultivaluedMap<String, String> formData = new MultivaluedMapImpl();
        formData.add("id", Long.toString(flowComponent.getId()));
        formData.add("version", Long.toString(flowComponent.getVersion()));
        final ClientResponse response = webResource.path(FLOWS_ENTRY_POINT)
                .path(Long.toString(flow.getId())).path(Long.toString(flow.getVersion())).path(FLOWS_COMPONENTS_ENTRY_POINT)
                .type(MediaType.APPLICATION_FORM_URLENCODED).post(ClientResponse.class, formData);
        if (response.getClientResponseStatus() != ClientResponse.Status.CREATED) {
            throw new IllegalStateException(response.getEntity(String.class));
        }
    }

    @Override
    public List<FlowComponent> findAllComponents() {
        final ClientResponse response = webResource.path(FLOWS_COMPONENTS_ENTRY_POINT).get(ClientResponse.class);
        log.info("Got response...");
        if (response.getClientResponseStatus() != ClientResponse.Status.OK) {
            throw new IllegalStateException(response.getEntity(String.class));
        }
//        log.info("Response: " + response.getEntity(String.class));
        log.info("hertil");
        log.info("Mime: " + response.getType());
        List<FlowComponent> listen = response.getEntity(new GenericType<List<FlowComponent>>() { });
        log.info("Response: " + listen);
        return listen;
        //        return response.getEntity(new GenericType<List<FlowComponent>>() { });
    }
}
