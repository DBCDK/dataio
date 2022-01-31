package dk.dbc.dataio.harvester.dmat;

import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.bfs.api.BinaryFileStoreFsImpl;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.filestore.service.connector.MockedFileStoreServiceConnector;
import dk.dbc.dataio.harvester.types.DMatHarvesterConfig;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.utils.rawrepo.RawRepoConnector;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.jobstore.types.JobInputStream;
import dk.dbc.dmat.service.connector.DMatServiceConnector;
import dk.dbc.dmat.service.connector.DMatServiceConnectorException;
import dk.dbc.dmat.service.dto.ExportedRecordList;
import dk.dbc.dmat.service.dto.RecordData;
import dk.dbc.dmat.service.persistence.DMatRecord;
import dk.dbc.dmat.service.persistence.enums.BKMCode;
import dk.dbc.dmat.service.persistence.enums.CatalogueCode;
import dk.dbc.dmat.service.persistence.enums.MaterialType;
import dk.dbc.dmat.service.persistence.enums.ReviewCode;
import dk.dbc.dmat.service.persistence.enums.Selection;
import dk.dbc.dmat.service.persistence.enums.Status;
import dk.dbc.dmat.service.persistence.enums.UpdateCode;
import dk.dbc.rawrepo.record.RecordServiceConnector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.verification.VerificationMode;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class HarvestOperationTest {
    private JobStoreServiceConnector jobStoreServiceConnector;
    private MockedFileStoreServiceConnector fileStoreServiceConnector;
    private FlowStoreServiceConnector flowStoreServiceConnector;
    private DMatServiceConnector dmatServiceConnector;
    private RawRepoConnector rawRepoConnector;
    private Path harvesterTmpFile;
    private final RecordServiceConnector recordServiceConnector = mock(RecordServiceConnector.class);

    @TempDir Path tempDir;

    @BeforeEach
    void setupMocks() throws Exception {
        // Intercept harvester data files with mocked FileStoreServiceConnectorBean
        harvesterTmpFile = tempDir.resolve("harvester.dat");
        fileStoreServiceConnector = new MockedFileStoreServiceConnector();
        fileStoreServiceConnector.destinations.add(harvesterTmpFile);

        jobStoreServiceConnector = mock(JobStoreServiceConnector.class);
        when(jobStoreServiceConnector.addJob(any(JobInputStream.class)))
                .thenReturn(new JobInfoSnapshot());

        flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        dmatServiceConnector = mock(DMatServiceConnector.class);

        rawRepoConnector = mock(RawRepoConnector.class);
    }

    @Test
    void noCasesToHarvest() throws HarvesterException, DMatServiceConnectorException, JobStoreServiceConnectorException,
            FlowStoreServiceConnectorException, JSONBException {

        when(dmatServiceConnector.getExportedRecords(any(HashMap.class)))
                .thenReturn(new ExportedRecordList());

        final DMatHarvesterConfig config = newConfig();
        final HarvestOperation harvestOperation = spy(newHarvestOperation(config));
        doReturn(recordServiceConnector).when(harvestOperation).createRecordServiceConnector();

        final int casesHarvested = harvestOperation.execute();
        assertThat("Number of cases harvested", casesHarvested, is(0));

        verify(dmatServiceConnector, never()).upsertRecord(any(RecordData.class));
        verify(jobStoreServiceConnector, never()).addJob(any(JobInputStream.class));
        verify(flowStoreServiceConnector).updateHarvesterConfig(any(DMatHarvesterConfig.class));
    }

    @Test
    void harvestOneRecord() throws HarvesterException, DMatServiceConnectorException, JobStoreServiceConnectorException,
            FlowStoreServiceConnectorException, JSONBException {
        LocalDateTime accession = LocalDateTime.now();

        when(dmatServiceConnector.getExportedRecords(any(HashMap.class)))
                .thenReturn((ExportedRecordList) new ExportedRecordList()
                        .withCreationDate(accession.toLocalDate())
                        .withRecords(Arrays.asList(
                                new DMatRecord().withActive(true).withAccession(accession)
                                        .withId(1)
                                        .withIsbn("123456789")
                                        .withRecordData("{\"recordReference\": \"2a69b0a2-cbdd-4afa-9dc8-9fb203732f01\"}")
                                        .withTitle("title 1")
                                        .withReviewId("456789123")
                                        .withMatch("123456789")
                                        .withRecordId("789123456")
                                        .withType(MaterialType.EBOOK)
                                        .withSelection(Selection.CREATE)
                                        .withBkmCodes(Arrays.asList(BKMCode.S, BKMCode.B))
                                        .withReviewCode(ReviewCode.R01)
                                        .withCatalogueCode(CatalogueCode.DBF)
                                        .withUpdateCode(UpdateCode.NEW)
                                        .withD08("d08 note")
                                        .withSelectedBy("HWHA")
                                        .withPromatPrimaryFaust("789123456")
                                        .withStatus(Status.PENDING_EXPORT)
                                        .withHasReview(true)
                                        .withOwnsReview(true)
                        )));

        final DMatHarvesterConfig config = newConfig();
        final HarvestOperation harvestOperation = spy(newHarvestOperation(config));
        doReturn(recordServiceConnector).when(harvestOperation).createRecordServiceConnector();

        final int casesHarvested = harvestOperation.execute();

        assertThat("Number of cases harvested", casesHarvested, is(1));

        verify(dmatServiceConnector).updateRecordStatus(1, Status.EXPORTED);
        verify(jobStoreServiceConnector).addJob(any(JobInputStream.class));
        verify(flowStoreServiceConnector).updateHarvesterConfig(any(DMatHarvesterConfig.class));
    }

    @Test
    void harvestTwoRecords() throws HarvesterException, DMatServiceConnectorException, JobStoreServiceConnectorException,
            FlowStoreServiceConnectorException, JSONBException {
        LocalDateTime accession = LocalDateTime.now();

        when(dmatServiceConnector.getExportedRecords(any(HashMap.class)))
                .thenReturn((ExportedRecordList) new ExportedRecordList()
                        .withCreationDate(accession.toLocalDate())
                        .withRecords(Arrays.asList(
                                new DMatRecord().withActive(true).withAccession(accession)
                                        .withId(1)
                                        .withIsbn("123456789")
                                        .withRecordData("{\"recordReference\": \"2a69b0a2-cbdd-4afa-9dc8-9fb203732f01\"}")
                                        .withTitle("title 1")
                                        .withReviewId("456789123")
                                        .withMatch("123456789")
                                        .withRecordId("789123456")
                                        .withType(MaterialType.EBOOK)
                                        .withSelection(Selection.CREATE)
                                        .withBkmCodes(Arrays.asList(BKMCode.S, BKMCode.B))
                                        .withReviewCode(ReviewCode.R01)
                                        .withCatalogueCode(CatalogueCode.DBF)
                                        .withUpdateCode(UpdateCode.NEW)
                                        .withD08("d08 note")
                                        .withSelectedBy("HWHA")
                                        .withPromatPrimaryFaust("789123456")
                                        .withStatus(Status.PENDING_EXPORT)
                                        .withHasReview(true)
                                        .withOwnsReview(true),
                                new DMatRecord().withActive(true).withAccession(accession)
                                        .withId(2)
                                        .withIsbn("123456789")
                                        .withRecordData("{\"recordReference\": \"2a69b0a2-cbdd-4afa-9dc8-9fb203732f01\"}")
                                        .withTitle("title 1")
                                        .withReviewId("456789123")
                                        .withMatch("123456789")
                                        .withRecordId("789123456")
                                        .withType(MaterialType.EBOOK)
                                        .withSelection(Selection.CREATE)
                                        .withBkmCodes(Arrays.asList(BKMCode.S, BKMCode.B))
                                        .withReviewCode(ReviewCode.R01)
                                        .withCatalogueCode(CatalogueCode.DBF)
                                        .withUpdateCode(UpdateCode.NEW)
                                        .withD08("d08 note")
                                        .withSelectedBy("HWHA")
                                        .withPromatPrimaryFaust("789123456")
                                        .withStatus(Status.PENDING_EXPORT)
                                        .withHasReview(true)
                                        .withOwnsReview(true)
                        ))).thenReturn(
                        (ExportedRecordList) new ExportedRecordList()
                                .withCreationDate(accession.toLocalDate())
                                .withRecords(Arrays.asList(
                                        new DMatRecord().withActive(true).withAccession(accession)
                                        .withId(3)
                                        .withIsbn("123456789")
                                        .withRecordData("{\"recordReference\": \"2a69b0a2-cbdd-4afa-9dc8-9fb203732f01\"}")
                                        .withTitle("title 1")
                                        .withReviewId("456789123")
                                        .withMatch("123456789")
                                        .withRecordId("789123456")
                                        .withType(MaterialType.EBOOK)
                                        .withSelection(Selection.CREATE)
                                        .withBkmCodes(Arrays.asList(BKMCode.S, BKMCode.B))
                                        .withReviewCode(ReviewCode.R01)
                                        .withCatalogueCode(CatalogueCode.DBF)
                                        .withUpdateCode(UpdateCode.NEW)
                                        .withD08("d08 note")
                                        .withSelectedBy("HWHA")
                                        .withPromatPrimaryFaust("789123456")
                                        .withStatus(Status.PENDING_EXPORT)
                                        .withHasReview(true)
                                        .withOwnsReview(true)
                )));

        final DMatHarvesterConfig config = newConfig();
        final HarvestOperation harvestOperation = spy(newHarvestOperation(config));
        doReturn(recordServiceConnector).when(harvestOperation).createRecordServiceConnector();

        final int casesHarvested = harvestOperation.execute();

        assertThat("Number of cases harvested", casesHarvested, is(3));

        verify(dmatServiceConnector, times(3)).updateRecordStatus(any(Integer.class), any(Status.class));
        verify(dmatServiceConnector).updateRecordStatus(1, Status.EXPORTED);
        verify(dmatServiceConnector).updateRecordStatus(2, Status.EXPORTED);
        verify(dmatServiceConnector).updateRecordStatus(3, Status.EXPORTED);

        verify(jobStoreServiceConnector).addJob(any(JobInputStream.class));
        verify(flowStoreServiceConnector).updateHarvesterConfig(any(DMatHarvesterConfig.class));
    }

    private HarvestOperation newHarvestOperation(DMatHarvesterConfig config) {
        HarvestOperation.DMAT_SERVICE_FETCH_SIZE = 2;     // force multiple iterations when dealing with >2 cases
        return new HarvestOperation(config,
                new BinaryFileStoreFsImpl(tempDir),
                fileStoreServiceConnector,
                flowStoreServiceConnector,
                jobStoreServiceConnector,
                dmatServiceConnector,
                rawRepoConnector);
    }

    private DMatHarvesterConfig newConfig() {
        final DMatHarvesterConfig config = new DMatHarvesterConfig(
                1, 2, new DMatHarvesterConfig.Content());
        config.getContent()
                .withName("HarvestOperationTest")
                .withFormat("-format-")
                .withDestination("-destination-")
                .withBaseurl("http://localhost/api/v1")
                .withResource("-resource-");
        return config;
    }
}
