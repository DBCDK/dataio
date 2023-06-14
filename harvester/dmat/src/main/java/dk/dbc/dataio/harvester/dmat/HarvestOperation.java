package dk.dbc.dataio.harvester.dmat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.commons.metricshandler.MetricsHandlerBean;
import dk.dbc.dataio.bfs.api.BinaryFileStore;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorUnexpectedStatusCodeException;
import dk.dbc.dataio.commons.time.StopWatch;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnector;
import dk.dbc.dataio.harvester.types.DMatHarvesterConfig;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.UncheckedHarvesterException;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dmat.service.connector.DMatServiceConnector;
import dk.dbc.dmat.service.connector.DMatServiceConnectorException;
import dk.dbc.dmat.service.dto.ExportedRecordList;
import dk.dbc.dmat.service.persistence.DMatRecord;
import dk.dbc.dmat.service.persistence.RecordView;
import dk.dbc.dmat.service.persistence.enums.Selection;
import dk.dbc.dmat.service.persistence.enums.Status;
import dk.dbc.dmat.service.persistence.enums.UpdateCode;
import dk.dbc.log.DBCTrackedLogContext;
import dk.dbc.rawrepo.record.RecordServiceConnector;
import dk.dbc.rawrepo.record.RecordServiceConnectorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

public class HarvestOperation {
    private static final Logger LOGGER = LoggerFactory.getLogger(HarvestOperation.class);

    static int DMAT_SERVICE_FETCH_SIZE = 100;

    private final DMatHarvesterConfig config;
    private final BinaryFileStore binaryFileStore;
    private final FileStoreServiceConnector fileStoreServiceConnector;
    private final FlowStoreServiceConnector flowStoreServiceConnector;
    private final JobStoreServiceConnector jobStoreServiceConnector;
    private static final ZoneId TIMEZONE = getTimezone();
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().registerModule(new JavaTimeModule()).disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    private final DMatServiceConnector dmatServiceConnector;
    private final RecordServiceConnector recordServiceConnector;
    private final String dmatDownloadUrl;

    private final MetricsHandlerBean metricsHandler;

    public HarvestOperation(DMatHarvesterConfig config,
                            BinaryFileStore binaryFileStore,
                            FileStoreServiceConnector fileStoreServiceConnector,
                            FlowStoreServiceConnector flowStoreServiceConnector,
                            JobStoreServiceConnector jobStoreServiceConnector,
                            DMatServiceConnector dmatServiceConnector,
                            RecordServiceConnector recordServiceConnector,
                            String dmatDownloadUrl,
                            MetricsHandlerBean metricsHandler) throws HarvesterException {
        this.config = config;
        this.binaryFileStore = binaryFileStore;
        this.fileStoreServiceConnector = fileStoreServiceConnector;
        this.flowStoreServiceConnector = flowStoreServiceConnector;
        this.jobStoreServiceConnector = jobStoreServiceConnector;
        this.dmatServiceConnector = dmatServiceConnector;
        this.recordServiceConnector = recordServiceConnector;
        this.dmatDownloadUrl = dmatDownloadUrl;
        this.metricsHandler = metricsHandler;
    }

    public int execute() throws HarvesterException {
        final StopWatch stopwatch = new StopWatch();
        final Map<Integer, Status> statusAfterExportRr = new HashMap<>();
        int recordsHarvested = 0;
        int recordsProcessed = 0;
        int recordsSkipped = 0;
        AtomicInteger recordsFailed = new AtomicInteger(0);

        try(JobBuilder rrJobBuilder = new JobBuilder(
                binaryFileStore, fileStoreServiceConnector, jobStoreServiceConnector,
                JobSpecificationTemplate.create(config))) {

            // Set all received records to status PROCESSING to prevent them from being
            // exported again in case of catastrophic errors in dmat or the harvester
            final ResultSet dmatRecords = new ResultSet(dmatServiceConnector);
            for (DMatRecord dmatRecord : dmatRecords) {
                recordsHarvested++;
                LOGGER.info("Fetched dmat record {}", dmatRecord.getId());

                // Move record to status PROCESSING to prevent reexport, skip record if we can not set new status
                if (!updateStatus(dmatRecord.getId(), Status.PROCESSING)) {
                    LOGGER.error("Unable to set status PROCESSING on DMat record {}. Harvesting aborted for this record", dmatRecord.getId());
                    recordsSkipped++;
                    continue;
                }

                // Process record
                try {

                    // Check validity of the received record
                    assertRecordState(dmatRecord);

                    // Create the addi object and add it to the job
                    final ExtendedAddiMetaData addiMetaData = createAddiMetaData(dmatRecords.getCreationTime(), dmatRecord);
                    try {
                        DBCTrackedLogContext.setTrackingId(addiMetaData.trackingId());
                        final AddiRecord addiRecord = createAddiRecord(recordServiceConnector, addiMetaData, dmatRecord);
                        rrJobBuilder.addRecord(addiRecord);
                        statusAfterExportRr.put(dmatRecord.getId(), Status.EXPORTED);
                        recordsProcessed++;
                        LOGGER.info("Added processed DMat record {} to job", dmatRecord.getId());
                    } finally {
                        DBCTrackedLogContext.remove();
                    }
                } catch (RecordServiceConnectorException | HarvesterException e) {
                    LOGGER.error("Caught RecordServiceConnectorException|HarvesterException for dmatrecord {}: {}",
                            dmatRecord.getId(), e.getMessage());
                    statusAfterExportRr.put(dmatRecord.getId(), Status.PENDING_EXPORT);
                    recordsSkipped++;
                } catch (JsonProcessingException e) {
                    LOGGER.error("Caught JsonProcessingException for dmatrecord {}: {}",
                            dmatRecord.getId(), e.getMessage());
                    statusAfterExportRr.put(dmatRecord.getId(), Status.PENDING_EXPORT);
                    recordsSkipped++;
                } catch (Exception e) {
                    LOGGER.error("Caught unexpected Exception for dmatrecord {}", dmatRecord.getId(), e);
                    statusAfterExportRr.put(dmatRecord.getId(), Status.PENDING_EXPORT);
                    recordsSkipped++;
                }
            }

            // Create job
            if (rrJobBuilder.getRecordsAdded() > 0) {
                int recordsAdded = rrJobBuilder.getRecordsAdded();
                Optional<JobInfoSnapshot> jobInfo = rrJobBuilder.build();
                jobInfo.ifPresent(jobInfoSnapshot -> {
                    LOGGER.info("Created job {} with {} items", jobInfoSnapshot.getJobId(), recordsAdded);
                    metricsHandler.increment(DmatHarvesterMetrics.RECORDS_ADDED, recordsAdded);
                });
            } else if (recordsHarvested == 0) {
                LOGGER.info("No new records harvested from DMat");
            } else {
                LOGGER.error("Did not create job for DMat harvester since 0 records was added to the job during processing");
            }

            // After the job for RR records has been successfully build, update the status of the
            // harvested dmat records to ensure that no record is marked as exported
            // if job creation fails
            statusAfterExportRr.forEach((recordId, status) -> {
                if (!updateStatus(recordId, status)) {
                    recordsFailed.incrementAndGet();
                }
            });
            updateConfig(config);

            return recordsProcessed;
        } catch (DMatServiceConnectorException e) {
            LOGGER.error(String.format("Caught unexpected DMatServiceConnectorException: %s", e.getMessage()), e);
            LOGGER.error("DMat may now have stale records in status PROCESSING");
            metricsHandler.increment(DmatHarvesterMetrics.EXCEPTIONS);
            throw new HarvesterException("Caught DMatServiceConnectorException", e);
        } catch (HarvesterException e) {
            LOGGER.error(String.format("Caught HarvesterException: %s", e.getMessage()), e);
            LOGGER.error("DMat may now have stale records in status PROCESSING");
            metricsHandler.increment(DmatHarvesterMetrics.EXCEPTIONS);
            throw e;
        } catch (Exception e) {
            LOGGER.error(String.format("Caught unexpected Exception: %s", e.getMessage()), e);
            LOGGER.error("DMat may now have stale records in status PROCESSING");
            metricsHandler.increment(DmatHarvesterMetrics.UNHANDLED_EXCEPTIONS);
            throw new HarvesterException("Caught Exception", e);
        } finally {
            LOGGER.info("Harvested {} dmat cases. {} was processed, {} was skipped, {} failed in {} ms", recordsHarvested,
                    recordsProcessed, recordsSkipped, recordsFailed.get(), stopwatch.getElapsedTime());
            metricsHandler.increment(DmatHarvesterMetrics.RECORDS_HARVESTED, recordsHarvested);
            metricsHandler.increment(DmatHarvesterMetrics.RECORDS_PROCESSED, recordsProcessed);
            metricsHandler.increment(DmatHarvesterMetrics.RECORDS_FAILED, recordsFailed.get() + recordsSkipped);
        }
    }

    private static ZoneId getTimezone() {
        String tzEnv = System.getenv("TZ");
        if (tzEnv == null) {
            tzEnv = "Europe/Copenhagen";
        }
        return ZoneId.of(tzEnv);
    }

    private void assertRecordState(DMatRecord dmatRecord) throws HarvesterException {

        // We should only receive records ready for export
        if (dmatRecord.getStatus() == null || dmatRecord.getStatus() != Status.PENDING_EXPORT) {
            LOGGER.error("Received DMatRecord {} with bad status {} when expecting PENDING_EXPORT",
                    dmatRecord.getId(), dmatRecord.getStatus() != null ? dmatRecord.getStatus() : "(null)");
            throw new HarvesterException(String.format("DMatRecord %d does not have status PENDING_EXPORT", dmatRecord.getId()));
        }

        // Must have updateCode and selection
        if (dmatRecord.getUpdateCode() == null || dmatRecord.getUpdateCode() == UpdateCode.NONE) {
            LOGGER.error("Received DMatRecord {} with invalid updateCode {}",
                    dmatRecord.getId(), dmatRecord.getUpdateCode() != null ? dmatRecord.getUpdateCode().toString() : "(null)");
            throw new HarvesterException(String.format("DMatRecord %d has invalid updateCode %s",
                    dmatRecord.getId(), dmatRecord.getUpdateCode() != null ? dmatRecord.getUpdateCode().toString() : "(null)"));
        }
        if (dmatRecord.getSelection() == null || dmatRecord.getSelection() == Selection.NONE) {
            LOGGER.error("Received DMatRecord {} with invalid selection {}",
                    dmatRecord.getId(), dmatRecord.getSelection() != null ? dmatRecord.getSelection().toString() : "(null)");
            throw new HarvesterException(String.format("DMatRecord %d has invalid selection %s",
                    dmatRecord.getId(), dmatRecord.getSelection() != null ? dmatRecord.getSelection().toString() : "(null)"));
        }

        // We must always have a recordId (the ebook/eaudiobook records faustnumber)
        if (dmatRecord.getRecordId() == null || dmatRecord.getRecordId().isEmpty()) {
            LOGGER.error("Received DMatRecord {} with no recordId", dmatRecord.getId());
            throw new HarvesterException(String.format("DMatRecord %d does not have a recordId", dmatRecord.getId()));
        }

        //Check for valid combinations of updateCode and selection - and check that we have all expected extra information
        if (dmatRecord.getUpdateCode() == UpdateCode.NEW || dmatRecord.getUpdateCode() == UpdateCode.AUTO) {
            if (dmatRecord.getSelection() == Selection.CREATE) {
                LOGGER.info("Received DMatRecord {} with faust {} for NEW|AUTO:CREATE", dmatRecord.getId(),
                        dmatRecord.getRecordId());
                return;
            }
            if (dmatRecord.getSelection() == Selection.CLONE) {
                if (dmatRecord.getMatch() == null || dmatRecord.getMatch().isEmpty()) {
                    LOGGER.error("Received DMatRecord {} for NEW|AUTO:CLONE with no match", dmatRecord.getId());
                    throw new HarvesterException(String.format("DMatRecord %d does not have a match", dmatRecord.getId()));
                }
                LOGGER.info("Received DMatRecord {} with faust {} and match {} for CREATE|AUTO:CLONE", dmatRecord.getId(),
                        dmatRecord.getRecordId(), dmatRecord.getMatch());
                return;
            }
        }
        if (dmatRecord.getUpdateCode() == UpdateCode.ACT && dmatRecord.getSelection() == Selection.CREATE) {
            LOGGER.info("Received DMatRecord {} with faust {} for ACT:CREATE", dmatRecord.getId(),
                    dmatRecord.getRecordId());
            return;
        }
        if (dmatRecord.getUpdateCode() == UpdateCode.NNB && (dmatRecord.getSelection() == Selection.DROP || dmatRecord.getSelection() == Selection.AUTODROP)) {
            LOGGER.info("Received DMatRecord {} with faust {} for NNB:DROP|AUTODROP", dmatRecord.getId(),
                    dmatRecord.getRecordId());
            return;
        }
        if (dmatRecord.getUpdateCode() == UpdateCode.REVIEW) {
            if (dmatRecord.getReviewId() == null || dmatRecord.getReviewId().isEmpty()) {
                LOGGER.error("Received DMatRecord {} for REVIEW:* with no review id", dmatRecord.getId());
                throw new HarvesterException(String.format("DMatRecord %d does not have a review id", dmatRecord.getId()));
            }
            LOGGER.info("Received DMatRecord {} with faust {} and review id {} for REVIEW:*", dmatRecord.getId(),
                    dmatRecord.getRecordId(), dmatRecord.getReviewId());
            return;
        }
        if (dmatRecord.getUpdateCode() == UpdateCode.UPDATE) {
            LOGGER.info("Received DMatRecord {} with faust {} for UPDATE:*", dmatRecord.getId(),
                    dmatRecord.getRecordId());
            return;
        }

        // Remaining combinations is an error
        LOGGER.error("Received DMatRecord {} with unknown combination of updateCode {} and selection {}", dmatRecord.getId(),
                dmatRecord.getUpdateCode(), dmatRecord.getSelection());
        throw new HarvesterException(String.format("DMatRecord %d has an invalid combination of updateCode and selection",
                dmatRecord.getId()));
    }

    private ExtendedAddiMetaData createAddiMetaData(LocalDate creationDate, DMatRecord dmatRecord) {
        return ((ExtendedAddiMetaData) new ExtendedAddiMetaData()
                .withTrackingId(String.join(".", "dmat", config.getLogId(),
                        String.valueOf(dmatRecord.getId())))
                .withSubmitterNumber(JobSpecificationTemplate.SUBMITTER_NUMBER_RR)
                .withFormat(config.getContent().getFormat())
                .withCreationDate(Date.from(creationDate.atStartOfDay(TIMEZONE).plusHours(12).toInstant()))
                .withBibliographicRecordId(dmatRecord.getIsbn()))
                .withDmatRecord(dmatRecord)
                .withDmatUrl(String.format(dmatDownloadUrl, dmatRecord.getRecordId()));

        // Note: Using creationDate.atStartOfDay(...) will actually, for us in a timezone with UTC -1 or -2
        //       hours, put the creationdate stamp at the day before.  This is kind of weird, but is
        //       the standardized way to handle this in dataio, so we shall not invent a new truth.
        //
        // The flowscript handling jobs from the dmat harvester MUST use the 'formattedCreationDate property
        // provided by the dmat export object.
    }

    private AddiRecord createAddiRecord(RecordServiceConnector recordServiceConnector, ExtendedAddiMetaData addiMetaData,
                                        DMatRecord dmatRecord) throws HarvesterException, JsonProcessingException,
            RecordServiceConnectorException {

        // Write the DMatRecord out with only those fields visible for export operations
        String metaData = OBJECT_MAPPER
                .writerWithView(RecordView.Export.class).writeValueAsString(addiMetaData);

        // Fetch attached record. MarcXchange records is wrapped in a collection since this is required by DAM,
        // even though there is ever only one record. Publizon records from tickle-repo is attached as is
        byte[] content = RecordFetcher.getRecordCollectionFor(recordServiceConnector, dmatRecord);

        // Assembly addi object
        return new AddiRecord(
                metaData.getBytes(StandardCharsets.UTF_8),
                content);
    }

    private Boolean updateStatus(Integer caseId, Status status) throws UncheckedHarvesterException {
        try {
            dmatServiceConnector.updateRecordStatus(caseId, status);
            LOGGER.info("dmat record {} set to status {}", caseId, status);
            return true;
        } catch (DMatServiceConnectorException|JSONBException e) {
            LOGGER.error(String.format("Unable to update status to %s for dmat record %d due to DMatServiceConnectorException|JSONBException", status, caseId), e);
            return false;
        } catch (Exception e) {
            LOGGER.error(String.format("Caught unexpected exception when updating status to %s for dmat record %d", status, caseId), e);
            return false;
        }
    }

    private DMatHarvesterConfig updateConfig(DMatHarvesterConfig config) throws HarvesterException {
        final Date timeOfLastHarvest = new Date();
        config.getContent().withTimeOfLastHarvest(timeOfLastHarvest);

        try {
            return flowStoreServiceConnector.updateHarvesterConfig(config);
        } catch (FlowStoreServiceConnectorException | RuntimeException e) {
            // Handle concurrency conflicts
            if (e instanceof FlowStoreServiceConnectorUnexpectedStatusCodeException
                    && ((FlowStoreServiceConnectorUnexpectedStatusCodeException) e).getStatusCode() == 409) {
                try {
                    final DMatHarvesterConfig refreshedConfig = flowStoreServiceConnector.getHarvesterConfig(
                            config.getId(), DMatHarvesterConfig.class);

                    refreshedConfig.getContent().withTimeOfLastHarvest(timeOfLastHarvest);
                    return updateConfig(refreshedConfig);
                } catch (FlowStoreServiceConnectorException fssce) {
                    LOGGER.error("Error refreshing config {}", config.getId(), fssce);
                }
            }
            throw new HarvesterException("Failed to update harvester config: " + config, e);
        }
    }

    /* Abstraction over one or more dmat service fetch cycles.
       The purpose of this ResultSet class is to avoid high memory consumption
       both on the server and client side if a very large number of cases need
       to be harvested. */
    private static class ResultSet implements Iterable<DMatRecord> {
        private final DMatServiceConnector dmatServiceConnector;
        private int from;
        private boolean exhausted = false;
        private Iterator<DMatRecord> records;
        private LocalDate creationTime;

        ResultSet(DMatServiceConnector dmatServiceConnector) throws DMatServiceConnectorException, HarvesterException {
            this.dmatServiceConnector = dmatServiceConnector;
            this.from = 0;
            fetchRecords();
        }

        public LocalDate getCreationTime() {
            return creationTime;
        }

        @Override
        public Iterator<DMatRecord> iterator() {
            return new Iterator<>() {
                @Override
                public boolean hasNext() {
                    if (!records.hasNext() && !exhausted) {
                        try {
                            fetchRecords();
                        } catch (DMatServiceConnectorException | HarvesterException e) {
                            throw new IllegalStateException(e);
                        }
                    }
                    return records.hasNext();
                }

                @Override
                public DMatRecord next() {
                    return records.next();
                }
            };
        }

        private void fetchRecords() throws DMatServiceConnectorException, HarvesterException {
            Map<String, String> queryParams = new HashMap<>();
            queryParams.put("limit", Integer.toString(DMAT_SERVICE_FETCH_SIZE));
            queryParams.put("from", Integer.toString(from));
            final ExportedRecordList result = dmatServiceConnector.getExportedRecords(queryParams);

            this.records = result.getRecords().iterator();
            this.creationTime = result.getCreationDate();

            if (result.getNumFound() > DMAT_SERVICE_FETCH_SIZE) {
                throw new HarvesterException(
                        String.format("DMat returned more than the requested number of records: wanted %d got %d",
                                DMAT_SERVICE_FETCH_SIZE, result.getNumFound()));
            }
            if (result.getNumFound() < DMAT_SERVICE_FETCH_SIZE) {
                this.exhausted = true;
            }
            if (result.getNumFound() == DMAT_SERVICE_FETCH_SIZE) {
                from = result.getRecords().get(DMAT_SERVICE_FETCH_SIZE - 1).getId();
            }
        }
    }
}
