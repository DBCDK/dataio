package dk.dbc.dataio.sink.batchexchange;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.EJB;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Startup;

/**
 * This enterprise Java bean represents periodic attempts at completing chunks as a result of finished batches
 * in the batch exchange system.
 */
@Singleton
@Startup
public class ScheduledBatchFinalizerBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScheduledBatchFinalizerBean.class);

    @EJB
    BatchFinalizerBean batchFinalizerBean;

    @Schedule(second = "*/5", minute = "*", hour = "*", persistent = false)
    public void run() {
        try {
            // Keep finalizing until we run out of completed batches.
            int numberOfBatchesCompleted = 0;
            while (batchFinalizerBean.finalizeNextCompletedBatch()) {
                numberOfBatchesCompleted++;
            }
            LOGGER.info("Finalized {} batches", numberOfBatchesCompleted);
        } catch (Exception e) {
            LOGGER.error("Exception caught during scheduled batch finalization", e);
        }
    }
}
