/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.dataio.harvester.promat;

import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.dataio.bfs.ejb.BinaryFileStoreBean;
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
import dk.dbc.dataio.jsonb.JSONBContext;
import dk.dbc.dataio.jsonb.JSONBException;
import dk.dbc.log.DBCTrackedLogContext;
import dk.dbc.promat.service.connector.PromatServiceConnector;
import dk.dbc.promat.service.connector.PromatServiceConnectorException;
import dk.dbc.promat.service.dto.CaseRequestDto;
import dk.dbc.promat.service.dto.CaseSummaryList;
import dk.dbc.promat.service.persistence.CaseStatus;
import dk.dbc.promat.service.persistence.PromatCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Iterator;

public class HarvestOperation {
    private static final Logger LOGGER = LoggerFactory.getLogger(HarvestOperation.class);

    static int PROMAT_SERVICE_FETCH_SIZE = 100;

    private final PromatHarvesterConfig config;
    private final BinaryFileStoreBean binaryFileStoreBean;
    private final FileStoreServiceConnector fileStoreServiceConnector;
    private final FlowStoreServiceConnector flowStoreServiceConnector;
    private final JobStoreServiceConnector jobStoreServiceConnector;
    private final PromatServiceConnector promatServiceConnector;
    private final FaustFactory faustFactory;
    private final JSONBContext jsonbContext;
    private final PromatCaseXmlTransformer promatCaseXmlTransformer;

    public HarvestOperation(PromatHarvesterConfig config,
                            BinaryFileStoreBean binaryFileStoreBean,
                            FileStoreServiceConnector fileStoreServiceConnector,
                            FlowStoreServiceConnector flowStoreServiceConnector,
                            JobStoreServiceConnector jobStoreServiceConnector,
                            PromatServiceConnector promatServiceConnector,
                            FaustFactory faustFactory) {
        this.config = config;
        this.binaryFileStoreBean = binaryFileStoreBean;
        this.fileStoreServiceConnector = fileStoreServiceConnector;
        this.flowStoreServiceConnector = flowStoreServiceConnector;
        this.jobStoreServiceConnector = jobStoreServiceConnector;
        this.promatServiceConnector = promatServiceConnector;
        this.faustFactory = faustFactory;
        this.jsonbContext = new JSONBContext();
        this.promatCaseXmlTransformer = new PromatCaseXmlTransformer();
    }

    public int execute() throws HarvesterException {
        final StopWatch stopwatch = new StopWatch();
        int recordsHarvested = 0;

        try (JobBuilder jobBuilder = new JobBuilder(
                binaryFileStoreBean, fileStoreServiceConnector, jobStoreServiceConnector,
                JobSpecificationTemplate.create(config))) {

            final ResultSet promatCases = new ResultSet(promatServiceConnector);
            for (PromatCase promatCase : promatCases) {
                LOGGER.info("Fetched promat case {}", promatCase.getId());
                promatCase = ensureRecordIdIsSet(promatCase);
                final AddiMetaData addiMetaData = createAddiMetaData(promatCase);
                try {
                    DBCTrackedLogContext.setTrackingId(addiMetaData.trackingId());
                    final AddiRecord addiRecord = createAddiRecord(addiMetaData, promatCase);
                    jobBuilder.addRecord(addiRecord);
                } finally {
                    DBCTrackedLogContext.remove();
                }
            }

            jobBuilder.build();
            updateConfig(config);
            return jobBuilder.getRecordsAdded();
        } catch (PromatServiceConnectorException e) {
            throw new HarvesterException(e);
        } finally {
            LOGGER.info("Harvested {} promat cases in {} ms", recordsHarvested, stopwatch.getElapsedTime());
        }
    }

    private PromatCase ensureRecordIdIsSet(PromatCase promatCase) throws HarvesterException {
        if (promatCase.getRecordId() == null) {
            LOGGER.info("Obtaining new faust number for promat case {}", promatCase.getId());
            try {
                promatCase.setRecordId(faustFactory.newFaust());
                // Upload the obtained faust number immediately to
                // avoid unnecessary withdrawals from the number roll
                // in case of errors resulting in re-harvesting.
                promatCase = promatServiceConnector.updateCase(promatCase.getId(), new CaseRequestDto()
                        .withRecordId(promatCase.getRecordId()));
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
        return new AddiMetaData()
                .withTrackingId(String.join(".", "promat", config.getLogId(), promatCase.getRecordId()))
                .withBibliographicRecordId(promatCase.getRecordId())
                .withSubmitterNumber(JobSpecificationTemplate.SUBMITTER_NUMBER)
                .withFormat(config.getContent().getFormat())
                .withDeleted(promatCase.getStatus() == CaseStatus.PENDING_REVERT);
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
        private final PromatServiceConnector.ListCasesParams listCasesParams;
        private int size;
        private boolean exhausted = false;
        private Iterator<PromatCase> cases;

        ResultSet(PromatServiceConnector promatServiceConnector) throws PromatServiceConnectorException {
            this.promatServiceConnector = promatServiceConnector;
            // TODO: 14/01/2021 also handle weekcode filtering
            this.listCasesParams = new PromatServiceConnector.ListCasesParams()
                    .withFormat(PromatServiceConnector.ListCasesParams.Format.EXPORT)
                    .withStatus(CaseStatus.PENDING_EXPORT)
                    .withStatus(CaseStatus.PENDING_REVERT)
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
