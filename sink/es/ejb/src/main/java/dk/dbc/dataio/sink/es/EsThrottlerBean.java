package dk.dbc.dataio.sink.es;

import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;

import javax.annotation.PostConstruct;
import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.LocalBean;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import java.util.concurrent.Semaphore;

/**
 * This Enterprise Java Bean (EJB) singleton is used to ensure that
 * an ES database is not pushed beyond its handling capacity.
 *
 * It does this internally by using a semaphore representing a number
 * of available record slots. Threads must then acquire their
 * required number of record slots through calls of acquireRecordSlots(),
 * therefore guaranteeing that the ES database is not swamped. When
 * ES processing is complete releaseRecordSlots() must likewise be
 * called.
 */
@LocalBean
@Singleton
@Startup
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
public class EsThrottlerBean {
    // Maximum number of records to be "in-flight" in ES database.
    // Be advised that this is not a hard limit, calling releaseRecordSlots
    // with a value larger than RECORDS_CAPACITY is possible but not advisable.
    // ToDo: should be made configurable
    static final int RECORDS_CAPACITY = 100000;

    private static final XLogger LOGGER = XLoggerFactory.getXLogger(EsThrottlerBean.class);

    private Semaphore availableRecordSlots;

    /**
     * Initializes this bean with a number of available record slots
     */
    @PostConstruct
    public void initialize() {
        availableRecordSlots = new Semaphore(RECORDS_CAPACITY, true);
    }

    /**
     * Acquires the given number of record slots, blocking until all are
     * available, or the thread is interrupted
     *
     * @param numOfSlots number of record slots to acquire
     *
     * @throws IllegalArgumentException if numOfSlots is negative
     * @throws InterruptedException if the current thread is interrupted
     */
    public void acquireRecordSlots(int numOfSlots) throws IllegalArgumentException, InterruptedException {
        LOGGER.entry(numOfSlots);
        try {
            availableRecordSlots.acquire(numOfSlots);
        } finally {
            LOGGER.exit();
        }
    }

    /**
     * Releases the given number of record slots, increasing the number of
     * available slots by that same amount
     *
     * @param numOfSlots number of record slots to release
     *
     * @throws IllegalArgumentException if numOfSlots is negative
     */
    public void releaseRecordSlots(int numOfSlots) {
        LOGGER.entry(numOfSlots);
        try {
            availableRecordSlots.release(numOfSlots);
        } finally {
            LOGGER.exit();
        }
    }
}
