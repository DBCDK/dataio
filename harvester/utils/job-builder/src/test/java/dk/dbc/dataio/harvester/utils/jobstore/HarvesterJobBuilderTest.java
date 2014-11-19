package dk.dbc.dataio.harvester.utils.jobstore;

import dk.dbc.dataio.bfs.api.BinaryFile;
import dk.dbc.dataio.bfs.api.BinaryFileStore;
import dk.dbc.dataio.commons.types.JobInfo;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.commons.utils.jobstore.MockedJobStoreServiceConnector;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnector;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnectorException;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.HarvesterXmlRecord;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class HarvesterJobBuilderTest {
    private final OutputStream os = mock(OutputStream.class);
    private final InputStream is = mock(InputStream.class);
    private final BinaryFile binaryFile = mock(BinaryFile.class);
    private final BinaryFileStore binaryFileStore = mock(BinaryFileStore.class);
    private final String fileId = "42";
    private final FileStoreServiceConnector fileStoreServiceConnector = mock(FileStoreServiceConnector.class);
    private final JobStoreServiceConnector jobStoreServiceConnector = mock(JobStoreServiceConnector.class);
    private final JobInfo jobInfo = mock(JobInfo.class);
    private final JobSpecification jobSpecificationTemplate =
            new JobSpecification("packaging", "format", "encoding", "destination", 42, "placeholder", "placeholder", "placeholder", "placeholder");

    @Before
    public void setupMocks() throws FileStoreServiceConnectorException, JobStoreServiceConnectorException {
        when(binaryFileStore.getBinaryFile(any(Path.class))).thenReturn(binaryFile);
        when(binaryFile.openOutputStream()).thenReturn(os);
        when(binaryFile.openInputStream()).thenReturn(is);
        when(fileStoreServiceConnector.addFile(is)).thenReturn(fileId);
        when(jobStoreServiceConnector.createJob(any(JobSpecification.class))).thenReturn(jobInfo);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_binaryFileStoreArgIsNull_throws() throws HarvesterException {
        new HarvesterJobBuilder(null, fileStoreServiceConnector, jobStoreServiceConnector, jobSpecificationTemplate);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_fileStoreServiceConnectorArgIsNull_throws() throws HarvesterException {
        new HarvesterJobBuilder(binaryFileStore, null, jobStoreServiceConnector, jobSpecificationTemplate);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_jobStoreServiceConnectorArgIsNull_throws() throws HarvesterException {
        new HarvesterJobBuilder(binaryFileStore, fileStoreServiceConnector, null, jobSpecificationTemplate);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_jobSpecificationTemplateArgIsNull_throws() throws HarvesterException {
        new HarvesterJobBuilder(binaryFileStore, fileStoreServiceConnector, jobStoreServiceConnector, null);
    }

    @Test
    public void constructor_openingOfOutputStreamThrowsIllegalStateException_throws() throws HarvesterException {
        when(binaryFile.openOutputStream()).thenThrow(new IllegalStateException("died"));

        try {
            newHarvesterJobBuilder();
            fail("No exception thrown");
        } catch (IllegalStateException e) {
        }
    }

    @Test
    public void addHarvesterRecord_harvesterRecordArgIsValid_incrementsRecordCounter() throws HarvesterException, IOException {
        final HarvesterJobBuilder harvesterJobBuilder = newHarvesterJobBuilder();
        assertThat(harvesterJobBuilder.getRecordsAdded(), is(0));
        harvesterJobBuilder.addHarvesterRecord(new HarvesterXmlRecordImpl("<record/>"));
        assertThat(harvesterJobBuilder.getRecordsAdded(), is(1));
    }

    @Test
    public void addHarvesterRecord_harvesterRecordArgIsValid_writesToDataFile() throws HarvesterException, IOException {
        final HarvesterJobBuilder harvesterJobBuilder = newHarvesterJobBuilder();
        harvesterJobBuilder.addHarvesterRecord(new HarvesterXmlRecordImpl("<record/>"));

        // Two writes - one for the header, and one for the record data
        verify(os, times(2)).write(any(byte[].class), anyInt(), anyInt());
    }

    @Test
    public void addHarvesterRecord_harvesterRecordArgIsInvalid_throws() throws HarvesterException, IOException {
        final HarvesterJobBuilder harvesterJobBuilder = newHarvesterJobBuilder();

        try {
            harvesterJobBuilder.addHarvesterRecord(new HarvesterXmlRecordImpl("<record/>", StandardCharsets.ISO_8859_1));
            fail("No exception thrown");
        } catch (HarvesterException e) {
        }
    }

    @Test
    public void build_closingOfOutputStreamThrowsIOException_throws() throws IOException, HarvesterException {
        doThrow(new IOException()).when(os).close();

        final HarvesterJobBuilder harvesterJobBuilder = newHarvesterJobBuilder();
        try {
            harvesterJobBuilder.build();
            fail("No exception thrown");
        } catch (HarvesterException e) {
        }
    }

    @Test
    public void build_noHarvesterRecordsAdded_noJobIsCreated()
            throws HarvesterException, JobStoreServiceConnectorException, FileStoreServiceConnectorException {
        final HarvesterJobBuilder harvesterJobBuilder = newHarvesterJobBuilder();
        assertThat(harvesterJobBuilder.build(), is(nullValue()));
        verify(fileStoreServiceConnector, times(0)).addFile(any(InputStream.class));
        verify(jobStoreServiceConnector, times(0)).createJob(any(JobSpecification.class));
    }

    @Test
    public void build_uploadingOfDataFileToFileStoreThrowsFileStoreServiceConnectorException_throws()
            throws FileStoreServiceConnectorException, HarvesterException, JobStoreServiceConnectorException {
        when(fileStoreServiceConnector.addFile(is)).thenThrow(new FileStoreServiceConnectorException("DIED"));

        final HarvesterJobBuilder harvesterJobBuilder = newHarvesterJobBuilder();
        harvesterJobBuilder.addHarvesterRecord(new HarvesterXmlRecordImpl("<record/>"));
        try {
            harvesterJobBuilder.build();
            fail("No exception thrown");
        } catch (HarvesterException e) {
        }
        verify(jobStoreServiceConnector, times(0)).createJob(any(JobSpecification.class));
    }

    @Test
    public void build_closingOfFileStoreServiceInputStreamThrowsIOException_noExceptionThrown()
            throws FileStoreServiceConnectorException, HarvesterException, JobStoreServiceConnectorException, IOException {
        doThrow(new IOException()).when(is).close();

        final HarvesterJobBuilder harvesterJobBuilder = newHarvesterJobBuilder();
        harvesterJobBuilder.addHarvesterRecord(new HarvesterXmlRecordImpl("<record/>"));
        harvesterJobBuilder.build();
        verify(jobStoreServiceConnector, times(1)).createJob(any(JobSpecification.class));
    }

    @Test
    public void build_creationOfJobThrowsJobStoreServiceConnectorException_throws()
            throws FileStoreServiceConnectorException, HarvesterException, JobStoreServiceConnectorException {
        when(jobStoreServiceConnector.createJob(any(JobSpecification.class))).thenThrow(new JobStoreServiceConnectorException("DIED"));

        final HarvesterJobBuilder harvesterJobBuilder = newHarvesterJobBuilder();
        harvesterJobBuilder.addHarvesterRecord(new HarvesterXmlRecordImpl("<record/>"));
        try {
            harvesterJobBuilder.build();
            fail("No exception thrown");
        } catch (HarvesterException e) {
        }
    }

    @Test
    public void build_creationOfFileStoreUrnThrowsURISyntaxException_throws() throws FileStoreServiceConnectorException, HarvesterException {
        when(fileStoreServiceConnector.addFile(is)).thenReturn("  ");

        final HarvesterJobBuilder harvesterJobBuilder = newHarvesterJobBuilder();
        harvesterJobBuilder.addHarvesterRecord(new HarvesterXmlRecordImpl("<record/>"));
        try {
            harvesterJobBuilder.build();
            fail("No exception thrown");
        } catch (HarvesterException e) {
        }
    }

    @Test
    public void build_harvesterRecordsAdded_createsJobUsingJobSpecificationTemplate()
            throws FileStoreServiceConnectorException, HarvesterException, JobStoreServiceConnectorException {
        final MockedJobStoreServiceConnector mockedJobStoreServiceConnector = new MockedJobStoreServiceConnector();
        mockedJobStoreServiceConnector.jobInfos.add(jobInfo);

        final HarvesterJobBuilder harvesterJobBuilder = new HarvesterJobBuilder(
                binaryFileStore, fileStoreServiceConnector, mockedJobStoreServiceConnector, jobSpecificationTemplate);
        harvesterJobBuilder.addHarvesterRecord(new HarvesterXmlRecordImpl("<record/>"));
        harvesterJobBuilder.build();

        verifyJobSpecification(mockedJobStoreServiceConnector.jobSpecifications.remove(),
                jobSpecificationTemplate);
    }

    @Test
    public void close_deletesTmpFile() throws HarvesterException {
        try (HarvesterJobBuilder harvesterJobBuilder = newHarvesterJobBuilder()) {
        }
        verify(binaryFile, times(1)).delete();
    }

    @Test
    public void close_deletionOfTmpFileThrowsIllegalStateException_noExceptionThrown() throws HarvesterException {
        doThrow(new IllegalStateException()).when(binaryFile).delete();

        HarvesterJobBuilder harvesterJobBuilder = newHarvesterJobBuilder();
        harvesterJobBuilder.close();
    }

    @Test
    public void close_closingOfOutputStreamThrowsIOException_throws() throws IOException, HarvesterException {
        doThrow(new IOException()).when(os).close();

        final HarvesterJobBuilder harvesterJobBuilder = newHarvesterJobBuilder();
        try {
            harvesterJobBuilder.close();
            fail("No exception thrown");
        } catch (IllegalStateException e) {
        }
    }

    private HarvesterJobBuilder newHarvesterJobBuilder() throws HarvesterException {
        return new HarvesterJobBuilder(binaryFileStore, fileStoreServiceConnector, jobStoreServiceConnector, jobSpecificationTemplate);
    }

    private void verifyJobSpecification(JobSpecification jobSpecification, JobSpecification jobSpecificationTemplate) {
        assertThat(jobSpecification.getPackaging(), is(jobSpecificationTemplate.getPackaging()));
        assertThat(jobSpecification.getFormat(), is(jobSpecificationTemplate.getFormat()));
        assertThat(jobSpecification.getCharset(), is(jobSpecificationTemplate.getCharset()));
        assertThat(jobSpecification.getDestination(), is(jobSpecificationTemplate.getDestination()));
        assertThat(jobSpecification.getSubmitterId(), is(jobSpecificationTemplate.getSubmitterId()));
    }

    private static class HarvesterXmlRecordImpl implements HarvesterXmlRecord {
        private final String xmlFragment;
        private final Charset charset;

        public HarvesterXmlRecordImpl(String xmlFragment) {
            this(xmlFragment, StandardCharsets.UTF_8);
        }

        public HarvesterXmlRecordImpl(String xmlFragment, Charset charset) {
            this.xmlFragment = xmlFragment;
            this.charset = charset;
        }

        @Override
        public byte[] asBytes() throws HarvesterException {
            return xmlFragment.getBytes();
        }

        @Override
        public Document asDocument() throws HarvesterException {
            return null;
        }

        @Override
        public Charset getCharset() {
            return charset;
        }
    }
}