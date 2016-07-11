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

import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.RRHarvesterConfig;
import dk.dbc.dataio.harvester.utils.rawrepo.RawRepoConnector;
import dk.dbc.rawrepo.QueueJob;
import dk.dbc.rawrepo.RawRepoException;

import java.sql.SQLException;

/**
 * Abstraction layer for rawrepo queue
 */
public class RawRepoQueue implements RecordQueue {
    private final RRHarvesterConfig.Content config;
    private final RawRepoConnector rawRepoConnector;
    private RecordWrapper head;

    public RawRepoQueue(RRHarvesterConfig config, RawRepoConnector rawRepoConnector) {
        this.config = config.getContent();
        this.rawRepoConnector = rawRepoConnector;
        this.head = null;
    }

    /**
     * Retrieves, but does not remove, the head of this rawrepo queue, or returns null if this queue is empty.
     * @return the head of this rawrepo queue, or null if this queue is empty
     * @throws HarvesterException on error while retrieving a queued record
     */
    @Override
    public RecordWrapper peek() throws HarvesterException {
        if (head == null) {
            head = head();
        }
        return head;
    }

    /**
     * Retrieves and removes the head of this rawrepo queue, or returns null if this queue is empty.
     * @return the head of this rawrepo queue, or null if this queue is empty
     * @throws HarvesterException on error while retrieving a queued record
     */
    @Override
    public RecordWrapper poll() throws HarvesterException {
        try {
            return peek();
        } finally {
            head = null;
        }
    }

    @Override
    public int size() {
        return head == null ? 0 : 1;
    }

    private RecordWrapper head() throws HarvesterException {
        try {
            final QueueJob queueJob = rawRepoConnector.dequeue(config.getConsumerId());
            RecordWrapper recordWrapper = null;
            if (queueJob != null) {
                recordWrapper = new RecordWrapper(queueJob.getJob());
                try {
                    recordWrapper.withRecord(HarvestOperation.fetchRecordFromRR(queueJob.getJob(), rawRepoConnector));
                } catch (HarvesterException e) {
                    recordWrapper.withError(e);
                }
            }
            return recordWrapper;
        } catch (SQLException | RawRepoException e) {
            throw new HarvesterException(e);
        }
    }
}
