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
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.RRHarvesterConfig;
import dk.dbc.dataio.harvester.utils.rawrepo.RawRepoConnector;
import dk.dbc.rawrepo.QueueJob;
import dk.dbc.rawrepo.RawRepoException;
import dk.dbc.rawrepo.RecordId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Abstraction layer for rawrepo queue.
 * This class is not thread safe.
 */
public class RawRepoQueue implements RecordHarvestTaskQueue {
    private static final Logger LOGGER = LoggerFactory.getLogger(RawRepoQueue.class);
    private static final int HIGH_PRIORITY_THRESHOLD = 500;

    private final RRHarvesterConfig.Content config;
    private final RawRepoConnector rawRepoConnector;
    private final int pileUpDuration;
    private final ChronoUnit pileUpDurationUnit;
    private RawRepoRecordHarvestTask head;

    private boolean breakDequeueLoop = false;
    private Instant firstHighPrioritySeenAt = null;

    public RawRepoQueue(RRHarvesterConfig config, RawRepoConnector rawRepoConnector) {
        this(config, rawRepoConnector, 45, ChronoUnit.SECONDS);
    }

    RawRepoQueue(RRHarvesterConfig config, RawRepoConnector rawRepoConnector,
                        int pileUpDuration, ChronoUnit pileUpDurationUnit) {
        this.config = config.getContent();
        this.rawRepoConnector = rawRepoConnector;
        this.head = null;
        this.pileUpDuration = pileUpDuration;
        this.pileUpDurationUnit = pileUpDurationUnit;
    }

    /**
     * Retrieves, but does not remove, the head of this rawrepo queue, or returns null if this queue is empty.
     * @return the head of this rawrepo queue, or null if this queue is empty
     * @throws HarvesterException on error while retrieving a queued record
     */
    @Override
    public RawRepoRecordHarvestTask peek() throws HarvesterException {
        if (head == null && !breakDequeueLoop) {
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
    public RawRepoRecordHarvestTask poll() throws HarvesterException {
        try {
            return peek();
        } finally {
            head = null;
        }
    }

    @Override
    public int estimatedSize() {
        return head == null ? 0 : 1;
    }

    @Override
    public int basedOnJob() {
        return 0;
    }

    @Override
    public boolean isEmpty() throws HarvesterException {
        return peek() == null;
    }

    @Override
    public void commit() { }

    private RawRepoRecordHarvestTask head() throws HarvesterException {
        try {
            final QueueJob queueJob = rawRepoConnector.dequeue(config.getConsumerId());
            if (queueJob != null) {
                breakDequeueLoop = breakOnHighPriority(queueJob);
                if (breakDequeueLoop) {
                    LOGGER.info("Breaking harvest loop for high priority item");
                }
                final RecordId recordId = queueJob.getJob();
                return new RawRepoRecordHarvestTask()
                            .withRecordId(recordId)
                            .withAddiMetaData(new AddiMetaData()
                                        .withSubmitterNumber(recordId.getAgencyId())
                                        .withBibliographicRecordId(recordId.getBibliographicRecordId()));
            }
            return null;
        } catch (SQLException | RawRepoException e) {
            throw new HarvesterException(e);
        }
    }

    private boolean breakOnHighPriority(QueueJob queueJob) {
        if (queueJob.getPriority() <= HIGH_PRIORITY_THRESHOLD) {
            // a high priority item was dequeued
            if (firstHighPrioritySeenAt == null) {
                // this is the first high priority item seen in this harvest loop
                // so we start the "pile-up" timer
                firstHighPrioritySeenAt = Instant.now();
            }
            // as long as we keep seeing high priority items
            // we allow them to pile up for a period of time
            return pileUpDurationUnit.between(firstHighPrioritySeenAt, Instant.now()) >= pileUpDuration;
        }
        // either we have not yet seen a high priority item,
        // in which case we should just keep on dequeueing,
        // or we have gone from high to low priority items,
        // in which case we should break.
        return firstHighPrioritySeenAt != null;
    }
}
