package dk.dbc.dataio.harvester.rr;

import dk.dbc.dataio.commons.time.StopWatch;
import dk.dbc.dataio.commons.types.AddiMetaData;
import dk.dbc.dataio.harvester.task.TaskRepo;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.RRHarvesterConfig;
import dk.dbc.dataio.harvester.utils.rawrepo.RawRepoConnector;
import dk.dbc.phlog.PhLog;
import dk.dbc.phlog.dto.PhLogEntry;
import dk.dbc.rawrepo.RecordServiceConnector;
import dk.dbc.rawrepo.queue.ConfigurationException;
import dk.dbc.rawrepo.queue.QueueException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;

public class PhHarvestOperation extends HarvestOperation {

    private static final Logger LOGGER = LoggerFactory.getLogger(PhHarvestOperation.class);
    private final PhLog phLog;

    public PhHarvestOperation(RRHarvesterConfig config,
            HarvesterJobBuilderFactory harvesterJobBuilderFactory,
            TaskRepo taskRepo, String openAgencyEndpoint, PhLog phLog)
            throws NullPointerException, IllegalArgumentException, SQLException, QueueException, ConfigurationException {
        this(config, harvesterJobBuilderFactory, taskRepo,
            new AgencyConnection(openAgencyEndpoint), null, phLog);
    }

    PhHarvestOperation(RRHarvesterConfig config, HarvesterJobBuilderFactory harvesterJobBuilderFactory, TaskRepo taskRepo,
                       AgencyConnection agencyConnection, RawRepoConnector rawRepoConnector, PhLog phLog)
            throws SQLException, QueueException, ConfigurationException {
        super(config, harvesterJobBuilderFactory, taskRepo, agencyConnection, rawRepoConnector);
        this.phLog = phLog;
    }

    PhHarvestOperation(RRHarvesterConfig config, HarvesterJobBuilderFactory harvesterJobBuilderFactory, TaskRepo taskRepo,
                       AgencyConnection agencyConnection, RawRepoConnector rawRepoConnector, PhLog phLog, RecordServiceConnector recordServiceConnector)
            throws SQLException, QueueException, ConfigurationException {
        super(config, harvesterJobBuilderFactory, taskRepo, agencyConnection, rawRepoConnector, recordServiceConnector);
        this.phLog = phLog;
    }

    /**
     * Runs this harvest operation, creating dataIO jobs from harvested records.
     * If any non-internal error occurs a record is marked as failed.
     * @return number of records processed
     * @throws HarvesterException on failure to complete harvest operation
     */
    @Override
    public int execute() throws HarvesterException {
        final StopWatch stopWatch = new StopWatch();
        final RecordHarvestTaskQueue recordHarvestTaskQueue = createTaskQueue();
        // Since we might (re)run batches with a size larger than the one currently configured
        final int batchSize = Math.max(configContent.getBatchSize(), recordHarvestTaskQueue.estimatedSize());

        int itemsProcessed = 0;
        RawRepoRecordHarvestTask recordHarvestTask = recordHarvestTaskQueue.poll();
        while (recordHarvestTask != null) {
            LOGGER.info("{} ready for harvesting", recordHarvestTask.getRecordId());

            final PhLogEntry.Key key = new PhLogEntry.Key()
                    .withAgencyId(recordHarvestTask.getAddiMetaData().submitterNumber())
                    .withBibliographicRecordId(recordHarvestTask.getAddiMetaData().bibliographicRecordId());

            final PhLogEntry phLogEntry = phLog.getEntityManager().find(PhLogEntry.class, key);
            if (phLogEntry != null) {
                final AddiMetaData addiMetaData = recordHarvestTask.getAddiMetaData();
                phLog.getEntityManager().refresh(phLogEntry);
                addiMetaData.withDeleted(phLogEntry.getDeleted());
                addiMetaData.withHoldingsStatusMap(phLogEntry.getHoldingsStatusMap());
                processRecordHarvestTask(recordHarvestTask.withAddiMetaData(addiMetaData));
            } else {
                LOGGER.info("Record {} has no entry in PH log", recordHarvestTask.getRecordId());
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
}
