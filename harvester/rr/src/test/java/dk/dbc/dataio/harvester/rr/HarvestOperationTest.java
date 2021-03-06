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
import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.commons.types.AddiMetaData;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.harvester.task.TaskRepo;
import dk.dbc.dataio.harvester.task.entity.HarvestTask;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.HarvesterInvalidRecordException;
import dk.dbc.dataio.harvester.types.HarvesterSourceException;
import dk.dbc.dataio.harvester.types.RRHarvesterConfig;
import dk.dbc.dataio.harvester.utils.rawrepo.RawRepoConnector;
import dk.dbc.rawrepo.MockedQueueItem;
import dk.dbc.rawrepo.dto.RecordDTO;
import dk.dbc.rawrepo.dto.RecordIdDTO;
import dk.dbc.rawrepo.queue.ConfigurationException;
import dk.dbc.rawrepo.queue.QueueException;
import dk.dbc.rawrepo.queue.QueueItem;
import dk.dbc.rawrepo.record.RecordServiceConnector;
import dk.dbc.rawrepo.record.RecordServiceConnectorException;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.Tag;
import org.eclipse.microprofile.metrics.Timer;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class HarvestOperationTest {
    public static final RecordIdDTO DBC_RECORD_ID = new RecordIdDTO("record", HarvestOperation.DBC_LIBRARY);
    public static final RecordIdDTO RECORD_ID = new RecordIdDTO("record", 710100);
    public static final String RECORD_CONTENT = getRecordContent(RECORD_ID);
    public static final RecordDTO RECORD = new RecordDTO();
    public static final QueueItem QUEUE_ITEM = getQueueItem(RECORD_ID);
    public static final int AGENCY_ID = 424242;
    public final static VipCoreConnection VIP_CORE_CONNECTION = mock(VipCoreConnection.class);
    public static final MetricRegistry metricRegistry = mock(MetricRegistry.class);
    private final Timer timer = mock(Timer.class);
    private final Counter counter = mock(Counter.class);

    static {
        RECORD.setRecordId(RECORD_ID);
        RECORD.setDeleted(false);
        RECORD.setCreated(Instant.now().toString());
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

    @Before
    public void setupTest() throws SQLException, RecordServiceConnectorException, HarvesterException, QueueException {
        when(rawRepoConnector.dequeue(anyString()))
                .thenReturn(QUEUE_ITEM)
                .thenReturn(null);
        when(rawRepoRecordServiceConnector.getRecordDataCollectionDataIO(any(RecordIdDTO.class)))
                .thenReturn(new HashMap<String, RecordDTO>() {{
                    put(RECORD_ID.getBibliographicRecordId(), RECORD);
                }});
        when(rawRepoRecordServiceConnector.recordFetch(any(RecordIdDTO.class))).thenReturn(RECORD);
        when(harvesterJobBuilderFactory.newHarvesterJobBuilder(any(JobSpecification.class))).thenReturn(harvesterJobBuilder);
        when(metricRegistry.timer(any(Metadata.class), any(Tag.class))).thenReturn(timer);
        when(metricRegistry.counter(any(Metadata.class), any(Tag.class))).thenReturn(counter);
        doNothing().when(timer).update(anyLong(), any());
        doNothing().when(counter).inc();
    }

    @Test
    public void constructor_noOpenAgencyTargetIsConfigured_throws() {
        final RRHarvesterConfig config = HarvesterTestUtil.getRRHarvesterConfig();
        assertThat(() -> new HarvestOperation(config,
            harvesterJobBuilderFactory, taskRepo, null, metricRegistry),
            isThrowing(IllegalArgumentException.class));
    }

    @Test
    public void execute_rawRepoConnectorDequeueThrowsSqlException_throws()
            throws SQLException,QueueException {
        when(rawRepoConnector.dequeue(anyString())).thenThrow(new SQLException());
        final HarvestOperation harvestOperation = newHarvestOperation();
        assertThat(harvestOperation::execute, isThrowing(HarvesterException.class));
    }

    @Test
    public void execute_rawRepoConnectorDequeueThrowsQueueException_throws()
            throws SQLException, QueueException {
        when(rawRepoConnector.dequeue(anyString())).thenThrow(new QueueException("died"));
        final HarvestOperation harvestOperation = newHarvestOperation();
        assertThat(harvestOperation::execute, isThrowing(HarvesterException.class));
    }

    @Test
    public void execute_rawRepoConnectorFetchRecordCollectionThrowsSqlException_recordIsFailed()
            throws RecordServiceConnectorException, HarvesterException {
        when(rawRepoRecordServiceConnector.getRecordDataCollectionDataIO(any(RecordIdDTO.class)))
                .thenThrow(new RecordServiceConnectorException("Record not found"));

        final HarvestOperation harvestOperation = newHarvestOperation();
        harvestOperation.execute();

        final ArgumentCaptor<AddiRecord> addiRecordCaptor = ArgumentCaptor.forClass(AddiRecord.class);
        verify(harvesterJobBuilder, times(1)).addRecord(addiRecordCaptor.capture());
        assertHasDiagnostic(addiRecordCaptor.getValue());
    }

    @Test
    public void execute_rawRepoConnectorFetchRecordThrowsSqlException_recordIsFailed()
            throws SQLException, RecordServiceConnectorException, HarvesterException, QueueException {
        final QueueItem queueItem = getQueueItem(RECORD_ID);
        final RecordDTO record = new RecordDTO();
        record.setRecordId(RECORD_ID);
        record.setCreated(Instant.now().toString());
        record.setContent(getDeleteRecordContent(RECORD_ID).getBytes(StandardCharsets.UTF_8));
        record.setDeleted(true);

        when(rawRepoConnector.dequeue(anyString()))
                .thenReturn(queueItem)
                .thenReturn(null);

        when(rawRepoRecordServiceConnector.getRecordDataCollectionDataIO(any(RecordIdDTO.class)))
                .thenReturn(new HashMap<String, RecordDTO>() {{
                    put(RECORD_ID.getBibliographicRecordId(), record);
                }});

        when(rawRepoRecordServiceConnector.recordFetch(any(RecordIdDTO.class)))
                .thenThrow(new RecordServiceConnectorException("Record not found"));

        final HarvestOperation harvestOperation = newHarvestOperation();
        harvestOperation.execute();

        final ArgumentCaptor<AddiRecord> addiRecordCaptor = ArgumentCaptor.forClass(AddiRecord.class);
        verify(harvesterJobBuilder, times(1)).addRecord(addiRecordCaptor.capture());
        assertHasDiagnostic(addiRecordCaptor.getValue());
    }

    @Test
    public void execute_rawRepoConnectorFetchRecordThrowsRawRepoException_recordIsFailed()
            throws RecordServiceConnectorException, HarvesterException, SQLException, QueueException {
        final QueueItem queueItem = getQueueItem(RECORD_ID);
        final RecordDTO record = new RecordDTO();
        record.setRecordId(RECORD_ID);
        record.setCreated(Instant.now().toString());
        record.setContent(getDeleteRecordContent(RECORD_ID).getBytes(StandardCharsets.UTF_8));
        record.setDeleted(true);

        when(rawRepoConnector.dequeue(anyString()))
                .thenReturn(queueItem)
                .thenReturn(null);

        when(rawRepoRecordServiceConnector.getRecordDataCollectionDataIO(
                any(RecordIdDTO.class), any(RecordServiceConnector.Params.class)))
                .thenReturn(new HashMap<String, RecordDTO>() {{
                    put(RECORD_ID.getBibliographicRecordId(), record);
                }});

        when(rawRepoRecordServiceConnector.recordFetch(any(RecordIdDTO.class)))
                .thenThrow(new RecordServiceConnectorException("Record not found"));

        final HarvestOperation harvestOperation = newHarvestOperation();
        harvestOperation.execute();

        final ArgumentCaptor<AddiRecord> addiRecordCaptor = ArgumentCaptor.forClass(AddiRecord.class);
        verify(harvesterJobBuilder, times(1)).addRecord(addiRecordCaptor.capture());
        assertHasDiagnostic(addiRecordCaptor.getValue());
    }

    @Test
    public void execute_rawRepoConnectorFetchRecordCollectionThrowsRawRepoException_recordIsFailed()
            throws RecordServiceConnectorException, HarvesterException {
        when(rawRepoRecordServiceConnector.getRecordDataCollectionDataIO(
                any(RecordIdDTO.class), any(RecordServiceConnector.Params.class)))
                .thenThrow(new RecordServiceConnectorException("Record not found"));

        final HarvestOperation harvestOperation = newHarvestOperation();
        harvestOperation.execute();

        final ArgumentCaptor<AddiRecord> addiRecordCaptor = ArgumentCaptor.forClass(AddiRecord.class);
        verify(harvesterJobBuilder, times(1)).addRecord(addiRecordCaptor.capture());
        assertHasDiagnostic(addiRecordCaptor.getValue());
    }

    @Test
    public void execute_rawRepoConnectorFetchRecordCollectionThrowsMarcXMergerException_recordIsFailed()
            throws RecordServiceConnectorException, HarvesterException {
        when(rawRepoRecordServiceConnector.getRecordDataCollectionDataIO(
                any(RecordIdDTO.class), any(RecordServiceConnector.Params.class)))
                .thenThrow(new RecordServiceConnectorException("Could not merge records"));

        final HarvestOperation harvestOperation = newHarvestOperation();
        harvestOperation.execute();

        final ArgumentCaptor<AddiRecord> addiRecordCaptor = ArgumentCaptor.forClass(AddiRecord.class);
        verify(harvesterJobBuilder, times(1)).addRecord(addiRecordCaptor.capture());
        assertHasDiagnostic(addiRecordCaptor.getValue());
    }

    @Test
    public void execute_rawRepoRecordHasInvalidXmlContent_recordIsFailed()
            throws HarvesterException, RecordServiceConnectorException {
        final RecordDTO rrRecord = new RecordDTO();
        rrRecord.setRecordId(RECORD_ID);
        rrRecord.setCreated(Instant.now().toString());
        rrRecord.setContent("invalid".getBytes());

        when(rawRepoRecordServiceConnector.getRecordDataCollectionDataIO(
                any(RecordIdDTO.class), any(RecordServiceConnector.Params.class)))
                .thenReturn(new HashMap<String, RecordDTO>() {{
                    put(RECORD_ID.getBibliographicRecordId(), rrRecord);
                }});

        final HarvestOperation harvestOperation = newHarvestOperation();
        harvestOperation.execute();

        final ArgumentCaptor<AddiRecord> addiRecordCaptor = ArgumentCaptor.forClass(AddiRecord.class);
        verify(harvesterJobBuilder, times(1)).addRecord(addiRecordCaptor.capture());
        assertHasDiagnostic(addiRecordCaptor.getValue());
    }

    @Test
    public void execute_rawRepoRecordHasNoCreationDate_recordIsFailed()
            throws RecordServiceConnectorException, HarvesterException {
        final RecordDTO rrRecord = new RecordDTO();
        rrRecord.setRecordId(RECORD_ID);
        rrRecord.setCreated(null);
        rrRecord.setContent(getRecordContent(RECORD_ID).getBytes(StandardCharsets.UTF_8));
        when(rawRepoRecordServiceConnector.recordFetch(any(RecordIdDTO.class))).thenReturn(rrRecord);

        final HarvestOperation harvestOperation = newHarvestOperation();
        harvestOperation.execute();

        final ArgumentCaptor<AddiRecord> addiRecordCaptor = ArgumentCaptor.forClass(AddiRecord.class);
        verify(harvesterJobBuilder, times(1)).addRecord(addiRecordCaptor.capture());
        assertHasDiagnostic(addiRecordCaptor.getValue());
    }

    @Test
    public void execute_rawRepoReturnsEmptyCollection_recordIsFailed()
            throws RecordServiceConnectorException, HarvesterException {
        when(rawRepoRecordServiceConnector.getRecordDataCollectionDataIO(
                any(RecordIdDTO.class), any(RecordServiceConnector.Params.class)))
                .thenReturn(new HashMap<>());

        final HarvestOperation harvestOperation = newHarvestOperation();
        harvestOperation.execute();

        final ArgumentCaptor<AddiRecord> addiRecordCaptor = ArgumentCaptor.forClass(AddiRecord.class);
        verify(harvesterJobBuilder, times(1)).addRecord(addiRecordCaptor.capture());
        assertHasDiagnostic(addiRecordCaptor.getValue());
    }

    @Test
    public void execute_rawRepoReturnsCollectionWithoutBibliographicRecordId_recordIsFailed()
            throws RecordServiceConnectorException, HarvesterException {
        final RecordDTO rrRecord = new RecordDTO();
        rrRecord.setRecordId(RECORD_ID);
        rrRecord.setCreated(Instant.now().toString());
        rrRecord.setContent(getRecordContent(RECORD_ID).getBytes(StandardCharsets.UTF_8));
        when(rawRepoRecordServiceConnector.getRecordDataCollectionDataIO(
                any(RecordIdDTO.class), any(RecordServiceConnector.Params.class)))
                .thenReturn(new HashMap<String, RecordDTO>() {{
                    put("unexpectedBibliographicRecordId", rrRecord);
                }});

        final HarvestOperation harvestOperation = newHarvestOperation();
        harvestOperation.execute();

        final ArgumentCaptor<AddiRecord> addiRecordCaptor = ArgumentCaptor.forClass(AddiRecord.class);
        verify(harvesterJobBuilder, times(1)).addRecord(addiRecordCaptor.capture());
        assertHasDiagnostic(addiRecordCaptor.getValue());
    }

    @Test
    public void execute_harvesterJobBuilderThrowsHarvesterException_throws() throws HarvesterException {
        when(harvesterJobBuilder.build()).thenThrow(new HarvesterException("DIED"));

        final HarvestOperation harvestOperation = newHarvestOperation();
        assertThat(harvestOperation::execute, isThrowing(HarvesterException.class));
    }

    @Test
    public void execute_rawRepoDeleteRecordHasAgencyIdContainedInExcludedSet_recordIsProcessed()
            throws SQLException, RecordServiceConnectorException, HarvesterException, QueueException {
        final RecordIdDTO recordId = new RecordIdDTO("record", 870970);
        final QueueItem queueItem = getQueueItem(recordId);
        final RecordDTO record = new RecordDTO();
        record.setRecordId(recordId);
        record.setCreated(Instant.now().toString());
        record.setContent(getDeleteRecordContent(recordId).getBytes(StandardCharsets.UTF_8));
        record.setDeleted(true);

        when(rawRepoConnector.dequeue(anyString()))
                .thenReturn(queueItem)
                .thenReturn(null);

        when(rawRepoRecordServiceConnector.getRecordDataCollectionDataIO(any(RecordIdDTO.class)))
                .thenReturn(new HashMap<String, RecordDTO>() {{
                    put(recordId.getBibliographicRecordId(), record);
                }});

        when(rawRepoRecordServiceConnector.recordFetch(any(RecordIdDTO.class))).thenReturn(record);

        final HarvestOperation harvestOperation = newHarvestOperation();
        assertThat(harvestOperation.execute(), is(1));
        verify(rawRepoRecordServiceConnector, times(1))
                .recordFetch(any(RecordIdDTO.class));
        verify(rawRepoRecordServiceConnector, times(1))
                .getRecordDataCollectionDataIO(any(RecordIdDTO.class), any(RecordServiceConnector.Params.class));
    }

    @Test
    public void execute_rawRepoDeleteRecordHasDbcId_recordIsSkipped()
            throws SQLException, RecordServiceConnectorException, HarvesterException, QueueException {
        final QueueItem queueItem = getQueueItem(DBC_RECORD_ID);
        final RecordDTO record = new RecordDTO();
        record.setRecordId(DBC_RECORD_ID);
        record.setCreated(Instant.now().toString());
        record.setContent(getDeleteRecordContent(DBC_RECORD_ID).getBytes(StandardCharsets.UTF_8));
        record.setDeleted(true);

        when(rawRepoConnector.dequeue(anyString()))
                .thenReturn(queueItem)
                .thenReturn(null);

        when(rawRepoRecordServiceConnector.getRecordDataCollectionDataIO(any(RecordIdDTO.class)))
                .thenReturn(new HashMap<String, RecordDTO>() {{
                    put(DBC_RECORD_ID.getBibliographicRecordId(), record);
                }});

        when(rawRepoRecordServiceConnector.recordFetch(any(RecordIdDTO.class))).thenReturn(record);

        final HarvestOperation harvestOperation = newHarvestOperation();
        harvestOperation.execute();
        verify(rawRepoRecordServiceConnector, times(1))
                .recordFetch(any(RecordIdDTO.class));
        verify(rawRepoRecordServiceConnector, times(0))
                .getRecordDataCollectionDataIO(any(RecordIdDTO.class), any(RecordServiceConnector.Params.class));
    }

    @Test
    public void getJobSpecificationTemplate_interpolatesConfigValues() {
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
    public void getJobSpecificationTemplate_interpolatesConfigWithFormatOverrides() {
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
    public void getJobSpecificationTemplate_harvestOperationConfigJobTypeIsSetToTransientAsDefault() {
        final RRHarvesterConfig config = HarvesterTestUtil.getRRHarvesterConfig();
        final HarvestOperation harvestOperation = newHarvestOperation(config);
        assertThat(harvestOperation.getJobSpecificationTemplate(AGENCY_ID).getType(), is(JobSpecification.Type.TRANSIENT));
    }

    @Test
    public void getJobSpecificationTemplate_harvestOperationConfigJobTypeCanBeChangedFromDefault() {
        final RRHarvesterConfig config = HarvesterTestUtil.getRRHarvesterConfig();
        config.getContent().withType(JobSpecification.Type.TEST);
        final HarvestOperation harvestOperation = newHarvestOperation(config);
        assertThat(harvestOperation.getJobSpecificationTemplate(AGENCY_ID).getType(), is(JobSpecification.Type.TEST));
    }

    @Test
    public void getAgencyId_DBC_enrichmentTrailArgIsNull_throws() {
        final RecordDTO record = new RecordDTO();
        record.setRecordId(DBC_RECORD_ID);
        record.setCreated(Instant.now().toString());
        record.setEnrichmentTrail(null);
        final HarvestOperation harvestOperation = newHarvestOperation();
        assertThat(() -> harvestOperation.getAgencyIdFromEnrichmentTrail(record), isThrowing(HarvesterInvalidRecordException.class));
    }

    @Test
    public void getAgencyId_DBC_enrichmentTrailArgIsEmpty_throws() {
        final RecordDTO record = new RecordDTO();
        record.setRecordId(DBC_RECORD_ID);
        record.setCreated(Instant.now().toString());
        record.setEnrichmentTrail(" ");
        final HarvestOperation harvestOperation = newHarvestOperation();
        assertThat(() -> harvestOperation.getAgencyIdFromEnrichmentTrail(record), isThrowing(HarvesterInvalidRecordException.class));
    }

    @Test
    public void getAgencyId_DBC_no870TrailFound_throws() {
        final RecordDTO record = new RecordDTO();
        record.setRecordId(DBC_RECORD_ID);
        record.setCreated(Instant.now().toString());
        record.setEnrichmentTrail("191919,123456");
        final HarvestOperation harvestOperation = newHarvestOperation();
        assertThat(() -> harvestOperation.getAgencyIdFromEnrichmentTrail(record), isThrowing(HarvesterInvalidRecordException.class));
    }

    @Test
    public void getAgencyId_DBC_invalid870TrailFound_throws() {
        final RecordDTO record = new RecordDTO();
        record.setRecordId(DBC_RECORD_ID);
        record.setCreated(Instant.now().toString());
        record.setEnrichmentTrail("191919,870abc");
        final HarvestOperation harvestOperation = newHarvestOperation();
        assertThat(() -> harvestOperation.getAgencyIdFromEnrichmentTrail(record), isThrowing(HarvesterInvalidRecordException.class));
    }

    @Test
    public void getAgencyId_DBC_returnsAgencyIdFromEnrichmentTrail()
            throws HarvesterInvalidRecordException {
        final RecordDTO record = new RecordDTO();
        record.setRecordId(DBC_RECORD_ID);
        record.setCreated(Instant.now().toString());
        record.setEnrichmentTrail("191919,870970");
        final HarvestOperation harvestOperation = newHarvestOperation();
        assertThat(harvestOperation.getAgencyIdFromEnrichmentTrail(record), is(870970));
    }

    @Test
    public void getAgencyId_DBC_returnsAutomarcAgencyIdFromEnrichmentTrail()
            throws HarvesterInvalidRecordException {
        final RecordDTO record = new RecordDTO();
        record.setRecordId(DBC_RECORD_ID);
        record.setCreated(Instant.now().toString());
        record.setEnrichmentTrail("190004,191919");
        final HarvestOperation harvestOperation = newHarvestOperation();
        assertThat(harvestOperation.getAgencyIdFromEnrichmentTrail(record), is(190004));
    }

    @Test
    public void execute_whenRawRepoQueueIsEmpty_fallsBackToTaskQueue()
            throws SQLException, HarvesterException, QueueException {
        final HarvestOperation harvestOperation = newHarvestOperation();
        final TypedQuery<HarvestTask> query = mock(TypedQuery.class);
        when(entityManager.createNamedQuery(HarvestTask.QUERY_FIND_NEXT, HarvestTask.class)).thenReturn(query);
        when(query.setParameter("configId", harvestOperation.config.getId())).thenReturn(query);
        when(query.setMaxResults(1)).thenReturn(query);
        when(query.getResultList()).thenReturn(Collections.emptyList());
        when(rawRepoConnector.dequeue(anyString()))
                .thenReturn(null);

        harvestOperation.execute();

        verify(entityManager).createNamedQuery(HarvestTask.QUERY_FIND_NEXT, HarvestTask.class);
    }

    @Test
    public void fetchRecord_returnRecord() throws RecordServiceConnectorException,
            HarvesterInvalidRecordException, HarvesterSourceException {

        final RecordIdDTO expectedRecordId = new RecordIdDTO("expected", HarvestOperation.DBC_LIBRARY);
        final RecordDTO expectedRecord = new RecordDTO();
        expectedRecord.setRecordId(expectedRecordId);
        expectedRecord.setContent(getRecordContent(expectedRecordId).getBytes(StandardCharsets.UTF_8));
        expectedRecord.setEnrichmentTrail("191919,870970");
        expectedRecord.setTrackingId("tracking id");
        expectedRecord.setCreated(Instant.now().toString());

        when(rawRepoRecordServiceConnector.recordFetch(any(RecordIdDTO.class)))
                .thenReturn(expectedRecord);

        HarvestOperation harvestOperation = newHarvestOperation();
        RecordDTO fetched = harvestOperation.fetchRecord(expectedRecordId);

        assertThat(fetched, notNullValue());
        assertThat(fetched.getRecordId(), is(expectedRecord.getRecordId()));
        assertThat(fetched.getContent(), is(expectedRecord.getContent()));
    }

    @Test
    public void fetchRecordCollection_returnRecordCollection() throws RecordServiceConnectorException,
            HarvesterInvalidRecordException, HarvesterSourceException {

        final RecordIdDTO expectedRecordId = new RecordIdDTO("expected", HarvestOperation.DBC_LIBRARY);
        final RecordDTO expectedRecord = new RecordDTO();
        expectedRecord.setRecordId(expectedRecordId);
        expectedRecord.setContent(getRecordContent(expectedRecordId).getBytes(StandardCharsets.UTF_8));
        expectedRecord.setEnrichmentTrail("191919,870970");
        expectedRecord.setTrackingId("tracking id");
        expectedRecord.setCreated(Instant.now().toString());
        final HashMap<String, RecordDTO> expectedRecordCollection = new HashMap<>();
        expectedRecordCollection.put(expectedRecordId.getBibliographicRecordId(), expectedRecord);

        when(rawRepoRecordServiceConnector.getRecordDataCollectionDataIO(
                any(RecordIdDTO.class), any(RecordServiceConnector.Params.class)))
                .thenReturn(expectedRecordCollection);

        HarvestOperation harvestOperation = newHarvestOperation();
        Map<String, RecordDTO> fetched = harvestOperation.fetchRecordCollection(expectedRecordId);

        assertThat(fetched, notNullValue());
        assertThat(fetched.size(), is(1));
        assertThat(fetched.containsKey(expectedRecordId.getBibliographicRecordId()), is(true));
        assertThat(fetched.get(expectedRecordId.getBibliographicRecordId()).getRecordId(), is(expectedRecord.getRecordId()));
        assertThat(fetched.get(expectedRecordId.getBibliographicRecordId()).getContent(), is(expectedRecord.getContent()));
    }

    public HarvestOperation newHarvestOperation(RRHarvesterConfig config) {
        try {
            return new HarvestOperation(config, harvesterJobBuilderFactory,
                taskRepo, VIP_CORE_CONNECTION,
                rawRepoConnector, rawRepoRecordServiceConnector, metricRegistry);
        } catch (QueueException | SQLException | ConfigurationException e) {
            throw new IllegalStateException(e);
        }
    }

    public HarvestOperation newHarvestOperation() {
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

    public static QueueItem getQueueItem(RecordIdDTO recordId, Date queued) {
        return new MockedQueueItem(recordId.getBibliographicRecordId(), recordId.getAgencyId(), "QUEUE_ID",
                new Timestamp(queued.getTime()));
    }

    public static QueueItem getQueueItem(RecordIdDTO recordId) {
        return getQueueItem(recordId, new Date());
    }

    public static String getRecordContent(RecordIdDTO recordId) {
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

    public static String getDeleteRecordContent(RecordIdDTO recordId) {
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
