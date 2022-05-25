package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.jobstore.service.cdi.JobstoreDB;
import dk.dbc.dataio.jobstore.service.entity.JobEntity;
import dk.dbc.dataio.jobstore.types.JobStoreException;

import javax.inject.Inject;
import javax.persistence.EntityManager;

public abstract class RepositoryBase {
    @Inject
    @JobstoreDB
    protected EntityManager entityManager;

    protected void syncedPersist(Object entity) {
        entityManager.persist(entity);
        entityManager.flush();
        entityManager.refresh(entity);
    }

    public JobEntity getJobEntityById(int jobId) {
        return entityManager.find(JobEntity.class, jobId);
    }

    /**
     * The EJB specification requires these to be public because they are called from another EJB!
     */

    /**
     * Merge the state of the given entity into the current persistence context
     *
     * @param entity entity instance
     * @param <T>    entity type parameter
     * @return the managed instance that the state was merged to
     * @throws JobStoreException on failure to merge entity state
     */
    public <T> T merge(T entity) throws JobStoreException {
        try {
            return entityManager.merge(entity);
        } catch (RuntimeException e) {
            throw new JobStoreException("Unable to merge entity state into persistence context", e);
        }
    }

    public void flushEntityManager() {
        entityManager.flush();
    }

    public void refreshFromDatabase(JobEntity jobEntity) {
        entityManager.refresh(jobEntity);
    }
}
