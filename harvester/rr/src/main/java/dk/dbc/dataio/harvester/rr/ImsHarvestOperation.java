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
import dk.dbc.dataio.harvester.task.TaskRepo;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.HarvesterSourceException;
import dk.dbc.dataio.harvester.types.RRHarvesterConfig;
import dk.dbc.dataio.harvester.utils.holdingsitems.HoldingsItemsConnector;
import dk.dbc.dataio.harvester.utils.rawrepo.RawRepoConnector;
import dk.dbc.rawrepo.RecordData;
import dk.dbc.rawrepo.RecordId;
import dk.dbc.rawrepo.RecordServiceConnector;
import dk.dbc.rawrepo.RecordServiceConnectorException;
import dk.dbc.rawrepo.queue.ConfigurationException;
import dk.dbc.rawrepo.queue.QueueException;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ImsHarvestOperation extends HarvestOperation {
    private static final Logger LOGGER = LoggerFactory.getLogger(ImsHarvestOperation.class);

    private final HoldingsItemsConnector holdingsItemsConnector;

    public ImsHarvestOperation(RRHarvesterConfig config,
            HarvesterJobBuilderFactory harvesterJobBuilderFactory,
            TaskRepo taskRepo, String openAgencyEndpoint, MetricRegistry metricRegistry)
            throws NullPointerException, IllegalArgumentException, QueueException, SQLException, ConfigurationException {
        this(config, harvesterJobBuilderFactory, taskRepo,
            new AgencyConnection(openAgencyEndpoint), null, null, null, metricRegistry);
    }

    ImsHarvestOperation(RRHarvesterConfig config, HarvesterJobBuilderFactory harvesterJobBuilderFactory, TaskRepo taskRepo,
                        AgencyConnection agencyConnection, RawRepoConnector rawRepoConnector,
                        HoldingsItemsConnector holdingsItemsConnector, RecordServiceConnector recordServiceConnector,
                        MetricRegistry metricRegistry)
            throws QueueException, SQLException, ConfigurationException {
        super(config, harvesterJobBuilderFactory, taskRepo, agencyConnection, rawRepoConnector, recordServiceConnector, metricRegistry);
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

    @Override
    public void close() {
        if (holdingsItemsConnector != null) {
            holdingsItemsConnector.close();
        }
    }

    private HoldingsItemsConnector getHoldingsItemsConnector(RRHarvesterConfig config) throws NullPointerException, IllegalArgumentException {
        return new HoldingsItemsConnector(config.getContent().getImsHoldingsTarget());
    }

    private List<RawRepoRecordHarvestTask> unfoldRecordHarvestTask(RawRepoRecordHarvestTask recordHarvestTask, Set<Integer> imsLibraries) throws HarvesterException {
        final RecordId recordId = recordHarvestTask.getRecordId();
        List<RawRepoRecordHarvestTask> tasksToProcess = new ArrayList<>();

        if (recordId.getAgencyId() == DBC_LIBRARY) {
            tasksToProcess = unfoldTaskDBC(recordHarvestTask, imsLibraries);
        } else if (imsLibraries.contains(recordId.getAgencyId())) {
            tasksToProcess.add(recordHarvestTask);
        }
        tasksToProcess = unfoldTaskIMS(tasksToProcess);
        return tasksToProcess;
    }


    private List<RawRepoRecordHarvestTask> unfoldTaskDBC(RawRepoRecordHarvestTask recordHarvestTask, Set<Integer> imsLibraries) {
        final List<RawRepoRecordHarvestTask> toProcess = new ArrayList<>();
        final RecordId recordId = recordHarvestTask.getRecordId();
        final Set<Integer> agenciesWithHoldings = holdingsItemsConnector.hasHoldings(recordId.getBibliographicRecordId(), imsLibraries);
        if (!agenciesWithHoldings.isEmpty()) {
            toProcess.addAll(agenciesWithHoldings.stream()
                    .filter(imsLibraries::contains)
                    .map(agencyId -> new RawRepoRecordHarvestTask()
                            .withRecordId(new RecordId(recordId.getBibliographicRecordId(), agencyId))
                            .withAddiMetaData(new AddiMetaData()
                                    .withBibliographicRecordId(recordId.getBibliographicRecordId())
                                    .withSubmitterNumber(agencyId)))
                    .collect(Collectors.toList()));
        }

        return toProcess;
    }

    private List<RawRepoRecordHarvestTask> unfoldTaskIMS(List<RawRepoRecordHarvestTask> recordHarvestTasks) throws HarvesterException {
        int currentRecord = 0;
        final List<RawRepoRecordHarvestTask> toProcess = new ArrayList<>();
        try {
            for(RawRepoRecordHarvestTask repoRecordHarvestTask : recordHarvestTasks) {
                final String bibliographicRecordId = repoRecordHarvestTask.getRecordId().getBibliographicRecordId();
                final int agencyId = repoRecordHarvestTask.getRecordId().getAgencyId();
                final RecordData record = fetchRecord(repoRecordHarvestTask.getRecordId());
                if (record.isDeleted()) {
                    final boolean hasHolding = !holdingsItemsConnector.hasHoldings(bibliographicRecordId, new HashSet<>(Collections.singletonList(agencyId))).isEmpty();
                    if (hasHolding) {
                        if (rawRepoRecordServiceConnector.recordExists(870970, bibliographicRecordId)) {
                            LOGGER.info("using 870970 record content for deleted record {}", repoRecordHarvestTask.getRecordId());
                            repoRecordHarvestTask.withRecordId(new RecordId(bibliographicRecordId, 870970));
                            repoRecordHarvestTask.withForceAdd(true);
                            toProcess.add(repoRecordHarvestTask);
                        }
                    } else {
                        LOGGER.info("no holding for deleted record {} - skipping", repoRecordHarvestTask.getRecordId());
                    }
                } else {
                    toProcess.add(repoRecordHarvestTask);
                }
                currentRecord++;
            }
        } catch ( RecordServiceConnectorException | HarvesterSourceException e) {
            final RawRepoRecordHarvestTask task = recordHarvestTasks.get(currentRecord);
            final String errorMsg = String.format("RawRepo communication failed for %s: %s", task.getRecordId(), e.getMessage());
            task.getAddiMetaData().withDiagnostic(new Diagnostic(Diagnostic.Level.FATAL, errorMsg));
            toProcess.add(task);
        }
        return toProcess;
    }
}
