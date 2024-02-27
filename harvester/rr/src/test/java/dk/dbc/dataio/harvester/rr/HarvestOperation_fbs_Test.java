package dk.dbc.dataio.harvester.rr;

import dk.dbc.dataio.bfs.api.BinaryFileStoreFsImpl;
import dk.dbc.dataio.commons.types.AddiMetaData;
import dk.dbc.dataio.commons.types.Diagnostic;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.utils.jobstore.MockedJobStoreServiceConnector;
import dk.dbc.dataio.filestore.service.connector.MockedFileStoreServiceConnector;
import dk.dbc.dataio.harvester.task.TaskRepo;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.RRHarvesterConfig;
import dk.dbc.dataio.harvester.utils.datafileverifier.AddiFileVerifier;
import dk.dbc.dataio.harvester.utils.datafileverifier.MarcExchangeCollectionExpectation;
import dk.dbc.dataio.harvester.utils.datafileverifier.MarcExchangeRecordExpectation;
import dk.dbc.dataio.harvester.utils.datafileverifier.XmlExpectation;
import dk.dbc.dataio.harvester.utils.rawrepo.RawRepoConnector;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.rawrepo.dto.RecordDTO;
import dk.dbc.rawrepo.dto.RecordIdDTO;
import dk.dbc.rawrepo.queue.ConfigurationException;
import dk.dbc.rawrepo.queue.QueueException;
import dk.dbc.rawrepo.queue.QueueItem;
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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HarvestOperation_fbs_Test {
    public static final MetricRegistry metricRegistry = mock(MetricRegistry.class);
    private static final Date QUEUED_TIME = new Date(1467277697583L); // 2016-06-30 11:08:17.583
    private static final String CONSUMER_ID = "consumerId";
    private static final int AGENCY_ID = 123456;
    private static final RawRepoConnector RAW_REPO_CONNECTOR = mock(RawRepoConnector.class);
    private static final RecordServiceConnector RAW_REPO_RECORD_SERVICE_CONNECTOR = mock(RecordServiceConnector.class);
    private static final RecordIdDTO FIRST_RECORD_ID = new RecordIdDTO("first", AGENCY_ID);
    private static final String FIRST_RECORD_CONTENT = HarvestOperationTest.getRecordContent(FIRST_RECORD_ID);
    private static final QueueItem FIRST_QUEUE_ITEM = HarvestOperationTest.getQueueItem(FIRST_RECORD_ID, QUEUED_TIME);
    private static final RecordDTO FIRST_RECORD = new RecordDTO();
    private static final RecordIdDTO FIRST_RECORD_HEAD_ID = new RecordIdDTO("first-head", AGENCY_ID);
    private static final RecordDTO FIRST_RECORD_HEAD = new RecordDTO();
    private static final RecordIdDTO SECOND_RECORD_ID = new RecordIdDTO("second", AGENCY_ID);
    private static final String SECOND_RECORD_CONTENT = HarvestOperationTest.getRecordContent(SECOND_RECORD_ID);
    private static final QueueItem SECOND_QUEUE_ITEM = HarvestOperationTest.getQueueItem(SECOND_RECORD_ID, QUEUED_TIME);
    private static final RecordDTO SECOND_RECORD = new RecordDTO();
    private static final RecordIdDTO THIRD_RECORD_ID = new RecordIdDTO("third", AGENCY_ID);
    private static final String THIRD_RECORD_CONTENT = HarvestOperationTest.getRecordContent(THIRD_RECORD_ID);
    private static final QueueItem THIRD_QUEUE_ITEM = HarvestOperationTest.getQueueItem(THIRD_RECORD_ID, QUEUED_TIME);
    private static final RecordDTO THIRD_RECORD = new RecordDTO();
    private static final VipCoreConnection VIP_CORE_CONNECTION = mock(VipCoreConnection.class);

    static {
        FIRST_RECORD.setRecordId(FIRST_RECORD_ID);
        FIRST_RECORD.setCreated(Instant.now().toString());
        FIRST_RECORD.setContent(FIRST_RECORD_CONTENT.getBytes(StandardCharsets.UTF_8));
        FIRST_RECORD.setEnrichmentTrail("trail");
        FIRST_RECORD.setTrackingId("tracking id");

        FIRST_RECORD_HEAD.setRecordId(FIRST_RECORD_HEAD_ID);
        FIRST_RECORD_HEAD.setCreated(Instant.now().toString());

        SECOND_RECORD.setRecordId(SECOND_RECORD_ID);
        SECOND_RECORD.setCreated(Instant.now().toString());
        SECOND_RECORD.setContent(SECOND_RECORD_CONTENT.getBytes(StandardCharsets.UTF_8));
        SECOND_RECORD.setTrackingId(null);

        THIRD_RECORD.setRecordId(THIRD_RECORD_ID);
        THIRD_RECORD.setCreated(Instant.now().toString());
        THIRD_RECORD.setContent(THIRD_RECORD_CONTENT.getBytes(StandardCharsets.UTF_8));
    }

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
    private List<XmlExpectation> recordsExpectations;

    @BeforeEach
    public void setupMocks() throws SQLException, IOException, QueueException {
        // Mock rawrepo return values
        when(RAW_REPO_CONNECTOR.dequeue(CONSUMER_ID)).thenReturn(FIRST_QUEUE_ITEM).thenReturn(SECOND_QUEUE_ITEM).thenReturn(THIRD_QUEUE_ITEM).thenReturn(null);

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
        when(RAW_REPO_RECORD_SERVICE_CONNECTOR.getRecordDataCollectionDataIO(any(RecordIdDTO.class), any(RecordServiceConnector.Params.class))).thenReturn(new HashMap<>() {{
            put(FIRST_RECORD_HEAD_ID.getBibliographicRecordId(), FIRST_RECORD_HEAD);
            put(FIRST_RECORD_ID.getBibliographicRecordId(), FIRST_RECORD);
        }}).thenReturn(new HashMap<>() {{
            put(SECOND_RECORD_ID.getBibliographicRecordId(), SECOND_RECORD);
        }}).thenReturn(new HashMap<>() {{
            put(THIRD_RECORD_ID.getBibliographicRecordId(), THIRD_RECORD);
        }});

        when(RAW_REPO_RECORD_SERVICE_CONNECTOR.recordFetch(any(RecordIdDTO.class))).thenReturn(FIRST_RECORD).thenReturn(SECOND_RECORD).thenReturn(THIRD_RECORD);

        // Setup harvester datafile content expectations

        MarcExchangeCollectionExpectation marcExchangeCollectionExpectation1 = new MarcExchangeCollectionExpectation();
        marcExchangeCollectionExpectation1.records.add(getMarcExchangeRecord(FIRST_RECORD_ID));
        recordsExpectations.add(marcExchangeCollectionExpectation1);
        recordsAddiMetaDataExpectations.add(new AddiMetaData().withBibliographicRecordId(FIRST_RECORD.getRecordId().getBibliographicRecordId()).withSubmitterNumber(FIRST_RECORD.getRecordId().getAgencyId()).withFormat("format").withCreationDate(Date.from(Instant.parse(FIRST_RECORD.getCreated()))).withEnrichmentTrail(FIRST_RECORD.getEnrichmentTrail()).withTrackingId(FIRST_RECORD.getTrackingId()).withDeleted(false).withLibraryRules(new AddiMetaData.LibraryRules()));

        MarcExchangeCollectionExpectation marcExchangeCollectionExpectation2 = new MarcExchangeCollectionExpectation();
        marcExchangeCollectionExpectation2.records.add(getMarcExchangeRecord(SECOND_RECORD_ID));
        recordsExpectations.add(marcExchangeCollectionExpectation2);
        recordsAddiMetaDataExpectations.add(new AddiMetaData().withBibliographicRecordId(SECOND_RECORD.getRecordId().getBibliographicRecordId()).withSubmitterNumber(SECOND_RECORD.getRecordId().getAgencyId()).withFormat("format").withCreationDate(Date.from(Instant.parse(SECOND_RECORD.getCreated()))).withDeleted(false).withLibraryRules(new AddiMetaData.LibraryRules()));

        MarcExchangeCollectionExpectation marcExchangeCollectionExpectation3 = new MarcExchangeCollectionExpectation();
        marcExchangeCollectionExpectation3.records.add(getMarcExchangeRecord(THIRD_RECORD_ID));
        recordsExpectations.add(marcExchangeCollectionExpectation3);
        recordsAddiMetaDataExpectations.add(new AddiMetaData().withBibliographicRecordId(THIRD_RECORD.getRecordId().getBibliographicRecordId()).withSubmitterNumber(THIRD_RECORD.getRecordId().getAgencyId()).withFormat("format").withCreationDate(Date.from(Instant.parse(THIRD_RECORD.getCreated()))).withDeleted(false).withLibraryRules(new AddiMetaData.LibraryRules()));

        HarvestOperation harvestOperation = newHarvestOperation();
        harvestOperation.execute();

        verifyHarvesterDataFiles();
        verifyJobSpecifications();
    }

    @Test
    public void execute_recordIsInvalid_recordIsFailed() throws HarvesterException, RecordServiceConnectorException {
        RecordDTO invalidRecord = new RecordDTO();
        invalidRecord.setRecordId(SECOND_RECORD_ID);
        invalidRecord.setCreated(Instant.now().toString());
        invalidRecord.setContent("not xml".getBytes(StandardCharsets.UTF_8));

        // Mock rawrepo return values
        when(RAW_REPO_RECORD_SERVICE_CONNECTOR.getRecordDataCollectionDataIO(any(RecordIdDTO.class), any(RecordServiceConnector.Params.class))).thenReturn(new HashMap<>() {{
            put(FIRST_RECORD_ID.getBibliographicRecordId(), FIRST_RECORD);
        }}).thenReturn(new HashMap<>() {{
            put(invalidRecord.getRecordId().toString(), invalidRecord);
        }}).thenReturn(new HashMap<>() {{
            put(THIRD_RECORD_ID.getBibliographicRecordId(), THIRD_RECORD);
        }});

        when(RAW_REPO_RECORD_SERVICE_CONNECTOR.recordFetch(any(RecordIdDTO.class))).thenReturn(FIRST_RECORD).thenReturn(SECOND_RECORD).thenReturn(THIRD_RECORD);

        // Setup harvester datafile content expectations
        MarcExchangeCollectionExpectation marcExchangeCollectionExpectation1 = new MarcExchangeCollectionExpectation();
        marcExchangeCollectionExpectation1.records.add(getMarcExchangeRecord(FIRST_RECORD_ID));
        recordsExpectations.add(marcExchangeCollectionExpectation1);
        recordsAddiMetaDataExpectations.add(new AddiMetaData().withBibliographicRecordId(FIRST_RECORD.getRecordId().getBibliographicRecordId()).withSubmitterNumber(FIRST_RECORD.getRecordId().getAgencyId()).withFormat("format").withCreationDate(Date.from(Instant.parse(FIRST_RECORD.getCreated()))).withEnrichmentTrail(FIRST_RECORD.getEnrichmentTrail()).withTrackingId(FIRST_RECORD.getTrackingId()).withDeleted(false).withLibraryRules(new AddiMetaData.LibraryRules()));

        recordsExpectations.add(null);
        recordsAddiMetaDataExpectations.add(new AddiMetaData().withBibliographicRecordId(SECOND_RECORD.getRecordId().getBibliographicRecordId()).withSubmitterNumber(SECOND_RECORD.getRecordId().getAgencyId()).withCreationDate(Date.from(Instant.parse(SECOND_RECORD.getCreated()))).withEnrichmentTrail(SECOND_RECORD.getEnrichmentTrail()).withTrackingId(SECOND_RECORD.getTrackingId()).withDiagnostic(new Diagnostic(Diagnostic.Level.FATAL, String.format("Harvesting RawRepo %s failed: Record %s was not found in returned collection", SECOND_RECORD.getRecordId(), SECOND_RECORD.getRecordId()))).withDeleted(false).withLibraryRules(new AddiMetaData.LibraryRules()));

        MarcExchangeCollectionExpectation marcExchangeCollectionExpectation2 = new MarcExchangeCollectionExpectation();
        marcExchangeCollectionExpectation2.records.add(getMarcExchangeRecord(THIRD_RECORD_ID));
        recordsExpectations.add(marcExchangeCollectionExpectation2);
        recordsAddiMetaDataExpectations.add(new AddiMetaData().withBibliographicRecordId(THIRD_RECORD.getRecordId().getBibliographicRecordId()).withSubmitterNumber(THIRD_RECORD.getRecordId().getAgencyId()).withFormat("format").withCreationDate(Date.from(Instant.parse(THIRD_RECORD.getCreated()))).withDeleted(false).withLibraryRules(new AddiMetaData.LibraryRules()));

        HarvestOperation harvestOperation = newHarvestOperation();
        harvestOperation.execute();

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

    private MarcExchangeRecordExpectation getMarcExchangeRecord(RecordIdDTO recordId) {
        return new MarcExchangeRecordExpectation(recordId.getBibliographicRecordId(), recordId.getAgencyId());
    }

    private HarvestOperation newHarvestOperation() {
        HarvesterJobBuilderFactory harvesterJobBuilderFactory;
        try {
            Path dir = Files.createDirectory(tmpFolder.resolve(UUID.randomUUID().toString()));
            harvesterJobBuilderFactory = new HarvesterJobBuilderFactory(new BinaryFileStoreFsImpl(dir), mockedFileStoreServiceConnector, mockedJobStoreServiceConnector);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        RRHarvesterConfig config = HarvesterTestUtil.getRRHarvesterConfig();
        config.getContent().withFormat("format").withConsumerId(CONSUMER_ID);
        try {
            return new HarvestOperation(config, harvesterJobBuilderFactory, taskRepo, VIP_CORE_CONNECTION, RAW_REPO_CONNECTOR, RAW_REPO_RECORD_SERVICE_CONNECTOR, metricRegistry);
        } catch (QueueException | SQLException | ConfigurationException e) {
            throw new IllegalStateException(e);
        }
    }
}
