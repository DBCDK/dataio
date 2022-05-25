package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.interceptor.Stopwatch;
import dk.dbc.dataio.jobstore.service.entity.JobQueueEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.TypedQuery;
import java.util.List;
import java.util.Optional;

/**
 * DAO for job queue repository
 */
@Stateless
public class JobQueueRepository extends RepositoryBase {
    private static final Logger LOGGER = LoggerFactory.getLogger(JobQueueRepository.class);

    public JobQueueRepository withEntityManager(EntityManager entityManager) {
        this.entityManager = entityManager;
        return this;
    }

    /**
     * Adds given {@link JobQueueEntity} to queue in waiting state
     *
     * @param jobQueueEntity entry to be added to queue
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void addWaiting(JobQueueEntity jobQueueEntity) {
        entityManager.persist(jobQueueEntity
                .withState(JobQueueEntity.State.WAITING));
    }

    /**
     * Removes given {@link JobQueueEntity} from queue
     *
     * @param jobQueueEntity entry to be removed
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void remove(JobQueueEntity jobQueueEntity) {
        if (!entityManager.contains(jobQueueEntity)) {
            jobQueueEntity = entityManager.merge(jobQueueEntity);
        }
        entityManager.remove(jobQueueEntity);
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void retry(JobQueueEntity jobQueueEntity) {
        if (!entityManager.contains(jobQueueEntity)) {
            jobQueueEntity = entityManager.merge(jobQueueEntity);
        }
        entityManager.refresh(jobQueueEntity, LockModeType.PESSIMISTIC_WRITE);
        jobQueueEntity
                .withState(JobQueueEntity.State.WAITING)
                .withRetries(jobQueueEntity.getRetries() + 1);
    }

    /**
     * Exclusively seizes head of queue for given {@link Sink} if it
     * is in {@link dk.dbc.dataio.jobstore.service.entity.JobQueueEntity.State#WAITING} state
     * and updates it to {@link dk.dbc.dataio.jobstore.service.entity.JobQueueEntity.State#IN_PROGRESS}
     *
     * @param sink {@link Sink} for which the head entry is to be seized
     * @return {@link JobQueueEntity} if the head entry was seized, empty if not
     */
    @Stopwatch
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public Optional<JobQueueEntity> seizeHeadOfQueueIfWaiting(Sink sink) {
        final TypedQuery<JobQueueEntity> query = entityManager.createNamedQuery(JobQueueEntity.NQ_FIND_BY_SINK_AND_AVAILABLE_SUBMITTER,
                        JobQueueEntity.class)
                .setParameter(JobQueueEntity.FIELD_SINK_ID, sink.getId());

        final List rs = query.getResultList();
        JobQueueEntity e = rs.isEmpty() ? null : (JobQueueEntity) rs.get(0);
        if (e == null || e.getState() != JobQueueEntity.State.WAITING) {
            return Optional.empty();
        }
        LOGGER.info("seizeHeadOfQueueIfWaiting seized job {}", e.getJob().getId());
        return Optional.of(e.withState(JobQueueEntity.State.IN_PROGRESS));
    }

    /**
     * @return list of job queue entries currently marked as being in-progress
     */
    @Stopwatch
    public List<JobQueueEntity> getInProgress() {
        return entityManager.createNamedQuery(JobQueueEntity.NQ_FIND_BY_STATE, JobQueueEntity.class)
                .setParameter(JobQueueEntity.FIELD_STATE, JobQueueEntity.State.IN_PROGRESS)
                .getResultList();
    }

    /**
     * @return list of job queue entries currently marked as being waiting
     */
    @Stopwatch
    public List<JobQueueEntity> getWaiting() {
        return entityManager.createNamedQuery(JobQueueEntity.NQ_FIND_BY_STATE, JobQueueEntity.class)
                .setParameter(JobQueueEntity.FIELD_STATE, JobQueueEntity.State.WAITING)
                .getResultList();
    }
}
