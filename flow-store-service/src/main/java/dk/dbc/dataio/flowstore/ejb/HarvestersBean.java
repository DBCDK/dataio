package dk.dbc.dataio.flowstore.ejb;

import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.commons.types.rest.FlowStoreServiceConstants;
import dk.dbc.dataio.commons.utils.service.ServiceUtil;
import dk.dbc.dataio.flowstore.entity.HarvesterConfig;
import dk.dbc.invariant.InvariantUtil;
import jakarta.annotation.Resource;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.List;

@Stateless
@Path("/")
public class
HarvestersBean {
    public static final String NO_CONTENT = "";

    private static final Logger LOGGER = LoggerFactory.getLogger(HarvestersBean.class);

    @PersistenceContext
    EntityManager entityManager;

    JSONBContext jsonbContext = new JSONBContext();
    @Resource
    SessionContext sessionContext;


    /**
     * Creates a new harvester config
     *
     * @param uriInfo       URI information
     * @param type          type of config as class name with full path
     * @param configContent content of the created harvester config
     * @return a HTTP 201 CREATED response with created harvester config as JSON,
     * a HTTP 400 BAD REQUEST response if type is unknown, f content is invalid JSON or if content is not compatible with type.
     * a HTTP 500 INTERNAL SERVER ERROR response in case of general error.
     * @throws ClassNotFoundException if type is unknown
     * @throws JSONBException         if content is invalid JSON or if content is not compatible with type
     */
    @POST
    @Path(FlowStoreServiceConstants.HARVESTER_CONFIGS_TYPE)
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public Response createHarvesterConfig(@Context UriInfo uriInfo, @PathParam("type") String type, String configContent)
            throws ClassNotFoundException, JSONBException {
        LOGGER.trace("Called with type='{}', content='{}'", type, configContent);
        InvariantUtil.checkNotNullNotEmptyOrThrow(configContent, "configContent");

        validateContent(type, configContent);

        final HarvesterConfig harvesterConfig = new HarvesterConfig()
                .withContent(configContent)
                .withType(type);
        entityManager.persist(harvesterConfig);
        entityManager.flush();

        return Response.created(getResourceUriOfVersionedEntity(uriInfo.getAbsolutePathBuilder(), harvesterConfig))
                .entity(jsonbContext.marshall(harvesterConfig))
                .tag(harvesterConfig.getVersion().toString())
                .build();
    }

    /**
     * Updates an existing harvester config
     *
     * @param id            ID of the harvester config to be updated
     * @param version       current version of the harvester config to be updated (from "{@value dk.dbc.dataio.commons.types.rest.FlowStoreServiceConstants#IF_MATCH_HEADER}"-header)
     * @param type          type of the harvester config to be updated (from "{@value dk.dbc.dataio.commons.types.rest.FlowStoreServiceConstants#RESOURCE_TYPE_HEADER}"-header),
     *                      if not set the current type of the harvester config is retained.
     * @param configContent content of the updated harvester config
     * @return a HTTP 200 OK response with updated harvester config as JSON,
     * a HTTP 400 BAD REQUEST response if type is unknown, f content is invalid JSON or if content is not compatible with type,
     * a HTTP 404 NOT FOUND response if given ID does not exist,
     * a HTTP 409 CONFLICT response in case of concurrent-modification error,
     * a HTTP 500 INTERNAL SERVER ERROR response in case of general error.
     * @throws ClassNotFoundException if type is unknown
     * @throws JSONBException         if content is invalid JSON or if content is not compatible with type
     */
    @POST
    @Path(FlowStoreServiceConstants.HARVESTER_CONFIG)
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public Response updateHarvesterConfig(
            @PathParam(FlowStoreServiceConstants.ID_VARIABLE) Long id,
            @HeaderParam(FlowStoreServiceConstants.IF_MATCH_HEADER) Long version,
            @HeaderParam(FlowStoreServiceConstants.RESOURCE_TYPE_HEADER) String type, String configContent)
            throws ClassNotFoundException, JSONBException {

        LOGGER.trace("Called with id='{}', version='{}' type='{}', content='{}'", id, version, type, configContent);
        InvariantUtil.checkNotNullNotEmptyOrThrow(configContent, "configContent");

        HarvesterConfig harvesterConfig = update(id, type, configContent, version);
        if (harvesterConfig == null) {
            return Response.status(Response.Status.NOT_FOUND).entity(NO_CONTENT).build();
        }
        return Response.ok().entity(jsonbContext.marshall(harvesterConfig))
                .tag(harvesterConfig.getVersion().toString())
                .build();
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public HarvesterConfig update(Long id, String type, String configContent, Long version) throws ClassNotFoundException, JSONBException {
        HarvesterConfig harvesterConfig = entityManager.find(HarvesterConfig.class, id);
        if (harvesterConfig == null) {
            return null;
        }
        harvesterConfig.assertLatestVersion(version);
        if (type != null && !type.trim().isEmpty()) {
            harvesterConfig.withType(type);
        }
        validateContent(harvesterConfig.getType(), configContent);

        harvesterConfig.setContent(configContent);
        entityManager.flush();
        entityManager.getEntityManagerFactory().getCache().evict(HarvesterConfig.class, harvesterConfig.getId());
        return harvesterConfig;
    }



    /**
     * Retrieves harvester config from underlying data store
     *
     * @param id harvester config identifier
     * @return a HTTP 200 OK response with harvester config content as JSON,
     * a HTTP 404 NOT FOUND response if not found,
     * a HTTP 500 INTERNAL SERVER ERROR response in case of general error.
     * @throws JSONBException on failure to marshall found harvester config
     */
    @GET
    @Path(FlowStoreServiceConstants.HARVESTER_CONFIG)
    @Produces({MediaType.APPLICATION_JSON})
    public Response getHarvesterConfig(@PathParam(FlowStoreServiceConstants.ID_VARIABLE) Long id) throws JSONBException {
        final HarvesterConfig harvesterConfig = entityManager.find(HarvesterConfig.class, id);
        if (harvesterConfig == null) {
            return Response.status(Response.Status.NOT_FOUND).entity(NO_CONTENT).build();
        }
        return Response
                .ok()
                .entity(jsonbContext.marshall(harvesterConfig))
                .tag(harvesterConfig.getVersion().toString())
                .build();
    }

    /**
     * Deletes an existing harvester config
     *
     * @param id      ID of config to be deleted
     * @param version current version of config at the time of deletion
     * @return a HTTP 204 NO CONTENT response with no content if config is deleted,
     * a HTTP 404 NOT FOUND response in case of ID not found,
     * a HTTP 409 CONFLICT response in case of version conflict,
     * a HTTP 500 INTERNAL SERVER ERROR response in case of general error.
     */
    @DELETE
    @Path(FlowStoreServiceConstants.HARVESTER_CONFIG)
    public Response deleteHarvesterConfig(
            @PathParam(FlowStoreServiceConstants.ID_VARIABLE) Long id,
            @HeaderParam(FlowStoreServiceConstants.IF_MATCH_HEADER) Long version) {
        HarvesterConfig harvesterConfig = entityManager.find(HarvesterConfig.class, id);
        if (harvesterConfig == null) {
            return Response.status(Response.Status.NOT_FOUND).entity(NO_CONTENT).build();
        }
        harvesterConfig.assertLatestVersion(version);

        entityManager.remove(harvesterConfig);
        entityManager.flush();
        return Response.noContent().build();
    }

    /**
     * Returns list of all harvester configs of given type
     *
     * @param type type of config as class name with full path
     * @return a HTTP 200 OK response with result list as JSON.
     * a HTTP 500 INTERNAL SERVER ERROR response in case of general error.
     */
    @GET
    @Path(FlowStoreServiceConstants.HARVESTER_CONFIGS_TYPE)
    @Produces({MediaType.APPLICATION_JSON})
    public Response findAllHarvesterConfigsByType(@PathParam("type") String type) {
        final Query query = entityManager.createNamedQuery(HarvesterConfig.QUERY_FIND_ALL_OF_TYPE)
                .setParameter(FlowStoreServiceConstants.TYPE_VARIABLE, type);
        final List<HarvesterConfig> results = query.getResultList();
        try {
            return Response.ok().entity(jsonbContext.marshall(results)).build();
        } catch (JSONBException e) {
            // Since JSONBException is mapped to BAD_REQUEST - note: this code will probably never be reached
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ServiceUtil.asJsonError(e)).build();
        }
    }

    /**
     * Returns list of all enabled harvester configs of given type
     *
     * @param type type of config as class name with full path
     * @return a HTTP 200 OK response with result list as JSON.
     * a HTTP 500 INTERNAL SERVER ERROR response in case of general error.
     */
    @GET
    @Path(FlowStoreServiceConstants.HARVESTER_CONFIGS_TYPE_ENABLED)
    @Produces({MediaType.APPLICATION_JSON})
    public Response findEnabledHarvesterConfigsByType(@PathParam("type") String type) {
        final Query query = entityManager.createNamedQuery(HarvesterConfig.QUERY_FIND_ALL_ENABLED_OF_TYPE)
                .setParameter(1, type);
        final List<HarvesterConfig> results = query.getResultList();
        try {
            return Response.ok().entity(jsonbContext.marshall(results)).build();
        } catch (JSONBException e) {
            // Since JSONBException is mapped to BAD_REQUEST - note: this code will probably never be reached
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ServiceUtil.asJsonError(e)).build();
        }
    }

    private void validateContent(String type, String content) throws ClassNotFoundException, JSONBException {
        // We assume that Content is always an inner class of given type.
        final Class<?> clazz = Class.forName(type + "$Content");
        // unmarshall to make sure the input is valid
        jsonbContext.unmarshall(content, clazz);
    }

    private URI getResourceUriOfVersionedEntity(UriBuilder uriBuilder, HarvesterConfig harvesterConfig) {
        return uriBuilder.path(String.valueOf(harvesterConfig.getId())).build();
    }

    public HarvestersBean self() {
        return sessionContext.getBusinessObject(HarvestersBean.class);
    }
}

