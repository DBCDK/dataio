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

import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.dataio.commons.types.AddiMetaData;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.utils.test.jndi.InMemoryInitialContextFactory;
import dk.dbc.dataio.harvester.task.TaskRepo;
import dk.dbc.dataio.harvester.task.entity.HarvestTask;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.HarvesterInvalidRecordException;
import dk.dbc.dataio.harvester.types.HarvesterSourceException;
import dk.dbc.dataio.harvester.types.RRHarvesterConfig;
import dk.dbc.dataio.harvester.utils.rawrepo.RawRepoConnector;
import dk.dbc.dataio.jsonb.JSONBContext;
import dk.dbc.dataio.jsonb.JSONBException;
import dk.dbc.rawrepo.QueueJob;
import dk.dbc.rawrepo.MockedQueueJob;
import dk.dbc.rawrepo.RecordServiceConnector;
import dk.dbc.rawrepo.RecordServiceConnectorException;
import dk.dbc.rawrepo.RecordData;
import dk.dbc.rawrepo.MockedRecord;
import dk.dbc.rawrepo.queue.ConfigurationException;
import dk.dbc.rawrepo.queue.QueueException;
import dk.dbc.rawrepo.RawRepoException;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import javax.naming.Context;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static dk.dbc.commons.testutil.Assert.assertThat;
import static dk.dbc.commons.testutil.Assert.isThrowing;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class HarvestOperationTest {
    public final static RecordData.RecordId DBC_RECORD_ID = new RecordData.RecordId("record", HarvestOperation.DBC_LIBRARY);
    public final static RecordData.RecordId RECORD_ID = new RecordData.RecordId("record", 710100);
    public final static String RECORD_CONTENT = getRecordContent(RECORD_ID);
    public final static RecordData RECORD = new MockedRecord(RECORD_ID, true);
    public final static QueueJob QUEUE_JOB = getQueueJob(RECORD_ID);
    public final static int AGENCY_ID = 424242;
    public static final String OPENAGENCY_ENDPOINT = "openagency.endpoint";

    static {
        RECORD.setContent(RECORD_CONTENT.getBytes(StandardCharsets.UTF_8));
    }

    final EntityManager entityManager = mock(EntityManager.class);
    final TaskRepo taskRepo = new TaskRepo(entityManager);
    final HarvesterJobBuilderFactory harvesterJobBuilderFactory = mock(HarvesterJobBuilderFactory.class);
    final HarvesterJobBuilder harvesterJobBuilder = mock(HarvesterJobBuilder.class);
    final RawRepoConnector rawRepoConnector = mock(RawRepoConnector.class);
    final RecordServiceConnector rawRepoRecordServiceConnector = mock(RecordServiceConnector.class);
    final JSONBContext jsonbContext = new JSONBContext();
    private final JobSpecification defaultJobSpecificationTemplate = getJobSpecification();

    @BeforeClass
    public static void setup() {
        // sets up the InMemoryInitialContextFactory as default factory.
        System.setProperty(Context.INITIAL_CONTEXT_FACTORY, InMemoryInitialContextFactory.class.getName());
    }

    @Before
    public void setupTest() throws RawRepoException, SQLException, RecordServiceConnectorException, HarvesterException {
        when(rawRepoConnector.dequeue(anyString()))
                .thenReturn(QUEUE_JOB)
                .thenReturn(null);
        when(rawRepoRecordServiceConnector.getRecordDataCollection(any(RecordData.RecordId.class)))
                .thenReturn(new HashMap<String, RecordData>() {{
                    put(RECORD_ID.getBibliographicRecordId(), RECORD);
                }});
        when(rawRepoRecordServiceConnector.getRecordData(any(RecordData.RecordId.class))).thenReturn(RECORD);
        when(harvesterJobBuilderFactory.newHarvesterJobBuilder(any(JobSpecification.class))).thenReturn(harvesterJobBuilder);
    }

    @Test
    public void constructor_noOpenAgencyTargetIsConfigured_throws() {
        final RRHarvesterConfig config = HarvesterTestUtil.getRRHarvesterConfig();
        assertThat(() -> new HarvestOperation(config,
            harvesterJobBuilderFactory, taskRepo, ""),
            isThrowing(IllegalArgumentException.class));
    }

    @Test
    public void execute_rawRepoConnectorDequeueThrowsSqlException_throws()
            throws SQLException, RawRepoException, QueueException, ConfigurationException, SQLException {
        when(rawRepoConnector.dequeue(anyString())).thenThrow(new SQLException());
        final HarvestOperation harvestOperation = newHarvestOperation();
        assertThat(() -> harvestOperation.execute(), isThrowing(HarvesterException.class));
    }

    @Test
    public void execute_rawRepoConnectorDequeueThrowsRawRepoException_throws()
            throws SQLException, RawRepoException, QueueException, ConfigurationException, SQLException {
        when(rawRepoConnector.dequeue(anyString())).thenThrow(new RawRepoException());
        final HarvestOperation harvestOperation = newHarvestOperation();
        assertThat(() -> harvestOperation.execute(), isThrowing(HarvesterException.class));
    }

    @Test
    public void execute_rawRepoConnectorFetchRecordCollectionThrowsSqlException_recordIsFailed()
            throws RecordServiceConnectorException, HarvesterException, QueueException, ConfigurationException, SQLException {
        when(rawRepoRecordServiceConnector.getRecordDataCollection(any(RecordData.RecordId.class))).thenThrow(new RecordServiceConnectorException("Record not found"));

        final HarvestOperation harvestOperation = newHarvestOperation();
        harvestOperation.execute();

        final ArgumentCaptor<AddiRecord> addiRecordCaptor = ArgumentCaptor.forClass(AddiRecord.class);
        verify(harvesterJobBuilder, times(1)).addRecord(addiRecordCaptor.capture());
        assertHasDiagnostic(addiRecordCaptor.getValue());
    }

    @Test
    public void execute_rawRepoConnectorFetchRecordThrowsSqlException_recordIsFailed()
            throws SQLException, RawRepoException, RecordServiceConnectorException, HarvesterException,
            QueueException, ConfigurationException {
        final QueueJob queueJob = getQueueJob(RECORD_ID);
        final RecordData record = new MockedRecord(RECORD_ID, true);
        record.setContent(getDeleteRecordContent(RECORD_ID).getBytes(StandardCharsets.UTF_8));
        record.setDeleted(true);

        when(rawRepoConnector.dequeue(anyString()))
                .thenReturn(queueJob)
                .thenReturn(null);

        when(rawRepoRecordServiceConnector.getRecordDataCollection(any(RecordData.RecordId.class)))
                .thenReturn(new HashMap<String, RecordData>() {{
                    put(RECORD_ID.getBibliographicRecordId(), record);
                }});

        when(rawRepoRecordServiceConnector.getRecordData(any(RecordData.RecordId.class))).thenThrow(new RecordServiceConnectorException("Record not found"));

        final HarvestOperation harvestOperation = newHarvestOperation();
        harvestOperation.execute();

        final ArgumentCaptor<AddiRecord> addiRecordCaptor = ArgumentCaptor.forClass(AddiRecord.class);
        verify(harvesterJobBuilder, times(1)).addRecord(addiRecordCaptor.capture());
        assertHasDiagnostic(addiRecordCaptor.getValue());
    }

    @Test
    public void execute_rawRepoConnectorFetchRecordThrowsRawRepoException_recordIsFailed()
            throws RawRepoException, RecordServiceConnectorException, HarvesterException, QueueException, ConfigurationException, SQLException {
        final QueueJob queueJob = getQueueJob(RECORD_ID);
        final RecordData record = new MockedRecord(RECORD_ID, true);
        record.setContent(getDeleteRecordContent(RECORD_ID).getBytes(StandardCharsets.UTF_8));
        record.setDeleted(true);

        when(rawRepoConnector.dequeue(anyString()))
                .thenReturn(queueJob)
                .thenReturn(null);

        when(rawRepoRecordServiceConnector.getRecordDataCollection(any(RecordData.RecordId.class), any(RecordServiceConnector.Params.class)))
                .thenReturn(new HashMap<String, RecordData>() {{
                    put(RECORD_ID.getBibliographicRecordId(), record);
                }});

        when(rawRepoRecordServiceConnector.getRecordData(any(RecordData.RecordId.class))).thenThrow(new RecordServiceConnectorException("Record not found"));

        final HarvestOperation harvestOperation = newHarvestOperation();
        harvestOperation.execute();

        final ArgumentCaptor<AddiRecord> addiRecordCaptor = ArgumentCaptor.forClass(AddiRecord.class);
        verify(harvesterJobBuilder, times(1)).addRecord(addiRecordCaptor.capture());
        assertHasDiagnostic(addiRecordCaptor.getValue());
    }

    @Test
    public void execute_rawRepoConnectorFetchRecordCollectionThrowsRawRepoException_recordIsFailed()
            throws RecordServiceConnectorException, HarvesterException, QueueException, ConfigurationException, SQLException {
        when(rawRepoRecordServiceConnector.getRecordDataCollection(any(RecordData.RecordId.class), any(RecordServiceConnector.Params.class))).thenThrow(new RecordServiceConnectorException("Record not found"));

        final HarvestOperation harvestOperation = newHarvestOperation();
        harvestOperation.execute();

        final ArgumentCaptor<AddiRecord> addiRecordCaptor = ArgumentCaptor.forClass(AddiRecord.class);
        verify(harvesterJobBuilder, times(1)).addRecord(addiRecordCaptor.capture());
        assertHasDiagnostic(addiRecordCaptor.getValue());
    }

    @Test
    public void execute_rawRepoConnectorFetchRecordCollectionThrowsMarcXMergerException_recordIsFailed()
            throws RecordServiceConnectorException, HarvesterException, QueueException, ConfigurationException, SQLException {
        when(rawRepoRecordServiceConnector.getRecordDataCollection(any(RecordData.RecordId.class), any(RecordServiceConnector.Params.class))).thenThrow(new RecordServiceConnectorException("Could not  merge records"));

        final HarvestOperation harvestOperation = newHarvestOperation();
        harvestOperation.execute();

        final ArgumentCaptor<AddiRecord> addiRecordCaptor = ArgumentCaptor.forClass(AddiRecord.class);
        verify(harvesterJobBuilder, times(1)).addRecord(addiRecordCaptor.capture());
        assertHasDiagnostic(addiRecordCaptor.getValue());
    }

    @Test
    public void execute_rawRepoRecordHasInvalidXmlContent_recordIsFailed()
            throws HarvesterException, RecordServiceConnectorException, QueueException, ConfigurationException, SQLException {
        final RecordData rrRecord = mock(RecordData.class);
        when(rawRepoRecordServiceConnector.getRecordDataCollection(any(RecordData.RecordId.class), any(RecordServiceConnector.Params.class)))
                .thenReturn(new HashMap<String, RecordData>() {{
                    put("ID", rrRecord);
                }});

        when(rrRecord.getContent()).thenReturn("invalid".getBytes());

        final HarvestOperation harvestOperation = newHarvestOperation();
        harvestOperation.execute();

        final ArgumentCaptor<AddiRecord> addiRecordCaptor = ArgumentCaptor.forClass(AddiRecord.class);
        verify(harvesterJobBuilder, times(1)).addRecord(addiRecordCaptor.capture());
        assertHasDiagnostic(addiRecordCaptor.getValue());
    }

    @Test
    public void execute_rawRepoRecordHasNoCreationDate_recordIsFailed()
            throws RecordServiceConnectorException, HarvesterException, QueueException, ConfigurationException, SQLException {
        final MockedRecord rrRecord = new MockedRecord(RECORD_ID, true);
        rrRecord.setCreated(null);
        rrRecord.setContent(getRecordContent(RECORD_ID).getBytes(StandardCharsets.UTF_8));
        when(rawRepoRecordServiceConnector.getRecordData(any(RecordData.RecordId.class))).thenReturn(rrRecord);

        final HarvestOperation harvestOperation = newHarvestOperation();
        harvestOperation.execute();

        final ArgumentCaptor<AddiRecord> addiRecordCaptor = ArgumentCaptor.forClass(AddiRecord.class);
        verify(harvesterJobBuilder, times(1)).addRecord(addiRecordCaptor.capture());
        assertHasDiagnostic(addiRecordCaptor.getValue());
    }

    @Test
    public void execute_rawRepoReturnsEmptyCollection_recordIsFailed()
            throws RecordServiceConnectorException, HarvesterException, QueueException, ConfigurationException, SQLException {
        when(rawRepoRecordServiceConnector.getRecordDataCollection(any(RecordData.RecordId.class), any(RecordServiceConnector.Params.class)))
                .thenReturn(new HashMap<>());

        final HarvestOperation harvestOperation = newHarvestOperation();
        harvestOperation.execute();

        final ArgumentCaptor<AddiRecord> addiRecordCaptor = ArgumentCaptor.forClass(AddiRecord.class);
        verify(harvesterJobBuilder, times(1)).addRecord(addiRecordCaptor.capture());
        assertHasDiagnostic(addiRecordCaptor.getValue());
    }

    @Test
    public void execute_rawRepoReturnsCollectionWithoutBibliographicRecordId_recordIsFailed()
            throws RecordServiceConnectorException, HarvesterException, QueueException, ConfigurationException, SQLException {
        final MockedRecord rrRecord = new MockedRecord(RECORD_ID, true);
        rrRecord.setContent(getRecordContent(RECORD_ID).getBytes(StandardCharsets.UTF_8));
        when(rawRepoRecordServiceConnector.getRecordDataCollection(any(RecordData.RecordId.class), any(RecordServiceConnector.Params.class)))
                .thenReturn(new HashMap<String, RecordData>() {{
                    put("unexpectedBibliographicRecordId", rrRecord);
                }});

        final HarvestOperation harvestOperation = newHarvestOperation();
        harvestOperation.execute();

        final ArgumentCaptor<AddiRecord> addiRecordCaptor = ArgumentCaptor.forClass(AddiRecord.class);
        verify(harvesterJobBuilder, times(1)).addRecord(addiRecordCaptor.capture());
        assertHasDiagnostic(addiRecordCaptor.getValue());
    }

    @Test
    public void execute_harvesterJobBuilderThrowsHarvesterException_throws()
            throws HarvesterException, QueueException, ConfigurationException, SQLException{
        when(harvesterJobBuilder.build()).thenThrow(new HarvesterException("DIED"));

        final HarvestOperation harvestOperation = newHarvestOperation();
        assertThat(() -> harvestOperation.execute(), isThrowing(HarvesterException.class));
    }

    @Test
    public void execute_rawRepoDeleteRecordHasAgencyIdContainedInExcludedSet_recordIsProcessed()
            throws RawRepoException, SQLException, RecordServiceConnectorException, HarvesterException,
            QueueException, ConfigurationException, SQLException {
        final RecordData.RecordId recordId = new RecordData.RecordId("record", 870970);
        final QueueJob queueJob = getQueueJob(recordId);
        final RecordData record = new MockedRecord(recordId, true);
        record.setContent(getDeleteRecordContent(recordId).getBytes(StandardCharsets.UTF_8));
        record.setDeleted(true);

        when(rawRepoConnector.dequeue(anyString()))
                .thenReturn(queueJob)
                .thenReturn(null);

        when(rawRepoRecordServiceConnector.getRecordDataCollection(any(RecordData.RecordId.class)))
                .thenReturn(new HashMap<String, RecordData>() {{
                    put(recordId.getBibliographicRecordId(), record);
                }});

        when(rawRepoRecordServiceConnector.getRecordData(any(RecordData.RecordId.class))).thenReturn(record);

        final HarvestOperation harvestOperation = newHarvestOperation();
        assertThat(harvestOperation.execute(), is(1));
        verify(rawRepoRecordServiceConnector, times(1)).getRecordData(any(RecordData.RecordId.class));
        verify(rawRepoRecordServiceConnector, times(1)).getRecordDataCollection(any(RecordData.RecordId.class), any(RecordServiceConnector.Params.class));
    }

    @Test
    public void execute_rawRepoDeleteRecordHasDbcId_recordIsSkipped()
            throws RawRepoException, SQLException, RecordServiceConnectorException, HarvesterException,
            QueueException, ConfigurationException, SQLException {
        final QueueJob queueJob = getQueueJob(DBC_RECORD_ID);
        final RecordData record = new MockedRecord(DBC_RECORD_ID, true);
        record.setContent(getDeleteRecordContent(DBC_RECORD_ID).getBytes(StandardCharsets.UTF_8));
        record.setDeleted(true);

        when(rawRepoConnector.dequeue(anyString()))
                .thenReturn(queueJob)
                .thenReturn(null);

        when(rawRepoRecordServiceConnector.getRecordDataCollection(any(RecordData.RecordId.class)))
                .thenReturn(new HashMap<String, RecordData>() {{
                    put(DBC_RECORD_ID.getBibliographicRecordId(), record);
                }});

        when(rawRepoRecordServiceConnector.getRecordData(any(RecordData.RecordId.class))).thenReturn(record);

        final HarvestOperation harvestOperation = newHarvestOperation();
        harvestOperation.execute();
        verify(rawRepoRecordServiceConnector, times(1)).getRecordData(any(RecordData.RecordId.class));
        verify(rawRepoRecordServiceConnector, times(0)).getRecordDataCollection(any(RecordData.RecordId.class), any(RecordServiceConnector.Params.class));
    }

    @Test
    public void getJobSpecificationTemplate_interpolatesConfigValues()
            throws QueueException, ConfigurationException, SQLException {
        final RRHarvesterConfig config = HarvesterTestUtil.getRRHarvesterConfig();
        final JobSpecification expectedJobSpecificationTemplate = defaultJobSpecificationTemplate
                .withAncestry(new JobSpecification.Ancestry().withHarvesterToken(config.getHarvesterToken()));

        config.getContent()
                .withConsumerId("consumerId")
                .withFormat(expectedJobSpecificationTemplate.getFormat())
                .withDestination(expectedJobSpecificationTemplate.getDestination());
        final HarvestOperation harvestOperation = newHarvestOperation(config);

        assertThat(harvestOperation.getJobSpecificationTemplate(AGENCY_ID), is(expectedJobSpecificationTemplate));
    }

    @Test
    public void getJobSpecificationTemplate_interpolatesConfigWithFormatOverrides()
            throws QueueException, ConfigurationException, SQLException {
        final String consumerId = "rrConsumer";
        final String formatOverride = "alternativeFormat";

        final RRHarvesterConfig config = HarvesterTestUtil.getRRHarvesterConfig();

        final JobSpecification expectedJobSpecificationTemplate = defaultJobSpecificationTemplate
                .withFormat(formatOverride)
                .withAncestry(new JobSpecification.Ancestry().withHarvesterToken(config.getHarvesterToken()));

        config.getContent()
                .withConsumerId(consumerId)
                .withDestination(expectedJobSpecificationTemplate.getDestination())
                .withFormat("format")
                .withFormatOverridesEntry(AGENCY_ID, formatOverride);
        final HarvestOperation harvestOperation = newHarvestOperation(config);

        assertThat(harvestOperation.getJobSpecificationTemplate(AGENCY_ID), is(expectedJobSpecificationTemplate));
    }

    @Test
    public void getJobSpecificationTemplate_harvestOperationConfigJobTypeIsSetToTransientAsDefault()
            throws QueueException, ConfigurationException, SQLException{
        final RRHarvesterConfig config = HarvesterTestUtil.getRRHarvesterConfig();
        final HarvestOperation harvestOperation = newHarvestOperation(config);
        assertThat(harvestOperation.getJobSpecificationTemplate(AGENCY_ID).getType(), is(JobSpecification.Type.TRANSIENT));
    }

    @Test
    public void getJobSpecificationTemplate_harvestOperationConfigJobTypeCanBeChangedFromDefault()
        throws QueueException, ConfigurationException, SQLException {
        final RRHarvesterConfig config = HarvesterTestUtil.getRRHarvesterConfig();
        config.getContent().withType(JobSpecification.Type.TEST);
        final HarvestOperation harvestOperation = newHarvestOperation(config);
        assertThat(harvestOperation.getJobSpecificationTemplate(AGENCY_ID).getType(), is(JobSpecification.Type.TEST));
    }

    @Test
    public void getRawRepoConnector_configuresRelationHints()
            throws QueueException, ConfigurationException, SQLException {
        try {
            final RRHarvesterConfig config = HarvesterTestUtil.getRRHarvesterConfig();
            InMemoryInitialContextFactory.bind(config.getContent().getResource(), mock(DataSource.class));

            final HarvestOperation harvestOperation = newHarvestOperation(config);
            final RawRepoConnector rawRepoConnector = harvestOperation.getRawRepoConnector(config);
            assertThat(rawRepoConnector.getRelationHints(), is(notNullValue()));
        } finally {
            InMemoryInitialContextFactory.clear();
        }
    }

    @Test
    public void getAgencyId_DBC_enrichmentTrailArgIsNull_throws() throws QueueException, ConfigurationException, SQLException {
        final MockedRecord record = new MockedRecord(DBC_RECORD_ID, true);
        record.setEnrichmentTrail(null);
        final HarvestOperation harvestOperation = newHarvestOperation();
        assertThat(() -> harvestOperation.getAgencyIdFromEnrichmentTrail(record), isThrowing(HarvesterInvalidRecordException.class));
    }

    @Test
    public void getAgencyId_DBC_enrichmentTrailArgIsEmpty_throws() throws QueueException, ConfigurationException, SQLException {
        final MockedRecord record = new MockedRecord(DBC_RECORD_ID, true);
        record.setEnrichmentTrail(" ");
        final HarvestOperation harvestOperation = newHarvestOperation();
        assertThat(() -> harvestOperation.getAgencyIdFromEnrichmentTrail(record), isThrowing(HarvesterInvalidRecordException.class));
    }

    @Test
    public void getAgencyId_DBC_no870TrailFound_throws() throws QueueException, ConfigurationException, SQLException {
        final MockedRecord record = new MockedRecord(DBC_RECORD_ID, true);
        record.setEnrichmentTrail("191919,123456");
        final HarvestOperation harvestOperation = newHarvestOperation();
        assertThat(() -> harvestOperation.getAgencyIdFromEnrichmentTrail(record), isThrowing(HarvesterInvalidRecordException.class));
    }

    @Test
    public void getAgencyId_DBC_invalid870TrailFound_throws() throws QueueException, ConfigurationException, SQLException {
        final MockedRecord record = new MockedRecord(DBC_RECORD_ID, true);
        record.setEnrichmentTrail("191919,870abc");
        final HarvestOperation harvestOperation = newHarvestOperation();
        assertThat(() -> harvestOperation.getAgencyIdFromEnrichmentTrail(record), isThrowing(HarvesterInvalidRecordException.class));
    }

    @Test
    public void getAgencyId_DBC_returnsAgencyIdFromEnrichmentTrail()
            throws HarvesterInvalidRecordException, QueueException, ConfigurationException, SQLException {
        final MockedRecord record = new MockedRecord(DBC_RECORD_ID, true);
        record.setEnrichmentTrail("191919,870970");
        final HarvestOperation harvestOperation = newHarvestOperation();
        assertThat(harvestOperation.getAgencyIdFromEnrichmentTrail(record), is(870970));
    }

    @Test
    public void execute_whenRawRepoQueueIsEmpty_fallsBackToTaskQueue()
            throws RawRepoException, SQLException, HarvesterException, QueueException, ConfigurationException {
        final TypedQuery<HarvestTask> query = mock(TypedQuery.class);
        when(entityManager.createNamedQuery(HarvestTask.QUERY_FIND_NEXT, HarvestTask.class)).thenReturn(query);
        when(query.setParameter(eq("configId"), anyInt())).thenReturn(query);
        when(query.setMaxResults(1)).thenReturn(query);
        when(query.getResultList()).thenReturn(Collections.emptyList());
        when(rawRepoConnector.dequeue(anyString()))
                .thenReturn(null);

        final HarvestOperation harvestOperation = newHarvestOperation();
        harvestOperation.execute();

        verify(entityManager).createNamedQuery(HarvestTask.QUERY_FIND_NEXT, HarvestTask.class);
    }

    @Test
    public void fetchRecord_returnRecord() throws RecordServiceConnectorException,
            SQLException, ConfigurationException, QueueException,
            HarvesterInvalidRecordException, HarvesterSourceException {

        final RecordData.RecordId EXPECTED_RECORD_ID = new RecordData.RecordId("expected", HarvestOperation.DBC_LIBRARY);
        final MockedRecord EXPECTED_RECORD = new MockedRecord(EXPECTED_RECORD_ID);
        EXPECTED_RECORD.setContent(getRecordContent(EXPECTED_RECORD_ID).getBytes(StandardCharsets.UTF_8));
        EXPECTED_RECORD.setEnrichmentTrail("191919,870970");
        EXPECTED_RECORD.setTrackingId("tracking id");
        EXPECTED_RECORD.setCreated(Instant.now().toString());

        when(rawRepoRecordServiceConnector.getRecordData(any(RecordData.RecordId.class)))
                .thenReturn(EXPECTED_RECORD);

        HarvestOperation harvestOperation = newHarvestOperation();
        RecordData fetched = harvestOperation.fetchRecord(EXPECTED_RECORD_ID);

        assertThat(fetched, notNullValue());
        assertThat(fetched.getRecordId(), is(EXPECTED_RECORD.getRecordId()));
        assertThat(fetched.getContent(), is(EXPECTED_RECORD.getContent()));
    }

    @Test
    public void fetchRecordCollection_returnRecordCollection() throws RecordServiceConnectorException,
            SQLException, ConfigurationException, QueueException,
            HarvesterInvalidRecordException, HarvesterSourceException {

        final RecordData.RecordId EXPECTED_RECORD_ID = new RecordData.RecordId("expected", HarvestOperation.DBC_LIBRARY);
        final MockedRecord EXPECTED_RECORD = new MockedRecord(EXPECTED_RECORD_ID);
        EXPECTED_RECORD.setContent(getRecordContent(EXPECTED_RECORD_ID).getBytes(StandardCharsets.UTF_8));
        EXPECTED_RECORD.setEnrichmentTrail("191919,870970");
        EXPECTED_RECORD.setTrackingId("tracking id");
        EXPECTED_RECORD.setCreated(Instant.now().toString());
        final HashMap<String, RecordData> EXPECTED_RECORD_COLLECTION = new HashMap<>();
        EXPECTED_RECORD_COLLECTION.put(EXPECTED_RECORD_ID.getBibliographicRecordId(), EXPECTED_RECORD);

        when(rawRepoRecordServiceConnector.getRecordDataCollection(any(RecordData.RecordId.class), any(RecordServiceConnector.Params.class)))
                .thenReturn(EXPECTED_RECORD_COLLECTION);

        HarvestOperation harvestOperation = newHarvestOperation();
        Map<String, RecordData> fetched = harvestOperation.fetchRecordCollection(EXPECTED_RECORD_ID);

        assertThat(fetched, notNullValue());
        assertThat(fetched.size(), is(1));
        assertThat(fetched.containsKey(EXPECTED_RECORD_ID.getBibliographicRecordId()), is(true));
        assertThat(fetched.get(EXPECTED_RECORD_ID.getBibliographicRecordId()).getRecordId(), is(EXPECTED_RECORD.getRecordId()));
        assertThat(fetched.get(EXPECTED_RECORD_ID.getBibliographicRecordId()).getContent(), is(EXPECTED_RECORD.getContent()));
    }

    public HarvestOperation newHarvestOperation(RRHarvesterConfig config)
            throws QueueException, ConfigurationException, SQLException {
        return new HarvestOperation(config, harvesterJobBuilderFactory,
            taskRepo, new AgencyConnection(OPENAGENCY_ENDPOINT),
            rawRepoConnector, rawRepoRecordServiceConnector);
    }

    public HarvestOperation newHarvestOperation() throws QueueException, ConfigurationException, SQLException {
        return newHarvestOperation(HarvesterTestUtil.getRRHarvesterConfig());
    }

    private void assertHasDiagnostic(AddiRecord addiRecord) {
        assertThat("Addi record", addiRecord, is(notNullValue()));
        try {
            final AddiMetaData addiMetaData = jsonbContext.unmarshall(
                    new String(addiRecord.getMetaData(), StandardCharsets.UTF_8), AddiMetaData.class);
            assertThat("Addi record metadata", addiMetaData, is(notNullValue()));
            assertThat("Addi record metadata has diagnostic", addiMetaData.diagnostic(), is(notNullValue()));
        } catch (JSONBException e) {
            throw new IllegalStateException(e);
        }
    }

    private JobSpecification getJobSpecification() {
        return new JobSpecification()
                .withPackaging("addi-xml")
                .withCharset("utf8")
                .withFormat("katalog")
                .withDestination("destination")
                .withSubmitterId(AGENCY_ID)
                .withType(JobSpecification.Type.TRANSIENT);
    }

    public static QueueJob getQueueJob(RecordData.RecordId recordId, Date queued) {
        return new MockedQueueJob(recordId.getBibliographicRecordId(), recordId.getAgencyId(), "QUEUE_ID",
                new Timestamp(queued.getTime()));
    }

    public static QueueJob getQueueJob(RecordData.RecordId recordId) {
        return getQueueJob(recordId, new Date());
    }

    public static String getRecordContent(RecordData.RecordId recordId) {
        return
        "<marcx:collection xmlns:marcx=\"info:lc/xmlns/marcxchange-v1\">" +
            "<marcx:record format=\"danMARC2\">" +
                "<marcx:leader>00000n 2200000 4500</marcx:leader>" +
                "<marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"001\">" +
                    "<marcx:subfield code=\"a\">" + recordId.getBibliographicRecordId() + "</marcx:subfield>" +
                    "<marcx:subfield code=\"b\">" + recordId.getAgencyId() + "</marcx:subfield>" +
                "</marcx:datafield>" +
                "<marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"245\">" +
                    "<marcx:subfield code=\"a\">title</marcx:subfield>" +
                "</marcx:datafield>" +
            "</marcx:record>" +
        "</marcx:collection>";
    }

    public static String getDeleteRecordContent(RecordData.RecordId recordId) {
        return
        "<marcx:collection xmlns:marcx=\"info:lc/xmlns/marcxchange-v1\">" +
            "<marcx:record format=\"danMARC2\">" +
                "<marcx:leader>00000n 2200000 4500</marcx:leader>" +
                "<marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"001\">" +
                    "<marcx:subfield code=\"a\">" + recordId.getBibliographicRecordId() + "</marcx:subfield>" +
                    "<marcx:subfield code=\"b\">" + recordId.getAgencyId() + "</marcx:subfield>" +
                "</marcx:datafield>" +
                "<marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"004\">" +
                    "<marcx:subfield code=\"r\">d</marcx:subfield>" +
                "</marcx:datafield>" +
            "</marcx:record>" +
        "</marcx:collection>";
    }
}
