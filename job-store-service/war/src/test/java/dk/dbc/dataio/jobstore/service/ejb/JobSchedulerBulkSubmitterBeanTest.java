package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.jobstore.service.dependencytracking.DependencyTrackingService;
import dk.dbc.dataio.jobstore.service.ejb.SweepMetrics.Repair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus.SCHEDULED_FOR_DELIVERY;
import static dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus.SCHEDULED_FOR_PROCESSING;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JobSchedulerBulkSubmitterBeanTest {
    private final JobSchedulerBulkSubmitterBean bean = new JobSchedulerBulkSubmitterBean();

    @BeforeEach
    void setUp() {
        bean.dependencyTrackingService = mock(DependencyTrackingService.class);
        bean.sweepMetrics = mock(SweepMetrics.class);
    }

    /**
     * A sink the table shows parked chunks for while the counts show none is one the per-second
     * sweeps had stopped dispatching for.
     */
    @Test
    void reportSinksMissingFromCounts_sinkMissingFromTheCounts_isCounted() {
        when(bean.dependencyTrackingService.getActiveSinks(SCHEDULED_FOR_DELIVERY)).thenReturn(Set.of(1));

        bean.reportSinksMissingFromCounts(Set.of(1, 2, 3), SCHEDULED_FOR_DELIVERY);

        verify(bean.sweepMetrics).countRepairs(Repair.SINK_MISSING_FROM_COUNTS_DELIVERY, 2);
    }

    @Test
    void reportSinksMissingFromCounts_processingPhase_isCountedUnderProcessing() {
        when(bean.dependencyTrackingService.getActiveSinks(SCHEDULED_FOR_PROCESSING)).thenReturn(Set.of());

        bean.reportSinksMissingFromCounts(Set.of(4), SCHEDULED_FOR_PROCESSING);

        verify(bean.sweepMetrics).countRepairs(Repair.SINK_MISSING_FROM_COUNTS_PROCESSING, 1);
    }

    @Test
    void reportSinksMissingFromCounts_countsAgreeWithTheTable_nothingIsCounted() {
        when(bean.dependencyTrackingService.getActiveSinks(SCHEDULED_FOR_DELIVERY)).thenReturn(Set.of(1, 2));

        bean.reportSinksMissingFromCounts(Set.of(1, 2), SCHEDULED_FOR_DELIVERY);

        verify(bean.sweepMetrics, never()).countRepairs(any(), anyInt());
    }
}
