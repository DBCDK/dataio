/*
 * DataIO - Data IO
 * Copyright (C) 2015 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
 * Denmark. CVR: 15149043
 *
 * This file is part of DataIO.
 *
 * DataIO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DataIO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DataIO.  If not, see <http://www.gnu.org/licenses/>.
 */

package dk.dbc.dataio.gui.server;

import dk.dbc.dataio.commons.utils.httpclient.HttpClient;
import dk.dbc.dataio.commons.utils.service.ServiceUtil;
import dk.dbc.dataio.gui.client.exceptions.ProxyError;
import dk.dbc.dataio.gui.client.exceptions.ProxyException;
import dk.dbc.dataio.gui.client.exceptions.StatusCodeTranslator;
import dk.dbc.dataio.gui.client.proxies.TickleHarvesterProxy;
import dk.dbc.dataio.harvester.task.connector.HarvesterTaskServiceConnector;
import dk.dbc.dataio.harvester.task.connector.HarvesterTaskServiceConnectorException;
import dk.dbc.dataio.harvester.task.connector.HarvesterTaskServiceConnectorUnexpectedStatusCodeException;
import dk.dbc.dataio.harvester.types.HarvestSelectorRequest;
import dk.dbc.dataio.harvester.types.HarvestTaskSelector;
import dk.dbc.dataio.harvester.types.TickleRepoHarvesterConfig;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.NamingException;
import javax.ws.rs.client.Client;

public class TickleHarvesterProxyImpl implements TickleHarvesterProxy {
    private static final Logger log = LoggerFactory.getLogger(TickleHarvesterProxyImpl.class);
    final Client client;
    final String endpoint;

    // Class scoped due to test
    HarvesterTaskServiceConnector harvesterTaskServiceConnector;

    public TickleHarvesterProxyImpl() throws NamingException {
        final ClientConfig clientConfig = new ClientConfig().register(new JacksonFeature());
        client = HttpClient.newClient(clientConfig);
        endpoint = ServiceUtil.getTickleHarvesterServiceEndpoint();
        log.info("TickleHarvesterProxy: Using Endpoint {}", endpoint);
        harvesterTaskServiceConnector = new HarvesterTaskServiceConnector(client, endpoint);
    }

    @Override
    public void createHarvestTask(TickleRepoHarvesterConfig config) throws ProxyException {
        final HarvestTaskSelector harvestTaskSelector = new HarvestTaskSelector("datasetName", config.getContent().getDatasetName());
        try {
            harvesterTaskServiceConnector.createHarvestTask(config.getId(), new HarvestSelectorRequest(harvestTaskSelector));
        } catch (HarvesterTaskServiceConnectorUnexpectedStatusCodeException e) {
            log.error("TickleHarvesterProxy: createHarvestTask - Unexpected Status Code Exception({})", StatusCodeTranslator.toProxyError(e.getStatusCode()), e);
            throw new ProxyException(StatusCodeTranslator.toProxyError(e.getStatusCode()), e);
        }
        catch(HarvesterTaskServiceConnectorException e) {
            log.error("TickleHarvesterProxy: createHarvestTask - Service Not Found Exception", e);
            throw new ProxyException(ProxyError.SERVICE_NOT_FOUND, e);
        }
    }

    @Override
    public void close() {
        HttpClient.closeClient(client);
    }
}
