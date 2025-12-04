package dk.dbc.dataio.harvester.rr;

import dk.dbc.dataio.bfs.api.BinaryFileStoreFsImpl;
import dk.dbc.dataio.commons.types.AddiMetaData;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.utils.jobstore.MockedJobStoreServiceConnector;
import dk.dbc.dataio.filestore.service.connector.MockedFileStoreServiceConnector;
import dk.dbc.dataio.harvester.task.TaskRepo;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.RRHarvesterConfig;
import dk.dbc.dataio.harvester.utils.datafileverifier.AddiFileVerifier;
import dk.dbc.dataio.harvester.utils.datafileverifier.MarcXchangeCollectionExpectation;
import dk.dbc.dataio.harvester.utils.datafileverifier.MarcXchangeRecordExpectation;
import dk.dbc.dataio.harvester.utils.datafileverifier.XmlExpectation;
import dk.dbc.dataio.harvester.utils.holdingsitems.HoldingsItemsConnector;
import dk.dbc.dataio.harvester.utils.rawrepo.RawRepoConnector;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.rawrepo.dto.RecordDTO;
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
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HarvestOperation_ims_Test implements TempFiles {
    public static final MetricRegistry metricRegistry = mock(MetricRegistry.class);
    private static final Date QUEUED_TIME = new Date(1467277697583L); // 2016-06-30 11:08:17.583
    private static final String CONSUMER_ID = "consumerId";
    private static final int IMS_LIBRARY = 775100;
    final RecordServiceConnector rawRepoRecordServiceConnector = mock(RecordServiceConnector.class);
    private final EntityManager entityManager = mock(EntityManager.class);
    private final TaskRepo taskRepo = new TaskRepo(entityManager);
    private final RawRepoConnector rawRepoConnector = mock(RawRepoConnector.class);
    private final VipCoreConnection vipCoreConnection = mock(VipCoreConnection.class);
    private final HoldingsItemsConnector holdingsItemsConnector = mock(HoldingsItemsConnector.class);
    private final AddiFileVerifier addiFileVerifier = new AddiFileVerifier();
    private final Timer timer = mock(Timer.class);
    private final Counter counter = mock(Counter.class);
    @TempDir
    public Path tmpFolder;
    private MockedJobStoreServiceConnector mockedJobStoreServiceConnector;
    private MockedFileStoreServiceConnector mockedFileStoreServiceConnector;
    private File harvesterDataFileWith710100;
    private File harvesterDataFileWith737000;
    private File harvesterDataFileWith775100;
    private List<AddiMetaData> addiMetaDataExpectationsFor710100;
    private List<AddiMetaData> addiMetaDataExpectationsFor737000;
    private List<AddiMetaData> addiMetaDataExpectationsFor775100;
    private List<XmlExpectation> recordsExpectationsFor710100;
    private List<XmlExpectation> recordsExpectationsFor737000;
    private List<XmlExpectation> recordsExpectationsFor775100;

    @BeforeEach
    public void setupMocks() throws IOException, HarvesterException {
        // Mock agency-connection and holdings-items lookup
        Set<Integer> imsLibraries = Stream.of(710100, 737000, 775100, 785100).collect(Collectors.toSet());
        Set<Integer> hasHoldingsResponse = new LinkedHashSet<>();
        hasHoldingsResponse.add(710100);
        hasHoldingsResponse.add(737000);
        hasHoldingsResponse.add(123456);
        when(holdingsItemsConnector.hasHoldings("dbc", imsLibraries)).thenReturn(hasHoldingsResponse);
        when(vipCoreConnection.getFbsImsLibraries()).thenReturn(imsLibraries);
        when(metricRegistry.timer(any(Metadata.class), any(Tag.class))).thenReturn(timer);
        when(metricRegistry.counter(any(Metadata.class), any(Tag.class))).thenReturn(counter);
        doNothing().when(timer).update(any(Duration.class));
        doNothing().when(counter).inc();

        // Intercept harvester data files with mocked FileStoreServiceConnectorBean
        harvesterDataFileWith710100 = createFile(tmpFolder);
        harvesterDataFileWith737000 = createFile(tmpFolder);
        harvesterDataFileWith775100 = createFile(tmpFolder);
        mockedFileStoreServiceConnector = new MockedFileStoreServiceConnector();
        mockedFileStoreServiceConnector.destinations.add(harvesterDataFileWith710100.toPath());
        mockedFileStoreServiceConnector.destinations.add(harvesterDataFileWith737000.toPath());
        mockedFileStoreServiceConnector.destinations.add(harvesterDataFileWith775100.toPath());

        // Intercept harvester job specifications with mocked JobStoreServiceConnectorBean
        mockedJobStoreServiceConnector = new MockedJobStoreServiceConnector();
        mockedJobStoreServiceConnector.jobInfoSnapshots.add(new JobInfoSnapshot());
        mockedJobStoreServiceConnector.jobInfoSnapshots.add(new JobInfoSnapshot());
        mockedJobStoreServiceConnector.jobInfoSnapshots.add(new JobInfoSnapshot());

        addiMetaDataExpectationsFor710100 = new ArrayList<>();
        addiMetaDataExpectationsFor737000 = new ArrayList<>();
        addiMetaDataExpectationsFor775100 = new ArrayList<>();
        recordsExpectationsFor710100 = new ArrayList<>();
        recordsExpectationsFor737000 = new ArrayList<>();
        recordsExpectationsFor775100 = new ArrayList<>();
    }

    @Test
    public void harvest_multipleAgencyIdsHarvested_agencyIdsInSeparateJobs() throws SQLException, RecordServiceConnectorException, HarvesterException, QueueException {

        RecordIdDTO dbcRecordId = new RecordIdDTO("dbc", HarvestOperation.DBC_LIBRARY);
        RecordDTO dbcRecord = new RecordDTO();
        dbcRecord.setRecordId(dbcRecordId);
        dbcRecord.setCreated(Instant.now().toString());
        dbcRecord.setContent(HarvestOperationTest.getRecordContent(dbcRecordId).getBytes(StandardCharsets.UTF_8));
        dbcRecord.setEnrichmentTrail("191919,870970");
        dbcRecord.setTrackingId("tracking id");

        RecordIdDTO dbcHeadRecordId = new RecordIdDTO("dbc-head", HarvestOperation.DBC_LIBRARY);
        RecordDTO dbcHeadRecord = new RecordDTO();
        dbcHeadRecord.setRecordId(dbcHeadRecordId);
        dbcHeadRecord.setCreated(Instant.now().toString());
        dbcHeadRecord.setContent(HarvestOperationTest.getRecordContent(dbcHeadRecordId).getBytes(StandardCharsets.UTF_8));

        RecordIdDTO dbcSectionRecordId = new RecordIdDTO("dbc-section", HarvestOperation.DBC_LIBRARY);
        RecordDTO dbcSectionRecord = new RecordDTO();
        dbcSectionRecord.setRecordId(dbcSectionRecordId);
        dbcSectionRecord.setCreated(Instant.now().toString());
        dbcSectionRecord.setContent(HarvestOperationTest.getRecordContent(dbcSectionRecordId).getBytes(StandardCharsets.UTF_8));

        RecordIdDTO imsRecordId = new RecordIdDTO("ims", IMS_LIBRARY);
        RecordDTO imsRecord = new RecordDTO();
        imsRecord.setRecordId(imsRecordId);
        imsRecord.setCreated(Instant.now().toString());
        imsRecord.setContent(HarvestOperationTest.getRecordContent(imsRecordId).getBytes(StandardCharsets.UTF_8));

        // Mock rawrepo return values
        when(rawRepoConnector.dequeue(CONSUMER_ID)).thenReturn(HarvestOperationTest.getQueueItem(dbcRecordId, QUEUED_TIME)).thenReturn(HarvestOperationTest.getQueueItem(imsRecordId, QUEUED_TIME)).thenReturn(null);

        when(rawRepoRecordServiceConnector.getRecordDataCollectionDataIO(any(RecordIdDTO.class), any(RecordServiceConnector.Params.class))).thenReturn(new HashMap<>() {{
            put(dbcHeadRecordId.getBibliographicRecordId(), dbcHeadRecord);
            put(dbcSectionRecordId.getBibliographicRecordId(), dbcSectionRecord);
            put(dbcRecordId.getBibliographicRecordId(), dbcRecord);
        }}).thenReturn(new HashMap<>() {{
            put(dbcHeadRecordId.getBibliographicRecordId(), dbcHeadRecord);
            put(dbcSectionRecordId.getBibliographicRecordId(), dbcSectionRecord);
            put(dbcRecordId.getBibliographicRecordId(), dbcRecord);
        }}).thenReturn(new HashMap<>() {{
            put(imsRecordId.getBibliographicRecordId(), imsRecord);
        }});

        when(rawRepoRecordServiceConnector.recordFetch(any(RecordIdDTO.class))).thenReturn(dbcRecord).thenReturn(dbcRecord).thenReturn(dbcRecord).thenReturn(dbcRecord).thenReturn(imsRecord).thenReturn(imsRecord);

        // Setup harvester datafile content expectations
        MarcXchangeCollectionExpectation marcXchangeCollectionExpectation710100 = new MarcXchangeCollectionExpectation();
        marcXchangeCollectionExpectation710100.records.add(getMarcXchangeRecord(dbcHeadRecordId));
        marcXchangeCollectionExpectation710100.records.add(getMarcXchangeRecord(dbcSectionRecordId));
        marcXchangeCollectionExpectation710100.records.add(getMarcXchangeRecord(dbcRecordId));
        recordsExpectationsFor710100.add(marcXchangeCollectionExpectation710100);
        addiMetaDataExpectationsFor710100.add(new AddiMetaData().withBibliographicRecordId(dbcRecord.getRecordId().getBibliographicRecordId()).withSubmitterNumber(710100).withFormat("katalog").withCreationDate(Date.from(Instant.parse(dbcRecord.getCreated()))).withEnrichmentTrail(dbcRecord.getEnrichmentTrail()).withTrackingId(dbcRecord.getTrackingId()).withDeleted(false).withLibraryRules(new AddiMetaData.LibraryRules()));

        MarcXchangeCollectionExpectation marcXchangeCollectionExpectation737000 = new MarcXchangeCollectionExpectation();
        marcXchangeCollectionExpectation737000.records.add(getMarcXchangeRecord(dbcHeadRecordId));
        marcXchangeCollectionExpectation737000.records.add(getMarcXchangeRecord(dbcSectionRecordId));
        marcXchangeCollectionExpectation737000.records.add(getMarcXchangeRecord(dbcRecordId));
        recordsExpectationsFor737000.add(marcXchangeCollectionExpectation737000);
        addiMetaDataExpectationsFor737000.add(new AddiMetaData().withBibliographicRecordId(dbcRecord.getRecordId().getBibliographicRecordId()).withSubmitterNumber(737000).withFormat("katalog").withCreationDate(Date.from(Instant.parse(dbcRecord.getCreated()))).withEnrichmentTrail(dbcRecord.getEnrichmentTrail()).withTrackingId(dbcRecord.getTrackingId()).withDeleted(false).withLibraryRules(new AddiMetaData.LibraryRules()));

        MarcXchangeCollectionExpectation marcXchangeCollectionExpectation775100 = new MarcXchangeCollectionExpectation();
        marcXchangeCollectionExpectation775100.records.add(getMarcXchangeRecord(imsRecordId));
        recordsExpectationsFor775100.add(marcXchangeCollectionExpectation775100);
        addiMetaDataExpectationsFor775100.add(new AddiMetaData().withBibliographicRecordId(imsRecord.getRecordId().getBibliographicRecordId()).withSubmitterNumber(775100).withFormat("katalog").withCreationDate(Date.from(Instant.parse(imsRecord.getCreated()))).withEnrichmentTrail(imsRecord.getEnrichmentTrail()).withTrackingId(imsRecord.getTrackingId()).withDeleted(false).withLibraryRules(new AddiMetaData.LibraryRules()));

        ImsHarvestOperation harvestOperation = newImsHarvestOperation();
        harvestOperation.execute();

        addiFileVerifier.verify(harvesterDataFileWith710100, addiMetaDataExpectationsFor710100, recordsExpectationsFor710100);
        addiFileVerifier.verify(harvesterDataFileWith737000, addiMetaDataExpectationsFor737000, recordsExpectationsFor737000);
        addiFileVerifier.verify(harvesterDataFileWith775100, addiMetaDataExpectationsFor775100, recordsExpectationsFor775100);
        verifyJobSpecification(mockedJobStoreServiceConnector.jobInputStreams.remove().getJobSpecification(), newImsHarvestOperation().getJobSpecificationTemplate(710100));
        verifyJobSpecification(mockedJobStoreServiceConnector.jobInputStreams.remove().getJobSpecification(), newImsHarvestOperation().getJobSpecificationTemplate(737000));
        verifyJobSpecification(mockedJobStoreServiceConnector.jobInputStreams.remove().getJobSpecification(), newImsHarvestOperation().getJobSpecificationTemplate(775100));
    }

    @Test
    public void imsRecordIsDeleted_870970RecordUsedInstead() throws SQLException, RecordServiceConnectorException, HarvesterException, QueueException {
        RecordIdDTO imsRecordId = new RecordIdDTO("faust", IMS_LIBRARY);
        RecordDTO imsRecord = new RecordDTO();
        imsRecord.setRecordId(imsRecordId);
        imsRecord.setCreated(Instant.now().toString());
        imsRecord.setContent(HarvestOperationTest.getRecordContent(imsRecordId).getBytes(StandardCharsets.UTF_8));
        imsRecord.setDeleted(true);

        RecordIdDTO recordId191919 = new RecordIdDTO("faust", 191919);
        RecordIdDTO dbcRecordId = new RecordIdDTO("faust", 870970);
        RecordDTO dbcRecord = new RecordDTO();
        dbcRecord.setRecordId(dbcRecordId);
        dbcRecord.setCreated(Instant.now().toString());
        dbcRecord.setContent(HarvestOperationTest.getRecordContent(recordId191919).getBytes(StandardCharsets.UTF_8));

        HashSet<Integer> hasHoldings = new HashSet<>(Collections.singletonList(IMS_LIBRARY));
        when(holdingsItemsConnector.hasHoldings("faust", hasHoldings)).thenReturn(hasHoldings);

        // Mock rawrepo return values
        when(rawRepoConnector.dequeue(CONSUMER_ID)).thenReturn(HarvestOperationTest.getQueueItem(imsRecordId, QUEUED_TIME)).thenReturn(null);

        when(rawRepoRecordServiceConnector.getRecordDataCollectionDataIO(eq(imsRecordId), any(RecordServiceConnector.Params.class))).thenReturn(new HashMap<>() {{
            put(imsRecordId.getBibliographicRecordId(), imsRecord);
        }});
        when(rawRepoRecordServiceConnector.getRecordDataCollectionDataIO(eq(dbcRecordId), any(RecordServiceConnector.Params.class))).thenReturn(new HashMap<>() {{
            put(dbcRecordId.getBibliographicRecordId(), dbcRecord);
        }});

        when(rawRepoRecordServiceConnector.recordFetch(imsRecordId)).thenReturn(imsRecord);
        when(rawRepoRecordServiceConnector.recordFetch(dbcRecordId)).thenReturn(dbcRecord);

        when(rawRepoRecordServiceConnector.recordExists(dbcRecordId.getAgencyId(), imsRecordId.getBibliographicRecordId())).thenReturn(true);

        mockedFileStoreServiceConnector = new MockedFileStoreServiceConnector();
        mockedFileStoreServiceConnector.destinations.add(harvesterDataFileWith775100.toPath());

        MarcXchangeCollectionExpectation marcXchangeCollectionExpectation775100 = new MarcXchangeCollectionExpectation();
        marcXchangeCollectionExpectation775100.records.add(getMarcXchangeRecord(recordId191919));
        recordsExpectationsFor775100.add(marcXchangeCollectionExpectation775100);
        addiMetaDataExpectationsFor775100.add(new AddiMetaData().withBibliographicRecordId(dbcRecord.getRecordId().getBibliographicRecordId()).withSubmitterNumber(775100).withFormat("katalog").withCreationDate(Date.from(Instant.parse(dbcRecord.getCreated()))).withEnrichmentTrail(dbcRecord.getEnrichmentTrail()).withTrackingId(dbcRecord.getTrackingId()).withDeleted(false).withLibraryRules(new AddiMetaData.LibraryRules()));

        ImsHarvestOperation harvestOperation = newImsHarvestOperation();
        harvestOperation.execute();

        addiFileVerifier.verify(harvesterDataFileWith775100, addiMetaDataExpectationsFor775100, recordsExpectationsFor775100);
        verifyJobSpecification(mockedJobStoreServiceConnector.jobInputStreams.remove().getJobSpecification(), newImsHarvestOperation().getJobSpecificationTemplate(775100));
    }

    @Test
    public void imsRecordIsDeletedAndNoHoldingExists_recordIsSkipped() throws SQLException, RecordServiceConnectorException, HarvesterException, QueueException {
        RecordIdDTO imsRecordId = new RecordIdDTO("faust", IMS_LIBRARY);
        RecordDTO imsRecord = new RecordDTO();
        imsRecord.setRecordId(imsRecordId);
        imsRecord.setCreated(Instant.now().toString());
        imsRecord.setContent(HarvestOperationTest.getRecordContent(imsRecordId).getBytes(StandardCharsets.UTF_8));
        imsRecord.setDeleted(true);

        RecordIdDTO recordId191919 = new RecordIdDTO("faust", 191919);
        RecordIdDTO dbcRecordId = new RecordIdDTO("faust", 870970);
        RecordDTO dbcRecord = new RecordDTO();
        dbcRecord.setRecordId(dbcRecordId);
        dbcRecord.setCreated(Instant.now().toString());
        dbcRecord.setContent(HarvestOperationTest.getRecordContent(recordId191919).getBytes(StandardCharsets.UTF_8));

        when(holdingsItemsConnector.hasHoldings("faust", new HashSet<>(Collections.singletonList(IMS_LIBRARY)))).thenReturn(Collections.emptySet());

        // Mock rawrepo return values
        when(rawRepoConnector.dequeue(CONSUMER_ID)).thenReturn(HarvestOperationTest.getQueueItem(imsRecordId, QUEUED_TIME)).thenReturn(null);

        when(rawRepoRecordServiceConnector.recordFetch(imsRecordId)).thenReturn(imsRecord);

        mockedFileStoreServiceConnector = new MockedFileStoreServiceConnector();
        mockedFileStoreServiceConnector.destinations.add(harvesterDataFileWith775100.toPath());

        ImsHarvestOperation harvestOperation = newImsHarvestOperation();
        harvestOperation.execute();

        addiFileVerifier.verify(harvesterDataFileWith775100, addiMetaDataExpectationsFor775100, recordsExpectationsFor775100);
        assertThat("Number of job created", mockedJobStoreServiceConnector.jobInputStreams.size(), is(0));
    }

    /**
     * Test if the ims library has a section record and in that case use that
     */
    @Test
    public void localSectionIsActive() throws SQLException, QueueException, RecordServiceConnectorException, HarvesterException {
        // Records section
        RecordIdDTO dbcRecordId = new RecordIdDTO("11111", HarvestOperation.DBC_LIBRARY);
        RecordDTO dbcRecord = new RecordDTO();
        dbcRecord.setRecordId(dbcRecordId);
        dbcRecord.setCreated(Instant.now().toString());
        dbcRecord.setContent(HarvestOperationTest.getRecordContent(dbcRecordId).getBytes(StandardCharsets.UTF_8));
        dbcRecord.setEnrichmentTrail("191919,870970");
        dbcRecord.setTrackingId("tracking id");

        RecordIdDTO dbcHeadRecordId = new RecordIdDTO("33333", HarvestOperation.DBC_LIBRARY);
        RecordDTO dbcHeadRecord = new RecordDTO();
        dbcHeadRecord.setRecordId(dbcHeadRecordId);
        dbcHeadRecord.setCreated(Instant.now().toString());
        dbcHeadRecord.setContent(HarvestOperationTest.getRecordContent(dbcHeadRecordId).getBytes(StandardCharsets.UTF_8));

        RecordIdDTO imsSectionRecordId = new RecordIdDTO("22222", 710100);
        RecordDTO imsSectionRecord = new RecordDTO();
        imsSectionRecord.setRecordId(imsSectionRecordId);
        imsSectionRecord.setCreated(Instant.now().toString());
        imsSectionRecord.setContent(HarvestOperationTest.getRecordContent(imsSectionRecordId).getBytes(StandardCharsets.UTF_8));

        RecordIdDTO imsRecordId = new RecordIdDTO("11111", 710100);
        RecordDTO imsRecord = new RecordDTO();
        imsRecord.setRecordId(imsRecordId);
        imsRecord.setCreated(Instant.now().toString());
        imsRecord.setContent(HarvestOperationTest.getRecordContent(imsRecordId).getBytes(StandardCharsets.UTF_8));

        // Mock section
        when(rawRepoConnector.dequeue(CONSUMER_ID)).thenReturn(HarvestOperationTest.getQueueItem(imsRecordId, QUEUED_TIME)).thenReturn(null);

        when(rawRepoRecordServiceConnector.getRecordDataCollectionDataIO(any(RecordIdDTO.class), any(RecordServiceConnector.Params.class))).thenReturn(new HashMap<>() {{
            put(dbcHeadRecordId.getBibliographicRecordId(), dbcHeadRecord);
            put(imsSectionRecordId.getBibliographicRecordId(), imsSectionRecord);
            put(dbcRecordId.getBibliographicRecordId(), dbcRecord);
        }});


        when(rawRepoRecordServiceConnector.recordFetch(eq(imsRecordId))).thenReturn(imsRecord);

        // Expected result section
        MarcXchangeCollectionExpectation marcXchangeCollectionExpectation710100 = new MarcXchangeCollectionExpectation();
        marcXchangeCollectionExpectation710100.records.add(getMarcXchangeRecord(dbcHeadRecordId));
        marcXchangeCollectionExpectation710100.records.add(getMarcXchangeRecord(imsSectionRecordId));
        marcXchangeCollectionExpectation710100.records.add(getMarcXchangeRecord(dbcRecordId));
        recordsExpectationsFor710100.add(marcXchangeCollectionExpectation710100);
        addiMetaDataExpectationsFor710100.add(new AddiMetaData().withBibliographicRecordId(imsRecord.getRecordId().getBibliographicRecordId()).withSubmitterNumber(710100).withFormat("katalog").withCreationDate(Date.from(Instant.parse(imsRecord.getCreated()))).withEnrichmentTrail(imsRecord.getEnrichmentTrail()).withTrackingId(imsRecord.getTrackingId()).withEnrichmentTrail(dbcRecord.getEnrichmentTrail()).withDeleted(false).withLibraryRules(new AddiMetaData.LibraryRules()));

        // Execute test section
        ImsHarvestOperation harvestOperation = newImsHarvestOperation();
        harvestOperation.execute();

        addiFileVerifier.verify(harvesterDataFileWith710100, addiMetaDataExpectationsFor710100, recordsExpectationsFor710100);
        verifyJobSpecification(mockedJobStoreServiceConnector.jobInputStreams.remove().getJobSpecification(), newImsHarvestOperation().getJobSpecificationTemplate(710100));
    }

    /**
     * Test if the ims library has a head record and in that case use that
     */
    @Test
    public void localHeadIsActive() throws RecordServiceConnectorException, SQLException, QueueException, HarvesterException {
        // Records section
        RecordIdDTO dbcRecordId = new RecordIdDTO("11111", HarvestOperation.DBC_LIBRARY);
        RecordDTO dbcRecord = new RecordDTO();
        dbcRecord.setRecordId(dbcRecordId);
        dbcRecord.setCreated(Instant.now().toString());
        dbcRecord.setContent(HarvestOperationTest.getRecordContent(dbcRecordId).getBytes(StandardCharsets.UTF_8));
        dbcRecord.setEnrichmentTrail("191919,870970");
        dbcRecord.setTrackingId("tracking id");

        RecordIdDTO imsHeadRecordId = new RecordIdDTO("33333", 710100);
        RecordDTO imsHeadRecord = new RecordDTO();
        imsHeadRecord.setRecordId(imsHeadRecordId);
        imsHeadRecord.setCreated(Instant.now().toString());
        imsHeadRecord.setContent(HarvestOperationTest.getRecordContent(imsHeadRecordId).getBytes(StandardCharsets.UTF_8));

        RecordIdDTO dbcSectionRecordId = new RecordIdDTO("22222", HarvestOperation.DBC_LIBRARY);
        RecordDTO dbcSectionRecord = new RecordDTO();
        dbcSectionRecord.setRecordId(dbcSectionRecordId);
        dbcSectionRecord.setCreated(Instant.now().toString());
        dbcSectionRecord.setContent(HarvestOperationTest.getRecordContent(dbcSectionRecordId).getBytes(StandardCharsets.UTF_8));

        RecordIdDTO imsRecordId = new RecordIdDTO("11111", 710100);
        RecordDTO imsRecord = new RecordDTO();
        imsRecord.setRecordId(imsRecordId);
        imsRecord.setCreated(Instant.now().toString());
        imsRecord.setContent(HarvestOperationTest.getRecordContent(imsRecordId).getBytes(StandardCharsets.UTF_8));

        // Mock section
        when(rawRepoConnector.dequeue(CONSUMER_ID)).thenReturn(HarvestOperationTest.getQueueItem(imsRecordId, QUEUED_TIME)).thenReturn(null);

        when(rawRepoRecordServiceConnector.getRecordDataCollectionDataIO(any(RecordIdDTO.class), any(RecordServiceConnector.Params.class))).thenReturn(new HashMap<>() {{
            put(imsHeadRecordId.getBibliographicRecordId(), imsHeadRecord);
            put(dbcSectionRecordId.getBibliographicRecordId(), dbcSectionRecord);
            put(dbcRecordId.getBibliographicRecordId(), dbcRecord);
        }});

        when(rawRepoRecordServiceConnector.recordFetch(eq(imsRecordId))).thenReturn(imsRecord);

        // Expected result section
        MarcXchangeCollectionExpectation marcXchangeCollectionExpectation710100 = new MarcXchangeCollectionExpectation();
        marcXchangeCollectionExpectation710100.records.add(getMarcXchangeRecord(imsHeadRecordId));
        marcXchangeCollectionExpectation710100.records.add(getMarcXchangeRecord(dbcSectionRecordId));
        marcXchangeCollectionExpectation710100.records.add(getMarcXchangeRecord(dbcRecordId));
        recordsExpectationsFor710100.add(marcXchangeCollectionExpectation710100);
        addiMetaDataExpectationsFor710100.add(new AddiMetaData().withBibliographicRecordId(imsRecord.getRecordId().getBibliographicRecordId()).withSubmitterNumber(710100).withFormat("katalog").withCreationDate(Date.from(Instant.parse(imsRecord.getCreated()))).withEnrichmentTrail(dbcRecord.getEnrichmentTrail()).withTrackingId(imsRecord.getTrackingId()).withDeleted(false).withLibraryRules(new AddiMetaData.LibraryRules()));

        // Execute test section
        ImsHarvestOperation harvestOperation = newImsHarvestOperation();
        harvestOperation.execute();

        addiFileVerifier.verify(harvesterDataFileWith710100, addiMetaDataExpectationsFor710100, recordsExpectationsFor710100);
        verifyJobSpecification(mockedJobStoreServiceConnector.jobInputStreams.remove().getJobSpecification(), newImsHarvestOperation().getJobSpecificationTemplate(710100));
    }

    /**
     * Test if the ims library has both a head record and a section record and in that case use that
     */
    @Test
    public void localHeadAndSectionIsActive() throws HarvesterException, RecordServiceConnectorException, SQLException, QueueException {
        // Records section
        RecordIdDTO dbcRecordId = new RecordIdDTO("11111", HarvestOperation.DBC_LIBRARY);
        RecordDTO dbcRecord = new RecordDTO();
        dbcRecord.setRecordId(dbcRecordId);
        dbcRecord.setCreated(Instant.now().toString());
        dbcRecord.setContent(HarvestOperationTest.getRecordContent(dbcRecordId).getBytes(StandardCharsets.UTF_8));
        dbcRecord.setEnrichmentTrail("191919,870970");
        dbcRecord.setTrackingId("tracking id");

        RecordIdDTO imsHeadRecordId = new RecordIdDTO("33333", 710100);
        RecordDTO imsHeadRecord = new RecordDTO();
        imsHeadRecord.setRecordId(imsHeadRecordId);
        imsHeadRecord.setCreated(Instant.now().toString());
        imsHeadRecord.setContent(HarvestOperationTest.getRecordContent(imsHeadRecordId).getBytes(StandardCharsets.UTF_8));

        RecordIdDTO imsSectionRecordId = new RecordIdDTO("22222", 710100);
        RecordDTO imsSectionRecord = new RecordDTO();
        imsSectionRecord.setRecordId(imsSectionRecordId);
        imsSectionRecord.setCreated(Instant.now().toString());
        imsSectionRecord.setContent(HarvestOperationTest.getRecordContent(imsSectionRecordId).getBytes(StandardCharsets.UTF_8));

        RecordIdDTO imsRecordId = new RecordIdDTO("11111", 710100);
        RecordDTO imsRecord = new RecordDTO();
        imsRecord.setRecordId(imsRecordId);
        imsRecord.setCreated(Instant.now().toString());
        imsRecord.setContent(HarvestOperationTest.getRecordContent(imsRecordId).getBytes(StandardCharsets.UTF_8));

        // Mock section
        when(rawRepoConnector.dequeue(CONSUMER_ID)).thenReturn(HarvestOperationTest.getQueueItem(imsRecordId, QUEUED_TIME)).thenReturn(null);

        when(rawRepoRecordServiceConnector.getRecordDataCollectionDataIO(any(RecordIdDTO.class), any(RecordServiceConnector.Params.class))).thenReturn(new HashMap<>() {{
            put(imsHeadRecordId.getBibliographicRecordId(), imsHeadRecord);
            put(imsSectionRecordId.getBibliographicRecordId(), imsSectionRecord);
            put(dbcRecordId.getBibliographicRecordId(), dbcRecord);
        }});

        when(rawRepoRecordServiceConnector.recordFetch(eq(imsRecordId))).thenReturn(imsRecord);

        // Expected result section
        MarcXchangeCollectionExpectation marcXchangeCollectionExpectation710100 = new MarcXchangeCollectionExpectation();
        marcXchangeCollectionExpectation710100.records.add(getMarcXchangeRecord(imsHeadRecordId));
        marcXchangeCollectionExpectation710100.records.add(getMarcXchangeRecord(imsSectionRecordId));
        marcXchangeCollectionExpectation710100.records.add(getMarcXchangeRecord(dbcRecordId));
        recordsExpectationsFor710100.add(marcXchangeCollectionExpectation710100);
        addiMetaDataExpectationsFor710100.add(new AddiMetaData().withBibliographicRecordId(imsRecord.getRecordId().getBibliographicRecordId()).withSubmitterNumber(710100).withFormat("katalog").withCreationDate(Date.from(Instant.parse(imsRecord.getCreated()))).withEnrichmentTrail(dbcRecord.getEnrichmentTrail()).withTrackingId(imsRecord.getTrackingId()).withDeleted(false).withLibraryRules(new AddiMetaData.LibraryRules()));

        // Execute test section
        ImsHarvestOperation harvestOperation = newImsHarvestOperation();
        harvestOperation.execute();

        addiFileVerifier.verify(harvesterDataFileWith710100, addiMetaDataExpectationsFor710100, recordsExpectationsFor710100);
        verifyJobSpecification(mockedJobStoreServiceConnector.jobInputStreams.remove().getJobSpecification(), newImsHarvestOperation().getJobSpecificationTemplate(710100));
    }

    /**
     * Test if the ims library has a deleted section record and in that case, the 870970 record is added to the collection
     */
    @Test
    public void localSectionIsDeletedFetchDBCInstead() throws RecordServiceConnectorException, SQLException, QueueException, HarvesterException {
        // Records section
        RecordIdDTO dbcRecordId = new RecordIdDTO("11111", HarvestOperation.DBC_LIBRARY);
        RecordDTO dbcRecord = new RecordDTO();
        dbcRecord.setRecordId(dbcRecordId);
        dbcRecord.setCreated(Instant.now().toString());
        dbcRecord.setContent(HarvestOperationTest.getRecordContent(dbcRecordId).getBytes(StandardCharsets.UTF_8));
        dbcRecord.setEnrichmentTrail("191919,870970");
        dbcRecord.setTrackingId("tracking id");

        RecordIdDTO dbcHeadRecordId = new RecordIdDTO("33333", HarvestOperation.DBC_LIBRARY);
        RecordDTO dbcHeadRecord = new RecordDTO();
        dbcHeadRecord.setRecordId(dbcHeadRecordId);
        dbcHeadRecord.setCreated(Instant.now().toString());
        dbcHeadRecord.setContent(HarvestOperationTest.getRecordContent(dbcHeadRecordId).getBytes(StandardCharsets.UTF_8));

        RecordIdDTO dbcSectionRecordId = new RecordIdDTO("22222", HarvestOperation.DBC_LIBRARY);
        RecordDTO dbcSectionRecord = new RecordDTO();
        dbcSectionRecord.setRecordId(dbcSectionRecordId);
        dbcSectionRecord.setCreated(Instant.now().toString());
        dbcSectionRecord.setContent(HarvestOperationTest.getRecordContent(dbcSectionRecordId).getBytes(StandardCharsets.UTF_8));

        RecordIdDTO imsSectionRecordId = new RecordIdDTO("22222", 710100);
        RecordDTO imsSectionRecord = new RecordDTO();
        imsSectionRecord.setRecordId(imsSectionRecordId);
        imsSectionRecord.setCreated(Instant.now().toString());
        imsSectionRecord.setContent(HarvestOperationTest.getDeleteRecordContent(imsSectionRecordId, 's').getBytes(StandardCharsets.UTF_8));

        RecordIdDTO imsRecordId = new RecordIdDTO("11111", 710100);
        RecordDTO imsRecord = new RecordDTO();
        imsRecord.setRecordId(imsRecordId);
        imsRecord.setCreated(Instant.now().toString());
        imsRecord.setContent(HarvestOperationTest.getRecordContent(imsRecordId).getBytes(StandardCharsets.UTF_8));

        // Mock section
        when(rawRepoConnector.dequeue(CONSUMER_ID)).thenReturn(HarvestOperationTest.getQueueItem(imsRecordId, QUEUED_TIME)).thenReturn(null);

        when(rawRepoRecordServiceConnector.getRecordDataCollectionDataIO(any(RecordIdDTO.class), any(RecordServiceConnector.Params.class))).thenReturn(new HashMap<>() {{
            put(dbcHeadRecordId.getBibliographicRecordId(), dbcHeadRecord);
            put(imsSectionRecordId.getBibliographicRecordId(), imsSectionRecord);
            put(dbcRecordId.getBibliographicRecordId(), dbcRecord);
        }});

        when(rawRepoRecordServiceConnector.recordFetch(eq(dbcSectionRecordId), any(RecordServiceConnector.Params.class))).thenReturn(dbcSectionRecord);
        when(rawRepoRecordServiceConnector.recordFetch(eq(imsRecordId))).thenReturn(imsRecord);

        // Expected result section
        MarcXchangeCollectionExpectation marcXchangeCollectionExpectation710100 = new MarcXchangeCollectionExpectation();
        marcXchangeCollectionExpectation710100.records.add(getMarcXchangeRecord(dbcHeadRecordId));
        marcXchangeCollectionExpectation710100.records.add(getMarcXchangeRecord(dbcSectionRecordId));
        marcXchangeCollectionExpectation710100.records.add(getMarcXchangeRecord(dbcRecordId));
        recordsExpectationsFor710100.add(marcXchangeCollectionExpectation710100);
        addiMetaDataExpectationsFor710100.add(new AddiMetaData().withBibliographicRecordId(imsRecord.getRecordId().getBibliographicRecordId()).withSubmitterNumber(710100).withFormat("katalog").withCreationDate(Date.from(Instant.parse(imsRecord.getCreated()))).withEnrichmentTrail(dbcRecord.getEnrichmentTrail()).withTrackingId(imsRecord.getTrackingId()).withDeleted(false).withLibraryRules(new AddiMetaData.LibraryRules()));

        // Execute test section
        ImsHarvestOperation harvestOperation = newImsHarvestOperation();
        harvestOperation.execute();

        addiFileVerifier.verify(harvesterDataFileWith710100, addiMetaDataExpectationsFor710100, recordsExpectationsFor710100);
        verifyJobSpecification(mockedJobStoreServiceConnector.jobInputStreams.remove().getJobSpecification(), newImsHarvestOperation().getJobSpecificationTemplate(710100));
    }

    /**
     * Test if the ims library has a deleted head record and in that case, the 870970 record is added to the collection
     */
    @Test
    public void localHeadIsDeletedFetchDBCInstead() throws HarvesterException, RecordServiceConnectorException, SQLException, QueueException {
        // Records section
        RecordIdDTO dbcRecordId = new RecordIdDTO("11111", HarvestOperation.DBC_LIBRARY);
        RecordDTO dbcRecord = new RecordDTO();
        dbcRecord.setRecordId(dbcRecordId);
        dbcRecord.setCreated(Instant.now().toString());
        dbcRecord.setContent(HarvestOperationTest.getRecordContent(dbcRecordId).getBytes(StandardCharsets.UTF_8));
        dbcRecord.setEnrichmentTrail("191919,870970");
        dbcRecord.setTrackingId("tracking id");

        RecordIdDTO dbcHeadRecordId = new RecordIdDTO("33333", HarvestOperation.DBC_LIBRARY);
        RecordDTO dbcHeadRecord = new RecordDTO();
        dbcHeadRecord.setRecordId(dbcHeadRecordId);
        dbcHeadRecord.setCreated(Instant.now().toString());
        dbcHeadRecord.setContent(HarvestOperationTest.getRecordContent(dbcHeadRecordId).getBytes(StandardCharsets.UTF_8));

        RecordIdDTO imsHeadRecordId = new RecordIdDTO("33333", 710100);
        RecordDTO imsHeadRecord = new RecordDTO();
        imsHeadRecord.setRecordId(imsHeadRecordId);
        imsHeadRecord.setCreated(Instant.now().toString());
        imsHeadRecord.setContent(HarvestOperationTest.getDeleteRecordContent(imsHeadRecordId, 'h').getBytes(StandardCharsets.UTF_8));

        RecordIdDTO imsRecordId = new RecordIdDTO("11111", 710100);
        RecordDTO imsRecord = new RecordDTO();
        imsRecord.setRecordId(imsRecordId);
        imsRecord.setCreated(Instant.now().toString());
        imsRecord.setContent(HarvestOperationTest.getRecordContent(imsRecordId).getBytes(StandardCharsets.UTF_8));

        // Mock section
        when(rawRepoConnector.dequeue(CONSUMER_ID)).thenReturn(HarvestOperationTest.getQueueItem(imsRecordId, QUEUED_TIME)).thenReturn(null);

        when(rawRepoRecordServiceConnector.getRecordDataCollectionDataIO(any(RecordIdDTO.class), any(RecordServiceConnector.Params.class))).thenReturn(new HashMap<>() {{
            put(imsHeadRecordId.getBibliographicRecordId(), imsHeadRecord);
            put(dbcRecordId.getBibliographicRecordId(), dbcRecord);
        }});

        when(rawRepoRecordServiceConnector.recordFetch(eq(dbcHeadRecordId), any(RecordServiceConnector.Params.class))).thenReturn(dbcHeadRecord);
        when(rawRepoRecordServiceConnector.recordFetch(eq(imsRecordId))).thenReturn(imsRecord);

        // Expected result section
        MarcXchangeCollectionExpectation marcXchangeCollectionExpectation710100 = new MarcXchangeCollectionExpectation();
        marcXchangeCollectionExpectation710100.records.add(getMarcXchangeRecord(dbcHeadRecordId));
        marcXchangeCollectionExpectation710100.records.add(getMarcXchangeRecord(dbcRecordId));
        recordsExpectationsFor710100.add(marcXchangeCollectionExpectation710100);
        addiMetaDataExpectationsFor710100.add(new AddiMetaData().withBibliographicRecordId(imsRecord.getRecordId().getBibliographicRecordId()).withSubmitterNumber(710100).withFormat("katalog").withCreationDate(Date.from(Instant.parse(imsRecord.getCreated()))).withEnrichmentTrail(dbcRecord.getEnrichmentTrail()).withTrackingId(imsRecord.getTrackingId()).withDeleted(false).withLibraryRules(new AddiMetaData.LibraryRules()));

        // Execute test section
        ImsHarvestOperation harvestOperation = newImsHarvestOperation();
        harvestOperation.execute();

        addiFileVerifier.verify(harvesterDataFileWith710100, addiMetaDataExpectationsFor710100, recordsExpectationsFor710100);
        verifyJobSpecification(mockedJobStoreServiceConnector.jobInputStreams.remove().getJobSpecification(), newImsHarvestOperation().getJobSpecificationTemplate(710100));
    }

    /**
     * Test if the ims library has a deleted head and section record and in that case, the 870970 records is added to the collection
     */
    @Test
    public void localHeadAndSectionIsDeletedFetchDBCInstead() throws SQLException, QueueException, RecordServiceConnectorException, HarvesterException {
        // Records section
        RecordIdDTO dbcRecordId = new RecordIdDTO("11111", HarvestOperation.DBC_LIBRARY);
        RecordDTO dbcRecord = new RecordDTO();
        dbcRecord.setRecordId(dbcRecordId);
        dbcRecord.setCreated(Instant.now().toString());
        dbcRecord.setContent(HarvestOperationTest.getRecordContent(dbcRecordId).getBytes(StandardCharsets.UTF_8));
        dbcRecord.setEnrichmentTrail("191919,870970");
        dbcRecord.setTrackingId("tracking id");

        RecordIdDTO dbcHeadRecordId = new RecordIdDTO("33333", HarvestOperation.DBC_LIBRARY);
        RecordDTO dbcHeadRecord = new RecordDTO();
        dbcHeadRecord.setRecordId(dbcHeadRecordId);
        dbcHeadRecord.setCreated(Instant.now().toString());
        dbcHeadRecord.setContent(HarvestOperationTest.getRecordContent(dbcHeadRecordId).getBytes(StandardCharsets.UTF_8));

        RecordIdDTO dbcSectionRecordId = new RecordIdDTO("22222", HarvestOperation.DBC_LIBRARY);
        RecordDTO dbcSectionRecord = new RecordDTO();
        dbcSectionRecord.setRecordId(dbcSectionRecordId);
        dbcSectionRecord.setCreated(Instant.now().toString());
        dbcSectionRecord.setContent(HarvestOperationTest.getRecordContent(dbcSectionRecordId).getBytes(StandardCharsets.UTF_8));

        RecordIdDTO imsHeadRecordId = new RecordIdDTO("33333", 710100);
        RecordDTO imsHeadRecord = new RecordDTO();
        imsHeadRecord.setRecordId(imsHeadRecordId);
        imsHeadRecord.setCreated(Instant.now().toString());
        imsHeadRecord.setContent(HarvestOperationTest.getDeleteRecordContent(imsHeadRecordId, 'h').getBytes(StandardCharsets.UTF_8));

        RecordIdDTO imsSectionRecordId = new RecordIdDTO("22222", 710100);
        RecordDTO imsSectionRecord = new RecordDTO();
        imsSectionRecord.setRecordId(imsSectionRecordId);
        imsSectionRecord.setCreated(Instant.now().toString());
        imsSectionRecord.setContent(HarvestOperationTest.getDeleteRecordContent(imsSectionRecordId, 's').getBytes(StandardCharsets.UTF_8));

        RecordIdDTO imsRecordId = new RecordIdDTO("11111", 710100);
        RecordDTO imsRecord = new RecordDTO();
        imsRecord.setRecordId(imsRecordId);
        imsRecord.setCreated(Instant.now().toString());
        imsRecord.setContent(HarvestOperationTest.getRecordContent(imsRecordId).getBytes(StandardCharsets.UTF_8));

        // Mock section
        when(rawRepoConnector.dequeue(CONSUMER_ID)).thenReturn(HarvestOperationTest.getQueueItem(imsRecordId, QUEUED_TIME)).thenReturn(null);

        when(rawRepoRecordServiceConnector.getRecordDataCollectionDataIO(any(RecordIdDTO.class), any(RecordServiceConnector.Params.class))).thenReturn(new HashMap<>() {{
            put(imsHeadRecordId.getBibliographicRecordId(), imsHeadRecord);
            put(imsSectionRecordId.getBibliographicRecordId(), imsSectionRecord);
            put(dbcRecordId.getBibliographicRecordId(), dbcRecord);
        }});

        when(rawRepoRecordServiceConnector.recordFetch(eq(dbcSectionRecordId), any(RecordServiceConnector.Params.class))).thenReturn(dbcSectionRecord);
        when(rawRepoRecordServiceConnector.recordFetch(eq(dbcHeadRecordId), any(RecordServiceConnector.Params.class))).thenReturn(dbcHeadRecord);
        when(rawRepoRecordServiceConnector.recordFetch(eq(imsRecordId))).thenReturn(imsRecord);

        // Expected result section
        MarcXchangeCollectionExpectation marcXchangeCollectionExpectation710100 = new MarcXchangeCollectionExpectation();
        marcXchangeCollectionExpectation710100.records.add(getMarcXchangeRecord(dbcHeadRecordId));
        marcXchangeCollectionExpectation710100.records.add(getMarcXchangeRecord(dbcSectionRecordId));
        marcXchangeCollectionExpectation710100.records.add(getMarcXchangeRecord(dbcRecordId));
        recordsExpectationsFor710100.add(marcXchangeCollectionExpectation710100);
        addiMetaDataExpectationsFor710100.add(new AddiMetaData().withBibliographicRecordId(imsRecord.getRecordId().getBibliographicRecordId()).withSubmitterNumber(710100).withFormat("katalog").withCreationDate(Date.from(Instant.parse(imsRecord.getCreated()))).withEnrichmentTrail(dbcRecord.getEnrichmentTrail()).withTrackingId(imsRecord.getTrackingId()).withDeleted(false).withLibraryRules(new AddiMetaData.LibraryRules()));

        // Execute test section
        ImsHarvestOperation harvestOperation = newImsHarvestOperation();
        harvestOperation.execute();

        addiFileVerifier.verify(harvesterDataFileWith710100, addiMetaDataExpectationsFor710100, recordsExpectationsFor710100);
        verifyJobSpecification(mockedJobStoreServiceConnector.jobInputStreams.remove().getJobSpecification(), newImsHarvestOperation().getJobSpecificationTemplate(710100));
    }

    private ImsHarvestOperation newImsHarvestOperation() {
        HarvesterJobBuilderFactory harvesterJobBuilderFactory;
        try {
            harvesterJobBuilderFactory = new HarvesterJobBuilderFactory(new BinaryFileStoreFsImpl(createDir(tmpFolder)), mockedFileStoreServiceConnector, mockedJobStoreServiceConnector);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        RRHarvesterConfig config = HarvesterTestUtil.getRRHarvesterConfig();
        config.getContent().withConsumerId(CONSUMER_ID).withFormat("katalog").withIncludeRelations(true).withHarvesterType(RRHarvesterConfig.HarvesterType.IMS);
        try {
            return new ImsHarvestOperation(config, harvesterJobBuilderFactory, taskRepo, vipCoreConnection, rawRepoConnector, holdingsItemsConnector, rawRepoRecordServiceConnector, metricRegistry);
        } catch (QueueException | SQLException | ConfigurationException e) {
            throw new IllegalStateException(e);
        }
    }

    private void verifyJobSpecification(JobSpecification jobSpecification, JobSpecification jobSpecificationTemplate) {
        assertThat(jobSpecification.getPackaging(), is(jobSpecificationTemplate.getPackaging()));
        assertThat(jobSpecification.getFormat(), is(jobSpecificationTemplate.getFormat()));
        assertThat(jobSpecification.getCharset(), is(jobSpecificationTemplate.getCharset()));
        assertThat(jobSpecification.getDestination(), is(jobSpecificationTemplate.getDestination()));
        assertThat(jobSpecification.getSubmitterId(), is(jobSpecificationTemplate.getSubmitterId()));
    }

    private MarcXchangeRecordExpectation getMarcXchangeRecord(RecordIdDTO recordId) {
        return new MarcXchangeRecordExpectation(recordId.getBibliographicRecordId(), recordId.getAgencyId());
    }
}
