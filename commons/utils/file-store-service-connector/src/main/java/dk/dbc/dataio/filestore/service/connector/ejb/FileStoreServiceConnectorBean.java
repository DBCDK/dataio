package dk.dbc.dataio.filestore.service.connector.ejb;

import dk.dbc.dataio.commons.types.jndi.JndiConstants;
import dk.dbc.dataio.commons.utils.httpclient.HttpClient;
import dk.dbc.dataio.commons.utils.service.ServiceUtil;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnector;
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
    @SuppressWarnings("deprecation")
    public void initializeConnector() {
        LOGGER.debug("Initializing connector");
        /* Since we need to be able to add data amounts exceeding the JVM
           final Client client = HttpClient.newClient(new ClientConfig()
           HEAP size and the default jersey client connector does not
           adhere to the CHUNKED_ENCODING_SIZE property we use the Apache
           HttpClient connector instead to avoid OutOfMemory errors.
         */
        final ClientConfig config = new ClientConfig();
        // PoolingClientConnectionManager is deprecated in favour of
        // PoolingHttpClientConnectionManager but we need to bump jersey
        // version before this shift can be made.

        final org.apache.http.impl.conn.PoolingClientConnectionManager poolingClientConnectionManager = new org.apache.http.impl.conn.PoolingClientConnectionManager();

        poolingClientConnectionManager.setMaxTotal(MAX_HTTP_CONNECTIONS);
        poolingClientConnectionManager.setDefaultMaxPerRoute(MAX_HTTP_CONNECTIONS);
        config.property(ApacheClientProperties.CONNECTION_MANAGER, poolingClientConnectionManager);

        config.connectorProvider(new ApacheConnectorProvider());
        config.property(ClientProperties.CHUNKED_ENCODING_SIZE, 8 * 1024);
        Client client = HttpClient.newClient(config);

        try {
            final String endpoint = ServiceUtil.getStringValueFromResource(JndiConstants.URL_RESOURCE_FILESTORE_RS);
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
