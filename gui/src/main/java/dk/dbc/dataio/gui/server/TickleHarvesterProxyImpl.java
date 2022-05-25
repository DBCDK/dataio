package dk.dbc.dataio.gui.server;

import dk.dbc.dataio.commons.utils.service.ServiceUtil;
import dk.dbc.dataio.gui.client.exceptions.ProxyError;
import dk.dbc.dataio.gui.client.exceptions.ProxyException;
import dk.dbc.dataio.gui.client.exceptions.StatusCodeTranslator;
import dk.dbc.dataio.gui.client.proxies.TickleHarvesterProxy;
import dk.dbc.dataio.harvester.connector.TickleHarvesterServiceConnector;
import dk.dbc.dataio.harvester.task.connector.HarvesterTaskServiceConnectorException;
import dk.dbc.dataio.harvester.task.connector.HarvesterTaskServiceConnectorUnexpectedStatusCodeException;
import dk.dbc.dataio.harvester.types.HarvestSelectorRequest;
import dk.dbc.dataio.harvester.types.HarvestTaskSelector;
import dk.dbc.dataio.harvester.types.TickleRepoHarvesterConfig;
import dk.dbc.httpclient.HttpClient;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Client;
import java.time.Instant;

public class TickleHarvesterProxyImpl implements TickleHarvesterProxy {
    private static final Logger log = LoggerFactory.getLogger(TickleHarvesterProxyImpl.class);
    final Client client;
    final String endpoint;

    // Class scoped due to test
    TickleHarvesterServiceConnector tickleHarvesterServiceConnector;

    public TickleHarvesterProxyImpl() {
        final ClientConfig clientConfig = new ClientConfig().register(new JacksonFeature());
        client = HttpClient.newClient(clientConfig);
        endpoint = ServiceUtil.getStringValueFromSystemEnvironmentOrProperty("TICKLE_REPO_HARVESTER_URL");
        log.info("TickleHarvesterProxy: Using Endpoint {}", endpoint);
        tickleHarvesterServiceConnector = new TickleHarvesterServiceConnector(client, endpoint);
    }

    @Override
    public void createHarvestTask(TickleRepoHarvesterConfig config) throws ProxyException {
        final HarvestTaskSelector harvestTaskSelector = new HarvestTaskSelector("datasetName", config.getContent().getDatasetName());
        try {
            tickleHarvesterServiceConnector.createHarvestTask(config.getId(), new HarvestSelectorRequest(harvestTaskSelector));
        } catch (HarvesterTaskServiceConnectorUnexpectedStatusCodeException e) {
            log.error("TickleHarvesterProxy: createHarvestTask - Unexpected Status Code Exception({})", StatusCodeTranslator.toProxyError(e.getStatusCode()), e);
            throw new ProxyException(StatusCodeTranslator.toProxyError(e.getStatusCode()), e);
        } catch (HarvesterTaskServiceConnectorException e) {
            log.error("TickleHarvesterProxy: createHarvestTask - Service Not Found Exception", e);
            throw new ProxyException(ProxyError.SERVICE_NOT_FOUND, e);
        }
    }

    @Override
    public int getDataSetSizeEstimate(String dataSetName) throws ProxyException {
        try {
            return tickleHarvesterServiceConnector.getDataSetSizeEstimate(dataSetName);
        } catch (HarvesterTaskServiceConnectorUnexpectedStatusCodeException e) {
            log.error("TickleHarvesterProxy: getDataSetSizeEstimate - Unexpected Status Code Exception({})", StatusCodeTranslator.toProxyError(e.getStatusCode()), e);
            throw new ProxyException(StatusCodeTranslator.toProxyError(e.getStatusCode()), e);
        } catch (HarvesterTaskServiceConnectorException e) {
            log.error("TickleHarvesterProxy: getDataSetSizeEstimate - Service Not Found Exception", e);
            throw new ProxyException(ProxyError.SERVICE_NOT_FOUND, e);
        }
    }

    @Override
    public void deleteOutdatedRecords(String dataSetName, long fromDateEpochMillis) throws ProxyException {
        try {
            tickleHarvesterServiceConnector.deleteOutdatedRecords(
                    dataSetName, Instant.ofEpochMilli(fromDateEpochMillis));
        } catch (HarvesterTaskServiceConnectorUnexpectedStatusCodeException e) {
            log.error("TickleHarvesterProxy: deleteOutdatedRecords - Unexpected Status Code Exception({})",
                    StatusCodeTranslator.toProxyError(e.getStatusCode()), e);
            throw new ProxyException(StatusCodeTranslator.toProxyError(e.getStatusCode()), e);
        } catch (HarvesterTaskServiceConnectorException e) {
            log.error("TickleHarvesterProxy: deleteOutdatedRecords - Service Not Found Exception", e);
            throw new ProxyException(ProxyError.SERVICE_NOT_FOUND, e);
        }
    }

    @Override
    public void close() {
        HttpClient.closeClient(client);
    }
}
