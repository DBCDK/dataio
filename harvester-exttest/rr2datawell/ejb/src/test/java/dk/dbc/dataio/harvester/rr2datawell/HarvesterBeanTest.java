package dk.dbc.dataio.harvester.rr2datawell;

import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnectorException;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.utils.jobstore.HarvesterJobBuilder;
import dk.dbc.dataio.harvester.utils.jobstore.HarvesterJobBuilderFactoryBean;
import dk.dbc.dataio.harvester.utils.rawrepo.RawRepoConnectorBean;
import dk.dbc.marcxmerge.MarcXMergerException;
import dk.dbc.rawrepo.MockedQueueJob;
import dk.dbc.rawrepo.MockedRecord;
import dk.dbc.rawrepo.QueueJob;
import dk.dbc.rawrepo.RawRepoException;
import dk.dbc.rawrepo.Record;
import dk.dbc.rawrepo.RecordId;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import javax.ejb.SessionContext;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;

import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class HarvesterBeanTest {
    private final static RecordId RECORD_ID = new RecordId("record", HarvesterBean.COMMUNITY_LIBRARY_NUMBER);
    private final static String RECORD_CONTENT = asRecordContent(RECORD_ID);
    private final static Record RECORD = new MockedRecord(RECORD_ID, true);
    private final static QueueJob QUEUE_JOB = asQueueJob(RECORD_ID);

    static {
        RECORD.setContent(RECORD_CONTENT.getBytes(StandardCharsets.UTF_8));
    }

    private RawRepoConnectorBean repoConnectorBean = mock(RawRepoConnectorBean.class);
    private SessionContext sessionContext = mock(SessionContext.class);
    private HarvesterJobBuilderFactoryBean harvesterJobBuilderFactoryBean = mock(HarvesterJobBuilderFactoryBean.class);
    private HarvesterJobBuilder harvesterJobBuilder = mock(HarvesterJobBuilder.class);

    @Before
    public void setupMocks() throws SQLException, FileStoreServiceConnectorException, JobStoreServiceConnectorException, RawRepoException, MarcXMergerException, HarvesterException {
        when(repoConnectorBean.dequeue(anyString()))
                .thenReturn(QUEUE_JOB)
                .thenReturn(null);
        when(repoConnectorBean.fetchRecordCollection(any(RecordId.class)))
                .thenReturn(new HashMap<String, Record>() {{
                    put(RECORD_ID.getBibliographicRecordId(), RECORD);
                }});
        when(harvesterJobBuilderFactoryBean.newHarvesterJobBuilder(any(JobSpecification.class))).thenReturn(harvesterJobBuilder);
    }

    @Test
    public void harvestBatch_repoConnectorBeanDequeueThrowsSqlException_throws() throws SQLException, RawRepoException {
        when(repoConnectorBean.dequeue(anyString())).thenThrow(new SQLException());

        final HarvesterBean harvesterBean = getInitializedBean();
        try {
            harvesterBean.harvestBatch();
            fail("No exception thrown");
        } catch (HarvesterException e) {
        }
    }

    @Test
    public void harvestBatch_repoConnectorBeanDequeueThrowsRawRepoException_throws() throws SQLException, RawRepoException {
        when(repoConnectorBean.dequeue(anyString())).thenThrow(new RawRepoException());

        final HarvesterBean harvesterBean = getInitializedBean();
        try {
            harvesterBean.harvestBatch();
            fail("No exception thrown");
        } catch (HarvesterException e) {
        }
    }

    @Test
    public void harvestBatch_repoConnectorBeanQueueSuccessThrowsSqlException_throws() throws SQLException, RawRepoException {
        doThrow(new SQLException()).when(repoConnectorBean).queueSuccess(QUEUE_JOB);

        final HarvesterBean harvesterBean = getInitializedBean();
        try {
            harvesterBean.harvestBatch();
            fail("No exception thrown");
        } catch (HarvesterException e) {
        }
    }

    @Test
    public void harvestBatch_repoConnectorBeanQueueSuccessThrowsRawRepoException_throws() throws SQLException, RawRepoException {
        doThrow(new RawRepoException()).when(repoConnectorBean).queueSuccess(QUEUE_JOB);

        final HarvesterBean harvesterBean = getInitializedBean();
        try {
            harvesterBean.harvestBatch();
            fail("No exception thrown");
        } catch (HarvesterException e) {
        }
    }

    @Test
    public void harvestBatch_repoConnectorBeanQueueFailThrowsSqlException_throws() throws SQLException, RawRepoException, MarcXMergerException {
        final Record rrRecord = mock(Record.class);
        when(repoConnectorBean.fetchRecordCollection(any(RecordId.class)))
                .thenReturn(new HashMap<String, Record>(){{
                    put("ID", rrRecord);}});
        when(rrRecord.getContent()).thenReturn("invalid".getBytes());
        doThrow(new SQLException()).when(repoConnectorBean).queueFail(any(QueueJob.class), anyString());

        final HarvesterBean harvesterBean = getInitializedBean();
        try {
            harvesterBean.harvestBatch();
            fail("No exception thrown");
        } catch (HarvesterException e) {
        }
    }

    @Test
    public void harvestBatch_repoConnectorBeanQueueFailThrowsRawRepoException_throws() throws SQLException, RawRepoException, MarcXMergerException {
        final Record rrRecord = mock(Record.class);
        when(repoConnectorBean.fetchRecordCollection(any(RecordId.class)))
                .thenReturn(new HashMap<String, Record>(){{
                    put("ID", rrRecord);}});
        when(rrRecord.getContent()).thenReturn("invalid".getBytes());
        doThrow(new RawRepoException()).when(repoConnectorBean).queueFail(any(QueueJob.class), anyString());

        final HarvesterBean harvesterBean = getInitializedBean();
        try {
            harvesterBean.harvestBatch();
            fail("No exception thrown");
        } catch (HarvesterException e) {
        }
    }

    @Test
    public void harvestBatch_repoConnectorBeanFetchRecordCollectionThrowsSqlException_recordIsFailed() throws SQLException, RawRepoException, MarcXMergerException, HarvesterException {
        when(repoConnectorBean.fetchRecordCollection(any(RecordId.class))).thenThrow(new SQLException());

        final HarvesterBean harvesterBean = getInitializedBean();
        harvesterBean.harvestBatch();

        verify(repoConnectorBean, times(1)).queueFail(any(QueueJob.class), anyString());
    }

    @Test
    public void harvestBatch_repoConnectorBeanFetchRecordCollectionThrowsRawRepoException_recordIsFailed() throws SQLException, RawRepoException, MarcXMergerException, HarvesterException {
        when(repoConnectorBean.fetchRecordCollection(any(RecordId.class))).thenThrow(new RawRepoException());

        final HarvesterBean harvesterBean = getInitializedBean();
        harvesterBean.harvestBatch();

        verify(repoConnectorBean, times(1)).queueFail(any(QueueJob.class), anyString());
    }

    @Test
    public void harvestBatch_repoConnectorBeanFetchRecordCollectionThrowsMarcXMergerException_recordIsFailed() throws SQLException, RawRepoException, MarcXMergerException, HarvesterException {
        when(repoConnectorBean.fetchRecordCollection(any(RecordId.class))).thenThrow(new MarcXMergerException());

        final HarvesterBean harvesterBean = getInitializedBean();
        harvesterBean.harvestBatch();

        verify(repoConnectorBean, times(1)).queueFail(any(QueueJob.class), anyString());
    }

    @Test
    public void harvestBatch_rawrepoRecordHasInvalidXmlContent_recordIsFailed() throws HarvesterException, SQLException, RawRepoException, MarcXMergerException {
        final Record rrRecord = mock(Record.class);
        when(repoConnectorBean.fetchRecordCollection(any(RecordId.class)))
                .thenReturn(new HashMap<String, Record>(){{
                    put("ID", rrRecord);}});
        when(rrRecord.getContent()).thenReturn("invalid".getBytes());

        final HarvesterBean harvesterBean = getInitializedBean();
        harvesterBean.harvestBatch();

        verify(repoConnectorBean, times(1)).queueFail(any(QueueJob.class), anyString());
    }

    @Test
    public void harvestBatch_recordHasNoCreationDate_recordIsFailed() throws RawRepoException, SQLException, MarcXMergerException, HarvesterException {
        final MockedRecord rrRecord = new MockedRecord(RECORD_ID, true);
        rrRecord.setCreated(null);
        rrRecord.setContent(asRecordContent(RECORD_ID).getBytes(StandardCharsets.UTF_8));
        when(repoConnectorBean.fetchRecordCollection(any(RecordId.class)))
                .thenReturn(new HashMap<String, Record>() {{
                    put(RECORD_ID.getBibliographicRecordId(), rrRecord);
                }});
        final HarvesterBean harvesterBean = getInitializedBean();
        harvesterBean.harvestBatch();

        verify(repoConnectorBean, times(1)).queueFail(any(QueueJob.class), anyString());
    }

    @Test
    public void harvestBatch_rawrepoReturnsEmptyCollection_recordIsFailed() throws RawRepoException, SQLException, MarcXMergerException, HarvesterException {
        when(repoConnectorBean.fetchRecordCollection(any(RecordId.class)))
                .thenReturn(Collections.<String, Record>emptyMap());

        final HarvesterBean harvesterBean = getInitializedBean();
        harvesterBean.harvestBatch();

        verify(repoConnectorBean, times(1)).queueFail(any(QueueJob.class), anyString());
    }

    @Test
    public void harvestBatch_rawrepoReturnsCollectionWithoutBibliographicRecordId_recordIsFailed() throws RawRepoException, SQLException, MarcXMergerException, HarvesterException {
        final MockedRecord rrRecord = new MockedRecord(RECORD_ID, true);
        rrRecord.setContent(asRecordContent(RECORD_ID).getBytes(StandardCharsets.UTF_8));
        when(repoConnectorBean.fetchRecordCollection(any(RecordId.class)))
                .thenReturn(new HashMap<String, Record>() {{
                    put("unexpectedBibliographicRecordId", rrRecord);
                }});
        final HarvesterBean harvesterBean = getInitializedBean();
        harvesterBean.harvestBatch();

        verify(repoConnectorBean, times(1)).queueFail(any(QueueJob.class), anyString());
    }

    @Test
    public void harvestBatch_harvesterJobBuilderThrowsHarvesterException_throws() throws HarvesterException {
        when(harvesterJobBuilder.build()).thenThrow(new HarvesterException("DIED"));

        final HarvesterBean harvesterBean = getInitializedBean();
        try {
            harvesterBean.harvestBatch();
        } catch (HarvesterException e) {
        }
    }

    @Test
    public void harvest_harvesterJobBuilderThrowsHarvesterException_throws() throws HarvesterException {
        when(harvesterJobBuilder.build()).thenThrow(new HarvesterException("DIED"));

        final HarvesterBean harvesterBean = getInitializedBean();
        try {
            harvesterBean.harvest();
        } catch (HarvesterException e) {
        }
    }

    @Test
    public void harvest_queueSizeExceedsBatchSize_multipleInvocationsOfHarvestBatch() throws HarvesterException {
        final HarvesterBean harvesterBean = getInitializedBean();
        doReturn(HarvesterBean.HARVEST_BATCH_SIZE).
        doReturn(HarvesterBean.HARVEST_BATCH_SIZE - 1).
                when(harvesterBean).harvestBatch();
        harvesterBean.harvest();

        verify(harvesterBean, times(2)).harvestBatch();
    }

    private HarvesterBean getInitializedBean() {
        final HarvesterBean harvesterBean = Mockito.spy(new HarvesterBean());
        harvesterBean.init();
        harvesterBean.rawRepoConnector = repoConnectorBean;
        harvesterBean.harvesterJobBuilderFactoryBean = harvesterJobBuilderFactoryBean;
        harvesterBean.sessionContext = sessionContext;

        when(sessionContext.getBusinessObject(HarvesterBean.class)).thenReturn(harvesterBean);
        return harvesterBean;
    }

    public static QueueJob asQueueJob(RecordId recordId) {
        return new MockedQueueJob(recordId.getBibliographicRecordId(), recordId.getAgencyId(), HarvesterBean.RAW_REPO_CONSUMER_ID,
                new Timestamp(new Date().getTime()));
    }

    public static String asRecordContent(RecordId recordId) {
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
}