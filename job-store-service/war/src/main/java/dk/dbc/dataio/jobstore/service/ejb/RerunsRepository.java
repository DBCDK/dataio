package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.commons.types.interceptor.Stopwatch;
import dk.dbc.dataio.jobstore.service.entity.RerunEntity;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

/**
 * DAO for job reruns repository
 */
@Stateless
public class RerunsRepository extends RepositoryBase {
    public RerunsRepository withEntityManager(EntityManager entityManager) {
        this.entityManager = entityManager;
        return this;
    }

    /**
     * Adds given {@link RerunEntity} to queue in waiting state
     *
     * @param rerunEntity entry to be added to queue
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void addWaiting(RerunEntity rerunEntity) {
        entityManager.persist(rerunEntity
                .withState(RerunEntity.State.WAITING));
    }

    /**
     * Removes given {@link RerunEntity} from queue
     *
     * @param rerunEntity entry to be removed
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void remove(RerunEntity rerunEntity) {
        if (!entityManager.contains(rerunEntity)) {
            rerunEntity = entityManager.merge(rerunEntity);
        }
        entityManager.remove(rerunEntity);
    }

    /**
     * Exclusively seizes head of queue if it is in
     * {@link dk.dbc.dataio.jobstore.service.entity.RerunEntity.State#WAITING} state
     * and updates it to {@link dk.dbc.dataio.jobstore.service.entity.RerunEntity.State#IN_PROGRESS}
     *
     * @return {@link RerunEntity} if the head entry was seized, empty if not
     */
    @Stopwatch
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public Optional<RerunEntity> seizeHeadOfQueueIfWaiting() {
        return entityManager.createNamedQuery(RerunEntity.FIND_HEAD_QUERY_NAME, RerunEntity.class)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .setMaxResults(1)
                .getResultList()
                .stream()
                .filter(entity -> entity.getState() == RerunEntity.State.WAITING)
                .peek(entity -> entity.withState(RerunEntity.State.IN_PROGRESS))
                .findFirst();
    }

    /**
     * Resets given {@link RerunEntity} to its {@link dk.dbc.dataio.jobstore.service.entity.RerunEntity.State#WAITING}
     * state
     *
     * @param rerunEntity entry to be reset
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void reset(RerunEntity rerunEntity) {
        if (!entityManager.contains(rerunEntity)) {
            rerunEntity = entityManager.merge(rerunEntity);
        }
        rerunEntity.withState(RerunEntity.State.WAITING);
    }

    /**
     * @return list of rerun entries currently marked as being in-progress (currently a list with a maximum size of one)
     */
    @Stopwatch
    public List<RerunEntity> getInProgress() {
        return entityManager.createNamedQuery(RerunEntity.FIND_BY_STATE_QUERY_NAME, RerunEntity.class)
                .setParameter(RerunEntity.FIELD_STATE, RerunEntity.State.IN_PROGRESS)
                .getResultList();
    }

    /**
     * @return list of rerun entries currently marked as waiting
     */
    @Stopwatch
    public List<RerunEntity> getWaiting() {
        return entityManager.createNamedQuery(RerunEntity.FIND_BY_STATE_QUERY_NAME, RerunEntity.class)
                .setParameter(RerunEntity.FIELD_STATE, RerunEntity.State.WAITING)
                .getResultList();
    }
}
