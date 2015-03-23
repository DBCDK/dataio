package dk.dbc.dataio.sink.es;

import javax.annotation.PostConstruct;
import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.DependsOn;
import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

/**
 * This Enterprise Java Bean (EJB) singleton is used to ensure that
 * an ES database is not pushed beyond its handling capacity.
 */
@LocalBean
@Singleton
@Startup
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
@DependsOn("EsSinkConfigurationBean")
public class EsThrottlerBean {
    @EJB
    EsSinkConfigurationBean configuration;

    private int availableRecordSlots;

    /**
     * Initializes this bean with a number of available record slots
     */
    @PostConstruct
    public void initialize() {
        // Maximum number of records to be "in-flight" in ES database is read from configuration.
        // Be advised that this is not a hard limit, calling releaseRecordSlots
        // with a value larger than this value is possible but not advisable.
        availableRecordSlots = configuration.getRecordsCapacity();
    }

    /**
     * Acquires the given number of record slots, blocking until all are
     * available, or the thread is interrupted
     * @param numOfSlots number of record slots to acquire
     */
    public boolean acquireRecordSlots(int numOfSlots) {
        boolean acquired = false;
        if (availableRecordSlots >= numOfSlots) {
            availableRecordSlots -= numOfSlots;
            acquired = true;
        }
        return acquired;
    }

    /**
     * Releases the given number of record slots, increasing the number of
     * available slots by that same amount
     * @param numOfSlots number of record slots to release
     */
    public void releaseRecordSlots(int numOfSlots) {
        availableRecordSlots += numOfSlots;
    }

    /* This method was added for testing purposes
     */
    int getAvailableSlots() {
        return availableRecordSlots;
    }
}
