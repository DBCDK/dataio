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
import dk.dbc.dataio.commons.types.PingResponse;
import dk.dbc.dataio.commons.types.rest.SinkServiceConstants;
import dk.dbc.dataio.commons.utils.httpclient.HttpClient;
import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;
import dk.dbc.dataio.gui.client.exceptions.ProxyError;
import dk.dbc.dataio.gui.client.exceptions.ProxyException;
import dk.dbc.dataio.gui.client.model.PingResponseModel;
import dk.dbc.dataio.gui.client.model.SinkModel;
import dk.dbc.dataio.gui.client.proxies.SinkServiceProxy;
import dk.dbc.dataio.gui.server.modelmappers.PingResponseModelMapper;
import dk.dbc.dataio.gui.server.modelmappers.SinkModelMapper;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;

public class SinkServiceProxyImpl implements SinkServiceProxy {
    private static final Logger log = LoggerFactory.getLogger(SinkServiceProxyImpl.class);
    private Client client = null;

    public SinkServiceProxyImpl() {
        final ClientConfig clientConfig = new ClientConfig().register(new JacksonFeature());
        client = HttpClient.newClient(clientConfig);
    }

    @Override
    public PingResponseModel ping(SinkModel model) throws ProxyException {
        log.trace("SinkServiceProxy: ping({}, \"{}\");", model.getId(), model.getSinkName());
        InvariantUtil.checkNotNullOrThrow(model, "model");
        final StopWatch stopWatch = new StopWatch();

        final Response response;
        final PingResponse result;
        try {
            response = HttpClient.doPostWithJson(client, SinkModelMapper.toSinkContent(model),
                    ServletUtil.getSinkServiceEndpoint(), SinkServiceConstants.PING);
        } catch (ServletException e) {
            log.error("SinkServiceProxy: ping - Service Not Found Exception", e);
            throw new ProxyException(ProxyError.SERVICE_NOT_FOUND, e);
        }
        try {
            assertStatusCode(response, Response.Status.OK);
            result = response.readEntity(PingResponse.class);
        } finally {
            response.close();
            log.debug("SinkServiceProxy: ping took {} milliseconds", stopWatch.getElapsedTime());
        }
        return PingResponseModelMapper.toModel(result);
    }


    @Override
    public void close() {
        log.trace("SinkServiceProxy: close();");
        HttpClient.closeClient(client);
    }

    private void assertStatusCode(Response response, Response.Status expectedStatus) throws ProxyException {
        final Response.Status status = Response.Status.fromStatusCode(response.getStatus());
        if (status != expectedStatus) {
            final ProxyError errorCode;
            switch (status) {
                case BAD_REQUEST: errorCode = ProxyError.BAD_REQUEST;
                    break;
                case NOT_FOUND: errorCode = ProxyError.SERVICE_NOT_FOUND;
                    break;
                default:
                    errorCode = ProxyError.INTERNAL_SERVER_ERROR;
            }
            log.error("SinkServiceProxy: assertStatusCode Error({})", errorCode);
            throw new ProxyException(errorCode, response.readEntity(String.class));
        }
    }
}
