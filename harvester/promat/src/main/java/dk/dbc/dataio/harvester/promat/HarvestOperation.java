package dk.dbc.dataio.harvester.promat;

import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.commons.metricshandler.MetricsHandlerBean;
import dk.dbc.dataio.bfs.api.BinaryFileStore;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorUnexpectedStatusCodeException;
import dk.dbc.dataio.commons.time.StopWatch;
import dk.dbc.dataio.commons.types.AddiMetaData;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnector;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.PromatHarvesterConfig;
import dk.dbc.dataio.harvester.types.UncheckedHarvesterException;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.log.DBCTrackedLogContext;
import dk.dbc.promat.service.connector.PromatServiceConnector;
import dk.dbc.promat.service.connector.PromatServiceConnectorException;
import dk.dbc.promat.service.dto.CaseRequest;
import dk.dbc.promat.service.dto.CaseSummaryList;
import dk.dbc.promat.service.dto.CriteriaOperator;
import dk.dbc.promat.service.dto.ListCasesParams;
import dk.dbc.promat.service.persistence.CaseStatus;
import dk.dbc.promat.service.persistence.PromatCase;
import dk.dbc.promat.service.persistence.PromatTask;
import dk.dbc.promat.service.persistence.TaskFieldType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.IsoFields;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

public class HarvestOperation {
    private static final Logger LOGGER = LoggerFactory.getLogger(HarvestOperation.class);

    static int PROMAT_SERVICE_FETCH_SIZE = 100;

    private final PromatHarvesterConfig config;
    private final BinaryFileStore binaryFileStore;
    private final FileStoreServiceConnector fileStoreServiceConnector;
    private final FlowStoreServiceConnector flowStoreServiceConnector;
    private final JobStoreServiceConnector jobStoreServiceConnector;
    private final PromatServiceConnector promatServiceConnector;
    private final JSONBContext jsonbContext;
    private final PromatCaseXmlTransformer promatCaseXmlTransformer;
    private final ZoneId timezone;

    private final MetricsHandlerBean metricsHandler;

    public HarvestOperation(PromatHarvesterConfig config,
                            BinaryFileStore binaryFileStore,
                            FileStoreServiceConnector fileStoreServiceConnector,
                            FlowStoreServiceConnector flowStoreServiceConnector,
                            JobStoreServiceConnector jobStoreServiceConnector,
                            PromatServiceConnector promatServiceConnector,
                            MetricsHandlerBean metricsHandler) {
        this.config = config;
        this.binaryFileStore = binaryFileStore;
        this.fileStoreServiceConnector = fileStoreServiceConnector;
        this.flowStoreServiceConnector = flowStoreServiceConnector;
        this.jobStoreServiceConnector = jobStoreServiceConnector;
        this.promatServiceConnector = promatServiceConnector;
        this.jsonbContext = new JSONBContext();
        this.promatCaseXmlTransformer = new PromatCaseXmlTransformer();
        this.timezone = getTimezone();
        this.metricsHandler = metricsHandler;
    }

    public int execute() throws HarvesterException {
        final StopWatch stopwatch = new StopWatch();
        final Map<Integer, CaseStatus> statusAfterExport = new HashMap<>();
        int recordsHarvested = 0;
        int recordsProcessed = 0;
        int recordsSkipped = 0;
        AtomicInteger recordsFailed = new AtomicInteger(0);

        try (JobBuilder jobBuilder = new JobBuilder(
                binaryFileStore, fileStoreServiceConnector, jobStoreServiceConnector,
                JobSpecificationTemplate.create(config))) {

            // Set all received records to status PROCESSING to prevent them from being
            // exported again in case of catastrophic errors in promat or the harvester
            final ResultSet promatCases = new ResultSet(promatServiceConnector);
            for (PromatCase promatCase : promatCases) {
                recordsHarvested++;
                LOGGER.info("Fetched promat case {}", promatCase.getId());

                // Move record to status PROCESSING to prevent reexport, skip record if we can not set new status
                if (!updateStatus(promatCase.getId(), CaseStatus.PROCESSING)) {
                    LOGGER.error("Unable to set status PROCESSING on Promat record {}. Harvesting aborted for this record", promatCase.getId());
                    recordsSkipped++;
                    continue;
                }

                // Process record
                try {
                    // Check validity of the received record
                    assertRecordIds(promatCase);

                    final AddiMetaData addiMetaData = createAddiMetaData(promatCase);
                    try {
                        // Create the addi object and add it to the job
                        DBCTrackedLogContext.setTrackingId(addiMetaData.trackingId());
                        final AddiRecord addiRecord = createAddiRecord(addiMetaData, promatCase);
                        jobBuilder.addRecord(addiRecord);
                        statusAfterExport.put(promatCase.getId(), addiMetaData.isDeleted() ?
                                CaseStatus.REVERTED : CaseStatus.EXPORTED);
                        recordsProcessed++;
                        LOGGER.info("Added processed Promat record {} to job", promatCase.getId());
                    } finally {
                        DBCTrackedLogContext.remove();
                    }
                } catch (HarvesterException e) {
                    LOGGER.error("Caught HarvesterException for promat record {}: {}",
                            promatCase.getId(), e.getMessage());
                    statusAfterExport.put(promatCase.getId(), promatCase.getStatus());
                    recordsSkipped++;
                } catch (Exception e) {
                    LOGGER.error("Caught unexpected Exception for dmatrecord {}", promatCase.getId(), e);
                    statusAfterExport.put(promatCase.getId(), promatCase.getStatus());
                    recordsSkipped++;
                }
            }

            // Create job
            if (jobBuilder.getRecordsAdded() > 0) {
                int recordsAdded = jobBuilder.getRecordsAdded();
                Optional<JobInfoSnapshot> jobInfo = jobBuilder.build();
                jobInfo.ifPresent(jobInfoSnapshot -> {
                    LOGGER.info("Created job {} with {} items", jobInfoSnapshot.getJobId(), recordsAdded);
                    metricsHandler.increment(PromatHarvesterMetrics.RECORDS_ADDED, recordsAdded);
                });
            } else if (recordsHarvested == 0) {
                LOGGER.info("No new records harvested from Promat");
            } else {
                LOGGER.error("Did not create job for Promat harvester since 0 records was added to the job during processing");
            }

            // After the job for RR records has been successfully build, update the status of the
            // harvested promat records to ensure that no record is marked as exported
            // if job creation fails
            statusAfterExport.forEach((caseId, status) -> {
                if (!updateStatus(caseId, status)) {
                    recordsFailed.incrementAndGet();
                }
            });
            updateConfig(config);

            return recordsProcessed;
        } catch (PromatServiceConnectorException e) {
            LOGGER.error(String.format("Caught unexpected PromatServiceConnectorException: %s", e.getMessage()), e);
            LOGGER.error("Promat may now have stale records in status PROCESSING");
            metricsHandler.increment(PromatHarvesterMetrics.EXCEPTIONS);
            throw new HarvesterException("Caught DMatServiceConnectorException", e);
        } catch (HarvesterException e) {
            LOGGER.error(String.format("Caught HarvesterException: %s", e.getMessage()), e);
            LOGGER.error("Promat may now have stale records in status PROCESSING");
            metricsHandler.increment(PromatHarvesterMetrics.EXCEPTIONS);
            throw e;
        } catch (Exception e) {
            LOGGER.error(String.format("Caught unexpected Exception: %s", e.getMessage()), e);
            LOGGER.error("Promat may now have stale records in status PROCESSING");
            metricsHandler.increment(PromatHarvesterMetrics.UNHANDLED_EXCEPTIONS);
            throw new HarvesterException("Caught Exception", e);
        } finally {
            LOGGER.info("Harvested {} promat cases. {} was processed, {} was skipped, {} failed in {} ms", recordsHarvested,
                    recordsProcessed, recordsSkipped, recordsFailed.get(), stopwatch.getElapsedTime());
            metricsHandler.increment(PromatHarvesterMetrics.RECORDS_HARVESTED, recordsHarvested);
            metricsHandler.increment(PromatHarvesterMetrics.RECORDS_PROCESSED, recordsProcessed);
            metricsHandler.increment(PromatHarvesterMetrics.RECORDS_FAILED, recordsFailed.get() + recordsSkipped);
        }
    }

    static String getWeekcode() {
        return getWeekcode(Instant.now().atZone(getTimezone()));
    }

    // For testability
    static String getWeekcode(ZonedDateTime zonedDateTime) {
        if (zonedDateTime == null) {
            return null;
        }
        // Shiftday is friday
        if (DayOfWeek.from(zonedDateTime).getValue() >= 5) {
            zonedDateTime = zonedDateTime.plusWeeks(1);
        }
        return String.format("%d%02d", zonedDateTime.getYear(), zonedDateTime.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR));
    }

    private static ZoneId getTimezone() {
        String tzEnv = System.getenv("TZ");
        if (tzEnv == null) {
            tzEnv = "Europe/Copenhagen";
        }
        return ZoneId.of(tzEnv);
    }

    private void assertRecordIds(PromatCase promatCase) throws HarvesterException {
        final List<PromatTask> tasks = promatCase.getTasks();
        if (tasks == null || tasks.isEmpty()) {
            throw new HarvesterException(String.format("Case %d contains no tasks", promatCase.getId()));
        }
        if (tasks.stream()
                .filter(task -> task.getTaskFieldType() == TaskFieldType.BRIEF)
                .map(PromatTask::getRecordId)
                .anyMatch(recordId -> recordId == null || recordId.trim().isEmpty())) {
            throw new HarvesterException(String.format("Case %d contains BRIEF tasks without record ID",
                    promatCase.getId()));
        }
    }

    private AddiMetaData createAddiMetaData(PromatCase promatCase) {
        final AddiMetaData metaData = new AddiMetaData()
                .withTrackingId(String.join(".", "promat", config.getLogId(),
                        String.valueOf(promatCase.getId())))
                .withSubmitterNumber(JobSpecificationTemplate.SUBMITTER_NUMBER)
                .withFormat(config.getContent().getFormat())
                .withDeleted(promatCase.getStatus() == CaseStatus.PENDING_REVERT);
        if (promatCase.getCreated() != null) {
            metaData.withCreationDate(Date.from(promatCase.getCreated().atStartOfDay(timezone).toInstant()));
        }
        return metaData;
    }

    private AddiRecord createAddiRecord(AddiMetaData addiMetaData, PromatCase promatCase) throws HarvesterException {
        try {
            return new AddiRecord(
                    jsonbContext.marshall(addiMetaData).getBytes(StandardCharsets.UTF_8),
                    promatCaseXmlTransformer.toXml(promatCase));
        } catch (JSONBException e) {
            throw new HarvesterException("Unable to marshall ADDI metadata for promat case " + promatCase.getId());
        }
    }

    private Boolean updateStatus(Integer caseId, CaseStatus caseStatus) throws UncheckedHarvesterException {
        try {
            promatServiceConnector.updateCase(caseId, new CaseRequest().withStatus(caseStatus));
            LOGGER.info("promat record {} set to status {}", caseId, caseStatus);
            return true;
        } catch (PromatServiceConnectorException e) {
            LOGGER.error(String.format("Unable to update status to %s for dmat record %d due to PromatServiceConnectorException", caseStatus, caseId), e);
            return false;
        } catch (Exception e) {
            LOGGER.error(String.format("Caught unexpected exception when updating status to %s for promat record %d", caseStatus, caseId), e);
            return false;
        }
    }

    private PromatHarvesterConfig updateConfig(PromatHarvesterConfig config) throws HarvesterException {
        final Date timeOfLastHarvest = new Date();
        config.getContent().withTimeOfLastHarvest(timeOfLastHarvest);

        try {
            return flowStoreServiceConnector.updateHarvesterConfig(config);
        } catch (FlowStoreServiceConnectorException | RuntimeException e) {
            // Handle concurrency conflicts
            if (e instanceof FlowStoreServiceConnectorUnexpectedStatusCodeException
                    && ((FlowStoreServiceConnectorUnexpectedStatusCodeException) e).getStatusCode() == 409) {
                try {
                    final PromatHarvesterConfig refreshedConfig = flowStoreServiceConnector.getHarvesterConfig(
                            config.getId(), PromatHarvesterConfig.class);

                    refreshedConfig.getContent().withTimeOfLastHarvest(timeOfLastHarvest);
                    return updateConfig(refreshedConfig);
                } catch (FlowStoreServiceConnectorException fssce) {
                    LOGGER.error("Error refreshing config {}", config.getId(), fssce);
                }
            }
            throw new HarvesterException("Failed to update harvester config: " + config.toString(), e);
        }
    }

    /* Abstraction over one or more promat service fetch cycles.
        The purpose of this ResultSet class is to avoid high memory consumption
        both on the server and client side if a very large number of cases need
        to be harvested. */
    private static class ResultSet implements Iterable<PromatCase> {
        private final PromatServiceConnector promatServiceConnector;
        private final ListCasesParams listCasesParams;
        private boolean exhausted = false;
        private Iterator<PromatCase> cases;

        ResultSet(PromatServiceConnector promatServiceConnector) throws PromatServiceConnectorException {
            this.promatServiceConnector = promatServiceConnector;
            this.listCasesParams = new ListCasesParams()
                    .withFormat(ListCasesParams.Format.EXPORT)
                    .withStatus(CaseStatus.PENDING_EXPORT)
                    .withStatus(CaseStatus.PENDING_REVERT)
                    .withTrimmedWeekcodeOperator(CriteriaOperator.LESS_THAN_OR_EQUAL_TO)
                    .withTrimmedWeekcode(getWeekcode())
                    .withLimit(PROMAT_SERVICE_FETCH_SIZE)
                    .withFrom(0);
            fetchCases();
        }

        @Override
        @Nonnull
        public Iterator<PromatCase> iterator() {
            return new Iterator<PromatCase>() {
                @Override
                public boolean hasNext() {
                    if (!cases.hasNext() && !exhausted) {
                        try {
                            fetchCases();
                        } catch (PromatServiceConnectorException e) {
                            throw new IllegalStateException(e);
                        }
                    }
                    return cases.hasNext();
                }

                @Override
                public PromatCase next() {
                    return cases.next();
                }
            };
        }

        private void fetchCases() throws PromatServiceConnectorException {
            final CaseSummaryList result = promatServiceConnector.listCases(listCasesParams);
            this.cases = result.getCases().iterator();
            if (result.getNumFound() < listCasesParams.getLimit()) {
                this.exhausted = true;
            }
            if (result.getNumFound() == listCasesParams.getLimit()) {
                listCasesParams.withFrom(result.getCases().get(PROMAT_SERVICE_FETCH_SIZE - 1).getId());
            }
        }
    }
}
