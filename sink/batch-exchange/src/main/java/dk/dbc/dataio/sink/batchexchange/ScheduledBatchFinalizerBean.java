package dk.dbc.dataio.sink.batchexchange;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Schedule;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

/**
 * This enterprise Java bean represents periodic attempts at completing chunks as a result of finished batches
 * in the batch exchange system.
 */
public class ScheduledBatchFinalizerBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScheduledBatchFinalizerBean.class);

    private final BatchFinalizer batchFinalizer;
    private static final Duration LIVENESS_THRESHOLD = Duration.ofMinutes(5);
    private static final AtomicReference<Instant> LAST_RUN = new AtomicReference<>(Instant.now());

    public ScheduledBatchFinalizerBean(BatchFinalizer batchFinalizer) {
        this.batchFinalizer = batchFinalizer;

    }

    @Schedule(second = "*/5", minute = "*", hour = "*", persistent = false)
    public void run() {
        try {
            // Keep finalizing until we run out of completed batches.
            int numberOfBatchesCompleted = 0;
            while (batchFinalizer.finalizeNextCompletedBatch()) {
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
