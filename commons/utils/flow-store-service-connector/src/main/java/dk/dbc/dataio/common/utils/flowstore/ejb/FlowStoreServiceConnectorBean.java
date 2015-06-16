package dk.dbc.dataio.common.utils.flowstore.ejb;

import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.commons.utils.httpclient.HttpClient;
import dk.dbc.dataio.commons.utils.service.ServiceUtil;
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
 * to the flow-store REST interface.
 */
@Singleton
@LocalBean
public class FlowStoreServiceConnectorBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(FlowStoreServiceConnectorBean.class);

    FlowStoreServiceConnector flowStoreServiceConnector;

    @PostConstruct
    public void initializeConnector() {
        LOGGER.debug("Initializing connector");
        final Client client = HttpClient.newClient(new ClientConfig()
                .register(new JacksonFeature()));
        try {
            final String endpoint = ServiceUtil.getFlowStoreServiceEndpoint();
            flowStoreServiceConnector = new FlowStoreServiceConnector(client, endpoint);
        } catch (NamingException e) {
            throw new EJBException(e);
        }
    }

    public FlowStoreServiceConnector getConnector() {
        return flowStoreServiceConnector;
    }

    @PreDestroy
    public void tearDownConnector() {
        HttpClient.closeClient(flowStoreServiceConnector.getHttpClient());
    }
}
