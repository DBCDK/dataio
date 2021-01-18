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

package dk.dbc.dataio.harvester.corepo;

import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.commons.time.StopWatch;
import dk.dbc.dataio.commons.types.AddiMetaData;
import dk.dbc.dataio.commons.types.Pid;
import dk.dbc.dataio.harvester.TimeInterval;
import dk.dbc.dataio.harvester.TimeIntervalGenerator;
import dk.dbc.dataio.harvester.task.connector.HarvesterTaskServiceConnector;
import dk.dbc.dataio.harvester.task.connector.HarvesterTaskServiceConnectorException;
import dk.dbc.dataio.harvester.types.CoRepoHarvesterConfig;
import dk.dbc.dataio.harvester.types.HarvestRecordsRequest;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.opensearch.commons.repository.RepositoryException;
import dk.dbc.vipcore.exception.VipCoreException;
import dk.dbc.vipcore.libraryrules.VipCoreLibraryRulesConnector;
import dk.dbc.vipcore.marshallers.LibraryRule;
import dk.dbc.vipcore.marshallers.LibraryRulesRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class HarvestOperation {
    private static final Logger LOGGER = LoggerFactory.getLogger(HarvestOperation.class);
    private static final long HARVEST_INTERVAL_DURATION_IN_SECONDS = 600;
    private static final long HARVEST_LAG_IN_SECONDS = 30;
    static int HARVEST_MAX_BATCH_SIZE = 10000;

    private final CORepoConnector coRepoConnector;
    private final FlowStoreServiceConnector flowStoreServiceConnector;
    private final VipCoreLibraryRulesConnector vipCoreLibraryRulesConnector;
    private final HarvesterTaskServiceConnector rrHarvesterServiceConnector;
    private final PidFilter pidFilter;
    private CoRepoHarvesterConfig config;

    public HarvestOperation(CoRepoHarvesterConfig config, FlowStoreServiceConnector flowStoreServiceConnector,
                            VipCoreLibraryRulesConnector vipCoreLibraryRulesConnector,
                            HarvesterTaskServiceConnector rrHarvesterServiceConnector) throws HarvesterException {
        this(config, createCoRepoConnector(config), flowStoreServiceConnector,
                vipCoreLibraryRulesConnector, rrHarvesterServiceConnector);
    }

    HarvestOperation(CoRepoHarvesterConfig config, CORepoConnector coRepoConnector, FlowStoreServiceConnector flowStoreServiceConnector,
                     VipCoreLibraryRulesConnector vipCoreLibraryRulesConnector,
                     HarvesterTaskServiceConnector rrHarvesterServiceConnector) throws HarvesterException {
        this.config = config;
        this.coRepoConnector = coRepoConnector;
        this.flowStoreServiceConnector = flowStoreServiceConnector;
        this.vipCoreLibraryRulesConnector = vipCoreLibraryRulesConnector;
        this.rrHarvesterServiceConnector = rrHarvesterServiceConnector;
        this.pidFilter = createPidFilter();
    }

    public int execute() throws HarvesterException {
        final StopWatch stopWatch = new StopWatch();

        int pidsHarvested = 0;
        Instant timeOfLastHarvest = getTimeOfLastHarvestFromConfig();
        final TimeIntervalGenerator timeIntervalGenerator = new TimeIntervalGenerator()
                .withIntervalDuration(HARVEST_INTERVAL_DURATION_IN_SECONDS, ChronoUnit.SECONDS)
                .withStartingPoint(timeOfLastHarvest)
                .withEndPoint(Instant.now(), HARVEST_LAG_IN_SECONDS, ChronoUnit.SECONDS);

        List<AddiMetaData> recordsChangedInCORepo = new ArrayList<>();

        for (TimeInterval timeInterval : timeIntervalGenerator) {
            for (Pid pid : getChangesInCORepo(timeInterval)) {
                LOGGER.info("Adding PID {}", pid);
                recordsChangedInCORepo.add(pidToAddi(pid));
            }
            timeOfLastHarvest = timeInterval.getTo();

            if (recordsChangedInCORepo.size() >= HARVEST_MAX_BATCH_SIZE) {
                if (!isEnabled()) {
                    recordsChangedInCORepo.clear();
                    break;
                }
                pidsHarvested += submitTaskToRRHarvester(recordsChangedInCORepo);
                updateTimeOfLastHarvestInConfig(timeOfLastHarvest);
            }
        }

        if (isEnabled()) {
            if (!recordsChangedInCORepo.isEmpty()) {
                pidsHarvested += submitTaskToRRHarvester(recordsChangedInCORepo);
            }
            updateTimeOfLastHarvestInConfig(timeOfLastHarvest);
        }

        LOGGER.info("Harvested {} PIDs in {} ms", pidsHarvested, stopWatch.getElapsedTime());
        return pidsHarvested;
    }

    private Instant getTimeOfLastHarvestFromConfig() {
        final Date timeOfLastHarvest = config.getContent().getTimeOfLastHarvest();
        if (timeOfLastHarvest != null) {
            return timeOfLastHarvest.toInstant();
        }
        return Instant.EPOCH;
    }

    private List<Pid> getChangesInCORepo(TimeInterval timeInterval) throws HarvesterException {
        try {
            return coRepoConnector.getChangesInRepository(timeInterval.getFrom(), timeInterval.getTo(), pidFilter);
        } catch (RepositoryException e) {
            throw new HarvesterException(String.format("Unable to harvest interval %s", timeInterval), e);
        }
    }


    private AddiMetaData pidToAddi(Pid pid) {
        return new AddiMetaData()
                .withPid(pid.toString())
                .withSubmitterNumber(pid.getAgencyId())
                .withBibliographicRecordId(pid.getBibliographicRecordId());
    }

    private int submitTaskToRRHarvester(List<AddiMetaData> records) throws HarvesterException {
        try {
            LOGGER.info("Created RR harvester task {}",
                    rrHarvesterServiceConnector.createHarvestTask(
                            config.getContent().getRrHarvester(),
                            new HarvestRecordsRequest(records)));
            return records.size();
        } catch (HarvesterTaskServiceConnectorException | RuntimeException e) {
            throw new HarvesterException("");
        } finally {
            records.clear();
        }
    }

    private void updateTimeOfLastHarvestInConfig(Instant timeOfLastHarvest) throws HarvesterException {
        try {
            config.getContent().withTimeOfLastHarvest(Date.from(timeOfLastHarvest));
            config = flowStoreServiceConnector.updateHarvesterConfig(config);
        } catch (FlowStoreServiceConnectorException | RuntimeException e) {
            throw new HarvesterException("Unable to update harvester configuration in flow-store", e);
        }
    }

    private boolean isEnabled() throws HarvesterException {
        return refreshConfig().getContent().isEnabled();
    }

    private CoRepoHarvesterConfig refreshConfig() throws HarvesterException {
        try {
            config = flowStoreServiceConnector.getHarvesterConfig(config.getId(), CoRepoHarvesterConfig.class);
        } catch (FlowStoreServiceConnectorException | RuntimeException e) {
            throw new HarvesterException("Unable to refresh harvester configuration from flow-store", e);
        }
        return config;
    }

    private PidFilter createPidFilter() throws HarvesterException {
        try {
            final LibraryRulesRequest libraryRulesRequest = new LibraryRulesRequest();
            final LibraryRule libraryRule = new LibraryRule();
            libraryRule.setName(VipCoreLibraryRulesConnector.Rule.WORLDCAT_SYNCHRONIZE.getValue());
            libraryRule.setBool(true);
            libraryRulesRequest.setLibraryRule(Collections.singletonList(libraryRule));

            return new PidFilter(
                    vipCoreLibraryRulesConnector.getLibraries(libraryRulesRequest).stream().
                            map(Integer::parseInt).
                            collect(Collectors.toSet()));
        } catch (VipCoreException | RuntimeException e) {
            throw new HarvesterException("Unable to retrieve WorldCat libraries from OpenAgency", e);
        }
    }

    private static CORepoConnector createCoRepoConnector(CoRepoHarvesterConfig config) throws HarvesterException {
        try {
            return new CORepoConnector(config.getContent().getResource(), config.getLogId());
        } catch (SQLException | ClassNotFoundException e) {
            throw new HarvesterException(e);
        }
    }
}
