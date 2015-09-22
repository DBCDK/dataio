package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.commons.types.RecordSplitterConstants;
import dk.dbc.dataio.jobstore.service.entity.JobEntity;
import dk.dbc.dataio.jobstore.service.entity.JobQueueEntity;
import org.slf4j.LoggerFactory;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.NoResultException;
import java.util.List;

import static dk.dbc.dataio.jobstore.service.entity.JobQueueEntity.AVAILABLE;
import static dk.dbc.dataio.jobstore.service.entity.JobQueueEntity.OCCUPIED;
import static dk.dbc.dataio.jobstore.service.entity.JobQueueEntity.State.IN_PROGRESS;
import static dk.dbc.dataio.jobstore.service.entity.JobQueueEntity.State.WAITING;

/**
 * Created by ThomasBerg on 08/09/15.
 */
@Stateless
public class JobQueueRepository extends RepositoryBase {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(JobQueueRepository.class);

    public List<Long> getUniqueSinkIds() {

        return entityManager.createNamedQuery(JobQueueEntity.NQ_FIND_UNIQUE_SINKS).getResultList();
    }

    /**
         1. hent job fra kø-tabel
         2. Hvis det ikke eksisterer i forvejen
             1. hvis sinken ikke er optaget så tilføj job i status IN_PROGRESS
             2. hvis sinken er optaget så tilføj job i status WAITING
         3. Hvis det eksisterer i forvejen
             1. hvis sinken ikke er optaget så opdater jobbet til status IN_PROGRESS
             2. hvis sinken er optaget så gør ingenting

     * @param sinkId                Id of the concrete Sink - NOT the CachedSink
     * @param job                   Id of the job
     * @param doSequenceAnalysis    Do it or not
     * @param recordSplitterType    type of the Record Splitter
     * @return                      true if sink is occupied
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean addJobToJobQueueInDatabase(long sinkId, JobEntity job, boolean doSequenceAnalysis, RecordSplitterConstants.RecordSplitter recordSplitterType) {

        final boolean sinkOccupied = isSinkOccupied(sinkId);

        if(sinkOccupied) {
            this.addAsWaiting(sinkId, job, doSequenceAnalysis, recordSplitterType);
        } else {

            if(this.isAlreadyWaiting(job)) {
                this.updateJobToBeInProgressIfExists(job);
            } else {
                this.addAsInProgress(sinkId, job, doSequenceAnalysis, recordSplitterType);
            }
        }

        return sinkOccupied;
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void removeJobFromJobQueueIfExists(JobEntity job) {

        try {
            JobQueueEntity jobQueueEntityToDelete = this.getJobQueueEntityByJob(job);
            entityManager.remove(jobQueueEntityToDelete);
            entityManager.flush();
        } catch (NoResultException nre) {
            LOGGER.info("Did not find any Job Queue Entity...");
        }

    }

    public JobQueueEntity getJobQueueEntityByJob(JobEntity job) {

        return (JobQueueEntity)entityManager.createNamedQuery(JobQueueEntity.NQ_FIND_BY_JOB).setParameter(JobQueueEntity.FIELD_JOB_ID, job).getSingleResult();
    }

    public List<JobQueueEntity> getJobQueueEntitiesBySink(Long sinkId) {
        return entityManager.createNamedQuery(JobQueueEntity.NQ_FIND_BY_SINK).setParameter(JobQueueEntity.FIELD_SINK_ID, sinkId).getResultList();
    }

    /*
            Private methods
    */
    private void updateJobToBeInProgressIfExists(JobEntity job) {

        try {
            JobQueueEntity jobQueueEntity = getJobQueueEntityByJob(job);
            jobQueueEntity.setState(IN_PROGRESS);
            entityManager.merge(jobQueueEntity);
            entityManager.flush();
        } catch (NoResultException nre) {
            LOGGER.info("Did not find any Job Queue Entity...");
        }

    }
    private boolean isAlreadyWaiting(JobEntity job) {

        final boolean A_ALREADY_WAITING_JOB = true;
        final boolean NEW_JOB = false;
        try {
            if(getJobQueueEntityByJob(job).getState() == WAITING) {
                return A_ALREADY_WAITING_JOB;
            } else {
                return NEW_JOB;
            }
        } catch (NoResultException nre) {
            return NEW_JOB;
        }
    }
    private boolean isSinkOccupied(long sinkId) {

        Long numberOfJobsBySink = (Long)entityManager
                .createNamedQuery(JobQueueEntity.NQ_FIND_NUMBER_OF_JOBS_BY_SINK)
                .setParameter(JobQueueEntity.FIELD_SINK_ID, sinkId)
                .setParameter(JobQueueEntity.FIELD_STATE, JobQueueEntity.State.IN_PROGRESS)
                .getSingleResult();

        return numberOfJobsBySink > 0 ? OCCUPIED : AVAILABLE;
    }
    private void addAsWaiting(
            long sinkId,
            JobEntity job,
            boolean doSequenceAnalysis,
            RecordSplitterConstants.RecordSplitter recordSplitterType) {

        persist(new JobQueueEntity(sinkId, job, WAITING, doSequenceAnalysis, recordSplitterType));
    }
    private void addAsInProgress(
            long sinkId,
            JobEntity job,
            boolean doSequenceAnalysis,
            RecordSplitterConstants.RecordSplitter recordSplitterType) {

        persist(new JobQueueEntity(sinkId, job, IN_PROGRESS, doSequenceAnalysis, recordSplitterType));
    }
}