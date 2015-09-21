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

package dk.dbc.dataio.sinkservice.ejb;

import dk.dbc.dataio.commons.types.PingResponse;
import dk.dbc.dataio.commons.types.SinkContent;
import dk.dbc.dataio.commons.types.rest.SinkServiceConstants;
import dk.dbc.dataio.commons.utils.json.JsonException;
import dk.dbc.dataio.commons.utils.json.JsonUtil;
import dk.dbc.dataio.sinkservice.ping.ResourcePing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.EJBException;
import javax.ejb.Stateless;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * This Enterprise Java Bean (EJB) class acts as a JAX-RS root resource
 * exposed by the '/SinkServiceConstants.PING' entry point
 */
@Stateless
@Path(SinkServiceConstants.PING)
public class PingBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(PingBean.class);

    /**
     * Pings sink defined by given content
     *
     * @param sinkContentData sink content as JSON string
     *
     * @return a HTTP 200 OK response with PingResponse entity.
     *         a HTTP 400 BAD_REQUEST response on invalid json content,
     *         a HTTP 500 INTERNAL_SERVER_ERROR response in case of general error.
     *
     * @throws EJBException when unable to obtain initial context, or if pinging
     * unknown resource type != {^jdbc/.*$, ^url/.*$}
     * @throws JsonException when given non-json sinkContent argument,
     * or if JSON object does not comply with model schema
     */
    @POST
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public Response ping(String sinkContentData) throws EJBException, JsonException {
        LOGGER.trace("SinkContent: {}", sinkContentData);
        final SinkContent sinkContent = JsonUtil.fromJson(sinkContentData, SinkContent.class);
        final InitialContext initialContext = getInitialContext();
        final PingResponse pingResponse;
        try {
            if (sinkContent.getResource().startsWith("jdbc/")) {
                pingResponse = ResourcePing.execute(initialContext, sinkContent.getResource(), DataSource.class);
            } else if (sinkContent.getResource().startsWith("url/")) {
                pingResponse = ResourcePing.execute(initialContext, sinkContent.getResource(), String.class);
            } else {
                throw new EJBException(String.format("Unknown resource type '%s'", sinkContent.getResource()));
            }
        } finally {
            closeInitialContext(initialContext);
        }
        return Response.ok().entity(JsonUtil.toJson(pingResponse)).build();
    }

    private static InitialContext getInitialContext() throws EJBException {
        final InitialContext initialContext;
        try {
            initialContext = new InitialContext();
        } catch (NamingException e) {
            throw new EJBException(e);
        }
        return initialContext;
    }

    private static void closeInitialContext(InitialContext initialContext) {
        if (initialContext != null) {
            try {
                initialContext.close();
            } catch (NamingException e) {
                LOGGER.warn("Unable to close initial context", e);
            }
        }
    }
}
