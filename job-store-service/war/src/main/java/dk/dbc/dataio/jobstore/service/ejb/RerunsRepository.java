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

import dk.dbc.dataio.commons.types.interceptor.Stopwatch;
import dk.dbc.dataio.jobstore.service.entity.RerunEntity;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
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
     * @param rerunEntity entry to be added to queue
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void addWaiting(RerunEntity rerunEntity) {
        entityManager.persist(rerunEntity
                .withState(RerunEntity.State.WAITING));
    }

    /**
     * Removes given {@link RerunEntity} from queue
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
