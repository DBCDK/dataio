package dk.dbc.dataio.logstore.service.connector.ejb;

import dk.dbc.dataio.commons.types.jndi.JndiConstants;
import dk.dbc.dataio.commons.utils.httpclient.HttpClient;
import dk.dbc.dataio.commons.utils.service.ServiceUtil;
import dk.dbc.dataio.logstore.service.connector.LogStoreServiceConnector;
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
 * This Enterprise Java Bean (EJB) is used as a connector
 * to the log-store REST interface.
 * <p>
 * This class expects that the log-store service endpoint can be looked
 * up via the {@value dk.dbc.dataio.commons.types.jndi.JndiConstants#URL_RESOURCE_LOGSTORE_RS}
 * JNDI name
 * </p>
 */
@Singleton
@LocalBean
public class LogStoreServiceConnectorBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(LogStoreServiceConnectorBean.class);

    Client client;
    LogStoreServiceConnector logStoreServiceConnector;

    @PostConstruct
    public void initializeConnector() {
        LOGGER.debug("Initializing connector");
        client = HttpClient.newClient();
        try {
            final String endpoint = ServiceUtil.getStringValueFromResource(JndiConstants.URL_RESOURCE_LOGSTORE_RS);
            logStoreServiceConnector = new LogStoreServiceConnector(client, endpoint);
        } catch (NamingException e) {
            throw new EJBException(e);
        }
    }

    public LogStoreServiceConnector getConnector() {
        return logStoreServiceConnector;
    }

    @PreDestroy
    public void tearDownConnector() {
        HttpClient.closeClient(client);
    }
}
