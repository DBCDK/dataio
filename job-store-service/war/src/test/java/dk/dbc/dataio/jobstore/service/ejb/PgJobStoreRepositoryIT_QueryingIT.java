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

package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.jobstore.service.entity.ChunkEntity;
import dk.dbc.dataio.jobstore.service.entity.ItemEntity;
import dk.dbc.dataio.jobstore.service.entity.JobEntity;
import dk.dbc.dataio.jobstore.service.sequenceanalyser.ChunkIdentifier;
import dk.dbc.dataio.jobstore.test.types.FlowStoreReferenceBuilder;
import dk.dbc.dataio.jobstore.types.FlowStoreReferences;
import dk.dbc.dataio.jobstore.types.ItemInfoSnapshot;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.jobstore.types.State;
import dk.dbc.dataio.jobstore.types.criteria.ChunkListCriteria;
import dk.dbc.dataio.jobstore.types.criteria.ItemListCriteria;
import dk.dbc.dataio.jobstore.types.criteria.JobListCriteria;
import dk.dbc.dataio.jobstore.types.criteria.ListFilter;
import dk.dbc.dataio.jobstore.types.criteria.ListOrderBy;
import dk.dbc.dataio.sequenceanalyser.CollisionDetectionElement;
import org.junit.Test;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

public class PgJobStoreRepositoryIT_QueryingIT extends PgJobStoreRepositoryAbstractIT {

    /**
     * Given: a job store containing three jobs
     * When : requesting a job count with a criteria selecting a subset of the jobs
     * Then : only the filtered snapshots are counted and orderby/offset is ignored
     */
    @Test
    public void countJobs() {
        // Given...
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
     * Given: a job store containing four jobs
     * When : requesting a job listing with a criteria selecting a subset of the jobs
     * Then : only the filtered snapshots are returned
     */
    @Test
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
    @Test
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
     *            one has not yet completed, one has failed during processing, one has completed without failure.
     * When     : requesting a job listing with a criteria selecting only jobs that has not yet completed
     * Then     : only jobs that has not yet completed are returned.
     */
    @Test
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
    @Test
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
     *            (only one with fatalError), and one has completed successfully
     * When     : requesting a job listing with a criteria selecting only jobs with fatal errors
     * Then     : only the job with fatal error is returned.
     */
    @Test
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
    @Test
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
    @Test
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
     * Given   : a job store containing two jobs, each with two chunks containing one successful item, three failed items and one ignored item
     * When    : requesting an item listing with a criteria selecting failed items from the selected job
     * Then    : the expected filtered snapshots are returned, sorted by chunk id ASC > item id ASC.
     */
    @Test
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
        for(ItemInfoSnapshot itemInfoSnapshot : returnedItemInfoSnapshots) {
            assertThat("itemInfoSnapshot.jobId", itemInfoSnapshot.getJobId(), is(selectedJobId));
            assertThat("itemInfoSnapshot.State.Phase.failed", itemInfoSnapshot.getState().getPhase(State.Phase.PROCESSING).getFailed(), is(1));
        }
    }

    /**
     * Given   : a job store containing two jobs, each with two chunks containing one successful item, three failed items and one ignored item
     * When    : requesting an item listing with a criteria selecting ignored items from the selected job
     * Then    : the expected filtered snapshot is returned.
     */
    @Test
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
    @Test
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
        for(ItemInfoSnapshot itemInfoSnapshot : returnedItemInfoSnapshots) {
            assertThat("itemInfoSnapshot.jobId", itemInfoSnapshot.getJobId(), is(selectedJobId));
        }
    }

    /**
     * Given    : a job store containing three jobs, where two are preview only and one has failed
     * When     : requesting a job listing with a criteria selecting preview only jobs
     * Then     : preview only jobs arer returned sorted by job ids in descending order.
     */
    @Test
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
     * Given   : a job store containing one job with two chunks where neither has finished
     * When    : requesting a chunk collision detection element listing with a criteria selecting all chunks that has not finished
     * Then    : the expected filtered chunk collision detection elements are returned, sorted by creation time ASC
     */
    @Test
    public void listChunksCollisionDetectionElements() {
        // Given...
        Timestamp timeOfCreation = new Timestamp(System.currentTimeMillis()); //timestamp older than creation time for any of the chunks.
        final int jobId = newPersistedJobEntity().getId();
        newPersistedAndRefreshedChunkEntity(new ChunkEntity.Key(0, jobId));
        newPersistedAndRefreshedChunkEntity(new ChunkEntity.Key(1, jobId));

        final ChunkListCriteria chunkListCriteria = new ChunkListCriteria()
                .where(new ListFilter<>(ChunkListCriteria.Field.TIME_OF_COMPLETION, ListFilter.Op.IS_NULL))
                .orderBy(new ListOrderBy<>(ChunkListCriteria.Field.TIME_OF_CREATION, ListOrderBy.Sort.ASC));

        // When...
        List<CollisionDetectionElement> returnedChunkCollisionDetectionElements = pgJobStoreRepository.listChunksCollisionDetectionElements(chunkListCriteria);

        // Then ...
        assertThat(returnedChunkCollisionDetectionElements.size(), is(2));
        
        for(CollisionDetectionElement cde : returnedChunkCollisionDetectionElements) {
            final ChunkIdentifier chunkIdentifier = (ChunkIdentifier) cde.getIdentifier();
            ChunkEntity.Key chunkEntityKey = new ChunkEntity.Key(Long.valueOf(chunkIdentifier.getChunkId()).intValue(), Long.valueOf(chunkIdentifier.getJobId()).intValue());
            ChunkEntity chunkEntity = entityManager.find(ChunkEntity.class, chunkEntityKey);

            assertThat("Time of completion is null", chunkEntity.getTimeOfCompletion(), is(nullValue())); // no end date
            assertThat(
                    "Previous collisionDetectionElement.timeOfCreation: {"
                            + timeOfCreation
                            + "} is before or equal to next collisionDetectionElement.timeOfCreation: {"
                            + chunkEntity.getTimeOfCreation() +"}.",
                    timeOfCreation.before(chunkEntity.getTimeOfCreation()) || timeOfCreation.equals(chunkEntity.getTimeOfCreation()), is(true)); // oldest first
            timeOfCreation = chunkEntity.getTimeOfCreation();
        }
    }

    /*
     * Private methods
     */

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
        jobEntity.getState().getPhase(failedPhase).setFailed(1);
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
        itemEntity.getState().getPhase(State.Phase.PROCESSING).setFailed(1);
        return itemEntity;
    }

    private ItemEntity newPersistedIgnoredItemEntity(ItemEntity.Key key) {
        final ItemEntity itemEntity = newIgnoredItemEntity(key);
        persist(itemEntity);
        return itemEntity;
    }

    private ItemEntity newIgnoredItemEntity(ItemEntity.Key key) {
        final ItemEntity itemEntity = newItemEntity(key);
        itemEntity.getState().getPhase(State.Phase.PROCESSING).setIgnored(1);
        return itemEntity;
    }

    // **************************** Helper methods *****************************

    private List<ItemEntity> generateChunkAndItemEntitiesForJob(int jobId) {
        List<ItemEntity> itemEntities = new ArrayList<>();

        // chunk entities
        ChunkEntity chunkEntity1 = newPersistedChunkEntity(new ChunkEntity.Key(0, jobId));
        ChunkEntity chunkEntity2 = newPersistedChunkEntity(new ChunkEntity.Key(1, jobId));

        // item entities for first chunk
        itemEntities.add(newPersistedFailedItemEntity(new ItemEntity.Key(jobId, chunkEntity1.getKey().getId(), (short)0)));
        itemEntities.add(newPersistedIgnoredItemEntity(new ItemEntity.Key(jobId, chunkEntity1.getKey().getId(), (short)1)));
        itemEntities.add(newPersistedItemEntity(new ItemEntity.Key(jobId, chunkEntity1.getKey().getId(), (short)2)));
        itemEntities.add(newPersistedFailedItemEntity(new ItemEntity.Key(jobId, chunkEntity1.getKey().getId(), (short)3)));

        // item entities for second chunk
        itemEntities.add(newPersistedFailedItemEntity(new ItemEntity.Key(jobId, chunkEntity2.getKey().getId(), (short)0)));

        return itemEntities;
    }

    private void assertAscendingChunkAndItemSorting(List<ItemInfoSnapshot> itemInfoSnapshots) {
        int previousChunkId = 0;
        int previousItemId = -1;
        for(ItemInfoSnapshot itemInfoSnapshot : itemInfoSnapshots) {
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
