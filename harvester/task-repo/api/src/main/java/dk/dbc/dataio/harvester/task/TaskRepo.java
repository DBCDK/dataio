/*
 * DataIO - Data IO
 *
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

package dk.dbc.dataio.harvester.task;

import dk.dbc.dataio.harvester.task.entity.HarvestTask;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.Optional;

/**
 * This class contains the harvester task repository API
 */
@Stateless
public class TaskRepo {
    @PersistenceContext(unitName = "taskrepo_PU")
    EntityManager entityManager;

    public TaskRepo() {}

    public TaskRepo(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public EntityManager getEntityManager() {
        return entityManager;
    }

    /**
     * Returns next available harvest for specified harvester
     * @param configId configuration ID of specific harvester
     * @return task or empty of none available
     */
    public Optional<HarvestTask> findNextHarvestTask(long configId) {
        return entityManager.createNamedQuery(HarvestTask.QUERY_FIND_NEXT, HarvestTask.class)
                .setParameter("configId", configId)
                .setMaxResults(1)
                .getResultList()
                .stream()
                .findFirst();
    }
}
