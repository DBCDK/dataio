package dk.dbc.dataio.gui.server;

import dk.dbc.dataio.commons.utils.service.ServiceUtil;
import dk.dbc.dataio.gui.client.exceptions.ProxyError;
import dk.dbc.dataio.gui.client.exceptions.ProxyException;
import dk.dbc.dataio.gui.client.proxies.PeriodicJobsHarvesterProxy;
import dk.dbc.dataio.harvester.periodicjobs.PeriodicJobsHarvesterConnectorUnexpectedStatusCodeException;
import dk.dbc.dataio.harvester.periodicjobs.PeriodicJobsHarvesterServiceConnector;
import dk.dbc.dataio.harvester.periodicjobs.PeriodicJobsHarvesterServiceConnectorException;
import dk.dbc.httpclient.HttpClient;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Client;

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

    @Override
    public void executePeriodicJob(Long harvesterId) throws ProxyException {
        try {
            connector.createPeriodicJob(harvesterId);
        } catch (PeriodicJobsHarvesterServiceConnectorException e) {
            if (e instanceof PeriodicJobsHarvesterConnectorUnexpectedStatusCodeException) {
                throw new ProxyException(toProxyError(
                        ((PeriodicJobsHarvesterConnectorUnexpectedStatusCodeException) e).getStatusCode()), e);
            } else {
                throw new ProxyException(ProxyError.INTERNAL_SERVER_ERROR, e);
            }
        }
    }

    public String executeSolrValidation(Long harvesterId) throws ProxyException {
        try {
            return connector.validatePeriodicJob(harvesterId);
        } catch (PeriodicJobsHarvesterServiceConnectorException e) {
            if (e instanceof PeriodicJobsHarvesterConnectorUnexpectedStatusCodeException) {
                throw new ProxyException(toProxyError(
                        ((PeriodicJobsHarvesterConnectorUnexpectedStatusCodeException) e).getStatusCode()), e);
            } else {
                throw new ProxyException(ProxyError.INTERNAL_SERVER_ERROR, e);
            }
        }
    }
}
