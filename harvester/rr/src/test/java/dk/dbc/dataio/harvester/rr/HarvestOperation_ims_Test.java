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

import dk.dbc.dataio.bfs.api.BinaryFileStoreFsImpl;
import dk.dbc.dataio.commons.types.AddiMetaData;
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
import dk.dbc.dataio.harvester.utils.holdingsitems.HoldingsItemsConnector;
import dk.dbc.dataio.harvester.utils.rawrepo.RawRepoConnector;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.rawrepo.MockedRecord;
import dk.dbc.rawrepo.RecordData;
import dk.dbc.rawrepo.RecordId;
import dk.dbc.rawrepo.RecordServiceConnector;
import dk.dbc.rawrepo.RecordServiceConnectorException;
import dk.dbc.rawrepo.queue.ConfigurationException;
import dk.dbc.rawrepo.queue.QueueException;
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
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HarvestOperation_ims_Test {
    private static final Date QUEUED_TIME = new Date(1467277697583L); // 2016-06-30 11:08:17.583
    private static final String CONSUMER_ID = "consumerId";
    private static final int IMS_LIBRARY = 775100;

    private final EntityManager entityManager = mock(EntityManager.class);
    private final TaskRepo taskRepo = new TaskRepo(entityManager);
    private final RawRepoConnector rawRepoConnector = mock(RawRepoConnector.class);
    final RecordServiceConnector rawRepoRecordServiceConnector = mock(RecordServiceConnector.class);
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

    private final AddiFileVerifier addiFileVerifier = new AddiFileVerifier();

    @Rule
    public TemporaryFolder tmpFolder = new TemporaryFolder();

    @Before
    public void setupMocks() throws IOException, HarvesterException {
        // Mock agency-connection and holdings-items lookup
        final Set<Integer> imsLibraries = Stream.of(710100, 737000, 775100, 785100).collect(Collectors.toSet());
        final Set<Integer> hasHoldingsResponse = new LinkedHashSet<>();
        hasHoldingsResponse.add(710100);
        hasHoldingsResponse.add(737000);
        hasHoldingsResponse.add(123456);
        when(holdingsItemsConnector.hasHoldings("dbc", imsLibraries))
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
    public void harvest_multipleAgencyIdsHarvested_agencyIdsInSeparateJobs()
            throws SQLException, RecordServiceConnectorException, HarvesterException, QueueException {

        final RecordId dbcRecordId = new RecordId("dbc", HarvestOperation.DBC_LIBRARY);
        final MockedRecord dbcRecord = new MockedRecord(dbcRecordId);
        dbcRecord.setContent(HarvestOperationTest.getRecordContent(dbcRecordId).getBytes(StandardCharsets.UTF_8));
        dbcRecord.setEnrichmentTrail("191919,870970");
        dbcRecord.setTrackingId("tracking id");

        final RecordId dbcHeadRecordId = new RecordId("dbc-head", HarvestOperation.DBC_LIBRARY);
        final RecordData dbcHeadRecord = new MockedRecord(dbcHeadRecordId);
        dbcHeadRecord.setContent(HarvestOperationTest.getRecordContent(dbcHeadRecordId).getBytes(StandardCharsets.UTF_8));

        final RecordId dbcSectionRecordId = new RecordId("dbc-section", HarvestOperation.DBC_LIBRARY);
        final RecordData dbcSectionRecord = new MockedRecord(dbcSectionRecordId);
        dbcSectionRecord.setContent(HarvestOperationTest.getRecordContent(dbcSectionRecordId).getBytes(StandardCharsets.UTF_8));

        final RecordId imsRecordId = new RecordId("ims", IMS_LIBRARY);
        final RecordData imsRecord = new MockedRecord(imsRecordId);
        imsRecord.setContent(HarvestOperationTest.getRecordContent(imsRecordId).getBytes(StandardCharsets.UTF_8));

        // Mock rawrepo return values
        when(rawRepoConnector.dequeue(CONSUMER_ID))
                .thenReturn(HarvestOperationTest.getQueueItem(dbcRecordId, QUEUED_TIME))
                .thenReturn(HarvestOperationTest.getQueueItem(imsRecordId, QUEUED_TIME))
                .thenReturn(null);

        when(rawRepoRecordServiceConnector.getRecordDataCollectionDataIO(any(RecordId.class), any(RecordServiceConnector.Params.class)))
                .thenReturn(new HashMap<String, RecordData>() {{
                    put(dbcHeadRecordId.getBibliographicRecordId(), dbcHeadRecord);
                    put(dbcSectionRecordId.getBibliographicRecordId(), dbcSectionRecord);
                    put(dbcRecordId.getBibliographicRecordId(), dbcRecord);
                }})
                .thenReturn(new HashMap<String, RecordData>() {{
                    put(dbcHeadRecordId.getBibliographicRecordId(), dbcHeadRecord);
                    put(dbcSectionRecordId.getBibliographicRecordId(), dbcSectionRecord);
                    put(dbcRecordId.getBibliographicRecordId(), dbcRecord);
                }})
                .thenReturn(new HashMap<String, RecordData>(){{
                    put(imsRecordId.getBibliographicRecordId(), imsRecord);
                }});

        when(rawRepoRecordServiceConnector.recordFetch(any(RecordId.class)))
                .thenReturn(dbcRecord)
                .thenReturn(dbcRecord)
                .thenReturn(dbcRecord)
                .thenReturn(dbcRecord)
                .thenReturn(imsRecord)
                .thenReturn(imsRecord);

        // Setup harvester datafile content expectations
        final MarcExchangeCollectionExpectation marcExchangeCollectionExpectation710100 = new MarcExchangeCollectionExpectation();
        marcExchangeCollectionExpectation710100.records.add(getMarcExchangeRecord(dbcHeadRecordId));
        marcExchangeCollectionExpectation710100.records.add(getMarcExchangeRecord(dbcSectionRecordId));
        marcExchangeCollectionExpectation710100.records.add(getMarcExchangeRecord(dbcRecordId));
        recordsExpectationsFor710100.add(marcExchangeCollectionExpectation710100);
        addiMetaDataExpectationsFor710100.add(new AddiMetaData()
                .withBibliographicRecordId(dbcRecord.getRecordId().getBibliographicRecordId())
                .withSubmitterNumber(710100)
                .withFormat("katalog")
                .withCreationDate(Date.from(Instant.parse(dbcRecord.getCreated())))
                .withEnrichmentTrail(dbcRecord.getEnrichmentTrail())
                .withTrackingId(dbcRecord.getTrackingId())
                .withDeleted(false)
                .withLibraryRules(new AddiMetaData.LibraryRules()));

        final MarcExchangeCollectionExpectation marcExchangeCollectionExpectation737000 = new MarcExchangeCollectionExpectation();
        marcExchangeCollectionExpectation737000.records.add(getMarcExchangeRecord(dbcHeadRecordId));
        marcExchangeCollectionExpectation737000.records.add(getMarcExchangeRecord(dbcSectionRecordId));
        marcExchangeCollectionExpectation737000.records.add(getMarcExchangeRecord(dbcRecordId));
        recordsExpectationsFor737000.add(marcExchangeCollectionExpectation737000);
        addiMetaDataExpectationsFor737000.add(new AddiMetaData()
                .withBibliographicRecordId(dbcRecord.getRecordId().getBibliographicRecordId())
                .withSubmitterNumber(737000)
                .withFormat("katalog")
                .withCreationDate(Date.from(Instant.parse(dbcRecord.getCreated())))
                .withEnrichmentTrail(dbcRecord.getEnrichmentTrail())
                .withTrackingId(dbcRecord.getTrackingId())
                .withDeleted(false)
                .withLibraryRules(new AddiMetaData.LibraryRules()));

        final MarcExchangeCollectionExpectation marcExchangeCollectionExpectation775100 = new MarcExchangeCollectionExpectation();
        marcExchangeCollectionExpectation775100.records.add(getMarcExchangeRecord(imsRecordId));
        recordsExpectationsFor775100.add(marcExchangeCollectionExpectation775100);
        addiMetaDataExpectationsFor775100.add(new AddiMetaData()
                .withBibliographicRecordId(imsRecord.getRecordId().getBibliographicRecordId())
                .withSubmitterNumber(775100)
                .withFormat("katalog")
                .withCreationDate(Date.from(Instant.parse(imsRecord.getCreated())))
                .withEnrichmentTrail(imsRecord.getEnrichmentTrail())
                .withTrackingId(imsRecord.getTrackingId())
                .withDeleted(false)
                .withLibraryRules(new AddiMetaData.LibraryRules()));

        final ImsHarvestOperation harvestOperation = newImsHarvestOperation();
        harvestOperation.execute();

        addiFileVerifier.verify(harvesterDataFileWith710100, addiMetaDataExpectationsFor710100, recordsExpectationsFor710100);
        addiFileVerifier.verify(harvesterDataFileWith737000, addiMetaDataExpectationsFor737000, recordsExpectationsFor737000);
        addiFileVerifier.verify(harvesterDataFileWith775100, addiMetaDataExpectationsFor775100, recordsExpectationsFor775100);
        verifyJobSpecification(mockedJobStoreServiceConnector.jobInputStreams.remove().getJobSpecification(),
                newImsHarvestOperation().getJobSpecificationTemplate(710100));
        verifyJobSpecification(mockedJobStoreServiceConnector.jobInputStreams.remove().getJobSpecification(),
                newImsHarvestOperation().getJobSpecificationTemplate(737000));
        verifyJobSpecification(mockedJobStoreServiceConnector.jobInputStreams.remove().getJobSpecification(),
                newImsHarvestOperation().getJobSpecificationTemplate(775100));
    }

    @Test
    public void imsRecordIsDeleted_870970RecordUsedInstead()
            throws SQLException, RecordServiceConnectorException, HarvesterException, QueueException {
        final RecordId imsRecordId = new RecordId("faust", IMS_LIBRARY);
        final RecordData imsRecord = new MockedRecord(imsRecordId);
        imsRecord.setContent(HarvestOperationTest.getRecordContent(imsRecordId).getBytes(StandardCharsets.UTF_8));
        imsRecord.setDeleted(true);

        final RecordId recordId191919 = new RecordId("faust", 191919);
        final RecordId dbcRecordId = new RecordId("faust", 870970);
        final RecordData dbcRecord = new MockedRecord(dbcRecordId);
        dbcRecord.setContent(HarvestOperationTest.getRecordContent(recordId191919).getBytes(StandardCharsets.UTF_8));

        final HashSet<Integer> hasHoldings = new HashSet<>(Collections.singletonList(IMS_LIBRARY));
        when(holdingsItemsConnector.hasHoldings("faust", hasHoldings))
                .thenReturn(hasHoldings);

        // Mock rawrepo return values
        when(rawRepoConnector.dequeue(CONSUMER_ID))
                .thenReturn(HarvestOperationTest.getQueueItem(imsRecordId, QUEUED_TIME))
                .thenReturn(null);

        when(rawRepoRecordServiceConnector.getRecordDataCollectionDataIO(eq(imsRecordId), any(RecordServiceConnector.Params.class)))
                .thenReturn(new HashMap<String, RecordData>(){{
                    put(imsRecordId.getBibliographicRecordId(), imsRecord);
                }});
        when(rawRepoRecordServiceConnector.getRecordDataCollectionDataIO(eq(dbcRecordId), any(RecordServiceConnector.Params.class)))
                .thenReturn(new HashMap<String, RecordData>(){{
                    put(dbcRecordId.getBibliographicRecordId(), dbcRecord);
                }});

        when(rawRepoRecordServiceConnector.recordFetch(imsRecordId))
                .thenReturn(imsRecord);
        when(rawRepoRecordServiceConnector.recordFetch(dbcRecordId))
                .thenReturn(dbcRecord);

        when(rawRepoRecordServiceConnector.recordExists(dbcRecordId.getAgencyId(), imsRecordId.getBibliographicRecordId()))
                .thenReturn(true);

        mockedFileStoreServiceConnector = new MockedFileStoreServiceConnector();
        mockedFileStoreServiceConnector.destinations.add(harvesterDataFileWith775100.toPath());

        final MarcExchangeCollectionExpectation marcExchangeCollectionExpectation775100 = new MarcExchangeCollectionExpectation();
        marcExchangeCollectionExpectation775100.records.add(getMarcExchangeRecord(recordId191919));
        recordsExpectationsFor775100.add(marcExchangeCollectionExpectation775100);
        addiMetaDataExpectationsFor775100.add(new AddiMetaData()
                .withBibliographicRecordId(dbcRecord.getRecordId().getBibliographicRecordId())
                .withSubmitterNumber(775100)
                .withFormat("katalog")
                .withCreationDate(Date.from(Instant.parse(dbcRecord.getCreated())))
                .withEnrichmentTrail(dbcRecord.getEnrichmentTrail())
                .withTrackingId(dbcRecord.getTrackingId())
                .withDeleted(false)
                .withLibraryRules(new AddiMetaData.LibraryRules()));

        final ImsHarvestOperation harvestOperation = newImsHarvestOperation();
        harvestOperation.execute();

        addiFileVerifier.verify(harvesterDataFileWith775100, addiMetaDataExpectationsFor775100, recordsExpectationsFor775100);
        verifyJobSpecification(mockedJobStoreServiceConnector.jobInputStreams.remove().getJobSpecification(),
                newImsHarvestOperation().getJobSpecificationTemplate(775100));
    }

    @Test
    public void imsRecordIsDeletedAndNoHoldingExists_recordIsSkipped()
            throws SQLException, RecordServiceConnectorException, HarvesterException, QueueException {
        final RecordId imsRecordId = new RecordId("faust", IMS_LIBRARY);
        final RecordData imsRecord = new MockedRecord(imsRecordId);
        imsRecord.setContent(HarvestOperationTest.getRecordContent(imsRecordId).getBytes(StandardCharsets.UTF_8));
        imsRecord.setDeleted(true);

        final RecordId recordId191919 = new RecordId("faust", 191919);
        final RecordId dbcRecordId = new RecordId("faust", 870970);
        final RecordData dbcRecord = new MockedRecord(dbcRecordId);
        dbcRecord.setContent(HarvestOperationTest.getRecordContent(recordId191919).getBytes(StandardCharsets.UTF_8));

        when(holdingsItemsConnector.hasHoldings("faust", new HashSet<>(Collections.singletonList(IMS_LIBRARY))))
                .thenReturn(Collections.emptySet());

        // Mock rawrepo return values
        when(rawRepoConnector.dequeue(CONSUMER_ID))
                .thenReturn(HarvestOperationTest.getQueueItem(imsRecordId, QUEUED_TIME))
                .thenReturn(null);

        when(rawRepoRecordServiceConnector.recordFetch(imsRecordId))
                .thenReturn(imsRecord);

        mockedFileStoreServiceConnector = new MockedFileStoreServiceConnector();
        mockedFileStoreServiceConnector.destinations.add(harvesterDataFileWith775100.toPath());

        final ImsHarvestOperation harvestOperation = newImsHarvestOperation();
        harvestOperation.execute();

        addiFileVerifier.verify(harvesterDataFileWith775100, addiMetaDataExpectationsFor775100, recordsExpectationsFor775100);
        assertThat("Number of job created", mockedJobStoreServiceConnector.jobInputStreams.size(), is(0));
    }

    private ImsHarvestOperation newImsHarvestOperation() {
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
                .withIncludeRelations(true)
                .withHarvesterType(RRHarvesterConfig.HarvesterType.IMS);
        try {
            return new ImsHarvestOperation(config, harvesterJobBuilderFactory, taskRepo,
                    agencyConnection, rawRepoConnector, holdingsItemsConnector,
                    rawRepoRecordServiceConnector);
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

    private MarcExchangeRecordExpectation getMarcExchangeRecord(RecordId recordId) {
        return new MarcExchangeRecordExpectation(recordId.getBibliographicRecordId(), recordId.getAgencyId());
    }
}
