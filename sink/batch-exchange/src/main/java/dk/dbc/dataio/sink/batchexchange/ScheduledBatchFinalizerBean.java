package dk.dbc.dataio.sink.batchexchange;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.EJB;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

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
    private static final Duration LIVENESS_THRESHOLD = Duration.ofMinutes(5);
    private static final AtomicReference<Instant> LAST_RUN = new AtomicReference<>(Instant.now());


    @Schedule(second = "*/5", minute = "*", hour = "*", persistent = false)
    public void run() {
        try {
            // Keep finalizing until we run out of completed batches.
            int numberOfBatchesCompleted = 0;
            while (batchFinalizerBean.finalizeNextCompletedBatch()) {
                numberOfBatchesCompleted++;
            }
            LOGGER.info("Finalized {} batches", numberOfBatchesCompleted);
            LAST_RUN.set(Instant.now());
        } catch (Exception e) {
            LOGGER.error("Exception caught during scheduled batch finalization", e);
        }
    }

    protected Instant getLastRun() {
        return LAST_RUN.get();
    }

    public boolean isDown() {
        return getLastRun().plus(LIVENESS_THRESHOLD).isBefore(Instant.now());
    }
}
