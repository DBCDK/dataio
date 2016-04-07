package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.jobstore.service.entity.JobEntity;
import dk.dbc.dataio.jobstore.test.types.FlowStoreReferenceBuilder;
import dk.dbc.dataio.jobstore.types.FlowStoreReferences;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.jobstore.types.State;
import dk.dbc.dataio.jobstore.types.criteria.JobListCriteria;
import dk.dbc.dataio.jobstore.types.criteria.ListFilter;
import dk.dbc.dataio.jobstore.types.criteria.ListOrderBy;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class PgJobStoreRepositoryIT_QueryingIT extends AbstractJobStoreIT {

    /**
     * Given: a job store containing 3 jobs
     * When : requesting a job count with a criteria selecting a subset of the jobs
     * Then : only the filtered snapshots are counted and orderby/offset is ignored
     */
    @Test
    public void countJobs() {
        // Given...
        final PgJobStoreRepository pgJobStoreRepository = newPgJobStoreRepository();
        final List<JobEntity> jobEntities = Arrays.asList(newPersistedJobEntity(), newPersistedJobEntity(), newPersistedJobEntity());

        // When...
        final JobListCriteria jobListCriteria = new JobListCriteria()
                .where(new ListFilter<>(JobListCriteria.Field.SPECIFICATION, ListFilter.Op.JSON_LEFT_CONTAINS, getJsonValue(jobEntities.get(0))))
                .and(new ListFilter<>(JobListCriteria.Field.JOB_ID, ListFilter.Op.GREATER_THAN, jobEntities.get(0).getId() -1))
                .and(new ListFilter<>(JobListCriteria.Field.JOB_ID, ListFilter.Op.LESS_THAN, jobEntities.get(jobEntities.size() - 1).getId()))
                .orderBy(new ListOrderBy<>(JobListCriteria.Field.JOB_ID, ListOrderBy.Sort.ASC))
                .limit(1).offset(12);

        // When...
        final long numberOfJobs = pgJobStoreRepository.countJobs(jobListCriteria);

        // Then...
        assertThat("number of jobs", numberOfJobs, is(2L));
    }

    /**
     * Given: a job store containing 4 jobs
     * When : requesting a job listing with a criteria selecting a subset of the jobs
     * Then : only the filtered snapshots are returned
     */
    @Test
    public void listJobs() {

        // Given...
        final PgJobStoreRepository pgJobStoreRepository = newPgJobStoreRepository();
        final List<JobEntity> jobEntities = Arrays.asList(
                newPersistedJobEntity(),
                newPersistedJobEntity(),
                newPersistedJobEntity(),
                newPersistedJobEntity());

        final JobListCriteria jobListCriteria = new JobListCriteria()
                .where(new ListFilter<>(JobListCriteria.Field.SPECIFICATION, ListFilter.Op.JSON_LEFT_CONTAINS, getJsonValue(jobEntities.get(0))))
                .and(new ListFilter<>(JobListCriteria.Field.JOB_ID, ListFilter.Op.GREATER_THAN, jobEntities.get(0).getId()))
                .and(new ListFilter<>(JobListCriteria.Field.JOB_ID, ListFilter.Op.LESS_THAN, jobEntities.get(jobEntities.size() - 1).getId()))
                .orderBy(new ListOrderBy<>(JobListCriteria.Field.JOB_ID, ListOrderBy.Sort.ASC));

        // When...
        final List<JobInfoSnapshot> returnedSnapshots = pgJobStoreRepository.listJobs(jobListCriteria);

        // Then...
        assertThat("Number of returned snapshots", returnedSnapshots.size(), is(2));
        assertThat("jobInfoSnapshot[0].jobId", returnedSnapshots.get(0).getJobId(), is(jobEntities.get(1).getId()));
        assertThat("jobInfoSnapshot[1].jobId", returnedSnapshots.get(1).getJobId(), is(jobEntities.get(2).getId()));
    }

    /**
     * Given    : a job store containing 3 jobs, where one has failed during processing and two has failed during delivering
     * When     : requesting a job listing with a criteria selecting only jobs failed in delivering
     * Then     : only jobs failed during delivery are returned, sorted by job ids in descending order.
     */
    @Test
    public void listJobs_withDeliveringFailedCriteria_returnsJobInfoSnapshotsForJobsFailedDuringDelivery() {
        // Given...
        final PgJobStoreRepository pgJobStoreRepository = newPgJobStoreRepository();
        final List<JobEntity> jobEntities = Arrays.asList(
                newPersistedFailedJobEntity(State.Phase.PROCESSING),
                newPersistedFailedJobEntity(State.Phase.DELIVERING),
                newPersistedFailedJobEntity(State.Phase.DELIVERING));

        final JobListCriteria jobListCriteriaDeliveringFailed = new JobListCriteria()
                .where(new ListFilter<>(JobListCriteria.Field.STATE_DELIVERING_FAILED))
                .orderBy(new ListOrderBy<>(JobListCriteria.Field.TIME_OF_CREATION, ListOrderBy.Sort.DESC));

        // When...
        final List<JobInfoSnapshot> returnedSnapshots = pgJobStoreRepository.listJobs(jobListCriteriaDeliveringFailed);

        // Then...
        assertThat("Number of returned snapshots", returnedSnapshots.size(), is(2));
        assertThat("jobInfoSnapshot[0].jobId", returnedSnapshots.get(0).getJobId(), is(jobEntities.get(2).getId()));
        assertThat("jobInfoSnapshot[1].jobId", returnedSnapshots.get(1).getJobId(), is(jobEntities.get(1).getId()));
    }

    /**
     * Given    : a job store containing 3 jobs, where two has failed during processing and one has failed during delivering
     * When     : requesting a job listing with a criteria selecting only jobs failed in processing
     * Then     : only jobs failed during processing are returned, sorted by job ids in descending order.
     */
    @Test
    public void listJobs_withProcessingFailedCriteria_returnsJobInfoSnapshotsForJobsFailedDuringProcessing() {
        // Given...
        final PgJobStoreRepository pgJobStoreRepository = newPgJobStoreRepository();
        final List<JobEntity> jobEntities = Arrays.asList(
                newPersistedFailedJobEntity(State.Phase.PROCESSING),
                newPersistedFailedJobEntity(State.Phase.PROCESSING),
                newPersistedFailedJobEntity(State.Phase.DELIVERING));

        final JobListCriteria jobListCriteriaProcessingFailed = new JobListCriteria()
                .where(new ListFilter<>(JobListCriteria.Field.STATE_PROCESSING_FAILED))
                .orderBy(new ListOrderBy<>(JobListCriteria.Field.TIME_OF_CREATION, ListOrderBy.Sort.DESC));

        // When...
        final List<JobInfoSnapshot> returnedSnapshots = pgJobStoreRepository.listJobs(jobListCriteriaProcessingFailed);

        // Then...
        assertThat("Number of returned snapshots", returnedSnapshots.size(), is(2));
        assertThat("jobInfoSnapshot[0].jobId", returnedSnapshots.get(0).getJobId(), is(jobEntities.get(1).getId()));
        assertThat("jobInfoSnapshot[1].jobId", returnedSnapshots.get(1).getJobId(), is(jobEntities.get(0).getId()));
    }

    /**
     * Given    : a job store containing 3 jobs, where two has failed during partitioning
     *            (only one with fatalError), and one has completed successfully
     * When     : requesting a job listing with a criteria selecting only jobs with fatal errors
     * Then     : only the job with fatal error is returned.
     */
    @Test
    public void listJobs_withFatalErrorCriteria_returnsJobInfoSnapshotsForJobsWithFatalError() {
        // Given...
        final PgJobStoreRepository pgJobStoreRepository = newPgJobStoreRepository();
        final List<JobEntity> jobEntities = Arrays.asList(
                newPersistedFailedJobEntity(State.Phase.PARTITIONING, true),
                newPersistedFailedJobEntity(State.Phase.PARTITIONING),
                newPersistedJobEntity());

        final JobListCriteria jobListCriteriaJobCreationFailed = new JobListCriteria()
                .where(new ListFilter<>(JobListCriteria.Field.WITH_FATAL_ERROR))
                .orderBy(new ListOrderBy<>(JobListCriteria.Field.TIME_OF_CREATION, ListOrderBy.Sort.DESC));

        // When...
        final List<JobInfoSnapshot> returnedSnapshots = pgJobStoreRepository.listJobs(jobListCriteriaJobCreationFailed);

        // Then...
        assertThat("Number of returned snapshots", returnedSnapshots.size(), is(1));
        assertThat("JobInfoSnapshot[0].jobId", returnedSnapshots.get(0).getJobId(), is(jobEntities.get(0).getId()));
    }

    /**
     * Given: a job store containing 3 jobs where two of them are referencing the same sink
     * When : requesting a job listing with a criteria selecting jobs with reference to that specific sink
     * Then : only two filtered snapshot is returned
     */
    @Test
    @SuppressWarnings("unchecked")
    public void listJobs_withSinkIdCriteria_returnsJobInfoSnapshotsForSpecifiedSink() {
        // Given...
        final PgJobStoreRepository pgJobStoreRepository = newPgJobStoreRepository();

        final List<JobEntity> jobEntities = (List<JobEntity>) persistEntities(Arrays.asList(
                newJobEntityWithSinkReference(1L),
                newJobEntityWithSinkReference(1L),
                newJobEntityWithSinkReference(2L)));

        final JobListCriteria jobListCriteria = new JobListCriteria().where(new ListFilter<>(
                JobListCriteria.Field.SINK_ID,
                ListFilter.Op.EQUAL,
                1L));

        // When...
        final List<JobInfoSnapshot> returnedSnapshotsForSink = pgJobStoreRepository.listJobs(jobListCriteria);

        // Then...
        assertThat("Number of returned snapshots", returnedSnapshotsForSink.size(), is(2));
        JobInfoSnapshot jobInfoSnapshot1 = returnedSnapshotsForSink.get(0);
        assertThat("jobInfoSnapshots[0].flowStoreReferences.Element.Sink.id",
                jobInfoSnapshot1.getFlowStoreReferences().getReference(FlowStoreReferences.Elements.SINK).getId(), is(1L));
        assertThat("jobInfoSnapshots[0].jobId", jobInfoSnapshot1.getJobId(), is(jobEntities.get(0).getId()));


        JobInfoSnapshot jobInfoSnapshot2 = returnedSnapshotsForSink.get(1);
        assertThat("jobInfoSnapshots[1].flowStoreReferences.Element.Sink.id",
                jobInfoSnapshot2.getFlowStoreReferences().getReference(FlowStoreReferences.Elements.SINK).getId(), is(1L));
        assertThat("jobInfoSnapshots[1].jobId", jobInfoSnapshot2.getJobId(), is(jobEntities.get(1).getId()));
    }

    /*
     * Private methods
     */


    private JobEntity newPersistedFailedJobEntity(State.Phase failedPhase) {
        return newPersistedFailedJobEntity(failedPhase, false);
    }

    private JobEntity newPersistedFailedJobEntity(State.Phase failedPhase, boolean hasFatalError) {
        final JobEntity jobEntity = newFailedJobEntity(failedPhase, hasFatalError);
        persist(jobEntity);
        return jobEntity;
    }

    private JobEntity newFailedJobEntity(State.Phase failedPhase, boolean hasFatalError) {
        final JobEntity jobEntity = newJobEntity();
        jobEntity.getState().getPhase(failedPhase).setFailed(1);
        jobEntity.setFatalError(hasFatalError);
        return jobEntity;
    }

    private List<?> persistEntities(List<?> entities) {
        entities.forEach(this::persist);
        return entities;
    }

    private JobEntity newJobEntityWithSinkReference(long sinkId) {
        final JobEntity jobEntity = newJobEntity();
        jobEntity.getFlowStoreReferences().setReference(
                FlowStoreReferences.Elements.SINK,
                new FlowStoreReferenceBuilder().setId(sinkId).build());
        return jobEntity;
    }

    private String getJsonValue(JobEntity jobEntity) {
        return createObjectBuilder()
                .add("destination", jobEntity.getSpecification().getDestination())
                .add("type", jobEntity.getSpecification().getType().name())
                .build().toString();
    }
}
