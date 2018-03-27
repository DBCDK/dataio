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
import dk.dbc.dataio.gui.client.exceptions.StatusCodeTranslator;
import dk.dbc.dataio.gui.client.exceptions.ProxyException;
import dk.dbc.dataio.gui.client.proxies.LogStoreProxy;
import dk.dbc.dataio.logstore.service.connector.LogStoreServiceConnector;
import dk.dbc.dataio.logstore.service.connector.LogStoreServiceConnectorException;
import dk.dbc.dataio.logstore.service.connector.LogStoreServiceConnectorUnexpectedStatusCodeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.NamingException;
import javax.ws.rs.client.Client;

public class LogStoreProxyImpl implements LogStoreProxy {
    private static final Logger log = LoggerFactory.getLogger(LogStoreProxyImpl.class);
    final Client client;
    final String baseUrl;
    LogStoreServiceConnector logStoreServiceConnector;

    public LogStoreProxyImpl() throws NamingException {
        client = HttpClient.newClient();
        baseUrl = ServiceUtil.getLogStoreServiceEndpoint();
        log.info("LogStoreProxy: Using Base URL {}", baseUrl);
        logStoreServiceConnector = new LogStoreServiceConnector(client, baseUrl);
    }

    //This constructor is intended for test purpose only with reference to dependency injection.
    LogStoreProxyImpl(LogStoreServiceConnector logStoreServiceConnector) throws NamingException{
        this.logStoreServiceConnector = logStoreServiceConnector;
        client = HttpClient.newClient();
        baseUrl = ServiceUtil.getLogStoreServiceEndpoint();
        log.info("LogStoreProxy: Using Base URL {}", baseUrl);
    }

    @Override
    public String getItemLog(String jobId, Long chunkId, Long itemId) throws ProxyException {
        final String itemLog;
        log.trace("LogStoreProxy: getItemLog({}, {}, {});", jobId, chunkId, itemId);
        final StopWatch stopWatch = new StopWatch();
        try {
            itemLog = logStoreServiceConnector.getItemLog(jobId, chunkId, itemId);
        } catch (NullPointerException e) {
            log.error("LogStoreProxy: getItemLog - Null Pointer Exception", e);
            throw new ProxyException(ProxyError.BAD_REQUEST, e);
        } catch (IllegalArgumentException e) {
            log.error("LogStoreProxy: getItemLog - Illegal Argument Exception", e);
            throw new ProxyException(ProxyError.BAD_REQUEST, e);
        } catch (LogStoreServiceConnectorUnexpectedStatusCodeException e) {
            log.error("LogStoreProxy: getItemLog - Unexpected Status Code Exception", e);
            throw new ProxyException(StatusCodeTranslator.toProxyError(e.getStatusCode()),e.getMessage());
        } catch (LogStoreServiceConnectorException e) {
            log.error("LogStoreProxy: getItemLog - Service Connector Exception", e);
            throw new ProxyException(ProxyError.SERVICE_NOT_FOUND, e);
        } finally {
            log.debug("LogStoreProxy: getItemLog took {} milliseconds", stopWatch.getElapsedTime());
        }
        return itemLog;
    }

    public void close() {
        HttpClient.closeClient(client);
    }
}
