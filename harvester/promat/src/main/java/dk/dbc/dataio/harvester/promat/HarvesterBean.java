/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.dataio.harvester.promat;

import dk.dbc.dataio.bfs.ejb.BinaryFileStoreBean;
import dk.dbc.dataio.common.utils.flowstore.ejb.FlowStoreServiceConnectorBean;
import dk.dbc.dataio.commons.utils.jobstore.ejb.JobStoreServiceConnectorBean;
import dk.dbc.dataio.filestore.service.connector.ejb.FileStoreServiceConnectorBean;
import dk.dbc.dataio.harvester.AbstractHarvesterBean;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.PromatHarvesterConfig;
import dk.dbc.promat.service.connector.PromatServiceConnector;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.MetricType;
import org.eclipse.microprofile.metrics.annotation.RegistryType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.inject.Inject;

@Singleton
public class HarvesterBean extends AbstractHarvesterBean<HarvesterBean, PromatHarvesterConfig> {
    private static final Logger LOGGER = LoggerFactory.getLogger(HarvesterBean.class);

    static final Metadata caseCounterMetadata = Metadata.builder()
            .withName("dataio_harvester_promat_case_counter")
            .withDescription("Number of cases harvested")
            .withType(MetricType.COUNTER)
            .withUnit("case")
            .build();
    static final Metadata exceptionCounterMetadata = Metadata.builder()
            .withName("dataio_harvester_promat_exception_counter")
            .withDescription("Number of unhandled exceptions caught")
            .withType(MetricType.COUNTER)
            .withUnit("exception")
            .build();

    @EJB BinaryFileStoreBean binaryFileStoreBean;
    @EJB FileStoreServiceConnectorBean fileStoreServiceConnectorBean;
    @EJB FlowStoreServiceConnectorBean flowStoreServiceConnectorBean;
    @EJB JobStoreServiceConnectorBean jobStoreServiceConnectorBean;
    @Inject PromatServiceConnector promatServiceConnector;

    @Inject
    @RegistryType(type = MetricRegistry.Type.APPLICATION)
    MetricRegistry metricRegistry;

    @Override
    public int executeFor(PromatHarvesterConfig config) throws HarvesterException {
        try {
            final HarvestOperation harvestOperation = new HarvestOperation(config,
                    binaryFileStoreBean,
                    fileStoreServiceConnectorBean.getConnector(),
                    flowStoreServiceConnectorBean.getConnector(),
                    jobStoreServiceConnectorBean.getConnector(),
                    promatServiceConnector);
            final int numberOfCasesHarvested = harvestOperation.execute();
            metricRegistry.counter(caseCounterMetadata).inc(numberOfCasesHarvested);
            return numberOfCasesHarvested;
        } catch (HarvesterException | RuntimeException e) {
            metricRegistry.counter(exceptionCounterMetadata).inc();
            throw e;
        }
    }

    @Override
    public HarvesterBean self() {
        return sessionContext.getBusinessObject(HarvesterBean.class);
    }

    @Override
    public Logger getLogger() {
        return LOGGER;
    }
}
