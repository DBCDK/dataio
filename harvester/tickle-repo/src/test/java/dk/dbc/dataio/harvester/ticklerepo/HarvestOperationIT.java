package dk.dbc.dataio.harvester.ticklerepo;

import dk.dbc.commons.persistence.JpaTestEnvironment;
import dk.dbc.dataio.bfs.api.BinaryFileStoreFsImpl;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.commons.types.AddiMetaData;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.filestore.service.connector.MockedFileStoreServiceConnector;
import dk.dbc.dataio.harvester.task.TaskRepo;
import dk.dbc.dataio.harvester.task.entity.HarvestTask;
import dk.dbc.dataio.harvester.types.HarvestTaskSelector;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.TickleRepoHarvesterConfig;
import dk.dbc.dataio.harvester.utils.datafileverifier.AddiFileVerifier;
import dk.dbc.dataio.harvester.utils.datafileverifier.Expectation;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.jobstore.types.JobInputStream;
import dk.dbc.dataio.jobstore.types.criteria.JobListCriteria;
import dk.dbc.ticklerepo.TickleRepo;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static dk.dbc.commons.testutil.Assert.assertThat;
import static dk.dbc.commons.testutil.Assert.isThrowing;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class HarvestOperationIT extends IntegrationTest {
    private final JobStoreServiceConnector jobStoreServiceConnector = mock(JobStoreServiceConnector.class);
    private final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
    private MockedFileStoreServiceConnector fileStoreServiceConnector;
    private Path harvesterTmpFile;

    @Rule
    public TemporaryFolder tmpFolder = new TemporaryFolder();

    @Before
    public void setupMocks() throws IOException, JobStoreServiceConnectorException {
        // Intercept harvester data files with mocked FileStoreServiceConnectorBean
        harvesterTmpFile = tmpFolder.newFile().toPath();
        fileStoreServiceConnector = new MockedFileStoreServiceConnector();
        fileStoreServiceConnector.destinations.add(harvesterTmpFile);

        when(jobStoreServiceConnector.addJob(any(JobInputStream.class)))
                .thenReturn(new JobInfoSnapshot());
    }

    @Test
    public void nonExistingDatasetHarvestOperationThrowsOnCreation() {
        final TickleRepoHarvesterConfig config = newConfig();
        config.getContent()
                .withDatasetName("non-existing");

        assertThat(() -> createHarvestOperation(config), isThrowing(IllegalStateException.class));
    }

    @Test
    public void recordsHarvestedByBatch() {
        final TickleRepoHarvesterConfig config = newConfig();
        config.getContent().withLastBatchHarvested(2);
        final HarvestOperation harvestOperation = createHarvestOperation(config);
        final JpaTestEnvironment ticklerepo = environment.get("ticklerepo");
        final int numberOfRecordsHarvested = ticklerepo.getPersistenceContext().run(harvestOperation::execute);
        assertThat("Number of records harvested", numberOfRecordsHarvested, is(3));

        final List<AddiMetaData> addiMetadataExpectations = new ArrayList<>();
        addiMetadataExpectations.add(new AddiMetaData()
                .withSubmitterNumber(123456)
                .withFormat("test-format")
                .withBibliographicRecordId("id_3_1")
                .withTrackingId("t_3_1")
                .withDeleted(false));
        addiMetadataExpectations.add(new AddiMetaData()
                .withSubmitterNumber(123456)
                .withFormat("test-format")
                .withBibliographicRecordId("id_3_2")
                .withTrackingId("t_3_2")
                .withDeleted(true));
        addiMetadataExpectations.add(new AddiMetaData()
                .withSubmitterNumber(123456)
                .withFormat("test-format")
                .withBibliographicRecordId("id_3_3")
                .withTrackingId("t_3_3")
                .withDeleted(false));

        final List<Expectation> addiContentExpectations = new ArrayList<>();
        addiContentExpectations.add(new Expectation("data_3_1"));
        addiContentExpectations.add(new Expectation("data_3_2"));
        addiContentExpectations.add(new Expectation("data_3_3"));

        final AddiFileVerifier addiFileVerifier = new AddiFileVerifier();
        addiFileVerifier.verify(harvesterTmpFile.toFile(), addiMetadataExpectations, addiContentExpectations);
    }

    @Test
    public void recordsHarvestedByTaskList() throws FlowStoreServiceConnectorException {
        final TickleRepoHarvesterConfig config = newConfig();
        config.getContent().withLastBatchHarvested(42);

        final List<AddiMetaData> addiMetadataExpectations = new ArrayList<>();
        addiMetadataExpectations.add(new AddiMetaData()
                .withSubmitterNumber(123456)
                .withFormat("test-format")
                .withBibliographicRecordId("id_3_2")
                .withTrackingId("t_3_2")
                .withDeleted(true));
        addiMetadataExpectations.add(new AddiMetaData()
                .withSubmitterNumber(123456)
                .withFormat("test-format")
                .withBibliographicRecordId("id_3_3")
                .withTrackingId("t_3_3")
                .withDeleted(false));

        final HarvestTask task = new HarvestTask();
        task.setConfigId(config.getId());
        task.setRecords(addiMetadataExpectations);

        final JpaTestEnvironment taskrepo = environment.get("taskrepo");
        taskrepo.getPersistenceContext().run(() -> taskrepo.getEntityManager().persist(task));

        final HarvestOperation harvestOperation = createHarvestOperation(config);
        final int numRecordsHarvested = taskrepo.getPersistenceContext().run(harvestOperation::execute);
        assertThat("Number of records harvested", numRecordsHarvested, is(2));

        final List<Expectation> addiContentExpectations = new ArrayList<>();
        addiContentExpectations.add(new Expectation("data_3_2"));
        addiContentExpectations.add(new Expectation("data_3_3"));

        final AddiFileVerifier addiFileVerifier = new AddiFileVerifier();
        addiFileVerifier.verify(harvesterTmpFile.toFile(), addiMetadataExpectations, addiContentExpectations);

        verify(flowStoreServiceConnector, times(0)).updateHarvesterConfig(any(TickleRepoHarvesterConfig.class));

        assertThat("Task is removed after successful harvest",
                taskrepo.getEntityManager().find(HarvestTask.class, task.getId()), is(nullValue()));
    }

    @Test
    public void taskListContainingNonExistingRecord() {
        final TickleRepoHarvesterConfig config = newConfig();
        config.getContent().withLastBatchHarvested(42);

        final List<AddiMetaData> addiMetaData = new ArrayList<>();
        addiMetaData.add(new AddiMetaData()
                .withSubmitterNumber(123456)
                .withBibliographicRecordId("non-existing"));

        final HarvestTask task = new HarvestTask();
        task.setConfigId(config.getId());
        task.setRecords(addiMetaData);

        final JpaTestEnvironment taskrepo = environment.get("taskrepo");
        taskrepo.getPersistenceContext().run(() -> taskrepo.getEntityManager().persist(task));

        final HarvestOperation harvestOperation = createHarvestOperation(config);
        final int numRecordsHarvested = taskrepo.getPersistenceContext().run(harvestOperation::execute);
        assertThat("Number of records harvested", numRecordsHarvested, is(0));
    }

    @Test
    public void recordsHarvestedByDataSetNameSelector() throws FlowStoreServiceConnectorException {
        final TickleRepoHarvesterConfig config = newConfig();
        config.getContent().withLastBatchHarvested(42);

        final HarvestTask task = new HarvestTask();
        task.setConfigId(config.getId());
        task.setSelector(new HarvestTaskSelector("dataSetName", config.getContent().getDatasetName()));

        final JpaTestEnvironment taskrepo = environment.get("taskrepo");
        taskrepo.getPersistenceContext().run(() -> taskrepo.getEntityManager().persist(task));

        final HarvestOperation harvestOperation = createHarvestOperation(config);
        final JpaTestEnvironment ticklerepo = environment.get("ticklerepo");
        final int numRecordsHarvested = ticklerepo.getPersistenceContext().run(harvestOperation::execute);
        assertThat("Number of records harvested", numRecordsHarvested, is(5));

        verify(flowStoreServiceConnector, times(0)).updateHarvesterConfig(any(TickleRepoHarvesterConfig.class));

        assertThat("Task is removed after successful harvest",
                taskrepo.getEntityManager().find(HarvestTask.class, task.getId()), is(nullValue()));
    }

    @Test
    public void recordsHarvestedByDataSetSelector() throws FlowStoreServiceConnectorException {
        final TickleRepoHarvesterConfig config = newConfig();
        config.getContent().withLastBatchHarvested(42);

        final HarvestTask task = new HarvestTask();
        task.setConfigId(config.getId());
        task.setSelector(new HarvestTaskSelector("dataSet", "1"));

        final JpaTestEnvironment taskrepo = environment.get("taskrepo");
        taskrepo.getPersistenceContext().run(() -> taskrepo.getEntityManager().persist(task));

        final HarvestOperation harvestOperation = createHarvestOperation(config);
        final JpaTestEnvironment ticklerepo = environment.get("ticklerepo");
        final int numRecordsHarvested = ticklerepo.getPersistenceContext().run(harvestOperation::execute);
        assertThat("Number of records harvested", numRecordsHarvested, is(5));

        verify(flowStoreServiceConnector, times(0)).updateHarvesterConfig(any(TickleRepoHarvesterConfig.class));

        assertThat("Task is removed after successful harvest",
                taskrepo.getEntityManager().find(HarvestTask.class, task.getId()), is(nullValue()));
    }

    @Test
    public void dataSetNameSelectorMismatch() throws FlowStoreServiceConnectorException {
        final TickleRepoHarvesterConfig config = newConfig();
        config.getContent().withLastBatchHarvested(42);

        final HarvestTask task = new HarvestTask();
        task.setConfigId(config.getId());
        task.setSelector(new HarvestTaskSelector("dataSetName", "not " + config.getContent().getDatasetName()));

        final JpaTestEnvironment taskrepo = environment.get("taskrepo");
        taskrepo.getPersistenceContext().run(() -> taskrepo.getEntityManager().persist(task));

        final HarvestOperation harvestOperation = createHarvestOperation(config);
        final int numRecordsHarvested = taskrepo.getPersistenceContext().run(harvestOperation::execute);
        assertThat("Number of records harvested", numRecordsHarvested, is(0));

        verify(flowStoreServiceConnector, times(0)).updateHarvesterConfig(any(TickleRepoHarvesterConfig.class));

        assertThat("Task is removed after attempted harvest",
                taskrepo.getEntityManager().find(HarvestTask.class, task.getId()), is(nullValue()));
    }

    @Test
    public void dataSetSelectorMismatch() throws FlowStoreServiceConnectorException {
        final TickleRepoHarvesterConfig config = newConfig();
        config.getContent().withLastBatchHarvested(42);

        final HarvestTask task = new HarvestTask();
        task.setConfigId(config.getId());
        task.setSelector(new HarvestTaskSelector("dataSet", "123456"));

        final JpaTestEnvironment taskrepo = environment.get("taskrepo");
        taskrepo.getPersistenceContext().run(() -> taskrepo.getEntityManager().persist(task));

        final HarvestOperation harvestOperation = createHarvestOperation(config);
        final int numRecordsHarvested = taskrepo.getPersistenceContext().run(harvestOperation::execute);
        assertThat("Number of records harvested", numRecordsHarvested, is(0));

        verify(flowStoreServiceConnector, times(0)).updateHarvesterConfig(any(TickleRepoHarvesterConfig.class));

        assertThat("Task is removed after attempted harvest",
                taskrepo.getEntityManager().find(HarvestTask.class, task.getId()), is(nullValue()));
    }

    @Test
    public void harvestTaskPreservedInCaseOfException() throws JobStoreServiceConnectorException {
        final TickleRepoHarvesterConfig config = newConfig();
        config.getContent().withLastBatchHarvested(42);

        final HarvestTask task = new HarvestTask();
        task.setConfigId(config.getId());
        task.setRecords(Collections.singletonList(
                new AddiMetaData().withBibliographicRecordId("id_3_2")));

        final JpaTestEnvironment taskrepo = environment.get("taskrepo");
        taskrepo.getPersistenceContext().run(() -> taskrepo.getEntityManager().persist(task));

        when(jobStoreServiceConnector.addJob(any(JobInputStream.class)))
                .thenThrow(new JobStoreServiceConnectorException("died"));

        final HarvestOperation harvestOperation = createHarvestOperation(config);
        try {
            taskrepo.getPersistenceContext().run(harvestOperation::execute);
        } catch (RuntimeException e) {}

        assertThat("Task remains after failed harvest",
                taskrepo.getEntityManager().find(HarvestTask.class, task.getId()), is(notNullValue()));
    }

    @Test
    public void noBatchOrTaskExist() throws HarvesterException {
        final TickleRepoHarvesterConfig config = newConfig();
        config.getContent().withLastBatchHarvested(42);

        final HarvestOperation harvestOperation = createHarvestOperation(config);
        assertThat("Number of records harvested", harvestOperation.execute(), is(0));
    }

    @Test
    public void reSubmitConfigUpdates() throws HarvesterException, JobStoreServiceConnectorException, FlowStoreServiceConnectorException {
        when(jobStoreServiceConnector.listJobs(any(JobListCriteria.class)))
                .thenReturn(Collections.singletonList(new JobInfoSnapshot()))
                .thenReturn(Collections.singletonList(new JobInfoSnapshot()))
                .thenReturn(Collections.emptyList());

        final TickleRepoHarvesterConfig config = newConfig();
        config.getContent().withLastBatchHarvested(0);
        final HarvestOperation harvestOperation = createHarvestOperation(config);
        final JpaTestEnvironment env = environment.get("ticklerepo");
        final int numberOfRecordsHarvested = env.getPersistenceContext().run(harvestOperation::execute);
        assertThat("Number of records harvested", numberOfRecordsHarvested, is(3));
        assertThat("Time of last batch harvested", config.getContent().getTimeOfLastBatchHarvested(), is(notNullValue()));
        verify(flowStoreServiceConnector, times(3)).updateHarvesterConfig(any(TickleRepoHarvesterConfig.class));
    }

    private HarvestOperation createHarvestOperation(TickleRepoHarvesterConfig config) {
        try {
            return new HarvestOperation(config,
                    flowStoreServiceConnector,
                    new BinaryFileStoreFsImpl(tmpFolder.newFolder().toPath()),
                    fileStoreServiceConnector,
                    jobStoreServiceConnector,
                    new TickleRepo(environment.get("ticklerepo").getEntityManager()),
                    new TaskRepo(environment.get("taskrepo").getEntityManager()));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private TickleRepoHarvesterConfig newConfig() {
        final TickleRepoHarvesterConfig config = new TickleRepoHarvesterConfig(1, 1, new TickleRepoHarvesterConfig.Content());
        config.getContent()
                .withDatasetName("dataset")
                .withFormat("test-format")
                .withDestination("test-destination")
                .withType(JobSpecification.Type.TEST)
                .withLastBatchHarvested(0);
        return config;
    }
}
