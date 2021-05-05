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

import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.flowstore.entity.Versioned;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;

/**
 * This class is deprecated and should not be used by new beans.
 */
public abstract class AbstractResourceBean {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractResourceBean.class);

    protected <T extends Versioned> T saveAsVersionedEntity(EntityManager entitymanager, Class<T> entityClass, String content)
            throws JSONBException {
        T entity = null;
        try {
            entity = entityClass.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            LOG.error("Unable to create instance of {} class: {}", entityClass, e);
        }
        if (entity != null) {
            entity.setContent(content);
            entitymanager.persist(entity);
            entitymanager.flush();
        }
        return entity;
    }


    protected <T extends Versioned> URI getResourceUriOfVersionedEntity(UriBuilder uriBuilder, T entity) {
        return uriBuilder.path(String.valueOf(entity.getId())).build();
    }
}
