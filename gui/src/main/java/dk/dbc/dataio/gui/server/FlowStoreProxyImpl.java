package dk.dbc.dataio.gui.server;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.json.JSONConfiguration;
import dk.dbc.dataio.gui.client.model.FlowData;
import dk.dbc.dataio.gui.client.proxy.FlowStoreProxy;
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
        ClientConfig clientConfig = new DefaultClientConfig();
        clientConfig.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);

        // force client to use Jackson JAX-RS provider (one in org.codehaus.jackson.jaxrs)
        clientConfig.getClasses().add(JacksonJsonProvider.class);

        Client httpClient = Client.create(clientConfig);

        webResource = httpClient.resource(serviceEndpoint);
    }

    @Override
    public void createFlow(FlowData flowData) throws NullPointerException, IllegalStateException {
        ClientResponse response = webResource.path("flows").type(MediaType.APPLICATION_JSON).post(ClientResponse.class, flowData);
        if (response.getClientResponseStatus() == ClientResponse.Status.BAD_REQUEST) {
            throw new IllegalStateException(response.getEntity(String.class));
        }
    }
}
