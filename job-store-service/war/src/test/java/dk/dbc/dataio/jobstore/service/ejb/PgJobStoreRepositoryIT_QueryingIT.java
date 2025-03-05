package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.jobstore.service.entity.ChunkEntity;
import dk.dbc.dataio.jobstore.service.entity.ItemEntity;
import dk.dbc.dataio.jobstore.service.entity.JobEntity;
import dk.dbc.dataio.jobstore.test.types.FlowStoreReferenceBuilder;
import dk.dbc.dataio.jobstore.types.FlowStoreReferences;
import dk.dbc.dataio.jobstore.types.ItemInfoSnapshot;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.jobstore.types.RecordInfo;
import dk.dbc.dataio.jobstore.types.State;
import dk.dbc.dataio.jobstore.types.criteria.ItemListCriteria;
import dk.dbc.dataio.jobstore.types.criteria.JobListCriteria;
import dk.dbc.dataio.jobstore.types.criteria.ListFilter;
import dk.dbc.dataio.jobstore.types.criteria.ListOrderBy;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class PgJobStoreRepositoryIT_QueryingIT extends PgJobStoreRepositoryAbstractIT {

    /**
     * Given: a job store containing three jobs
     * When : requesting a job count with a criteria selecting a subset of the jobs
     * Then : only the filtered snapshots are counted and orderby/offset is ignored
     */
    @org.junit.Test
    public void countJobs() {
        // Given...
        final List<JobEntity> jobEntities = Arrays.asList(newPersistedJobEntity(), newPersistedJobEntity(), newPersistedJobEntity());

        // When...
        final JobListCriteria jobListCriteria = new JobListCriteria()
                .where(new ListFilter<>(JobListCriteria.Field.SPECIFICATION, ListFilter.Op.JSON_LEFT_CONTAINS, getJsonValue(jobEntities.get(0))))
                .and(new ListFilter<>(JobListCriteria.Field.JOB_ID, ListFilter.Op.GREATER_THAN, jobEntities.get(0).getId() - 1))
                .and(new ListFilter<>(JobListCriteria.Field.JOB_ID, ListFilter.Op.LESS_THAN, jobEntities.get(jobEntities.size() - 1).getId()))
                .orderBy(new ListOrderBy<>(JobListCriteria.Field.JOB_ID, ListOrderBy.Sort.ASC))
                .limit(1).offset(12);

        // When...
        final long numberOfJobs = pgJobStoreRepository.countJobs(jobListCriteria);

        // Then...
        assertThat("number of jobs", numberOfJobs, is(2L));
    }

    /**
     * Given: a job store containing four jobs
     * When : requesting a job listing with a criteria selecting a subset of the jobs
     * Then : only the filtered snapshots are returned
     */
    @org.junit.Test
    public void listJobs() {
        // Given...
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
     * Given    : a job store containing three jobs, where one has failed during processing and two has failed during delivering
     * When     : requesting a job listing with a criteria selecting only jobs failed in delivering
     * Then     : only jobs failed during delivery are returned, sorted by job ids in descending order.
     */
    @org.junit.Test
    public void listJobs_withDeliveringFailedCriteria_returnsJobInfoSnapshotsForJobsFailedDuringDelivery() {
        // Given...
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
     * Given    : a job store containing three jobs where:
     * one has not yet completed, one has failed during processing, one has completed without failure.
     * When     : requesting a job listing with a criteria selecting only jobs that has not yet completed
     * Then     : only jobs that has not yet completed are returned.
     */
    @org.junit.Test
    public void listJobs_withOutTimeOfCompletionCriteria_returnsJobInfoSnapshotsForJobsWithoutTimeOfCompletion() {
        // Given...
        final List<JobEntity> jobEntities = Arrays.asList(
                newPersistedJobEntityWithTimeOfCompletion(),
                newPersistedFailedJobEntity(State.Phase.PROCESSING),
                newPersistedJobEntity());

        final JobListCriteria jobListCriteriaWithOutTimeOfCompletion = new JobListCriteria()
                .where(new ListFilter<>(JobListCriteria.Field.TIME_OF_COMPLETION, ListFilter.Op.IS_NULL));

        // When...
        final List<JobInfoSnapshot> returnedSnapshots = pgJobStoreRepository.listJobs(jobListCriteriaWithOutTimeOfCompletion);

        // Then...
        assertThat("Number of returned snapshots", returnedSnapshots.size(), is(1));
        assertThat("jobInfoSnapshot[0].jobId", returnedSnapshots.get(0).getJobId(), is(jobEntities.get(2).getId()));
    }

    /**
     * Given    : a job store containing three jobs, where two has failed during processing and one has failed during delivering
     * When     : requesting a job listing with a criteria selecting only jobs failed in processing
     * Then     : only jobs failed during processing are returned, sorted by job ids in descending order.
     */
    @org.junit.Test
    public void listJobs_withProcessingFailedCriteria_returnsJobInfoSnapshotsForJobsFailedDuringProcessing() {
        // Given...
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
     * Given    : a job store containing three jobs, where two has failed during partitioning
     * (only one with fatalError), and one has completed successfully
     * When     : requesting a job listing with a criteria selecting only jobs with fatal errors
     * Then     : only the job with fatal error is returned.
     */
    @org.junit.Test
    public void listJobs_withFatalErrorCriteria_returnsJobInfoSnapshotsForJobsWithFatalError() {
        // Given...
        final List<JobEntity> jobEntities = Arrays.asList(
                newPersistedFailedJobEntity(State.Phase.PARTITIONING, true),
                newPersistedFailedJobEntity(State.Phase.PROCESSING),
                newPersistedJobEntity());

        final JobListCriteria jobListCriteriaJobCreationFailed = new JobListCriteria()
                .where(new ListFilter<>(JobListCriteria.Field.JOB_CREATION_FAILED))
                .orderBy(new ListOrderBy<>(JobListCriteria.Field.TIME_OF_CREATION, ListOrderBy.Sort.DESC));

        // When...
        final List<JobInfoSnapshot> returnedSnapshots = pgJobStoreRepository.listJobs(jobListCriteriaJobCreationFailed);

        // Then...
        assertThat("Number of returned snapshots", returnedSnapshots.size(), is(1));
        assertThat("JobInfoSnapshot[0].jobId", returnedSnapshots.get(0).getJobId(), is(jobEntities.get(0).getId()));
    }

    /**
     * Given: a job store containing three jobs where two of them are referencing the same sink
     * When : requesting a job listing with a criteria selecting jobs with reference to that specific sink
     * Then : only two filtered snapshot is returned
     */
    @org.junit.Test
    public void listJobs_withSinkIdCriteria_returnsJobInfoSnapshotsForSelectedSink() {
        // Given...
        JobEntity jobEntity1 = newPersistedJobEntityWithSinkReference(1L);
        JobEntity jobEntity2 = newPersistedJobEntityWithSinkReference(1L);
        newPersistedJobEntityWithSinkReference(2L);

        final JobListCriteria jobListCriteria = new JobListCriteria().where(new ListFilter<>(
                        JobListCriteria.Field.SINK_ID, ListFilter.Op.EQUAL, 1L))
                .orderBy(new ListOrderBy<>(JobListCriteria.Field.JOB_ID, ListOrderBy.Sort.ASC));

        // When...
        final List<JobInfoSnapshot> returnedSnapshotsForSink = pgJobStoreRepository.listJobs(jobListCriteria);

        // Then...
        assertThat("Number of returned snapshots", returnedSnapshotsForSink.size(), is(2));
        JobInfoSnapshot jobInfoSnapshot1 = returnedSnapshotsForSink.get(0);
        assertThat("jobInfoSnapshots[0].flowStoreReferences.Element.Sink.id",
                jobInfoSnapshot1.getFlowStoreReferences().getReference(FlowStoreReferences.Elements.SINK).getId(), is(1L));
        assertThat("jobInfoSnapshots[0].jobId", jobInfoSnapshot1.getJobId(), is(jobEntity1.getId()));


        JobInfoSnapshot jobInfoSnapshot2 = returnedSnapshotsForSink.get(1);
        assertThat("jobInfoSnapshots[1].flowStoreReferences.Element.Sink.id",
                jobInfoSnapshot2.getFlowStoreReferences().getReference(FlowStoreReferences.Elements.SINK).getId(), is(1L));
        assertThat("jobInfoSnapshots[1].jobId", jobInfoSnapshot2.getJobId(), is(jobEntity2.getId()));
    }

    /**
     * Given: a job store containing two jobs
     * When : requesting an item count with a criteria selecting items from the selected job
     * Then : only the filtered snapshots from the specific job are counted and orderby/offset is ignored
     */
    @org.junit.Test
    public void countItems() {
        // Given...
        generateChunkAndItemEntitiesForJob(newPersistedJobEntity().getId());
        final int selectedJobId = newPersistedJobEntity().getId();
        final List<ItemEntity> expectedItemEntities = generateChunkAndItemEntitiesForJob(selectedJobId);

        final ItemListCriteria itemListCriteria = new ItemListCriteria()
                .where(new ListFilter<>(ItemListCriteria.Field.JOB_ID, ListFilter.Op.EQUAL, selectedJobId))
                .orderBy(new ListOrderBy<>(ItemListCriteria.Field.CHUNK_ID, ListOrderBy.Sort.ASC))
                .orderBy(new ListOrderBy<>(ItemListCriteria.Field.ITEM_ID, ListOrderBy.Sort.ASC))
                .limit(1).offset(6);

        // When...
        final long count = pgJobStoreRepository.countItems(itemListCriteria);

        // Then...
        assertThat("item count returned", count, is((long) expectedItemEntities.size()));
    }

    /**
     * Given: a job store containing one job
     * When : requesting items with specified record id from the selected job
     * Then : only the expected snapshot is returned
     */
    @org.junit.Test
    public void listItems_withRecordIdCriteria_returnsItemInfoSnapshotsForSelectedJob() throws JSONBException {
        // Given...
        final int selectedJobId = newPersistedJobEntity().getId();
        final String recordId = "00012345";

        // chunk entity
        ChunkEntity chunkEntity = newPersistedChunkEntity(new ChunkEntity.Key(0, selectedJobId));

        // first item entity for first chunk
        final ItemEntity first = newPersistedFailedItemEntity(new ItemEntity.Key(selectedJobId, chunkEntity.getKey().getId(), (short) 0));
        first.withRecordInfo(new RecordInfo(recordId));
        persistAndRefresh(first);

        // second item entity for first chunk
        final ItemEntity second = newPersistedFailedItemEntity(new ItemEntity.Key(selectedJobId, chunkEntity.getKey().getId(), (short) 1));
        second.withRecordInfo(new RecordInfo("123345"));
        persistAndRefresh(second);

        final ItemListCriteria itemListCriteria = new ItemListCriteria()
                .where(new ListFilter<>(ItemListCriteria.Field.JOB_ID, ListFilter.Op.EQUAL, selectedJobId))
                .and(new ListFilter<>(ItemListCriteria.Field.RECORD_ID, ListFilter.Op.EQUAL, recordId));

        // When...
        final List<ItemInfoSnapshot> itemInfoSnapshots = pgJobStoreRepository.listItems(itemListCriteria);

        // Then...
        assertThat(itemInfoSnapshots.size(), is(1));
        assertThat(itemInfoSnapshots.get(0).getJobId(), is(selectedJobId));
        assertThat(itemInfoSnapshots.get(0).getRecordInfo().getId(), is(recordId));
    }

    /**
     * Given   : a job store containing two jobs, each with two chunks containing one successful item, three failed items and one ignored item
     * When    : requesting an item listing with a criteria selecting failed items from the selected job
     * Then    : the expected filtered snapshots are returned, sorted by chunk id ASC > item id ASC.
     */
    @org.junit.Test
    public void listItems_withFailedItemsCriteria_returnsItemInfoSnapshotsForSelectedJob() {
        // Given...
        generateChunkAndItemEntitiesForJob(newPersistedJobEntity().getId());
        final int selectedJobId = newPersistedJobEntity().getId();
        generateChunkAndItemEntitiesForJob(selectedJobId);

        final ItemListCriteria findAllItemsForJobWithStatusFailed = new ItemListCriteria()
                .where(new ListFilter<>(ItemListCriteria.Field.JOB_ID, ListFilter.Op.EQUAL, selectedJobId))
                .and(new ListFilter<>(ItemListCriteria.Field.STATE_FAILED))
                .orderBy(new ListOrderBy<>(ItemListCriteria.Field.CHUNK_ID, ListOrderBy.Sort.ASC))
                .orderBy(new ListOrderBy<>(ItemListCriteria.Field.ITEM_ID, ListOrderBy.Sort.ASC));

        // When...
        final List<ItemInfoSnapshot> returnedItemInfoSnapshots = pgJobStoreRepository.listItems(findAllItemsForJobWithStatusFailed);

        // Then...
        assertThat("Number of returned snapshots", returnedItemInfoSnapshots.size(), is(3));

        assertAscendingChunkAndItemSorting(returnedItemInfoSnapshots);
        for (ItemInfoSnapshot itemInfoSnapshot : returnedItemInfoSnapshots) {
            assertThat("itemInfoSnapshot.jobId", itemInfoSnapshot.getJobId(), is(selectedJobId));
            assertThat("itemInfoSnapshot.State.Phase.failed", itemInfoSnapshot.getState().getPhase(State.Phase.PROCESSING).getFailed(), is(1));
        }
    }

    /**
     * Given   : a job store containing two jobs, each with two chunks containing one successful item, three failed items and one ignored item
     * When    : requesting an item listing with a criteria selecting ignored items from the selected job
     * Then    : the expected filtered snapshot is returned.
     */
    @org.junit.Test
    public void listItems_withIgnoredItemsCriteria_returnsItemInfoSnapshotsForSelectedJob() {
        // Given...
        generateChunkAndItemEntitiesForJob(newPersistedJobEntity().getId());
        final int selectedJobId = newPersistedJobEntity().getId();
        generateChunkAndItemEntitiesForJob(selectedJobId);

        final ItemListCriteria findAllItemsForJobWithStatusIgnored = new ItemListCriteria()
                .where(new ListFilter<>(ItemListCriteria.Field.JOB_ID, ListFilter.Op.EQUAL, selectedJobId))
                .and(new ListFilter<>(ItemListCriteria.Field.STATE_IGNORED));

        // When...
        final List<ItemInfoSnapshot> returnedItemInfoSnapshots = pgJobStoreRepository.listItems(findAllItemsForJobWithStatusIgnored);

        // Then...
        assertThat("Number of returned snapshots", returnedItemInfoSnapshots.size(), is(1));
        final ItemInfoSnapshot itemInfoSnapshot = returnedItemInfoSnapshots.get(0);
        assertThat("itemInfoSnapshot.jobId", itemInfoSnapshot.getJobId(), is(selectedJobId));
        assertThat("itemInfoSnapshot.State.Phase.ignored", itemInfoSnapshot.getState().getPhase(State.Phase.PROCESSING).getIgnored(), is(1));
    }

    /**
     * Given   : a job store containing two jobs
     * When    : requesting an item listing with a criteria selecting all items from the selected job
     * Then    : the expected filtered snapshots are returned
     */
    @org.junit.Test
    public void listItems_withoutItemCriteria_returnsItemInfoSnapshotsForSelectedJob() {
        // Given...
        generateChunkAndItemEntitiesForJob(newPersistedJobEntity().getId());
        final int selectedJobId = newPersistedJobEntity().getId();
        final List<ItemEntity> expectedItemEntities = generateChunkAndItemEntitiesForJob(selectedJobId);

        final ItemListCriteria itemListCriteria = new ItemListCriteria()
                .where(new ListFilter<>(ItemListCriteria.Field.JOB_ID, ListFilter.Op.EQUAL, selectedJobId))
                .orderBy(new ListOrderBy<>(ItemListCriteria.Field.CHUNK_ID, ListOrderBy.Sort.ASC))
                .orderBy(new ListOrderBy<>(ItemListCriteria.Field.ITEM_ID, ListOrderBy.Sort.ASC));

        // When...
        final List<ItemInfoSnapshot> returnedItemInfoSnapshots = pgJobStoreRepository.listItems(itemListCriteria);

        // Then
        assertThat("Number of returned items", returnedItemInfoSnapshots.size(), is(expectedItemEntities.size()));
        assertAscendingChunkAndItemSorting(returnedItemInfoSnapshots);
        for (ItemInfoSnapshot itemInfoSnapshot : returnedItemInfoSnapshots) {
            assertThat("itemInfoSnapshot.jobId", itemInfoSnapshot.getJobId(), is(selectedJobId));
        }
    }

    /**
     * Given    : a job store containing three jobs, where two are preview only and one has failed
     * When     : requesting a job listing with a criteria selecting preview only jobs
     * Then     : preview only jobs arer returned sorted by job ids in descending order.
     */
    @org.junit.Test
    public void listJobs_withPreviewOnlyCriteria_returnsJobInfoSnapshotsForJobPreviewsOnly() {
        // Given...
        final JobEntity successfulPreview = newPersistedJobEntityWithTimeOfCompletion();
        successfulPreview.setNumberOfChunks(0);
        successfulPreview.setNumberOfItems(10);

        final JobEntity failedPreview = newPersistedJobEntityWithTimeOfCompletion();
        failedPreview.setNumberOfChunks(0);
        failedPreview.setNumberOfItems(2);
        failedPreview.setFatalError(true);

        final List<JobEntity> jobEntities = Arrays.asList(
                successfulPreview,
                newPersistedJobEntityWithTimeOfCompletion(),
                failedPreview);

        final JobListCriteria jobListCriteriaJobPreview = new JobListCriteria()
                .where(new ListFilter<>(JobListCriteria.Field.PREVIEW_ONLY))
                .orderBy(new ListOrderBy<>(JobListCriteria.Field.TIME_OF_CREATION, ListOrderBy.Sort.DESC));

        // When...
        final List<JobInfoSnapshot> returnedSnapshots = pgJobStoreRepository.listJobs(jobListCriteriaJobPreview);

        // Then...
        assertThat("Number of returned snapshots", returnedSnapshots.size(), is(2));
        assertThat("jobInfoSnapshot[0].jobId", returnedSnapshots.get(0).getJobId(), is(jobEntities.get(2).getId()));
        assertThat("jobInfoSnapshot[1].jobId", returnedSnapshots.get(1).getJobId(), is(jobEntities.get(0).getId()));
    }

    /**
     * Given    : a job store containing 2 jobs
     * When     : requesting a job listing with a criteria selecting only jobs with a creation time earlier than given date
     * Then     : only the job with a creation time earlier than requested is returned.
     */
    @org.junit.Test
    public void listJobs_fromBeforeDateCriterea_returnsJobInfoSnapshotsForJobsWithCreationDateBeforeDate() {
        final JobEntity old = newPersistedJobEntity();
        old.setTimeOfCompletion(new Timestamp(Instant.now().minusSeconds(1).toEpochMilli()));
        persist(old);

        final Date marker = new Date();

        final JobEntity recent = newPersistedJobEntity();
        recent.setTimeOfCompletion(new Timestamp(System.currentTimeMillis()));
        persist(recent);

        final JobListCriteria criteria = new JobListCriteria()
                .where(new ListFilter<>(JobListCriteria.Field.SPECIFICATION, ListFilter.Op.JSON_LEFT_CONTAINS, "{ \"type\": \"" + JobSpecification.Type.TEST.name() + "\"}"))
                .and(new ListFilter<>(JobListCriteria.Field.TIME_OF_COMPLETION, ListFilter.Op.IS_NOT_NULL))
                .and(new ListFilter<>(JobListCriteria.Field.TIME_OF_CREATION, ListFilter.Op.LESS_THAN, marker));

        final List<JobInfoSnapshot> jobInfoSnapshots = pgJobStoreRepository.listJobs(criteria);
        assertThat(jobInfoSnapshots.size(), is(1));
        assertThat(jobInfoSnapshots.get(0).getJobId(), is(old.getId()));
    }

    /**
     * Given    : a job store containing 2 jobs
     * When     : requesting a job listing with a criteria selecting only jobs with a specific data file
     * Then     : only jobs with the specific data file are returned.
     */
    @org.junit.Test
    public void listJobs_withDataFile_returnsJobInfoSnapshotsForJobsWithDataFile() {
        final JobEntity jobEntity = newPersistedJobEntity();
        jobEntity.setSpecification(new JobSpecification().withDataFile("requestedDataFile"));
        persist(jobEntity);
        newPersistedJobEntity();
        final JobListCriteria criteria = new JobListCriteria()
                .where(new ListFilter<>(JobListCriteria.Field.SPECIFICATION, ListFilter.Op.JSON_LEFT_CONTAINS, "{ \"dataFile\": \"" + jobEntity.getSpecification().getDataFile() + "\"}"));
        assertThat(pgJobStoreRepository.countJobs(criteria), is(1L));
    }

    // ************************** JobEntity creation **************************
    private JobEntity newPersistedJobEntityWithTimeOfCompletion() {
        JobEntity jobEntity = newJobEntity();
        jobEntity.setTimeOfCompletion(new Timestamp(System.currentTimeMillis()));
        persist(jobEntity);
        return jobEntity;
    }

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
        jobEntity.getState().getPhase(failedPhase).withFailed(1);
        jobEntity.setFatalError(hasFatalError);
        jobEntity.setTimeOfCompletion(new Timestamp(System.currentTimeMillis()));
        return jobEntity;
    }

    private JobEntity newPersistedJobEntityWithSinkReference(long sinkId) {
        final JobEntity jobEntity = newJobEntityWithSinkReference(sinkId);
        persist(jobEntity);
        return jobEntity;
    }

    private JobEntity newJobEntityWithSinkReference(long sinkId) {
        final JobEntity jobEntity = newJobEntity();
        jobEntity.getFlowStoreReferences().setReference(
                FlowStoreReferences.Elements.SINK,
                new FlowStoreReferenceBuilder().setId(sinkId).build());
        return jobEntity;
    }

    // ************************* ChunkEntity creation **************************

    private ChunkEntity newPersistedAndRefreshedChunkEntity(ChunkEntity.Key key) {
        final ChunkEntity chunkEntity = newChunkEntity(key);
        persistAndRefresh(chunkEntity);
        return chunkEntity;
    }

    // ************************** ItemEntity creation **************************

    private ItemEntity newPersistedFailedItemEntity(ItemEntity.Key key) {
        final ItemEntity itemEntity = newFailedItemEntity(key);
        persist(itemEntity);
        return itemEntity;
    }

    private ItemEntity newFailedItemEntity(ItemEntity.Key key) {
        final ItemEntity itemEntity = newItemEntity(key);
        itemEntity.getState().getPhase(State.Phase.PROCESSING).withFailed(1);
        return itemEntity;
    }

    private ItemEntity newPersistedIgnoredItemEntity(ItemEntity.Key key) {
        final ItemEntity itemEntity = newIgnoredItemEntity(key);
        persist(itemEntity);
        return itemEntity;
    }

    private ItemEntity newIgnoredItemEntity(ItemEntity.Key key) {
        final ItemEntity itemEntity = newItemEntity(key);
        itemEntity.getState().getPhase(State.Phase.PROCESSING).withIgnored(1);
        return itemEntity;
    }

    // **************************** Helper methods *****************************

    private List<ItemEntity> generateChunkAndItemEntitiesForJob(int jobId) {
        List<ItemEntity> itemEntities = new ArrayList<>();

        // chunk entities
        ChunkEntity chunkEntity1 = newPersistedChunkEntity(new ChunkEntity.Key(0, jobId));
        ChunkEntity chunkEntity2 = newPersistedChunkEntity(new ChunkEntity.Key(1, jobId));

        // item entities for first chunk
        itemEntities.add(newPersistedFailedItemEntity(new ItemEntity.Key(jobId, chunkEntity1.getKey().getId(), (short) 0)));
        itemEntities.add(newPersistedIgnoredItemEntity(new ItemEntity.Key(jobId, chunkEntity1.getKey().getId(), (short) 1)));
        itemEntities.add(newPersistedItemEntity(new ItemEntity.Key(jobId, chunkEntity1.getKey().getId(), (short) 2)));
        itemEntities.add(newPersistedFailedItemEntity(new ItemEntity.Key(jobId, chunkEntity1.getKey().getId(), (short) 3)));

        // item entities for second chunk
        itemEntities.add(newPersistedFailedItemEntity(new ItemEntity.Key(jobId, chunkEntity2.getKey().getId(), (short) 0)));

        return itemEntities;
    }

    private void assertAscendingChunkAndItemSorting(List<ItemInfoSnapshot> itemInfoSnapshots) {
        int previousChunkId = 0;
        int previousItemId = -1;
        for (ItemInfoSnapshot itemInfoSnapshot : itemInfoSnapshots) {
            // Assert sorted by chunk id ASC > item id ASC
            assertThat(previousChunkId <= itemInfoSnapshot.getChunkId(), is(true));
            if (previousChunkId < itemInfoSnapshot.getChunkId()) {
                previousChunkId = itemInfoSnapshot.getChunkId();
                previousItemId = -1;
            }
            assertThat(previousItemId < itemInfoSnapshot.getItemId(), is(true));
            previousItemId = itemInfoSnapshot.getItemId();
        }
    }

    private String getJsonValue(JobEntity jobEntity) {
        return createObjectBuilder()
                .add("destination", jobEntity.getSpecification().getDestination())
                .add("type", jobEntity.getSpecification().getType().name())
                .build().toString();
    }

    private void persistAndRefresh(Object entity) {
        persistenceContext.run(() -> {
            entityManager.persist(entity);
            entityManager.flush();
            entityManager.refresh(entity);
        });
    }
}
