package dk.dbc.dataio.jobstore.service.ejb;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.Tag;
import org.eclipse.microprofile.metrics.Timer;

/**
 * Counts what the recovery sweeps repair and times how long they run.
 * <p>
 * Every sweep fixes something the primary path should already have done, so in a healthy system
 * every repair count stays at 0. A repair counted here says which part of the primary path failed.
 * The chunks and jobs involved are named in the log lines the sweeps already write, and are kept
 * out of the tags so the series stay few and fixed.
 * <p>
 * Every counter is registered at 0 when the application starts. A series that first appears
 * already at 1 has no earlier sample to rate against, so Prometheus would never see the first
 * repair after a restart, and for the repairs that should never happen the first is the one that
 * matters. Registering at startup also gives the dashboard a 0 to show instead of no data.
 * <p>
 * Durations are taken in the method body rather than with {@code @Timed}, because the sweeps run as
 * EJB timer callbacks, which pass through {@code @AroundTimeout} interceptors and not through the
 * {@code @AroundInvoke} interceptor the annotation binds.
 */
@ApplicationScoped
public class SweepMetrics {
    static final String REPAIRS = "dataio_sweep_repairs";
    static final String DURATION = "dataio_sweep_duration";
    static final String COUNTER_CORRECTIONS = "dataio_sink_status_counter_drift";

    public enum Sweep {
        /**
         * {@code AdminBean.updateStaleChunks}, once a minute. Looks at chunks that have sat too long
         * in a status something else was supposed to move them out of: {@code READY_*}, where the
         * dispatch attempt that should have followed never ran, and {@code QUEUED_*}, where the chunk
         * was sent to the job processor or the sink and no completion came back within the timeout.
         * It moves stranded {@code READY_*} chunks to {@code SCHEDULED_*}, re-runs completions whose
         * work is already recorded, resends the rest up to the retry limit, and reports those past
         * it. Its repairs are counted per chunk.
         */
        STALE("stale"),
        /**
         * {@code JobSchedulerBulkSubmitterBean.sweepSinksWithParkedChunks}, once a minute. A chunk in
         * {@code SCHEDULED_*} is parked rather than stale: it waits for queue capacity, however long
         * that takes. The once-a-second submitters that dispatch parked chunks find their sinks in
         * the in-memory sink chunk counts, so a sink whose count lost a delta is never dispatched
         * for. This sweep asks the table which sinks hold parked chunks and dispatches for all of
         * them. Its repairs are counted per sink the counts had missed, not per chunk.
         */
        PARKED("parked"),
        /**
         * {@code AdminBean.recheckBlocks}, hourly. Drops the rows of jobs that are gone or already
         * completed, lifts barriers and opens gates that the delivery path failed to, and recounts
         * the sink chunk counts.
         */
        RECHECK("recheck"),
        /**
         * {@code AdminBean.completeFinishedJobs}, hourly. Marks jobs completed whose chunks have all
         * completed while the job itself was never marked.
         */
        COMPLETE("complete");

        private final String tag;

        Sweep(String tag) {
            this.tag = tag;
        }
    }

    public enum Repair {
        READY_RESCUED_PROCESSING(Sweep.STALE, "ready_rescued_processing"),
        READY_RESCUED_DELIVERY(Sweep.STALE, "ready_rescued_delivery"),
        PHASE_ADVANCED(Sweep.STALE, "phase_advanced"),
        RESENT(Sweep.STALE, "resent"),
        RETRIES_EXHAUSTED(Sweep.STALE, "retries_exhausted"),
        SINK_MISSING_FROM_COUNTS_PROCESSING(Sweep.PARKED, "sink_missing_from_counts_processing"),
        SINK_MISSING_FROM_COUNTS_DELIVERY(Sweep.PARKED, "sink_missing_from_counts_delivery"),
        ROWS_DROPPED(Sweep.RECHECK, "rows_dropped"),
        BARRIER_LIFTED(Sweep.RECHECK, "barrier_lifted"),
        GATE_OPENED(Sweep.RECHECK, "gate_opened"),
        JOB_COMPLETED(Sweep.COMPLETE, "job_completed");

        private final Sweep sweep;
        private final String action;

        Repair(Sweep sweep, String action) {
            this.sweep = sweep;
            this.action = action;
        }
    }

    @Inject
    MetricRegistry metricRegistry;

    /**
     * Registers every counter at 0 as soon as the application has started.
     * <p>
     * Observing the application scope's initialisation is what makes this run at deployment. The
     * bean is otherwise created on the first call a sweep makes, which is after the first repair
     * it would need to have been registered for.
     *
     * @param ignored the event payload, which carries nothing needed here
     */
    void registerCounters(@Observes @Initialized(ApplicationScoped.class) Object ignored) {
        for (Repair repair : Repair.values()) {
            repairs(repair);
        }
        metricRegistry.counter(COUNTER_CORRECTIONS);
    }

    /**
     * Adds a sweep's repairs of one kind to the {@code dataio_sweep_repairs} counter.
     *
     * @param repair what was repaired
     * @param count  how many were repaired
     */
    public void countRepairs(Repair repair, int count) {
        if (count > 0) {
            repairs(repair).inc(count);
        }
    }

    /**
     * Adds the sink and status pairs the hourly recount found the sink chunk counts had wrong to
     * the {@code dataio_sink_status_counter_drift} counter.
     *
     * @param corrected how many pairs the recount corrected
     */
    public void countCounterCorrections(int corrected) {
        if (corrected > 0) {
            metricRegistry.counter(COUNTER_CORRECTIONS).inc(corrected);
        }
    }

    private Counter repairs(Repair repair) {
        return metricRegistry.counter(REPAIRS, new Tag("sweep", repair.sweep.tag), new Tag("action", repair.action));
    }

    /**
     * Starts timing one run of a sweep, recorded in the {@code dataio_sweep_duration} timer when
     * the returned context is closed.
     *
     * @param sweep sweep being run
     * @return context to close when the run ends
     */
    public Timer.Context time(Sweep sweep) {
        return metricRegistry.timer(DURATION, new Tag("sweep", sweep.tag)).time();
    }
}
