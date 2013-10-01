package dk.dbc.dataio.flowstore.util;

import dk.dbc.dataio.commons.utils.json.JsonException;
import dk.dbc.dataio.flowstore.entity.VersionedEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;

/**
 * Utility class for implementation of RESTful service methods
 */
public class ServiceUtil {
    private static final Logger log = LoggerFactory.getLogger(ServiceUtil.class);

    private ServiceUtil() { }

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
}

