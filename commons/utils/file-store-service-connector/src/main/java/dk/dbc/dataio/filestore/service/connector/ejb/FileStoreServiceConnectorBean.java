package dk.dbc.dataio.filestore.service.connector.ejb;

import dk.dbc.dataio.commons.utils.httpclient.HttpClient;
import dk.dbc.dataio.commons.utils.service.ServiceUtil;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnector;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.glassfish.jersey.apache.connector.ApacheClientProperties;
import org.glassfish.jersey.apache.connector.ApacheConnectorProvider;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.EJBException;
import javax.ejb.LocalBean;
import javax.ejb.Singleton;
import javax.naming.NamingException;
import javax.ws.rs.client.Client;

/**
 * This Enterprise Java Bean (EJB) singleton is used as a connector
 * to the file-store REST interface.
 */

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
        Client client = HttpClient.newClient(config);

        try {
            final String endpoint = ServiceUtil.getFileStoreServiceEndpoint();
            fileStoreServiceConnector = new FileStoreServiceConnector(client, endpoint);
            LOGGER.info("Using service endpoint {}", endpoint);
        } catch (NamingException e) {
            throw new EJBException(e);
        }
    }

    public FileStoreServiceConnector getConnector() {
        return fileStoreServiceConnector;
    }

    @PreDestroy
    public void tearDownConnector() {
        HttpClient.closeClient(fileStoreServiceConnector.getHttpClient());
    }
}
