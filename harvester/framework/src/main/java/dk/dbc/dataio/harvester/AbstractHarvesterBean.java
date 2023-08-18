package dk.dbc.dataio.harvester;

import dk.dbc.dataio.harvester.types.HarvesterConfig;
import dk.dbc.dataio.harvester.types.HarvesterException;
import jakarta.annotation.Resource;
import jakarta.ejb.AsyncResult;
import jakarta.ejb.Asynchronous;
import jakarta.ejb.Lock;
import jakarta.ejb.LockType;
import jakarta.ejb.SessionContext;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import org.slf4j.Logger;
import org.slf4j.MDC;

import java.util.concurrent.Future;

/**
 * Abstract base class for harvesters
 *
 * @param <T> type parameter (recursive) for AbstractHarvesterBean implementation
 * @param <U> type parameter for harvester configuration type
 */
public abstract class AbstractHarvesterBean<T extends AbstractHarvesterBean<T, U>, U extends HarvesterConfig<?>> {
    private static final String HARVESTER_MDC_KEY = "HARVESTER_ID";

    @Resource
    public SessionContext sessionContext;

    /**
     * Executes harvest operation in batches (each batch in its own transactional
     * scope to avoid tearing down any controlling timers in case of an exception)
     *
     * @param config harvest configuration
     * @return number of items harvested
     * @throws HarvesterException on failure to complete harvest operation
     */
    @Asynchronous
    @Lock(LockType.READ)
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public Future<Integer> harvest(U config) throws HarvesterException {
        getLogger().debug("Called with config {}", config);
        try {
            MDC.put(HARVESTER_MDC_KEY, config.getLogId());
            int itemsHarvested = self().executeFor(config);
            return new AsyncResult<>(itemsHarvested);
        } finally {
            MDC.remove(HARVESTER_MDC_KEY);
        }
    }

    /**
     * Executes harvest operation
     *
     * @param config harvest configuration
     * @return number of items harvested in batch
     * @throws HarvesterException on failure to complete harvest operation
     */
    @Lock(LockType.READ)
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public abstract int executeFor(U config) throws HarvesterException;

    /**
     * @return reference to business interface of this AbstractHarvesterBean implementation
     */
    public abstract T self();

    /**
     * @return Logger
     */
    public abstract Logger getLogger();
}
