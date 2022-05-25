package dk.dbc.dataio.logstore.service.connector.ejb;

import dk.dbc.dataio.logstore.service.connector.LogStoreServiceConnector;
import dk.dbc.httpclient.HttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.LocalBean;
import javax.ejb.Singleton;

// TODO: 05-07-19 replace EJB with @ApplicationScoped CDI producer

@Singleton
@LocalBean
public class LogStoreServiceConnectorBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(LogStoreServiceConnectorBean.class);

    LogStoreServiceConnector logStoreServiceConnector;

    @PostConstruct
    public void initializeConnector() {
        final String endpoint = System.getenv("LOGSTORE_URL");
        logStoreServiceConnector = new LogStoreServiceConnector(
                HttpClient.newClient(), endpoint);
        LOGGER.info("Using service endpoint {}", endpoint);
    }

    public LogStoreServiceConnector getConnector() {
        return logStoreServiceConnector;
    }

    @PreDestroy
    public void tearDownConnector() {
        HttpClient.closeClient(logStoreServiceConnector.getHttpClient());
    }
}
