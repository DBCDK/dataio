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
import dk.dbc.dataio.harvester.types.HarvesterInvalidRecordException;
import dk.dbc.dataio.harvester.types.HarvesterSourceException;
import dk.dbc.dataio.harvester.types.RRHarvesterConfig;
import dk.dbc.dataio.harvester.utils.rawrepo.RawRepoConnector;
import dk.dbc.rawrepo.QueueJob;
import dk.dbc.rawrepo.RawRepoException;
import dk.dbc.rawrepo.Record;
import dk.dbc.rawrepo.RecordId;

import java.sql.SQLException;
import java.util.Optional;

/**
 * Source of records for a harvest operation
 */
public class RecordSource {
    private final RawRepoQueue rawRepoQueue;

    public RecordSource(RRHarvesterConfig config, RawRepoConnector rawRepoConnector) {
        this.rawRepoQueue = new RawRepoQueue(config, rawRepoConnector);
    }

    /**
     * @return next record or null if this source is empty.
     * @throws HarvesterException on failure to access this source
     */
    public RecordWrapper getRecord() throws HarvesterException {
        return rawRepoQueue.poll();
    }

    /**
     * This class acts as a wrapper for records in the rawrepo
     */
    public static class RecordWrapper {
        private final RecordId recordId;
        private Record record;
        private HarvesterException error;

        public RecordWrapper(RecordId recordId) {
            this.recordId = recordId;
        }

        public RecordId getRecordId() {
            return recordId;
        }

        public RecordWrapper withRecord(Record record) {
            this.record = record;
            return this;
        }

        public Optional<Record> getRecord() {
            return Optional.ofNullable(record);
        }

        public RecordWrapper withError(HarvesterException error) {
            this.error = error;
            return this;
        }

        public HarvesterException getError() {
            return error;
        }

        @Override
        public String toString() {
            return recordId.toString();
        }
    }

    /* Abstraction layer for rawrepo queue */
    private static class RawRepoQueue {
        private final RRHarvesterConfig.Content config;
        private final RawRepoConnector rawRepoConnector;

        public RawRepoQueue(RRHarvesterConfig config, RawRepoConnector rawRepoConnector) {
            this.config = config.getContent();
            this.rawRepoConnector = rawRepoConnector;
        }

        /**
         * Retrieves and removes the head of this rawrepo queue, or returns null if this queue is empty.
         * @return the head of this rawrepo queue, or null if this queue is empty
         * @throws HarvesterException on error while retrieving a queued record
         */
        public RecordWrapper poll() throws HarvesterException {
            try {
                final QueueJob queueJob = rawRepoConnector.dequeue(config.getConsumerId());
                RecordWrapper recordWrapper = null;
                if (queueJob != null) {
                    recordWrapper = new RecordWrapper(queueJob.getJob());
                    try {
                        recordWrapper.withRecord(fetchRecordFromRR(queueJob.getJob(), rawRepoConnector));
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

    /**
     * Fetched rawrepo record with given record ID using given connector
     * @param recordId ID of record to be fetched
     * @param connector rawrepo connector
     * @return rawrepo record
     * @throws HarvesterSourceException on error communicating with the rawrepo
     * @throws HarvesterInvalidRecordException if null-valued record is retrieved
     */
    public static Record fetchRecordFromRR(RecordId recordId, RawRepoConnector connector)
            throws HarvesterSourceException, HarvesterInvalidRecordException {
        try {
            final Record record = connector.fetchRecord(recordId);
            if (record == null) {
                throw new HarvesterInvalidRecordException("Record for " + recordId + " was not found");
            }
            return record;
        } catch (SQLException | RawRepoException e) {
            throw new HarvesterSourceException("Unable to fetch record for " + recordId + ": " + e.getMessage(), e);
        }
    }
}
