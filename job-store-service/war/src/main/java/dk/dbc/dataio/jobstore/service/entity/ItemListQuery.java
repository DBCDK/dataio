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

import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;
import dk.dbc.dataio.jobstore.types.criteria.ItemListCriteria;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;
import javax.persistence.Query;
import java.util.List;

/**
 * Item listing ListQuery implementation
 */
public class ItemListQuery extends ListQuery<ItemListCriteria, ItemListCriteria.Field> {

    static final String QUERY_BASE = "SELECT * FROM item";
    static final String QUERY_COUNT_BASE = "SELECT count(*) FROM item";

    private static final Logger LOGGER = LoggerFactory.getLogger(ItemListQuery.class);

    private final EntityManager entityManager;

    /**
     * Constructor
     * @param entityManager EntityManager used for native query creation and execution
     * @throws NullPointerException if given null-valued entityManager argument
     */
    public ItemListQuery(EntityManager entityManager) throws NullPointerException {
        this.entityManager = InvariantUtil.checkNotNullOrThrow(entityManager, "entityManager");
        // Build list of available fields with associated field mappings
        fieldMap.put(ItemListCriteria.Field.JOB_ID, new BooleanOpField("jobId", new ListQuery.NumricValue()));
        fieldMap.put(ItemListCriteria.Field.CHUNK_ID, new BooleanOpField("chunkId", new ListQuery.NumricValue()));
        fieldMap.put(ItemListCriteria.Field.ITEM_ID, new BooleanOpField("id", new ListQuery.NumricValue()));
        fieldMap.put(ItemListCriteria.Field.TIME_OF_CREATION, new BooleanOpField("timeOfCreation", new ListQuery.TimestampValue()));
        fieldMap.put(ItemListCriteria.Field.STATE_FAILED, new VerbatimField("(state->'states'->'PARTITIONING'->>'failed' != '0' OR state->'states'->'PROCESSING'->>'failed' != '0' OR state->'states'->'DELIVERING'->>'failed' != '0')"));
        fieldMap.put(ItemListCriteria.Field.STATE_IGNORED, new VerbatimField("(state->'states'->'PARTITIONING'->>'ignored' != '0' OR state->'states'->'PROCESSING'->>'ignored' != '0' OR state->'states'->'DELIVERING'->>'ignored' != '0')"));
    }

    /**
     * Creates and executes item listing query with given criteria
     * @param criteria query criteria
     * @return list of entities for selected items
     * @throws NullPointerException if given null-valued criteria argument
     * @throws javax.persistence.PersistenceException if unable to flushNotifications query
     */
    @Override
    public List<ItemEntity> execute(ItemListCriteria criteria) throws NullPointerException, PersistenceException {
        final String query = buildQueryString(QUERY_BASE, criteria);
        LOGGER.debug("query = {}", query);
        final Query listItemQuery = entityManager.createNativeQuery(query, ItemEntity.class);
        setParameters(listItemQuery, criteria);
        return listItemQuery.getResultList();
    }

    /**
     * Creates and executes item count query with given criteria
     *
     * @param criteria query criteria
     * @return list of information snapshots for selected items
     * @throws NullPointerException if given null-valued criteria argument
     * @throws PersistenceException if unable to flushNotifications query
     */
    public long execute_count(ItemListCriteria criteria) throws NullPointerException, PersistenceException {
        final String query = buildCountQueryString(QUERY_COUNT_BASE, criteria);
        LOGGER.debug("query = {}", query);
        final Query listItemQuery = entityManager.createNativeQuery(query);
        setParameters(listItemQuery, criteria);
        final Long items = (Long)listItemQuery.getSingleResult();
        return items;
    }
}
