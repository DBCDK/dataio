/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.dataio.harvester.promat;

import dk.dbc.dataio.bfs.ejb.BinaryFileStoreBean;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnector;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.PromatHarvesterConfig;
import dk.dbc.promat.service.connector.PromatServiceConnector;
import dk.dbc.promat.service.connector.PromatServiceConnectorException;
import dk.dbc.promat.service.dto.CaseSummaryList;
import dk.dbc.promat.service.persistence.CaseStatus;
import dk.dbc.promat.service.persistence.PromatCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    public HarvestOperation(PromatHarvesterConfig config,
                            BinaryFileStoreBean binaryFileStoreBean,
                            FileStoreServiceConnector fileStoreServiceConnector,
                            FlowStoreServiceConnector flowStoreServiceConnector,
                            JobStoreServiceConnector jobStoreServiceConnector,
                            PromatServiceConnector promatServiceConnector) {
        this.config = config;
        this.binaryFileStoreBean = binaryFileStoreBean;
        this.fileStoreServiceConnector = fileStoreServiceConnector;
        this.flowStoreServiceConnector = flowStoreServiceConnector;
        this.jobStoreServiceConnector = jobStoreServiceConnector;
        this.promatServiceConnector = promatServiceConnector;
    }

    public int execute() throws HarvesterException {
        try {
            // TODO: 14/01/2021 create dataIO job out of harvested cases.

            final ResultSet promatCases = new ResultSet(promatServiceConnector);
            for (PromatCase promatCase : promatCases) {
                LOGGER.info("Harvested promat case {}", promatCase.getId());
            }
            return promatCases.getSize();
        } catch (PromatServiceConnectorException e) {
            throw new HarvesterException(e);
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
