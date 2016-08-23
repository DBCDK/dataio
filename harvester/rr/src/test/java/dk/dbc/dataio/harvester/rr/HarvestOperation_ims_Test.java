/*
 * DataIO - Data IO
 * Copyright (C) 2015 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
 * Denmark. CVR: 15149043
 *
 * This file is part of DataIO.
 *
 * DataIO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DataIO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DataIO.  If not, see <http://www.gnu.org/licenses/>.
 */

package dk.dbc.dataio.harvester.rr;

import dk.dbc.dataio.bfs.ejb.BinaryFileStoreBeanTestUtil;
import dk.dbc.dataio.commons.types.AddiMetaData;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.utils.jobstore.MockedJobStoreServiceConnector;
import dk.dbc.dataio.commons.utils.test.jndi.InMemoryInitialContextFactory;
import dk.dbc.dataio.filestore.service.connector.MockedFileStoreServiceConnector;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.RRHarvesterConfig;
import dk.dbc.dataio.harvester.utils.datafileverifier.AddiFileVerifier;
import dk.dbc.dataio.harvester.utils.datafileverifier.DataContainerExpectation;
import dk.dbc.dataio.harvester.utils.datafileverifier.MarcExchangeCollectionExpectation;
import dk.dbc.dataio.harvester.utils.datafileverifier.MarcExchangeRecordExpectation;
import dk.dbc.dataio.harvester.utils.datafileverifier.XmlExpectation;
import dk.dbc.dataio.harvester.utils.holdingsitems.HoldingsItemsConnector;
import dk.dbc.dataio.harvester.utils.rawrepo.RawRepoConnector;
import dk.dbc.dataio.jobstore.test.types.JobInfoSnapshotBuilder;
import dk.dbc.dataio.jsonb.JSONBException;
import dk.dbc.marcxmerge.MarcXMergerException;
import dk.dbc.rawrepo.MockedRecord;
import dk.dbc.rawrepo.QueueJob;
import dk.dbc.rawrepo.RawRepoException;
import dk.dbc.rawrepo.Record;
import dk.dbc.rawrepo.RecordId;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.xml.sax.SAXException;

import javax.naming.Context;
import javax.persistence.EntityManager;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HarvestOperation_ims_Test {
    private static final Date QUEUED_TIME = new Date(1467277697583L); // 2016-06-30 11:08:17.583
    private static final String CONSUMER_ID = "consumerId";
    private static final int IMS_LIBRARY = 775100;

    private final static String BFS_BASE_PATH_JNDI_NAME = "bfs/home";

    /* 1st record is a DBC record */
    private final static RecordId FIRST_RECORD_ID = new RecordId("first", HarvestOperation.DBC_LIBRARY);
    private final static String FIRST_RECORD_CONTENT = HarvestOperationTest.getRecordContent(FIRST_RECORD_ID);
    private final static MockedRecord FIRST_RECORD = new MockedRecord(FIRST_RECORD_ID);
    private final static MockedRecord FIRST_RECORD_WITHOUT_ENRICHMENT_TRAIL = new MockedRecord(FIRST_RECORD_ID);
    private final static QueueJob FIRST_QUEUE_JOB = HarvestOperationTest.getQueueJob(FIRST_RECORD_ID, QUEUED_TIME);

    private final static RecordId FIRST_RECORD_HEAD_ID = new RecordId("first-head", HarvestOperation.DBC_LIBRARY);
    private final static String FIRST_RECORD_HEAD_CONTENT = HarvestOperationTest.getRecordContent(FIRST_RECORD_HEAD_ID);
    private final static Record FIRST_RECORD_HEAD = new MockedRecord(FIRST_RECORD_HEAD_ID);

    private final static RecordId FIRST_RECORD_SECTION_ID = new RecordId("first-section", HarvestOperation.DBC_LIBRARY);
    private final static String FIRST_RECORD_SECTION_CONTENT = HarvestOperationTest.getRecordContent(FIRST_RECORD_SECTION_ID);
    private final static Record FIRST_RECORD_SECTION = new MockedRecord(FIRST_RECORD_SECTION_ID);

    /* 2nd record is a local IMS record */
    private final static RecordId SECOND_RECORD_ID = new RecordId("second", IMS_LIBRARY);
    private final static String SECOND_RECORD_CONTENT = HarvestOperationTest.getRecordContent(SECOND_RECORD_ID);
    private final static Record SECOND_RECORD = new MockedRecord(SECOND_RECORD_ID);
    private final static QueueJob SECOND_QUEUE_JOB = HarvestOperationTest.getQueueJob(SECOND_RECORD_ID, QUEUED_TIME);

    static {
        FIRST_RECORD_HEAD.setContent(FIRST_RECORD_HEAD_CONTENT.getBytes(StandardCharsets.UTF_8));
        FIRST_RECORD_SECTION.setContent(FIRST_RECORD_SECTION_CONTENT.getBytes(StandardCharsets.UTF_8));
        FIRST_RECORD.setContent(FIRST_RECORD_CONTENT.getBytes(StandardCharsets.UTF_8));
        FIRST_RECORD.setEnrichmentTrail("191919,870970");
        FIRST_RECORD.setTrackingId("tracking id");
        FIRST_RECORD_WITHOUT_ENRICHMENT_TRAIL.setTrackingId("tracking id");
        SECOND_RECORD.setContent(SECOND_RECORD_CONTENT.getBytes(StandardCharsets.UTF_8));
    }

    private final EntityManager entityManager = mock(EntityManager.class);
    private final RawRepoConnector rawRepoConnector = mock(RawRepoConnector.class);
    private final AgencyConnection agencyConnection = mock(AgencyConnection.class);
    private final HoldingsItemsConnector holdingsItemsConnector = mock(HoldingsItemsConnector.class);

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

    @Rule
    public TemporaryFolder tmpFolder = new TemporaryFolder();

    @BeforeClass
    public static void setup() {
        // sets up the InMemoryInitialContextFactory as default factory.
        System.setProperty(Context.INITIAL_CONTEXT_FACTORY, InMemoryInitialContextFactory.class.getName());
    }

    @Before
    public void setupMocks() throws IOException, RawRepoException, SQLException, HarvesterException {
        // Enable JNDI lookup of base path for BinaryFileStoreBean
        final File testFolder = tmpFolder.newFolder();
        InMemoryInitialContextFactory.bind(BFS_BASE_PATH_JNDI_NAME, testFolder.toString());

        // Mock rawrepo return values
        when(rawRepoConnector.dequeue(CONSUMER_ID))
                .thenReturn(FIRST_QUEUE_JOB)
                .thenReturn(SECOND_QUEUE_JOB)
                .thenReturn(null);

        // Mock agency-connection and holdings-items lookup
        final Set<Integer> imsLibraries = Stream.of(710100, 737000, 775100, 785100).collect(Collectors.toSet());
        final Set<Integer> hasHoldingsResponse = new LinkedHashSet<>();
        hasHoldingsResponse.add(710100);
        hasHoldingsResponse.add(737000);
        hasHoldingsResponse.add(123456);
        when(holdingsItemsConnector.hasHoldings(FIRST_RECORD_ID.getBibliographicRecordId(), imsLibraries))
                .thenReturn(hasHoldingsResponse);
        when(agencyConnection.getFbsImsLibraries()).thenReturn(imsLibraries);

        // Intercept harvester data files with mocked FileStoreServiceConnectorBean
        harvesterDataFileWith710100 = tmpFolder.newFile();
        harvesterDataFileWith737000 = tmpFolder.newFile();
        harvesterDataFileWith775100 = tmpFolder.newFile();
        mockedFileStoreServiceConnector = new MockedFileStoreServiceConnector();
        mockedFileStoreServiceConnector.destinations.add(harvesterDataFileWith710100.toPath());
        mockedFileStoreServiceConnector.destinations.add(harvesterDataFileWith737000.toPath());
        mockedFileStoreServiceConnector.destinations.add(harvesterDataFileWith775100.toPath());

        // Intercept harvester job specifications with mocked JobStoreServiceConnectorBean
        mockedJobStoreServiceConnector = new MockedJobStoreServiceConnector();
        mockedJobStoreServiceConnector.jobInfoSnapshots.add(new JobInfoSnapshotBuilder().build());
        mockedJobStoreServiceConnector.jobInfoSnapshots.add(new JobInfoSnapshotBuilder().build());
        mockedJobStoreServiceConnector.jobInfoSnapshots.add(new JobInfoSnapshotBuilder().build());

        addiMetaDataExpectationsFor710100 = new ArrayList<>();
        addiMetaDataExpectationsFor737000 = new ArrayList<>();
        addiMetaDataExpectationsFor775100 = new ArrayList<>();
        recordsExpectationsFor710100 = new ArrayList<>();
        recordsExpectationsFor737000 = new ArrayList<>();
        recordsExpectationsFor775100 = new ArrayList<>();
    }

    @Test
    public void harvest_multipleAgencyIdsHarvested_agencyIdsInSeparateJobs()
            throws RawRepoException, SQLException, MarcXMergerException, HarvesterException, ParserConfigurationException,
                   SAXException, JSONBException, IOException {
        // Mock rawrepo return values
        when(rawRepoConnector.fetchRecordCollection(any(RecordId.class)))
                .thenReturn(new HashMap<String, Record>() {{
                    put(FIRST_RECORD_HEAD_ID.getBibliographicRecordId(), FIRST_RECORD_HEAD);
                    put(FIRST_RECORD_SECTION_ID.getBibliographicRecordId(), FIRST_RECORD_SECTION);
                    put(FIRST_RECORD_ID.getBibliographicRecordId(), FIRST_RECORD);
                }})
                .thenReturn(new HashMap<String, Record>() {{
                    put(FIRST_RECORD_HEAD_ID.getBibliographicRecordId(), FIRST_RECORD_HEAD);
                    put(FIRST_RECORD_SECTION_ID.getBibliographicRecordId(), FIRST_RECORD_SECTION);
                    put(FIRST_RECORD_ID.getBibliographicRecordId(), FIRST_RECORD);
                }})
                .thenReturn(new HashMap<String, Record>(){{
                    put(SECOND_RECORD_ID.getBibliographicRecordId(), SECOND_RECORD);
                }});

        when(rawRepoConnector.fetchRecord(any(RecordId.class)))
                .thenReturn(FIRST_RECORD_WITHOUT_ENRICHMENT_TRAIL)
                .thenReturn(FIRST_RECORD_WITHOUT_ENRICHMENT_TRAIL)
                .thenReturn(SECOND_RECORD);

        // Setup harvester datafile content expectations
        final MarcExchangeCollectionExpectation marcExchangeCollectionExpectation710100 = new MarcExchangeCollectionExpectation();
        marcExchangeCollectionExpectation710100.records.add(getMarcExchangeRecord(FIRST_RECORD_HEAD_ID));
        marcExchangeCollectionExpectation710100.records.add(getMarcExchangeRecord(FIRST_RECORD_SECTION_ID));
        marcExchangeCollectionExpectation710100.records.add(getMarcExchangeRecord(FIRST_RECORD_ID));
        final DataContainerExpectation dataContainerExpectation710100 = new DataContainerExpectation();
        dataContainerExpectation710100.dataExpectation = marcExchangeCollectionExpectation710100;
        dataContainerExpectation710100.supplementaryDataExpectation.put("creationDate", getRecordCreationDate(FIRST_RECORD));
        dataContainerExpectation710100.supplementaryDataExpectation.put("enrichmentTrail", FIRST_RECORD.getEnrichmentTrail());
        dataContainerExpectation710100.supplementaryDataExpectation.put("trackingId", FIRST_RECORD.getTrackingId());
        recordsExpectationsFor710100.add(dataContainerExpectation710100);
        addiMetaDataExpectationsFor710100.add(new AddiMetaData()
                .withBibliographicRecordId(FIRST_RECORD.getId().getBibliographicRecordId())
                .withSubmitterNumber(710100)
                .withFormat("katalog")
                .withCreationDate(FIRST_RECORD.getCreated())
                .withEnrichmentTrail(FIRST_RECORD.getEnrichmentTrail())
                .withTrackingId(FIRST_RECORD.getTrackingId())
                .withLibraryRules(new AddiMetaData.LibraryRules()));

        final MarcExchangeCollectionExpectation marcExchangeCollectionExpectation737000 = new MarcExchangeCollectionExpectation();
        marcExchangeCollectionExpectation737000.records.add(getMarcExchangeRecord(FIRST_RECORD_HEAD_ID));
        marcExchangeCollectionExpectation737000.records.add(getMarcExchangeRecord(FIRST_RECORD_SECTION_ID));
        marcExchangeCollectionExpectation737000.records.add(getMarcExchangeRecord(FIRST_RECORD_ID));
        final DataContainerExpectation dataContainerExpectation737000 = new DataContainerExpectation();
        dataContainerExpectation737000.dataExpectation = marcExchangeCollectionExpectation737000;
        dataContainerExpectation737000.supplementaryDataExpectation.put("creationDate", getRecordCreationDate(FIRST_RECORD));
        dataContainerExpectation737000.supplementaryDataExpectation.put("enrichmentTrail", FIRST_RECORD.getEnrichmentTrail());
        dataContainerExpectation737000.supplementaryDataExpectation.put("trackingId", FIRST_RECORD.getTrackingId());
        recordsExpectationsFor737000.add(dataContainerExpectation737000);
        addiMetaDataExpectationsFor737000.add(new AddiMetaData()
                .withBibliographicRecordId(FIRST_RECORD.getId().getBibliographicRecordId())
                .withSubmitterNumber(737000)
                .withFormat("katalog")
                .withCreationDate(FIRST_RECORD.getCreated())
                .withEnrichmentTrail(FIRST_RECORD.getEnrichmentTrail())
                .withTrackingId(FIRST_RECORD.getTrackingId())
                .withLibraryRules(new AddiMetaData.LibraryRules()));

        final MarcExchangeCollectionExpectation marcExchangeCollectionExpectation775100 = new MarcExchangeCollectionExpectation();
        marcExchangeCollectionExpectation775100.records.add(getMarcExchangeRecord(SECOND_RECORD_ID));
        final DataContainerExpectation dataContainerExpectation775100 = new DataContainerExpectation();
        dataContainerExpectation775100.dataExpectation = marcExchangeCollectionExpectation775100;
        dataContainerExpectation775100.supplementaryDataExpectation.put("creationDate", getRecordCreationDate(SECOND_RECORD));
        recordsExpectationsFor775100.add(dataContainerExpectation775100);
        addiMetaDataExpectationsFor775100.add(new AddiMetaData()
                .withBibliographicRecordId(SECOND_RECORD.getId().getBibliographicRecordId())
                .withSubmitterNumber(775100)
                .withFormat("katalog")
                .withCreationDate(SECOND_RECORD.getCreated())
                .withEnrichmentTrail(SECOND_RECORD.getEnrichmentTrail())
                .withTrackingId(SECOND_RECORD.getTrackingId())
                .withLibraryRules(new AddiMetaData.LibraryRules()));

        final ImsHarvestOperation harvestOperation = newImsHarvestOperation();
        harvestOperation.execute(entityManager);

        verifyHarvesterDataFiles();
        verifyJobSpecifications();
    }

    private ImsHarvestOperation newImsHarvestOperation() {
        final HarvesterJobBuilderFactory harvesterJobBuilderFactory = new HarvesterJobBuilderFactory(
                BinaryFileStoreBeanTestUtil.getBinaryFileStoreBean(BFS_BASE_PATH_JNDI_NAME), mockedFileStoreServiceConnector, mockedJobStoreServiceConnector);
        final RRHarvesterConfig config = HarvesterTestUtil.getRRHarvesterConfig();
        config.getContent()
            .withConsumerId(CONSUMER_ID)
            .withFormat("katalog")
            .withIncludeRelations(true)
            .withImsHarvester(true);
        return new ImsHarvestOperation(config, harvesterJobBuilderFactory, agencyConnection, rawRepoConnector, holdingsItemsConnector);
    }

    private void verifyHarvesterDataFiles() throws IOException, ParserConfigurationException, JSONBException, SAXException {
        final AddiFileVerifier addiFileVerifier = new AddiFileVerifier();
        addiFileVerifier.verify(harvesterDataFileWith710100, addiMetaDataExpectationsFor710100, recordsExpectationsFor710100);
        addiFileVerifier.verify(harvesterDataFileWith737000, addiMetaDataExpectationsFor737000, recordsExpectationsFor737000);
        addiFileVerifier.verify(harvesterDataFileWith775100, addiMetaDataExpectationsFor775100, recordsExpectationsFor775100);
    }

    private void verifyJobSpecifications() {
        verifyJobSpecification(mockedJobStoreServiceConnector.jobInputStreams.remove().getJobSpecification(),
                newImsHarvestOperation().getJobSpecificationTemplate(710100));
        verifyJobSpecification(mockedJobStoreServiceConnector.jobInputStreams.remove().getJobSpecification(),
                newImsHarvestOperation().getJobSpecificationTemplate(737000));
        verifyJobSpecification(mockedJobStoreServiceConnector.jobInputStreams.remove().getJobSpecification(),
                newImsHarvestOperation().getJobSpecificationTemplate(775100));
    }

    private void verifyJobSpecification(JobSpecification jobSpecification, JobSpecification jobSpecificationTemplate) {
        assertThat(jobSpecification.getPackaging(), is(jobSpecificationTemplate.getPackaging()));
        assertThat(jobSpecification.getFormat(), is(jobSpecificationTemplate.getFormat()));
        assertThat(jobSpecification.getCharset(), is(jobSpecificationTemplate.getCharset()));
        assertThat(jobSpecification.getDestination(), is(jobSpecificationTemplate.getDestination()));
        assertThat(jobSpecification.getSubmitterId(), is(jobSpecificationTemplate.getSubmitterId()));
    }

    private MarcExchangeRecordExpectation getMarcExchangeRecord(RecordId recordId) {
        return new MarcExchangeRecordExpectation(recordId.getBibliographicRecordId(), recordId.getAgencyId());
    }

    private String getRecordCreationDate(Record record) {
        return new SimpleDateFormat("yyyyMMdd").format(record.getCreated());
    }
}
