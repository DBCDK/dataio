package dk.dbc.dataio.cli.command;

import dk.dbc.dataio.cli.options.Options;
import dk.dbc.dataio.urlresolver.service.connector.UrlResolverServiceConnector;
import dk.dbc.dataio.urlresolver.service.connector.UrlResolverServiceConnectorException;
import dk.dbc.httpclient.HttpClient;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jackson.JacksonFeature;

import javax.ws.rs.client.Client;
import java.util.Map;

public abstract class Command<T extends Options> {

    T options;

    Command(T options) {
        this.options = options;
    }

    public abstract void execute() throws Exception;

    protected Map<String, String> getEndpoints() throws UrlResolverServiceConnectorException {
        final Client client = HttpClient.newClient(new ClientConfig()
                .register(new JacksonFeature()));
        final UrlResolverServiceConnector urlResolverServiceConnector = new UrlResolverServiceConnector(client, options.guiUrl);
        return urlResolverServiceConnector.getUrls();
    }
}
