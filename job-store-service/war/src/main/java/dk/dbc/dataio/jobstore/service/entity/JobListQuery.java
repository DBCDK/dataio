package dk.dbc.dataio.jobstore.service.entity;

import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;
import dk.dbc.dataio.jobstore.service.util.JobInfoSnapshotConverter;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.jobstore.types.criteria.JobListCriteria;
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
public class JobListQuery extends ListQuery<JobListCriteria, JobListCriteria.Field> {
    /* How fragile is this with regards to schema changes for table columns?
     */
    static final String QUERY_BASE = "SELECT * FROM job";

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
        fieldMap.put(JobListCriteria.Field.JOB_ID, new BooleanOpField("id", new ObjectValue()));
        fieldMap.put(JobListCriteria.Field.SPECIFICATION, new VerbatimBooleanOpField("specification", new JsonbValue()));
        fieldMap.put(JobListCriteria.Field.TIME_OF_CREATION, new BooleanOpField("timeOfCreation", new TimestampValue()));
        fieldMap.put(JobListCriteria.Field.TIME_OF_LAST_MODIFICATION, new BooleanOpField("timeOfLastModification", new TimestampValue()));
        fieldMap.put(JobListCriteria.Field.STATE_PROCESSING_FAILED, new VerbatimField("(state->'states'->'PROCESSING'->>'failed' != '0')"));
        fieldMap.put(JobListCriteria.Field.STATE_DELIVERING_FAILED, new VerbatimField("(state->'states'->'DELIVERING'->>'failed' != '0')"));
        fieldMap.put(JobListCriteria.Field.SINK_ID, new BooleanOpField("((flowstorereferences->'references'->'SINK'->>'id')::INT)", new ObjectValue()));
    }

    /**
     * Creates and executes job listing query with given criteria
     * @param criteria query criteria
     * @return list of information snapshots for selected jobs
     * @throws NullPointerException if given null-valued criteria argument
     * @throws PersistenceException if unable to execute query
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
}
