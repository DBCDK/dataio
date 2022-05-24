/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.dataio.harvester.oai;

import dk.dbc.commons.addi.AddiReader;
import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.bfs.api.BinaryFileStoreFsImpl;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.commons.types.AddiMetaData;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnector;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnectorException;
import dk.dbc.dataio.filestore.service.connector.MockedFileStoreServiceConnector;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.OaiHarvesterConfig;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.jobstore.types.JobInputStream;
import dk.dbc.oai.OaiConnector;
import dk.dbc.oai.OaiConnectorException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.openarchives.oai.Header;
import org.openarchives.oai.ListRecords;
import org.openarchives.oai.Metadata;
import org.openarchives.oai.Record;
import org.openarchives.oai.ResumptionToken;
import org.openarchives.oai.Status;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class HarvestOperationTest {
    private FlowStoreServiceConnector flowStoreServiceConnector;
    private JobStoreServiceConnector jobStoreServiceConnector;
    private FileStoreServiceConnector fileStoreServiceConnector;
    private OaiConnector oaiConnector;
    private Path harvesterTmpFile;
    private ZonedDateTime serverCurrentTime =
            ZonedDateTime.of(2019, 05, 27, 12, 0, 1, 0, ZoneId.of("UTC"));
    private JSONBContext jsonbContext = new JSONBContext();

    @Rule
    public TemporaryFolder tmpFolder = new TemporaryFolder();

    @Before
    public void setupMocks() throws IOException, JobStoreServiceConnectorException, OaiConnectorException {
        // Intercept harvester data files with mocked FileStoreServiceConnectorBean
        harvesterTmpFile = tmpFolder.newFile().toPath();
        fileStoreServiceConnector = new MockedFileStoreServiceConnector();
        ((MockedFileStoreServiceConnector) fileStoreServiceConnector)
                .destinations.add(harvesterTmpFile);

        jobStoreServiceConnector = mock(JobStoreServiceConnector.class);
        when(jobStoreServiceConnector.addJob(any(JobInputStream.class)))
                .thenReturn(new JobInfoSnapshot());

        flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);

        oaiConnector = mock(OaiConnector.class);
        when(oaiConnector.getServerCurrentTime())
                .thenReturn(serverCurrentTime);
        when(oaiConnector.listRecords(any(OaiConnector.Params.class)))
                .thenReturn(new ListRecords());
    }

    @Test
    public void splitIntoTimeIntervals()
            throws HarvesterException, OaiConnectorException, FlowStoreServiceConnectorException {
        // test config should result in 2 intervals
        final OaiHarvesterConfig config = newConfig();
        config.getContent().withTimeOfLastHarvest(Date.from(
                serverCurrentTime.minus(3, ChronoUnit.HOURS).toInstant()));

        newHarvestOperation(config).execute();

        verify(oaiConnector, times(2)).listRecords(any(OaiConnector.Params.class));

        // Updated config is pushed
        verify(flowStoreServiceConnector).updateHarvesterConfig(config);
        final Duration between = Duration.between(
                config.getContent().getTimeOfLastHarvest().toInstant(), serverCurrentTime);
        assertThat("duration between server current time and last harvest",
                between.getSeconds(), is(30L));
    }

    @Test
    public void exhaustResumptionTokens() throws HarvesterException, OaiConnectorException {
        final Header header = new Header();
        header.setIdentifier("recordId");
        final Record record = new Record();
        record.setHeader(header);

        final ResumptionToken resumptionToken1 = new ResumptionToken();
        resumptionToken1.setValue("firstResumption");
        final ListRecords listRecords1 = new ListRecords();
        listRecords1.setResumptionToken(resumptionToken1);
        listRecords1.getRecords().add(record);

        final ResumptionToken resumptionToken2 = new ResumptionToken();
        resumptionToken2.setValue("secondResumption");
        final ListRecords listRecords2 = new ListRecords();
        listRecords2.setResumptionToken(resumptionToken2);
        listRecords2.getRecords().add(record);

        final ListRecords listRecords3 = new ListRecords();
        listRecords3.getRecords().add(record);

        when(oaiConnector.listRecords(any(OaiConnector.Params.class)))
                .thenReturn(listRecords1)
                .thenReturn(listRecords2)
                .thenReturn(listRecords3);

        final OaiHarvesterConfig config = newConfig();
        newHarvestOperation(config).execute();

        verify(oaiConnector, times(3)).listRecords(any(OaiConnector.Params.class));
    }

    @Test
    public void breakOnMaxBatchSizeExceeded()
            throws HarvesterException, OaiConnectorException,
                   FileStoreServiceConnectorException, FlowStoreServiceConnectorException {
        final Header header = new Header();
        header.setIdentifier("recordId");
        final Record record = new Record();
        record.setHeader(header);

        final ListRecords listRecords1 = new ListRecords();
        listRecords1.getRecords().add(record);
        listRecords1.getRecords().add(record);

        final ListRecords listRecords2 = new ListRecords();
        listRecords2.getRecords().add(record);

        when(oaiConnector.listRecords(any(OaiConnector.Params.class)))
                .thenReturn(listRecords1)
                .thenReturn(listRecords2);

        // test config should result in 2 intervals
        final OaiHarvesterConfig config = newConfig();
        config.getContent().withTimeOfLastHarvest(Date.from(
                serverCurrentTime.minus(3, ChronoUnit.HOURS).toInstant()));
        when(flowStoreServiceConnector.updateHarvesterConfig(config)).thenReturn(config);

        fileStoreServiceConnector = mock(FileStoreServiceConnector.class);
        final HarvestOperation harvestOperation = newHarvestOperation(config);

        // Set a small batch size so that a break is forced
        // between time intervals
        HarvestOperation.HARVEST_MAX_BATCH_SIZE = 2;

        harvestOperation.execute();

        verify(oaiConnector, times(2)).listRecords(any(OaiConnector.Params.class));
        verify(fileStoreServiceConnector, times(2)).addFile(any(InputStream.class));
    }

    @Test
    public void produceAddiRecords() throws OaiConnectorException, HarvesterException, IOException {
        final Header header1 = new Header();
        header1.setIdentifier("record1");
        final Metadata metadata1 = new Metadata();
        metadata1.setAny(toElement("<content1/>"));
        final Record record1 = new Record();
        record1.setHeader(header1);
        record1.setMetadata(metadata1);

        final Header header2 = new Header();
        header2.setIdentifier("record2");
        header2.setStatus(Status.DELETED);
        final Record record2 = new Record();
        record2.setHeader(header2);

        final ListRecords listRecords = new ListRecords();
        listRecords.getRecords().add(record1);
        listRecords.getRecords().add(record2);

        when(oaiConnector.listRecords(any(OaiConnector.Params.class)))
                .thenReturn(listRecords);

        final List<AddiMetaData> addiMetadataExpectations = new ArrayList<>();
        addiMetadataExpectations.add(new AddiMetaData()
                .withSubmitterNumber(123456)
                .withFormat("test-format")
                .withBibliographicRecordId("record1")
                .withDeleted(false));

        final OaiHarvesterConfig config = newConfig();
        newHarvestOperation(config).execute();

        final AddiReader addiReader = new AddiReader(
                new BufferedInputStream(
                        new FileInputStream(harvesterTmpFile.toFile())));

        final AddiRecord addi1 = addiReader.getNextRecord();
        assertThat("1st addi metadata", toAddiMetaData(addi1.getMetaData()),
                is(new AddiMetaData()
                        .withTrackingId("oai.test.record1")
                        .withSubmitterNumber(123456)
                        .withFormat("test-format")
                        .withBibliographicRecordId("record1")
                        .withLibraryRules(new AddiMetaData.LibraryRules())));
        assertThat("1st addi content", new String(addi1.getContentData()),
                is("<content1/>"));

        final AddiRecord addi2 = addiReader.getNextRecord();
        assertThat("2nd addi metadata", toAddiMetaData(addi2.getMetaData()),
                is(new AddiMetaData()
                        .withTrackingId("oai.test.record2")
                        .withSubmitterNumber(123456)
                        .withFormat("test-format")
                        .withBibliographicRecordId("record2")
                        .withDeleted(true)
                        .withLibraryRules(new AddiMetaData.LibraryRules())));
        assertThat("2nd addi content", addi2.getContentData(),
                is(new byte[0]));
    }

    private HarvestOperation newHarvestOperation(OaiHarvesterConfig config) {
        final HarvestOperation harvestOperation;
        try {
            harvestOperation = new HarvestOperation(config,
                    new BinaryFileStoreFsImpl(tmpFolder.newFolder().toPath()),
                    flowStoreServiceConnector,
                    fileStoreServiceConnector,
                    jobStoreServiceConnector,
                    oaiConnector);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        HarvestOperation.HARVEST_MAX_BATCH_SIZE = 10000;
        return harvestOperation;
    }

    private OaiHarvesterConfig newConfig() {
        final OaiHarvesterConfig config = new OaiHarvesterConfig(
                1, 2, new OaiHarvesterConfig.Content());
        config.getContent()
                .withId("test")
                .withFormat("test-format")
                .withDestination("test-destination")
                .withSubmitterNumber("123456")
                .withTimeOfLastHarvest(Date.from(
                        serverCurrentTime.minus(1, ChronoUnit.HOURS).toInstant()));
        return config;
    }

    private Element toElement(String xmlString) {
        try {
            final DocumentBuilder documentBuilder =
                    DocumentBuilderFactory.newInstance().newDocumentBuilder();
            final Document document = documentBuilder.parse(
                    new ByteArrayInputStream(xmlString.getBytes(StandardCharsets.UTF_8)));
            return document.getDocumentElement();
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private AddiMetaData toAddiMetaData(byte[] bytes) {
        try {
            return jsonbContext.unmarshall(
                    new String(bytes, StandardCharsets.UTF_8),
                    AddiMetaData.class);
        } catch (JSONBException e) {
            throw new IllegalStateException(e);
        }
    }
}
