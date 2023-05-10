package dk.dbc.dataio.harvester.dmat;

import dk.dbc.commons.addi.AddiReader;
import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.commons.metricshandler.CounterMetric;
import dk.dbc.commons.metricshandler.MetricsHandlerBean;
import dk.dbc.commons.metricshandler.SimpleTimerMetric;
import dk.dbc.dataio.bfs.api.BinaryFileStoreFsImpl;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.filestore.service.connector.MockedFileStoreServiceConnector;
import dk.dbc.dataio.harvester.types.DMatHarvesterConfig;
import dk.dbc.dataio.harvester.types.HarvesterException;
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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class HarvestOperationTest {
    private JobStoreServiceConnector jobStoreServiceConnector;
    private MockedFileStoreServiceConnector fileStoreServiceConnector;
    private FlowStoreServiceConnector flowStoreServiceConnector;
    private Path harvesterTmpFile;
    private HarvestOperation harvestOperation;
    private final DMatServiceConnector dmatServiceConnector = mock(DMatServiceConnector.class);
    private final RecordServiceConnector recordServiceConnector = mock(RecordServiceConnector.class);
    private final MetricsHandlerBean metricsHandlerBean = mock(MetricsHandlerBean.class);
    private static final Logger LOGGER = LoggerFactory.getLogger(HarvestOperationTest.class);

    @TempDir
    Path tempDir;

    @BeforeEach
    public void setupMocks() throws Exception {
        // Intercept harvester data files with mocked FileStoreServiceConnectorBean
        harvesterTmpFile = tempDir.resolve("harvester.dat");

        fileStoreServiceConnector = new MockedFileStoreServiceConnector();
        fileStoreServiceConnector.destinations.add(harvesterTmpFile);

        jobStoreServiceConnector = mock(JobStoreServiceConnector.class);
        when(jobStoreServiceConnector.addJob(any(JobInputStream.class)))
                .thenReturn(new JobInfoSnapshot());

        flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);

        final DMatHarvesterConfig config = newConfig();
        harvestOperation = spy(newHarvestOperation(config));

        doNothing().when(metricsHandlerBean).increment(any(CounterMetric.class), any());
    }

    @Test
    public void noCasesToHarvest() throws HarvesterException, DMatServiceConnectorException, JobStoreServiceConnectorException,
            FlowStoreServiceConnectorException, JSONBException {

        when(dmatServiceConnector.getExportedRecords(any(HashMap.class)))
                .thenReturn(new ExportedRecordList());

        final int casesHarvested = harvestOperation.execute();
        assertThat("Number of cases harvested", casesHarvested, is(0));

        verify(dmatServiceConnector, never()).upsertRecord(any(RecordData.class));
        verify(jobStoreServiceConnector, never()).addJob(any(JobInputStream.class));
        verify(flowStoreServiceConnector).updateHarvesterConfig(any(DMatHarvesterConfig.class));

        verify(metricsHandlerBean).increment(DmatHarvesterMetrics.RECORDS_HARVESTED, 0L);
        verify(metricsHandlerBean).increment(DmatHarvesterMetrics.RECORDS_PROCESSED, 0L);
        verify(metricsHandlerBean).increment(DmatHarvesterMetrics.RECORDS_FAILED, 0L);
    }

    @Test
    public void harvestOneRecord() throws HarvesterException, DMatServiceConnectorException, JobStoreServiceConnectorException,
            FlowStoreServiceConnectorException, JSONBException, RecordServiceConnectorException, IOException {
        LocalDateTime accession = LocalDateTime.now();

        when(dmatServiceConnector.getExportedRecords(any(HashMap.class)))
                .thenReturn((ExportedRecordList) new ExportedRecordList()
                        .withCreationDate(accession.toLocalDate())
                        .withRecords(Arrays.asList(
                                mockRecord(1, accession, UpdateCode.NEW, Selection.CREATE)
                        )));

        when(recordServiceConnector.getRecordContentCollection(191919, MATCH_FAUST, fetchParameters()))
                .thenReturn(mockMarcXchange(MATCH_FAUST, MATCH_AGENCY));

        final int casesHarvested = harvestOperation.execute();
        assertThat("Number of cases harvested", casesHarvested, is(1));

        verify(dmatServiceConnector).updateRecordStatus(1, Status.PROCESSING);
        verify(dmatServiceConnector).updateRecordStatus(1, Status.EXPORTED);
        verify(jobStoreServiceConnector).addJob(any(JobInputStream.class));
        verify(flowStoreServiceConnector).updateHarvesterConfig(any(DMatHarvesterConfig.class));

        verify(metricsHandlerBean).increment(DmatHarvesterMetrics.RECORDS_HARVESTED, 1L);
        verify(metricsHandlerBean).increment(DmatHarvesterMetrics.RECORDS_PROCESSED, 1L);
        verify(metricsHandlerBean).increment(DmatHarvesterMetrics.RECORDS_ADDED, 1L);
        verify(metricsHandlerBean).increment(DmatHarvesterMetrics.RECORDS_FAILED, 0L);

        // Check that the dmat url is constructed correctly
        FileInputStream is = new FileInputStream(harvesterTmpFile.toString());
        byte[] dataFile = new byte[is.available()];
        is.read(dataFile, 0, is.available());

        final AddiReader addiReader = new AddiReader(new ByteArrayInputStream(dataFile));
        if (addiReader.hasNext()) {
            final AddiRecord addiRecord = addiReader.next();
            String meta = new String(addiRecord.getMetaData());
            assertThat("dmat url", meta.contains("\"dmatUrl\":\"http://some.dmat.service/api/v1/content/faust/" + RECORD_FAUST + "\""));
        } else {
            throw new AssertionError("Expecting addi record");
        }
    }

    @Test
    public void harvestAllRecords() throws HarvesterException, DMatServiceConnectorException, JobStoreServiceConnectorException,
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

        when(recordServiceConnector.getRecordContentCollection(191919, MATCH_FAUST, fetchParameters()))
                .thenReturn(mockMarcXchange(MATCH_FAUST, MATCH_AGENCY));

        final int casesHarvested = harvestOperation.execute();
        assertThat("Number of cases harvested", casesHarvested, is(3));

        verify(dmatServiceConnector, times(6)).updateRecordStatus(any(Integer.class), any(Status.class));
        verify(dmatServiceConnector).updateRecordStatus(1, Status.PROCESSING);
        verify(dmatServiceConnector).updateRecordStatus(2, Status.PROCESSING);
        verify(dmatServiceConnector).updateRecordStatus(3, Status.PROCESSING);
        verify(dmatServiceConnector).updateRecordStatus(1, Status.EXPORTED);
        verify(dmatServiceConnector).updateRecordStatus(2, Status.EXPORTED);
        verify(dmatServiceConnector).updateRecordStatus(3, Status.EXPORTED);

        verify(jobStoreServiceConnector).addJob(any(JobInputStream.class));
        verify(flowStoreServiceConnector).updateHarvesterConfig(any(DMatHarvesterConfig.class));

        verify(metricsHandlerBean).increment(DmatHarvesterMetrics.RECORDS_HARVESTED, 3L);
        verify(metricsHandlerBean).increment(DmatHarvesterMetrics.RECORDS_PROCESSED, 3L);
        verify(metricsHandlerBean).increment(DmatHarvesterMetrics.RECORDS_ADDED, 3L);
        verify(metricsHandlerBean).increment(DmatHarvesterMetrics.RECORDS_FAILED, 0L);
    }

    @Test
    public void harvestWithMissingRrRecordThrows() throws DMatServiceConnectorException, RecordServiceConnectorException, JSONBException, JobStoreServiceConnectorException, HarvesterException {
        LocalDateTime accession = LocalDateTime.now();

        when(dmatServiceConnector.getExportedRecords(any(HashMap.class)))
                .thenReturn((ExportedRecordList) new ExportedRecordList()
                        .withCreationDate(accession.toLocalDate())
                        .withRecords(Arrays.asList(
                                mockRecord(1, accession, UpdateCode.NEW, Selection.CREATE)
                        )));

        when(recordServiceConnector.getRecordContentCollection(191919, MATCH_FAUST, fetchParameters()))
                .thenThrow(new RecordServiceConnectorException("No content"));

        executeExpectSkipped(harvestOperation, 1, UpdateCode.NEW, Selection.CLONE);

        verify(metricsHandlerBean).increment(DmatHarvesterMetrics.RECORDS_HARVESTED, 1L);
        verify(metricsHandlerBean).increment(DmatHarvesterMetrics.RECORDS_PROCESSED, 0L);
        verify(metricsHandlerBean).increment(DmatHarvesterMetrics.RECORDS_FAILED, 1L);
    }

    @Test
    public void harvestNewCreateRecord() throws HarvesterException, DMatServiceConnectorException, JobStoreServiceConnectorException,
            FlowStoreServiceConnectorException, JSONBException, RecordServiceConnectorException {
        LocalDateTime accession = LocalDateTime.now();

        when(dmatServiceConnector.getExportedRecords(any(HashMap.class)))
                .thenReturn((ExportedRecordList) new ExportedRecordList()
                        .withCreationDate(accession.toLocalDate())
                        .withRecords(Arrays.asList(
                                mockRecord(1, accession, UpdateCode.NEW, Selection.CREATE)
                        )));

        when(recordServiceConnector.getRecordContentCollection(191919, MATCH_FAUST, fetchParameters()))
                .thenReturn(mockMarcXchange(MATCH_FAUST, MATCH_AGENCY));

        final int casesHarvested = harvestOperation.execute();
        assertThat("Number of cases harvested", casesHarvested, is(1));

        verify(dmatServiceConnector).updateRecordStatus(1, Status.PROCESSING);
        verify(dmatServiceConnector).updateRecordStatus(1, Status.EXPORTED);
        verify(recordServiceConnector, times(0)).getRecordContent(any(Integer.class),
                any(String.class), any(RecordServiceConnector.Params.class));
        verify(jobStoreServiceConnector).addJob(any(JobInputStream.class));
        verify(flowStoreServiceConnector).updateHarvesterConfig(any(DMatHarvesterConfig.class));

        verify(metricsHandlerBean).increment(DmatHarvesterMetrics.RECORDS_HARVESTED, 1L);
        verify(metricsHandlerBean).increment(DmatHarvesterMetrics.RECORDS_PROCESSED, 1L);
        verify(metricsHandlerBean).increment(DmatHarvesterMetrics.RECORDS_ADDED, 1L);
        verify(metricsHandlerBean).increment(DmatHarvesterMetrics.RECORDS_FAILED, 0L);
    }

    @Test
    public void harvestNewCloneRecord() throws HarvesterException, DMatServiceConnectorException, JobStoreServiceConnectorException,
            FlowStoreServiceConnectorException, JSONBException, RecordServiceConnectorException {
        LocalDateTime accession = LocalDateTime.now();

        when(dmatServiceConnector.getExportedRecords(any(HashMap.class)))
                .thenReturn((ExportedRecordList) new ExportedRecordList()
                        .withCreationDate(accession.toLocalDate())
                        .withRecords(Arrays.asList(
                                mockRecord(1, accession, UpdateCode.NEW, Selection.CLONE)
                        )));

        when(recordServiceConnector.getRecordContentCollection(191919, MATCH_FAUST, fetchParameters()))
                .thenReturn(mockMarcXchange(MATCH_FAUST, MATCH_AGENCY));

        final int casesHarvested = harvestOperation.execute();
        assertThat("Number of cases harvested", casesHarvested, is(1));

        verify(dmatServiceConnector).updateRecordStatus(1, Status.PROCESSING);
        verify(dmatServiceConnector).updateRecordStatus(1, Status.EXPORTED);
        verify(recordServiceConnector, times(1)).getRecordContentCollection(191919, MATCH_FAUST, fetchParameters());
        verify(jobStoreServiceConnector).addJob(any(JobInputStream.class));
        verify(flowStoreServiceConnector).updateHarvesterConfig(any(DMatHarvesterConfig.class));

        verify(metricsHandlerBean).increment(DmatHarvesterMetrics.RECORDS_HARVESTED, 1L);
        verify(metricsHandlerBean).increment(DmatHarvesterMetrics.RECORDS_PROCESSED, 1L);
        verify(metricsHandlerBean).increment(DmatHarvesterMetrics.RECORDS_ADDED, 1L);
        verify(metricsHandlerBean).increment(DmatHarvesterMetrics.RECORDS_FAILED, 0L);
    }

    @Test
    public void harvestAutoCreateRecord() throws HarvesterException, DMatServiceConnectorException, JobStoreServiceConnectorException,
            FlowStoreServiceConnectorException, JSONBException, RecordServiceConnectorException {
        LocalDateTime accession = LocalDateTime.now();

        when(dmatServiceConnector.getExportedRecords(any(HashMap.class)))
                .thenReturn((ExportedRecordList) new ExportedRecordList()
                        .withCreationDate(accession.toLocalDate())
                        .withRecords(Arrays.asList(
                                mockRecord(1, accession, UpdateCode.AUTO, Selection.CREATE)
                        )));

        when(recordServiceConnector.getRecordContentCollection(191919, MATCH_FAUST, fetchParameters()))
                .thenReturn(mockMarcXchange(MATCH_FAUST, MATCH_AGENCY));

        final int casesHarvested = harvestOperation.execute();
        assertThat("Number of cases harvested", casesHarvested, is(1));

        verify(dmatServiceConnector).updateRecordStatus(1, Status.PROCESSING);
        verify(dmatServiceConnector).updateRecordStatus(1, Status.EXPORTED);
        verify(recordServiceConnector, times(0)).getRecordContent(any(Integer.class),
                any(String.class), any(RecordServiceConnector.Params.class));
        verify(jobStoreServiceConnector).addJob(any(JobInputStream.class));
        verify(flowStoreServiceConnector).updateHarvesterConfig(any(DMatHarvesterConfig.class));

        verify(metricsHandlerBean).increment(DmatHarvesterMetrics.RECORDS_HARVESTED, 1L);
        verify(metricsHandlerBean).increment(DmatHarvesterMetrics.RECORDS_PROCESSED, 1L);
        verify(metricsHandlerBean).increment(DmatHarvesterMetrics.RECORDS_ADDED, 1L);
        verify(metricsHandlerBean).increment(DmatHarvesterMetrics.RECORDS_FAILED, 0L);
    }

    @Test
    public void harvestAutoCloneRecord() throws HarvesterException, DMatServiceConnectorException, JobStoreServiceConnectorException,
            FlowStoreServiceConnectorException, JSONBException, RecordServiceConnectorException {
        LocalDateTime accession = LocalDateTime.now();

        when(dmatServiceConnector.getExportedRecords(any(HashMap.class)))
                .thenReturn((ExportedRecordList) new ExportedRecordList()
                        .withCreationDate(accession.toLocalDate())
                        .withRecords(Arrays.asList(
                                mockRecord(1, accession, UpdateCode.AUTO, Selection.CLONE)
                        )));

        when(recordServiceConnector.getRecordContentCollection(191919, MATCH_FAUST, fetchParameters()))
                .thenReturn(mockMarcXchange(MATCH_FAUST, MATCH_AGENCY));

        final int casesHarvested = harvestOperation.execute();
        assertThat("Number of cases harvested", casesHarvested, is(1));

        verify(dmatServiceConnector).updateRecordStatus(1, Status.PROCESSING);
        verify(dmatServiceConnector).updateRecordStatus(1, Status.EXPORTED);
        verify(recordServiceConnector, times(1)).getRecordContentCollection(191919, MATCH_FAUST, fetchParameters());
        verify(jobStoreServiceConnector).addJob(any(JobInputStream.class));
        verify(flowStoreServiceConnector).updateHarvesterConfig(any(DMatHarvesterConfig.class));

        verify(metricsHandlerBean).increment(DmatHarvesterMetrics.RECORDS_HARVESTED, 1L);
        verify(metricsHandlerBean).increment(DmatHarvesterMetrics.RECORDS_PROCESSED, 1L);
        verify(metricsHandlerBean).increment(DmatHarvesterMetrics.RECORDS_ADDED, 1L);
        verify(metricsHandlerBean).increment(DmatHarvesterMetrics.RECORDS_FAILED, 0L);
    }

    @Test
    public void harvestActCreateRecord() throws HarvesterException, DMatServiceConnectorException, JobStoreServiceConnectorException,
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

        verify(dmatServiceConnector).updateRecordStatus(1, Status.PROCESSING);
        verify(dmatServiceConnector).updateRecordStatus(1, Status.EXPORTED);
        verify(jobStoreServiceConnector).addJob(any(JobInputStream.class));
        verify(flowStoreServiceConnector).updateHarvesterConfig(any(DMatHarvesterConfig.class));

        verify(metricsHandlerBean).increment(DmatHarvesterMetrics.RECORDS_HARVESTED, 1L);
        verify(metricsHandlerBean).increment(DmatHarvesterMetrics.RECORDS_PROCESSED, 1L);
        verify(metricsHandlerBean).increment(DmatHarvesterMetrics.RECORDS_ADDED, 1L);
        verify(metricsHandlerBean).increment(DmatHarvesterMetrics.RECORDS_FAILED, 0L);
    }

    @Test
    public void harvestNnbDropRecord() throws HarvesterException, DMatServiceConnectorException, JobStoreServiceConnectorException,
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

        verify(dmatServiceConnector).updateRecordStatus(1, Status.PROCESSING);
        verify(dmatServiceConnector).updateRecordStatus(1, Status.EXPORTED);
        verify(jobStoreServiceConnector).addJob(any(JobInputStream.class));
        verify(flowStoreServiceConnector).updateHarvesterConfig(any(DMatHarvesterConfig.class));

        verify(metricsHandlerBean).increment(DmatHarvesterMetrics.RECORDS_HARVESTED, 1L);
        verify(metricsHandlerBean).increment(DmatHarvesterMetrics.RECORDS_PROCESSED, 1L);
        verify(metricsHandlerBean).increment(DmatHarvesterMetrics.RECORDS_ADDED, 1L);
        verify(metricsHandlerBean).increment(DmatHarvesterMetrics.RECORDS_FAILED, 0L);
    }

    @Test
    public void harvestNnbAutodropRecord() throws HarvesterException, DMatServiceConnectorException, JobStoreServiceConnectorException,
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

        verify(dmatServiceConnector).updateRecordStatus(1, Status.PROCESSING);
        verify(dmatServiceConnector).updateRecordStatus(1, Status.EXPORTED);
        verify(jobStoreServiceConnector).addJob(any(JobInputStream.class));
        verify(flowStoreServiceConnector).updateHarvesterConfig(any(DMatHarvesterConfig.class));

        verify(metricsHandlerBean).increment(DmatHarvesterMetrics.RECORDS_HARVESTED, 1L);
        verify(metricsHandlerBean).increment(DmatHarvesterMetrics.RECORDS_PROCESSED, 1L);
        verify(metricsHandlerBean).increment(DmatHarvesterMetrics.RECORDS_ADDED, 1L);
        verify(metricsHandlerBean).increment(DmatHarvesterMetrics.RECORDS_FAILED, 0L);
    }

    @Test
    public void harvestReviewRecord() throws HarvesterException, DMatServiceConnectorException, JobStoreServiceConnectorException,
            FlowStoreServiceConnectorException, JSONBException, RecordServiceConnectorException {
        LocalDateTime accession = LocalDateTime.now();

        when(dmatServiceConnector.getExportedRecords(any(HashMap.class)))
                .thenReturn((ExportedRecordList) new ExportedRecordList()
                        .withCreationDate(accession.toLocalDate())
                        .withRecords(Arrays.asList(
                                mockRecord(1, accession, UpdateCode.REVIEW, Selection.CREATE) // selection is not looked at for this updateCode
                        )));

        when(recordServiceConnector.getRecordContentCollection(191919, REVIEW_FAUST, fetchParameters()))
                .thenReturn(mockMarcXchange(REVIEW_FAUST, REVIEW_AGENCY));

        final int casesHarvested = harvestOperation.execute();
        assertThat("Number of cases harvested", casesHarvested, is(1));

        verify(dmatServiceConnector).updateRecordStatus(1, Status.PROCESSING);
        verify(dmatServiceConnector).updateRecordStatus(1, Status.EXPORTED);
        verify(recordServiceConnector, times(1)).getRecordContentCollection(191919, REVIEW_FAUST, fetchParameters());
        verify(jobStoreServiceConnector).addJob(any(JobInputStream.class));
        verify(flowStoreServiceConnector).updateHarvesterConfig(any(DMatHarvesterConfig.class));

        verify(metricsHandlerBean).increment(DmatHarvesterMetrics.RECORDS_HARVESTED, 1L);
        verify(metricsHandlerBean).increment(DmatHarvesterMetrics.RECORDS_PROCESSED, 1L);
        verify(metricsHandlerBean).increment(DmatHarvesterMetrics.RECORDS_ADDED, 1L);
        verify(metricsHandlerBean).increment(DmatHarvesterMetrics.RECORDS_FAILED, 0L);
    }

    @Test
    public void harvestUpdateRecord() throws HarvesterException, DMatServiceConnectorException, JobStoreServiceConnectorException,
            FlowStoreServiceConnectorException, JSONBException, RecordServiceConnectorException {
        LocalDateTime accession = LocalDateTime.now();

        when(dmatServiceConnector.getExportedRecords(any(HashMap.class)))
                .thenReturn((ExportedRecordList) new ExportedRecordList()
                        .withCreationDate(accession.toLocalDate())
                        .withRecords(Arrays.asList(
                                mockRecord(1, accession, UpdateCode.UPDATE, Selection.CREATE) // selection is not looked at for this updateCode
                        )));

        when(recordServiceConnector.getRecordContentCollection(191919, RECORD_FAUST, fetchParameters()))
                .thenReturn(mockMarcXchange(RECORD_FAUST, RECORD_AGENCY));

        final int casesHarvested = harvestOperation.execute();
        assertThat("Number of cases harvested", casesHarvested, is(1));

        verify(dmatServiceConnector).updateRecordStatus(1, Status.PROCESSING);
        verify(dmatServiceConnector).updateRecordStatus(1, Status.EXPORTED);
        verify(recordServiceConnector, times(1)).getRecordContentCollection(191919, RECORD_FAUST, fetchParameters());
        verify(jobStoreServiceConnector).addJob(any(JobInputStream.class));
        verify(flowStoreServiceConnector).updateHarvesterConfig(any(DMatHarvesterConfig.class));

        verify(metricsHandlerBean).increment(DmatHarvesterMetrics.RECORDS_HARVESTED, 1L);
        verify(metricsHandlerBean).increment(DmatHarvesterMetrics.RECORDS_PROCESSED, 1L);
        verify(metricsHandlerBean).increment(DmatHarvesterMetrics.RECORDS_ADDED, 1L);
        verify(metricsHandlerBean).increment(DmatHarvesterMetrics.RECORDS_FAILED, 0L);
    }

    @Test
    public void harvestInvalidRecords() throws DMatServiceConnectorException, JSONBException, JobStoreServiceConnectorException, HarvesterException {

        for (Status status : Arrays.stream(Status.values())
                .filter(s -> s != Status.PENDING_EXPORT)
                .collect(Collectors.toList())) {
            executeExpectSkipped(harvestOperation, 1, UpdateCode.NEW, Selection.CREATE, status);
        }

        executeExpectSkipped(harvestOperation, 1, UpdateCode.NEW, Selection.NONE);
        executeExpectSkipped(harvestOperation, 1, UpdateCode.NONE, Selection.CREATE);

        executeExpectSkipped(harvestOperation, 1, UpdateCode.NEW, Selection.MERGE);
        executeExpectSkipped(harvestOperation, 1, UpdateCode.NEW, Selection.DROP);
        executeExpectSkipped(harvestOperation, 1, UpdateCode.NEW, Selection.AUTODROP);

        executeExpectSkipped(harvestOperation, 1, UpdateCode.AUTO, Selection.MERGE);
        executeExpectSkipped(harvestOperation, 1, UpdateCode.AUTO, Selection.DROP);
        executeExpectSkipped(harvestOperation, 1, UpdateCode.AUTO, Selection.AUTODROP);

        executeExpectSkipped(harvestOperation, 1, UpdateCode.NNB, Selection.MERGE);
        executeExpectSkipped(harvestOperation, 1, UpdateCode.NNB, Selection.CREATE);
        executeExpectSkipped(harvestOperation, 1, UpdateCode.NNB, Selection.CLONE);

        executeExpectSkipped(harvestOperation, 1, UpdateCode.ACT, Selection.MERGE);
        executeExpectSkipped(harvestOperation, 1, UpdateCode.ACT, Selection.CLONE);
        executeExpectSkipped(harvestOperation, 1, UpdateCode.ACT, Selection.DROP);
        executeExpectSkipped(harvestOperation, 1, UpdateCode.ACT, Selection.AUTODROP);
    }

    @Test
    public void harvestMoreThanRequestedRecords() throws DMatServiceConnectorException {
        LocalDateTime accession = LocalDateTime.now();

        when(dmatServiceConnector.getExportedRecords(any(HashMap.class)))
                .thenReturn((ExportedRecordList) new ExportedRecordList()
                        .withCreationDate(accession.toLocalDate())
                        .withRecords(Arrays.asList(
                                mockRecord(1, accession, UpdateCode.UPDATE, Selection.CREATE, Status.PENDING_EXPORT, RECORD_FAUST, REVIEW_FAUST, MATCH_FAUST),
                                mockRecord(2, accession, UpdateCode.UPDATE, Selection.CREATE, Status.PENDING_EXPORT, RECORD_FAUST, REVIEW_FAUST, MATCH_FAUST),
                                mockRecord(3, accession, UpdateCode.UPDATE, Selection.CREATE, Status.PENDING_EXPORT, RECORD_FAUST, REVIEW_FAUST, MATCH_FAUST),
                                mockRecord(4, accession, UpdateCode.UPDATE, Selection.CREATE, Status.PENDING_EXPORT, RECORD_FAUST, REVIEW_FAUST, MATCH_FAUST),
                                mockRecord(5, accession, UpdateCode.UPDATE, Selection.CREATE, Status.PENDING_EXPORT, RECORD_FAUST, REVIEW_FAUST, MATCH_FAUST)
                        )));

        HarvesterException harvesterException = assertThrows(HarvesterException.class, harvestOperation::execute);
        LOGGER.info("message: {}", harvesterException.getMessage());
        assertThat("exception message", harvesterException.getMessage()
                .equals("DMat returned more than the requested number of records: wanted 2 got 5"));

        verify(metricsHandlerBean).increment(DmatHarvesterMetrics.RECORDS_HARVESTED, 0L);
        verify(metricsHandlerBean).increment(DmatHarvesterMetrics.RECORDS_PROCESSED, 0L);
        verify(metricsHandlerBean).increment(DmatHarvesterMetrics.RECORDS_FAILED, 0L);
        verify(metricsHandlerBean).increment(DmatHarvesterMetrics.EXCEPTIONS);
    }

    @Test
    public void harvestOneFailingRecord() throws HarvesterException, DMatServiceConnectorException, JobStoreServiceConnectorException,
            FlowStoreServiceConnectorException, JSONBException, RecordServiceConnectorException {
        LocalDateTime accession = LocalDateTime.now();

        when(dmatServiceConnector.getExportedRecords(any(HashMap.class)))
                .thenReturn((ExportedRecordList) new ExportedRecordList()
                        .withCreationDate(accession.toLocalDate())
                        .withRecords(Arrays.asList(
                                mockRecord(1, accession, UpdateCode.UPDATE, Selection.CREATE, Status.PENDING_EXPORT, RECORD_FAUST, REVIEW_FAUST, MATCH_FAUST)
                        )));

        when(recordServiceConnector.getRecordContentCollection(191919, RECORD_FAUST, fetchParameters()))
                .thenReturn(mockMarcXchange(RECORD_FAUST, RECORD_AGENCY));
        when(recordServiceConnector.getRecordContentCollection(191919, RECORD_FAUST, fetchParameters()))
                .thenThrow(new RecordServiceConnectorException("No content"));

        final int casesHarvested = harvestOperation.execute();
        assertThat("Number of cases harvested", casesHarvested, is(0));

        verify(dmatServiceConnector, times(2)).updateRecordStatus(any(Integer.class), any(Status.class));
        verify(dmatServiceConnector).updateRecordStatus(1, Status.PROCESSING);
        verify(dmatServiceConnector).updateRecordStatus(1, Status.PENDING_EXPORT);

        verify(recordServiceConnector, times(1)).getRecordContentCollection(any(Integer.class), any(String.class), any());
        verify(recordServiceConnector, times(1)).getRecordContentCollection(191919, RECORD_FAUST, fetchParameters());

        verify(jobStoreServiceConnector, never()).addJob(any(JobInputStream.class));
        verify(flowStoreServiceConnector).updateHarvesterConfig(any(DMatHarvesterConfig.class));

        verify(metricsHandlerBean).increment(DmatHarvesterMetrics.RECORDS_HARVESTED, 1L);
        verify(metricsHandlerBean).increment(DmatHarvesterMetrics.RECORDS_PROCESSED, 0L);
        verify(metricsHandlerBean).increment(DmatHarvesterMetrics.RECORDS_FAILED, 1L);
    }

    @Test
    public void harvestFailingUpdateRecord() throws HarvesterException, DMatServiceConnectorException, JobStoreServiceConnectorException,
            FlowStoreServiceConnectorException, JSONBException, RecordServiceConnectorException {
        LocalDateTime accession = LocalDateTime.now();

        when(dmatServiceConnector.getExportedRecords(any(HashMap.class)))
                .thenReturn((ExportedRecordList) new ExportedRecordList()
                        .withCreationDate(accession.toLocalDate())
                        .withRecords(Arrays.asList(
                                mockRecord(1, accession, UpdateCode.UPDATE, Selection.CREATE, Status.PENDING_EXPORT, RECORD_FAUST, REVIEW_FAUST, MATCH_FAUST),
                                mockRecord(2, accession, UpdateCode.UPDATE, Selection.CREATE, Status.PENDING_EXPORT, null, REVIEW_FAUST, MATCH_FAUST)
                        )))
                .thenReturn((ExportedRecordList) new ExportedRecordList()
                        .withCreationDate(accession.toLocalDate())
                        .withRecords(Arrays.asList(
                                mockRecord(3, accession, UpdateCode.UPDATE, Selection.CREATE, Status.PENDING_EXPORT, "", REVIEW_FAUST, MATCH_FAUST),
                                mockRecord(4, accession, UpdateCode.UPDATE, Selection.CREATE, Status.PENDING_EXPORT, "98765432", REVIEW_FAUST, MATCH_FAUST)
                        )))
                .thenReturn((ExportedRecordList) new ExportedRecordList()
                        .withCreationDate(accession.toLocalDate())
                        .withRecords(Arrays.asList(
                                mockRecord(5, accession, UpdateCode.UPDATE, Selection.CREATE, Status.PENDING_EXPORT, RECORD_FAUST, REVIEW_FAUST, MATCH_FAUST)
                        )));

        when(recordServiceConnector.getRecordContentCollection(191919, RECORD_FAUST, fetchParameters()))
                .thenReturn(mockMarcXchange(RECORD_FAUST, RECORD_AGENCY));
        when(recordServiceConnector.getRecordContentCollection(191919, "98765432", fetchParameters()))
                .thenThrow(new RecordServiceConnectorException("No content"));

        final int casesHarvested = harvestOperation.execute();
        assertThat("Number of cases harvested", casesHarvested, is(2));

        verify(dmatServiceConnector, times(10)).updateRecordStatus(any(Integer.class), any(Status.class));
        verify(dmatServiceConnector).updateRecordStatus(1, Status.PROCESSING);
        verify(dmatServiceConnector).updateRecordStatus(2, Status.PROCESSING);
        verify(dmatServiceConnector).updateRecordStatus(3, Status.PROCESSING);
        verify(dmatServiceConnector).updateRecordStatus(4, Status.PROCESSING);
        verify(dmatServiceConnector).updateRecordStatus(5, Status.PROCESSING);
        verify(dmatServiceConnector).updateRecordStatus(1, Status.EXPORTED);
        verify(dmatServiceConnector).updateRecordStatus(2, Status.PENDING_EXPORT);
        verify(dmatServiceConnector).updateRecordStatus(3, Status.PENDING_EXPORT);
        verify(dmatServiceConnector).updateRecordStatus(4, Status.PENDING_EXPORT);
        verify(dmatServiceConnector).updateRecordStatus(5, Status.EXPORTED);

        verify(recordServiceConnector, times(3)).getRecordContentCollection(any(Integer.class), any(String.class), any());
        verify(recordServiceConnector, times(2)).getRecordContentCollection(191919, RECORD_FAUST, fetchParameters());
        verify(recordServiceConnector, times(1)).getRecordContentCollection(191919, "98765432", fetchParameters());

        verify(jobStoreServiceConnector).addJob(any(JobInputStream.class));
        verify(flowStoreServiceConnector).updateHarvesterConfig(any(DMatHarvesterConfig.class));

        verify(metricsHandlerBean).increment(DmatHarvesterMetrics.RECORDS_HARVESTED, 5L);
        verify(metricsHandlerBean).increment(DmatHarvesterMetrics.RECORDS_PROCESSED, 2L);
        verify(metricsHandlerBean).increment(DmatHarvesterMetrics.RECORDS_ADDED, 2L);
        verify(metricsHandlerBean).increment(DmatHarvesterMetrics.RECORDS_FAILED, 3L);
    }

    @Test
    public void harvestAllRecordsWithOneFailingOnStatusProcessing() throws HarvesterException, DMatServiceConnectorException, JobStoreServiceConnectorException,
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

        when(recordServiceConnector.getRecordContentCollection(191919, MATCH_FAUST, fetchParameters()))
                .thenReturn(mockMarcXchange(MATCH_FAUST, MATCH_AGENCY));

        // Throw an error when the second case is set to PROCESSING. This should not make the
        // harvester throw an exception, but instead leave one record in its PENDING_EXPORT state
        when(dmatServiceConnector.updateRecordStatus(2, Status.PROCESSING))
                .thenThrow(new DMatServiceConnectorException("Sorry, not today!"));

        final int casesHarvested = harvestOperation.execute();
        assertThat("Number of cases harvested", casesHarvested, is(2));

        verify(dmatServiceConnector, times(5)).updateRecordStatus(any(Integer.class), any(Status.class));
        verify(dmatServiceConnector).updateRecordStatus(1, Status.PROCESSING);
        verify(dmatServiceConnector).updateRecordStatus(2, Status.PROCESSING);
        verify(dmatServiceConnector).updateRecordStatus(3, Status.PROCESSING);
        verify(dmatServiceConnector).updateRecordStatus(1, Status.EXPORTED);
        verify(dmatServiceConnector).updateRecordStatus(3, Status.EXPORTED);

        verify(jobStoreServiceConnector).addJob(any(JobInputStream.class));
        verify(flowStoreServiceConnector).updateHarvesterConfig(any(DMatHarvesterConfig.class));

        verify(metricsHandlerBean).increment(DmatHarvesterMetrics.RECORDS_HARVESTED, 3L);
        verify(metricsHandlerBean).increment(DmatHarvesterMetrics.RECORDS_PROCESSED, 2L);
        verify(metricsHandlerBean).increment(DmatHarvesterMetrics.RECORDS_ADDED, 2);
        verify(metricsHandlerBean).increment(DmatHarvesterMetrics.RECORDS_FAILED, 1L);
    }

    @Test
    public void harvestAllRecordsWithOneFailingOnStatusExported() throws HarvesterException, DMatServiceConnectorException, JobStoreServiceConnectorException,
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

        when(recordServiceConnector.getRecordContentCollection(191919, MATCH_FAUST, fetchParameters()))
                .thenReturn(mockMarcXchange(MATCH_FAUST, MATCH_AGENCY));

        // Throw an error when the first case is updated. This should not make the
        // harvester throw an exception, but instead leave one record in its PROCESSING state
        when(dmatServiceConnector.updateRecordStatus(1, Status.EXPORTED))
              .thenThrow(new DMatServiceConnectorException("Dont do that to me!!!"));

        final int casesHarvested = harvestOperation.execute();
        assertThat("Number of cases harvested", casesHarvested, is(3));

        verify(dmatServiceConnector, times(6)).updateRecordStatus(any(Integer.class), any(Status.class));
        verify(dmatServiceConnector).updateRecordStatus(1, Status.PROCESSING);
        verify(dmatServiceConnector).updateRecordStatus(2, Status.PROCESSING);
        verify(dmatServiceConnector).updateRecordStatus(3, Status.PROCESSING);
        verify(dmatServiceConnector).updateRecordStatus(1, Status.EXPORTED);
        verify(dmatServiceConnector).updateRecordStatus(2, Status.EXPORTED);
        verify(dmatServiceConnector).updateRecordStatus(3, Status.EXPORTED);

        verify(jobStoreServiceConnector).addJob(any(JobInputStream.class));
        verify(flowStoreServiceConnector).updateHarvesterConfig(any(DMatHarvesterConfig.class));

        verify(metricsHandlerBean).increment(DmatHarvesterMetrics.RECORDS_HARVESTED, 3L);
        verify(metricsHandlerBean).increment(DmatHarvesterMetrics.RECORDS_PROCESSED, 3L);
        verify(metricsHandlerBean).increment(DmatHarvesterMetrics.RECORDS_ADDED, 3);
        verify(metricsHandlerBean).increment(DmatHarvesterMetrics.RECORDS_FAILED, 1L);
    }

    private HarvestOperation newHarvestOperation(DMatHarvesterConfig config) throws HarvesterException {
        HarvestOperation.DMAT_SERVICE_FETCH_SIZE = 2;     // force multiple iterations when dealing with >2 cases
        return new HarvestOperation(config,
                new BinaryFileStoreFsImpl(tempDir),
                fileStoreServiceConnector,
                flowStoreServiceConnector,
                jobStoreServiceConnector,
                dmatServiceConnector,
                recordServiceConnector,
                "http://some.dmat.service/api/v1/content/faust/%s",
                metricsHandlerBean);
    }

    private DMatHarvesterConfig newConfig() {
        final DMatHarvesterConfig config = new DMatHarvesterConfig(
                1, 2, new DMatHarvesterConfig.Content());
        config.getContent()
                .withName("HarvestOperationTest")
                .withFormat("-format-")
                .withDestination("-destination-");
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
        return mockRecord(id, accession, updateCode, selection, Status.PENDING_EXPORT, RECORD_FAUST, REVIEW_FAUST, MATCH_FAUST);
    }

    private DMatRecord mockRecord(Integer id, LocalDateTime accession, UpdateCode updateCode, Selection selection, String isbn) {
        DMatRecord dMatRecord = mockRecord(id, accession, updateCode, selection);
        return dMatRecord.withIsbn(isbn);
    }

    private DMatRecord mockRecord(Integer id, LocalDateTime accession, UpdateCode updateCode, Selection selection,
                                  Status status, String recordId, String reviewId, String match) {
        RecordData recordData = new RecordData();
        recordData.setRecordReference("2a69b0a2-cbdd-4afa-9dc8-9fb203732f01");
        return new DMatRecord().withActive(true).withAccession(accession)
                .withId(id)
                .withIsbn("123456789")
                .withRecordData(recordData)
                .withTitle("title 1")
                .withReviewId(reviewId)
                .withMatch(match)
                .withRecordId(recordId)
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
                "    <record>\n" +
                "        <leader>00000n 2200000 4500</leader>\n" +
                "        <datafield ind1='0' ind2='0' tag='001'>\n" +
                "            <subfield code='a'>" + (id + 1) + "</subfield>\n" +
                "            <subfield code='b'>" + agency + "</subfield>\n" +
                "        </datafield>\n" +
                "    </record>" +
                "</collection>").getBytes(StandardCharsets.UTF_8);
    }

    private RecordServiceConnector.Params fetchParameters() {
        return new RecordServiceConnector.Params()
                .withMode(RecordServiceConnector.Params.Mode.EXPANDED)
                .withKeepAutFields(true)
                .withUseParentAgency(true)
                .withExpand(true);
    }

    private void executeExpectSkipped(HarvestOperation harvestOperation, Integer id, UpdateCode updateCode,
                                      Selection selection) throws DMatServiceConnectorException, JSONBException, JobStoreServiceConnectorException, HarvesterException {
        executeExpectSkipped(harvestOperation, id, updateCode, selection, Status.PENDING_EXPORT);
    }

    private void executeExpectSkipped(HarvestOperation harvestOperation, Integer id, UpdateCode updateCode,
                                      Selection selection, Status status) throws DMatServiceConnectorException, JSONBException, JobStoreServiceConnectorException, HarvesterException {
        LocalDateTime accession = LocalDateTime.now();

        reset(dmatServiceConnector);
        reset(metricsHandlerBean);

        when(dmatServiceConnector.getExportedRecords(any(HashMap.class)))
                .thenReturn((ExportedRecordList) new ExportedRecordList()
                        .withCreationDate(accession.toLocalDate())
                        .withRecords(Arrays.asList(
                                mockRecord(id, accession, updateCode, selection, status, null, null, null))));
        harvestOperation.execute();
        verify(dmatServiceConnector, times(1)).updateRecordStatus(anyInt(), eq(Status.PROCESSING));
        verify(dmatServiceConnector, times(0)).updateRecordStatus(anyInt(), eq(Status.EXPORTED));
        verify(dmatServiceConnector, atLeastOnce()).updateRecordStatus(anyInt(), eq(Status.PENDING_EXPORT));
        verify(jobStoreServiceConnector, times(0)).addJob(any(JobInputStream.class));

        verify(metricsHandlerBean).increment(DmatHarvesterMetrics.RECORDS_HARVESTED, 1L);
        verify(metricsHandlerBean).increment(DmatHarvesterMetrics.RECORDS_PROCESSED, 0L);
        verify(metricsHandlerBean).increment(DmatHarvesterMetrics.RECORDS_FAILED, 1L);
    }
}
