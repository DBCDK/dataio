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

package dk.dbc.dataio.harvester.ush.solr;

import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.dataio.bfs.api.BinaryFile;
import dk.dbc.dataio.bfs.api.BinaryFileStore;
import dk.dbc.dataio.commons.types.Constants;
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
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
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
    private final FileStoreServiceConnector fileStoreServiceConnector = mock(FileStoreServiceConnector.class);
    private final JobStoreServiceConnector jobStoreServiceConnector = mock(JobStoreServiceConnector.class);
    private final JobInfoSnapshot jobInfoSnapshot = mock(JobInfoSnapshot.class);
    private final String fileId = "42";
    private final AddiRecord addiRecord = newAddiRecord();
    private final JobSpecification jobSpecificationTemplate =
            new JobSpecification("packaging", "format", "encoding", "destination", 42, "placeholder", "placeholder", "placeholder", "placeholder", JobSpecification.Type.TEST);

    @Before
    public void setupMocks() throws FileStoreServiceConnectorException, JobStoreServiceConnectorException {
        when(binaryFileStore.getBinaryFile(any(Path.class))).thenReturn(binaryFile);
        when(binaryFile.openOutputStream()).thenReturn(os);
        when(binaryFile.openInputStream()).thenReturn(is);
        when(fileStoreServiceConnector.addFile(is)).thenReturn(fileId);
        when(jobStoreServiceConnector.addJob(any(JobInputStream.class))).thenReturn(jobInfoSnapshot);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_binaryFileStoreArgIsNull_throws() throws HarvesterException {
        new HarvesterJobBuilder(null, fileStoreServiceConnector, jobStoreServiceConnector, jobSpecificationTemplate);
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
        assertThat(this::newHarvesterJobBuilder, isThrowing(HarvesterException.class));
    }

    @Test
    public void addHarvesterRecord_incrementsRecordCounter() throws HarvesterException {
        final HarvesterJobBuilder harvesterJobBuilder = newHarvesterJobBuilder();
        assertThat(harvesterJobBuilder.getRecordsAdded(), is(0));
        harvesterJobBuilder.addRecord(addiRecord);
        assertThat(harvesterJobBuilder.getRecordsAdded(), is(1));
    }

    @Test
    public void addHarvesterRecord_writesToDataFile() throws HarvesterException, IOException {
        final HarvesterJobBuilder harvesterJobBuilder = newHarvesterJobBuilder();
        harvesterJobBuilder.addRecord(addiRecord);
        verify(os).write(addiRecord.getBytes());
    }

    @Test
    public void addHarvesterRecord_addiRecordArgIsNull_throws() throws HarvesterException, IOException {
        final HarvesterJobBuilder harvesterJobBuilder = newHarvesterJobBuilder();
        assertThat(() -> harvesterJobBuilder.addRecord(null), isThrowing(NullPointerException.class));
    }

    @Test
    public void build_closingOfOutputStreamThrowsIOException_throws() throws IOException, HarvesterException {
        doThrow(new IOException()).when(os).close();

        final HarvesterJobBuilder harvesterJobBuilder = newHarvesterJobBuilder();
        assertThat(() -> harvesterJobBuilder.build(), isThrowing(HarvesterException.class));
    }

    @Test
    public void build_noHarvesterRecordsAdded_noJobIsCreated()
            throws HarvesterException, FileStoreServiceConnectorException, JobStoreServiceConnectorException {
        final HarvesterJobBuilder harvesterJobBuilder = newHarvesterJobBuilder();
        assertThat(harvesterJobBuilder.build(), is(Optional.empty()));
        verify(fileStoreServiceConnector, times(0)).addFile(any(InputStream.class));
        verify(jobStoreServiceConnector, times(0)).addJob(any(JobInputStream.class));
    }

    @Test
    public void build_uploadOfDataFileThrowsFileStoreServiceConnectorException_throws()
            throws FileStoreServiceConnectorException, HarvesterException, JobStoreServiceConnectorException {
        when(fileStoreServiceConnector.addFile(is)).thenThrow(new FileStoreServiceConnectorException("DIED"));

        final HarvesterJobBuilder harvesterJobBuilder = newHarvesterJobBuilder();
        harvesterJobBuilder.addRecord(addiRecord);
        assertThat(() -> harvesterJobBuilder.build(), isThrowing(HarvesterException.class));
        verify(jobStoreServiceConnector, times(0)).addJob(any(JobInputStream.class));
    }

    @Test
    public void build_closingOfFileStoreServiceInputStreamThrowsIOException_noExceptionThrown()
            throws FileStoreServiceConnectorException, HarvesterException, JobStoreServiceConnectorException, IOException {
        doThrow(new IOException()).when(is).close();

        final HarvesterJobBuilder harvesterJobBuilder = newHarvesterJobBuilder();
        harvesterJobBuilder.addRecord(addiRecord);
        harvesterJobBuilder.build();
        verify(jobStoreServiceConnector, times(1)).addJob(any(JobInputStream.class));
    }

    @Test
    public void build_creationOfJobThrowsJobStoreServiceConnectorException_deletesUploadedFileAndThrows()
            throws JobStoreServiceConnectorException, HarvesterException, FileStoreServiceConnectorException {
        when(jobStoreServiceConnector.addJob(any(JobInputStream.class))).thenThrow(new JobStoreServiceConnectorException("DIED"));

        final HarvesterJobBuilder harvesterJobBuilder = newHarvesterJobBuilder();
        harvesterJobBuilder.addRecord(addiRecord);

        assertThat(() -> harvesterJobBuilder.build(), isThrowing(HarvesterException.class));
        verify(fileStoreServiceConnector).deleteFile(fileId);
    }

    @Test
    public void build_creationOfFileStoreUrnThrowsIllegalArgumentException_throws() throws FileStoreServiceConnectorException, HarvesterException {
        when(fileStoreServiceConnector.addFile(is)).thenReturn("\\");

        final HarvesterJobBuilder harvesterJobBuilder = newHarvesterJobBuilder();
        harvesterJobBuilder.addRecord(addiRecord);
        assertThat(() -> harvesterJobBuilder.build(), isThrowing(IllegalArgumentException.class));
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

        final HarvesterJobBuilder harvesterJobBuilder = newHarvesterJobBuilder();
        harvesterJobBuilder.close();
    }

    @Test
    public void build_harvesterRecordsAdded_createsJobUsingJobSpecificationTemplate()
            throws FileStoreServiceConnectorException, HarvesterException, JobStoreServiceConnectorException {
        final MockedJobStoreServiceConnector mockedJobStoreServiceConnector = new MockedJobStoreServiceConnector();
        mockedJobStoreServiceConnector.jobInfoSnapshots.add(jobInfoSnapshot);

        final HarvesterJobBuilder harvesterJobBuilder = new HarvesterJobBuilder(binaryFileStore, fileStoreServiceConnector,
                mockedJobStoreServiceConnector, jobSpecificationTemplate);
        harvesterJobBuilder.addRecord(addiRecord);
        harvesterJobBuilder.build();

        verifyJobSpecification(mockedJobStoreServiceConnector.jobInputStreams.remove().getJobSpecification(),
                jobSpecificationTemplate);
    }

    @Test
    public void close_closingOfOutputStreamThrowsIOException_throws() throws IOException, HarvesterException {
        doThrow(new IOException()).when(os).close();

        final HarvesterJobBuilder harvesterJobBuilder = newHarvesterJobBuilder();
        assertThat(harvesterJobBuilder::build, isThrowing(HarvesterException.class));
    }

    private HarvesterJobBuilder newHarvesterJobBuilder() throws HarvesterException {
        return new HarvesterJobBuilder(binaryFileStore, fileStoreServiceConnector, jobStoreServiceConnector, jobSpecificationTemplate);
    }

    private AddiRecord newAddiRecord() {
        return new AddiRecord("meta".getBytes(StandardCharsets.UTF_8), "content".getBytes(StandardCharsets.UTF_8));
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
}