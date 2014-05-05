package dk.dbc.dataio.filestore.service.connector.ejb;

import dk.dbc.dataio.commons.utils.httpclient.HttpClient;
import dk.dbc.dataio.commons.utils.service.ServiceUtil;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnector;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnectorException;
import org.glassfish.jersey.apache.connector.ApacheConnectorProvider;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.EJBException;
import javax.ejb.LocalBean;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.naming.NamingException;
import javax.ws.rs.client.Client;
import java.io.InputStream;

/**
 * This Enterprise Java Bean (EJB) singleton is used as a connector
 * to the file-store REST interface.
 * <p>
 * This class expects that the file-store service endpoint can be looked
 * up via the {@value dk.dbc.dataio.commons.utils.service.ServiceUtil#FILE_STORE_SERVICE_ENDPOINT_RESOURCE}
 * JNDI name
 * </p>
 */
@Singleton
@LocalBean
public class FileStoreServiceConnectorBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(FileStoreServiceConnectorBean.class);

    Client client;

    @PostConstruct
    public void initializeConnector() {
        LOGGER.debug("Initializing connector");
        /* Since we need to be able to add data amounts exceeding the JVM
           HEAP size and the default jersey client connector does not
           adhere to the CHUNKED_ENCODING_SIZE property we use the Apache
           HttpClient connector instead to avoid OutOfMemory errors.
         */
        final ClientConfig config = new ClientConfig();
        config.connectorProvider(new ApacheConnectorProvider());
        config.property(ClientProperties.CHUNKED_ENCODING_SIZE, 8 * 1024);
        client = HttpClient.newClient(config);
    }

    @Lock(LockType.READ)
    public String addFile(final InputStream is) throws EJBException, FileStoreServiceConnectorException {
        LOGGER.debug("Adding file");
        try {
            // performance: consider JNDI lookup cache or service-locator pattern
            final String baseUrl = ServiceUtil.getFileStoreServiceEndpoint();
            final FileStoreServiceConnector fileStoreServiceConnector = new FileStoreServiceConnector(client, baseUrl);
            return fileStoreServiceConnector.addFile(is);
        } catch (NamingException e) {
            throw new EJBException(e);
        }
    }

    @Lock(LockType.READ)
    public InputStream getFile(final String fileId) throws FileStoreServiceConnectorException {
        LOGGER.debug("Getting file with id '{}'", fileId);
        try {
            // performance: consider JNDI lookup cache or service-locator pattern
            final String baseUrl = ServiceUtil.getFileStoreServiceEndpoint();
            final FileStoreServiceConnector fileStoreServiceConnector = new FileStoreServiceConnector(client, baseUrl);
            return fileStoreServiceConnector.getFile(fileId);
        } catch (NamingException e) {
            throw new EJBException(e);
        }

    }

    @PreDestroy
    public void tearDownConnector() {
        HttpClient.closeClient(client);
    }
}
