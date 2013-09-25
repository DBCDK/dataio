package dk.dbc.dataio.flowstore.util;

import dk.dbc.dataio.commons.utils.json.JsonException;
import dk.dbc.dataio.commons.utils.json.JsonUtil;
import dk.dbc.dataio.flowstore.entity.VersionedEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;

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
    public static <T extends VersionedEntity> T saveAsVersionedEntity(EntityManager entityManager, Class<T> entityClass, String content) throws JsonException {
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
     * Return resource URI by adding id path element extracted from given entity
     * to URI represented by given URI builder
     *
     * @param uriBuilder URI builder
     * @param entity resource entity
     * @param <T> type parameter for entity type
     *
     * @return URI of resource represented by entity
     */
    public static <T extends VersionedEntity> URI getResourceUriOfVersionedEntity(UriBuilder uriBuilder, T entity) {
        return uriBuilder.path(String.valueOf(entity.getId())).build();
    }

    /**
     * Returns JSON string representation of dk.dbc.dataio.flowstore.entity.Error object
     * constructed from given error message
     *
     * @param errorMessage error message
     *
     * @return JSON string representation of Error object
     */
    public static String asJsonError(String errorMessage) {
        String error = null;
        try {
            error = JsonUtil.toJson(new dk.dbc.dataio.flowstore.entity.Error(errorMessage));
        } catch (JsonException e) {
            log.error("Caught exception trying to create JSON representation of error", e);
        }
        return error;
    }

    /**
     * Returns JSON string representation of dk.dbc.dataio.flowstore.entity.Error object
     * constructed from given exception
     *
     * @param ex exception to wrap
     *
     * @return JSON string representation of Error object
     */
    public static String asJsonError(Exception ex) {
        String error = null;
        try {
            log.error("Generating error based on exception", ex);
            error = JsonUtil.toJson(new dk.dbc.dataio.flowstore.entity.Error(ex.getMessage(), stackTraceToString(ex)));
        } catch (JsonException e) {
            log.error("Caught exception trying to create JSON representation of error", e);
        }
        return error;
    }

    public static String stackTraceToString(Throwable t) {
        final StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }
}

