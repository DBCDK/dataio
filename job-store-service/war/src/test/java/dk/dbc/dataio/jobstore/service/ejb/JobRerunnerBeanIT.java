package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.commons.types.AddiMetaData;
import dk.dbc.dataio.commons.types.HarvesterToken;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.SinkContent;
import dk.dbc.dataio.commons.utils.test.model.SinkBuilder;
import dk.dbc.dataio.commons.utils.test.model.SinkContentBuilder;
import dk.dbc.dataio.harvester.connector.ejb.TickleHarvesterServiceConnectorBean;
import dk.dbc.dataio.harvester.task.connector.HarvesterTaskServiceConnector;
import dk.dbc.dataio.harvester.task.connector.HarvesterTaskServiceConnectorException;
import dk.dbc.dataio.harvester.types.HarvestRecordsRequest;
import dk.dbc.dataio.harvester.types.HarvestSelectorRequest;
import dk.dbc.dataio.harvester.types.HarvestTaskSelector;
import dk.dbc.dataio.harvester.types.TickleRepoHarvesterConfig;
import dk.dbc.dataio.jobstore.service.AbstractJobStoreIT;
import dk.dbc.dataio.jobstore.service.entity.ChunkEntity;
import dk.dbc.dataio.jobstore.service.entity.ItemEntity;
import dk.dbc.dataio.jobstore.service.entity.JobEntity;
import dk.dbc.dataio.jobstore.service.entity.RerunEntity;
import dk.dbc.dataio.jobstore.service.entity.SinkCacheEntity;
import dk.dbc.dataio.jobstore.service.param.AddJobParam;
import dk.dbc.dataio.jobstore.types.InvalidInputException;
import dk.dbc.dataio.jobstore.types.JobError;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import dk.dbc.dataio.jobstore.types.RecordInfo;
import dk.dbc.dataio.jobstore.types.State;
import dk.dbc.dataio.jobstore.types.StateChange;
import dk.dbc.dataio.rrharvester.service.connector.ejb.RRHarvesterServiceConnectorBean;
import jakarta.persistence.EntityTransaction;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;
import java.util.BitSet;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class JobRerunnerBeanIT extends AbstractJobStoreIT {
    private RRHarvesterServiceConnectorBean rrHarvesterServiceConnectorBean = mock(RRHarvesterServiceConnectorBean.class);
    private HarvesterTaskServiceConnector rrHarvesterServiceConnector = mock(HarvesterTaskServiceConnector.class);
    private TickleHarvesterServiceConnectorBean tickleHarvesterServiceConnectorBean = mock(TickleHarvesterServiceConnectorBean.class);
    private HarvesterTaskServiceConnector tickleHarvesterServiceConnector = mock(HarvesterTaskServiceConnector.class);
    private PgJobStore pgJobStore = mock(PgJobStore.class);

    private final HarvesterToken rawRepoHarvesterToken = HarvesterToken.of("raw-repo:42:1");
    private final HarvesterToken tickleRepoHarvesterToken = HarvesterToken.of("tickle-repo:42:1");
    private final String fallbackNotificationDestination = "fallback";

    private JobRerunnerBean jobRerunnerBean;

    @Rule
    public final EnvironmentVariables environmentVariables = new EnvironmentVariables();

    @Before
    public void initializeJobRerunnerBean() {
        environmentVariables.set("MAIL_TO_FALLBACK", fallbackNotificationDestination);
        jobRerunnerBean = new JobRerunnerBean();
        jobRerunnerBean.rerunsRepository = newRerunsRepository();
        jobRerunnerBean.rrHarvesterServiceConnectorBean = rrHarvesterServiceConnectorBean;
        jobRerunnerBean.tickleHarvesterServiceConnectorBean = tickleHarvesterServiceConnectorBean;
        jobRerunnerBean.flowStoreServiceConnectorBean = mockedFlowStoreServiceConnectorBean;
        jobRerunnerBean.sessionContext = mockedSessionContext;
        jobRerunnerBean.entityManager = entityManager;
        jobRerunnerBean.pgJobStore = pgJobStore;
        when(rrHarvesterServiceConnectorBean.getConnector()).thenReturn(rrHarvesterServiceConnector);
        when(tickleHarvesterServiceConnectorBean.getConnector()).thenReturn(tickleHarvesterServiceConnector);
        when(mockedSessionContext.getBusinessObject(JobRerunnerBean.class)).thenReturn(jobRerunnerBean);
        when(mockedFlowStoreServiceConnectorBean.getConnector()).thenReturn(mockedFlowStoreServiceConnector);
    }

    @Test
    public void jobDoesNotExist() throws JobStoreException {
        final EntityTransaction transaction = entityManager.getTransaction();
        try {
            transaction.begin();
            jobRerunnerBean.requestJobRerun(42);
            fail("no InvalidInputException thrown");
        } catch (InvalidInputException e) {
            assertThat(e.getJobError().getCode(), is(JobError.Code.INVALID_JOB_IDENTIFIER));
        } finally {
            if (transaction != null && transaction.isActive()) {
                transaction.commit();
            }
        }
    }

    @Test
    public void createsWaitingRerunTask() {
        final JobEntity job = newJobEntity();
        job.setSpecification(new JobSpecification()
                .withAncestry(new JobSpecification.Ancestry()
                        .withHarvesterToken(new HarvesterToken()
                                .withHarvesterVariant(HarvesterToken.HarvesterVariant.RAW_REPO)
                                .withId(42)
                                .withVersion(1)
                                .toString()))
        );

        persist(job);

        when(mockedSessionContext.getBusinessObject(JobRerunnerBean.class)).thenReturn(mock(JobRerunnerBean.class));

        final RerunEntity rerun = persistenceContext.run(() -> jobRerunnerBean.requestJobRerun(job.getId()));
        assertThat("entity in database", entityManager.find(RerunEntity.class, rerun.getId()), is(notNullValue()));
    }

    @Test
    public void rerunNextIfAvailable_removesRerunEntityAfterTaskCompletion() {
        final RerunEntity rerun = getRerunEntity();

        persistenceContext.run(jobRerunnerBean::rerunNextIfAvailable);
        assertThat("entity in database", entityManager.find(RerunEntity.class, rerun.getId()), is(nullValue()));
    }

    @Test
    public void rerunRawRepoHarvesterJob_exportingBibliographicRecordIds() throws HarvesterTaskServiceConnectorException {
        final RerunEntity rerun = getRerunEntityForRawRepoJob();
        final JobEntity job = rerun.getJob();

        persistenceContext.run(() -> jobRerunnerBean.rerunHarvesterJob(rerun, rawRepoHarvesterToken));

        final ArgumentCaptor<HarvestRecordsRequest> argumentCaptor = ArgumentCaptor.forClass(HarvestRecordsRequest.class);
        verify(rrHarvesterServiceConnector).createHarvestTask(eq(rawRepoHarvesterToken.getId()), argumentCaptor.capture());

        final HarvestRecordsRequest request = argumentCaptor.getValue();
        assertThat("request.getBasedOnJob()", request.getBasedOnJob(), is(job.getId()));
        assertThat("request submitter", (long) request.getRecords().get(0).submitterNumber(),
                is(job.getSpecification().getSubmitterId()));
        assertThat("request bibliographic record IDs", request.getRecords()
                        .stream().map(AddiMetaData::bibliographicRecordId).collect(Collectors.toList()),
                is(Arrays.asList("id0", "id1", "id2")));
    }

    @Test
    public void rerunRawRepoHarvesterJob_exportingBibliographicRecordIdsFromFailedItems()
            throws HarvesterTaskServiceConnectorException {
        final RerunEntity rerun = getRerunEntityForRawRepoJob();
        persistenceContext.run(() -> rerun.withIncludeFailedOnly(true));

        final JobEntity job = rerun.getJob();

        persistenceContext.run(() -> jobRerunnerBean.rerunHarvesterJob(rerun, rawRepoHarvesterToken));

        final ArgumentCaptor<HarvestRecordsRequest> argumentCaptor = ArgumentCaptor.forClass(HarvestRecordsRequest.class);
        verify(rrHarvesterServiceConnector).createHarvestTask(eq(rawRepoHarvesterToken.getId()), argumentCaptor.capture());

        final HarvestRecordsRequest request = argumentCaptor.getValue();
        assertThat("request.getBasedOnJob()", request.getBasedOnJob(), is(job.getId()));
        assertThat("request submitter", (long) request.getRecords().get(0).submitterNumber(),
                is(job.getSpecification().getSubmitterId()));
        assertThat("request bibliographic record IDs", request.getRecords()
                        .stream().map(AddiMetaData::bibliographicRecordId).collect(Collectors.toList()),
                is(Arrays.asList("id1", "id2")));
    }

    @Test
    public void rerunTickleRepoHarvesterIncrementalJob() throws HarvesterTaskServiceConnectorException {
        final RerunEntity rerun = getRerunEntityForTickleRepoIncrementalJob();
        final JobEntity job = rerun.getJob();
        job.setCachedSink(newPersistedSinkCacheEntity());

        persistenceContext.run(() -> jobRerunnerBean.rerunHarvesterJob(rerun, tickleRepoHarvesterToken));

        final ArgumentCaptor<HarvestRecordsRequest> argumentCaptor = ArgumentCaptor.forClass(HarvestRecordsRequest.class);
        verify(tickleHarvesterServiceConnector).createHarvestTask(eq(tickleRepoHarvesterToken.getId()), argumentCaptor.capture());

        final HarvestRecordsRequest request = argumentCaptor.getValue();
        assertThat("request.getBasedOnJob()", request.getBasedOnJob(), is(job.getId()));
        assertThat("request bibliographic record IDs", request.getRecords()
                        .stream().map(AddiMetaData::bibliographicRecordId).collect(Collectors.toList()),
                is(Arrays.asList("id0", "id1", "id2")));
    }

    @Test
    public void rerunTickleRepoHarvesterIncrementalJob_failedItemsOnly() throws HarvesterTaskServiceConnectorException {
        final RerunEntity rerun = getRerunEntityForTickleRepoIncrementalJob();
        persistenceContext.run(() -> rerun.withIncludeFailedOnly(true));

        final JobEntity job = rerun.getJob();
        job.setCachedSink(newPersistedSinkCacheEntity());

        persistenceContext.run(() -> jobRerunnerBean.rerunHarvesterJob(rerun, tickleRepoHarvesterToken));

        final ArgumentCaptor<HarvestRecordsRequest> argumentCaptor = ArgumentCaptor.forClass(HarvestRecordsRequest.class);
        verify(tickleHarvesterServiceConnector).createHarvestTask(eq(tickleRepoHarvesterToken.getId()), argumentCaptor.capture());

        final HarvestRecordsRequest request = argumentCaptor.getValue();
        assertThat("request.getBasedOnJob()", request.getBasedOnJob(), is(job.getId()));
        assertThat("request bibliographic record IDs", request.getRecords()
                        .stream().map(AddiMetaData::bibliographicRecordId).collect(Collectors.toList()),
                is(Arrays.asList("id1", "id2")));
    }

    @Test
    public void rerunTickleRepoHarvesterTotalJob()
            throws FlowStoreServiceConnectorException, HarvesterTaskServiceConnectorException {
        final String dataSetName = "tickle-dataset";
        when(mockedFlowStoreServiceConnector
                .getHarvesterConfig(tickleRepoHarvesterToken.getId(), TickleRepoHarvesterConfig.class))
                .thenReturn(new TickleRepoHarvesterConfig(
                        tickleRepoHarvesterToken.getId(),
                        tickleRepoHarvesterToken.getVersion(),
                        new TickleRepoHarvesterConfig.Content()
                                .withDatasetName(dataSetName)));

        final RerunEntity rerun = getRerunEntityForTickleRepoTotalJob();
        persistenceContext.run(() -> jobRerunnerBean.rerunHarvesterJob(rerun, tickleRepoHarvesterToken));

        final ArgumentCaptor<HarvestSelectorRequest> argumentCaptor =
                ArgumentCaptor.forClass(HarvestSelectorRequest.class);
        verify(tickleHarvesterServiceConnector)
                .createHarvestTask(eq(tickleRepoHarvesterToken.getId()), argumentCaptor.capture());

        final HarvestSelectorRequest request = argumentCaptor.getValue();
        assertThat("request.getSelector()", request.getSelector(),
                is(new HarvestTaskSelector("dataSetName", dataSetName)));
    }

    @Test
    public void rerunTickleRepoHarvesterTotalJob_failedItemsOnly()
            throws FlowStoreServiceConnectorException, HarvesterTaskServiceConnectorException {
        final String dataSetName = "tickle-dataset";
        when(mockedFlowStoreServiceConnector
                .getHarvesterConfig(tickleRepoHarvesterToken.getId(), TickleRepoHarvesterConfig.class))
                .thenReturn(new TickleRepoHarvesterConfig(
                        tickleRepoHarvesterToken.getId(),
                        tickleRepoHarvesterToken.getVersion(),
                        new TickleRepoHarvesterConfig.Content()
                                .withDatasetName(dataSetName)));

        final RerunEntity rerun = getRerunEntityForTickleRepoTotalJob();
        persistenceContext.run(() -> rerun.withIncludeFailedOnly(true));
        persistenceContext.run(() -> jobRerunnerBean.rerunHarvesterJob(rerun, tickleRepoHarvesterToken));

        final ArgumentCaptor<HarvestSelectorRequest> argumentCaptor =
                ArgumentCaptor.forClass(HarvestSelectorRequest.class);
        verify(tickleHarvesterServiceConnector)
                .createHarvestTask(eq(tickleRepoHarvesterToken.getId()), argumentCaptor.capture());

        final HarvestSelectorRequest request = argumentCaptor.getValue();
        assertThat("request.getSelector()", request.getSelector(),
                is(new HarvestTaskSelector("dataSetName", dataSetName)));
    }

    @Test
    public void rerunJob() throws JobStoreException {
        final JobSpecification jobSpecification = createJobSpecification()
                .withMailForNotificationAboutVerification("mail@company.com")
                .withMailForNotificationAboutProcessing("mail@company.com");
        final RerunEntity rerun = getRerunEntity(jobSpecification);
        jobRerunnerBean.rerunsRepository.addWaiting(rerun);
        persistenceContext.run(() -> jobRerunnerBean.rerunNextIfAvailable());

        boolean emptyRerunQueue = entityManager.createNamedQuery(
                RerunEntity.FIND_HEAD_QUERY_NAME).getResultList().isEmpty();
        assertThat("Rerun removed from queue", emptyRerunQueue, is(true));

        final ArgumentCaptor<AddJobParam> addJobParamArgumentCaptor = ArgumentCaptor.forClass(AddJobParam.class);
        final ArgumentCaptor<byte[]> includeFilterArgumentCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(pgJobStore).addJob(addJobParamArgumentCaptor.capture(), includeFilterArgumentCaptor.capture());

        final AddJobParam addJobParam = addJobParamArgumentCaptor.getValue();
        final JobSpecification specification = addJobParam.getJobInputStream().getJobSpecification();
        assertThat("Re-run job has ancestry", specification.getAncestry(), is(notNullValue()));
        assertThat("Re-run job ancestry has previous job ID",
                specification.getAncestry().getPreviousJobId() > 0, is(true));
        assertThat("Re-run job has fallback notification destination (verification)",
                specification.getMailForNotificationAboutVerification(), is(fallbackNotificationDestination));
        assertThat("Re-run job has fallback notification destination (processing)",
                specification.getMailForNotificationAboutProcessing(), is(fallbackNotificationDestination));

        final byte[] includeFilterBytes = includeFilterArgumentCaptor.getValue();
        assertThat("Re-run job has include filter", includeFilterBytes, is(notNullValue()));
        final BitSet includeFilter = BitSet.valueOf(includeFilterBytes);
        assertThat("include filter size", includeFilter.length(), is(3));
        assertThat("include filter 1st index", includeFilter.get(0), is(true));
        assertThat("include filter 2nd index", includeFilter.get(1), is(true));
        assertThat("include filter 3rd index", includeFilter.get(2), is(true));
    }

    @Test
    public void rerunJob_failedOnly() throws JobStoreException {
        final JobSpecification jobSpecification = createJobSpecification()
                .withMailForNotificationAboutVerification("mail@company.com")
                .withMailForNotificationAboutProcessing("mail@company.com");
        final RerunEntity rerun = getRerunEntity(jobSpecification);
        persistenceContext.run(() -> rerun.withIncludeFailedOnly(true));

        jobRerunnerBean.rerunsRepository.addWaiting(rerun);
        persistenceContext.run(() -> jobRerunnerBean.rerunNextIfAvailable());

        boolean emptyRerunQueue = entityManager.createNamedQuery(
                RerunEntity.FIND_HEAD_QUERY_NAME).getResultList().isEmpty();
        assertThat("Rerun removed from queue", emptyRerunQueue, is(true));

        final ArgumentCaptor<AddJobParam> addJobParamArgumentCaptor = ArgumentCaptor.forClass(AddJobParam.class);
        final ArgumentCaptor<byte[]> includeFilterArgumentCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(pgJobStore).addJob(addJobParamArgumentCaptor.capture(), includeFilterArgumentCaptor.capture());

        final AddJobParam addJobParam = addJobParamArgumentCaptor.getValue();
        final JobSpecification specification = addJobParam.getJobInputStream().getJobSpecification();
        assertThat("Re-run job has ancestry", specification.getAncestry(), is(notNullValue()));
        assertThat("Re-run job ancestry has previous job ID",
                specification.getAncestry().getPreviousJobId() > 0, is(true));
        assertThat("Re-run job has fallback notification destination (verification)",
                specification.getMailForNotificationAboutVerification(), is(fallbackNotificationDestination));
        assertThat("Re-run job has fallback notification destination (processing)",
                specification.getMailForNotificationAboutProcessing(), is(fallbackNotificationDestination));

        final byte[] includeFilterBytes = includeFilterArgumentCaptor.getValue();
        assertThat("Re-run job has include filter", includeFilterBytes, is(notNullValue()));
        final BitSet includeFilter = BitSet.valueOf(includeFilterBytes);
        assertThat("include filter size", includeFilter.length(), is(3));
        assertThat("include filter 1st index", includeFilter.get(0), is(false));
        assertThat("include filter 2nd index", includeFilter.get(1), is(true));
        assertThat("include filter 3rd index", includeFilter.get(2), is(true));
    }

    private RerunEntity getRerunEntityForRawRepoJob() {
        return getRerunEntity(createJobSpecification()
                .withAncestry(new JobSpecification.Ancestry()
                        .withHarvesterToken(rawRepoHarvesterToken.toString())));
    }

    private RerunEntity getRerunEntityForTickleRepoIncrementalJob() {
        return getRerunEntity(createJobSpecification()
                .withAncestry(new JobSpecification.Ancestry()
                        .withHarvesterToken(tickleRepoHarvesterToken.toString())));
    }

    private RerunEntity getRerunEntityForTickleRepoTotalJob() {
        final Sink sink = new SinkBuilder()
                .setContent(new SinkContentBuilder()
                        .setSinkType(SinkContent.SinkType.TICKLE)
                        .setQueue("sink::tickle-repo/total")
                        .build())
                .build();
        final SinkCacheEntity sinkCacheEntity = newPersistedSinkCacheEntity(sink);
        final JobEntity job = newJobEntity();
        job.setCachedSink(sinkCacheEntity);
        persist(job);

        return getRerunEntity(createJobSpecification()
                .withAncestry(new JobSpecification.Ancestry()
                        .withHarvesterToken(tickleRepoHarvesterToken.toString())))
                .withJob(job);
    }

    private RerunEntity getRerunEntity() {
        return getRerunEntity(createJobSpecification());
    }

    private RerunEntity getRerunEntity(JobSpecification jobSpecification) {
        final JobEntity job = newJobEntity();
        job.setSpecification(jobSpecification);
        persist(job);

        final ChunkEntity chunk = newPersistedChunkEntity(new ChunkEntity.Key(0, job.getId()));

        final ItemEntity item0 = newItemEntity(new ItemEntity.Key(job.getId(), chunk.getKey().getId(), (short) 0));
        item0.withRecordInfo(new RecordInfo("id0"))
                .withPositionInDatafile(0);
        persist(item0);

        final ItemEntity item1 = newItemEntity(new ItemEntity.Key(job.getId(), chunk.getKey().getId(), (short) 1));
        item1.withRecordInfo(new RecordInfo("id1"))
                .withPositionInDatafile(1);
        State state = new State(item1.getState());
        state.updateState(new StateChange()
                .setPhase(State.Phase.PROCESSING)
                .incFailed(1));
        item1.setState(state);
        persist(item1);

        final ItemEntity item2 = newItemEntity(new ItemEntity.Key(job.getId(), chunk.getKey().getId(), (short) 2));
        item2.withRecordInfo(new RecordInfo("id2"))
                .withPositionInDatafile(2);
        state = new State(item2.getState());
        state.updateState(new StateChange()
                .setPhase(State.Phase.DELIVERING)
                .incFailed(1));
        item2.setState(state);
        persist(item2);

        return newPersistedRerunEntity(job);
    }
}
