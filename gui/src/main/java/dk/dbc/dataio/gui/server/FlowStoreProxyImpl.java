package dk.dbc.dataio.gui.server;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.json.JSONConfiguration;
import dk.dbc.dataio.engine.FlowContent;
import dk.dbc.dataio.gui.client.proxies.FlowStoreProxy;
import org.codehaus.jackson.jaxrs.JacksonJsonProvider;

import javax.ws.rs.core.MediaType;

public class FlowStoreProxyImpl implements FlowStoreProxy {

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
        final ClientResponse response = webResource.path("flows").type(MediaType.APPLICATION_JSON).post(ClientResponse.class, flowContent);
        if (response.getClientResponseStatus() == ClientResponse.Status.BAD_REQUEST) {
            throw new IllegalStateException(response.getEntity(String.class));
        }
    }
}
