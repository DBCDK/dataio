package dk.dbc.dataio.harvester.rr;

import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.RawRepoHarvesterConfig;
import dk.dbc.dataio.harvester.utils.rawrepo.RawRepoConnector;
import dk.dbc.marcxmerge.MarcXMergerException;
import dk.dbc.rawrepo.MockedQueueJob;
import dk.dbc.rawrepo.MockedRecord;
import dk.dbc.rawrepo.QueueJob;
import dk.dbc.rawrepo.RawRepoException;
import dk.dbc.rawrepo.Record;
import dk.dbc.rawrepo.RecordId;
import org.junit.Before;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;

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
        new HarvestOperation(getHarvestOperationConfig(), null);
    }

    @Test
    public void execute_rawRepoConnectorDequeueThrowsSqlException_throws() throws SQLException, RawRepoException {
        when(rawRepoConnector.dequeue(anyString())).thenThrow(new SQLException());

        final HarvestOperation harvestOperation = getHarvestOperation(getHarvestOperationConfig());
        try {
            harvestOperation.execute();
            fail("No exception thrown");
        } catch (HarvesterException e) {
        }
    }

    @Test
    public void execute_rawRepoConnectorDequeueThrowsRawRepoException_throws() throws SQLException, RawRepoException {
        when(rawRepoConnector.dequeue(anyString())).thenThrow(new RawRepoException());

        final HarvestOperation harvestOperation = getHarvestOperation(getHarvestOperationConfig());
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

        final HarvestOperation harvestOperation = getHarvestOperation(getHarvestOperationConfig());
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

        final HarvestOperation harvestOperation = getHarvestOperation(getHarvestOperationConfig());
        try {
            harvestOperation.execute();
            fail("No exception thrown");
        } catch (HarvesterException e) {
        }
    }

    @Test
    public void execute_rawRepoConnectorFetchRecordCollectionThrowsSqlException_recordIsFailed() throws SQLException, RawRepoException, MarcXMergerException, HarvesterException {
        when(rawRepoConnector.fetchRecordCollection(any(RecordId.class))).thenThrow(new SQLException());

        final HarvestOperation harvestOperation = getHarvestOperation(getHarvestOperationConfig());
        harvestOperation.execute();

        verify(rawRepoConnector, times(1)).queueFail(any(QueueJob.class), anyString());
    }

    @Test
    public void execute_rawRepoConnectorFetchRecordCollectionThrowsRawRepoException_recordIsFailed() throws SQLException, RawRepoException, MarcXMergerException, HarvesterException {
        when(rawRepoConnector.fetchRecordCollection(any(RecordId.class))).thenThrow(new RawRepoException());

        final HarvestOperation harvestOperation = getHarvestOperation(getHarvestOperationConfig());
        harvestOperation.execute();

        verify(rawRepoConnector, times(1)).queueFail(any(QueueJob.class), anyString());
    }

    @Test
    public void execute_rawRepoConnectorFetchRecordCollectionThrowsMarcXMergerException_recordIsFailed() throws SQLException, RawRepoException, MarcXMergerException, HarvesterException {
        when(rawRepoConnector.fetchRecordCollection(any(RecordId.class))).thenThrow(new MarcXMergerException());

        final HarvestOperation harvestOperation = getHarvestOperation(getHarvestOperationConfig());
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

        final HarvestOperation harvestOperation = getHarvestOperation(getHarvestOperationConfig());
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

        final HarvestOperation harvestOperation = getHarvestOperation(getHarvestOperationConfig());
        harvestOperation.execute();

        verify(rawRepoConnector, times(1)).queueFail(any(QueueJob.class), anyString());
    }

    @Test
    public void execute_rawRepoReturnsEmptyCollection_recordIsFailed() throws RawRepoException, SQLException, MarcXMergerException, HarvesterException {
        when(rawRepoConnector.fetchRecordCollection(any(RecordId.class)))
                .thenReturn(Collections.<String, Record>emptyMap());

        final HarvestOperation harvestOperation = getHarvestOperation(getHarvestOperationConfig());
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

        final HarvestOperation harvestOperation = getHarvestOperation(getHarvestOperationConfig());
        harvestOperation.execute();

        verify(rawRepoConnector, times(1)).queueFail(any(QueueJob.class), anyString());
    }

    @Test
    public void execute_harvesterJobBuilderThrowsHarvesterException_throws() throws HarvesterException {
        when(harvesterJobBuilder.build()).thenThrow(new HarvesterException("DIED"));

        final HarvestOperation harvestOperation = getHarvestOperation(getHarvestOperationConfig());
        try {
            harvestOperation.execute();
            fail("No exception thrown");
        } catch (HarvesterException e) {
        }
    }

    private HarvestOperation getHarvestOperation(RawRepoHarvesterConfig.Entry config) {
        return new ClassUnderTest(config, harvesterJobBuilderFactory);
    }

    private RawRepoHarvesterConfig.Entry getHarvestOperationConfig() {
        return new RawRepoHarvesterConfig.Entry()
                .setId("id")
                .setResource("resource");
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
        RawRepoConnector getRawRepoConnector(String dataSourceName) {
            return rawRepoConnector;
        }
    }
}