package dk.dbc.dataio.harvester.rr2fbs;

import dk.dbc.dataio.bfs.api.BinaryFile;
import dk.dbc.dataio.bfs.ejb.BinaryFileStoreBean;
import dk.dbc.dataio.commons.types.JobInfo;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.commons.utils.jobstore.ejb.JobStoreServiceConnectorBean;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnectorException;
import dk.dbc.dataio.filestore.service.connector.ejb.FileStoreServiceConnectorBean;
import dk.dbc.dataio.harvester.types.HarvesterException;
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
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;

import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class HarvesterBeanTest {
    private final static RecordId RECORD_ID = new RecordId("record", 12345678);
    private final static String RECORD_CONTENT = asRecordContent(RECORD_ID);
    private final static Record RECORD = new MockedRecord(RECORD_ID, true);
    private final static QueueJob QUEUE_JOB = asQueueJob(RECORD_ID);
    private final static String FILE_ID = "1234";

    static {
        RECORD.setContent(RECORD_CONTENT.getBytes(StandardCharsets.UTF_8));
    }

    private BinaryFile binaryFile = mock(BinaryFile.class);
    private OutputStream os = mock(OutputStream.class);
    private InputStream is = mock(InputStream.class);
    private BinaryFileStoreBean binaryFileStoreBean = mock(BinaryFileStoreBean.class);
    private FileStoreServiceConnectorBean fileStoreServiceConnector = mock(FileStoreServiceConnectorBean.class);
    private JobStoreServiceConnectorBean jobStoreServiceConnector = mock(JobStoreServiceConnectorBean.class);
    private RawRepoConnectorBean repoConnectorBean = mock(RawRepoConnectorBean.class);
    private JobInfo jobInfo = mock(JobInfo.class);
    private SessionContext sessionContext = mock(SessionContext.class);

    @Before
    public void setupMocks() throws SQLException, FileStoreServiceConnectorException, JobStoreServiceConnectorException, RawRepoException, MarcXMergerException {
        when(binaryFileStoreBean.getBinaryFile(any(Path.class))).thenReturn(binaryFile);
        when(binaryFile.openInputStream()).thenReturn(is);
        when(binaryFile.openOutputStream()).thenReturn(os);
        when(repoConnectorBean.dequeue(anyString()))
                .thenReturn(QUEUE_JOB)
                .thenReturn(null);
        when(repoConnectorBean.fetchRecordCollection(any(RecordId.class)))
                .thenReturn(new HashMap<String, Record>(){{
                    put(RECORD_ID.toString(), RECORD);
                }});
        when(fileStoreServiceConnector.addFile(is)).thenReturn(FILE_ID);
        when(jobStoreServiceConnector.createJob(any(JobSpecification.class))).thenReturn(jobInfo);
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
                    put("ID", rrRecord);
                }});
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
                .thenReturn(new HashMap<String, Record>() {{
                    put("ID", rrRecord);
                }});
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
                .thenReturn(new HashMap<String, Record>() {{
                    put("ID", rrRecord);
                }});

        when(rrRecord.getContent()).thenReturn("invalid".getBytes());

        final HarvesterBean harvesterBean = getInitializedBean();
        harvesterBean.harvestBatch();

        verify(repoConnectorBean, times(1)).queueFail(any(QueueJob.class), anyString());
    }

    @Test
    public void harvestBatch_openingOfOutputStreamThrowsIllegalStateException_throws() throws HarvesterException {
        when(binaryFile.openOutputStream()).thenThrow(new IllegalStateException("died"));

        final HarvesterBean harvesterBean = getInitializedBean();
        try {
            harvesterBean.harvestBatch();
            fail("No exception thrown");
        } catch (IllegalStateException e) {
        }
    }

    @Test
    public void harvestBatch_closingOfOutputStreamThrowsIllegalStateException_throws() throws IOException, HarvesterException {
        doThrow(new IOException()).when(os).close();

        final HarvesterBean harvesterBean = getInitializedBean();
        try {
            harvesterBean.harvestBatch();
            fail("No exception thrown");
        } catch (HarvesterException e) {
        }
    }

    @Test
    public void harvestBatch_writingOfOutputStreamThrowsIOException_throws() throws IOException, HarvesterException {
        doThrow(new IOException()).when(os).write(any(byte[].class), anyInt(), anyInt());

        final HarvesterBean harvesterBean = getInitializedBean();
        try {
            harvesterBean.harvestBatch();
            fail("No exception thrown");
        } catch (HarvesterException e) {
        }
    }

    @Test
    public void harvestBatch_openingOfInputStreamThrowsIllegalStateException_throws() throws HarvesterException {
        when(binaryFile.openInputStream()).thenThrow(new IllegalStateException("died"));

        final HarvesterBean harvesterBean = getInitializedBean();
        try {
            harvesterBean.harvestBatch();
            fail("No exception thrown");
        } catch (IllegalStateException e) {
        }
    }

    @Test
    public void harvestBatch_closingOfInputStreamThrowsIOException_ignored() throws IOException, HarvesterException {
        doThrow(new IOException()).when(is).close();

        final HarvesterBean harvesterBean = getInitializedBean();
        harvesterBean.harvestBatch();
    }

    @Test
    public void harvestBatch_fileStoreServiceConnectorThrowsFileStoreServiceConnectorException_throws() throws HarvesterException, FileStoreServiceConnectorException {
        when(fileStoreServiceConnector.addFile(is)).thenThrow(new FileStoreServiceConnectorException("died"));

        final HarvesterBean harvesterBean = getInitializedBean();
        try {
            harvesterBean.harvestBatch();
            fail("No exception thrown");
        } catch (HarvesterException e) {
        }
    }

    @Test
    public void harvestBatch_deletionOfTmpFileThrowsIllegalStateException_ignored() throws HarvesterException {
        doThrow(new IllegalStateException()).when(binaryFile).delete();

        final HarvesterBean harvesterBean = getInitializedBean();
        harvesterBean.harvestBatch();
    }

    @Test
    public void harvestBatch_noDataToHarvest_noJobIsCreated() throws SQLException, HarvesterException, JobStoreServiceConnectorException, RawRepoException {
        when(repoConnectorBean.dequeue(anyString())).thenReturn(null);

        final HarvesterBean harvesterBean = getInitializedBean();
        harvesterBean.harvestBatch();

        verify(jobStoreServiceConnector, times(0)).createJob(any(JobSpecification.class));
    }

    @Test
    public void harvestBatch_dataToHarvest_jobIsCreated() throws HarvesterException, JobStoreServiceConnectorException {
        final HarvesterBean harvesterBean = getInitializedBean();
        harvesterBean.harvestBatch();

        verify(jobStoreServiceConnector, times(1)).createJob(any(JobSpecification.class));
    }

    @Test
    public void harvestBatch_jobCreationThrowsJobStoreServiceConnectorException_throws() throws HarvesterException, JobStoreServiceConnectorException {
        when(jobStoreServiceConnector.createJob(any(JobSpecification.class))).thenThrow(new JobStoreServiceConnectorException("died"));

        final HarvesterBean harvesterBean = getInitializedBean();
        try {
            harvesterBean.harvestBatch();
            fail("No exception thrown");
        } catch (HarvesterException e) {
        }
    }

    @Test
    public void harvestBatch_CreationOfFileStoreUrnThrowsURISyntaxException_throws() throws FileStoreServiceConnectorException, HarvesterException {
        when(fileStoreServiceConnector.addFile(is)).thenReturn("  ");

        final HarvesterBean harvesterBean = getInitializedBean();
        try {
            harvesterBean.harvestBatch();
            fail("No exception thrown");
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
        harvesterBean.binaryFileStore = binaryFileStoreBean;
        harvesterBean.fileStoreServiceConnector = fileStoreServiceConnector;
        harvesterBean.jobStoreServiceConnector = jobStoreServiceConnector;
        harvesterBean.rawRepoConnector = repoConnectorBean;
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