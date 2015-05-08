package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.commons.types.ExternalChunk;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.utils.test.model.ExternalChunkBuilder;
import dk.dbc.dataio.commons.utils.test.model.SinkBuilder;
import dk.dbc.dataio.jobstore.service.ejb.monitoring.SequenceAnalyserMonitorBean;
import dk.dbc.dataio.jobstore.service.ejb.monitoring.SequenceAnalyserMonitorMXBean;
import dk.dbc.dataio.jobstore.service.ejb.monitoring.SequenceAnalyserMonitorSample;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import dk.dbc.dataio.sequenceanalyser.ChunkIdentifier;
import dk.dbc.dataio.sequenceanalyser.CollisionDetectionElement;
import dk.dbc.dataio.sequenceanalyser.SequenceAnalyser;
import org.junit.Before;
import org.junit.Test;
import org.mockito.stubbing.OngoingStubbing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
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
    private final PgJobStore jobStoreBean = mock(PgJobStore.class);
    private final SequenceAnalyserMonitorBean sequenceAnalyserMonitorBean = mock(SequenceAnalyserMonitorBean.class);
    private final ExternalChunk chunk = new ExternalChunkBuilder(ExternalChunk.Type.PARTITIONED).build();
    private final ChunkIdentifier chunkIdentifier = new ChunkIdentifier(chunk.getJobId(), chunk.getChunkId());
    private final CollisionDetectionElement chunkCDE = new CollisionDetectionElement(chunkIdentifier, new HashSet<>(Arrays.asList("key")));
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
        when(sequenceAnalyser.getInactiveIndependentChunks(anyInt())).thenReturn(Collections.<ChunkIdentifier>emptyList());
        when(sequenceAnalyserMonitorBean.getMBeans()).thenReturn(mBeans);
    }

    @Test
    public void scheduleChunk2arg_chunkArgIsNull_throws() throws JobStoreException {
        final JobSchedulerBean jobSchedulerBean = getJobSchedulerBean();
        try {
            jobSchedulerBean.scheduleChunk(null, sink);
            fail("No exception thrown");
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void scheduleChunk2arg_sinkArgIsNull_throws() throws JobStoreException {
        final JobSchedulerBean jobSchedulerBean = getJobSchedulerBean();
        try {
            jobSchedulerBean.scheduleChunk(chunkCDE, null);
            fail("No exception thrown");
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void scheduleChunk2arg_givenValidCDE_addsToSequenceAnalysis() throws JobStoreException {
        final JobSchedulerBean jobSchedulerBean = getJobSchedulerBean();
        injectSequenceAnalyserComposite(jobSchedulerBean);

        jobSchedulerBean.scheduleChunk(chunkCDE, sink);
        verify(sequenceAnalyser).addChunk(any(CollisionDetectionElement.class));
    }

    @Test
    public void scheduleChunk2arg_givenValidCDE_publishesWorkload() throws JobStoreException {
        final List<ChunkIdentifier> identifiers = whenGetWorkloadThenReturn(2);
        whenGetChunk().thenReturn(chunk);

        final JobSchedulerBean jobSchedulerBean = getJobSchedulerBean();
        injectSequenceAnalyserComposite(jobSchedulerBean);
        jobSchedulerBean.scheduleChunk(chunkCDE, sink);
        verify(jobStoreBean, times(identifiers.size())).getChunk(ExternalChunk.Type.PARTITIONED, (int) chunk.getJobId(), (int) chunk.getChunkId());
        verify(jobProcessorMessageProducerBean, times(identifiers.size())).send(chunk);
    }

    @Test
    public void scheduleChunk2arg_onFailureToRetrieveWorkloadItem_proceedsToNextWorkloadItem() throws JobStoreException {
        final List<ChunkIdentifier> identifiers = whenGetWorkloadThenReturn(4);
        whenGetChunk()
                .thenReturn(chunk)
                .thenThrow(new NullPointerException("died in getChunk()"))
                .thenReturn(chunk)
                .thenReturn(chunk);
        doNothing().when(jobProcessorMessageProducerBean).send(chunk);

        final JobSchedulerBean jobSchedulerBean = getJobSchedulerBean();
        injectSequenceAnalyserComposite(jobSchedulerBean);
        jobSchedulerBean.scheduleChunk(chunkCDE, sink);
        verify(jobStoreBean, times(identifiers.size())).getChunk(ExternalChunk.Type.PARTITIONED, (int) chunk.getJobId(), (int) chunk.getChunkId());
        verify(jobProcessorMessageProducerBean, times(identifiers.size()-1)).send(chunk);
    }

    @Test
    public void scheduleChunk2arg_onFailureToSendWorkloadItem_proceedsToNextWorkloadItem() throws JobStoreException {
        final List<ChunkIdentifier> identifiers = whenGetWorkloadThenReturn(4);
        whenGetChunk().thenReturn(chunk);
        doNothing().
        doThrow(new JobStoreException("died in send()")).
        doNothing().
        doNothing().
                when(jobProcessorMessageProducerBean).send(chunk);

        final JobSchedulerBean jobSchedulerBean = getJobSchedulerBean();
        injectSequenceAnalyserComposite(jobSchedulerBean);
        jobSchedulerBean.scheduleChunk(chunkCDE, sink);
        verify(jobStoreBean, times(identifiers.size())).getChunk(ExternalChunk.Type.PARTITIONED, (int) chunk.getJobId(), (int) chunk.getChunkId());
        verify(jobProcessorMessageProducerBean, times(identifiers.size())).send(chunk);
    }

    @Test
    public void scheduleChunk2arg_sinkNotSeenBefore_createsSequenceAnalyserAndMaintainsToSinkMapping() throws JobStoreException {
        final JobSchedulerBean jobSchedulerBean = getJobSchedulerBean();
        jobSchedulerBean.scheduleChunk(chunkCDE, sink);
        assertThat("sequenceAnalysers.size(),", jobSchedulerBean.sequenceAnalysers.size(), is(1));
        assertThat("sequenceAnalysers.get(),", getMXBean(jobSchedulerBean), is(notNullValue()));
        assertThat("toSinkMapping.size()", jobSchedulerBean.toSinkMapping.size(), is(1));
        assertThat("toSinkMapping.get(),", jobSchedulerBean.toSinkMapping.get(chunkIdentifier), is(sink));
    }

    @Test
    public void scheduleChunk2arg_sequenceAnalyserMonitorBeanThrowsIllegalStateException_throws() throws JobStoreException {
        doThrow(new IllegalStateException()).when(sequenceAnalyserMonitorBean).registerInJmx(anyString());

        final JobSchedulerBean jobSchedulerBean = getJobSchedulerBean();
        try {
            jobSchedulerBean.scheduleChunk(chunkCDE, sink);
            fail("No exception thrown");
        } catch (JobStoreException e) {
        }
    }

    @Test
    public void scheduleChunk2arg_givenValidCDE_updatesMonitor() throws JobStoreException {
        final JobSchedulerBean jobSchedulerBean = getJobSchedulerBean();
        jobSchedulerBean.scheduleChunk(chunkCDE, sink);
        final SequenceAnalyserMonitorMXBean mxBean = getMXBean(jobSchedulerBean);

        // Inject recognizable timestamp into monitor sample
        final long oldTimestamp = 42;
        mxBean.setSample(new SequenceAnalyserMonitorSample(1, oldTimestamp));

        jobSchedulerBean.scheduleChunk(new CollisionDetectionElement(
                new ChunkIdentifier(chunk.getJobId()+1, chunk.getChunkId()+1), Collections.<String>emptySet()), sink);

        // Verify that monitor sample has correct "queue" size and has retained timestamp from "queue" head
        assertThat(mxBean.getSample().getQueued(), is(2L));
        assertThat(mxBean.getSample().getHeadOfQueueMonitoringStartTime(), is(oldTimestamp));
    }

    @Test
    public void scheduleChunk3arg_chunkArgIsNull_throws() throws JobStoreException {
        final JobSchedulerBean jobSchedulerBean = getJobSchedulerBean();
        try {
            jobSchedulerBean.scheduleChunk(null, sink, false);
            fail("No exception thrown");
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void scheduleChunk3arg_sinkArgIsNull_throws() throws JobStoreException {
        final JobSchedulerBean jobSchedulerBean = getJobSchedulerBean();
        try {
            jobSchedulerBean.scheduleChunk(chunkCDE, null, false);
            fail("No exception thrown");
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void scheduleChunk3arg_givenValidCDE_addsToSequenceAnalysis() throws JobStoreException {
        final JobSchedulerBean jobSchedulerBean = getJobSchedulerBean();
        injectSequenceAnalyserComposite(jobSchedulerBean);

        jobSchedulerBean.scheduleChunk(chunkCDE, sink, false);
        verify(sequenceAnalyser).addChunk(any(CollisionDetectionElement.class));
    }

    @Test
    public void scheduleChunk3arg_doPublishWorkloadArgIsTrue_publishesWorkload() throws JobStoreException {
        final List<ChunkIdentifier> identifiers = whenGetWorkloadThenReturn(2);
        whenGetChunk().thenReturn(chunk);

        final JobSchedulerBean jobSchedulerBean = getJobSchedulerBean();
        injectSequenceAnalyserComposite(jobSchedulerBean);
        jobSchedulerBean.scheduleChunk(chunkCDE, sink, true);
        verify(jobStoreBean, times(identifiers.size())).getChunk(ExternalChunk.Type.PARTITIONED, (int) chunk.getJobId(), (int) chunk.getChunkId());
        verify(jobProcessorMessageProducerBean, times(identifiers.size())).send(chunk);
    }

    @Test
    public void scheduleChunk3arg_doPublishWorkloadArgIsFalse_noWorkloadPublished() throws JobStoreException {
        final List<ChunkIdentifier> identifiers = whenGetWorkloadThenReturn(2);
        whenGetChunk().thenReturn(chunk);

        final JobSchedulerBean jobSchedulerBean = getJobSchedulerBean();
        injectSequenceAnalyserComposite(jobSchedulerBean);
        jobSchedulerBean.scheduleChunk(chunkCDE, sink, false);
        verify(jobStoreBean, times(0)).getChunk(ExternalChunk.Type.PARTITIONED, (int) chunk.getJobId(), (int) chunk.getChunkId());
        verify(jobProcessorMessageProducerBean, times(0)).send(chunk);
    }

    @Test
    public void scheduleChunk3arg_onFailureToRetrieveWorkloadItem_proceedsToNextWorkloadItem() throws JobStoreException {
        final List<ChunkIdentifier> identifiers = whenGetWorkloadThenReturn(4);
        whenGetChunk()
                .thenReturn(chunk)
                .thenThrow(new NullPointerException("died in getChunk()"))
                .thenReturn(chunk)
                .thenReturn(chunk);
        doNothing().when(jobProcessorMessageProducerBean).send(chunk);

        final JobSchedulerBean jobSchedulerBean = getJobSchedulerBean();
        injectSequenceAnalyserComposite(jobSchedulerBean);
        jobSchedulerBean.scheduleChunk(chunkCDE, sink, true);
        verify(jobStoreBean, times(identifiers.size())).getChunk(ExternalChunk.Type.PARTITIONED, (int) chunk.getJobId(), (int) chunk.getChunkId());
        verify(jobProcessorMessageProducerBean, times(identifiers.size()-1)).send(chunk);
    }

    @Test
    public void scheduleChunk3arg_onFailureToSendWorkloadItem_proceedsToNextWorkloadItem() throws JobStoreException {
        final List<ChunkIdentifier> identifiers = whenGetWorkloadThenReturn(4);
        whenGetChunk().thenReturn(chunk);
        doNothing().
        doThrow(new JobStoreException("died in send()")).
        doNothing().
        doNothing().
                when(jobProcessorMessageProducerBean).send(chunk);

        final JobSchedulerBean jobSchedulerBean = getJobSchedulerBean();
        injectSequenceAnalyserComposite(jobSchedulerBean);
        jobSchedulerBean.scheduleChunk(chunkCDE, sink, true);
        verify(jobStoreBean, times(identifiers.size())).getChunk(ExternalChunk.Type.PARTITIONED, (int) chunk.getJobId(), (int) chunk.getChunkId());
        verify(jobProcessorMessageProducerBean, times(identifiers.size())).send(chunk);
    }

    @Test
    public void scheduleChunk3arg_sinkNotSeenBefore_createsSequenceAnalyserAndMaintainsToSinkMapping() throws JobStoreException {
        final JobSchedulerBean jobSchedulerBean = getJobSchedulerBean();
        jobSchedulerBean.scheduleChunk(chunkCDE, sink, false);
        assertThat("sequenceAnalysers.size(),", jobSchedulerBean.sequenceAnalysers.size(), is(1));
        assertThat("sequenceAnalysers.get(),", getMXBean(jobSchedulerBean), is(notNullValue()));
        assertThat("toSinkMapping.size()", jobSchedulerBean.toSinkMapping.size(), is(1));
        assertThat("toSinkMapping.get(),", jobSchedulerBean.toSinkMapping.get(chunkIdentifier), is(sink));
    }

    @Test
    public void scheduleChunk3arg_sequenceAnalyserMonitorBeanThrowsIllegalStateException_throws() throws JobStoreException {
        doThrow(new IllegalStateException()).when(sequenceAnalyserMonitorBean).registerInJmx(anyString());

        final JobSchedulerBean jobSchedulerBean = getJobSchedulerBean();
        try {
            jobSchedulerBean.scheduleChunk(chunkCDE, sink, false);
            fail("No exception thrown");
        } catch (JobStoreException e) {
        }
    }

    @Test
    public void scheduleChunk3arg_givenValidCDE_updatesMonitor() throws JobStoreException {
        final JobSchedulerBean jobSchedulerBean = getJobSchedulerBean();
        jobSchedulerBean.scheduleChunk(chunkCDE, sink, false);
        final SequenceAnalyserMonitorMXBean mxBean = getMXBean(jobSchedulerBean);

        // Inject recognizable timestamp into monitor sample
        final long oldTimestamp = 42;
        mxBean.setSample(new SequenceAnalyserMonitorSample(1, oldTimestamp));

        jobSchedulerBean.scheduleChunk(new CollisionDetectionElement(
                new ChunkIdentifier(chunk.getJobId()+1, chunk.getChunkId()+1), Collections.<String>emptySet()), sink);

        // Verify that monitor sample has correct "queue" size and has retained timestamp from "queue" head
        assertThat(mxBean.getSample().getQueued(), is(2L));
        assertThat(mxBean.getSample().getHeadOfQueueMonitoringStartTime(), is(oldTimestamp));
    }

    @Test
    public void releaseChunk_givenValidChunkIdentifier_releasesChunkFromSequenceAnalysis() throws JobStoreException {
        final JobSchedulerBean jobSchedulerBean = getJobSchedulerBean();
        injectSequenceAnalyserComposite(jobSchedulerBean);
        injectSinkMapping(jobSchedulerBean);
        jobSchedulerBean.releaseChunk(chunkIdentifier);
        verify(sequenceAnalyser).deleteAndReleaseChunk(chunkIdentifier);
    }

    @Test
    public void releaseChunk_givenValidChunkIdentifier_publishesWorkload() throws JobStoreException {
        final List<ChunkIdentifier> identifiers = whenGetWorkloadThenReturn(2);
        whenGetChunk().thenReturn(chunk);

        final JobSchedulerBean jobSchedulerBean = getJobSchedulerBean();
        injectSequenceAnalyserComposite(jobSchedulerBean);
        injectSinkMapping(jobSchedulerBean);
        jobSchedulerBean.releaseChunk(chunkIdentifier);
        verify(jobStoreBean, times(identifiers.size())).getChunk(ExternalChunk.Type.PARTITIONED, (int) chunk.getJobId(), (int) chunk.getChunkId());
        verify(jobProcessorMessageProducerBean, times(identifiers.size())).send(chunk);
    }

    @Test
    public void releaseChunk_onFailureToRetrieveWorkloadItem_proceedsToNextWorkloadItem() throws JobStoreException {
        final List<ChunkIdentifier> identifiers = whenGetWorkloadThenReturn(4);
        whenGetChunk()
                .thenReturn(chunk)
                .thenThrow(new NullPointerException("died in getChunk()"))
                .thenReturn(chunk)
                .thenReturn(chunk);

        final JobSchedulerBean jobSchedulerBean = getJobSchedulerBean();
        injectSequenceAnalyserComposite(jobSchedulerBean);
        injectSinkMapping(jobSchedulerBean);
        jobSchedulerBean.releaseChunk(chunkIdentifier);
        verify(jobStoreBean, times(identifiers.size())).getChunk(ExternalChunk.Type.PARTITIONED, (int) chunk.getJobId(), (int) chunk.getChunkId());
        verify(jobProcessorMessageProducerBean, times(identifiers.size()-1)).send(chunk);
    }

    @Test
    public void releaseChunk_onFailureToSendWorkloadItem_proceedsToNextWorkloadItem() throws JobStoreException {
        final List<ChunkIdentifier> identifiers = whenGetWorkloadThenReturn(4);
        whenGetChunk().thenReturn(chunk);
        doNothing().
        doThrow(new JobStoreException("died in send()")).
        doNothing().
        doNothing().
                when(jobProcessorMessageProducerBean).send(chunk);

        final JobSchedulerBean jobSchedulerBean = getJobSchedulerBean();
        injectSequenceAnalyserComposite(jobSchedulerBean);
        injectSinkMapping(jobSchedulerBean);
        jobSchedulerBean.releaseChunk(chunkIdentifier);
        verify(jobStoreBean, times(identifiers.size())).getChunk(ExternalChunk.Type.PARTITIONED, (int) chunk.getJobId(), (int) chunk.getChunkId());
        verify(jobProcessorMessageProducerBean, times(identifiers.size())).send(chunk);
    }

    @Test
    public void releaseChunk_maintainsToSinkMapping() throws JobStoreException {
        final JobSchedulerBean jobSchedulerBean = getJobSchedulerBean();
        jobSchedulerBean.scheduleChunk(chunkCDE, sink);
        assertThat(jobSchedulerBean.toSinkMapping.size(), is(1));
        jobSchedulerBean.releaseChunk(chunkIdentifier);
        assertThat(jobSchedulerBean.toSinkMapping.size(), is(0));
    }

    @Test
    public void releaseChunk_givenChunkIdentifierIsHeadOfQueue_updatesMonitor() throws JobStoreException {
        final JobSchedulerBean jobSchedulerBean = getJobSchedulerBean();
        jobSchedulerBean.scheduleChunk(chunkCDE, sink);

        final SequenceAnalyserMonitorMXBean sequenceAnalyserMonitorMXBean = getMXBean(jobSchedulerBean);

        // Inject recognizable timestamp into monitor sample
        final long oldTimestamp = 42;
        sequenceAnalyserMonitorMXBean.setSample(new SequenceAnalyserMonitorSample(1, oldTimestamp));

        jobSchedulerBean.releaseChunk(chunkIdentifier);

        // Verify that monitor sample has correct "queue" size and has updated timestamp to "now"
        assertThat(sequenceAnalyserMonitorMXBean.getSample().getQueued(), is(0L));
        assertThat(sequenceAnalyserMonitorMXBean.getSample().getHeadOfQueueMonitoringStartTime(), is(not(oldTimestamp)));
    }

    @Test
    public void releaseChunk_givenChunkIdentifierIsNotHeadOfQueue_updatesMonitor() throws JobStoreException {
        final JobSchedulerBean jobSchedulerBean = getJobSchedulerBean();
        jobSchedulerBean.scheduleChunk(new CollisionDetectionElement(
                new ChunkIdentifier(chunk.getJobId()+1, chunk.getChunkId()+1), Collections.<String>emptySet()), sink);

        final SequenceAnalyserMonitorMXBean sequenceAnalyserMonitorMXBean = getMXBean(jobSchedulerBean);

        // Inject recognizable timestamp into monitor sample
        final long oldTimestamp = 42;
        sequenceAnalyserMonitorMXBean.setSample(new SequenceAnalyserMonitorSample(1, oldTimestamp));

        jobSchedulerBean.scheduleChunk(chunkCDE, sink);
        jobSchedulerBean.releaseChunk(chunkIdentifier);

        // Verify that monitor sample has correct "queue" size and has retained timestamp from "queue" head
        assertThat(sequenceAnalyserMonitorMXBean.getSample().getQueued(), is(1L));
        assertThat(sequenceAnalyserMonitorMXBean.getSample().getHeadOfQueueMonitoringStartTime(), is(oldTimestamp));
    }

    @Test
    public void releaseChunk_toSinkMappingIsMissing_publishesWorkload() throws JobStoreException {
        final List<ChunkIdentifier> identifiers = whenGetWorkloadThenReturn(2);
        whenGetChunk().thenReturn(chunk);

        final JobSchedulerBean jobSchedulerBean = getJobSchedulerBean();
        injectSequenceAnalyserComposite(jobSchedulerBean);
        jobSchedulerBean.releaseChunk(chunkIdentifier);
        verify(jobStoreBean, times(identifiers.size())).getChunk(ExternalChunk.Type.PARTITIONED, (int) chunk.getJobId(), (int) chunk.getChunkId());
        verify(jobProcessorMessageProducerBean, times(identifiers.size())).send(chunk);
    }

    private JobSchedulerBean getJobSchedulerBean() {
        final JobSchedulerBean jobSchedulerBean = new JobSchedulerBean();
        jobSchedulerBean.jobProcessorMessageProducerBean = jobProcessorMessageProducerBean;
        jobSchedulerBean.jobStoreBean = jobStoreBean;
        jobSchedulerBean.sequenceAnalyserMonitorBean = sequenceAnalyserMonitorBean;
        return jobSchedulerBean;
    }

    private void injectSequenceAnalyserComposite(JobSchedulerBean jobSchedulerBean) {
        jobSchedulerBean.sequenceAnalysers.put(jobSchedulerBean.getLockObject(String.valueOf(sink.getId())),
                new JobSchedulerBean.SequenceAnalyserComposite(sequenceAnalyser, sequenceAnalyserMonitorMXBean));
    }

    private void injectSinkMapping(JobSchedulerBean jobSchedulerBean) {
        jobSchedulerBean.toSinkMapping.put(chunkIdentifier, sink);
    }

    private OngoingStubbing<ExternalChunk> whenGetChunk() {
        return when(jobStoreBean.getChunk(ExternalChunk.Type.PARTITIONED, (int) chunk.getJobId(), (int) chunk.getChunkId()));
    }

    private List<ChunkIdentifier> whenGetWorkloadThenReturn(int numIdentifiers) {
        final List<ChunkIdentifier> identifiers = new ArrayList<>();
        while (numIdentifiers-- > 0) {
            identifiers.add(chunkIdentifier);
        }
        when(sequenceAnalyser.getInactiveIndependentChunks(anyInt())).thenReturn(identifiers);
        return identifiers;
    }

    private SequenceAnalyserMonitorMXBean getMXBean(JobSchedulerBean jobSchedulerBean) {
        return jobSchedulerBean.sequenceAnalysers.get(
                jobSchedulerBean.getLockObject(String.valueOf(sink.getId()))).sequenceAnalyserMonitorMXBean;
    }
}