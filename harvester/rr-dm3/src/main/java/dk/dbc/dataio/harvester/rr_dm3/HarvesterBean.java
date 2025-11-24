package dk.dbc.dataio.harvester.rr_dm3;

import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.RRV3HarvesterConfig;
import jakarta.annotation.Resource;
import jakarta.ejb.AsyncResult;
import jakarta.ejb.Asynchronous;
import jakarta.ejb.EJB;
import jakarta.ejb.Lock;
import jakarta.ejb.LockType;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Singleton;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.Set;
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

    @EJB
    HarvestOperationFactoryBean harvestOperationFactory;

    @Inject
    @ConfigProperty (name = "EXCLUDED_HARVESTER_IDS", defaultValue = "")
    Set<Long> excludedHarvesterIds;

    /**
     * Executes harvest operation in batches (each batch in its own transactional
     * scope to avoid tearing down any controlling timers in case of an exception)
     * creating the corresponding jobs in the job-store if data is retrieved.
     *
     * @param config harvest configuration
     * @return number of items harvested
     * @throws IllegalStateException on low-level binary file operation failure
     * @throws HarvesterException    on failure to complete harvest operation
     */
    @Asynchronous
    @Lock(LockType.READ)
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public Future<Integer> harvest(RRV3HarvesterConfig config, String key) throws HarvesterException {
        boolean allowRun = !excludedHarvesterIds.contains(config.getId());
        LOGGER.debug("Called with config id: {}, and excludedHarvesterIds:{}", config.getId(), excludedHarvesterIds);
        if (allowRun) {
            try {
                MDC.put(HARVESTER_MDC_KEY, config.getContent().getId());
                final HarvesterBean businessObject = sessionContext.getBusinessObject(HarvesterBean.class);
                int itemsHarvested = businessObject.executeFor(config, key);
                return new AsyncResult<>(itemsHarvested);
            } finally {
                MDC.remove(HARVESTER_MDC_KEY);
            }
        } else {
            LOGGER.info("Config with id {} not allowed to run. ExcludedharvesterIds:{}", config.getId(), excludedHarvesterIds);
            return new AsyncResult<>(0);
        }
    }

    /**
     * Executes harvest operation
     *
     * @param config harvest configuration
     * @param key
     * @return number of items harvested in batch
     * @throws IllegalStateException on low-level binary file operation failure
     * @throws HarvesterException    on failure to complete harvest operation
     */
    @Lock(LockType.READ)
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public int executeFor(RRV3HarvesterConfig config, String key) throws HarvesterException {
        try (HarvestOperation harvestOperation = harvestOperationFactory.createFor(config, key)) {
            return harvestOperation.execute();
        }
    }
}
