package dk.dbc.dataio.harvester.rr;

import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.utils.test.jndi.InMemoryInitialContextFactory;
import dk.dbc.dataio.commons.utils.test.model.JobSpecificationBuilder;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.OpenAgencyTarget;
import dk.dbc.dataio.harvester.types.RawRepoHarvesterConfig;
import dk.dbc.dataio.harvester.utils.rawrepo.RawRepoConnector;
import dk.dbc.marcxmerge.MarcXMergerException;
import dk.dbc.rawrepo.AgencySearchOrderFallback;
import dk.dbc.rawrepo.MockedQueueJob;
import dk.dbc.rawrepo.MockedRecord;
import dk.dbc.rawrepo.QueueJob;
import dk.dbc.rawrepo.RawRepoException;
import dk.dbc.rawrepo.Record;
import dk.dbc.rawrepo.RecordId;
import dk.dbc.rawrepo.showorder.AgencySearchOrderFromShowOrder;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.naming.Context;
import javax.sql.DataSource;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class HarvestOperationTest {
    private final static RecordId RECORD_ID = new RecordId("record", 12345678);
    private final static String RECORD_CONTENT = getRecordContent(RECORD_ID);
    private final static Record RECORD = new MockedRecord(RECORD_ID, true);
    private final static QueueJob QUEUE_JOB = getQueueJob(RECORD_ID);

    static {
        RECORD.setContent(RECORD_CONTENT.getBytes(StandardCharsets.UTF_8));
    }

    private final HarvesterJobBuilderFactory harvesterJobBuilderFactory = mock(HarvesterJobBuilderFactory.class);
    private final HarvesterJobBuilder harvesterJobBuilder = mock(HarvesterJobBuilder.class);
    private final RawRepoConnector rawRepoConnector = mock(RawRepoConnector.class);

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
        when(harvesterJobBuilderFactory.newHarvesterJobBuilder(any(JobSpecification.class))).thenReturn(harvesterJobBuilder);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_configArgIsNull_throws() {
        new HarvestOperation(null, harvesterJobBuilderFactory);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_harvesterJobBuilderFactoryArgIsNull_throws() {
        new HarvestOperation(HarvesterTestUtil.getHarvestOperationConfigEntry(), null);
    }

    @Test
    public void execute_rawRepoConnectorDequeueThrowsSqlException_throws() throws SQLException, RawRepoException {
        when(rawRepoConnector.dequeue(anyString())).thenThrow(new SQLException());

        final HarvestOperation harvestOperation = getHarvestOperation();
        try {
            harvestOperation.execute();
            fail("No exception thrown");
        } catch (HarvesterException e) {
        }
    }

    @Test
    public void execute_rawRepoConnectorDequeueThrowsRawRepoException_throws() throws SQLException, RawRepoException {
        when(rawRepoConnector.dequeue(anyString())).thenThrow(new RawRepoException());

        final HarvestOperation harvestOperation = getHarvestOperation();
        try {
            harvestOperation.execute();
            fail("No exception thrown");
        } catch (HarvesterException e) {
        }
    }

    @Test
    public void execute_rawRepoConnectorQueueFailThrowsSqlException_throws() throws SQLException, RawRepoException, MarcXMergerException {
        final Record rrRecord = mock(Record.class);
        when(rawRepoConnector.fetchRecordCollection(any(RecordId.class)))
                .thenReturn(new HashMap<String, Record>(){{
                    put("ID", rrRecord);
                }});
        when(rrRecord.getContent()).thenReturn("invalid".getBytes());
        doThrow(new SQLException()).when(rawRepoConnector).queueFail(any(QueueJob.class), anyString());

        final HarvestOperation harvestOperation = getHarvestOperation();
        try {
            harvestOperation.execute();
            fail("No exception thrown");
        } catch (HarvesterException e) {
        }
    }

    @Test
    public void execute_rawRepoConnectorQueueFailThrowsRawRepoException_throws() throws SQLException, RawRepoException, MarcXMergerException {
        final Record rrRecord = mock(Record.class);
        when(rawRepoConnector.fetchRecordCollection(any(RecordId.class)))
                .thenReturn(new HashMap<String, Record>() {{
                    put("ID", rrRecord);
                }});
        when(rrRecord.getContent()).thenReturn("invalid".getBytes());
        doThrow(new RawRepoException()).when(rawRepoConnector).queueFail(any(QueueJob.class), anyString());

        final HarvestOperation harvestOperation = getHarvestOperation();
        try {
            harvestOperation.execute();
            fail("No exception thrown");
        } catch (HarvesterException e) {
        }
    }

    @Test
    public void execute_rawRepoConnectorFetchRecordCollectionThrowsSqlException_recordIsFailed() throws SQLException, RawRepoException, MarcXMergerException, HarvesterException {
        when(rawRepoConnector.fetchRecordCollection(any(RecordId.class))).thenThrow(new SQLException());

        final HarvestOperation harvestOperation = getHarvestOperation();
        harvestOperation.execute();

        verify(rawRepoConnector, times(1)).queueFail(any(QueueJob.class), anyString());
    }

    @Test
    public void execute_rawRepoConnectorFetchRecordCollectionThrowsRawRepoException_recordIsFailed() throws SQLException, RawRepoException, MarcXMergerException, HarvesterException {
        when(rawRepoConnector.fetchRecordCollection(any(RecordId.class))).thenThrow(new RawRepoException());

        final HarvestOperation harvestOperation = getHarvestOperation();
        harvestOperation.execute();

        verify(rawRepoConnector, times(1)).queueFail(any(QueueJob.class), anyString());
    }

    @Test
    public void execute_rawRepoConnectorFetchRecordCollectionThrowsMarcXMergerException_recordIsFailed() throws SQLException, RawRepoException, MarcXMergerException, HarvesterException {
        when(rawRepoConnector.fetchRecordCollection(any(RecordId.class))).thenThrow(new MarcXMergerException());

        final HarvestOperation harvestOperation = getHarvestOperation();
        harvestOperation.execute();

        verify(rawRepoConnector, times(1)).queueFail(any(QueueJob.class), anyString());
    }

    @Test
    public void execute_rawRepoRecordHasInvalidXmlContent_recordIsFailed() throws HarvesterException, SQLException, RawRepoException, MarcXMergerException {
        final Record rrRecord = mock(Record.class);
        when(rawRepoConnector.fetchRecordCollection(any(RecordId.class)))
                .thenReturn(new HashMap<String, Record>() {{
                    put("ID", rrRecord);
                }});

        when(rrRecord.getContent()).thenReturn("invalid".getBytes());

        final HarvestOperation harvestOperation = getHarvestOperation();
        harvestOperation.execute();

        verify(rawRepoConnector, times(1)).queueFail(any(QueueJob.class), anyString());
    }

    @Test
    public void execute_rawRepoRecordHasNoCreationDate_recordIsFailed() throws RawRepoException, SQLException, MarcXMergerException, HarvesterException {
        final MockedRecord rrRecord = new MockedRecord(RECORD_ID, true);
        rrRecord.setCreated(null);
        rrRecord.setContent(getRecordContent(RECORD_ID).getBytes(StandardCharsets.UTF_8));
        when(rawRepoConnector.fetchRecordCollection(any(RecordId.class)))
                .thenReturn(new HashMap<String, Record>() {{
                    put(RECORD_ID.getBibliographicRecordId(), rrRecord);
                }});

        final HarvestOperation harvestOperation = getHarvestOperation();
        harvestOperation.execute();

        verify(rawRepoConnector, times(1)).queueFail(any(QueueJob.class), anyString());
    }

    @Test
    public void execute_rawRepoReturnsEmptyCollection_recordIsFailed() throws RawRepoException, SQLException, MarcXMergerException, HarvesterException {
        when(rawRepoConnector.fetchRecordCollection(any(RecordId.class)))
                .thenReturn(Collections.<String, Record>emptyMap());

        final HarvestOperation harvestOperation = getHarvestOperation();
        harvestOperation.execute();

        verify(rawRepoConnector, times(1)).queueFail(any(QueueJob.class), anyString());
    }

    @Test
    public void execute_rawRepoReturnsCollectionWithoutBibliographicRecordId_recordIsFailed() throws RawRepoException, SQLException, MarcXMergerException, HarvesterException {
        final MockedRecord rrRecord = new MockedRecord(RECORD_ID, true);
        rrRecord.setContent(getRecordContent(RECORD_ID).getBytes(StandardCharsets.UTF_8));
        when(rawRepoConnector.fetchRecordCollection(any(RecordId.class)))
                .thenReturn(new HashMap<String, Record>() {{
                    put("unexpectedBibliographicRecordId", rrRecord);
                }});

        final HarvestOperation harvestOperation = getHarvestOperation();
        harvestOperation.execute();

        verify(rawRepoConnector, times(1)).queueFail(any(QueueJob.class), anyString());
    }

    @Test
    public void execute_harvesterJobBuilderThrowsHarvesterException_throws() throws HarvesterException {
        when(harvesterJobBuilder.build()).thenThrow(new HarvesterException("DIED"));

        final HarvestOperation harvestOperation = getHarvestOperation();
        try {
            harvestOperation.execute();
            fail("No exception thrown");
        } catch (HarvesterException e) {
        }
    }

    @Test
    public void execute_rawRepoRecordHasAgencyIdMatchingCommunityLibraryNumber_recordIsSkipped()
            throws RawRepoException, SQLException, MarcXMergerException, HarvesterException {
        final RecordId recordId = new RecordId("record", HarvestOperation.COMMUNITY_LIBRARY_NUMBER);
        final String recordContent = getRecordContent(recordId);
        final QueueJob queueJob = getQueueJob(recordId);
        final Record record = new MockedRecord(recordId, true);
        record.setContent(recordContent.getBytes(StandardCharsets.UTF_8));

        when(rawRepoConnector.dequeue(anyString()))
                .thenReturn(queueJob)
                .thenReturn(null);

        final HarvestOperation harvestOperation = getHarvestOperation();
        harvestOperation.execute();

        verify(rawRepoConnector, times(0)).fetchRecordCollection(any(RecordId.class));
    }

    @Test
    public void getJobSpecificationTemplate_interpolatesConfigValues() {
        final int agencyId = 424242;
        final JobSpecification expectedJobSpecificationTemplate = getJobSpecificationTemplateBuilder()
                .setSubmitterId(agencyId)
                .build();

        final RawRepoHarvesterConfig.Entry config = HarvesterTestUtil.getHarvestOperationConfigEntry()
                .setConsumerId("consumerId")
                .setFormat(expectedJobSpecificationTemplate.getFormat())
                .setDestination(expectedJobSpecificationTemplate.getDestination());
        final HarvestOperation harvestOperation = getHarvestOperation(config);

        assertThat(harvestOperation.getJobSpecificationTemplate(agencyId), is(expectedJobSpecificationTemplate));
    }

    @Test
    public void getJobSpecificationTemplate_interpolatesConfigWithFormatOverrides() {
        final int agencyId = 424242;
        final String consumerId = "rrConsumer";
        final String formatOverride = "alternativeFormat";
        final JobSpecification expectedJobSpecificationTemplate = getJobSpecificationTemplateBuilder()
                .setSubmitterId(agencyId)
                .setFormat(formatOverride)
                .build();

        final RawRepoHarvesterConfig.Entry config = HarvesterTestUtil.getHarvestOperationConfigEntry()
                .setConsumerId(consumerId)
                .setDestination(expectedJobSpecificationTemplate.getDestination())
                .setFormat("format")
                .setFormatOverride(agencyId, formatOverride);
        final HarvestOperation harvestOperation = getHarvestOperation(config);

        assertThat(harvestOperation.getJobSpecificationTemplate(agencyId), is(expectedJobSpecificationTemplate));
    }

    @Test
    public void getJobSpecificationTemplate_harvestOperationConfigEntryTypeIsSetToTransientAsDefault() {
        final int agencyId = 424242;
        final RawRepoHarvesterConfig.Entry config = HarvesterTestUtil.getHarvestOperationConfigEntry();
        final HarvestOperation harvestOperation = getHarvestOperation(config);
        assertThat(harvestOperation.getJobSpecificationTemplate(agencyId).getType(), is(JobSpecification.Type.TRANSIENT));
    }

    @Test
    public void getJobSpecificationTemplate_harvestOperationConfigEntryTypeCanBeChangedFromDefault() {
        final int agencyId = 424242;
        final RawRepoHarvesterConfig.Entry config = HarvesterTestUtil.getHarvestOperationConfigEntry()
                .setType(JobSpecification.Type.TEST);
        final HarvestOperation harvestOperation = getHarvestOperation(config);
        assertThat(harvestOperation.getJobSpecificationTemplate(agencyId).getType(), is(JobSpecification.Type.TEST));
    }

    @Test
    public void getRawRepoConnector_noOpenAgencyTargetIsConfigured_usesFallbackAgencySearchOrder() {
        try {
            final RawRepoHarvesterConfig.Entry config = HarvesterTestUtil.getHarvestOperationConfigEntry();
            InMemoryInitialContextFactory.bind(config.getResource(), mock(DataSource.class));

            final HarvestOperation harvestOperation = new HarvestOperation(config, harvesterJobBuilderFactory);
            final RawRepoConnector rawRepoConnector = harvestOperation.getRawRepoConnector(config);
            assertThat(rawRepoConnector.getAgencySearchOrder() instanceof AgencySearchOrderFallback, is(true));
        } finally {
            InMemoryInitialContextFactory.clear();
        }
    }

    @Test
    public void getRawRepoConnector_openAgencyTargetIsConfigured_usesShowOrderAgencySearchOrder() throws MalformedURLException {
        try {
            final OpenAgencyTarget openAgencyTarget = new OpenAgencyTarget();
            openAgencyTarget.setUrl(new URL("http://test.dbc.dk/oa"));

            final RawRepoHarvesterConfig.Entry config = HarvesterTestUtil.getHarvestOperationConfigEntry();
            config.setOpenAgencyTarget(openAgencyTarget);
            InMemoryInitialContextFactory.bind(config.getResource(), mock(DataSource.class));

            final HarvestOperation harvestOperation = new HarvestOperation(config, harvesterJobBuilderFactory);
            final RawRepoConnector rawRepoConnector = harvestOperation.getRawRepoConnector(config);
            assertThat(rawRepoConnector.getAgencySearchOrder() instanceof AgencySearchOrderFromShowOrder, is(true));
        } finally {
            InMemoryInitialContextFactory.clear();
        }
    }

    @Test
    public void getRawRepoConnector_openAgencyTargetIsConfiguredWithAuthentication_usesShowOrderAgencySearchOrder() throws MalformedURLException {
        try {
            final OpenAgencyTarget openAgencyTarget = new OpenAgencyTarget();
            openAgencyTarget.setUrl(new URL("http://test.dbc.dk/oa"));
            openAgencyTarget.setGroup("groupId");
            openAgencyTarget.setUser("userId");
            openAgencyTarget.setPassword("passw0rd");

            final RawRepoHarvesterConfig.Entry config = HarvesterTestUtil.getHarvestOperationConfigEntry();
            config.setOpenAgencyTarget(openAgencyTarget);
            InMemoryInitialContextFactory.bind(config.getResource(), mock(DataSource.class));

            final HarvestOperation harvestOperation = new HarvestOperation(config, harvesterJobBuilderFactory);
            final RawRepoConnector rawRepoConnector = harvestOperation.getRawRepoConnector(config);
            assertThat(rawRepoConnector.getAgencySearchOrder() instanceof AgencySearchOrderFromShowOrder, is(true));
        } finally {
            InMemoryInitialContextFactory.clear();
        }
    }

    private HarvestOperation getHarvestOperation(RawRepoHarvesterConfig.Entry config) {
        return new ClassUnderTest(config, harvesterJobBuilderFactory);
    }

    private HarvestOperation getHarvestOperation() {
        return getHarvestOperation(HarvesterTestUtil.getHarvestOperationConfigEntry());
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

    public static QueueJob getQueueJob(RecordId recordId) {
        return new MockedQueueJob(recordId.getBibliographicRecordId(), recordId.getAgencyId(), "QUEUE_ID",
                new Timestamp(new Date().getTime()));
    }

    public static String getRecordContent(RecordId recordId) {
        return
        "<marcx:collection xmlns:marcx=\"info:lc/xmlns/marcxchange-v1\">" +
            "<marcx:record format=\"danMARC2\">" +
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

    private class ClassUnderTest extends HarvestOperation {
        public ClassUnderTest(RawRepoHarvesterConfig.Entry config, HarvesterJobBuilderFactory harvesterJobBuilderFactory) {
            super(config, harvesterJobBuilderFactory);
        }
        @Override
        RawRepoConnector getRawRepoConnector(RawRepoHarvesterConfig.Entry config) {
            return rawRepoConnector;
        }
    }
}