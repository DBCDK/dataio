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

import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.interceptor.Stopwatch;
import dk.dbc.dataio.jobstore.service.entity.JobQueueEntity;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.LockModeType;
import javax.persistence.TypedQuery;
import java.util.List;
import java.util.Optional;

/**
 * DAO for job queue repository
 */
@Stateless
public class JobQueueRepository extends RepositoryBase {
    /**
     * Adds given {@link JobQueueEntity} to queue in waiting state
     * @param jobQueueEntity entry to be added to queue
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void addWaiting(JobQueueEntity jobQueueEntity) {
        entityManager.persist(jobQueueEntity
                .withState(JobQueueEntity.State.WAITING));
    }

    /**
     * Removes given {@link JobQueueEntity} from queue
     * @param jobQueueEntity entry to be removed
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void remove(JobQueueEntity jobQueueEntity) {
        if (!entityManager.contains(jobQueueEntity)) {
            jobQueueEntity = entityManager.merge(jobQueueEntity);
        }
        entityManager.remove(jobQueueEntity);
    }

    /**
     * Exclusively seizes head of queue for given {@link Sink} if it
     * is in {@link dk.dbc.dataio.jobstore.service.entity.JobQueueEntity.State#WAITING} state
     * and updates it to {@link dk.dbc.dataio.jobstore.service.entity.JobQueueEntity.State#IN_PROGRESS}
     * @param sink {@link Sink} for which the head entry is to be seized
     * @return {@link JobQueueEntity} if the head entry was seized, empty if not
     */
    @Stopwatch
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public Optional<JobQueueEntity> seizeHeadOfQueueIfWaiting(Sink sink) {
        final TypedQuery<JobQueueEntity> query = entityManager.createNamedQuery(JobQueueEntity.NQ_FIND_QUEUE_FOR_SINK, JobQueueEntity.class)
                .setParameter(JobQueueEntity.FIELD_SINK_ID, sink.getId())
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .setMaxResults(1);

        final List<JobQueueEntity> rs = query.getResultList();
        if (rs.isEmpty() || rs.get(0).getState() != JobQueueEntity.State.WAITING) {
            return Optional.empty();
        }
        return Optional.of(rs.get(0)
            .withState(JobQueueEntity.State.IN_PROGRESS));
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