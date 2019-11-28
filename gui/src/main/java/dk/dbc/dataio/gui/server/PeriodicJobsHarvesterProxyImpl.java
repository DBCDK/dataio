package dk.dbc.dataio.gui.server;

import dk.dbc.dataio.commons.utils.service.ServiceUtil;
import dk.dbc.dataio.gui.client.exceptions.ProxyError;
import dk.dbc.dataio.gui.client.exceptions.ProxyException;
import dk.dbc.dataio.gui.client.proxies.PeriodicJobsHarvesterProxy;
import dk.dbc.dataio.harvester.periodicjobs.PeriodicJobsHarvesterConnectorUnexpectedStatusCodeException;
import dk.dbc.dataio.harvester.periodicjobs.PeriodicJobsHarvesterServiceConnector;
import dk.dbc.httpclient.HttpClient;
import javax.ws.rs.client.Client;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static dk.dbc.dataio.gui.client.exceptions.StatusCodeTranslator.toProxyError;

public class PeriodicJobsHarvesterProxyImpl implements PeriodicJobsHarvesterProxy {
    private static final Logger log = LoggerFactory.getLogger(PeriodicJobsHarvesterProxyImpl.class);
    final Client client;
    private final String baseUrl;
    private PeriodicJobsHarvesterServiceConnector connector;

    PeriodicJobsHarvesterProxyImpl() {
        final ClientConfig clientConfig = new ClientConfig().register(new JacksonFeature());
        client = HttpClient.newClient(clientConfig);
        baseUrl = ServiceUtil.getStringValueFromSystemEnvironmentOrProperty("PERIODIC_JOBS_HARVESTER_URL");
        log.info("PeriodicJobsHarvesterProxy: Using Base URL {}", baseUrl);
        connector = new PeriodicJobsHarvesterServiceConnector(client, baseUrl);

    }

    //This constructor is intended for test purpose only with reference to dependency injection.
    PeriodicJobsHarvesterProxyImpl(PeriodicJobsHarvesterServiceConnector periodicJobsHarvesterServiceConnector, Client client, String baseUrl) {
        this.connector = periodicJobsHarvesterServiceConnector;
        this.client = client;
        this.baseUrl = baseUrl;
        log.info("PeriodicJobsHarvesterProxy: Using Base URL {}", baseUrl);
    }

    @Override
    public void executePeriodicJob(Long id) throws ProxyException {
        try {
            connector.createPeriodicJob(id);
        } catch (PeriodicJobsHarvesterConnectorUnexpectedStatusCodeException e) {
            throw new ProxyException(toProxyError(e.getStatusCode()), e);
        }
    }
}
