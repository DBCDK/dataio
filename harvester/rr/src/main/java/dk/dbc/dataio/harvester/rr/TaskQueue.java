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

package dk.dbc.dataio.harvester.rr;

import dk.dbc.dataio.commons.types.AddiMetaData;
import dk.dbc.dataio.harvester.rr.entity.HarvestTask;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.RRHarvesterConfig;
import dk.dbc.rawrepo.RecordId;

import javax.persistence.EntityManager;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.Optional;

/**
 * Abstraction layer for outstanding tasks.
 * This class is not thread safe.
 */
public class TaskQueue implements RecordQueue {
    private final RRHarvesterConfig config;
    private final EntityManager entityManager;
    private final HarvestTask task;
    private int cursor;
    private RawRepoRecordHarvestTask head;

    public TaskQueue(RRHarvesterConfig config, EntityManager entityManager) {
        this.config = config;
        this.entityManager = entityManager;
        final Optional<HarvestTask> readyTask = findNextReadyTask();
        if (readyTask.isPresent()) {
            this.task = readyTask.get();
        } else {
            this.task = new HarvestTask();
            this.task.setRecordIds(Collections.emptyList());
        }
        this.cursor = 0;
        this.head = null;
    }

    /**
     * Retrieves, but does not remove, the head of this task, or returns null if this task is empty.
     * @return the head of this task, or null if this task is empty
     * @throws HarvesterException on error while retrieving a queued record
     */
    @Override
    public RawRepoRecordHarvestTask peek() throws HarvesterException {
        if (head == null) {
            head = head();
        }
        return head;
    }

    /**
     * Retrieves and removes the head of this task, or returns null if this task is empty.
     * @return the head of this task, or null if this task is empty
     * @throws HarvesterException on error while retrieving a queued record
     */
    @Override
    public RawRepoRecordHarvestTask poll() throws HarvesterException {
        final RawRepoRecordHarvestTask peek = peek();
        if (peek != null) {
            cursor++;
        }
        head = null;
        return peek;
    }

    @Override
    public int size() {
        return task.getRecordIds().size() - cursor;
    }

    @Override
    public void commit() {
        task.setStatus(HarvestTask.Status.COMPLETED);
        task.setTimeOfCompletion(new Timestamp(System.currentTimeMillis()));
    }

    private Optional<HarvestTask> findNextReadyTask() {
        return entityManager.createNamedQuery(HarvestTask.QUERY_FIND_READY, HarvestTask.class)
                .setParameter("configId", config.getId())
                .setMaxResults(1)
                .getResultList()
                .stream()
                .findFirst();
    }

    private RawRepoRecordHarvestTask head() throws HarvesterException {
        if (size() > 0) {
            final String bibliographicRecordId = task.getRecordIds().get(cursor);
            final RecordId recordId = new RecordId(bibliographicRecordId, Math.toIntExact(task.getSubmitterNumber()));
            return new RawRepoRecordHarvestTask()
                            .withRecordId(recordId)
                            .withAddiMetaData(new AddiMetaData()
                                        .withSubmitterNumber(recordId.getAgencyId())
                                        .withBibliographicRecordId(recordId.getBibliographicRecordId()));
        }
        return null;
    }
}
