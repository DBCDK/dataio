package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.jobstore.service.ejb.SweepMetrics.Repair;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.Tag;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SweepMetricsTest {
    @Test
    void countRepairs_repairsMade_areAddedUnderSweepAndActionTags() {
        SweepMetrics sweepMetrics = new SweepMetrics();
        sweepMetrics.metricRegistry = mock(MetricRegistry.class);
        Counter counter = mock(Counter.class);
        when(sweepMetrics.metricRegistry.counter(eq(SweepMetrics.REPAIRS),
                eq(new Tag("sweep", "recheck")), eq(new Tag("action", "gate_opened")))).thenReturn(counter);

        sweepMetrics.countRepairs(Repair.GATE_OPENED, 3);

        verify(counter).inc(3);
    }

    @Test
    void countRepairs_nothingRepaired_leavesTheCounterAlone() {
        SweepMetrics sweepMetrics = new SweepMetrics();
        sweepMetrics.metricRegistry = mock(MetricRegistry.class);

        sweepMetrics.countRepairs(Repair.GATE_OPENED, 0);

        verify(sweepMetrics.metricRegistry, never()).counter(anyString(), any(Tag[].class));
    }

    /**
     * Every series exists from startup, so the first repair after a restart is a step from 0 that
     * a rate can see.
     */
    @Test
    void registerCounters_registersEveryRepairAndTheCounterCorrections() {
        SweepMetrics sweepMetrics = new SweepMetrics();
        sweepMetrics.metricRegistry = mock(MetricRegistry.class);

        sweepMetrics.registerCounters(new Object());

        verify(sweepMetrics.metricRegistry, times(Repair.values().length))
                .counter(eq(SweepMetrics.REPAIRS), any(Tag.class), any(Tag.class));
        verify(sweepMetrics.metricRegistry).counter(eq(SweepMetrics.REPAIRS),
                eq(new Tag("sweep", "complete")), eq(new Tag("action", "job_completed")));
        verify(sweepMetrics.metricRegistry).counter(SweepMetrics.COUNTER_CORRECTIONS);
    }

    @Test
    void countCounterCorrections_correctionsMade_areAdded() {
        SweepMetrics sweepMetrics = new SweepMetrics();
        sweepMetrics.metricRegistry = mock(MetricRegistry.class);
        Counter counter = mock(Counter.class);
        when(sweepMetrics.metricRegistry.counter(SweepMetrics.COUNTER_CORRECTIONS)).thenReturn(counter);

        sweepMetrics.countCounterCorrections(4);

        verify(counter).inc(4);
    }
}
