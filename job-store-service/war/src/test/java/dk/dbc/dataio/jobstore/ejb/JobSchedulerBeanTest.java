package dk.dbc.dataio.jobstore.ejb;

import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.utils.test.model.ChunkBuilder;
import dk.dbc.dataio.commons.utils.test.model.SinkBuilder;
import dk.dbc.dataio.jobstore.JobStore;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import dk.dbc.dataio.sequenceanalyser.SequenceAnalyser;
import dk.dbc.dataio.sequenceanalyser.naive.ChunkIdentifier;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class JobSchedulerBeanTest {
    private final SequenceAnalyser sequenceAnalyser = mock(SequenceAnalyser.class);
    private final JobProcessorMessageProducerBean jobProcessorMessageProducerBean = mock(JobProcessorMessageProducerBean.class);
    private final JobStoreBean jobStoreBean = mock(JobStoreBean.class);
    private final JobStore jobStore = mock(JobStore.class);
    private final Chunk chunk = new ChunkBuilder().build();
    private final Sink sink = new SinkBuilder().build();

    @Before
    public void setupExpectations() {
        doNothing().when(sequenceAnalyser).addChunk(any(Chunk.class));
        when(sequenceAnalyser.getInactiveIndependentChunks()).thenReturn(Collections.<ChunkIdentifier>emptyList());
        when(jobStoreBean.getJobStore()).thenReturn(jobStore);
    }

    @Test(expected = NullPointerException.class)
    public void scheduleChunk_chunkArgIsNull_throws() {
        getJobSchedulerBean().scheduleChunk(null, sink);
    }

    @Test(expected = NullPointerException.class)
    public void scheduleChunk_sinkArgIsNull_throws() {
        getJobSchedulerBean().scheduleChunk(chunk, null);
    }

    @Test
    public void scheduleChunk_allArgsAreValid_addsChunkToSequenceAnalysis() {
        getJobSchedulerBean().scheduleChunk(chunk, sink);
        verify(sequenceAnalyser).addChunk(any(Chunk.class));
    }

    @Test
    public void scheduleChunk_allArgsAreValid_notifiesPipelineOfNextAvailableWorkload() throws JobStoreException {
        final ChunkIdentifier chunkIdentifier = new ChunkIdentifier(chunk.getJobId(), chunk.getChunkId());
        final List<ChunkIdentifier> identifiers = new ArrayList<>(Arrays.asList(chunkIdentifier, chunkIdentifier));
        when(sequenceAnalyser.getInactiveIndependentChunks()).thenReturn(identifiers);
        when(jobStore.getChunk(chunk.getJobId(), chunk.getChunkId())).thenReturn(chunk);
        doNothing().when(jobProcessorMessageProducerBean).send(chunk);

        getJobSchedulerBean().scheduleChunk(chunk, sink);
        verify(sequenceAnalyser).getInactiveIndependentChunks();
        verify(jobStore, times(identifiers.size())).getChunk(chunk.getJobId(), chunk.getChunkId());
        verify(jobProcessorMessageProducerBean, times(identifiers.size())).send(any(Chunk.class));
    }

    @Test
    public void scheduleChunk_onFailureToRetrieveChunkForWorkload_proceedsToNextWorkloadItem() throws JobStoreException {
        final ChunkIdentifier chunkIdentifier = new ChunkIdentifier(chunk.getJobId(), chunk.getChunkId());
        final List<ChunkIdentifier> identifiers = new ArrayList<>(Arrays.asList(chunkIdentifier, chunkIdentifier, chunkIdentifier, chunkIdentifier));
        when(sequenceAnalyser.getInactiveIndependentChunks()).thenReturn(identifiers);
        when(jobStore.getChunk(chunk.getJobId(), chunk.getChunkId()))
                .thenReturn(chunk)
                .thenThrow(new JobStoreException("died in getChunk()"))
                .thenReturn(chunk)
                .thenReturn(chunk);
        doNothing().when(jobProcessorMessageProducerBean).send(chunk);

        getJobSchedulerBean().scheduleChunk(chunk, sink);
        verify(sequenceAnalyser).getInactiveIndependentChunks();
        verify(jobStore, times(identifiers.size())).getChunk(chunk.getJobId(), chunk.getChunkId());
        verify(jobProcessorMessageProducerBean, times(identifiers.size()-1)).send(any(Chunk.class));
    }

    @Test
    public void scheduleChunk_onFailureToSendChunkNotificationForWorkload_proceedsToNextWorkloadItem() throws JobStoreException {
        final ChunkIdentifier chunkIdentifier = new ChunkIdentifier(chunk.getJobId(), chunk.getChunkId());
        final List<ChunkIdentifier> identifiers = new ArrayList<>(Arrays.asList(chunkIdentifier, chunkIdentifier, chunkIdentifier, chunkIdentifier));
        when(sequenceAnalyser.getInactiveIndependentChunks()).thenReturn(identifiers);
        when(jobStore.getChunk(chunk.getJobId(), chunk.getChunkId())).thenReturn(chunk);
        doNothing().
        doThrow(new JobStoreException("died in send()")).
        doNothing().
        doNothing().
                when(jobProcessorMessageProducerBean).send(chunk);

        getJobSchedulerBean().scheduleChunk(chunk, sink);
        verify(sequenceAnalyser).getInactiveIndependentChunks();
        verify(jobStore, times(identifiers.size())).getChunk(chunk.getJobId(), chunk.getChunkId());
        verify(jobProcessorMessageProducerBean, times(identifiers.size())).send(any(Chunk.class));
    }

    @Test
    public void scheduleChunk_givenChunkFromNeverBeforeSeenSink_createsSequenceAnalyserAndMaintainsToSinkMapping() {
        final JobSchedulerBean jobSchedulerBean = getJobSchedulerBean();
        jobSchedulerBean.scheduleChunk(chunk, sink);
        assertThat(jobSchedulerBean.sequenceAnalysers.size(), is(1));
        assertThat(jobSchedulerBean.sequenceAnalysers.get(jobSchedulerBean.getLockObject(String.valueOf(sink.getId()))), is(notNullValue()));
        assertThat(jobSchedulerBean.toSinkMapping.size(), is(1));
        assertThat(jobSchedulerBean.toSinkMapping.get(new ChunkIdentifier(chunk.getJobId(), chunk.getChunkId())), is(sink));
    }

    @Test
    public void releaseChunk_allArgsAreValid_releasesChunkFromSequenceAnalysis() {
        final JobSchedulerBean jobSchedulerBean = getJobSchedulerBean();
        jobSchedulerBean.toSinkMapping.put(new ChunkIdentifier(chunk.getJobId(), chunk.getChunkId()), sink);
        jobSchedulerBean.releaseChunk(chunk.getJobId(), chunk.getChunkId());
        verify(sequenceAnalyser).deleteAndReleaseChunk(any(ChunkIdentifier.class));
    }

    @Test
    public void releaseChunk_allArgsAreValid_notifiesPipelineOfNextAvailableWorkload() throws JobStoreException {
        final ChunkIdentifier chunkIdentifier = new ChunkIdentifier(chunk.getJobId(), chunk.getChunkId());
        final List<ChunkIdentifier> identifiers = new ArrayList<>(Arrays.asList(chunkIdentifier, chunkIdentifier));
        when(sequenceAnalyser.getInactiveIndependentChunks()).thenReturn(identifiers);
        when(jobStore.getChunk(chunk.getJobId(), chunk.getChunkId())).thenReturn(chunk);
        doNothing().when(jobProcessorMessageProducerBean).send(chunk);

        final JobSchedulerBean jobSchedulerBean = getJobSchedulerBean();
        jobSchedulerBean.toSinkMapping.put(new ChunkIdentifier(chunk.getJobId(), chunk.getChunkId()), sink);
        jobSchedulerBean.releaseChunk(chunk.getJobId(), chunk.getChunkId());
        verify(sequenceAnalyser).getInactiveIndependentChunks();
        verify(jobStore, times(identifiers.size())).getChunk(chunk.getJobId(), chunk.getChunkId());
        verify(jobProcessorMessageProducerBean, times(identifiers.size())).send(any(Chunk.class));
    }

    @Test
    public void releaseChunk_onFailureToRetrieveChunkForWorkload_proceedsToNextWorkloadItem() throws JobStoreException {
        final ChunkIdentifier chunkIdentifier = new ChunkIdentifier(chunk.getJobId(), chunk.getChunkId());
        final List<ChunkIdentifier> identifiers = new ArrayList<>(Arrays.asList(chunkIdentifier, chunkIdentifier, chunkIdentifier, chunkIdentifier));
        when(sequenceAnalyser.getInactiveIndependentChunks()).thenReturn(identifiers);
        when(jobStore.getChunk(chunk.getJobId(), chunk.getChunkId()))
                .thenReturn(chunk)
                .thenThrow(new JobStoreException("died in getChunk()"))
                .thenReturn(chunk)
                .thenReturn(chunk);
        doNothing().when(jobProcessorMessageProducerBean).send(chunk);

        final JobSchedulerBean jobSchedulerBean = getJobSchedulerBean();
        jobSchedulerBean.toSinkMapping.put(new ChunkIdentifier(chunk.getJobId(), chunk.getChunkId()), sink);
        jobSchedulerBean.releaseChunk(chunk.getJobId(), chunk.getChunkId());
        verify(sequenceAnalyser).getInactiveIndependentChunks();
        verify(jobStore, times(identifiers.size())).getChunk(chunk.getJobId(), chunk.getChunkId());
        verify(jobProcessorMessageProducerBean, times(identifiers.size()-1)).send(any(Chunk.class));
    }

    @Test
    public void releaseChunk_onFailureToSendChunkNotificationForWorkload_proceedsToNextWorkloadItem() throws JobStoreException {
        final ChunkIdentifier chunkIdentifier = new ChunkIdentifier(chunk.getJobId(), chunk.getChunkId());
        final List<ChunkIdentifier> identifiers = new ArrayList<>(Arrays.asList(chunkIdentifier, chunkIdentifier, chunkIdentifier, chunkIdentifier));
        when(sequenceAnalyser.getInactiveIndependentChunks()).thenReturn(identifiers);
        when(jobStore.getChunk(chunk.getJobId(), chunk.getChunkId())).thenReturn(chunk);
        doNothing().
        doThrow(new JobStoreException("died in send()")).
        doNothing().
        doNothing().
                when(jobProcessorMessageProducerBean).send(chunk);

        final JobSchedulerBean jobSchedulerBean = getJobSchedulerBean();
        jobSchedulerBean.toSinkMapping.put(new ChunkIdentifier(chunk.getJobId(), chunk.getChunkId()), sink);
        jobSchedulerBean.releaseChunk(chunk.getJobId(), chunk.getChunkId());
        verify(sequenceAnalyser).getInactiveIndependentChunks();
        verify(jobStore, times(identifiers.size())).getChunk(chunk.getJobId(), chunk.getChunkId());
        verify(jobProcessorMessageProducerBean, times(identifiers.size())).send(any(Chunk.class));
    }

    @Test
    public void releaseChunk_maintainsToSinkMapping() {
        final JobSchedulerBean jobSchedulerBean = getJobSchedulerBean();
        jobSchedulerBean.scheduleChunk(chunk, sink);
        assertThat(jobSchedulerBean.toSinkMapping.size(), is(1));
        jobSchedulerBean.releaseChunk(chunk.getJobId(), chunk.getChunkId());
        assertThat(jobSchedulerBean.toSinkMapping.size(), is(0));
    }

    private JobSchedulerBean getJobSchedulerBean() {
        final JobSchedulerBean jobSchedulerBean = new JobSchedulerBean();
        jobSchedulerBean.jobProcessorMessageProducerBean = jobProcessorMessageProducerBean;
        jobSchedulerBean.jobStoreBean = jobStoreBean;
        jobSchedulerBean.sequenceAnalysers.put(jobSchedulerBean.getLockObject(String.valueOf(sink.getId())), sequenceAnalyser);
        return jobSchedulerBean;
    }
}