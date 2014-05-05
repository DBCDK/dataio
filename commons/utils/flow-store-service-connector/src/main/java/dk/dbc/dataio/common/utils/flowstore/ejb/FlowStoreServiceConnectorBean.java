package dk.dbc.dataio.common.utils.flowstore.ejb;

import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.SinkContent;
import dk.dbc.dataio.commons.utils.httpclient.HttpClient;
import dk.dbc.dataio.commons.utils.jersey.jackson.Jackson2xFeature;
import dk.dbc.dataio.commons.utils.service.ServiceUtil;
import org.glassfish.jersey.client.ClientConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.*;
import javax.naming.NamingException;
import javax.ws.rs.client.Client;

/**
 * Created by sma on 29/04/14.
 */
@Singleton
@LocalBean
public class FlowStoreServiceConnectorBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(FlowStoreServiceConnectorBean.class);

    Client client;

    @PostConstruct
    public void initializeConnector() {
        LOGGER.debug("Initializing connector");
        client = HttpClient.newClient(new ClientConfig().register(new Jackson2xFeature()));
    }

    @Lock(LockType.READ)
    public Sink createSink(SinkContent sinkContent) throws FlowStoreServiceConnectorException {
        LOGGER.debug("Creating new sink");
        try {
            // performance: consider JNDI lookup cache or service-locator pattern
            final String baseUrl = ServiceUtil.getFlowStoreServiceEndpoint();
            final FlowStoreServiceConnector flowStoreServiceConnector = new FlowStoreServiceConnector(client, baseUrl);
            return flowStoreServiceConnector.createSink(sinkContent);
        } catch (NamingException e) {
            throw new EJBException(e);
        }
    }

    @Lock(LockType.READ)
    public Sink getSink(long sinkId) throws FlowStoreServiceConnectorException {
        LOGGER.debug("Retrieving sink with id: " + sinkId);
        try {
            final String baseUrl = ServiceUtil.getFlowStoreServiceEndpoint();
            final FlowStoreServiceConnector flowStoreServiceConnector = new FlowStoreServiceConnector(client, baseUrl);
            return flowStoreServiceConnector.getSink(sinkId);
        } catch (NamingException e) {
            throw new EJBException(e);
        }
    }

    @PreDestroy
    public void tearDownConnector() {
        HttpClient.closeClient(client);
    }
}
