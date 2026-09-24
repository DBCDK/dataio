package dk.dbc.dataio.jobstore.service.entity;

import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.jobstore.service.AbstractJobStoreIT;
import dk.dbc.dataio.jobstore.service.ejb.PgJobStoreRepository;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.jobstore.types.State;
import dk.dbc.dataio.jobstore.types.StateChange;
import dk.dbc.dataio.jobstore.types.criteria.JobListCriteria;
import dk.dbc.dataio.jobstore.types.criteria.ListFilter;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.Persistence;

import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import static dk.dbc.dataio.jobstore.types.State.Phase.DELIVERING;
import static dk.dbc.dataio.jobstore.types.State.Phase.PARTITIONING;
import static org.eclipse.persistence.config.PersistenceUnitProperties.JDBC_DRIVER;
import static org.eclipse.persistence.config.PersistenceUnitProperties.JDBC_PASSWORD;
import static org.eclipse.persistence.config.PersistenceUnitProperties.JDBC_URL;
import static org.eclipse.persistence.config.PersistenceUnitProperties.JDBC_USER;
import static org.eclipse.persistence.config.PersistenceUnitProperties.SESSION_NAME;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Asserts that a row written by one job-store instance is visible to the next read on another.
 * <p>
 * The persistence unit runs under shared-cache-mode DISABLE_SELECTIVE, so an entity that does not
 * opt out with {@code @Cacheable(false)} is held in EclipseLink's shared identity map. That map
 * belongs to one ServerSession, so an instance keeps serving the instance it cached no matter what
 * another instance writes, and every read path here goes through it: a native query with an entity
 * result class hands back the cached instance and discards the fetched row, and {@code find} does
 * not reach the database at all on a hit.
 * <p>
 * A second instance is modelled with a second EntityManagerFactory under its own
 * {@code eclipselink.session-name}, which is what makes it a separate ServerSession with a shared
 * cache of its own rather than another handle on the first. Each test clears the persistence
 * context between the two reads, so what it measures is the shared cache and not the first-level
 * one.
 */
public class SharedCacheStalenessIT extends AbstractJobStoreIT {

    @org.junit.Test
    public void jobListing_jobCompletedOnAnotherInstance_reportsTheCompletion() {
        // Given...
        JobEntity job = newPersistedJobEntity();
        PgJobStoreRepository repository = newPgJobStoreRepository();
        JobListCriteria criteria = new JobListCriteria()
                .where(new ListFilter<>(JobListCriteria.Field.JOB_ID, ListFilter.Op.EQUAL, job.getId()));

        List<JobInfoSnapshot> before = repository.listJobs(criteria);
        assertThat("timeOfCompletion before", before.get(0).getTimeOfCompletion(), is(nullValue()));
        entityManager.clear();

        // When...
        onAnotherInstance(otherEntityManager -> {
            JobEntity onOtherInstance = otherEntityManager.find(JobEntity.class, job.getId());
            onOtherInstance.setTimeOfCompletion(new Timestamp(System.currentTimeMillis()));
        });

        // Then...
        List<JobInfoSnapshot> after = repository.listJobs(criteria);
        assertThat("timeOfCompletion after", after.get(0).getTimeOfCompletion(), is(notNullValue()));
    }

    @org.junit.Test
    public void chunkListing_deliveringPhaseClosedOnAnotherInstance_reportsThePhaseAsDone() {
        // Given...
        JobEntity job = newPersistedJobEntity();
        ChunkEntity.Key chunkKey = new ChunkEntity.Key(0, job.getId());
        newPersistedChunkEntity(chunkKey);

        ChunkEntity before = entityManager.find(ChunkEntity.class, chunkKey);
        assertThat("DELIVERING done before", before.getState().phaseIsDone(DELIVERING), is(false));
        entityManager.clear();

        // When...
        onAnotherInstance(otherEntityManager -> {
            ChunkEntity onOtherInstance = otherEntityManager.find(ChunkEntity.class, chunkKey);
            Date now = new Date();
            State state = new State(onOtherInstance.getState());
            // DELIVERING cannot carry an end date until PARTITIONING has one, see State.setEndDate.
            state.updateState(new StateChange().setPhase(PARTITIONING).setSucceeded(1)
                    .setBeginDate(now).setEndDate(now));
            state.updateState(new StateChange().setPhase(DELIVERING).setSucceeded(1)
                    .setBeginDate(now).setEndDate(now));
            onOtherInstance.setState(state);
        });

        // Then...
        ChunkEntity after = entityManager.find(ChunkEntity.class, chunkKey);
        assertThat("DELIVERING done after", after.getState().phaseIsDone(DELIVERING), is(true));
    }

    @org.junit.Test
    public void deliveryDispatchItems_processedOnAnotherInstance_carryTheProcessingOutcome() {
        // Given...
        JobEntity job = newPersistedJobEntity();
        newPersistedChunkEntity(new ChunkEntity.Key(0, job.getId()));
        ItemEntity.Key itemKey = new ItemEntity.Key(job.getId(), 0, (short) 0);
        newPersistedItemEntity(itemKey);
        PgJobStoreRepository repository = newPgJobStoreRepository();

        List<ItemEntity> before = repository.getChunkItemEntities(job.getId(), 0);
        assertThat("processingOutcome before", before.get(0).getProcessingOutcome(), is(nullValue()));
        entityManager.clear();

        // When...
        onAnotherInstance(otherEntityManager -> {
            ItemEntity onOtherInstance = otherEntityManager.find(ItemEntity.class, itemKey);
            onOtherInstance.setProcessingOutcome(ChunkItem.successfulChunkItem()
                    .withId(itemKey.getId())
                    .withData("processed"));
        });

        // Then...
        List<ItemEntity> after = repository.getChunkItemEntities(job.getId(), 0);
        assertThat("processingOutcome after", after.get(0).getProcessingOutcome(), is(notNullValue()));
    }

    /**
     * Runs the given block against a second job-store instance and commits it.
     *
     * @param block work to carry out on the other instance
     */
    private void onAnotherInstance(Consumer<EntityManager> block) {
        Map<String, String> properties = new HashMap<>();
        properties.put(JDBC_USER, dbContainer.getUsername());
        properties.put(JDBC_PASSWORD, dbContainer.getPassword());
        properties.put(JDBC_URL, String.format("jdbc:postgresql://localhost:%s/%s",
                dbContainer.getHostPort(), dbContainer.getDatabaseName()));
        properties.put(JDBC_DRIVER, "org.postgresql.Driver");
        // Without a session name of its own, EclipseLink hands back another handle on the
        // ServerSession the first factory already built, shared cache included, and there is then
        // only one instance in the test.
        properties.put(SESSION_NAME, "jobstoreIT-" + UUID.randomUUID());

        EntityManagerFactory factory = Persistence.createEntityManagerFactory("jobstoreIT", properties);
        try (EntityManager otherEntityManager = factory.createEntityManager(properties)) {
            EntityTransaction transaction = otherEntityManager.getTransaction();
            transaction.begin();
            try {
                block.accept(otherEntityManager);
            } finally {
                transaction.commit();
            }
        } finally {
            factory.close();
        }
    }
}
