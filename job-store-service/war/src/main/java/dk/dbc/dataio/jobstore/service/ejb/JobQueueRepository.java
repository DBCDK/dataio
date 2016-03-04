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

import dk.dbc.dataio.commons.types.RecordSplitterConstants;
import dk.dbc.dataio.commons.types.interceptor.Stopwatch;
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

@Stateless
public class JobQueueRepository extends RepositoryBase {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(JobQueueRepository.class);

    /**
     *
     * @return list of unique Sink id's.
     */
    public List<Long> getUniqueSinkIds() {

        return entityManager.createNamedQuery(JobQueueEntity.NQ_FIND_UNIQUE_SINKS).getResultList();
    }

    /**

             1. hvis sinken er optaget så tilføj job i status WAITING
             2. Hvis sinken ikke er optaget og jobbet allerede venter så opdater jobbet til status IN_PROGRESS
             3. hvis sinken ikke er optaget og jobbet ikke venter så tilføj job i status IN_PROGRESS

     * @param sinkId                Id of the concrete Sink - NOT the CachedSink
     * @param job                   Id of the job
     * @param recordSplitterType    type of the Record Splitter
     * @return                      true if sink is occupied
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean addJobToJobQueueInDatabase(long sinkId, JobEntity job, RecordSplitterConstants.RecordSplitter recordSplitterType) {

        final boolean sinkOccupied = isSinkOccupied(sinkId);

        if(sinkOccupied) {
            this.addAsWaiting(sinkId, job, recordSplitterType);
        } else {

            if(this.isAlreadyWaiting(job)) {
                this.updateJobToBeInProgressIfExists(job);
            } else {
                this.addAsInProgress(sinkId, job, recordSplitterType);
            }
        }

        return sinkOccupied;
    }

    /**
     *
     * @param job JobEntity to delete in database
     */
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

    /**
     *
     * @param sinkId    The SinkId of the Sink to see if occupied.
     * @return          true if Sink is occupied
     */
    public boolean isSinkOccupied(long sinkId) {

        Long numberOfJobsBySink = (Long)entityManager
                .createNamedQuery(JobQueueEntity.NQ_FIND_NUMBER_OF_JOBS_BY_SINK)
                .setParameter(JobQueueEntity.FIELD_SINK_ID, sinkId)
                .setParameter(JobQueueEntity.FIELD_STATE, JobQueueEntity.State.IN_PROGRESS)
                .getSingleResult();

        return numberOfJobsBySink > 0 ? OCCUPIED : AVAILABLE;
    }

    /**
     *
     * @param job   JobEntity
     * @return      Job Queue element from the database
     */
    public JobQueueEntity getJobQueueEntityByJob(JobEntity job) {

        return (JobQueueEntity)entityManager.createNamedQuery(JobQueueEntity.NQ_FIND_BY_JOB).setParameter(JobQueueEntity.FIELD_JOB_ID, job).getSingleResult();
    }

    /**
     *
     * @param sinkId    The SinkId of the Sink to find first waiting job for.
     * @return          null if Queue element is not found
     */
    public JobQueueEntity getFirstWaitingJobQueueEntityBySink(Long sinkId) {

        JobQueueEntity firstWaitingJob = null;
        try {
            firstWaitingJob = (JobQueueEntity)entityManager.createNamedQuery(JobQueueEntity.NQ_FIND_WAITING_JOBS_BY_SINK)
                    .setFirstResult(0)
                    .setMaxResults(1)
                    .setParameter(JobQueueEntity.FIELD_SINK_ID, sinkId)
                    .setParameter(JobQueueEntity.FIELD_STATE, WAITING)
                    .getSingleResult();

        } catch (NoResultException nre) {
            LOGGER.info("Did not find any Job Queue Entity...");
        }

        return firstWaitingJob;
    }

    /**
     * @return list of job queue entries currently marked as being in-progress
     */
    @Stopwatch
    public List<JobQueueEntity> getInProgress() {
        @SuppressWarnings("unchecked")
        final List<JobQueueEntity> inProgress = entityManager.createNamedQuery(JobQueueEntity.NQ_FIND_BY_STATE)
                .setParameter(JobQueueEntity.FIELD_STATE, JobQueueEntity.State.IN_PROGRESS)
                .getResultList();
        return inProgress;
    }

    /* Private methods */

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
    private void addAsWaiting(
            long sinkId,
            JobEntity job,
            RecordSplitterConstants.RecordSplitter recordSplitterType) {

        persist(new JobQueueEntity(sinkId, job, WAITING, recordSplitterType));
    }
    private void addAsInProgress(
            long sinkId,
            JobEntity job,
            RecordSplitterConstants.RecordSplitter recordSplitterType) {

        persist(new JobQueueEntity(sinkId, job, IN_PROGRESS, recordSplitterType));
    }
}