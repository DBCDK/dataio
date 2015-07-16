package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.commons.types.ExternalChunk;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.utils.test.model.ExternalChunkBuilder;
import dk.dbc.dataio.commons.utils.test.model.SinkBuilder;
import dk.dbc.dataio.jobstore.service.ejb.monitoring.SequenceAnalyserMonitorBean;
import dk.dbc.dataio.jobstore.service.ejb.monitoring.SequenceAnalyserMonitorMXBean;
import dk.dbc.dataio.jobstore.service.ejb.monitoring.SequenceAnalyserMonitorSample;
import dk.dbc.dataio.jobstore.service.sequenceanalyser.ChunkIdentifier;
import dk.dbc.dataio.jobstore.types.JobStoreException;
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

import static dk.dbc.dataio.jobstore.service.ejb.JobSchedulerBean.DO_PUBLISH_WORKLOAD;
import static dk.dbc.dataio.jobstore.service.ejb.JobSchedulerBean.NOT_PUBLISH_WORKLOAD;
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

    private final static CollisionDetectionElement NO_CHUNK_CDE = null;
    private final static Sink NO_SINK = null;
    private final SequenceAnalyser sequenceAnalyser = mock(SequenceAnalyser.class);
    private final JobProcessorMessageProducerBean jobProcessorMessageProducerBean = mock(JobProcessorMessageProducerBean.class);
    private final PgJobStore jobStoreBean = mock(PgJobStore.class);
    private final SequenceAnalyserMonitorBean sequenceAnalyserMonitorBean = mock(SequenceAnalyserMonitorBean.class);
    private final ExternalChunk chunk = new ExternalChunkBuilder(ExternalChunk.Type.PARTITIONED).build();
    private final ChunkIdentifier chunkIdentifier = new ChunkIdentifier(chunk.getJobId(), chunk.getChunkId());
    private final CollisionDetectionElement chunkCDE = new CollisionDetectionElement(chunkIdentifier, new HashSet<>(Arrays.asList("key")), chunk.size());
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
        doNothing().when(sequenceAnalyser).add(any(CollisionDetectionElement.class));
        when(sequenceAnalyser.getInactiveIndependent(anyInt())).thenReturn(Collections.<CollisionDetectionElement>emptyList());
        when(sequenceAnalyserMonitorBean.getMBeans()).thenReturn(mBeans);
    }

    @Test
    public void scheduleChunk2arg_chunkArgIsNull_throws() throws JobStoreException {
        final JobSchedulerBean jobSchedulerBean = getJobSchedulerBean();
        try {
            jobSchedulerBean.scheduleChunk(NO_CHUNK_CDE, sink);
            fail("No exception thrown");
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void scheduleChunk2arg_sinkArgIsNull_throws() throws JobStoreException {
        final JobSchedulerBean jobSchedulerBean = getJobSchedulerBean();
        try {
            jobSchedulerBean.scheduleChunk(chunkCDE, NO_SINK);
            fail("No exception thrown");
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void scheduleChunk2arg_givenValidCDE_addsToSequenceAnalysis() throws JobStoreException {
        final JobSchedulerBean jobSchedulerBean = getJobSchedulerBean();
        injectSequenceAnalyserComposite(jobSchedulerBean, String.valueOf(sink.getId()));

        jobSchedulerBean.scheduleChunk(chunkCDE, sink);
        verify(sequenceAnalyser).add(any(CollisionDetectionElement.class));
    }

    @Test
    public void scheduleChunk2arg_givenValidCDE_publishesWorkload() throws JobStoreException {
        final List<CollisionDetectionElement> elements = whenGetWorkloadThenReturn(2);
        whenGetChunk().thenReturn(chunk);

        final JobSchedulerBean jobSchedulerBean = getJobSchedulerBean();
        injectSequenceAnalyserComposite(jobSchedulerBean, String.valueOf(sink.getId()));
        jobSchedulerBean.scheduleChunk(chunkCDE, sink);
        verify(jobStoreBean, times(elements.size())).getChunk(ExternalChunk.Type.PARTITIONED, (int) chunk.getJobId(), (int) chunk.getChunkId());
        verify(jobProcessorMessageProducerBean, times(elements.size())).send(chunk);
    }

    @Test
    public void scheduleChunk2arg_onFailureToRetrieveWorkloadItem_proceedsToNextWorkloadItem() throws JobStoreException {
        final List<CollisionDetectionElement> elements = whenGetWorkloadThenReturn(4);
        whenGetChunk()
                .thenReturn(chunk)
                .thenThrow(new NullPointerException("died in getChunk()"))
                .thenReturn(chunk)
                .thenReturn(chunk);
        doNothing().when(jobProcessorMessageProducerBean).send(chunk);

        final JobSchedulerBean jobSchedulerBean = getJobSchedulerBean();
        injectSequenceAnalyserComposite(jobSchedulerBean, String.valueOf(sink.getId()));
        jobSchedulerBean.scheduleChunk(chunkCDE, sink);
        verify(jobStoreBean, times(elements.size())).getChunk(ExternalChunk.Type.PARTITIONED, (int) chunk.getJobId(), (int) chunk.getChunkId());
        verify(jobProcessorMessageProducerBean, times(elements.size()-1)).send(chunk);
    }

    @Test
    public void scheduleChunk2arg_onFailureToSendWorkloadItem_proceedsToNextWorkloadItem() throws JobStoreException {
        final List<CollisionDetectionElement> elements = whenGetWorkloadThenReturn(4);
        whenGetChunk().thenReturn(chunk);
        doNothing().
        doThrow(new JobStoreException("died in send()")).
        doNothing().
        doNothing().
                when(jobProcessorMessageProducerBean).send(chunk);

        final JobSchedulerBean jobSchedulerBean = getJobSchedulerBean();
        injectSequenceAnalyserComposite(jobSchedulerBean, String.valueOf(sink.getId()));
        jobSchedulerBean.scheduleChunk(chunkCDE, sink);
        verify(jobStoreBean, times(elements.size())).getChunk(ExternalChunk.Type.PARTITIONED, (int) chunk.getJobId(), (int) chunk.getChunkId());
        verify(jobProcessorMessageProducerBean, times(elements.size())).send(chunk);
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
                new ChunkIdentifier(chunk.getJobId()+1, chunk.getChunkId()+1), Collections.<String>emptySet(), chunk.size()), sink);

        // Verify that monitor sample has correct "queue" size and has retained timestamp from "queue" head
        assertThat(mxBean.getSample().getQueued(), is(2L));
        assertThat(mxBean.getSample().getHeadOfQueueMonitoringStartTime(), is(oldTimestamp));
    }

    @Test
    public void scheduleChunk3arg_chunkArgIsNull_throws() throws JobStoreException {
        final JobSchedulerBean jobSchedulerBean = getJobSchedulerBean();
        try {
            jobSchedulerBean.scheduleChunk(NO_CHUNK_CDE, sink, NOT_PUBLISH_WORKLOAD);
            fail("No exception thrown");
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void scheduleChunk3arg_sinkArgIsNull_throws() throws JobStoreException {
        final JobSchedulerBean jobSchedulerBean = getJobSchedulerBean();
        try {
            jobSchedulerBean.scheduleChunk(chunkCDE, NO_SINK, NOT_PUBLISH_WORKLOAD);
            fail("No exception thrown");
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void scheduleChunk3arg_givenValidCDE_addsToSequenceAnalysis() throws JobStoreException {
        final JobSchedulerBean jobSchedulerBean = getJobSchedulerBean();
        injectSequenceAnalyserComposite(jobSchedulerBean, String.valueOf(sink.getId()));

        jobSchedulerBean.scheduleChunk(chunkCDE, sink, NOT_PUBLISH_WORKLOAD);
        verify(sequenceAnalyser).add(any(CollisionDetectionElement.class));
    }

    @Test
    public void scheduleChunk3arg_doPublishWorkloadArgIsTrue_publishesWorkload() throws JobStoreException {
        final List<CollisionDetectionElement> elements = whenGetWorkloadThenReturn(2);
        whenGetChunk().thenReturn(chunk);

        final JobSchedulerBean jobSchedulerBean = getJobSchedulerBean();
        injectSequenceAnalyserComposite(jobSchedulerBean, String.valueOf(sink.getId()));
        jobSchedulerBean.scheduleChunk(chunkCDE, sink, DO_PUBLISH_WORKLOAD);
        verify(jobStoreBean, times(elements.size())).getChunk(ExternalChunk.Type.PARTITIONED, (int) chunk.getJobId(), (int) chunk.getChunkId());
        verify(jobProcessorMessageProducerBean, times(elements.size())).send(chunk);
    }

    @Test
    public void scheduleChunk3arg_doPublishWorkloadArgIsFalse_noWorkloadPublished() throws JobStoreException {
        whenGetWorkloadThenReturn(2);
        whenGetChunk().thenReturn(chunk);

        final JobSchedulerBean jobSchedulerBean = getJobSchedulerBean();
        injectSequenceAnalyserComposite(jobSchedulerBean, String.valueOf(sink.getId()));
        jobSchedulerBean.scheduleChunk(chunkCDE, sink, NOT_PUBLISH_WORKLOAD);
        verify(jobStoreBean, times(0)).getChunk(ExternalChunk.Type.PARTITIONED, (int) chunk.getJobId(), (int) chunk.getChunkId());
        verify(jobProcessorMessageProducerBean, times(0)).send(chunk);
    }

    @Test
    public void scheduleChunk3arg_onFailureToRetrieveWorkloadItem_proceedsToNextWorkloadItem() throws JobStoreException {
        final List<CollisionDetectionElement> elements = whenGetWorkloadThenReturn(4);
        whenGetChunk()
                .thenReturn(chunk)
                .thenThrow(new NullPointerException("died in getChunk()"))
                .thenReturn(chunk)
                .thenReturn(chunk);
        doNothing().when(jobProcessorMessageProducerBean).send(chunk);

        final JobSchedulerBean jobSchedulerBean = getJobSchedulerBean();
        injectSequenceAnalyserComposite(jobSchedulerBean, String.valueOf(sink.getId()));
        jobSchedulerBean.scheduleChunk(chunkCDE, sink, DO_PUBLISH_WORKLOAD);
        verify(jobStoreBean, times(elements.size())).getChunk(ExternalChunk.Type.PARTITIONED, (int) chunk.getJobId(), (int) chunk.getChunkId());
        verify(jobProcessorMessageProducerBean, times(elements.size()-1)).send(chunk);
    }

    @Test
    public void scheduleChunk3arg_onFailureToSendWorkloadItem_proceedsToNextWorkloadItem() throws JobStoreException {
        final List<CollisionDetectionElement> elements = whenGetWorkloadThenReturn(4);
        whenGetChunk().thenReturn(chunk);
        doNothing().
        doThrow(new JobStoreException("died in send()")).
        doNothing().
        doNothing().
                when(jobProcessorMessageProducerBean).send(chunk);

        final JobSchedulerBean jobSchedulerBean = getJobSchedulerBean();
        injectSequenceAnalyserComposite(jobSchedulerBean, String.valueOf(sink.getId()));
        jobSchedulerBean.scheduleChunk(chunkCDE, sink, DO_PUBLISH_WORKLOAD);
        verify(jobStoreBean, times(elements.size())).getChunk(ExternalChunk.Type.PARTITIONED, (int) chunk.getJobId(), (int) chunk.getChunkId());
        verify(jobProcessorMessageProducerBean, times(elements.size())).send(chunk);
    }

    @Test
    public void scheduleChunk3arg_sinkNotSeenBefore_createsSequenceAnalyserAndMaintainsToSinkMapping() throws JobStoreException {
        final JobSchedulerBean jobSchedulerBean = getJobSchedulerBean();
        jobSchedulerBean.scheduleChunk(chunkCDE, sink, NOT_PUBLISH_WORKLOAD);
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
            jobSchedulerBean.scheduleChunk(chunkCDE, sink, NOT_PUBLISH_WORKLOAD);
            fail("No exception thrown");
        } catch (JobStoreException e) {
        }
    }

    @Test
    public void scheduleChunk3arg_givenValidCDE_updatesMonitor() throws JobStoreException {
        final JobSchedulerBean jobSchedulerBean = getJobSchedulerBean();
        jobSchedulerBean.scheduleChunk(chunkCDE, sink, NOT_PUBLISH_WORKLOAD);
        final SequenceAnalyserMonitorMXBean mxBean = getMXBean(jobSchedulerBean);

        // Inject recognizable timestamp into monitor sample
        final long oldTimestamp = 42;
        mxBean.setSample(new SequenceAnalyserMonitorSample(1, oldTimestamp));

        jobSchedulerBean.scheduleChunk(new CollisionDetectionElement(
                new ChunkIdentifier(chunk.getJobId()+1, chunk.getChunkId()+1), Collections.<String>emptySet(), chunk.size()), sink);

        // Verify that monitor sample has correct "queue" size and has retained timestamp from "queue" head
        assertThat(mxBean.getSample().getQueued(), is(2L));
        assertThat(mxBean.getSample().getHeadOfQueueMonitoringStartTime(), is(oldTimestamp));
    }

    @Test
    public void releaseChunk_givenValidChunkIdentifier_releasesChunkFromSequenceAnalysis() throws JobStoreException {
        final JobSchedulerBean jobSchedulerBean = getJobSchedulerBean();
        injectSequenceAnalyserComposite(jobSchedulerBean, String.valueOf(sink.getId()));
        injectSinkMapping(jobSchedulerBean);
        jobSchedulerBean.releaseChunk(chunkIdentifier);
        verify(sequenceAnalyser).deleteAndRelease(chunkIdentifier);
    }

    @Test
    public void releaseChunk_givenValidChunkIdentifier_publishesWorkload() throws JobStoreException {
        final List<CollisionDetectionElement> elements = whenGetWorkloadThenReturn(2);
        whenGetChunk().thenReturn(chunk);

        final JobSchedulerBean jobSchedulerBean = getJobSchedulerBean();
        injectSequenceAnalyserComposite(jobSchedulerBean, String.valueOf(sink.getId()));
        injectSinkMapping(jobSchedulerBean);
        jobSchedulerBean.releaseChunk(chunkIdentifier);
        verify(jobStoreBean, times(elements.size())).getChunk(ExternalChunk.Type.PARTITIONED, (int) chunk.getJobId(), (int) chunk.getChunkId());
        verify(jobProcessorMessageProducerBean, times(elements.size())).send(chunk);
    }

    @Test
    public void releaseChunk_onFailureToRetrieveWorkloadItem_proceedsToNextWorkloadItem() throws JobStoreException {
        final List<CollisionDetectionElement> elements = whenGetWorkloadThenReturn(4);
        whenGetChunk()
                .thenReturn(chunk)
                .thenThrow(new NullPointerException("died in getChunk()"))
                .thenReturn(chunk)
                .thenReturn(chunk);

        final JobSchedulerBean jobSchedulerBean = getJobSchedulerBean();
        injectSequenceAnalyserComposite(jobSchedulerBean, String.valueOf(sink.getId()));
        injectSinkMapping(jobSchedulerBean);
        jobSchedulerBean.releaseChunk(chunkIdentifier);
        verify(jobStoreBean, times(elements.size())).getChunk(ExternalChunk.Type.PARTITIONED, (int) chunk.getJobId(), (int) chunk.getChunkId());
        verify(jobProcessorMessageProducerBean, times(elements.size()-1)).send(chunk);
    }

    @Test
    public void releaseChunk_onFailureToSendWorkloadItem_proceedsToNextWorkloadItem() throws JobStoreException {
        final List<CollisionDetectionElement> elements = whenGetWorkloadThenReturn(4);
        whenGetChunk().thenReturn(chunk);
        doNothing().
        doThrow(new JobStoreException("died in send()")).
        doNothing().
        doNothing().
                when(jobProcessorMessageProducerBean).send(chunk);

        final JobSchedulerBean jobSchedulerBean = getJobSchedulerBean();
        injectSequenceAnalyserComposite(jobSchedulerBean, String.valueOf(sink.getId()));
        injectSinkMapping(jobSchedulerBean);
        jobSchedulerBean.releaseChunk(chunkIdentifier);
        verify(jobStoreBean, times(elements.size())).getChunk(ExternalChunk.Type.PARTITIONED, (int) chunk.getJobId(), (int) chunk.getChunkId());
        verify(jobProcessorMessageProducerBean, times(elements.size())).send(chunk);
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
                new ChunkIdentifier(chunk.getJobId()+1, chunk.getChunkId()+1), Collections.<String>emptySet(), chunk.size()), sink);

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
        final List<CollisionDetectionElement> elements = whenGetWorkloadThenReturn(2);
        whenGetChunk().thenReturn(chunk);

        final JobSchedulerBean jobSchedulerBean = getJobSchedulerBean();
        injectSequenceAnalyserComposite(jobSchedulerBean, String.valueOf(sink.getId()));
        jobSchedulerBean.releaseChunk(chunkIdentifier);
        verify(jobStoreBean, times(elements.size())).getChunk(ExternalChunk.Type.PARTITIONED, (int) chunk.getJobId(), (int) chunk.getChunkId());
        verify(jobProcessorMessageProducerBean, times(elements.size())).send(chunk);
    }

    @Test
    public void jumpStart() throws JobStoreException {
        whenGetWorkloadThenReturn(1);
        whenGetChunk().thenReturn(chunk);

        final JobSchedulerBean jobSchedulerBean = getJobSchedulerBean();
        injectSequenceAnalyserComposite(jobSchedulerBean, "sink1");
        injectSequenceAnalyserComposite(jobSchedulerBean, "sink2");
        jobSchedulerBean.jumpStart();
        verify(jobStoreBean, times(2)).getChunk(ExternalChunk.Type.PARTITIONED, (int) chunk.getJobId(), (int) chunk.getChunkId());
        verify(jobProcessorMessageProducerBean, times(2)).send(chunk);
    }

    @Test
    public void getWorkload2arg_maxNumberOfItemsExeedsAvailableSlots_returnsWorkload() {
        final int expectedWorkloadSize = 5;
        whenGetWorkloadPreciseMatchThenReturn(expectedWorkloadSize);
        final JobSchedulerBean jobSchedulerBean = getJobSchedulerBean();
        final String saId = String.valueOf(sink.getId());
        injectSequenceAnalyserComposite(jobSchedulerBean, saId);
        final JobSchedulerBean.SequenceAnalyserComposite sac = jobSchedulerBean.sequenceAnalysers.get(jobSchedulerBean.getLockObject(saId));
        sac.itemsInProgress = JobSchedulerBean.MAX_NUMBER_OF_ITEMS_IN_PROGRESS_PER_SINK - expectedWorkloadSize;
        final List<CollisionDetectionElement> workload = jobSchedulerBean.getWorkload(sac, 100);
        assertThat("workload.size()", workload.size(), is(expectedWorkloadSize));
        assertThat("sac.itemsInProgress", sac.itemsInProgress, is(JobSchedulerBean.MAX_NUMBER_OF_ITEMS_IN_PROGRESS_PER_SINK));
    }

    @Test
    public void getWorkload2arg_maxNumberOfItemsDoesNotExceedAvailableSlots_returnsWorkload() {
        final int expectedWorkloadSize = 42;
        whenGetWorkloadPreciseMatchThenReturn(expectedWorkloadSize);
        final JobSchedulerBean jobSchedulerBean = getJobSchedulerBean();
        final String saId = String.valueOf(sink.getId());
        injectSequenceAnalyserComposite(jobSchedulerBean, saId);
        final JobSchedulerBean.SequenceAnalyserComposite sac = jobSchedulerBean.sequenceAnalysers.get(jobSchedulerBean.getLockObject(saId));
        final List<CollisionDetectionElement> workload = jobSchedulerBean.getWorkload(sac, expectedWorkloadSize);
        assertThat("workload.size()", workload.size(), is(expectedWorkloadSize));
        assertThat("sac.itemsInProgress", sac.itemsInProgress, is(expectedWorkloadSize));
    }

    @Test
    public void getWorkload2arg_zeroAvailableSlots_returnsEmptyWorkload() {
        final int expectedWorkloadSize = 0;
        final JobSchedulerBean jobSchedulerBean = getJobSchedulerBean();
        final String saId = String.valueOf(sink.getId());
        injectSequenceAnalyserComposite(jobSchedulerBean, saId);
        final JobSchedulerBean.SequenceAnalyserComposite sac = jobSchedulerBean.sequenceAnalysers.get(jobSchedulerBean.getLockObject(saId));
        sac.itemsInProgress = JobSchedulerBean.MAX_NUMBER_OF_ITEMS_IN_PROGRESS_PER_SINK;
        final List<CollisionDetectionElement> workload = jobSchedulerBean.getWorkload(sac, 100);
        assertThat("workload.size()", workload.size(), is(expectedWorkloadSize));
        assertThat("sac.itemsInProgress", sac.itemsInProgress, is(JobSchedulerBean.MAX_NUMBER_OF_ITEMS_IN_PROGRESS_PER_SINK));
    }

    @Test
    public void getWorkload2arg_lessThanZeroAvailableSlots_returnsEmptyWorkload() {
        final int expectedWorkloadSize = 0;
        final JobSchedulerBean jobSchedulerBean = getJobSchedulerBean();
        final String saId = String.valueOf(sink.getId());
        injectSequenceAnalyserComposite(jobSchedulerBean, saId);
        final JobSchedulerBean.SequenceAnalyserComposite sac = jobSchedulerBean.sequenceAnalysers.get(jobSchedulerBean.getLockObject(saId));
        sac.itemsInProgress = JobSchedulerBean.MAX_NUMBER_OF_ITEMS_IN_PROGRESS_PER_SINK + 5;
        final List<CollisionDetectionElement> workload = jobSchedulerBean.getWorkload(sac, 100);
        assertThat("workload.size()", workload.size(), is(expectedWorkloadSize));
        assertThat("sac.itemsInProgress", sac.itemsInProgress, is(JobSchedulerBean.MAX_NUMBER_OF_ITEMS_IN_PROGRESS_PER_SINK + 5));
    }

    private JobSchedulerBean getJobSchedulerBean() {
        final JobSchedulerBean jobSchedulerBean = new JobSchedulerBean();
        jobSchedulerBean.jobProcessorMessageProducerBean = jobProcessorMessageProducerBean;
        jobSchedulerBean.jobStoreBean = jobStoreBean;
        jobSchedulerBean.sequenceAnalyserMonitorBean = sequenceAnalyserMonitorBean;
        return jobSchedulerBean;
    }

    private void injectSequenceAnalyserComposite(JobSchedulerBean jobSchedulerBean, String saId) {
        jobSchedulerBean.sequenceAnalysers.put(jobSchedulerBean.getLockObject(saId),
                new JobSchedulerBean.SequenceAnalyserComposite(sequenceAnalyser, sequenceAnalyserMonitorMXBean));
    }

    private void injectSinkMapping(JobSchedulerBean jobSchedulerBean) {
        jobSchedulerBean.toSinkMapping.put(chunkIdentifier, sink);
    }

    private OngoingStubbing<ExternalChunk> whenGetChunk() {
        return when(jobStoreBean.getChunk(ExternalChunk.Type.PARTITIONED, (int) chunk.getJobId(), (int) chunk.getChunkId()));
    }

    private List<CollisionDetectionElement> whenGetWorkloadThenReturn(int numElements) {
        final List<CollisionDetectionElement> elements = new ArrayList<>();
        while (numElements-- > 0) {
            elements.add(new CollisionDetectionElement(chunkIdentifier, Collections.<String>emptySet(), chunk.size()));
        }
        when(sequenceAnalyser.getInactiveIndependent(anyInt())).thenReturn(elements);
        return elements;
    }

    private List<CollisionDetectionElement> whenGetWorkloadPreciseMatchThenReturn(int numElements) {
        final List<CollisionDetectionElement> elements = new ArrayList<>();
        int counter = numElements;
        while (counter-- > 0) {
            elements.add(new CollisionDetectionElement(chunkIdentifier, Collections.<String>emptySet(), chunk.size()));
        }
        when(sequenceAnalyser.getInactiveIndependent(numElements)).thenReturn(elements);
        return elements;
    }

    private SequenceAnalyserMonitorMXBean getMXBean(JobSchedulerBean jobSchedulerBean) {
        return jobSchedulerBean.sequenceAnalysers.get(
                jobSchedulerBean.getLockObject(String.valueOf(sink.getId()))).sequenceAnalyserMonitorMXBean;
    }
}