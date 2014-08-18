package dk.dbc.dataio.harvester.rr2datawell;

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
import dk.dbc.rawrepo.MockedQueueJob;
import dk.dbc.rawrepo.MockedRecord;
import dk.dbc.rawrepo.QueueJob;
import dk.dbc.rawrepo.Record;
import dk.dbc.rawrepo.RecordId;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class HarvesterBeanTest {
    private final static RecordId RECORD_ID = new RecordId("record", HarvesterBean.LIBRARY_NUMBER_870970);
    private final static String RECORD_CONTENT = asRcordContent(RECORD_ID);
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
    private Record rrRecord = mock(Record.class);
    private JobInfo jobInfo = mock(JobInfo.class);

    @Before
    public void setupMocks() throws SQLException, FileStoreServiceConnectorException, JobStoreServiceConnectorException {
        when(binaryFileStoreBean.getBinaryFile(any(Path.class))).thenReturn(binaryFile);
        when(binaryFile.openInputStream()).thenReturn(is);
        when(binaryFile.openOutputStream()).thenReturn(os);
        when(repoConnectorBean.dequeue(anyString()))
                .thenReturn(QUEUE_JOB)
                .thenReturn(null);
        when(repoConnectorBean.fetchRecordCollection(any(RecordId.class)))
                .thenReturn(new HashSet<>(Arrays.asList(rrRecord)));
        when(rrRecord.getContent()).thenReturn(RECORD.getContent());
        when(fileStoreServiceConnector.addFile(is)).thenReturn(FILE_ID);
        when(jobStoreServiceConnector.createJob(any(JobSpecification.class))).thenReturn(jobInfo);
    }

    @Test
    public void harvest_repoConnectorBeanDequeueThrowsSqlException_throws() throws SQLException {
        when(repoConnectorBean.dequeue(anyString())).thenThrow(new SQLException());

        final HarvesterBean harvesterBean = getInitializedBean();
        try {
            harvesterBean.harvest();
        } catch (HarvesterException e) {
        }
    }

    @Test
    public void harvest_repoConnectorBeanQueueSuccessThrowsSqlException_throws() throws SQLException {
        doThrow(new SQLException()).when(repoConnectorBean).queueSuccess(QUEUE_JOB);

        final HarvesterBean harvesterBean = getInitializedBean();
        try {
            harvesterBean.harvest();
        } catch (HarvesterException e) {
        }
    }

    @Test
    public void harvest_repoConnectorBeanFetchRecordThrowsSqlException_throws() throws SQLException {
        when(repoConnectorBean.fetchRecord(any(RecordId.class))).thenThrow(new SQLException());

        final HarvesterBean harvesterBean = getInitializedBean();
        try {
            harvesterBean.harvest();
        } catch (HarvesterException e) {
        }
    }

    @Test
    public void harvest_rawrepoRecordHasInvalidXmlContent_recordIsIgnored() throws HarvesterException {
        when(rrRecord.getContent()).thenReturn("invalid".getBytes());

        final HarvesterBean harvesterBean = getInitializedBean();
        harvesterBean.harvest();
    }

    @Test
    public void harvest_openingOfOutputStreamThrowsIllegalStateException_throws() throws HarvesterException {
        when(binaryFile.openOutputStream()).thenThrow(new IllegalStateException("died"));

        final HarvesterBean harvesterBean = getInitializedBean();
        try {
            harvesterBean.harvest();
        } catch (IllegalStateException e) {
        }
    }

    @Test
    public void harvest_closingOfOutputStreamThrowsIOException_throws() throws IOException, HarvesterException {
        doThrow(new IOException()).when(os).close();

        final HarvesterBean harvesterBean = getInitializedBean();
        try {
            harvesterBean.harvest();
        } catch (HarvesterException e) {
        }
    }

    @Test
    public void harvest_writingOfOutputStreamThrowsIOException_throws() throws IOException, HarvesterException {
        doThrow(new IOException()).when(os).write(any(byte[].class), anyInt(), anyInt());

        final HarvesterBean harvesterBean = getInitializedBean();
        try {
            harvesterBean.harvest();
        } catch (HarvesterException e) {
        }
    }

    @Test
    public void harvest_openingOfInputStreamThrowsIllegalStateException_throws() throws HarvesterException {
        when(binaryFile.openInputStream()).thenThrow(new IllegalStateException("died"));

        final HarvesterBean harvesterBean = getInitializedBean();
        try {
            harvesterBean.harvest();
        } catch (IllegalStateException e) {
        }
    }

    @Test
    public void harvest_closingOfInputStreamThrowsIOException_ignored() throws IOException, HarvesterException {
        doThrow(new IOException()).when(is).close();

        final HarvesterBean harvesterBean = getInitializedBean();
        harvesterBean.harvest();
    }

    @Test
    public void harvest_fileStoreServiceConnectorThrowsFileStoreServiceConnectorException_throws() throws HarvesterException, FileStoreServiceConnectorException {
        when(fileStoreServiceConnector.addFile(is)).thenThrow(new FileStoreServiceConnectorException("died"));

        final HarvesterBean harvesterBean = getInitializedBean();
        try {
            harvesterBean.harvest();
        } catch (HarvesterException e) {
        }
    }

    @Test
    public void harvest_deletionOfTmpFileThrowsIllegalStateException_ignored() throws HarvesterException {
        doThrow(new IllegalStateException()).when(binaryFile).delete();

        final HarvesterBean harvesterBean = getInitializedBean();
        harvesterBean.harvest();
    }

    @Test
    public void harvest_noDataToHarvest_noJobIsCreated() throws SQLException, HarvesterException, JobStoreServiceConnectorException {
        when(repoConnectorBean.dequeue(anyString())).thenReturn(null);

        final HarvesterBean harvesterBean = getInitializedBean();
        harvesterBean.harvest();

        verify(jobStoreServiceConnector, times(0)).createJob(any(JobSpecification.class));
    }

    @Test
    public void harvest_dataToHarvest_jobIsCreated() throws HarvesterException, JobStoreServiceConnectorException {
        final HarvesterBean harvesterBean = getInitializedBean();
        harvesterBean.harvest();

        verify(jobStoreServiceConnector, times(1)).createJob(any(JobSpecification.class));
    }

    @Test
    public void harvest_jobCreationThrowsJobStoreServiceConnectorException_throws() throws HarvesterException, JobStoreServiceConnectorException {
        when(jobStoreServiceConnector.createJob(any(JobSpecification.class))).thenThrow(new JobStoreServiceConnectorException("died"));

        final HarvesterBean harvesterBean = getInitializedBean();
        try {
            harvesterBean.harvest();
        } catch (HarvesterException e) {
        }
    }

    @Test
    public void harvest_CreationOfFileStoreUrnThrowsURISyntaxException_throws() throws FileStoreServiceConnectorException, HarvesterException {
        when(fileStoreServiceConnector.addFile(is)).thenReturn("  ");

        final HarvesterBean harvesterBean = getInitializedBean();
        try {
            harvesterBean.harvest();
        } catch (HarvesterException e) {
        }
    }

    private HarvesterBean getInitializedBean() {
        final HarvesterBean harvesterBean = new HarvesterBean();
        harvesterBean.init();
        harvesterBean.binaryFileStore = binaryFileStoreBean;
        harvesterBean.fileStoreServiceConnector = fileStoreServiceConnector;
        harvesterBean.jobStoreServiceConnector = jobStoreServiceConnector;
        harvesterBean.rawRepoConnector = repoConnectorBean;
        return harvesterBean;
    }

    public static QueueJob asQueueJob(RecordId recordId) {
        return new MockedQueueJob(recordId.getId(), recordId.getLibrary(), HarvesterBean.RAW_REPO_CONSUMER_ID,
                new Timestamp(new Date().getTime()));
    }

    public static String asRcordContent(RecordId recordId) {
        return
        "<marcx:collection xmlns:marcx=\"info:lc/xmlns/marcxchange-v1\">" +
            "<marcx:record format=\"danMARC2\">" +
                "<marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"001\">" +
                    "<marcx:subfield code=\"a\">" + recordId.getId() + "</marcx:subfield>" +
                    "<marcx:subfield code=\"b\">" + recordId.getLibrary() + "</marcx:subfield>" +
                "</marcx:datafield>" +
                "<marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"245\">" +
                    "<marcx:subfield code=\"a\">title</marcx:subfield>" +
                "</marcx:datafield>" +
            "</marcx:record>" +
        "</marcx:collection>";
    }
}