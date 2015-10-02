package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.jobstore.service.entity.JobEntity;
import dk.dbc.dataio.jobstore.service.entity.JobQueueEntity;
import dk.dbc.dataio.jobstore.service.util.JobstoreDB;

import javax.inject.Inject;
import javax.persistence.EntityManager;

/**
 * Created by ThomasBerg on 08/09/15.
 */
public abstract class RepositoryBase {

    @Inject @JobstoreDB protected EntityManager entityManager;

    protected void persist(JobQueueEntity jobQueueEntity) {

        entityManager.persist(jobQueueEntity);
        entityManager.flush();
    }

    protected JobEntity getJobEntityById(int jobId) {

        return entityManager.find(JobEntity.class, jobId);
    }

    /**
     * The EJB specification requires these to be public because they are called from another EJB!
     */
    public void flushEntityManager() {
        entityManager.flush();
    }
    public void refreshFromDatabase(JobEntity jobEntity) {
        entityManager.refresh(jobEntity);
    }
}
