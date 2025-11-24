package dk.dbc.dataio.harvester.rr_dm3;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import dk.dbc.dataio.bfs.api.BinaryFileStoreFsImpl;
import dk.dbc.dataio.commons.types.AddiMetaData;
import dk.dbc.dataio.commons.types.Diagnostic;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.utils.jobstore.MockedJobStoreServiceConnector;
import dk.dbc.dataio.filestore.service.connector.MockedFileStoreServiceConnector;
import dk.dbc.dataio.harvester.task.TaskRepo;
import dk.dbc.dataio.harvester.task.entity.HarvestTask;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.RRV3HarvesterConfig;
import dk.dbc.dataio.harvester.utils.datafileverifier.AddiFileVerifier;
import dk.dbc.dataio.harvester.utils.datafileverifier.Expectation;
import dk.dbc.dataio.harvester.utils.rawrepo.RawRepo3Connector;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.rawrepo.dto.RecordEntryDTO;
import dk.dbc.rawrepo.dto.RecordIdDTO;
import dk.dbc.rawrepo.queue.ConfigurationException;
import dk.dbc.rawrepo.queue.QueueException;
import dk.dbc.rawrepo.record.RecordServiceConnector;
import dk.dbc.rawrepo.record.RecordServiceConnectorException;
import jakarta.persistence.EntityManager;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.Tag;
import org.eclipse.microprofile.metrics.Timer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static dk.dbc.dataio.harvester.rr_dm3.RecordEntryBuilder.AGENCY_ID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HarvestOperationFbsTest {
    public static final MetricRegistry metricRegistry = mock(MetricRegistry.class);
    private static final Date QUEUED_TIME = new Date(1467277697583L); // 2016-06-30 11:08:17.583
    private static final String CONSUMER_ID = "consumerId";
    private static RawRepo3Connector rawRepo3Connector = mock(RawRepo3Connector.class);
    private static final RecordServiceConnector RAW_REPO_RECORD_SERVICE_CONNECTOR = mock(RecordServiceConnector.class);
    private static final RecordEntryDTO FIRST_RECORD = new RecordEntryBuilder().defaults("first")
            .trail("trail")
            .trackingId()
            .build();
    private static final RecordEntryDTO FIRST_RECORD_HEAD = new RecordEntryBuilder()
            .id(new RecordIdDTO("first-head", AGENCY_ID))
            .createdNow()
            .build();
    private static final RecordEntryDTO SECOND_RECORD = new RecordEntryBuilder().defaults("second")
            .set(r -> r.setTrackingId(null))
            .build();
    private static final RecordEntryDTO THIRD_RECORD = new RecordEntryBuilder().defaults("third").build();
    private static final VipCoreConnection VIP_CORE_CONNECTION = mock(VipCoreConnection.class);
    private final EntityManager entityManager = mock(EntityManager.class);
    private final TaskRepo taskRepo = new TaskRepo(entityManager);
    private final Timer timer = mock(Timer.class);
    private final Counter counter = mock(Counter.class);
    @TempDir
    public Path tmpFolder;
    private MockedJobStoreServiceConnector mockedJobStoreServiceConnector;
    private MockedFileStoreServiceConnector mockedFileStoreServiceConnector;
    private File harvesterDataFile;
    private List<AddiMetaData> recordsAddiMetaDataExpectations;
    private List<Expectation> recordsExpectations;

    @BeforeEach
    public void setupMocks() throws IOException {
        rawRepo3Connector = HarvestOperationTest.rawRepo3Connector(FIRST_RECORD.getRecordId(), SECOND_RECORD.getRecordId(), THIRD_RECORD.getRecordId());

        // Intercept harvester data files with mocked FileStoreServiceConnector
        harvesterDataFile = Files.createFile(tmpFolder.resolve(UUID.randomUUID() + ".tmp")).toFile();
        mockedFileStoreServiceConnector = new MockedFileStoreServiceConnector();
        mockedFileStoreServiceConnector.destinations.add(harvesterDataFile.toPath());

        // Intercept harvester job specifications with mocked JobStoreServiceConnector
        mockedJobStoreServiceConnector = new MockedJobStoreServiceConnector();
        mockedJobStoreServiceConnector.jobInfoSnapshots.add(new JobInfoSnapshot());

        recordsAddiMetaDataExpectations = new ArrayList<>();
        recordsExpectations = new ArrayList<>();

        when(metricRegistry.timer(any(Metadata.class), any(Tag.class))).thenReturn(timer);
        when(metricRegistry.counter(any(Metadata.class), any(Tag.class))).thenReturn(counter);
        doNothing().when(timer).update(any(Duration.class));
        doNothing().when(counter).inc();
    }

    @Test
    public void execute_multipleRecordsHarvested_dataFileContainsMarcExchangeCollections() throws HarvesterException, RecordServiceConnectorException {
        // Mock rawrepo return values
        when(RAW_REPO_RECORD_SERVICE_CONNECTOR.getRecordDataCollectionDataIO(any(RecordIdDTO.class), any(RecordServiceConnector.Params.class)))
                .thenReturn(List.of(FIRST_RECORD_HEAD, FIRST_RECORD))
                .thenReturn(List.of(SECOND_RECORD))
                .thenReturn(List.of(THIRD_RECORD));

        when(RAW_REPO_RECORD_SERVICE_CONNECTOR.getRecordData(any(RecordIdDTO.class))).thenReturn(FIRST_RECORD).thenReturn(SECOND_RECORD).thenReturn(THIRD_RECORD);

        // Setup harvester datafile content expectations

        recordsExpectations.add(Expectations.of(FIRST_RECORD));
        recordsAddiMetaDataExpectations.add(new AddiMetaData().withBibliographicRecordId(FIRST_RECORD.getRecordId().getBibliographicRecordId()).withSubmitterNumber(FIRST_RECORD.getRecordId().getAgencyId()).withFormat("format").withCreationDate(Date.from(Instant.parse(FIRST_RECORD.getCreated()))).withEnrichmentTrail(FIRST_RECORD.getEnrichmentTrail()).withTrackingId(FIRST_RECORD.getTrackingId()).withDeleted(false).withLibraryRules(new AddiMetaData.LibraryRules()));

        recordsExpectations.add(Expectations.of(SECOND_RECORD));
        recordsAddiMetaDataExpectations.add(new AddiMetaData().withBibliographicRecordId(SECOND_RECORD.getRecordId().getBibliographicRecordId()).withSubmitterNumber(SECOND_RECORD.getRecordId().getAgencyId()).withFormat("format").withCreationDate(Date.from(Instant.parse(SECOND_RECORD.getCreated()))).withDeleted(false).withLibraryRules(new AddiMetaData.LibraryRules()));

        recordsExpectations.add(Expectations.of(THIRD_RECORD));
        recordsAddiMetaDataExpectations.add(new AddiMetaData().withBibliographicRecordId(THIRD_RECORD.getRecordId().getBibliographicRecordId()).withSubmitterNumber(THIRD_RECORD.getRecordId().getAgencyId()).withFormat("format").withCreationDate(Date.from(Instant.parse(THIRD_RECORD.getCreated()))).withDeleted(false).withLibraryRules(new AddiMetaData.LibraryRules()));

        HarvestOperation harvestOperation = newHarvestOperation();
        harvestOperation.execute();

        verifyHarvesterDataFiles();
        verifyJobSpecifications();
    }

    @Test
    public void execute_recordIsInvalid_recordIsFailed() throws HarvesterException, RecordServiceConnectorException {
        RecordEntryDTO invalidRecord = new RecordEntryDTO();
        invalidRecord.setRecordId(SECOND_RECORD.getRecordId());
        invalidRecord.setCreated(Instant.now().toString());
        invalidRecord.setContent(JsonNodeFactory.instance.textNode("garbage"));

        // Mock rawrepo return values
        when(RAW_REPO_RECORD_SERVICE_CONNECTOR.getRecordDataCollectionDataIO(any(RecordIdDTO.class), any(RecordServiceConnector.Params.class)))
                .thenReturn(List.of(FIRST_RECORD))
                .thenReturn(List.of(invalidRecord))
                .thenReturn(List.of(THIRD_RECORD));

        when(RAW_REPO_RECORD_SERVICE_CONNECTOR.getRecordData(any(RecordIdDTO.class))).thenReturn(FIRST_RECORD).thenReturn(SECOND_RECORD).thenReturn(THIRD_RECORD);

        // Setup harvester datafile content expectations
        recordsExpectations.add(Expectations.of(FIRST_RECORD));
        recordsAddiMetaDataExpectations.add(new AddiMetaData().withBibliographicRecordId(FIRST_RECORD.getRecordId().getBibliographicRecordId()).withSubmitterNumber(FIRST_RECORD.getRecordId().getAgencyId()).withFormat("format").withCreationDate(Date.from(Instant.parse(FIRST_RECORD.getCreated()))).withEnrichmentTrail(FIRST_RECORD.getEnrichmentTrail()).withTrackingId(FIRST_RECORD.getTrackingId()).withDeleted(false).withLibraryRules(new AddiMetaData.LibraryRules()));

        recordsExpectations.add(null);
        recordsAddiMetaDataExpectations.add(new AddiMetaData().withBibliographicRecordId(SECOND_RECORD.getRecordId().getBibliographicRecordId()).withSubmitterNumber(SECOND_RECORD.getRecordId().getAgencyId()).withFormat("format").withCreationDate(Date.from(Instant.parse(SECOND_RECORD.getCreated()))).withEnrichmentTrail(SECOND_RECORD.getEnrichmentTrail()).withTrackingId(SECOND_RECORD.getTrackingId()).withDiagnostic(new Diagnostic(Diagnostic.Level.FATAL, "No marcXchange record found")).withDeleted(false).withLibraryRules(new AddiMetaData.LibraryRules()));

        recordsExpectations.add(Expectations.of(THIRD_RECORD));
        recordsAddiMetaDataExpectations.add(new AddiMetaData().withBibliographicRecordId(THIRD_RECORD.getRecordId().getBibliographicRecordId()).withSubmitterNumber(THIRD_RECORD.getRecordId().getAgencyId()).withFormat("format").withCreationDate(Date.from(Instant.parse(THIRD_RECORD.getCreated()))).withDeleted(false).withLibraryRules(new AddiMetaData.LibraryRules()));

        HarvestOperation harvestOperation = newHarvestOperation();
        harvestOperation.execute();
        harvestOperation.close();
        verifyHarvesterDataFiles();
        verifyJobSpecifications();
    }

    private void verifyHarvesterDataFiles() {
        AddiFileVerifier addiFileVerifier = new AddiFileVerifier();
        addiFileVerifier.verify(harvesterDataFile, recordsAddiMetaDataExpectations, recordsExpectations);
    }

    private void verifyJobSpecifications() {
        verifyJobSpecification(mockedJobStoreServiceConnector.jobInputStreams.remove().getJobSpecification(), newHarvestOperation().getJobSpecificationTemplate(AGENCY_ID));
    }

    private void verifyJobSpecification(JobSpecification jobSpecification, JobSpecification jobSpecificationTemplate) {
        assertThat("JobSpecification.packaging", jobSpecification.getPackaging(), is(jobSpecificationTemplate.getPackaging()));
        assertThat("JobSpecification.format", jobSpecification.getFormat(), is(jobSpecificationTemplate.getFormat()));
        assertThat("JobSpecification.charset", jobSpecification.getCharset(), is(jobSpecificationTemplate.getCharset()));
        assertThat("JobSpecification.destination", jobSpecification.getDestination(), is(jobSpecificationTemplate.getDestination()));
        assertThat("JobSpecification.submitterId", jobSpecification.getSubmitterId(), is(jobSpecificationTemplate.getSubmitterId()));
    }

    private HarvestOperation newHarvestOperation() {
        HarvesterJobBuilderFactory harvesterJobBuilderFactory;
        try {
            Path dir = Files.createDirectory(tmpFolder.resolve(UUID.randomUUID().toString()));
            harvesterJobBuilderFactory = new HarvesterJobBuilderFactory(new BinaryFileStoreFsImpl(dir), mockedFileStoreServiceConnector, mockedJobStoreServiceConnector);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        RRV3HarvesterConfig config = HarvesterTestUtil.getRRHarvesterConfig();
        config.getContent().withFormat("format").withConsumerId(CONSUMER_ID);
        try {
            return new HarvestOperation("test:0", config, harvesterJobBuilderFactory, newTaskRepo(), VIP_CORE_CONNECTION, rawRepo3Connector, RAW_REPO_RECORD_SERVICE_CONNECTOR, metricRegistry);
        } catch (QueueException | SQLException | ConfigurationException e) {
            throw new IllegalStateException(e);
        }
    }

    public TaskRepo newTaskRepo() {
        return new TaskRepo(entityManager) {
            @Override
            public Optional<HarvestTask> findNextHarvestTask(long configId) {
                return Optional.of(mock(HarvestTask.class));
            }
        };
    }
}
