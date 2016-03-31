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

import dk.dbc.dataio.commons.types.interceptor.Stopwatch;
import dk.dbc.dataio.commons.types.rest.FlowStoreServiceConstants;
import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;
import dk.dbc.dataio.flowstore.entity.GatekeeperDestinationEntity;
import dk.dbc.dataio.jsonb.JSONBContext;
import dk.dbc.dataio.jsonb.JSONBException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.List;

@Stateless
@Path("/")
public class GatekeeperDestinationsBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(GatekeeperDestinationsBean.class);
    private static final String NULL_ENTITY = "";

    JSONBContext jsonbContext = new JSONBContext();

    @PersistenceContext
    EntityManager entityManager;

    /**
     * Creates a new gatekeeper destination
     *
     * @param uriInfo URI information
     * @param gatekeeperDestination The gatekeeperDestination to save
     *
     * @return a HTTP 201 response with gatekeeper destination as JSON,
     *         a HTTP 406 response in case of Unique Constraint Violation
     *         a HTTP 500 response in case of general error.
     *
     * @throws JSONBException on failure to create json gatekeeperDestination
     */
    @POST
    @Path(FlowStoreServiceConstants.GATEKEEPER_DESTINATIONS)
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Stopwatch
    public Response createGatekeeperDestination(@Context UriInfo uriInfo, String gatekeeperDestination) throws JSONBException {
        LOGGER.trace("GateKeeperDestination {}", gatekeeperDestination);
        InvariantUtil.checkNotNullNotEmptyOrThrow(gatekeeperDestination, "gatekeeperDestination");
        final GatekeeperDestinationEntity gatekeeperDestinationEntity = saveEntity(gatekeeperDestination);

        return Response.created(getUri(uriInfo, String.valueOf(gatekeeperDestinationEntity.getId())))
                .entity(jsonbContext.marshall(gatekeeperDestinationEntity))
                .build();
    }

    /**
     * Returns list of all stored gatekeeper destinations sorted by submitterNumber in ascending order
     * @return a HTTP OK response with result list as JSON
     * @throws JSONBException on failure to create result list as JSON
     */
    @GET
    @Path(FlowStoreServiceConstants.GATEKEEPER_DESTINATIONS)
    @Produces({ MediaType.APPLICATION_JSON })
    public Response findAllGatekeeperDestinations() throws JSONBException {
        final Query query = entityManager.createNamedQuery(GatekeeperDestinationEntity.QUERY_FIND_ALL);
        final List results = query.getResultList();
        return Response
                .ok()
                .entity(jsonbContext.marshall(results))
                .build();
    }

    /**
     * Deletes an existing gatekeeper destination
     *
     * @param id of the gatekeeper destination
     *
     * @return a HTTP 204 response with no content,
     *         a HTTP 404 response in case of gatekeeper destination not found
     *         a HTTP 500 response in case of general error.
     */
    @DELETE
    @Path(FlowStoreServiceConstants.GATEKEEPER_DESTINATION)
    public Response deleteGatekeeperDestination(
            @PathParam(FlowStoreServiceConstants.ID_VARIABLE) Long id) {

        final GatekeeperDestinationEntity gatekeeperDestinationEntity = entityManager.find(GatekeeperDestinationEntity.class, id);

        if(gatekeeperDestinationEntity == null) {
            return Response.status(Response.Status.NOT_FOUND).entity(NULL_ENTITY).build();
        }

        entityManager.remove(gatekeeperDestinationEntity);
        return Response.noContent().build();
    }

    /*
     * Private methods
     */

    /**
     *
     * @param gatekeeperDestination to persist
     * @return persisted gatekeeperDestination
     * @throws JSONBException JSONBException on failure unmarshalling to gatekeeperDestinationEntity
     */
    private GatekeeperDestinationEntity saveEntity(String gatekeeperDestination) throws JSONBException {
        GatekeeperDestinationEntity gatekeeperDestinationEntity = jsonbContext.unmarshall(gatekeeperDestination, GatekeeperDestinationEntity.class);
        entityManager.persist(gatekeeperDestinationEntity);
        entityManager.flush();
        entityManager.refresh(gatekeeperDestinationEntity);
        return gatekeeperDestinationEntity;
    }

    private URI getUri(UriInfo uriInfo, String id) {
        final UriBuilder absolutePathBuilder = uriInfo.getAbsolutePathBuilder();
        return absolutePathBuilder.path(id).build();
    }
}
