package dk.dbc.dataio.harvester.rr;

import dk.dbc.dataio.commons.time.StopWatch;
import dk.dbc.dataio.commons.types.AddiMetaData;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.RRHarvesterConfig;
import dk.dbc.dataio.harvester.utils.rawrepo.RawRepoConnector;
import dk.dbc.phlog.PhLog;
import dk.dbc.phlog.dto.PhLogEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;

public class PhHarvestOperation extends HarvestOperation {

    private static final Logger LOGGER = LoggerFactory.getLogger(PhHarvestOperation.class);
    private final PhLog phLog;

    public PhHarvestOperation(RRHarvesterConfig config, HarvesterJobBuilderFactory harvesterJobBuilderFactory, EntityManager harvestTaskEntityManager, PhLog phLog)
            throws NullPointerException, IllegalArgumentException {
        this(config, harvesterJobBuilderFactory, harvestTaskEntityManager, null, null, phLog);
    }

    PhHarvestOperation(RRHarvesterConfig config, HarvesterJobBuilderFactory harvesterJobBuilderFactory, EntityManager harvestTaskEntityManager,
                       AgencyConnection agencyConnection, RawRepoConnector rawRepoConnector, PhLog phLog) {
        super(config, harvesterJobBuilderFactory, harvestTaskEntityManager, agencyConnection, rawRepoConnector);
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

            processRecordHarvestTask(recordHarvestTask);
            final PhLogEntry.Key key = new PhLogEntry.Key()
                    .withAgencyId(recordHarvestTask.getAddiMetaData().submitterNumber())
                    .withBibliographicRecordId(recordHarvestTask.getAddiMetaData().bibliographicRecordId());

            final PhLogEntry phLogEntry = phLog.getEntityManager().find(PhLogEntry.class, key);
            if(phLogEntry != null) {
                final AddiMetaData addiMetaData = recordHarvestTask.getAddiMetaData();
                addiMetaData.withDeleted(phLogEntry.getDeleted());
                addiMetaData.withHoldingsStatusMap(phLogEntry.getHoldingsStatusMap());
                recordHarvestTask.withAddiMetaData(addiMetaData);
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