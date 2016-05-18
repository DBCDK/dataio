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

package dk.dbc.dataio.flowstore.ejb;

import dk.dbc.dataio.commons.types.jndi.JndiConstants;
import dk.dbc.dataio.commons.types.rest.FlowStoreServiceConstants;
import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;
import dk.dbc.dataio.commons.utils.service.ServiceUtil;
import dk.dbc.dataio.flowstore.entity.HarvesterConfig;
import dk.dbc.dataio.jsonb.JSONBContext;
import dk.dbc.dataio.jsonb.JSONBException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Stateless;
import javax.naming.NamingException;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

@Stateless
@Path("/")
public class HarvestersBean extends AbstractResourceBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(HarvestersBean.class);

    @PersistenceContext
    EntityManager entityManager;

    JSONBContext jsonbContext = new JSONBContext();

    /**
     * Returns list of all stored harvester RR configs
     *
     * @return a HTTP OK response with result list as JSON
     * @throws NamingException on failure to create result list as JSON
     */
    @GET
    @Path(FlowStoreServiceConstants.HARVESTERS_RR_CONFIG)
    @Produces({MediaType.APPLICATION_JSON})
    public Response getHarvesterRrConfigs() throws NamingException {
        final String jsonConfig = ServiceUtil.getStringValueFromResource(JndiConstants.CONFIG_RESOURCE_HARVESTER_RR);
        return Response
                .ok()
                .entity(jsonConfig)
                .build();
    }

    /**
     * Creates a new harvester config
     * @param uriInfo URI information
     * @param type type of config as class name with full path
     * @param configContent content of the created harvester config
     * @return a HTTP 201 CREATED response with created harvester config as JSON,
     *         a HTTP 400 BAD REQUEST response if type is unknown, f content is invalid JSON or if content is not of type.
     *         a HTTP 500 INTERNAL SERVER ERROR response in case of general error.
     * @throws ClassNotFoundException if type is unknown
     * @throws JSONBException if content is invalid JSON or if content is not of type
     */
    @POST
    @Path(FlowStoreServiceConstants.HARVESTER_CONFIGS_TYPED)
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public Response createHarvesterConfig(@Context UriInfo uriInfo, @PathParam("type") String type, String configContent)
            throws ClassNotFoundException, JSONBException {
        LOGGER.trace("Called with type='{}', content='{}'", type, configContent);
        InvariantUtil.checkNotNullNotEmptyOrThrow(configContent, "configContent");

        validateContent(type, configContent);

        final HarvesterConfig harvesterConfig = saveAsVersionedEntity(entityManager, HarvesterConfig.class, configContent);
        entityManager.flush();
        return Response
                .created(getResourceUriOfVersionedEntity(uriInfo.getAbsolutePathBuilder(), harvesterConfig))
                .entity(jsonbContext.marshall(harvesterConfig))
                .tag(harvesterConfig.getVersion().toString())
                .build();
    }

    private void validateContent(String type, String content) throws ClassNotFoundException, JSONBException {
        // We assume that Content is always an inner class of given type.
        final Class<?> clazz = Class.forName(type + "$Content");
        // unmarshall to make sure the input is valid
        jsonbContext.unmarshall(content, clazz);
    }
}

