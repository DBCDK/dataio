package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.common.utils.flowstore.ejb.FlowStoreServiceConnectorBean;
import dk.dbc.dataio.jobstore.service.AbstractJobStoreIT;
import dk.dbc.dataio.jobstore.service.entity.ChunkEntity;
import dk.dbc.dataio.jobstore.service.entity.FlowCacheEntity;
import dk.dbc.dataio.jobstore.service.entity.ItemEntity;
import dk.dbc.dataio.jobstore.service.entity.JobEntity;
import dk.dbc.dataio.jobstore.service.entity.JobQueueEntity;
import dk.dbc.dataio.jobstore.service.entity.RerunEntity;
import dk.dbc.dataio.jobstore.service.entity.SinkCacheEntity;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.junit.Before;
import org.junit.Test;

import javax.ejb.ScheduleExpression;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BootstrapBeanIT extends AbstractJobStoreIT {
    private final TimerService timerService = mock(TimerService.class);
    private final Timer timer = mock(Timer.class);

    @Before
    public void setupMocks() {
        when(timerService.createCalendarTimer(any(ScheduleExpression.class), any(TimerConfig.class)))
                .thenReturn(timer);
    }

    /**
     * Given: a job queue with multiple entries marked as in-progress
     * When : job-store bootstrap process is executed
     * Then : jobs linked to in-progress queue entries are reset
     * And  : in-progress queue entries are updated to waiting state
     */
    @Test
    public void initialize_resetsJobsInterruptedDuringPartitioning() throws FlowStoreServiceConnectorException {
        // Given...
        final JobEntity job1 = newPersistedJobEntity();
        final JobEntity job2 = newPersistedJobEntity();
        final JobEntity job3 = newPersistedJobEntity();
        final FlowCacheEntity cachedFlow = newPersistedFlowCacheEntity();
        final SinkCacheEntity cachedSink = newPersistedSinkCacheEntity();
        final JobQueueEntity job1QueueEntry = newPersistedJobQueueEntity(job1);
        final JobQueueEntity job2QueueEntry = newPersistedJobQueueEntity(job2);
        newPersistedChunkEntity(new ChunkEntity.Key(0, job1.getId()));
        newPersistedChunkEntity(new ChunkEntity.Key(0, job2.getId()));
        newPersistedChunkEntity(new ChunkEntity.Key(0, job3.getId()));
        newPersistedItemEntity(new ItemEntity.Key(job1.getId(), 0, (short) 0));
        newPersistedItemEntity(new ItemEntity.Key(job2.getId(), 0, (short) 0));
        newPersistedItemEntity(new ItemEntity.Key(job3.getId(), 0, (short) 0));

        persistenceContext.run(() -> {
            job1.setCachedFlow(cachedFlow);
            job2.setCachedFlow(cachedFlow);
            job3.setCachedFlow(cachedFlow);
            job1.setCachedSink(cachedSink);
            job2.setCachedSink(cachedSink);
            job3.setCachedSink(cachedSink);
            job1QueueEntry.withState(JobQueueEntity.State.IN_PROGRESS);
            job2QueueEntry.withState(JobQueueEntity.State.IN_PROGRESS);
        });

        // When...
        BootstrapBean bootstrapBean = newBootstrapBean();
        persistenceContext.run(bootstrapBean::initialize);

        // Then...
        final List<ChunkEntity> remainingChunks = findAllChunks();
        assertThat("Number of remaining chunks", remainingChunks.size(), is(3));

        final List<ItemEntity> remainingItems = findAllItems();
        assertThat("Number of remaining items", remainingItems.size(), is(3));

        // And...
        assertThat("Number of in-progress entries on job queue",
                bootstrapBean.jobQueueRepository.getInProgress().size(), is(0));
    }

    /**
     * Given: a rerun queue with entry marked as in-progress
     * When : job-store bootstrap process is executed
     * Then : in-progress queue entry is reset to waiting state
     */
    @Test
    public void initialize_resetsInterruptedRerunTasks() throws FlowStoreServiceConnectorException {
        // Given
        final JobEntity job = newPersistedJobEntity();
        final RerunEntity rerun = newPersistedRerunEntity(job);

        persistenceContext.run(() -> rerun.withState(RerunEntity.State.IN_PROGRESS));

        // When...
        final BootstrapBean bootstrapBean = newBootstrapBean();
        persistenceContext.run(bootstrapBean::initialize);

        // Then
        assertThat("Number of in-progress entries on rerun queue",
                bootstrapBean.rerunsRepository.getInProgress().size(), is(0));
    }

    private BootstrapBean newBootstrapBean() throws FlowStoreServiceConnectorException {
        final BootstrapBean bootstrapBean = new BootstrapBean();
        bootstrapBean.jobQueueRepository = newJobQueueRepository();
        bootstrapBean.jobSchedulerBean = newJobSchedulerBean();
        bootstrapBean.rerunsRepository = newRerunsRepository();
        bootstrapBean.timerService = timerService;
        bootstrapBean.jobSchedulerBean.jobSchedulerTransactionsBean =
                mock(JobSchedulerTransactionsBean.class);
        bootstrapBean.jobSchedulerBean.metricRegistry =
                mock(MetricRegistry.class);
        bootstrapBean.jobSchedulerBean.flowStore =
                mock(FlowStoreServiceConnectorBean.class);
        FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        when(bootstrapBean.jobSchedulerBean.flowStore.getConnector()).thenReturn(flowStoreServiceConnector);
        when(bootstrapBean.jobSchedulerBean.flowStore.getConnector().findAllSinks()).thenReturn(Collections.emptyList());
        return bootstrapBean;
    }
}
