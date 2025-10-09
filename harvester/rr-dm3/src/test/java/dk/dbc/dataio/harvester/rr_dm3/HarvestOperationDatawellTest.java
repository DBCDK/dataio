package dk.dbc.dataio.harvester.rr_dm3;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
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
import dk.dbc.dataio.harvester.utils.datafileverifier.Expectation;
import dk.dbc.dataio.harvester.utils.rawrepo.RawRepoConnector;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.rawrepo.dto.RecordEntryDTO;
import dk.dbc.rawrepo.dto.RecordIdDTO;
import dk.dbc.rawrepo.queue.ConfigurationException;
import dk.dbc.rawrepo.queue.QueueException;
import dk.dbc.rawrepo.queue.QueueItem;
import dk.dbc.rawrepo.queue.RawRepoQueueDAO;
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
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HarvestOperationDatawellTest implements TempFiles {
    private static final Date QUEUED_TIME = new Date(1467277697583L); // 2016-06-30 11:08:17.583
    private static final String CONSUMER_ID = "consumerId";
    private static final int LOCAL_LIBRARY = 700000;

    private final static RawRepoConnector RAW_REPO_CONNECTOR = mock(RawRepoConnector.class);
    private final static VipCoreConnection VIP_CORE_CONNECTION = mock(VipCoreConnection.class);
    private final static RecordServiceConnector RAW_REPO_RECORD_SERVICE_CONNECTOR = mock(RecordServiceConnector.class);

    private final static HashMap<String, String> QUEUE_DAO_CONFIGURATION = new HashMap<>(Map.of("RAWREPO_RECORD_URL", "http://localhost:4221"));
    private final static RawRepoQueueDAO QUEUE_DAO = mock(RawRepoQueueDAO.class);

    /* 1st record is a DBC record */
    private final static RecordEntryDTO FIRST_RECORD = new RecordEntryBuilder().defaults("first", HarvestOperation.DBC_LIBRARY)
            .trail("191919,870970")
            .trackingId()
            .build();
    private final static QueueItem FIRST_QUEUE_ITEM = HarvestOperationTest.getQueueItem(FIRST_RECORD.getRecordId(), QUEUED_TIME);
    private final static RecordEntryDTO FIRST_RECORD_WITHOUT_ENRICHMENT_TRAIL = new RecordEntryBuilder()
            .id(FIRST_RECORD.getRecordId())
            .set(r -> r.setCreated(FIRST_RECORD.getCreated()))
            .trackingId()
            .build();
    private final static RecordEntryDTO FIRST_RECORD_HEAD = new RecordEntryBuilder().defaults("first-head", HarvestOperation.DBC_LIBRARY).build();

    private final static RecordEntryDTO FIRST_RECORD_SECTION = new RecordEntryBuilder().defaults("first-section", HarvestOperation.DBC_LIBRARY).build();

    /* 2nd record is a local record */
    private final static RecordEntryDTO SECOND_RECORD = new RecordEntryBuilder().defaults("second", LOCAL_LIBRARY).build();
    private final static QueueItem SECOND_QUEUE_ITEM = HarvestOperationTest.getQueueItem(SECOND_RECORD.getRecordId(), QUEUED_TIME);

    /* 3rd record is a DBC record */
    private final static RecordEntryDTO THIRD_RECORD = new RecordEntryBuilder()
            .defaults("third", HarvestOperation.DBC_LIBRARY)
            .trail("191919,870970")
            .build();
    private final static QueueItem THIRD_QUEUE_ITEM = HarvestOperationTest.getQueueItem(THIRD_RECORD.getRecordId(), QUEUED_TIME);

    private final static RecordEntryDTO THIRD_RECORD_WITHOUT_ENRICHMENT_TRAIL = new RecordEntryBuilder()
            .id(THIRD_RECORD.getRecordId())
            .set(r -> r.setCreated(THIRD_RECORD.getCreated()))
            .build();

    private final EntityManager entityManager = mock(EntityManager.class);
    private final TaskRepo taskRepo = new TaskRepo(entityManager);

    private MockedJobStoreServiceConnector mockedJobStoreServiceConnector;
    private MockedFileStoreServiceConnector mockedFileStoreServiceConnector;
    private File harvesterDataFileWithDbcRecords;
    private File harvesterDataFileWithLocalRecords;
    private List<AddiMetaData> dbcRecordsAddiMetaDataExpectations;
    private List<AddiMetaData> localRecordsAddiMetaDataExpectations;
    private List<Expectation> dbcRecordsExpectations;
    private List<Expectation> localRecordsExpectations;

    private final AddiMetaData.LibraryRules localLibraryRules = new AddiMetaData.LibraryRules()
            .withLibraryRule("rule1", true);

    public static final MetricRegistry metricRegistry = mock(MetricRegistry.class);
    private final Counter counter = mock(Counter.class);
    private final Timer timer = mock(Timer.class);

    @TempDir
    public Path tmpFolder;

    @BeforeEach
    public void setupMocks() throws SQLException, IOException, ConfigurationException, QueueException {
        // Mock rawrepo return values
        when(RAW_REPO_CONNECTOR.dequeue(CONSUMER_ID))
                .thenReturn(FIRST_QUEUE_ITEM)
                .thenReturn(SECOND_QUEUE_ITEM)
                .thenReturn(THIRD_QUEUE_ITEM)
                .thenReturn(null);

        // Intercept harvester data files with mocked FileStoreServiceConnectorBean
        harvesterDataFileWithDbcRecords = createFile(tmpFolder);
        harvesterDataFileWithLocalRecords = createFile(tmpFolder);
        mockedFileStoreServiceConnector = new MockedFileStoreServiceConnector();
        mockedFileStoreServiceConnector.destinations.add(harvesterDataFileWithDbcRecords.toPath());
        mockedFileStoreServiceConnector.destinations.add(harvesterDataFileWithLocalRecords.toPath());

        // Intercept harvester job specifications with mocked JobStoreServiceConnectorBean
        mockedJobStoreServiceConnector = new MockedJobStoreServiceConnector();
        mockedJobStoreServiceConnector.jobInfoSnapshots.add(new JobInfoSnapshot());
        mockedJobStoreServiceConnector.jobInfoSnapshots.add(new JobInfoSnapshot());

        dbcRecordsAddiMetaDataExpectations = new ArrayList<>();
        localRecordsAddiMetaDataExpectations = new ArrayList<>();
        dbcRecordsExpectations = new ArrayList<>();
        localRecordsExpectations = new ArrayList<>();


        // mock vipcore calls for non-DBC libraries
        when(VIP_CORE_CONNECTION.getLibraryRules(LOCAL_LIBRARY, null)).thenReturn(localLibraryRules);

        when(QUEUE_DAO.getConfiguration())
                .thenReturn(QUEUE_DAO_CONFIGURATION);

        when(metricRegistry.counter(any(Metadata.class), any(Tag.class))).thenReturn(counter);
        when(metricRegistry.timer(any(Metadata.class), any(Tag.class))).thenReturn(timer);
        doNothing().when(counter).inc();
        doNothing().when(timer).update(any(Duration.class));
    }

    @Test
    public void harvest_multipleAgencyIdsHarvested_agencyIdsInSeparateJobs()
            throws HarvesterException, RecordServiceConnectorException {
        // Mock rawrepo return values
        when(RAW_REPO_RECORD_SERVICE_CONNECTOR.getRecordDataCollectionDataIO(any(RecordIdDTO.class), any(RecordServiceConnector.Params.class)))
                .thenReturn(List.of(FIRST_RECORD_HEAD, FIRST_RECORD_SECTION, FIRST_RECORD))
                .thenReturn(List.of(SECOND_RECORD))
                .thenReturn(List.of(THIRD_RECORD));

        when(RAW_REPO_RECORD_SERVICE_CONNECTOR.getRecordData(any(RecordIdDTO.class)))
                .thenReturn(FIRST_RECORD_WITHOUT_ENRICHMENT_TRAIL)
                .thenReturn(SECOND_RECORD)
                .thenReturn(THIRD_RECORD_WITHOUT_ENRICHMENT_TRAIL);

        // Setup harvester datafile content expectations
        dbcRecordsExpectations.add(Expectations.of(FIRST_RECORD_HEAD, FIRST_RECORD_SECTION, FIRST_RECORD));
        dbcRecordsAddiMetaDataExpectations.add(new AddiMetaData()
                .withBibliographicRecordId(FIRST_RECORD.getRecordId().getBibliographicRecordId())
                .withSubmitterNumber(870970)
                .withFormat("katalog")
                .withCreationDate(Date.from(Instant.parse(FIRST_RECORD.getCreated())))
                .withEnrichmentTrail(FIRST_RECORD.getEnrichmentTrail())
                .withTrackingId(FIRST_RECORD.getTrackingId())
                .withDeleted(false)
                .withLibraryRules(new AddiMetaData.LibraryRules()));

        dbcRecordsExpectations.add(Expectations.of(THIRD_RECORD));
        dbcRecordsAddiMetaDataExpectations.add(new AddiMetaData()
                .withBibliographicRecordId(THIRD_RECORD.getRecordId().getBibliographicRecordId())
                .withSubmitterNumber(870970)
                .withFormat("katalog")
                .withCreationDate(Date.from(Instant.parse(THIRD_RECORD.getCreated())))
                .withEnrichmentTrail(THIRD_RECORD.getEnrichmentTrail())
                .withDeleted(false)
                .withLibraryRules(new AddiMetaData.LibraryRules()));

        localRecordsExpectations.add(Expectations.of(SECOND_RECORD));
        localRecordsAddiMetaDataExpectations.add(new AddiMetaData()
                .withBibliographicRecordId(SECOND_RECORD.getRecordId().getBibliographicRecordId())
                .withSubmitterNumber(SECOND_RECORD.getRecordId().getAgencyId())
                .withFormat("katalog")
                .withCreationDate(Date.from(Instant.parse(SECOND_RECORD.getCreated())))
                .withDeleted(false)
                .withLibraryRules(localLibraryRules));

        HarvestOperation harvestOperation = newHarvestOperation();
        harvestOperation.execute();

        verifyHarvesterDataFiles();
        verifyJobSpecifications();
    }

    @Test
    public void harvest_recordCollectionContainsInvalidEntry_recordIsFailed()
            throws HarvesterException, RecordServiceConnectorException {
        RecordEntryDTO invalidRecord = new RecordEntryDTO();
        invalidRecord.setRecordId(FIRST_RECORD_HEAD.getRecordId());
        invalidRecord.setCreated(Instant.now().toString());
        invalidRecord.setContent(JsonNodeFactory.instance.textNode("garbage"));

        // Mock rawrepo return values
        when(RAW_REPO_RECORD_SERVICE_CONNECTOR.getRecordDataCollectionDataIO(any(RecordIdDTO.class), any(RecordServiceConnector.Params.class)))
                .thenReturn(List.of(FIRST_RECORD, invalidRecord))
                .thenReturn(List.of(SECOND_RECORD))
                .thenReturn(List.of(THIRD_RECORD));
        when(RAW_REPO_RECORD_SERVICE_CONNECTOR.getRecordData(any(RecordIdDTO.class))).thenReturn(FIRST_RECORD).thenReturn(SECOND_RECORD).thenReturn(THIRD_RECORD);

        // Setup harvester datafile content expectations
        dbcRecordsExpectations.add(null);
        dbcRecordsAddiMetaDataExpectations.add(new AddiMetaData()
                .withBibliographicRecordId(FIRST_RECORD.getRecordId().getBibliographicRecordId())
                .withSubmitterNumber(870970)
                .withFormat("katalog")
                .withCreationDate(Date.from(Instant.parse(FIRST_RECORD.getCreated())))
                .withEnrichmentTrail(FIRST_RECORD.getEnrichmentTrail())
                .withTrackingId(FIRST_RECORD.getTrackingId())
                .withDiagnostic(new Diagnostic(Diagnostic.Level.FATAL, "No marcXchange record found"))
                .withDeleted(false)
                .withLibraryRules(new AddiMetaData.LibraryRules()));

        localRecordsExpectations.add(Expectations.of(SECOND_RECORD));
        localRecordsAddiMetaDataExpectations.add(new AddiMetaData()
                .withBibliographicRecordId(SECOND_RECORD.getRecordId().getBibliographicRecordId())
                .withSubmitterNumber(SECOND_RECORD.getRecordId().getAgencyId())
                .withFormat("katalog")
                .withCreationDate(Date.from(Instant.parse(SECOND_RECORD.getCreated())))
                .withDeleted(false)
                .withLibraryRules(localLibraryRules));

        dbcRecordsExpectations.add(Expectations.of(THIRD_RECORD));
        dbcRecordsAddiMetaDataExpectations.add(new AddiMetaData()
                .withBibliographicRecordId(THIRD_RECORD.getRecordId().getBibliographicRecordId())
                .withSubmitterNumber(870970)
                .withFormat("katalog")
                .withCreationDate(Date.from(Instant.parse(THIRD_RECORD.getCreated())))
                .withEnrichmentTrail(THIRD_RECORD.getEnrichmentTrail())
                .withDeleted(false)
                .withLibraryRules(new AddiMetaData.LibraryRules()));

        HarvestOperation harvestOperation = newHarvestOperation();
        harvestOperation.execute();

        verifyHarvesterDataFiles();
        verifyJobSpecifications();
    }

    private HarvestOperation newHarvestOperation() {
        HarvesterJobBuilderFactory harvesterJobBuilderFactory;
        try {
            harvesterJobBuilderFactory = new HarvesterJobBuilderFactory(
                    new BinaryFileStoreFsImpl(createDir(tmpFolder)),
                    mockedFileStoreServiceConnector,
                    mockedJobStoreServiceConnector);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        RRHarvesterConfig config = HarvesterTestUtil.getRRHarvesterConfig();
        config.getContent()
                .withConsumerId(CONSUMER_ID)
                .withFormat("katalog")
                .withFormatOverridesEntry(HarvestOperation.DBC_LIBRARY, "basis")
                .withIncludeRelations(true)
                .withIncludeLibraryRules(true);
        try {
            return new HarvestOperation(config, harvesterJobBuilderFactory, taskRepo, VIP_CORE_CONNECTION, RAW_REPO_CONNECTOR, RAW_REPO_RECORD_SERVICE_CONNECTOR, metricRegistry);
        } catch (QueueException | SQLException | ConfigurationException e) {
            throw new IllegalStateException(e);
        }
    }

    private void verifyHarvesterDataFiles() {
        AddiFileVerifier addiFileVerifier = new AddiFileVerifier();
        addiFileVerifier.verify(harvesterDataFileWithDbcRecords, dbcRecordsAddiMetaDataExpectations, dbcRecordsExpectations);
        addiFileVerifier.verify(harvesterDataFileWithLocalRecords, localRecordsAddiMetaDataExpectations, localRecordsExpectations);
    }

    private void verifyJobSpecifications() {
        verifyJobSpecification(mockedJobStoreServiceConnector.jobInputStreams.remove().getJobSpecification(),
                newHarvestOperation().getJobSpecificationTemplate(870970));
        verifyJobSpecification(mockedJobStoreServiceConnector.jobInputStreams.remove().getJobSpecification(),
                newHarvestOperation().getJobSpecificationTemplate(LOCAL_LIBRARY));
    }

    private void verifyJobSpecification(JobSpecification jobSpecification, JobSpecification jobSpecificationTemplate) {
        assertThat(jobSpecification.getPackaging(), is(jobSpecificationTemplate.getPackaging()));
        assertThat(jobSpecification.getFormat(), is(jobSpecificationTemplate.getFormat()));
        assertThat(jobSpecification.getCharset(), is(jobSpecificationTemplate.getCharset()));
        assertThat(jobSpecification.getDestination(), is(jobSpecificationTemplate.getDestination()));
        assertThat(jobSpecification.getSubmitterId(), is(jobSpecificationTemplate.getSubmitterId()));
    }
}
