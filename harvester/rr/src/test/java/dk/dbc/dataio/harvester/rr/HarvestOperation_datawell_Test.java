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
import dk.dbc.rawrepo.queue.RawRepoQueueDAO;
import dk.dbc.rawrepo.record.RecordServiceConnector;
import dk.dbc.rawrepo.record.RecordServiceConnectorException;
import dk.dbc.vipcore.exception.VipCoreException;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.SimpleTimer;
import org.eclipse.microprofile.metrics.Tag;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import javax.persistence.EntityManager;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HarvestOperation_datawell_Test {
    private static final Date QUEUED_TIME = new Date(1467277697583L); // 2016-06-30 11:08:17.583
    private static final String CONSUMER_ID = "consumerId";
    private static final int LOCAL_LIBRARY = 700000;

    private final static RawRepoConnector RAW_REPO_CONNECTOR = mock(RawRepoConnector.class);
    private final static VipCoreConnection VIP_CORE_CONNECTION = mock(VipCoreConnection.class);
    private final static RecordServiceConnector RAW_REPO_RECORD_SERVICE_CONNECTOR = mock(RecordServiceConnector.class);

    private final static HashMap<String, String> QUEUE_DAO_CONFIGURATION = new HashMap<String, String> ();
    private final static RawRepoQueueDAO QUEUE_DAO = mock(RawRepoQueueDAO.class);

    /* 1st record is a DBC record */
    private final static RecordIdDTO FIRST_RECORD_ID = new RecordIdDTO("first", HarvestOperation.DBC_LIBRARY);
    private final static String FIRST_RECORD_CONTENT = HarvestOperationTest.getRecordContent(FIRST_RECORD_ID);
    private final static RecordDTO FIRST_RECORD = new RecordDTO();
    private final static RecordDTO FIRST_RECORD_WITHOUT_ENRICHMENT_TRAIL = new RecordDTO();
    private final static QueueItem FIRST_QUEUE_ITEM = HarvestOperationTest.getQueueItem(FIRST_RECORD_ID, QUEUED_TIME);

    private final static RecordIdDTO FIRST_RECORD_HEAD_ID = new RecordIdDTO("first-head", HarvestOperation.DBC_LIBRARY);
    private final static String FIRST_RECORD_HEAD_CONTENT = HarvestOperationTest.getRecordContent(FIRST_RECORD_HEAD_ID);
    private final static RecordDTO FIRST_RECORD_HEAD = new RecordDTO();

    private final static RecordIdDTO FIRST_RECORD_SECTION_ID = new RecordIdDTO("first-section", HarvestOperation.DBC_LIBRARY);
    private final static String FIRST_RECORD_SECTION_CONTENT = HarvestOperationTest.getRecordContent(FIRST_RECORD_SECTION_ID);
    private final static RecordDTO FIRST_RECORD_SECTION = new RecordDTO();

    /* 2nd record is a local record */
    private final static RecordIdDTO SECOND_RECORD_ID = new RecordIdDTO("second", LOCAL_LIBRARY);
    private final static String SECOND_RECORD_CONTENT = HarvestOperationTest.getRecordContent(SECOND_RECORD_ID);
    private final static RecordDTO SECOND_RECORD = new RecordDTO();
    private final static QueueItem SECOND_QUEUE_ITEM = HarvestOperationTest.getQueueItem(SECOND_RECORD_ID, QUEUED_TIME);

    /* 3rd record is a DBC record */
    private final static RecordIdDTO THIRD_RECORD_ID = new RecordIdDTO("third", HarvestOperation.DBC_LIBRARY);
    private final static String THIRD_RECORD_CONTENT = HarvestOperationTest.getRecordContent(THIRD_RECORD_ID);
    private final static RecordDTO THIRD_RECORD = new RecordDTO();
    private final static RecordDTO THIRD_RECORD_WITHOUT_ENRICHMENT_TRAIL = new RecordDTO();
    private final static QueueItem THIRD_QUEUE_ITEM = HarvestOperationTest.getQueueItem(THIRD_RECORD_ID, QUEUED_TIME);

    static {
        FIRST_RECORD_HEAD.setRecordId(FIRST_RECORD_HEAD_ID);
        FIRST_RECORD_HEAD.setCreated(Instant.now().toString());
        FIRST_RECORD_HEAD.setContent(FIRST_RECORD_HEAD_CONTENT.getBytes(StandardCharsets.UTF_8));

        FIRST_RECORD_SECTION.setRecordId(FIRST_RECORD_SECTION_ID);
        FIRST_RECORD_SECTION.setCreated(Instant.now().toString());
        FIRST_RECORD_SECTION.setContent(FIRST_RECORD_SECTION_CONTENT.getBytes(StandardCharsets.UTF_8));

        FIRST_RECORD.setRecordId(FIRST_RECORD_ID);
        FIRST_RECORD.setCreated(Instant.now().toString());
        FIRST_RECORD.setContent(FIRST_RECORD_CONTENT.getBytes(StandardCharsets.UTF_8));
        FIRST_RECORD.setEnrichmentTrail("191919,870970");
        FIRST_RECORD.setTrackingId("tracking id");

        FIRST_RECORD_WITHOUT_ENRICHMENT_TRAIL.setRecordId(FIRST_RECORD_ID);
        FIRST_RECORD_WITHOUT_ENRICHMENT_TRAIL.setCreated(FIRST_RECORD.getCreated());
        FIRST_RECORD_WITHOUT_ENRICHMENT_TRAIL.setTrackingId("tracking id");

        SECOND_RECORD.setRecordId(SECOND_RECORD_ID);
        SECOND_RECORD.setCreated(Instant.now().toString());
        SECOND_RECORD.setContent(SECOND_RECORD_CONTENT.getBytes(StandardCharsets.UTF_8));

        THIRD_RECORD.setRecordId(THIRD_RECORD_ID);
        THIRD_RECORD.setCreated(Instant.now().toString());
        THIRD_RECORD.setContent(THIRD_RECORD_CONTENT.getBytes(StandardCharsets.UTF_8));
        THIRD_RECORD.setEnrichmentTrail("191919,870970");

        THIRD_RECORD_WITHOUT_ENRICHMENT_TRAIL.setRecordId(THIRD_RECORD_ID);
        THIRD_RECORD_WITHOUT_ENRICHMENT_TRAIL.setCreated(THIRD_RECORD.getCreated());

        QUEUE_DAO_CONFIGURATION.put ("RAWREPO_RECORD_URL", "http://localhost:4221");
    }

    private final EntityManager entityManager = mock(EntityManager.class);
    private final TaskRepo taskRepo = new TaskRepo(entityManager);

    private MockedJobStoreServiceConnector mockedJobStoreServiceConnector;
    private MockedFileStoreServiceConnector mockedFileStoreServiceConnector;
    private File harvesterDataFileWithDbcRecords;
    private File harvesterDataFileWithLocalRecords;
    private List<AddiMetaData> dbcRecordsAddiMetaDataExpectations;
    private List<AddiMetaData> localRecordsAddiMetaDataExpectations;
    private List<XmlExpectation> dbcRecordsExpectations;
    private List<XmlExpectation> localRecordsExpectations;

    private AddiMetaData.LibraryRules localLibraryRules = new AddiMetaData.LibraryRules()
            .withLibraryRule("rule1", true);

    public static final MetricRegistry metricRegistry = mock(MetricRegistry.class);
    private final Counter counter = mock(Counter.class);
    private final SimpleTimer timer = mock(SimpleTimer.class);

    @Rule
    public TemporaryFolder tmpFolder = new TemporaryFolder();

    @Before
    public void setupMocks() throws SQLException, IOException, ConfigurationException, QueueException, VipCoreException {
        // Mock rawrepo return values
        when(RAW_REPO_CONNECTOR.dequeue(CONSUMER_ID))
                .thenReturn(FIRST_QUEUE_ITEM)
                .thenReturn(SECOND_QUEUE_ITEM)
                .thenReturn(THIRD_QUEUE_ITEM)
                .thenReturn(null);

        // Intercept harvester data files with mocked FileStoreServiceConnectorBean
        harvesterDataFileWithDbcRecords = tmpFolder.newFile();
        harvesterDataFileWithLocalRecords = tmpFolder.newFile();
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

        when(QUEUE_DAO.getConfiguration ())
                .thenReturn (QUEUE_DAO_CONFIGURATION);

        when(metricRegistry.counter(any(Metadata.class), any(Tag.class))).thenReturn(counter);
        when(metricRegistry.simpleTimer(any(Metadata.class), any(Tag.class))).thenReturn(timer);
        doNothing().when(counter).inc();
        doNothing().when(timer).update(any(Duration.class));
    }

    @Test
    public void harvest_multipleAgencyIdsHarvested_agencyIdsInSeparateJobs()
            throws HarvesterException, RecordServiceConnectorException {
        // Mock rawrepo return values
        when(RAW_REPO_RECORD_SERVICE_CONNECTOR.getRecordDataCollectionDataIO(any(RecordIdDTO.class), any(RecordServiceConnector.Params.class)))
                .thenReturn(new HashMap<String, RecordDTO>() {{
                    put(FIRST_RECORD_HEAD_ID.getBibliographicRecordId(), FIRST_RECORD_HEAD);
                    put(FIRST_RECORD_SECTION_ID.getBibliographicRecordId(), FIRST_RECORD_SECTION);
                    put(FIRST_RECORD_ID.getBibliographicRecordId(), FIRST_RECORD);
                }})
                .thenReturn(new HashMap<String, RecordDTO>(){{
                    put(SECOND_RECORD_ID.getBibliographicRecordId(), SECOND_RECORD);
                }})
                .thenReturn(new HashMap<String, RecordDTO>(){{
                    put(THIRD_RECORD_ID.getBibliographicRecordId(), THIRD_RECORD);
                }});

        when(RAW_REPO_RECORD_SERVICE_CONNECTOR.recordFetch(any(RecordIdDTO.class)))
                .thenReturn(FIRST_RECORD_WITHOUT_ENRICHMENT_TRAIL)
                .thenReturn(SECOND_RECORD)
                .thenReturn(THIRD_RECORD_WITHOUT_ENRICHMENT_TRAIL);

        // Setup harvester datafile content expectations
        final MarcExchangeCollectionExpectation marcExchangeCollectionExpectation1 = new MarcExchangeCollectionExpectation();
        marcExchangeCollectionExpectation1.records.add(getMarcExchangeRecord(FIRST_RECORD_HEAD_ID));
        marcExchangeCollectionExpectation1.records.add(getMarcExchangeRecord(FIRST_RECORD_SECTION_ID));
        marcExchangeCollectionExpectation1.records.add(getMarcExchangeRecord(FIRST_RECORD_ID));
        dbcRecordsExpectations.add(marcExchangeCollectionExpectation1);
        dbcRecordsAddiMetaDataExpectations.add(new AddiMetaData()
                .withBibliographicRecordId(FIRST_RECORD.getRecordId().getBibliographicRecordId())
                .withSubmitterNumber(870970)
                .withFormat("katalog")
                .withCreationDate(Date.from(Instant.parse(FIRST_RECORD.getCreated())))
                .withEnrichmentTrail(FIRST_RECORD.getEnrichmentTrail())
                .withTrackingId(FIRST_RECORD.getTrackingId())
                .withDeleted(false)
                .withLibraryRules(new AddiMetaData.LibraryRules()));

        final MarcExchangeCollectionExpectation marcExchangeCollectionExpectation2 = new MarcExchangeCollectionExpectation();
        marcExchangeCollectionExpectation2.records.add(getMarcExchangeRecord(THIRD_RECORD_ID));
        dbcRecordsExpectations.add(marcExchangeCollectionExpectation2);
        dbcRecordsAddiMetaDataExpectations.add(new AddiMetaData()
                .withBibliographicRecordId(THIRD_RECORD.getRecordId().getBibliographicRecordId())
                .withSubmitterNumber(870970)
                .withFormat("katalog")
                .withCreationDate(Date.from(Instant.parse(THIRD_RECORD.getCreated())))
                .withEnrichmentTrail(THIRD_RECORD.getEnrichmentTrail())
                .withDeleted(false)
                .withLibraryRules(new AddiMetaData.LibraryRules()));

        final MarcExchangeCollectionExpectation marcExchangeCollectionExpectation3 = new MarcExchangeCollectionExpectation();
        marcExchangeCollectionExpectation3.records.add(getMarcExchangeRecord(SECOND_RECORD_ID));
        localRecordsExpectations.add(marcExchangeCollectionExpectation3);
        localRecordsAddiMetaDataExpectations.add(new AddiMetaData()
                .withBibliographicRecordId(SECOND_RECORD.getRecordId().getBibliographicRecordId())
                .withSubmitterNumber(SECOND_RECORD.getRecordId().getAgencyId())
                .withFormat("katalog")
                .withCreationDate(Date.from(Instant.parse(SECOND_RECORD.getCreated())))
                .withDeleted(false)
                .withLibraryRules(localLibraryRules));

        final HarvestOperation harvestOperation = newHarvestOperation();
        harvestOperation.execute();

        verifyHarvesterDataFiles();
        verifyJobSpecifications();
    }

    @Test
    public void harvest_recordCollectionContainsInvalidEntry_recordIsFailed()
            throws HarvesterException, RecordServiceConnectorException {
        final RecordDTO invalidRecord = new RecordDTO();
        invalidRecord.setRecordId(FIRST_RECORD_HEAD_ID);
        invalidRecord.setCreated(Instant.now().toString());
        invalidRecord.setContent("not xml".getBytes(StandardCharsets.UTF_8));

        // Mock rawrepo return values
        when(RAW_REPO_RECORD_SERVICE_CONNECTOR.getRecordDataCollectionDataIO(any(RecordIdDTO.class), any(RecordServiceConnector.Params.class)))
                .thenReturn(new HashMap<String, RecordDTO>() {{
                    put(FIRST_RECORD_ID.getBibliographicRecordId(), FIRST_RECORD);
                    put("INVALID_RECORD_ID", invalidRecord);
                }})
                .thenReturn(new HashMap<String, RecordDTO>(){{
                    put(SECOND_RECORD_ID.getBibliographicRecordId(), SECOND_RECORD);
                }})
                .thenReturn(new HashMap<String, RecordDTO>(){{
                    put(THIRD_RECORD_ID.getBibliographicRecordId(), THIRD_RECORD);
                }});
        when(RAW_REPO_RECORD_SERVICE_CONNECTOR.recordFetch(any(RecordIdDTO.class))).thenReturn(FIRST_RECORD).thenReturn(SECOND_RECORD).thenReturn(THIRD_RECORD);

        // Setup harvester datafile content expectations
        dbcRecordsExpectations.add(null);
        dbcRecordsAddiMetaDataExpectations.add(new AddiMetaData()
                .withBibliographicRecordId(FIRST_RECORD.getRecordId().getBibliographicRecordId())
                .withSubmitterNumber(870970)
                .withFormat("katalog")
                .withCreationDate(Date.from(Instant.parse(FIRST_RECORD.getCreated())))
                .withEnrichmentTrail(FIRST_RECORD.getEnrichmentTrail())
                .withTrackingId(FIRST_RECORD.getTrackingId())
                .withDiagnostic(new Diagnostic(Diagnostic.Level.FATAL, String.format(
                        "Harvesting RawRepo %s failed: member data can not be parsed as marcXchange", FIRST_RECORD.getRecordId())))
                .withDeleted(false)
                .withLibraryRules(new AddiMetaData.LibraryRules()));

        final MarcExchangeCollectionExpectation marcExchangeCollectionExpectation1 = new MarcExchangeCollectionExpectation();
        marcExchangeCollectionExpectation1.records.add(getMarcExchangeRecord(SECOND_RECORD_ID));
        localRecordsExpectations.add(marcExchangeCollectionExpectation1);
        localRecordsAddiMetaDataExpectations.add(new AddiMetaData()
                .withBibliographicRecordId(SECOND_RECORD.getRecordId().getBibliographicRecordId())
                .withSubmitterNumber(SECOND_RECORD.getRecordId().getAgencyId())
                .withFormat("katalog")
                .withCreationDate(Date.from(Instant.parse(SECOND_RECORD.getCreated())))
                .withDeleted(false)
                .withLibraryRules(localLibraryRules));

        final MarcExchangeCollectionExpectation marcExchangeCollectionExpectation2 = new MarcExchangeCollectionExpectation();
        marcExchangeCollectionExpectation2.records.add(getMarcExchangeRecord(THIRD_RECORD_ID));
        dbcRecordsExpectations.add(marcExchangeCollectionExpectation2);
        dbcRecordsAddiMetaDataExpectations.add(new AddiMetaData()
                .withBibliographicRecordId(THIRD_RECORD.getRecordId().getBibliographicRecordId())
                .withSubmitterNumber(870970)
                .withFormat("katalog")
                .withCreationDate(Date.from(Instant.parse(THIRD_RECORD.getCreated())))
                .withEnrichmentTrail(THIRD_RECORD.getEnrichmentTrail())
                .withDeleted(false)
                .withLibraryRules(new AddiMetaData.LibraryRules()));

        final HarvestOperation harvestOperation = newHarvestOperation();
        harvestOperation.execute();

        verifyHarvesterDataFiles();
        verifyJobSpecifications();
    }

    private HarvestOperation newHarvestOperation() {
        final HarvesterJobBuilderFactory harvesterJobBuilderFactory;
        try {
            harvesterJobBuilderFactory = new HarvesterJobBuilderFactory(
                    new BinaryFileStoreFsImpl(tmpFolder.newFolder().toPath()),
                    mockedFileStoreServiceConnector,
                    mockedJobStoreServiceConnector);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        final RRHarvesterConfig config = HarvesterTestUtil.getRRHarvesterConfig();
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
        final AddiFileVerifier addiFileVerifier = new AddiFileVerifier();
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

    private MarcExchangeRecordExpectation getMarcExchangeRecord(RecordIdDTO recordId) {
        return new MarcExchangeRecordExpectation(recordId.getBibliographicRecordId(), recordId.getAgencyId());
    }
}
