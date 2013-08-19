package dk.dbc.dataio.flowstore.util;

import dk.dbc.dataio.flowstore.entity.Entity;
import dk.dbc.dataio.flowstore.entity.EntityPrimaryKey;
import dk.dbc.dataio.flowstore.util.json.JsonException;
import dk.dbc.dataio.flowstore.util.json.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.Date;

/**
 * Utility class for implementation of RESTful service methods
 */
public class ServiceUtil {
    private static final Logger log = LoggerFactory.getLogger(ServiceUtil.class);

    private ServiceUtil() { }

    /**
     * Builds service method response
     *
     * @param status HTTP status code of response
     * @param entity entity to include in response
     * @param <T> type parameter for entity type
     *
     * @return response object
     */
    public static <T> Response buildResponse(Response.Status status, T entity) {
        return Response.status(status).entity(entity).build();
    }

    /**
     * Returns entity identified by given id and version as managed object
     *
     * @param entityManager entity manager
     * @param entityClass class of returned entity
     * @param id entity identifier
     * @param version entity version
     * @param <T> type parameter for entity type
     *
     * @return entity as managed object or null if no entity could be found
     */
    public static <T> T getEntity(EntityManager entityManager, Class<T> entityClass, Long id, Long version) {
        final EntityPrimaryKey pk = new EntityPrimaryKey(id, new Date(version));
        return entityManager.find(entityClass, pk);
    }

    /**
     * Saves new entity of given type and with given content
     *
     * @param entityManager entity manager
     * @param entityClass class of returned entity
     * @param content entity content
     * @param <T> type parameter for entity type
     *
     * @return entity as managed object or null if no entity could be created
     *
     * @throws JsonException if unable to handle entity content as JSON
     */
    public static <T extends Entity> T saveAsEntity(EntityManager entityManager, Class<T> entityClass, String content) throws JsonException {
        T entity = null;
        try {
            entity = entityClass.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            log.error("Unable to create instance of {} class: {}", entityClass, e);
        }
        if (entity != null) {
            entity.setContent(content);
            entityManager.persist(entity);
        }
        return entity;
    }

    /**
     * Saves new version of entity of given type and existing id with given content
     *
     * @param entityManager entity manager
     * @param entityClass class of returned entity
     * @param id entity identifier
     * @param content entity content
     * @param <T> type parameter for entity type
     *
     * @return entity as managed object or null if no entity could be created
     *
     * @throws JsonException if unable to handle entity content as JSON
     */
    public static <T extends Entity> T saveAsNewVersionOfEntity(EntityManager entityManager, Class<T> entityClass, Long id, String content) throws JsonException {
        T entity = null;
        try {
            entity = entityClass.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            log.error("Unable to create instance of {} class: {}", entityClass, e);
        }
        if (entity != null) {
            entity.setId(id);
            entity.setContent(content);
            entityManager.persist(entity);
        }
        return entity;
    }

    /**
     * Return resource URI by adding id and version path elements extracted from given entity
     * to URI represented by given URI builder
     *
     * @param uriBuilder URI builder
     * @param entity resource entity
     * @param <T> type parameter for entity type
     *
     * @return URI of resource represented by entity
     */
    public static <T extends Entity> URI getResourceUri(UriBuilder uriBuilder, T entity) {
        return uriBuilder.path(String.valueOf(entity.getId()))
                         .path(String.valueOf(entity.getVersion().getTime()))
                         .build();
    }

    /**
     * Returns JSON string representation of Error object with given error message
     *
     * @param errorMessage message given to Error constructor
     *
     * @return JSON string representation of Error object
     *
     * @throws JsonException if unable to create JSON representation
     */
    public static String newErrorAsJson(String errorMessage) throws JsonException {
        return JsonUtil.toJson(new Error(errorMessage));
    }
}

