package dk.dbc.dataio.flowstore.ejb;

import dk.dbc.dataio.flowstore.entity.Versioned;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;

public abstract class AbstractResourceBean {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractResourceBean.class);

    protected <T extends Versioned> T saveAsVersionedEntity(EntityManager entityManager, Class<T> entityClass, String content) {
        T entity = null;
        try {
            entity = entityClass.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            LOG.error("Unable to create instance of {} class: {}", entityClass, e);
        }
        if (entity != null) {
            entity.setContent(content);
            entityManager.persist(entity);
        }
        return entity;
    }


    protected <T extends Versioned> URI getResourceUriOfVersionedEntity(UriBuilder uriBuilder, T entity) {
        return uriBuilder.path(String.valueOf(entity.getId())).build();
    }
}
