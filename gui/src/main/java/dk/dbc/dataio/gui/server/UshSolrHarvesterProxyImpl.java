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

import dk.dbc.dataio.commons.time.StopWatch;
import dk.dbc.httpclient.HttpClient;
import dk.dbc.dataio.commons.utils.service.ServiceUtil;
import dk.dbc.dataio.gui.client.exceptions.ProxyError;
import dk.dbc.dataio.gui.client.exceptions.ProxyException;
import dk.dbc.dataio.gui.client.exceptions.StatusCodeTranslator;
import dk.dbc.dataio.gui.client.proxies.UshSolrHarvesterProxy;
import dk.dbc.dataio.ushsolrharvester.service.connector.UshSolrHarvesterServiceConnector;
import dk.dbc.dataio.ushsolrharvester.service.connector.UshSolrHarvesterServiceConnectorException;
import dk.dbc.dataio.ushsolrharvester.service.connector.UshSolrHarvesterServiceConnectorUnexpectedStatusCodeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.NamingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;

public class UshSolrHarvesterProxyImpl implements UshSolrHarvesterProxy {
    private static final Logger log = LoggerFactory.getLogger(UshSolrHarvesterProxyImpl.class);
    final Client client;
    final String baseUrl;
    UshSolrHarvesterServiceConnector ushSolrHarvesterServiceConnector;

    public UshSolrHarvesterProxyImpl() throws NamingException {
        client = HttpClient.newClient();
        baseUrl = ServiceUtil.getUshSolrHarvesterServiceEndpoint();
        log.info("Using Base URL {}", baseUrl);
        ushSolrHarvesterServiceConnector = new UshSolrHarvesterServiceConnector(client, baseUrl);
    }

    //This constructor is intended for test purpose only with reference to dependency injection.
    UshSolrHarvesterProxyImpl(UshSolrHarvesterServiceConnector ushSolrHarvesterServiceConnector) throws NamingException{
        this.ushSolrHarvesterServiceConnector = ushSolrHarvesterServiceConnector;
        client = HttpClient.newClient();
        baseUrl = ServiceUtil.getUshSolrHarvesterServiceEndpoint();
        log.info("Using Base URL {}", baseUrl);
    }

    @Override
    public String runTestHarvest(long id) throws ProxyException {
        final String itemLog;
        log.trace("runTestHarvest({});", id);
        final StopWatch stopWatch = new StopWatch();
        try {
            itemLog = ushSolrHarvesterServiceConnector.runTestHarvest(id);
        } catch (NullPointerException | IllegalArgumentException e) {
            log.error("runTestHarvest - Exception", e);
            throw new ProxyException(ProxyError.BAD_REQUEST, e);
        } catch (UshSolrHarvesterServiceConnectorUnexpectedStatusCodeException e) {
            if (e.getStatusCode() == Response.Status.NO_CONTENT.getStatusCode()) {
                log.error("runTestHarvest - No Content Status Code Exception", e);
                throw new ProxyException(ProxyError.NO_CONTENT, e);
            } else {
                log.error("runTestHarvest - Unexpected Status Code Exception", e);
                throw new ProxyException(StatusCodeTranslator.toProxyError(e.getStatusCode()),e.getMessage());
            }
        } catch (UshSolrHarvesterServiceConnectorException e) {
            log.error("runTestHarvest - Service Connector Exception", e);
            throw new ProxyException(ProxyError.SERVICE_NOT_FOUND, e);
        } finally {
            log.debug("runTestHarvest took {} milliseconds", stopWatch.getElapsedTime());
        }
        return itemLog;
    }

    public void close() {
        HttpClient.closeClient(client);
    }
}
