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
import dk.dbc.dataio.commons.utils.test.model.JobSpecificationBuilder;
import dk.dbc.dataio.harvester.rr.entity.HarvestTask;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.HarvesterInvalidRecordException;
import dk.dbc.dataio.harvester.types.RRHarvesterConfig;
import dk.dbc.dataio.harvester.utils.rawrepo.RawRepoConnector;
import dk.dbc.dataio.jsonb.JSONBContext;
import dk.dbc.dataio.jsonb.JSONBException;
import dk.dbc.marcxmerge.MarcXMergerException;
import dk.dbc.rawrepo.MockedQueueJob;
import dk.dbc.rawrepo.MockedRecord;
import dk.dbc.rawrepo.QueueJob;
import dk.dbc.rawrepo.RawRepoException;
import dk.dbc.rawrepo.Record;
import dk.dbc.rawrepo.RecordId;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import javax.naming.Context;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.sql.DataSource;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;

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
    public final static RecordId DBC_RECORD_ID = new RecordId("record", HarvestOperation.DBC_LIBRARY);
    public final static RecordId RECORD_ID = new RecordId("record", 710100);
    public final static String RECORD_CONTENT = getRecordContent(RECORD_ID);
    public final static Record RECORD = new MockedRecord(RECORD_ID, true);
    public final static QueueJob QUEUE_JOB = getQueueJob(RECORD_ID);

    static {
        RECORD.setContent(RECORD_CONTENT.getBytes(StandardCharsets.UTF_8));
    }

    final EntityManager entityManager = mock(EntityManager.class);
    final HarvesterJobBuilderFactory harvesterJobBuilderFactory = mock(HarvesterJobBuilderFactory.class);
    final HarvesterJobBuilder harvesterJobBuilder = mock(HarvesterJobBuilder.class);
    final RawRepoConnector rawRepoConnector = mock(RawRepoConnector.class);
    final JSONBContext jsonbContext = new JSONBContext();

    @BeforeClass
    public static void setup() {
        // sets up the InMemoryInitialContextFactory as default factory.
        System.setProperty(Context.INITIAL_CONTEXT_FACTORY, InMemoryInitialContextFactory.class.getName());
    }

    @Before
    public void setupTest() throws RawRepoException, SQLException, MarcXMergerException, HarvesterException {
        when(rawRepoConnector.dequeue(anyString()))
                .thenReturn(QUEUE_JOB)
                .thenReturn(null);
        when(rawRepoConnector.fetchRecordCollection(any(RecordId.class)))
                .thenReturn(new HashMap<String, Record>() {{
                    put(RECORD_ID.getBibliographicRecordId(), RECORD);
                }});
        when(rawRepoConnector.fetchRecord(any(RecordId.class))).thenReturn(RECORD);
        when(harvesterJobBuilderFactory.newHarvesterJobBuilder(any(JobSpecification.class))).thenReturn(harvesterJobBuilder);
    }

    @Test
    public void constructor_noOpenAgencyTargetIsConfigured_throws() {
        final RRHarvesterConfig config = HarvesterTestUtil.getRRHarvesterConfig();
        config.getContent().withOpenAgencyTarget(null);
        assertThat(() -> new HarvestOperation(config, harvesterJobBuilderFactory, entityManager), isThrowing(IllegalArgumentException.class));
    }

    @Test
    public void execute_rawRepoConnectorDequeueThrowsSqlException_throws() throws SQLException, RawRepoException {
        when(rawRepoConnector.dequeue(anyString())).thenThrow(new SQLException());
        final HarvestOperation harvestOperation = newHarvestOperation();
        assertThat(() -> harvestOperation.execute(), isThrowing(HarvesterException.class));
    }

    @Test
    public void execute_rawRepoConnectorDequeueThrowsRawRepoException_throws() throws SQLException, RawRepoException {
        when(rawRepoConnector.dequeue(anyString())).thenThrow(new RawRepoException());
        final HarvestOperation harvestOperation = newHarvestOperation();
        assertThat(() -> harvestOperation.execute(), isThrowing(HarvesterException.class));
    }

    @Test
    public void execute_rawRepoConnectorFetchRecordCollectionThrowsSqlException_recordIsFailed() throws SQLException, RawRepoException, MarcXMergerException, HarvesterException {
        when(rawRepoConnector.fetchRecordCollection(any(RecordId.class))).thenThrow(new SQLException());

        final HarvestOperation harvestOperation = newHarvestOperation();
        harvestOperation.execute();

        final ArgumentCaptor<AddiRecord> addiRecordCaptor = ArgumentCaptor.forClass(AddiRecord.class);
        verify(harvesterJobBuilder, times(1)).addRecord(addiRecordCaptor.capture());
        assertHasDiagnostic(addiRecordCaptor.getValue());
    }

    @Test
    public void execute_rawRepoConnectorFetchRecordThrowsSqlException_recordIsFailed() throws SQLException, RawRepoException, MarcXMergerException, HarvesterException {
        final QueueJob queueJob = getQueueJob(RECORD_ID);
        final Record record = new MockedRecord(RECORD_ID, true);
        record.setContent(getDeleteRecordContent(RECORD_ID).getBytes(StandardCharsets.UTF_8));
        record.setDeleted(true);

        when(rawRepoConnector.dequeue(anyString()))
                .thenReturn(queueJob)
                .thenReturn(null);

        when(rawRepoConnector.fetchRecordCollection(any(RecordId.class)))
                .thenReturn(new HashMap<String, Record>() {{
                    put(RECORD_ID.getBibliographicRecordId(), record);
                }});

        when(rawRepoConnector.fetchRecord(any(RecordId.class))).thenThrow(new SQLException());

        final HarvestOperation harvestOperation = newHarvestOperation();
        harvestOperation.execute();

        final ArgumentCaptor<AddiRecord> addiRecordCaptor = ArgumentCaptor.forClass(AddiRecord.class);
        verify(harvesterJobBuilder, times(1)).addRecord(addiRecordCaptor.capture());
        assertHasDiagnostic(addiRecordCaptor.getValue());
    }

    @Test
    public void execute_rawRepoConnectorFetchRecordThrowsRawRepoException_recordIsFailed() throws SQLException, RawRepoException, MarcXMergerException, HarvesterException {
        final QueueJob queueJob = getQueueJob(RECORD_ID);
        final Record record = new MockedRecord(RECORD_ID, true);
        record.setContent(getDeleteRecordContent(RECORD_ID).getBytes(StandardCharsets.UTF_8));
        record.setDeleted(true);

        when(rawRepoConnector.dequeue(anyString()))
                .thenReturn(queueJob)
                .thenReturn(null);

        when(rawRepoConnector.fetchRecordCollection(any(RecordId.class)))
                .thenReturn(new HashMap<String, Record>() {{
                    put(RECORD_ID.getBibliographicRecordId(), record);
                }});

        when(rawRepoConnector.fetchRecord(any(RecordId.class))).thenThrow(new SQLException());

        final HarvestOperation harvestOperation = newHarvestOperation();
        harvestOperation.execute();

        final ArgumentCaptor<AddiRecord> addiRecordCaptor = ArgumentCaptor.forClass(AddiRecord.class);
        verify(harvesterJobBuilder, times(1)).addRecord(addiRecordCaptor.capture());
        assertHasDiagnostic(addiRecordCaptor.getValue());
    }

    @Test
    public void execute_rawRepoConnectorFetchRecordCollectionThrowsRawRepoException_recordIsFailed() throws SQLException, RawRepoException, MarcXMergerException, HarvesterException {
        when(rawRepoConnector.fetchRecordCollection(any(RecordId.class))).thenThrow(new RawRepoException());

        final HarvestOperation harvestOperation = newHarvestOperation();
        harvestOperation.execute();

        final ArgumentCaptor<AddiRecord> addiRecordCaptor = ArgumentCaptor.forClass(AddiRecord.class);
        verify(harvesterJobBuilder, times(1)).addRecord(addiRecordCaptor.capture());
        assertHasDiagnostic(addiRecordCaptor.getValue());
    }

    @Test
    public void execute_rawRepoConnectorFetchRecordCollectionThrowsMarcXMergerException_recordIsFailed() throws SQLException, RawRepoException, MarcXMergerException, HarvesterException {
        when(rawRepoConnector.fetchRecordCollection(any(RecordId.class))).thenThrow(new MarcXMergerException());

        final HarvestOperation harvestOperation = newHarvestOperation();
        harvestOperation.execute();

        final ArgumentCaptor<AddiRecord> addiRecordCaptor = ArgumentCaptor.forClass(AddiRecord.class);
        verify(harvesterJobBuilder, times(1)).addRecord(addiRecordCaptor.capture());
        assertHasDiagnostic(addiRecordCaptor.getValue());
    }

    @Test
    public void execute_rawRepoRecordHasInvalidXmlContent_recordIsFailed() throws HarvesterException, SQLException, RawRepoException, MarcXMergerException {
        final Record rrRecord = mock(Record.class);
        when(rawRepoConnector.fetchRecordCollection(any(RecordId.class)))
                .thenReturn(new HashMap<String, Record>() {{
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
    public void execute_rawRepoRecordHasNoCreationDate_recordIsFailed() throws RawRepoException, SQLException, MarcXMergerException, HarvesterException {
        final MockedRecord rrRecord = new MockedRecord(RECORD_ID, true);
        rrRecord.setCreated(null);
        rrRecord.setContent(getRecordContent(RECORD_ID).getBytes(StandardCharsets.UTF_8));
        when(rawRepoConnector.fetchRecord(any(RecordId.class))).thenReturn(rrRecord);

        final HarvestOperation harvestOperation = newHarvestOperation();
        harvestOperation.execute();

        final ArgumentCaptor<AddiRecord> addiRecordCaptor = ArgumentCaptor.forClass(AddiRecord.class);
        verify(harvesterJobBuilder, times(1)).addRecord(addiRecordCaptor.capture());
        assertHasDiagnostic(addiRecordCaptor.getValue());
    }

    @Test
    public void execute_rawRepoReturnsEmptyCollection_recordIsFailed() throws RawRepoException, SQLException, MarcXMergerException, HarvesterException {
        when(rawRepoConnector.fetchRecordCollection(any(RecordId.class)))
                .thenReturn(Collections.emptyMap());

        final HarvestOperation harvestOperation = newHarvestOperation();
        harvestOperation.execute();

        final ArgumentCaptor<AddiRecord> addiRecordCaptor = ArgumentCaptor.forClass(AddiRecord.class);
        verify(harvesterJobBuilder, times(1)).addRecord(addiRecordCaptor.capture());
        assertHasDiagnostic(addiRecordCaptor.getValue());
    }

    @Test
    public void execute_rawRepoReturnsCollectionWithoutBibliographicRecordId_recordIsFailed() throws RawRepoException, SQLException, MarcXMergerException, HarvesterException {
        final MockedRecord rrRecord = new MockedRecord(RECORD_ID, true);
        rrRecord.setContent(getRecordContent(RECORD_ID).getBytes(StandardCharsets.UTF_8));
        when(rawRepoConnector.fetchRecordCollection(any(RecordId.class)))
                .thenReturn(new HashMap<String, Record>() {{
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
        assertThat(() -> harvestOperation.execute(), isThrowing(HarvesterException.class));
    }

    @Test
    public void execute_rawRepoDeleteRecordHasAgencyIdContainedInExcludedSet_recordIsProcessed()
            throws RawRepoException, SQLException, MarcXMergerException, HarvesterException {
        final RecordId recordId = new RecordId("record", 870970);
        final QueueJob queueJob = getQueueJob(recordId);
        final Record record = new MockedRecord(recordId, true);
        record.setContent(getDeleteRecordContent(recordId).getBytes(StandardCharsets.UTF_8));
        record.setDeleted(true);

        when(rawRepoConnector.dequeue(anyString()))
                .thenReturn(queueJob)
                .thenReturn(null);

        when(rawRepoConnector.fetchRecordCollection(any(RecordId.class)))
                .thenReturn(new HashMap<String, Record>() {{
                    put(recordId.getBibliographicRecordId(), record);
                }});

        when(rawRepoConnector.fetchRecord(any(RecordId.class))).thenReturn(record);

        final HarvestOperation harvestOperation = newHarvestOperation();
        assertThat(harvestOperation.execute(), is(1));
        verify(rawRepoConnector, times(1)).fetchRecord(any(RecordId.class));
        verify(rawRepoConnector, times(1)).fetchRecordCollection(any(RecordId.class));
    }

    @Test
    public void execute_rawRepoDeleteRecordHasDbcId_recordIsSkipped()
            throws RawRepoException, SQLException, MarcXMergerException, HarvesterException {
        final QueueJob queueJob = getQueueJob(DBC_RECORD_ID);
        final Record record = new MockedRecord(DBC_RECORD_ID, true);
        record.setContent(getDeleteRecordContent(DBC_RECORD_ID).getBytes(StandardCharsets.UTF_8));
        record.setDeleted(true);

        when(rawRepoConnector.dequeue(anyString()))
                .thenReturn(queueJob)
                .thenReturn(null);

        when(rawRepoConnector.fetchRecordCollection(any(RecordId.class)))
                .thenReturn(new HashMap<String, Record>() {{
                    put(DBC_RECORD_ID.getBibliographicRecordId(), record);
                }});

        when(rawRepoConnector.fetchRecord(any(RecordId.class))).thenReturn(record);

        final HarvestOperation harvestOperation = newHarvestOperation();
        harvestOperation.execute();
        verify(rawRepoConnector, times(1)).fetchRecord(any(RecordId.class));
        verify(rawRepoConnector, times(0)).fetchRecordCollection(any(RecordId.class));
    }

    @Test
    public void getJobSpecificationTemplate_interpolatesConfigValues() {
        final int agencyId = 424242;

        final RRHarvesterConfig config = HarvesterTestUtil.getRRHarvesterConfig();

        final JobSpecification expectedJobSpecificationTemplate = getJobSpecificationTemplateBuilder()
                .setSubmitterId(agencyId)
                .setPackaging("addi-xml")
                .setAncestry(new JobSpecification.Ancestry()
                            .withHarvesterToken(config.getHarvesterToken()))
                .build();

        config.getContent()
                .withConsumerId("consumerId")
                .withFormat(expectedJobSpecificationTemplate.getFormat())
                .withDestination(expectedJobSpecificationTemplate.getDestination());
        final HarvestOperation harvestOperation = newHarvestOperation(config);

        assertThat(harvestOperation.getJobSpecificationTemplate(agencyId), is(expectedJobSpecificationTemplate));
    }

    @Test
    public void getJobSpecificationTemplate_interpolatesConfigWithFormatOverrides() {
        final int agencyId = 424242;
        final String consumerId = "rrConsumer";
        final String formatOverride = "alternativeFormat";

        final RRHarvesterConfig config = HarvesterTestUtil.getRRHarvesterConfig();

        final JobSpecification expectedJobSpecificationTemplate = getJobSpecificationTemplateBuilder()
                .setSubmitterId(agencyId)
                .setPackaging("addi-xml")
                .setFormat(formatOverride)
                .setAncestry(new JobSpecification.Ancestry()
                            .withHarvesterToken(config.getHarvesterToken()))
                .build();

        config.getContent()
                .withConsumerId(consumerId)
                .withDestination(expectedJobSpecificationTemplate.getDestination())
                .withFormat("format")
                .withFormatOverridesEntry(agencyId, formatOverride);
        final HarvestOperation harvestOperation = newHarvestOperation(config);

        assertThat(harvestOperation.getJobSpecificationTemplate(agencyId), is(expectedJobSpecificationTemplate));
    }

    @Test
    public void getJobSpecificationTemplate_harvestOperationConfigJobTypeIsSetToTransientAsDefault() {
        final int agencyId = 424242;
        final RRHarvesterConfig config = HarvesterTestUtil.getRRHarvesterConfig();
        final HarvestOperation harvestOperation = newHarvestOperation(config);
        assertThat(harvestOperation.getJobSpecificationTemplate(agencyId).getType(), is(JobSpecification.Type.TRANSIENT));
    }

    @Test
    public void getJobSpecificationTemplate_harvestOperationConfigJobTypeCanBeChangedFromDefault() {
        final int agencyId = 424242;
        final RRHarvesterConfig config = HarvesterTestUtil.getRRHarvesterConfig();
        config.getContent().withType(JobSpecification.Type.TEST);
        final HarvestOperation harvestOperation = newHarvestOperation(config);
        assertThat(harvestOperation.getJobSpecificationTemplate(agencyId).getType(), is(JobSpecification.Type.TEST));
    }

    @Test
    public void getRawRepoConnector_openAgencyTargetIsConfigured_configuresAgencySearchOrder() throws MalformedURLException {
        try {
            final RRHarvesterConfig config = HarvesterTestUtil.getRRHarvesterConfig();
            InMemoryInitialContextFactory.bind(config.getContent().getResource(), mock(DataSource.class));

            final HarvestOperation harvestOperation = new HarvestOperation(config, harvesterJobBuilderFactory, entityManager);
            final RawRepoConnector rawRepoConnector = harvestOperation.getRawRepoConnector(config);
            assertThat(rawRepoConnector.getAgencySearchOrder(), is(notNullValue()));
            assertThat(rawRepoConnector.getRelationHints(), is(notNullValue()));
        } finally {
            InMemoryInitialContextFactory.clear();
        }
    }

    @Test
    public void getAgencyId_DBC_enrichmentTrailArgIsNull_throws() throws HarvesterInvalidRecordException {
        final MockedRecord record = new MockedRecord(DBC_RECORD_ID, true);
        record.setEnrichmentTrail(null);
        final HarvestOperation harvestOperation = newHarvestOperation();
        assertThat(() -> harvestOperation.getAgencyIdFromEnrichmentTrail(record), isThrowing(HarvesterInvalidRecordException.class));
    }

    @Test
    public void getAgencyId_DBC_enrichmentTrailArgIsEmpty_throws() throws HarvesterInvalidRecordException {
        final MockedRecord record = new MockedRecord(DBC_RECORD_ID, true);
        record.setEnrichmentTrail(" ");
        final HarvestOperation harvestOperation = newHarvestOperation();
        assertThat(() -> harvestOperation.getAgencyIdFromEnrichmentTrail(record), isThrowing(HarvesterInvalidRecordException.class));
    }

    @Test
    public void getAgencyId_DBC_no870TrailFound_throws() throws HarvesterInvalidRecordException {
        final MockedRecord record = new MockedRecord(DBC_RECORD_ID, true);
        record.setEnrichmentTrail("191919,123456");
        final HarvestOperation harvestOperation = newHarvestOperation();
        assertThat(() -> harvestOperation.getAgencyIdFromEnrichmentTrail(record), isThrowing(HarvesterInvalidRecordException.class));
    }

    @Test
    public void getAgencyId_DBC_invalid870TrailFound_throws() throws HarvesterInvalidRecordException {
        final MockedRecord record = new MockedRecord(DBC_RECORD_ID, true);
        record.setEnrichmentTrail("191919,870abc");
        final HarvestOperation harvestOperation = newHarvestOperation();
        assertThat(() -> harvestOperation.getAgencyIdFromEnrichmentTrail(record), isThrowing(HarvesterInvalidRecordException.class));
    }

    @Test
    public void getAgencyId_DBC_returnsAgencyIdFromEnrichmentTrail() throws HarvesterInvalidRecordException {
        final MockedRecord record = new MockedRecord(DBC_RECORD_ID, true);
        record.setEnrichmentTrail("191919,870970");
        final HarvestOperation harvestOperation = newHarvestOperation();
        assertThat(harvestOperation.getAgencyIdFromEnrichmentTrail(record), is(870970));
    }

    @Test
    public void execute_whenRawRepoQueueIsEmpty_fallsBackToTaskQueue() throws RawRepoException, SQLException, HarvesterException {
        final TypedQuery<HarvestTask> query = mock(TypedQuery.class);
        when(entityManager.createNamedQuery(HarvestTask.QUERY_FIND_READY, HarvestTask.class)).thenReturn(query);
        when(query.setParameter(eq("configId"), anyInt())).thenReturn(query);
        when(query.setMaxResults(1)).thenReturn(query);
        when(query.getResultList()).thenReturn(Collections.emptyList());
        when(rawRepoConnector.dequeue(anyString()))
                .thenReturn(null);

        final HarvestOperation harvestOperation = newHarvestOperation();
        harvestOperation.execute();

        verify(entityManager).createNamedQuery(HarvestTask.QUERY_FIND_READY, HarvestTask.class);
    }

    public HarvestOperation newHarvestOperation(RRHarvesterConfig config) {
        return new HarvestOperation(config, harvesterJobBuilderFactory, entityManager, null, rawRepoConnector);
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

    private JobSpecificationBuilder getJobSpecificationTemplateBuilder() {
        return new JobSpecificationBuilder()
            .setPackaging("xml")
            .setCharset("utf8")
            .setFormat("katalog")
            .setMailForNotificationAboutVerification("placeholder")
            .setMailForNotificationAboutProcessing("placeholder")
            .setResultmailInitials("placeholder")
            .setDataFile("placeholder")
            .setType(JobSpecification.Type.TRANSIENT);
    }

    public static QueueJob getQueueJob(RecordId recordId, Date queued) {
        return new MockedQueueJob(recordId.getBibliographicRecordId(), recordId.getAgencyId(), "QUEUE_ID",
                new Timestamp(queued.getTime()));
    }

    public static QueueJob getQueueJob(RecordId recordId) {
        return getQueueJob(recordId, new Date());
    }

    public static String getRecordContent(RecordId recordId) {
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

    public static String getDeleteRecordContent(RecordId recordId) {
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