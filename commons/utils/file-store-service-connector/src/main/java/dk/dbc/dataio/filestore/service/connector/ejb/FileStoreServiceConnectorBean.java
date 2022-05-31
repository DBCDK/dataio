package dk.dbc.dataio.filestore.service.connector.ejb;

import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnector;
import dk.dbc.httpclient.HttpClient;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.glassfish.jersey.apache.connector.ApacheClientProperties;
import org.glassfish.jersey.apache.connector.ApacheConnectorProvider;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.LocalBean;
import javax.ejb.Singleton;
import javax.ws.rs.client.Client;

// TODO: 05-07-19 replace EJB with @ApplicationScoped CDI producer

@Singleton
@LocalBean
public class FileStoreServiceConnectorBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(FileStoreServiceConnectorBean.class);
    private static final int MAX_HTTP_CONNECTIONS = 100;

    FileStoreServiceConnector fileStoreServiceConnector;

    @PostConstruct
    public void initializeConnector() {
        LOGGER.debug("Initializing connector");
        /* Since we need to be able to add data amounts exceeding the JVM
           final Client client = HttpClient.newClient(new ClientConfig()
           HEAP size and the default jersey client connector does not
           adhere to the CHUNKED_ENCODING_SIZE property we use the Apache
           HttpClient connector instead to avoid OutOfMemory errors.
         */
        final PoolingHttpClientConnectionManager poolingHttpClientConnectionManager = new PoolingHttpClientConnectionManager();
        poolingHttpClientConnectionManager.setMaxTotal(MAX_HTTP_CONNECTIONS);
        poolingHttpClientConnectionManager.setDefaultMaxPerRoute(MAX_HTTP_CONNECTIONS);

        final ClientConfig config = new ClientConfig();
        config.connectorProvider(new ApacheConnectorProvider());
        config.property(ClientProperties.CHUNKED_ENCODING_SIZE, 8 * 1024);
        config.property(ApacheClientProperties.CONNECTION_MANAGER, poolingHttpClientConnectionManager);
        config.register(new JacksonFeature());
        Client client = HttpClient.newClient(config);

        final String endpoint = System.getenv("FILESTORE_URL");
        fileStoreServiceConnector = new FileStoreServiceConnector(client, endpoint);
        LOGGER.info("Using service endpoint {}", endpoint);
    }

    public FileStoreServiceConnector getConnector() {
        return fileStoreServiceConnector;
    }

    @PreDestroy
    public void tearDownConnector() {
        HttpClient.closeClient(fileStoreServiceConnector.getClient());
    }
}
