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
import dk.dbc.rawrepo.record.RecordServiceConnectorException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
    private HarvestOperation harvestOperation;

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

        final DMatHarvesterConfig config = newConfig();
        harvestOperation = spy(newHarvestOperation(config));

        doReturn(recordServiceConnector).when(harvestOperation).createRecordServiceConnector();
    }

    @Test
    void noCasesToHarvest() throws HarvesterException, DMatServiceConnectorException, JobStoreServiceConnectorException,
            FlowStoreServiceConnectorException, JSONBException {

        when(dmatServiceConnector.getExportedRecords(any(HashMap.class)))
                .thenReturn(new ExportedRecordList());

        final int casesHarvested = harvestOperation.execute();
        assertThat("Number of cases harvested", casesHarvested, is(0));

        verify(dmatServiceConnector, never()).upsertRecord(any(RecordData.class));
        verify(jobStoreServiceConnector, never()).addJob(any(JobInputStream.class));
        verify(flowStoreServiceConnector).updateHarvesterConfig(any(DMatHarvesterConfig.class));
    }

    @Test
    void harvestOneRecord() throws HarvesterException, DMatServiceConnectorException, JobStoreServiceConnectorException,
            FlowStoreServiceConnectorException, JSONBException, RecordServiceConnectorException {
        LocalDateTime accession = LocalDateTime.now();

        when(dmatServiceConnector.getExportedRecords(any(HashMap.class)))
                .thenReturn((ExportedRecordList) new ExportedRecordList()
                        .withCreationDate(accession.toLocalDate())
                        .withRecords(Arrays.asList(
                                mockRecord(1, accession, UpdateCode.NEW, Selection.CREATE)
                        )));

        when(recordServiceConnector.getRecordContent(191919, MATCH_FAUST, fetchParameters()))
                .thenReturn(mockMarcXchange(MATCH_FAUST, MATCH_AGENCY));

        final int casesHarvested = harvestOperation.execute();
        assertThat("Number of cases harvested", casesHarvested, is(1));

        verify(dmatServiceConnector).updateRecordStatus(1, Status.EXPORTED);
        verify(jobStoreServiceConnector).addJob(any(JobInputStream.class));
        verify(flowStoreServiceConnector).updateHarvesterConfig(any(DMatHarvesterConfig.class));
    }

    @Test
    void harvestAllRecords() throws HarvesterException, DMatServiceConnectorException, JobStoreServiceConnectorException,
            FlowStoreServiceConnectorException, JSONBException, RecordServiceConnectorException {
        LocalDateTime accession = LocalDateTime.now();

        when(dmatServiceConnector.getExportedRecords(any(HashMap.class)))
                .thenReturn((ExportedRecordList) new ExportedRecordList()
                        .withCreationDate(accession.toLocalDate())
                        .withRecords(Arrays.asList(
                                mockRecord(1, accession, UpdateCode.NEW, Selection.CREATE),
                                mockRecord(2, accession, UpdateCode.NEW, Selection.CREATE)
                        )))
                .thenReturn((ExportedRecordList) new ExportedRecordList()
                        .withCreationDate(accession.toLocalDate())
                        .withRecords(Arrays.asList(
                                mockRecord(3, accession, UpdateCode.NEW, Selection.CREATE)
                )));

        when(recordServiceConnector.getRecordContent(191919, MATCH_FAUST, fetchParameters()))
                .thenReturn(mockMarcXchange(MATCH_FAUST, MATCH_AGENCY));

        final int casesHarvested = harvestOperation.execute();
        assertThat("Number of cases harvested", casesHarvested, is(3));

        verify(dmatServiceConnector, times(3)).updateRecordStatus(any(Integer.class), any(Status.class));
        verify(dmatServiceConnector).updateRecordStatus(1, Status.EXPORTED);
        verify(dmatServiceConnector).updateRecordStatus(2, Status.EXPORTED);
        verify(dmatServiceConnector).updateRecordStatus(3, Status.EXPORTED);

        verify(jobStoreServiceConnector).addJob(any(JobInputStream.class));
        verify(flowStoreServiceConnector).updateHarvesterConfig(any(DMatHarvesterConfig.class));
    }

    @Test
    void harvestWithMissingRrRecordThrows() throws HarvesterException, DMatServiceConnectorException, RecordServiceConnectorException {
        LocalDateTime accession = LocalDateTime.now();

        when(dmatServiceConnector.getExportedRecords(any(HashMap.class)))
                .thenReturn((ExportedRecordList) new ExportedRecordList()
                        .withCreationDate(accession.toLocalDate())
                        .withRecords(Arrays.asList(
                                mockRecord(1, accession, UpdateCode.NEW, Selection.CREATE)
                        )));

        when(recordServiceConnector.getRecordContent(191919, MATCH_FAUST, fetchParameters()))
                .thenThrow(new RecordServiceConnectorException("No content"));

        assertThrowsWithMessage(harvestOperation, 1, UpdateCode.NEW, Selection.CREATE, "Caught RecordServiceConnectorException");
    }

    @Test
    void harvestNewCreateRecord() throws HarvesterException, DMatServiceConnectorException, JobStoreServiceConnectorException,
            FlowStoreServiceConnectorException, JSONBException, RecordServiceConnectorException {
        LocalDateTime accession = LocalDateTime.now();

        when(dmatServiceConnector.getExportedRecords(any(HashMap.class)))
                .thenReturn((ExportedRecordList) new ExportedRecordList()
                        .withCreationDate(accession.toLocalDate())
                        .withRecords(Arrays.asList(
                                mockRecord(1, accession, UpdateCode.NEW, Selection.CREATE)
                        )));

        when(recordServiceConnector.getRecordContent(191919, MATCH_FAUST, fetchParameters()))
                .thenReturn(mockMarcXchange(MATCH_FAUST, MATCH_AGENCY));

        final int casesHarvested = harvestOperation.execute();
        assertThat("Number of cases harvested", casesHarvested, is(1));

        verify(dmatServiceConnector).updateRecordStatus(1, Status.EXPORTED);
        verify(recordServiceConnector, times(1)).getRecordContent(191919, MATCH_FAUST, fetchParameters());
        verify(jobStoreServiceConnector).addJob(any(JobInputStream.class));
        verify(flowStoreServiceConnector).updateHarvesterConfig(any(DMatHarvesterConfig.class));
    }

    @Test
    void harvestNewCloneRecord() throws HarvesterException, DMatServiceConnectorException, JobStoreServiceConnectorException,
            FlowStoreServiceConnectorException, JSONBException, RecordServiceConnectorException {
        LocalDateTime accession = LocalDateTime.now();

        when(dmatServiceConnector.getExportedRecords(any(HashMap.class)))
                .thenReturn((ExportedRecordList) new ExportedRecordList()
                        .withCreationDate(accession.toLocalDate())
                        .withRecords(Arrays.asList(
                                mockRecord(1, accession, UpdateCode.NEW, Selection.CLONE)
                        )));

        when(recordServiceConnector.getRecordContent(191919, MATCH_FAUST, fetchParameters()))
                .thenReturn(mockMarcXchange(MATCH_FAUST, MATCH_AGENCY));

        final int casesHarvested = harvestOperation.execute();
        assertThat("Number of cases harvested", casesHarvested, is(1));

        verify(dmatServiceConnector).updateRecordStatus(1, Status.EXPORTED);
        verify(recordServiceConnector, times(1)).getRecordContent(191919, MATCH_FAUST, fetchParameters());
        verify(jobStoreServiceConnector).addJob(any(JobInputStream.class));
        verify(flowStoreServiceConnector).updateHarvesterConfig(any(DMatHarvesterConfig.class));
    }

    @Test
    void harvestAutoCreateRecord() throws HarvesterException, DMatServiceConnectorException, JobStoreServiceConnectorException,
            FlowStoreServiceConnectorException, JSONBException, RecordServiceConnectorException {
        LocalDateTime accession = LocalDateTime.now();

        when(dmatServiceConnector.getExportedRecords(any(HashMap.class)))
                .thenReturn((ExportedRecordList) new ExportedRecordList()
                        .withCreationDate(accession.toLocalDate())
                        .withRecords(Arrays.asList(
                                mockRecord(1, accession, UpdateCode.AUTO, Selection.CREATE)
                        )));

        when(recordServiceConnector.getRecordContent(191919, MATCH_FAUST, fetchParameters()))
                .thenReturn(mockMarcXchange(MATCH_FAUST, MATCH_AGENCY));

        final int casesHarvested = harvestOperation.execute();
        assertThat("Number of cases harvested", casesHarvested, is(1));

        verify(dmatServiceConnector).updateRecordStatus(1, Status.EXPORTED);
        verify(recordServiceConnector, times(1)).getRecordContent(191919, MATCH_FAUST, fetchParameters());
        verify(jobStoreServiceConnector).addJob(any(JobInputStream.class));
        verify(flowStoreServiceConnector).updateHarvesterConfig(any(DMatHarvesterConfig.class));
    }

    @Test
    void harvestAutoCloneRecord() throws HarvesterException, DMatServiceConnectorException, JobStoreServiceConnectorException,
            FlowStoreServiceConnectorException, JSONBException, RecordServiceConnectorException {
        LocalDateTime accession = LocalDateTime.now();

        when(dmatServiceConnector.getExportedRecords(any(HashMap.class)))
                .thenReturn((ExportedRecordList) new ExportedRecordList()
                        .withCreationDate(accession.toLocalDate())
                        .withRecords(Arrays.asList(
                                mockRecord(1, accession, UpdateCode.AUTO, Selection.CLONE)
                        )));

        when(recordServiceConnector.getRecordContent(191919, MATCH_FAUST, fetchParameters()))
                .thenReturn(mockMarcXchange(MATCH_FAUST, MATCH_AGENCY));

        final int casesHarvested = harvestOperation.execute();
        assertThat("Number of cases harvested", casesHarvested, is(1));

        verify(dmatServiceConnector).updateRecordStatus(1, Status.EXPORTED);
        verify(recordServiceConnector, times(1)).getRecordContent(191919, MATCH_FAUST, fetchParameters());
        verify(jobStoreServiceConnector).addJob(any(JobInputStream.class));
        verify(flowStoreServiceConnector).updateHarvesterConfig(any(DMatHarvesterConfig.class));
    }

    @Test
    void harvestActCreateRecord() throws HarvesterException, DMatServiceConnectorException, JobStoreServiceConnectorException,
            FlowStoreServiceConnectorException, JSONBException {
        LocalDateTime accession = LocalDateTime.now();

        when(dmatServiceConnector.getExportedRecords(any(HashMap.class)))
                .thenReturn((ExportedRecordList) new ExportedRecordList()
                        .withCreationDate(accession.toLocalDate())
                        .withRecords(Arrays.asList(
                                mockRecord(1, accession, UpdateCode.ACT, Selection.CREATE)
                        )));

        final int casesHarvested = harvestOperation.execute();
        assertThat("Number of cases harvested", casesHarvested, is(1));

        verify(dmatServiceConnector).updateRecordStatus(1, Status.EXPORTED);
        verify(jobStoreServiceConnector).addJob(any(JobInputStream.class));
        verify(flowStoreServiceConnector).updateHarvesterConfig(any(DMatHarvesterConfig.class));
    }

    @Test
    void harvestNnbDropRecord() throws HarvesterException, DMatServiceConnectorException, JobStoreServiceConnectorException,
            FlowStoreServiceConnectorException, JSONBException {
        LocalDateTime accession = LocalDateTime.now();

        when(dmatServiceConnector.getExportedRecords(any(HashMap.class)))
                .thenReturn((ExportedRecordList) new ExportedRecordList()
                        .withCreationDate(accession.toLocalDate())
                        .withRecords(Arrays.asList(
                                mockRecord(1, accession, UpdateCode.ACT, Selection.CREATE)
                        )));

        final int casesHarvested = harvestOperation.execute();
        assertThat("Number of cases harvested", casesHarvested, is(1));

        verify(dmatServiceConnector).updateRecordStatus(1, Status.EXPORTED);
        verify(jobStoreServiceConnector).addJob(any(JobInputStream.class));
        verify(flowStoreServiceConnector).updateHarvesterConfig(any(DMatHarvesterConfig.class));
    }

    @Test
    void harvestNnbAutodropRecord() throws HarvesterException, DMatServiceConnectorException, JobStoreServiceConnectorException,
            FlowStoreServiceConnectorException, JSONBException {
        LocalDateTime accession = LocalDateTime.now();

        when(dmatServiceConnector.getExportedRecords(any(HashMap.class)))
                .thenReturn((ExportedRecordList) new ExportedRecordList()
                        .withCreationDate(accession.toLocalDate())
                        .withRecords(Arrays.asList(
                                mockRecord(1, accession, UpdateCode.ACT, Selection.CREATE)
                        )));

        final int casesHarvested = harvestOperation.execute();
        assertThat("Number of cases harvested", casesHarvested, is(1));

        verify(dmatServiceConnector).updateRecordStatus(1, Status.EXPORTED);
        verify(jobStoreServiceConnector).addJob(any(JobInputStream.class));
        verify(flowStoreServiceConnector).updateHarvesterConfig(any(DMatHarvesterConfig.class));
    }

    @Test
    void harvestReviewRecord() throws HarvesterException, DMatServiceConnectorException, JobStoreServiceConnectorException,
            FlowStoreServiceConnectorException, JSONBException {
        // Todo: add test
    }

    @Test
    void harvestUpdateRecord() throws HarvesterException, DMatServiceConnectorException, JobStoreServiceConnectorException,
            FlowStoreServiceConnectorException, JSONBException {
        // Todo: add test
    }

    @Test
    void harvestInvalidRecords() throws HarvesterException, DMatServiceConnectorException, JobStoreServiceConnectorException,
            FlowStoreServiceConnectorException, JSONBException {

        for(Status status : Arrays.stream(Status.values())
                .filter(s -> s != Status.PENDING_EXPORT)
                .collect(Collectors.toList())) {
            assertThrowsWithMessage(harvestOperation, 1, UpdateCode.NEW, Selection.CREATE, status, "DMatRecord 1 does not have status PENDING_EXPORT");
        }

        assertThrowsWithMessage(harvestOperation, 1, UpdateCode.NEW, Selection.NONE, "DMatRecord 1 has invalid selection NONE");
        assertThrowsWithMessage(harvestOperation, 1, UpdateCode.NONE, Selection.CREATE, "DMatRecord 1 has invalid updateCode NONE");

        assertThrowsWithMessage(harvestOperation, 1, UpdateCode.NEW, Selection.MERGE, "DMatRecord 1 has an invalid combination of updateCode and selection");
        assertThrowsWithMessage(harvestOperation, 1, UpdateCode.NEW, Selection.DROP, "DMatRecord 1 has an invalid combination of updateCode and selection");
        assertThrowsWithMessage(harvestOperation, 1, UpdateCode.NEW, Selection.AUTODROP, "DMatRecord 1 has an invalid combination of updateCode and selection");

        assertThrowsWithMessage(harvestOperation, 1, UpdateCode.AUTO, Selection.MERGE, "DMatRecord 1 has an invalid combination of updateCode and selection");
        assertThrowsWithMessage(harvestOperation, 1, UpdateCode.AUTO, Selection.DROP, "DMatRecord 1 has an invalid combination of updateCode and selection");
        assertThrowsWithMessage(harvestOperation, 1, UpdateCode.AUTO, Selection.AUTODROP, "DMatRecord 1 has an invalid combination of updateCode and selection");

        assertThrowsWithMessage(harvestOperation, 1, UpdateCode.NNB, Selection.MERGE, "DMatRecord 1 has an invalid combination of updateCode and selection");
        assertThrowsWithMessage(harvestOperation, 1, UpdateCode.NNB, Selection.CREATE, "DMatRecord 1 has an invalid combination of updateCode and selection");
        assertThrowsWithMessage(harvestOperation, 1, UpdateCode.NNB, Selection.CLONE, "DMatRecord 1 has an invalid combination of updateCode and selection");

        assertThrowsWithMessage(harvestOperation, 1, UpdateCode.ACT, Selection.MERGE, "DMatRecord 1 has an invalid combination of updateCode and selection");
        assertThrowsWithMessage(harvestOperation, 1, UpdateCode.ACT, Selection.CLONE, "DMatRecord 1 has an invalid combination of updateCode and selection");
        assertThrowsWithMessage(harvestOperation, 1, UpdateCode.ACT, Selection.DROP, "DMatRecord 1 has an invalid combination of updateCode and selection");
        assertThrowsWithMessage(harvestOperation, 1, UpdateCode.ACT, Selection.AUTODROP, "DMatRecord 1 has an invalid combination of updateCode and selection");

        verify(dmatServiceConnector, times(0)).updateRecordStatus(any(Integer.class), any(Status.class));
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

    private final String MATCH_FAUST = "1213456789";
    private final String REVIEW_FAUST = "456789123";
    private final String RECORD_FAUST = "789123456";
    private final String PROMAT_FAUST = "147258369";

    private final String MATCH_AGENCY = "870970";
    private final String REVIEW_AGENCY = "870976";
    private final String RECORD_AGENCY = "870970";
    private final String PROMAT_AGENCY = "870970";

    private DMatRecord mockRecord(Integer id, LocalDateTime accession, UpdateCode updateCode, Selection selection) {
        return mockRecord(id, accession, updateCode, selection, Status.PENDING_EXPORT);
    }

    private DMatRecord mockRecord(Integer id, LocalDateTime accession, UpdateCode updateCode, Selection selection, Status status) {
        return new DMatRecord().withActive(true).withAccession(accession)
                .withId(id)
                .withIsbn("123456789")
                .withRecordData("{\"recordReference\": \"2a69b0a2-cbdd-4afa-9dc8-9fb203732f01\"}")
                .withTitle("title 1")
                .withReviewId(REVIEW_FAUST)
                .withMatch(MATCH_FAUST)
                .withRecordId(RECORD_FAUST)
                .withType(MaterialType.EBOOK)
                .withSelection(selection)
                .withBkmCodes(Arrays.asList(BKMCode.S, BKMCode.B))
                .withReviewCode(ReviewCode.R01)
                .withCatalogueCode(CatalogueCode.DBF)
                .withUpdateCode(updateCode)
                .withD08("d08 note")
                .withSelectedBy("HWHA")
                .withPromatPrimaryFaust(PROMAT_FAUST)
                .withStatus(status)
                .withHasReview(true)
                .withOwnsReview(true);
    }

    private byte[] mockMarcXchange(String id, String agency) {
        return ("<collection xmlns='info:lc/xmlns/marcxchange-v1' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'\n" +
                "            xsi:schemaLocation='info:lc/xmlns/marcxchange-v1 http://www.loc.gov/standards/iso25577/marcxchange-1-1.xsd'>\n" +
                "    <record>\n" +
                "        <leader>00000n 2200000 4500</leader>\n" +
                "        <datafield ind1='0' ind2='0' tag='001'>\n" +
                "            <subfield code='a'>" + id + "</subfield>\n" +
                "            <subfield code='b'>" + agency + "</subfield>\n" +
                "        </datafield>\n" +
                "    </record>" +
                "</collection>").getBytes(StandardCharsets.UTF_8);
    }

    private RecordServiceConnector.Params fetchParameters() {
        return new RecordServiceConnector.Params()
                .withMode(RecordServiceConnector.Params.Mode.EXPANDED)
                .withKeepAutFields(true)
                .withUseParentAgency(true);
    }

    private void assertThrowsWithMessage(HarvestOperation harvestOperation, Integer id, UpdateCode updateCode,
                                         Selection selection, String message) throws DMatServiceConnectorException {
        assertThrowsWithMessage(harvestOperation, id, updateCode, selection, Status.PENDING_EXPORT, message);
    }

    private void assertThrowsWithMessage(HarvestOperation harvestOperation, Integer id, UpdateCode updateCode,
            Selection selection, Status status, String message) throws DMatServiceConnectorException {
        LocalDateTime accession = LocalDateTime.now();

        when(dmatServiceConnector.getExportedRecords(any(HashMap.class)))
                .thenReturn((ExportedRecordList) new ExportedRecordList()
                        .withCreationDate(accession.toLocalDate())
                        .withRecords(Arrays.asList(
                                mockRecord(1, accession, updateCode, selection, status))));

        HarvesterException harvesterException = assertThrows(HarvesterException.class, harvestOperation::execute);
        assertThat("Exception message", harvesterException.getMessage(),
                is(message));
    }
}
