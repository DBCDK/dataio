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

package dk.dbc.dataio.jobstore.service.entity;

import dk.dbc.invariant.InvariantUtil;
import dk.dbc.dataio.jobstore.types.criteria.ChunkListCriteria;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;
import javax.persistence.Query;
import java.util.List;

/**
 * Chunk listing ListQuery implementation
 */
public class ChunkListQuery extends ListQuery<ChunkListCriteria, ChunkListCriteria.Field, ChunkEntity> {

    static final String QUERY_BASE = "SELECT * FROM chunk";

    private static final Logger LOGGER = LoggerFactory.getLogger(ChunkListQuery.class);

    private final EntityManager entityManager;

    /**
     * Constructor
     * @param entityManager EntityManager used for native query creation and execution
     * @throws NullPointerException if given null-valued entityManager argument
     */
    public ChunkListQuery(EntityManager entityManager) throws NullPointerException {
        this.entityManager = InvariantUtil.checkNotNullOrThrow(entityManager, "entityManager");
        // Build list of available fields with associated field mappings
        fieldMap.put(ChunkListCriteria.Field.TIME_OF_CREATION, new BooleanOpField("timeOfCreation", new TimestampValue()));
        fieldMap.put(ChunkListCriteria.Field.TIME_OF_COMPLETION, new BooleanOpField("timeOfCompletion", new TimestampValue()));
    }

    /**
     * Creates and executes chunk listing query with given criteria
     * @param criteria query criteria
     * @return list of entities for selected chunks
     * @throws NullPointerException if given null-valued criteria argument
     * @throws javax.persistence.PersistenceException if unable to flushNotifications query
     */
    @Override
    public List<ChunkEntity> execute(ChunkListCriteria criteria) throws NullPointerException, PersistenceException {
        final String query = buildQueryString(QUERY_BASE, criteria);
        LOGGER.debug("query = {}", query);
        final Query listChunkQuery = entityManager.createNativeQuery(query, ChunkEntity.class);
        setParameters(listChunkQuery, criteria);
        return listChunkQuery.getResultList();
    }
}
