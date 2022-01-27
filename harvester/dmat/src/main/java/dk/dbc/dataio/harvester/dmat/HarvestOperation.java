package dk.dbc.dataio.harvester.dmat;

import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.bfs.api.BinaryFileStore;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorUnexpectedStatusCodeException;
import dk.dbc.dataio.commons.time.StopWatch;
import dk.dbc.dataio.commons.types.AddiMetaData;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnector;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.DMatHarvesterConfig;
import dk.dbc.dataio.harvester.types.UncheckedHarvesterException;
import dk.dbc.dmat.service.connector.DMatServiceConnectorException;
import dk.dbc.dmat.service.dto.ExportedRecordList;
import dk.dbc.dmat.service.persistence.DMatRecord;
import dk.dbc.dmat.service.connector.DMatServiceConnector;
import dk.dbc.dmat.service.persistence.enums.Selection;
import dk.dbc.dmat.service.persistence.enums.Status;
import dk.dbc.dmat.service.persistence.enums.UpdateCode;
import dk.dbc.log.DBCTrackedLogContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

public class HarvestOperation {
    private static final Logger LOGGER = LoggerFactory.getLogger(HarvestOperation.class);

    static int DMAT_SERVICE_FETCH_SIZE = 100;

    private final DMatHarvesterConfig config;
    private final BinaryFileStore binaryFileStore;
    private final FileStoreServiceConnector fileStoreServiceConnector;
    private final FlowStoreServiceConnector flowStoreServiceConnector;
    private final JobStoreServiceConnector jobStoreServiceConnector;
    private final DMatServiceConnector dmatServiceConnector;
    private final JSONBContext jsonbContext;
    private final ZoneId timezone;

    public HarvestOperation(DMatHarvesterConfig config,
                            BinaryFileStore binaryFileStore,
                            FileStoreServiceConnector fileStoreServiceConnector,
                            FlowStoreServiceConnector flowStoreServiceConnector,
                            JobStoreServiceConnector jobStoreServiceConnector,
                            DMatServiceConnector dmatServiceConnector) {
        this.config = config;
        this.binaryFileStore = binaryFileStore;
        this.fileStoreServiceConnector = fileStoreServiceConnector;
        this.flowStoreServiceConnector = flowStoreServiceConnector;
        this.jobStoreServiceConnector = jobStoreServiceConnector;
        this.dmatServiceConnector = dmatServiceConnector;
        this.jsonbContext = new JSONBContext();
        this.timezone = getTimezone();
    }

    public int execute() throws HarvesterException {
        final StopWatch stopwatch = new StopWatch();
        final Map<Integer, Status> statusAfterExport = new HashMap<>();
        int recordsHarvested = 0;

        try (JobBuilder jobBuilder = new JobBuilder(
                binaryFileStore, fileStoreServiceConnector, jobStoreServiceConnector,
                JobSpecificationTemplate.create(config))) {
            final ResultSet dmatRecords = new ResultSet(dmatServiceConnector);
            for (DMatRecord dmatRecord : dmatRecords) {
                LOGGER.info("Fetched dmat record {}", dmatRecord.getId());
                assertRecordState(dmatRecord);

                final AddiMetaData addiMetaData = createAddiMetaData(dmatRecords.getCreationTime(), dmatRecord);
                try {
                    DBCTrackedLogContext.setTrackingId(addiMetaData.trackingId());
                    statusAfterExport.put(dmatRecord.getId(), Status.EXPORTED);
                    final AddiRecord addiRecord = createAddiRecord(addiMetaData, dmatRecord);
                    jobBuilder.addRecord(addiRecord);
                } finally {
                    DBCTrackedLogContext.remove();
                }
            }

            jobBuilder.build();

            // Wait until dataIO job has been successfully created before updating status
            // to eliminate risk of "losing" dmat cases.
            statusAfterExport.forEach(this::updateStatus);

            updateConfig(config);

            recordsHarvested = jobBuilder.getRecordsAdded();
            return recordsHarvested;
        } catch (DMatServiceConnectorException e) {
            throw new HarvesterException(e);
        } finally {
            LOGGER.info("Harvested {} dmat cases in {} ms", recordsHarvested, stopwatch.getElapsedTime());
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
        if( dmatRecord.getStatus() == null || dmatRecord.getStatus() != Status.PENDING_EXPORT ) {
            LOGGER.error("Received DMatRecord with bad status {} when expecting PENDING_EXPORT",
                    dmatRecord.getStatus() != null ? dmatRecord.getStatus() : "(null)");
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

        // Todo: Check for valid combinations of updateCode and selection
    }

    private AddiMetaData createAddiMetaData(LocalDate creationDate, DMatRecord dmatRecord) {
        return new AddiMetaData()
                .withTrackingId(String.join(".", "dmat", config.getLogId(),
                        String.valueOf(dmatRecord.getId())))
                .withSubmitterNumber(JobSpecificationTemplate.SUBMITTER_NUMBER)
                .withFormat(config.getContent().getFormat())
                .withCreationDate(Date.from(creationDate.atStartOfDay(timezone).toInstant()));
    }

    // Todo: here we should take, not a DMatRecord, but a finished export object (which includes the
    //       visible fields of the dmatrecord
    private AddiRecord createAddiRecord(AddiMetaData addiMetaData, DMatRecord dmatRecord) throws HarvesterException {
        try {
            return new AddiRecord(
                    jsonbContext.marshall(addiMetaData).getBytes(StandardCharsets.UTF_8),
                    new byte[0]);  // Todo: add dmatrecord record and possibly an attached record
        } catch (JSONBException e) {
            LOGGER.error("Unable to marshall ADDI metadata for dmat record {}: {}", dmatRecord.getId(), e.getMessage());
            throw new HarvesterException("Unable to marshall ADDI metadata for dmat record " + dmatRecord.getId(), e);
        }
    }

    private void updateStatus(Integer caseId, Status status) throws UncheckedHarvesterException {
        try {
            dmatServiceConnector.updateRecordStatus(caseId, status);
        } catch (DMatServiceConnectorException e) {
            LOGGER.error("Unable to update status for dmat record {}: {}", caseId, e);
            throw new UncheckedHarvesterException("Unable to update status for dmat record " + caseId, e);
        } catch (JSONBException e) {
            LOGGER.error("Caught JSONBException when updating status for dmat record {}: {}", caseId, e);
            throw new UncheckedHarvesterException("Caught JSONBException when updating status for dmat record " + caseId, e);
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
            throw new HarvesterException("Failed to update harvester config: " + config.toString(), e);
        }
    }

   /* Abstraction over one or more dmat service fetch cycles.
       The purpose of this ResultSet class is to avoid high memory consumption
       both on the server and client side if a very large number of cases need
       to be harvested. */
    private static class ResultSet implements Iterable<DMatRecord> {
        private final DMatServiceConnector dmatServiceConnector;
        private int size;
        private int from;
        private boolean exhausted = false;
        private Iterator<DMatRecord> records;
        private LocalDate creationTime;

        ResultSet(DMatServiceConnector dmatServiceConnector) throws DMatServiceConnectorException {
            this.dmatServiceConnector = dmatServiceConnector;
            this.from = 0;
            this.size = fetchRecords().getNumFound();
        }

        public int getSize() {
            return size;
        }

        public LocalDate getCreationTime() {
            return creationTime;
        }

        @Override
        public Iterator<DMatRecord> iterator() {
            return new Iterator<DMatRecord>() {
                @Override
                public boolean hasNext() {
                    if (!records.hasNext() && !exhausted) {
                        try {
                            size += fetchRecords().getNumFound();
                        } catch (DMatServiceConnectorException e) {
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

        private ExportedRecordList fetchRecords() throws DMatServiceConnectorException {
            Map<String, String> queryParms = new HashMap<>();
            queryParms.put("limit", Integer.toString(DMAT_SERVICE_FETCH_SIZE));
            queryParms.put("from", Integer.toString(from));
            final ExportedRecordList result = dmatServiceConnector.getExportedRecords(queryParms);

            this.records = result.getRecords().iterator();
            this.creationTime = result.getCreationDate();

            if (result.getNumFound() < DMAT_SERVICE_FETCH_SIZE) {
                this.exhausted = true;
            }
            if (result.getNumFound() == DMAT_SERVICE_FETCH_SIZE) {
                from = result.getRecords().get(DMAT_SERVICE_FETCH_SIZE - 1).getId();
            }
            return result;
        }
    }
}
