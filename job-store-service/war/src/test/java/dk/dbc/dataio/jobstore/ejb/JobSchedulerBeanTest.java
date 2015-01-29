package dk.dbc.dataio.jobstore.ejb;

import dk.dbc.dataio.jobstore.types.Chunk;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.jobstore.types.ChunkBuilder;
import dk.dbc.dataio.commons.utils.test.model.SinkBuilder;
import dk.dbc.dataio.jobstore.JobStore;
import dk.dbc.dataio.jobstore.ejb.monitoring.SequenceAnalyserMonitorBean;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import dk.dbc.dataio.jobstore.types.monitoring.SequenceAnalyserMonitorMXBean;
import dk.dbc.dataio.jobstore.types.monitoring.SequenceAnalyserMonitorSample;
import dk.dbc.dataio.sequenceanalyser.SequenceAnalyser;
import dk.dbc.dataio.sequenceanalyser.ChunkIdentifier;
import dk.dbc.dataio.sequenceanalyser.CollisionDetectionElement;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class JobSchedulerBeanTest {
    private final SequenceAnalyser sequenceAnalyser = mock(SequenceAnalyser.class);
    private final JobProcessorMessageProducerBean jobProcessorMessageProducerBean = mock(JobProcessorMessageProducerBean.class);
    private final JobStoreBean jobStoreBean = mock(JobStoreBean.class);
    private final SequenceAnalyserMonitorBean sequenceAnalyserMonitorBean = mock(SequenceAnalyserMonitorBean.class);
    private final JobStore jobStore = mock(JobStore.class);
    private final Chunk chunk = new ChunkBuilder().build();
    private final ChunkIdentifier chunkIdentifier = new ChunkIdentifier(chunk.getJobId(), chunk.getChunkId());
    private final Sink sink = new SinkBuilder().build();

    private final SequenceAnalyserMonitorMXBean sequenceAnalyserMonitorMXBean = new SequenceAnalyserMonitorMXBean() {
        private SequenceAnalyserMonitorSample sample;
        @Override
        public SequenceAnalyserMonitorSample getSample() {
            return sample;
        }
        @Override
        public void setSample(SequenceAnalyserMonitorSample sample) {
            this.sample = sample;
        }
    };

    private final ConcurrentHashMap<String, SequenceAnalyserMonitorMXBean> mBeans = new ConcurrentHashMap<>();
    {
        sequenceAnalyserMonitorMXBean.setSample(new SequenceAnalyserMonitorSample(1, 0));
        mBeans.put(sink.getContent().getName(), sequenceAnalyserMonitorMXBean);
    }

    @Before
    public void setupExpectations() {
        doNothing().when(sequenceAnalyser).addChunk(any(CollisionDetectionElement.class));
        when(sequenceAnalyser.getInactiveIndependentChunks()).thenReturn(Collections.<ChunkIdentifier>emptyList());
        when(jobStoreBean.getJobStore()).thenReturn(jobStore);
        when(sequenceAnalyserMonitorBean.getMBeans()).thenReturn(mBeans);
    }

    @Test(expected = NullPointerException.class)
    public void scheduleChunk_chunkArgIsNull_throws() throws JobStoreException {
        getJobSchedulerBean().scheduleChunk(null, sink);
    }

    @Test(expected = NullPointerException.class)
    public void scheduleChunk_sinkArgIsNull_throws() throws JobStoreException {
        getJobSchedulerBean().scheduleChunk(chunk, null);
    }

    @Test
    public void scheduleChunk_givenValidChunk_addsChunkToSequenceAnalysis() throws JobStoreException {
        JobSchedulerBean jobSchedulerBean = getJobSchedulerBean();
        jobSchedulerBean = injectSequenceAnalyserComposite(jobSchedulerBean);
        jobSchedulerBean.scheduleChunk(chunk, sink);
        verify(sequenceAnalyser).addChunk(any(CollisionDetectionElement.class));
    }

    @Test
    public void scheduleChunk_givenValidChunk_notifiesPipelineOfNextAvailableWorkload() throws JobStoreException {
        final List<ChunkIdentifier> identifiers = new ArrayList<>(Arrays.asList(chunkIdentifier, chunkIdentifier));
        when(sequenceAnalyser.getInactiveIndependentChunks()).thenReturn(identifiers);
        when(jobStore.getChunk(chunk.getJobId(), chunk.getChunkId())).thenReturn(chunk);
        doNothing().when(jobProcessorMessageProducerBean).send(chunk);

        JobSchedulerBean jobSchedulerBean = getJobSchedulerBean();
        jobSchedulerBean = injectSequenceAnalyserComposite(jobSchedulerBean);
        jobSchedulerBean.scheduleChunk(chunk, sink);
        verify(sequenceAnalyser).getInactiveIndependentChunks();
        verify(jobStore, times(identifiers.size())).getChunk(chunk.getJobId(), chunk.getChunkId());
        verify(jobProcessorMessageProducerBean, times(identifiers.size())).send(any(Chunk.class));
    }

    @Test
    public void scheduleChunk_onFailureToRetrieveChunkForWorkload_proceedsToNextWorkloadItem() throws JobStoreException {
        final List<ChunkIdentifier> identifiers = new ArrayList<>(Arrays.asList(chunkIdentifier, chunkIdentifier, chunkIdentifier, chunkIdentifier));
        when(sequenceAnalyser.getInactiveIndependentChunks()).thenReturn(identifiers);
        when(jobStore.getChunk(chunk.getJobId(), chunk.getChunkId()))
                .thenReturn(chunk)
                .thenThrow(new JobStoreException("died in getChunk()"))
                .thenReturn(chunk)
                .thenReturn(chunk);
        doNothing().when(jobProcessorMessageProducerBean).send(chunk);

        JobSchedulerBean jobSchedulerBean = getJobSchedulerBean();
        jobSchedulerBean = injectSequenceAnalyserComposite(jobSchedulerBean);
        jobSchedulerBean.scheduleChunk(chunk, sink);
        verify(sequenceAnalyser).getInactiveIndependentChunks();
        verify(jobStore, times(identifiers.size())).getChunk(chunk.getJobId(), chunk.getChunkId());
        verify(jobProcessorMessageProducerBean, times(identifiers.size()-1)).send(any(Chunk.class));
    }

    @Test
    public void scheduleChunk_onFailureToSendChunkNotificationForWorkload_proceedsToNextWorkloadItem() throws JobStoreException {
        final List<ChunkIdentifier> identifiers = new ArrayList<>(Arrays.asList(chunkIdentifier, chunkIdentifier, chunkIdentifier, chunkIdentifier));
        when(sequenceAnalyser.getInactiveIndependentChunks()).thenReturn(identifiers);
        when(jobStore.getChunk(chunk.getJobId(), chunk.getChunkId())).thenReturn(chunk);
        doNothing().
        doThrow(new JobStoreException("died in send()")).
        doNothing().
        doNothing().
                when(jobProcessorMessageProducerBean).send(chunk);

        JobSchedulerBean jobSchedulerBean = getJobSchedulerBean();
        jobSchedulerBean = injectSequenceAnalyserComposite(jobSchedulerBean);
        jobSchedulerBean.scheduleChunk(chunk, sink);
        verify(sequenceAnalyser).getInactiveIndependentChunks();
        verify(jobStore, times(identifiers.size())).getChunk(chunk.getJobId(), chunk.getChunkId());
        verify(jobProcessorMessageProducerBean, times(identifiers.size())).send(any(Chunk.class));
    }

    @Test
    public void scheduleChunk_givenChunkFromNeverBeforeSeenSink_createsSequenceAnalyserAndMaintainsToSinkMapping() throws JobStoreException {
        final JobSchedulerBean jobSchedulerBean = getJobSchedulerBean();
        jobSchedulerBean.scheduleChunk(chunk, sink);
        assertThat(jobSchedulerBean.sequenceAnalysers.size(), is(1));
        assertThat(jobSchedulerBean.sequenceAnalysers.get(jobSchedulerBean.getLockObject(String.valueOf(sink.getId()))), is(notNullValue()));
        assertThat(jobSchedulerBean.toSinkMapping.size(), is(1));
        assertThat(jobSchedulerBean.toSinkMapping.get(new ChunkIdentifier(chunk.getJobId(), chunk.getChunkId())), is(sink));
    }

    @Test
    public void scheduleChunk_sequenceAnalyserMonitorBeanThrowsIllegalStateException_throws() throws JobStoreException {
        doThrow(new IllegalStateException()).when(sequenceAnalyserMonitorBean).registerInJmx(anyString());

        final JobSchedulerBean jobSchedulerBean = getJobSchedulerBean();
        try {
            jobSchedulerBean.scheduleChunk(chunk, sink);
            fail("No exception thrown");
        } catch (JobStoreException e) {
        }
    }

    @Test
    public void scheduleChunk_givenValidChunk_updatesMonitor() throws JobStoreException {
        final JobSchedulerBean jobSchedulerBean = getJobSchedulerBean();
        jobSchedulerBean.scheduleChunk(chunk, sink);
        final SequenceAnalyserMonitorMXBean sequenceAnalyserMonitorMXBean = jobSchedulerBean.sequenceAnalysers.
                get(jobSchedulerBean.getLockObject(String.valueOf(sink.getId()))).sequenceAnalyserMonitorMXBean;

        // Inject recognizable timestamp into monitor sample
        final long oldTimestamp = 42;
        sequenceAnalyserMonitorMXBean.setSample(new SequenceAnalyserMonitorSample(1, oldTimestamp));

        jobSchedulerBean.scheduleChunk(new ChunkBuilder().setJobId(987654321).build(), sink);

        // Verify that monitor sample has correct "queue" size and has retained timestamp from "queue" head
        assertThat(sequenceAnalyserMonitorMXBean.getSample().getQueued(), is(2L));
        assertThat(sequenceAnalyserMonitorMXBean.getSample().getHeadOfQueueMonitoringStartTime(), is(oldTimestamp));
    }

    @Test
    public void releaseChunk_givenValidChunkIdentifier_releasesChunkFromSequenceAnalysis() throws JobStoreException {
        JobSchedulerBean jobSchedulerBean = getJobSchedulerBean();
        jobSchedulerBean = injectSequenceAnalyserComposite(jobSchedulerBean);
        jobSchedulerBean = injectSinkMapping(jobSchedulerBean);
        jobSchedulerBean.releaseChunk(chunk.getJobId(), chunk.getChunkId());
        verify(sequenceAnalyser).deleteAndReleaseChunk(any(ChunkIdentifier.class));
    }

    @Test
    public void releaseChunk_givenValidChunkIdentifier_notifiesPipelineOfNextAvailableWorkload() throws JobStoreException {
        final List<ChunkIdentifier> identifiers = new ArrayList<>(Arrays.asList(chunkIdentifier, chunkIdentifier));
        when(sequenceAnalyser.getInactiveIndependentChunks()).thenReturn(identifiers);
        when(jobStore.getChunk(chunk.getJobId(), chunk.getChunkId())).thenReturn(chunk);
        doNothing().when(jobProcessorMessageProducerBean).send(chunk);

        JobSchedulerBean jobSchedulerBean = getJobSchedulerBean();
        jobSchedulerBean = injectSequenceAnalyserComposite(jobSchedulerBean);
        jobSchedulerBean = injectSinkMapping(jobSchedulerBean);
        jobSchedulerBean.releaseChunk(chunk.getJobId(), chunk.getChunkId());
        verify(sequenceAnalyser).getInactiveIndependentChunks();
        verify(jobStore, times(identifiers.size())).getChunk(chunk.getJobId(), chunk.getChunkId());
        verify(jobProcessorMessageProducerBean, times(identifiers.size())).send(any(Chunk.class));
    }

    @Test
    public void releaseChunk_onFailureToRetrieveChunkForWorkload_proceedsToNextWorkloadItem() throws JobStoreException {
        final List<ChunkIdentifier> identifiers = new ArrayList<>(Arrays.asList(chunkIdentifier, chunkIdentifier, chunkIdentifier, chunkIdentifier));
        when(sequenceAnalyser.getInactiveIndependentChunks()).thenReturn(identifiers);
        when(jobStore.getChunk(chunk.getJobId(), chunk.getChunkId()))
                .thenReturn(chunk)
                .thenThrow(new JobStoreException("died in getChunk()"))
                .thenReturn(chunk)
                .thenReturn(chunk);
        doNothing().when(jobProcessorMessageProducerBean).send(chunk);

        JobSchedulerBean jobSchedulerBean = getJobSchedulerBean();
        jobSchedulerBean = injectSequenceAnalyserComposite(jobSchedulerBean);
        jobSchedulerBean = injectSinkMapping(jobSchedulerBean);
        jobSchedulerBean.releaseChunk(chunk.getJobId(), chunk.getChunkId());
        verify(sequenceAnalyser).getInactiveIndependentChunks();
        verify(jobStore, times(identifiers.size())).getChunk(chunk.getJobId(), chunk.getChunkId());
        verify(jobProcessorMessageProducerBean, times(identifiers.size()-1)).send(any(Chunk.class));
    }

    @Test
    public void releaseChunk_onFailureToSendChunkNotificationForWorkload_proceedsToNextWorkloadItem() throws JobStoreException {
        final List<ChunkIdentifier> identifiers = new ArrayList<>(Arrays.asList(chunkIdentifier, chunkIdentifier, chunkIdentifier, chunkIdentifier));
        when(sequenceAnalyser.getInactiveIndependentChunks()).thenReturn(identifiers);
        when(jobStore.getChunk(chunk.getJobId(), chunk.getChunkId())).thenReturn(chunk);
        doNothing().
        doThrow(new JobStoreException("died in send()")).
        doNothing().
        doNothing().
                when(jobProcessorMessageProducerBean).send(chunk);

        JobSchedulerBean jobSchedulerBean = getJobSchedulerBean();
        jobSchedulerBean = injectSequenceAnalyserComposite(jobSchedulerBean);
        jobSchedulerBean = injectSinkMapping(jobSchedulerBean);
        jobSchedulerBean.releaseChunk(chunk.getJobId(), chunk.getChunkId());
        verify(sequenceAnalyser).getInactiveIndependentChunks();
        verify(jobStore, times(identifiers.size())).getChunk(chunk.getJobId(), chunk.getChunkId());
        verify(jobProcessorMessageProducerBean, times(identifiers.size())).send(any(Chunk.class));
    }

    @Test
    public void releaseChunk_maintainsToSinkMapping() throws JobStoreException {
        final JobSchedulerBean jobSchedulerBean = getJobSchedulerBean();
        jobSchedulerBean.scheduleChunk(chunk, sink);
        assertThat(jobSchedulerBean.toSinkMapping.size(), is(1));
        jobSchedulerBean.releaseChunk(chunk.getJobId(), chunk.getChunkId());
        assertThat(jobSchedulerBean.toSinkMapping.size(), is(0));
    }

    @Test
    public void releaseChunk_givenChunkIdentifierIsHeadOfQueue_updatesMonitor() throws JobStoreException {
        final JobSchedulerBean jobSchedulerBean = getJobSchedulerBean();
        jobSchedulerBean.scheduleChunk(chunk, sink);
        final SequenceAnalyserMonitorMXBean sequenceAnalyserMonitorMXBean = jobSchedulerBean.sequenceAnalysers.
                get(jobSchedulerBean.getLockObject(String.valueOf(sink.getId()))).sequenceAnalyserMonitorMXBean;

        // Inject recognizable timestamp into monitor sample
        final long oldTimestamp = 42;
        sequenceAnalyserMonitorMXBean.setSample(new SequenceAnalyserMonitorSample(1, oldTimestamp));

        jobSchedulerBean.releaseChunk(chunk.getJobId(), chunk.getChunkId());

        // Verify that monitor sample has correct "queue" size and has updated timestamp to "now"
        assertThat(sequenceAnalyserMonitorMXBean.getSample().getQueued(), is(0L));
        assertThat(sequenceAnalyserMonitorMXBean.getSample().getHeadOfQueueMonitoringStartTime(), is(not(oldTimestamp)));
    }

    @Test
    public void releaseChunk_givenChunkIdentifierIsNotHeadOfQueue_updatesMonitor() throws JobStoreException {
        final JobSchedulerBean jobSchedulerBean = getJobSchedulerBean();
        jobSchedulerBean.scheduleChunk(new ChunkBuilder().setJobId(987654321).build(), sink);
        final SequenceAnalyserMonitorMXBean sequenceAnalyserMonitorMXBean = jobSchedulerBean.sequenceAnalysers.
                get(jobSchedulerBean.getLockObject(String.valueOf(sink.getId()))).sequenceAnalyserMonitorMXBean;

        // Inject recognizable timestamp into monitor sample
        final long oldTimestamp = 42;
        sequenceAnalyserMonitorMXBean.setSample(new SequenceAnalyserMonitorSample(1, oldTimestamp));

        jobSchedulerBean.scheduleChunk(chunk, sink);

        jobSchedulerBean.releaseChunk(chunk.getJobId(), chunk.getChunkId());

        // Verify that monitor sample has correct "queue" size and has retained timestamp from "queue" head
        assertThat(sequenceAnalyserMonitorMXBean.getSample().getQueued(), is(1L));
        assertThat(sequenceAnalyserMonitorMXBean.getSample().getHeadOfQueueMonitoringStartTime(), is(oldTimestamp));
    }

    @Test
    public void releaseChunk_toSinkMappingIsMissing_notifiesPipelineOfNextAvailableWorkload() throws JobStoreException {
        final List<ChunkIdentifier> identifiers = new ArrayList<>(Arrays.asList(chunkIdentifier, chunkIdentifier));
        when(sequenceAnalyser.getInactiveIndependentChunks()).thenReturn(identifiers);
        when(jobStore.getChunk(chunk.getJobId(), chunk.getChunkId())).thenReturn(chunk);
        doNothing().when(jobProcessorMessageProducerBean).send(chunk);

        JobSchedulerBean jobSchedulerBean = getJobSchedulerBean();
        jobSchedulerBean = injectSequenceAnalyserComposite(jobSchedulerBean);
        jobSchedulerBean.releaseChunk(chunk.getJobId(), chunk.getChunkId());
        verify(sequenceAnalyser).getInactiveIndependentChunks();
        verify(jobStore, times(identifiers.size())).getChunk(chunk.getJobId(), chunk.getChunkId());
        verify(jobProcessorMessageProducerBean, times(identifiers.size())).send(any(Chunk.class));
    }

    private JobSchedulerBean getJobSchedulerBean() {
        final JobSchedulerBean jobSchedulerBean = new JobSchedulerBean();
        jobSchedulerBean.jobProcessorMessageProducerBean = jobProcessorMessageProducerBean;
        jobSchedulerBean.jobStoreBean = jobStoreBean;
        jobSchedulerBean.sequenceAnalyserMonitorBean = sequenceAnalyserMonitorBean;
        return jobSchedulerBean;
    }

    private JobSchedulerBean injectSequenceAnalyserComposite(JobSchedulerBean jobSchedulerBean) {
        jobSchedulerBean.sequenceAnalysers.put(jobSchedulerBean.getLockObject(String.valueOf(sink.getId())),
                new JobSchedulerBean.SequenceAnalyserComposite(sequenceAnalyser, sequenceAnalyserMonitorMXBean));
        return jobSchedulerBean;
    }

    private JobSchedulerBean injectSinkMapping(JobSchedulerBean jobSchedulerBean) {
        jobSchedulerBean.toSinkMapping.put(chunkIdentifier, sink);
        return jobSchedulerBean;
    }
}