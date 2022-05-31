package dk.dbc.dataio.harvester.utils.harvesterjobbuilder;

import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.dataio.bfs.api.BinaryFile;
import dk.dbc.dataio.bfs.api.BinaryFileStore;
import dk.dbc.dataio.commons.types.Constants;
import dk.dbc.dataio.commons.types.FileStoreUrn;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.commons.utils.jobstore.MockedJobStoreServiceConnector;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnector;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnectorException;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.jobstore.types.JobInputStream;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Optional;

import static dk.dbc.commons.testutil.Assert.assertThat;
import static dk.dbc.commons.testutil.Assert.isThrowing;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AbstractHarvesterJobBuilderTest {

    private final OutputStream os = mock(OutputStream.class);
    private final InputStream is = mock(InputStream.class);
    private final BinaryFile binaryFile = mock(BinaryFile.class);
    private final BinaryFileStore binaryFileStore = mock(BinaryFileStore.class);
    private final FileStoreServiceConnector fileStoreServiceConnector = mock(FileStoreServiceConnector.class);
    private final JobStoreServiceConnector jobStoreServiceConnector = mock(JobStoreServiceConnector.class);
    private final JobInfoSnapshot jobInfoSnapshot = mock(JobInfoSnapshot.class);
    private final String fileId = "42";
    private final AddiRecord addiRecord = newAddiRecord();
    private final JobSpecification jobSpecificationTemplate = getJobSpecificationTemplate();

    @Before
    public void setupMocks() throws FileStoreServiceConnectorException, JobStoreServiceConnectorException {
        when(binaryFileStore.getBinaryFile(any(Path.class))).thenReturn(binaryFile);
        when(binaryFile.openOutputStream()).thenReturn(os);
        when(binaryFile.openInputStream()).thenReturn(is);
        when(fileStoreServiceConnector.addFile(is)).thenReturn(fileId);
        when(jobStoreServiceConnector.addJob(any(JobInputStream.class))).thenReturn(jobInfoSnapshot);
    }

    @Test
    public void constructor_binaryFileStoreArgIsNull_throws() {
        assertThat(() -> new AbstractHarvesterJobBuilderImpl(null, fileStoreServiceConnector, jobStoreServiceConnector, jobSpecificationTemplate),
                isThrowing(NullPointerException.class));
    }

    @Test
    public void constructor_jobStoreServiceConnectorArgIsNull_throws() {
        assertThat(() -> new AbstractHarvesterJobBuilderImpl(binaryFileStore, fileStoreServiceConnector, null, jobSpecificationTemplate),
                isThrowing(NullPointerException.class));
    }

    @Test
    public void constructor_jobSpecificationTemplateArgIsNull_throws() {
        assertThat(() -> new AbstractHarvesterJobBuilderImpl(binaryFileStore, fileStoreServiceConnector, jobStoreServiceConnector, null),
                isThrowing(NullPointerException.class));
    }

    @Test
    public void constructor_openingOfOutputStreamThrowsIllegalStateException_throws() {
        when(binaryFile.openOutputStream()).thenThrow(new IllegalStateException("died"));
        assertThat(this::newHarvesterJobBuilder, isThrowing(HarvesterException.class));
    }

    @Test
    public void addHarvesterRecord_incrementsRecordCounter() throws HarvesterException {
        final AbstractHarvesterJobBuilder abstractHarvesterJobBuilder = newHarvesterJobBuilder();
        assertThat(abstractHarvesterJobBuilder.getRecordsAdded(), is(0));
        abstractHarvesterJobBuilder.addRecord(addiRecord);
        assertThat(abstractHarvesterJobBuilder.getRecordsAdded(), is(1));
    }

    @Test
    public void addHarvesterRecord_writesToDataFile() throws HarvesterException, IOException {
        final AbstractHarvesterJobBuilder abstractHarvesterJobBuilder = newHarvesterJobBuilder();
        abstractHarvesterJobBuilder.addRecord(addiRecord);
        verify(os).write(addiRecord.getBytes());
    }

    @Test
    public void addHarvesterRecord_addiRecordArgIsNull_throws() throws HarvesterException {
        final AbstractHarvesterJobBuilder abstractHarvesterJobBuilder = newHarvesterJobBuilder();
        assertThat(() -> abstractHarvesterJobBuilder.addRecord(null), isThrowing(NullPointerException.class));
    }

    @Test
    public void close_deletesTmpFile() throws HarvesterException {
        try (AbstractHarvesterJobBuilder abstractHarvesterJobBuilder = newHarvesterJobBuilder()) {
        }
        verify(binaryFile, times(1)).delete();
    }

    @Test
    public void close_deletionOfTmpFileThrowsIllegalStateException_noExceptionThrown() throws HarvesterException {
        doThrow(new IllegalStateException()).when(binaryFile).delete();

        final AbstractHarvesterJobBuilder abstractHarvesterJobBuilder = newHarvesterJobBuilder();
        abstractHarvesterJobBuilder.close();
    }

    @Test
    public void build_closingOfOutputStreamThrowsIOException_throws() throws IOException, HarvesterException {
        doThrow(new IOException()).when(os).close();

        final AbstractHarvesterJobBuilder harvesterJobBuilder = newHarvesterJobBuilder();
        assertThat(() -> harvesterJobBuilder.build(), isThrowing(HarvesterException.class));
    }

    @Test
    public void build_noHarvesterRecordsAdded_noJobIsCreated()
            throws HarvesterException, FileStoreServiceConnectorException, JobStoreServiceConnectorException {
        final AbstractHarvesterJobBuilder harvesterJobBuilder = newHarvesterJobBuilder();
        assertThat(harvesterJobBuilder.build(), is(Optional.empty()));
        verify(fileStoreServiceConnector, times(0)).addFile(any(InputStream.class));
        verify(jobStoreServiceConnector, times(0)).addJob(any(JobInputStream.class));
    }

    @Test
    public void build_uploadOfDataFileThrowsFileStoreServiceConnectorException_throws()
            throws FileStoreServiceConnectorException, HarvesterException, JobStoreServiceConnectorException {
        when(fileStoreServiceConnector.addFile(is)).thenThrow(new FileStoreServiceConnectorException("DIED"));

        final AbstractHarvesterJobBuilder harvesterJobBuilder = newHarvesterJobBuilder();
        harvesterJobBuilder.addRecord(addiRecord);
        assertThat(() -> harvesterJobBuilder.build(), isThrowing(HarvesterException.class));
        verify(jobStoreServiceConnector, times(0)).addJob(any(JobInputStream.class));
    }

    @Test
    public void build_closingOfFileStoreServiceInputStreamThrowsIOException_noExceptionThrown()
            throws FileStoreServiceConnectorException, HarvesterException, JobStoreServiceConnectorException, IOException {
        doThrow(new IOException()).when(is).close();

        final AbstractHarvesterJobBuilder harvesterJobBuilder = newHarvesterJobBuilder();
        harvesterJobBuilder.addRecord(addiRecord);
        harvesterJobBuilder.build();
        verify(jobStoreServiceConnector, times(1)).addJob(any(JobInputStream.class));
    }

    @Test
    public void build_creationOfJobThrowsJobStoreServiceConnectorException_deletesUploadedFileAndThrows()
            throws JobStoreServiceConnectorException, HarvesterException, FileStoreServiceConnectorException {
        when(jobStoreServiceConnector.addJob(any(JobInputStream.class))).thenThrow(new JobStoreServiceConnectorException("DIED"));

        final AbstractHarvesterJobBuilder harvesterJobBuilder = newHarvesterJobBuilder();
        harvesterJobBuilder.addRecord(addiRecord);

        assertThat(() -> harvesterJobBuilder.build(), isThrowing(HarvesterException.class));
        verify(fileStoreServiceConnector).deleteFile(fileId);
    }

    @Test
    public void build_creationOfFileStoreUrnThrowsIllegalArgumentException_throws() throws FileStoreServiceConnectorException, HarvesterException {
        when(fileStoreServiceConnector.addFile(is)).thenReturn("\\");

        final AbstractHarvesterJobBuilder harvesterJobBuilder = newHarvesterJobBuilder();
        harvesterJobBuilder.addRecord(addiRecord);
        assertThat(() -> harvesterJobBuilder.build(), isThrowing(IllegalArgumentException.class));
    }

    @Test
    public void build_harvesterRecordsAdded_createsJobUsingJobSpecificationTemplate()
            throws FileStoreServiceConnectorException, HarvesterException, JobStoreServiceConnectorException {
        final MockedJobStoreServiceConnector mockedJobStoreServiceConnector = new MockedJobStoreServiceConnector();
        mockedJobStoreServiceConnector.jobInfoSnapshots.add(jobInfoSnapshot);

        final AbstractHarvesterJobBuilderImpl harvesterJobBuilder = new AbstractHarvesterJobBuilderImpl(binaryFileStore, fileStoreServiceConnector,
                mockedJobStoreServiceConnector, jobSpecificationTemplate);
        harvesterJobBuilder.addRecord(addiRecord);
        harvesterJobBuilder.build();

        verifyJobSpecification(mockedJobStoreServiceConnector.jobInputStreams.remove().getJobSpecification(),
                jobSpecificationTemplate);
    }

    /*
     * private methods
     */


    private AddiRecord newAddiRecord() {
        return new AddiRecord("meta".getBytes(StandardCharsets.UTF_8), "content".getBytes(StandardCharsets.UTF_8));
    }

    private AbstractHarvesterJobBuilder newHarvesterJobBuilder() throws HarvesterException {
        return new AbstractHarvesterJobBuilderImpl(binaryFileStore, fileStoreServiceConnector, jobStoreServiceConnector, jobSpecificationTemplate);
    }

    private void verifyJobSpecification(JobSpecification jobSpecification, JobSpecification jobSpecificationTemplate) {
        assertThat("packaging", jobSpecification.getPackaging(), is(jobSpecificationTemplate.getPackaging()));
        assertThat("format", jobSpecification.getFormat(), is(jobSpecificationTemplate.getFormat()));
        assertThat("charset", jobSpecification.getCharset(), is(jobSpecificationTemplate.getCharset()));
        assertThat("destination", jobSpecification.getDestination(), is(jobSpecificationTemplate.getDestination()));
        assertThat("submitter", jobSpecification.getSubmitterId(), is(jobSpecificationTemplate.getSubmitterId()));
        assertThat("mailForNotificationAboutVerification", jobSpecification.getMailForNotificationAboutVerification(), is(Constants.CALL_OPEN_AGENCY));
        assertThat("mailForNotificationAboutProcessing", jobSpecification.getMailForNotificationAboutProcessing(), is(Constants.CALL_OPEN_AGENCY));
    }

    static class AbstractHarvesterJobBuilderImpl extends AbstractHarvesterJobBuilder {

        public AbstractHarvesterJobBuilderImpl(BinaryFileStore binaryFileStore, FileStoreServiceConnector fileStoreServiceConnector, JobStoreServiceConnector jobStoreServiceConnector, JobSpecification jobSpecificationTemplate) throws NullPointerException, HarvesterException {
            super(binaryFileStore, fileStoreServiceConnector, jobStoreServiceConnector, jobSpecificationTemplate);
        }

        @Override
        protected JobSpecification createJobSpecification(String fileId) {
            final FileStoreUrn fileStoreUrn = FileStoreUrn.create(fileId);
            return new JobSpecification()
                    .withDataFile(fileStoreUrn.toString())
                    .withPackaging(jobSpecificationTemplate.getPackaging())
                    .withFormat(jobSpecificationTemplate.getFormat())
                    .withCharset(jobSpecificationTemplate.getCharset())
                    .withDestination(jobSpecificationTemplate.getDestination())
                    .withSubmitterId(jobSpecificationTemplate.getSubmitterId())
                    .withMailForNotificationAboutVerification(Constants.CALL_OPEN_AGENCY)
                    .withMailForNotificationAboutProcessing(Constants.CALL_OPEN_AGENCY);
        }
    }

    public JobSpecification getJobSpecificationTemplate() {
        return new JobSpecification()
                .withPackaging("packaging")
                .withFormat("format")
                .withCharset("utf8")
                .withDestination("destination")
                .withSubmitterId(42)
                .withMailForNotificationAboutVerification("placeholder")
                .withMailForNotificationAboutProcessing("placeholder")
                .withResultmailInitials("placeholder")
                .withDataFile("placeholder")
                .withType(JobSpecification.Type.TEST);
    }
}
