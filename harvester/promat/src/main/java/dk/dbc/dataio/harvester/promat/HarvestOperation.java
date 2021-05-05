/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.dataio.harvester.promat;

import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.bfs.api.BinaryFileStore;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorUnexpectedStatusCodeException;
import dk.dbc.dataio.commons.faust.factory.FaustFactory;
import dk.dbc.dataio.commons.time.StopWatch;
import dk.dbc.dataio.commons.types.AddiMetaData;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnector;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.PromatHarvesterConfig;
import dk.dbc.dataio.harvester.types.UncheckedHarvesterException;
import dk.dbc.log.DBCTrackedLogContext;
import dk.dbc.promat.service.connector.PromatServiceConnector;
import dk.dbc.promat.service.connector.PromatServiceConnectorException;
import dk.dbc.promat.service.dto.CaseRequest;
import dk.dbc.promat.service.dto.CaseSummaryList;
import dk.dbc.promat.service.dto.CriteriaOperator;
import dk.dbc.promat.service.dto.ListCasesParams;
import dk.dbc.promat.service.persistence.CaseStatus;
import dk.dbc.promat.service.persistence.PromatCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.IsoFields;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class HarvestOperation {
    private static final Logger LOGGER = LoggerFactory.getLogger(HarvestOperation.class);

    static int PROMAT_SERVICE_FETCH_SIZE = 100;

    private final PromatHarvesterConfig config;
    private final BinaryFileStore binaryFileStore;
    private final FileStoreServiceConnector fileStoreServiceConnector;
    private final FlowStoreServiceConnector flowStoreServiceConnector;
    private final JobStoreServiceConnector jobStoreServiceConnector;
    private final PromatServiceConnector promatServiceConnector;
    private final FaustFactory faustFactory;
    private final JSONBContext jsonbContext;
    private final PromatCaseXmlTransformer promatCaseXmlTransformer;
    private final ZoneId timezone;

    public HarvestOperation(PromatHarvesterConfig config,
                            BinaryFileStore binaryFileStore,
                            FileStoreServiceConnector fileStoreServiceConnector,
                            FlowStoreServiceConnector flowStoreServiceConnector,
                            JobStoreServiceConnector jobStoreServiceConnector,
                            PromatServiceConnector promatServiceConnector,
                            FaustFactory faustFactory) {
        this.config = config;
        this.binaryFileStore = binaryFileStore;
        this.fileStoreServiceConnector = fileStoreServiceConnector;
        this.flowStoreServiceConnector = flowStoreServiceConnector;
        this.jobStoreServiceConnector = jobStoreServiceConnector;
        this.promatServiceConnector = promatServiceConnector;
        this.faustFactory = faustFactory;
        this.jsonbContext = new JSONBContext();
        this.promatCaseXmlTransformer = new PromatCaseXmlTransformer();
        this.timezone = getTimezone();
    }

    public int execute() throws HarvesterException {
        final StopWatch stopwatch = new StopWatch();
        final Map<Integer, CaseStatus> statusAfterExport = new HashMap<>();
        int recordsHarvested = 0;

        try (JobBuilder jobBuilder = new JobBuilder(
                binaryFileStore, fileStoreServiceConnector, jobStoreServiceConnector,
                JobSpecificationTemplate.create(config))) {

            final ResultSet promatCases = new ResultSet(promatServiceConnector);
            for (PromatCase promatCase : promatCases) {
                LOGGER.info("Fetched promat case {}", promatCase.getId());
                promatCase = ensureRecordIdIsSet(promatCase);
                final AddiMetaData addiMetaData = createAddiMetaData(promatCase);
                try {
                    DBCTrackedLogContext.setTrackingId(addiMetaData.trackingId());
                    statusAfterExport.put(promatCase.getId(), addiMetaData.isDeleted() ?
                            CaseStatus.REVERTED : CaseStatus.EXPORTED);
                    final AddiRecord addiRecord = createAddiRecord(addiMetaData, promatCase);
                    jobBuilder.addRecord(addiRecord);
                } finally {
                    DBCTrackedLogContext.remove();
                }
            }

            jobBuilder.build();

            // Wait until dataIO job has been successfully created before updating status
            // to eliminate risk of "losing" Promat cases.
            statusAfterExport.forEach(this::updateStatus);

            updateConfig(config);
            recordsHarvested = jobBuilder.getRecordsAdded();
            return recordsHarvested;
        } catch (PromatServiceConnectorException e) {
            throw new HarvesterException(e);
        } finally {
            LOGGER.info("Harvested {} promat cases in {} ms", recordsHarvested, stopwatch.getElapsedTime());
        }
    }

    private static String getWeekcode() {
        final ZonedDateTime zonedDateTime = Instant.now().atZone(getTimezone());
        return String.format("%d%02d",  zonedDateTime.getYear(), zonedDateTime.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR));
    }

    private static ZoneId getTimezone() {
        String tzEnv = System.getenv("TZ");
        if (tzEnv == null) {
            tzEnv = "Europe/Copenhagen";
        }
        return ZoneId.of(tzEnv);
    }

    private PromatCase ensureRecordIdIsSet(PromatCase promatCase) throws HarvesterException {
        if (promatCase.getRecordId() == null) {
            LOGGER.info("Obtaining new faust number for promat case {}", promatCase.getId());
            try {
                promatCase.setRecordId(faustFactory.newFaust());
                // Upload the obtained faust number immediately to
                // avoid unnecessary withdrawals from the number roll
                // in case of errors resulting in re-harvesting.
                promatServiceConnector.updateCase(promatCase.getId(), new CaseRequest()
                        .withRecordId(promatCase.getRecordId()));
                // Do not return the updated case from the response
                // since the update endpoint does not respect the
                // export format and the returned case actually
                // violates the GDPR.
                return promatCase;
            } catch (IllegalStateException e) {
                throw new HarvesterException(e);
            } catch (PromatServiceConnectorException e) {
                throw new HarvesterException("Unable to set recordId for promat case " +
                        promatCase.getId(), e);
            }
        }
        LOGGER.info("Using recordId {} for promat case {}", promatCase.getRecordId(), promatCase.getId());
        return promatCase;
    }

    private AddiMetaData createAddiMetaData(PromatCase promatCase) {
        final AddiMetaData metaData = new AddiMetaData()
                .withTrackingId(String.join(".", "promat", config.getLogId(), promatCase.getRecordId()))
                .withBibliographicRecordId(promatCase.getRecordId())
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

    private void updateStatus(Integer caseId, CaseStatus caseStatus) throws UncheckedHarvesterException {
        try {
            promatServiceConnector.updateCase(caseId, new CaseRequest().withStatus(caseStatus));
        } catch (PromatServiceConnectorException e) {
            throw new UncheckedHarvesterException("Unable to update status for promat case " + caseId, e);
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
        private int size;
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
            this.size = fetchCases().getNumFound();
        }

        public int getSize() {
            return size;
        }

        @Override
        public Iterator<PromatCase> iterator() {
            return new Iterator<PromatCase>() {
                @Override
                public boolean hasNext() {
                    if (!cases.hasNext() && !exhausted) {
                        try {
                            size += fetchCases().getNumFound();
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

        private CaseSummaryList fetchCases() throws PromatServiceConnectorException {
            final CaseSummaryList result = promatServiceConnector.listCases(listCasesParams);
            this.cases = result.getCases().iterator();
            if (result.getNumFound() < listCasesParams.getLimit()) {
                this.exhausted = true;
            }
            if (result.getNumFound() == listCasesParams.getLimit()) {
                listCasesParams.withFrom(result.getCases().get(PROMAT_SERVICE_FETCH_SIZE - 1).getId());
            }
            return result;
        }
    }
}
