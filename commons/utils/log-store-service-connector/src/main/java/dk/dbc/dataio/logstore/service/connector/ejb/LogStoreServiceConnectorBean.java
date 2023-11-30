package dk.dbc.dataio.logstore.service.connector.ejb;

import dk.dbc.dataio.logstore.service.connector.LogStoreServiceConnector;
import dk.dbc.httpclient.HttpClient;
import jakarta.annotation.PreDestroy;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO: 05-07-19 replace EJB with @ApplicationScoped CDI producer

@Singleton
@LocalBean
public class LogStoreServiceConnectorBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(LogStoreServiceConnectorBean.class);

    private final LogStoreServiceConnector logStoreServiceConnector;

    public LogStoreServiceConnectorBean() {
        this(System.getenv("LOGSTORE_URL"));
    }

    public LogStoreServiceConnectorBean(String logStoreUrl) {
        logStoreServiceConnector = new LogStoreServiceConnector(HttpClient.newClient(), logStoreUrl);
        LOGGER.info("Using service endpoint {}", logStoreUrl);
    }

    public LogStoreServiceConnector getConnector() {
        return logStoreServiceConnector;
    }

    @PreDestroy
    public void tearDownConnector() {
        HttpClient.closeClient(logStoreServiceConnector.getHttpClient());
    }
}
