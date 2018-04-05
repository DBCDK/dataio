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
import dk.dbc.dataio.jobstore.service.util.JobInfoSnapshotConverter;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.jobstore.types.criteria.JobListCriteria;
import dk.dbc.dataio.querylanguage.DataIOQLParser;
import dk.dbc.dataio.querylanguage.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;
import javax.persistence.Query;
import java.util.ArrayList;
import java.util.List;

/**
 * Job listing ListQuery implementation
 */
public class JobListQuery extends ListQuery<JobListCriteria, JobListCriteria.Field, JobInfoSnapshot> {
    public List<JobInfoSnapshot> execute(String query) throws IllegalArgumentException {
        final DataIOQLParser dataIOQLParser = new DataIOQLParser();
        try {
            final String sql = dataIOQLParser.parse(query);
            final Query q = entityManager.createNativeQuery(sql, JobEntity.class);
            final List<JobEntity> jobs = q.getResultList();
            final List<JobInfoSnapshot> jobInfoSnapshots = new ArrayList<>(jobs.size());
            for (JobEntity jobEntity : jobs) {
                jobInfoSnapshots.add(JobInfoSnapshotConverter.toJobInfoSnapshot(jobEntity));
            }
            return jobInfoSnapshots;
        } catch (ParseException e) {
            throw new IllegalArgumentException("Unable to parse '" + query + "'", e);
        }
    }

    public long count(String query) throws IllegalArgumentException {
        final DataIOQLParser dataIOQLParser = new DataIOQLParser();
        try {
            final String sql = dataIOQLParser.parse("COUNT " + query);
            final Query q = entityManager.createNativeQuery(sql);
            return (long) q.getSingleResult();
        } catch (ParseException e) {
            throw new IllegalArgumentException("Unable to parse '" + query + "'", e);
        }
    }

    /* !!! DEPRECATION WARNING !!!

        Future enhancements should NOT use the Criteria based API
        but work towards using the IO query language instead.

        Below code is therefore considered deprecated.
     */

    static final String QUERY_BASE = "SELECT * FROM job";
    static final String QUERY_COUNT_BASE = "SELECT count(*) FROM job";

    private static final Logger LOGGER = LoggerFactory.getLogger(JobListQuery.class);

    private final EntityManager entityManager;

    /**
     * Constructor
     * @param entityManager EntityManager used for native query creation and execution
     * @throws NullPointerException if given null-valued entityManager argument
     */
    public JobListQuery(EntityManager entityManager) throws NullPointerException {
        this.entityManager = InvariantUtil.checkNotNullOrThrow(entityManager, "entityManager");
        // Build list of available fields with associated field mappings
        fieldMap.put(JobListCriteria.Field.JOB_ID, new BooleanOpField("id", new NumericValue()));
        fieldMap.put(JobListCriteria.Field.SPECIFICATION, new VerbatimBooleanOpField("specification", new JsonbValue()));
        fieldMap.put(JobListCriteria.Field.TIME_OF_CREATION, new BooleanOpField("timeOfCreation", new TimestampValue()));
        fieldMap.put(JobListCriteria.Field.TIME_OF_LAST_MODIFICATION, new BooleanOpField("timeOfLastModification", new TimestampValue()));
        fieldMap.put(JobListCriteria.Field.TIME_OF_COMPLETION, new BooleanOpField("timeOfCompletion", new TimestampValue()));
        fieldMap.put(JobListCriteria.Field.STATE_PROCESSING_FAILED, new VerbatimField("(state->'states'->'PROCESSING'->>'failed' != '0')"));
        fieldMap.put(JobListCriteria.Field.STATE_DELIVERING_FAILED, new VerbatimField("(state->'states'->'DELIVERING'->>'failed' != '0')"));
        fieldMap.put(JobListCriteria.Field.SINK_ID, new BooleanOpField("((flowstorereferences->'references'->'SINK'->>'id')::INT)", new NumericValue()));
        fieldMap.put(JobListCriteria.Field.JOB_CREATION_FAILED, new VerbatimField("fatalerror = 't' or (state->'states'->'PARTITIONING'->>'failed' != '0')"));
        fieldMap.put(JobListCriteria.Field.RECORD_ID, new BooleanOpField("id", new SubSelectJsonValue("jobid", "item", "recordinfo", "id")));
        fieldMap.put(JobListCriteria.Field.PREVIEW_ONLY, new VerbatimField("numberofchunks = '0' and numberofitems != '0'"));
    }

    /**
     * Creates and executes job listing query with given criteria
     * @param criteria query criteria
     * @return list of information snapshots for selected jobs
     * @throws NullPointerException if given null-valued criteria argument
     * @throws PersistenceException if unable to flushNotifications query
     */
    @Override
    public List<JobInfoSnapshot> execute(JobListCriteria criteria) throws NullPointerException, PersistenceException {
        final String query = buildQueryString(QUERY_BASE, criteria);
        LOGGER.debug("query = {}", query);
        final Query listJobQuery = entityManager.createNativeQuery(query, JobEntity.class);
        setParameters(listJobQuery, criteria);

        /* We can not utilise @SqlResultSetMapping to map directly to JobInfoSnapshot
           since we have no way to convert our complex JSON types into their corresponding POJOs */

        final List<JobEntity> jobs = listJobQuery.getResultList();
        final List<JobInfoSnapshot> jobInfoSnapshots = new ArrayList<>(jobs.size());
        for (JobEntity jobEntity : jobs) {
            jobInfoSnapshots.add(JobInfoSnapshotConverter.toJobInfoSnapshot(jobEntity));
        }
        return jobInfoSnapshots;
    }

    /**
     * Creates and executes job count query with given criteria
     *
     * @param criteria query criteria
     * @return list of information snapshots for selected jobs
     * @throws NullPointerException if given null-valued criteria argument
     * @throws PersistenceException if unable to flushNotifications query
     */
    public long execute_count(JobListCriteria criteria) throws NullPointerException, PersistenceException {
        final String query = buildCountQueryString(QUERY_COUNT_BASE, criteria);
        LOGGER.debug("query = {}", query);
        final Query listJobQuery = entityManager.createNativeQuery(query);
        setParameters(listJobQuery, criteria);

        /* We can not utilise @SqlResultSetMapping to map directly to JobInfoSnapshot
           since we have no way to convert our complex JSON types into their corresponding POJOs */

        final Long jobs = (Long) listJobQuery.getSingleResult();
        return jobs;
    }
}
