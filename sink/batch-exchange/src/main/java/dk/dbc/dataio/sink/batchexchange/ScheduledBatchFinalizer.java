package dk.dbc.dataio.sink.batchexchange;

import dk.dbc.dataio.jse.artemis.common.Health;
import dk.dbc.dataio.jse.artemis.common.service.HealthService;
import dk.dbc.dataio.jse.artemis.common.service.ServiceHub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * This enterprise Java bean represents periodic attempts at completing chunks as a result of finished batches
 * in the batch exchange system.
 */
public class ScheduledBatchFinalizer {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScheduledBatchFinalizer.class);

    private final BatchFinalizer batchFinalizer;
    private static final Duration LIVENESS_THRESHOLD = Duration.ofMinutes(5);
    private static final AtomicReference<Instant> LAST_RUN = new AtomicReference<>(Instant.now());
    private final HealthService healthService;
    private final AtomicInteger THREAD_ID = new AtomicInteger();

    public ScheduledBatchFinalizer(ServiceHub serviceHub, EntityManager entityManager) {
        batchFinalizer = new BatchFinalizer(entityManager, serviceHub.jobStoreServiceConnector);
        healthService = serviceHub.healthService;
        serviceHub.zombieWatch.addCheck("batch-finalizer", this::healthcheck);
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "batch-finalizer-" + THREAD_ID.getAndIncrement()));
        scheduler.scheduleAtFixedRate(this::run, 30, 5, TimeUnit.SECONDS);
        Runtime.getRuntime().addShutdownHook(new Thread(scheduler::shutdown));
    }

    public void run() {
        try {
            // Keep finalizing until we run out of completed batches.
            int numberOfBatchesCompleted = 0;
            while (batchFinalizer.finalizeNextCompletedBatch()) {
                numberOfBatchesCompleted++;
            }
            if(numberOfBatchesCompleted > 0) LOGGER.info("Finalized {} batches", numberOfBatchesCompleted);
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

    private void healthcheck() {
        if(isDown()) healthService.signal(FinalizerHealth.STUCK);
    }

    public enum FinalizerHealth implements Health {
        STUCK;

        @Override
        public int getStatusCode() {
            return 600;
        }

        @Override
        public String getMessage() {
            return "Batch finalizer is stuck";
        }
    }
}
