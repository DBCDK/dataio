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

import dk.dbc.dataio.commons.time.StopWatch;
import dk.dbc.dataio.commons.types.AddiMetaData;
import dk.dbc.dataio.commons.types.Diagnostic;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.RRHarvesterConfig;
import dk.dbc.dataio.harvester.utils.holdingsitems.HoldingsItemsConnector;
import dk.dbc.dataio.harvester.utils.rawrepo.RawRepoConnector;
import dk.dbc.rawrepo.RawRepoException;
import dk.dbc.rawrepo.Record;
import dk.dbc.rawrepo.RecordId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ImsHarvestOperation extends HarvestOperation {
    private static final Logger LOGGER = LoggerFactory.getLogger(ImsHarvestOperation.class);

    private final HoldingsItemsConnector holdingsItemsConnector;

    public ImsHarvestOperation(RRHarvesterConfig config, HarvesterJobBuilderFactory harvesterJobBuilderFactory, EntityManager harvestTaskEntityManager)
            throws NullPointerException, IllegalArgumentException {
        this(config, harvesterJobBuilderFactory, harvestTaskEntityManager, null, null, null);
    }

    ImsHarvestOperation(RRHarvesterConfig config, HarvesterJobBuilderFactory harvesterJobBuilderFactory, EntityManager harvestTaskEntityManager,
                     AgencyConnection agencyConnection, RawRepoConnector rawRepoConnector, HoldingsItemsConnector holdingsItemsConnector) {
        super(config, harvesterJobBuilderFactory, harvestTaskEntityManager, agencyConnection, rawRepoConnector);
        this.holdingsItemsConnector = holdingsItemsConnector != null ? holdingsItemsConnector : getHoldingsItemsConnector(config);
    }

    /**
     * Runs this harvest operation, creating dataIO jobs from harvested records.
     * If any non-internal error occurs a record is marked as failed. Only records from
     * IMS agency IDs are processed and DBC library are process, all others are skipped.
     * Records from DBC library are mapped into IMS libraries with holdings (if any).
     * @return number of records processed
     * @throws HarvesterException on failure to complete harvest operation
     */
    @Override
    public int execute() throws HarvesterException {
        final StopWatch stopWatch = new StopWatch();
        final RecordHarvestTaskQueue recordHarvestTaskQueue = createTaskQueue();
        // Since we might (re)run batches with a size larger than the one currently configured
        final int batchSize = Math.max(configContent.getBatchSize(), recordHarvestTaskQueue.estimatedSize());

        Set<Integer> imsLibraries = null;
        if (!recordHarvestTaskQueue.isEmpty()) {
            imsLibraries = agencyConnection.getFbsImsLibraries();
        }

        int itemsProcessed = 0;
        RawRepoRecordHarvestTask recordHarvestTask = recordHarvestTaskQueue.poll();
        while (recordHarvestTask != null) {
            LOGGER.info("{} ready for harvesting", recordHarvestTask.getRecordId());

            // There is quite a bit of waisted effort being done here when
            // the workload contains more than one record, since we will actually be
            // fetching/merging the same record more than once. Fixing this entails
            // either an ImsHarvestOperation implementation having very little code
            // in common with HarvestOperation or a complete rewrite of the
            // HarvestOperation class, neither of which we have the time for
            // currently.
            for (RawRepoRecordHarvestTask task : unfoldRecordHarvestTask(recordHarvestTask, imsLibraries)) {
                processRecordHarvestTask(task);
            }

            if (++itemsProcessed == batchSize) {
                break;
            }
            recordHarvestTask = recordHarvestTaskQueue.poll();
        }
        flushHarvesterJobBuilders();

        recordHarvestTaskQueue.commit();

        LOGGER.info("Processed {} items from {} queue in {} ms",
                itemsProcessed, configContent.getConsumerId(), stopWatch.getElapsedTime());

        return itemsProcessed;
    }

    private HoldingsItemsConnector getHoldingsItemsConnector(RRHarvesterConfig config) throws NullPointerException, IllegalArgumentException {
        return new HoldingsItemsConnector(config.getContent().getImsHoldingsTarget());
    }

    private List<RawRepoRecordHarvestTask> unfoldRecordHarvestTask(RawRepoRecordHarvestTask recordHarvestTask, Set<Integer> imsLibraries) throws HarvesterException {
        final RecordId recordId = recordHarvestTask.getRecordId();
        if (recordId.getAgencyId() == DBC_LIBRARY) {
            return unfoldTaskDBC(recordHarvestTask, imsLibraries);
        }

        if (imsLibraries.contains(recordId.getAgencyId())) {
            return unfoldTaskIMS(recordHarvestTask);
        }

        return Collections.emptyList();
    }

    private List<RawRepoRecordHarvestTask> unfoldTaskDBC(RawRepoRecordHarvestTask recordHarvestTask, Set<Integer> imsLibraries) {
        final RecordId recordId = recordHarvestTask.getRecordId();
        final Set<Integer> agenciesWithHoldings = holdingsItemsConnector.hasHoldings(recordId.getBibliographicRecordId(), imsLibraries);
        if (!agenciesWithHoldings.isEmpty()) {
            return agenciesWithHoldings.stream()
                    .filter(imsLibraries::contains)
                    .map(agencyId -> new RawRepoRecordHarvestTask()
                            .withRecordId(new RecordId(recordId.getBibliographicRecordId(), agencyId))
                            .withAddiMetaData(new AddiMetaData()
                                    .withBibliographicRecordId(recordId.getBibliographicRecordId())
                                    .withSubmitterNumber(agencyId)))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    private List<RawRepoRecordHarvestTask> unfoldTaskIMS(RawRepoRecordHarvestTask recordHarvestTask) throws HarvesterException {
        try {
            final String bibliographicRecordId = recordHarvestTask.getRecordId().getBibliographicRecordId();
            final int agencyId = recordHarvestTask.getRecordId().getAgencyId();
            final Record record = rawRepoConnector.fetchRecord(recordHarvestTask.getRecordId());
            if (record.isDeleted()) {
                final boolean hasHolding = !holdingsItemsConnector.hasHoldings(bibliographicRecordId, new HashSet<>(Collections.singletonList(agencyId))).isEmpty();
                if (hasHolding) {
                    if (rawRepoConnector.recordExists(bibliographicRecordId, 870970)) {
                        LOGGER.info("using 870970 record content for deleted record {}", recordHarvestTask.getRecordId());
                        recordHarvestTask.withRecordId(new RecordId(bibliographicRecordId, 870970));
                        recordHarvestTask.getAddiMetaData().withDeleted(false);
                    }
                } else {
                    LOGGER.info("no holding for deleted record {} - skipping", recordHarvestTask.getRecordId());
                    return Collections.emptyList();
                }
            } else {
                LOGGER.info("record was not marked as delete");
            }
        } catch (SQLException | RawRepoException e) {
            final String errorMsg = String.format("RawRepo communication failed for %s: %s",
                    recordHarvestTask.getRecordId(), e.getMessage());
            recordHarvestTask.getAddiMetaData()
                    .withDiagnostic(new Diagnostic(Diagnostic.Level.FATAL, errorMsg));
        }
        return Collections.singletonList(recordHarvestTask);
    }
}
