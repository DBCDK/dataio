package dk.dbc.dataio.harvester.rr;

import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.RawRepoHarvesterConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import javax.annotation.Resource;
import javax.ejb.AsyncResult;
import javax.ejb.Asynchronous;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.SessionContext;
import javax.ejb.Singleton;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.concurrent.Future;

/**
 * This Enterprise Java Bean (EJB) handles an actual RawRepo harvest
 */
@Singleton
public class HarvesterBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(HarvesterBean.class);
    private static final String HARVESTER_MDC_KEY = "HARVESTER_ID";

    @Resource
    SessionContext sessionContext;

    /**
     * Executes harvest operation in batches (each batch in its own transactional
     * scope to avoid tearing down any controlling timers in case of an exception)
     * creating the corresponding jobs in the job-store if data is retrieved.
     * @param config harvest configuration
     * @return number of items harvested
     * @throws IllegalStateException on low-level binary file operation failure
     * @throws HarvesterException on failure to complete harvest operation
     */
    @Asynchronous
    @Lock(LockType.READ)
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public Future<Integer> harvest(RawRepoHarvesterConfig.Entry config) throws HarvesterException {
        LOGGER.debug("Called with config {}", config);
        try {
            MDC.put(HARVESTER_MDC_KEY, config.getId());
            final HarvesterBean businessObject = sessionContext.getBusinessObject(HarvesterBean.class);
            final HarvestOperation harvestOperation = getHarvestOperation(config);
            int itemsHarvested = 0, itemsInBatch;
            do {
                itemsHarvested += itemsInBatch = businessObject.execute(harvestOperation);
            } while (itemsInBatch == config.getBatchSize());
            return new AsyncResult<>(itemsHarvested);
        } finally {
            MDC.remove(HARVESTER_MDC_KEY);
        }
    }

    /**
     * Executes harvest operation
     * @param config harvest configuration
     * @return number of items harvested in batch
     * @throws IllegalStateException on low-level binary file operation failure
     * @throws HarvesterException on failure to complete harvest operation
     */
    @Lock(LockType.READ)
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public int execute(HarvestOperation harvestOperation) throws HarvesterException {
        return harvestOperation.execute();
    }

    /* Stand-alone method to enable easy injection during testing (via partial mocking)
     */
    public HarvestOperation getHarvestOperation(RawRepoHarvesterConfig.Entry config) {
        LOGGER.debug("Using rr resource {}", config.getResource());
        return new HarvestOperation();
    }
}
