/*
 * DataIO - Data IO
 * Copyright (C) 2017 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
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

package dk.dbc.dataio.harvester.phholdingsitems;

import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.commons.time.StopWatch;
import dk.dbc.dataio.commons.types.AddiMetaData;
import dk.dbc.dataio.harvester.TimeInterval;
import dk.dbc.dataio.harvester.TimeIntervalGenerator;
import dk.dbc.dataio.harvester.types.HarvestRecordsRequest;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.PhHoldingsItemsHarvesterConfig;
import dk.dbc.dataio.rrharvester.service.connector.RRHarvesterServiceConnector;
import dk.dbc.dataio.rrharvester.service.connector.RRHarvesterServiceConnectorException;
import dk.dbc.phlog.PhLog;
import dk.dbc.phlog.dto.PhLogEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class HarvestOperation {
    private static final Logger LOGGER = LoggerFactory.getLogger(HarvestOperation.class);
    private static final long HARVEST_INTERVAL_DURATION_IN_SECONDS = 600;
    private static final long HARVEST_LAG_IN_SECONDS = 30;
    static int HARVEST_MAX_BATCH_SIZE = 100000;

    private final PhLog phLog;
    private final FlowStoreServiceConnector flowStoreServiceConnector;
    private final RRHarvesterServiceConnector rrHarvesterServiceConnector;
    private PhHoldingsItemsHarvesterConfig config;

    public HarvestOperation(PhHoldingsItemsHarvesterConfig config, PhLog phLog,
            FlowStoreServiceConnector flowStoreServiceConnector,
            RRHarvesterServiceConnector rrHarvesterServiceConnector)
            throws HarvesterException {
        this.config = config;
        this.phLog = phLog;
        this.flowStoreServiceConnector = flowStoreServiceConnector;
        this.rrHarvesterServiceConnector = rrHarvesterServiceConnector;
    }

    public int execute() throws HarvesterException {
        final StopWatch stopWatch = new StopWatch();

        int recordsHarvested = 0;
        Instant timeOfLastHarvest = getTimeOfLastHarvestFromConfig();
        final TimeIntervalGenerator timeIntervalGenerator = new TimeIntervalGenerator()
            .withIntervalDuration(HARVEST_INTERVAL_DURATION_IN_SECONDS, ChronoUnit.SECONDS)
            .withStartingPoint(timeOfLastHarvest)
            .withEndPoint(Instant.now(), HARVEST_LAG_IN_SECONDS, ChronoUnit.SECONDS);

        List<AddiMetaData> modifiedRecords = new ArrayList<>();
        for (TimeInterval timeInterval : timeIntervalGenerator) {
            PhLog.ResultSet<PhLogEntry> phLogEntries =
                phLog.getEntriesModifiedBetween(timeInterval.getFrom(),
                timeInterval.getTo());
            for(PhLogEntry entry : phLogEntries) {
                AddiMetaData metaData = new AddiMetaData()
                    .withSubmitterNumber(entry.getKey().getAgencyId())
                    .withBibliographicRecordId(entry.getKey()
                        .getBibliographicRecordId())
                    .withDeleted(entry.getDeleted())
                    .withHoldingsStatusMap(entry.getHoldingsStatusMap());
                modifiedRecords.add(metaData);
            }
            timeOfLastHarvest = timeInterval.getTo();
            // duplicate code from harvester/corepo/HarvestOperation.java
            if(modifiedRecords.size() >= HARVEST_MAX_BATCH_SIZE) {
                if(!isEnabled()) {
                    modifiedRecords.clear();
                    break;
                }
                recordsHarvested += submitTaskToRRHarvester(modifiedRecords);
                updateTimeOfLastHarvestInConfig(timeOfLastHarvest);
            }
        }
        if(isEnabled()) {
            if(!modifiedRecords.isEmpty()) {
                recordsHarvested += submitTaskToRRHarvester(modifiedRecords);
            }
            updateTimeOfLastHarvestInConfig(timeOfLastHarvest);
        }

        LOGGER.info("harvested {} elements in {} ms", recordsHarvested, stopWatch.getElapsedTime());
        return recordsHarvested;
    }

    /*
     * duplicate code from harvester/corepo/HarvestOperation.java.
     * since the return type of config.getContent is generic, these methods
     * cannot be placed in a super-class.
     * to make HarvesterConfig.Content type-specific the json blobs stored
     * in databases need to carry this type information and all old data need
     * to be migrated.
     */
    private Instant getTimeOfLastHarvestFromConfig() {
        final Date timeOfLastHarvest = config.getContent().getTimeOfLastHarvest();
        if (timeOfLastHarvest != null) {
            return timeOfLastHarvest.toInstant();
        }
        return Instant.now().minus(60, ChronoUnit.SECONDS);
    }

    private void updateTimeOfLastHarvestInConfig(Instant timeOfLastHarvest) throws HarvesterException {
        try {
            config.getContent().withTimeOfLastHarvest(Date.from(timeOfLastHarvest));
            config = flowStoreServiceConnector.updateHarvesterConfig(config);
        } catch (FlowStoreServiceConnectorException | RuntimeException e) {
            throw new HarvesterException("Unable to update harvester configuration in flow-store", e);
        }
    }

    private int submitTaskToRRHarvester(List<AddiMetaData> records) throws HarvesterException {
        try {
            for(long harvesterId : config.getContent().getRrHarvesters()) {
                LOGGER.info("Created RR harvester task {}",
                    rrHarvesterServiceConnector.createHarvestTask(harvesterId,
                    new HarvestRecordsRequest(records)));
            }
            return records.size();
        } catch (RRHarvesterServiceConnectorException | RuntimeException e) {
            throw new HarvesterException("Exception caught while harvesting phlog", e);
        } finally {
            records.clear();
        }
    }

    private boolean isEnabled() throws HarvesterException {
        return refreshConfig().getContent().isEnabled();
    }

    private PhHoldingsItemsHarvesterConfig refreshConfig() throws HarvesterException {
        try {
            config = flowStoreServiceConnector.getHarvesterConfig(config.getId(),
                PhHoldingsItemsHarvesterConfig.class);
        } catch (FlowStoreServiceConnectorException | RuntimeException e) {
            throw new HarvesterException("Unable to refresh harvester configuration from flow-store", e);
        }
        return config;
    }
    // end duplicate code
}
