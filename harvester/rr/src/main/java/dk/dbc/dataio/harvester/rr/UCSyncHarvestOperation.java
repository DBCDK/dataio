package dk.dbc.dataio.harvester.rr;

import dk.dbc.dataio.commons.time.StopWatch;
import dk.dbc.dataio.commons.types.AddiMetaData;
import dk.dbc.dataio.harvester.task.TaskRepo;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.RRHarvesterConfig;
import dk.dbc.rawrepo.dto.RecordIdDTO;
import dk.dbc.rawrepo.queue.ConfigurationException;
import dk.dbc.rawrepo.queue.QueueException;
import dk.dbc.vipcore.libraryrules.VipCoreLibraryRulesConnector;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class UCSyncHarvestOperation extends ImsHarvestOperation {
    private static final Logger LOGGER = LoggerFactory.getLogger(UCSyncHarvestOperation.class);
    public UCSyncHarvestOperation(RRHarvesterConfig config,
                                  HarvesterJobBuilderFactory harvesterJobBuilderFactory,
                                  TaskRepo taskRepo, MetricRegistry metricRegistry,
                                  VipCoreLibraryRulesConnector vipCoreLibraryRulesConnector)
            throws SQLException, ConfigurationException, QueueException {
        super(config, harvesterJobBuilderFactory, taskRepo,
                new VipCoreConnection(vipCoreLibraryRulesConnector), null, null,
                null, metricRegistry);
    }

    /**
     * Runs this harvest operation, creating one dataIO job from harvested records.
     * The agencyid used is looked up as the agencyid of the first record on queue.
     * If any non-internal error occurs, a record is marked as failed. DBC library records (191919) that occur,
     * are considered belonging to that same batch (library), and are therefore also harvested and
     * added to the same job.
     * Records encountered and not belonging to that initially determined agency (and is not 191919 (DBC)), are
     * discarded.
     *
     * @return number of records processed
     * @throws HarvesterException on failure to complete harvest operation
     */
    @Override
    public int execute() throws HarvesterException {
        StopWatch stopWatch = new StopWatch();
        RecordHarvestTaskQueue recordHarvestTaskQueue = createTaskQueue();

        // Since we might (re)run batches with a size larger than the one currently configured
        int batchSize = Math.max(configContent.getBatchSize(), recordHarvestTaskQueue.estimatedSize());

        int itemsProcessed = 0;
        RawRepoRecordHarvestTask recordHarvestTask = recordHarvestTaskQueue.poll();
        int agencyId = getFirstNonDBCAgencyIdFromQueue();
        while (recordHarvestTask != null) {
            LOGGER.info("{} ready for harvesting", recordHarvestTask.getRecordId());

            RawRepoRecordHarvestTask unfoldedTask = unfoldRecordHarvestTask(recordHarvestTask, agencyId);
            if (unfoldedTask != null) {
                processRecordHarvestTask(unfoldedTask);
            } else {
                LOGGER.info("{} NOT harvested. Probably due to missing holding.",
                        recordHarvestTask.getRecordId().toString());
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

    private RawRepoRecordHarvestTask unfoldRecordHarvestTask(RawRepoRecordHarvestTask recordHarvestTask, int agencyId) {
        RecordIdDTO recordId = recordHarvestTask.getRecordId();

        if (recordId.getAgencyId() == DBC_LIBRARY) {
            return unfoldTaskDBC(recordHarvestTask, agencyId);
        } else {
            return unfoldTaskUCSync(recordHarvestTask, agencyId);
        }
    }

    private RawRepoRecordHarvestTask unfoldTaskDBC(RawRepoRecordHarvestTask recordHarvestTask, int agencyId) {
        RecordIdDTO recordId = recordHarvestTask.getRecordId();
        boolean holdings = !holdingsItemsConnector.hasHoldings(recordId.getBibliographicRecordId(),
                Set.of(agencyId)).isEmpty();
        if (holdings) {
            return new RawRepoRecordHarvestTask()
                    .withRecordId(new RecordIdDTO(recordId.getBibliographicRecordId(), agencyId))
                    .withAddiMetaData(new AddiMetaData()
                            .withBibliographicRecordId(recordId.getBibliographicRecordId())
                            .withSubmitterNumber(agencyId));
        } else {
            return null;
        }
    }

    private RawRepoRecordHarvestTask unfoldTaskUCSync(RawRepoRecordHarvestTask recordHarvestTask, int agencyId){
        RecordIdDTO recordId = recordHarvestTask.getRecordId();
        if (recordId.getAgencyId() != agencyId) {
            LOGGER.info("{} not belonging to this batch (agencyId:{})", recordId, agencyId);
            return null;
        }
        final String bibliographicRecordId = recordHarvestTask.getRecordId().getBibliographicRecordId();
        boolean hasHolding = !holdingsItemsConnector.hasHoldings(bibliographicRecordId, new HashSet<>(Collections.singletonList(agencyId))).isEmpty();
        return hasHolding ? recordHarvestTask : null;
    }

    private int getFirstNonDBCAgencyIdFromQueue() throws HarvesterException {
        try {
            return rawRepoConnector.getFirstNonDBCAgencyIdOnQueue(config.getContent().getConsumerId());
        } catch (SQLException e) {
            throw new HarvesterException(e);
        }
    }
}
