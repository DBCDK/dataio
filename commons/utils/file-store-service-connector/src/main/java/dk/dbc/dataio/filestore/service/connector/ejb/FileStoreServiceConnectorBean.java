package dk.dbc.dataio.filestore.service.connector.ejb;

import dk.dbc.dataio.commons.types.jndi.JndiConstants;
import dk.dbc.dataio.commons.utils.httpclient.HttpClient;
import dk.dbc.dataio.commons.utils.service.ServiceUtil;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnector;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jackson.JacksonFeature;
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

    FileStoreServiceConnector fileStoreServiceConnector;

    @PostConstruct
    public void initializeConnector() {
        LOGGER.debug("Initializing connector");
        final Client client = HttpClient.newClient(new ClientConfig()
                .register(new JacksonFeature()));
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
