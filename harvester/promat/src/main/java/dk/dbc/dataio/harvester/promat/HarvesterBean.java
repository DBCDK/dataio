/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.dataio.harvester.promat;

import dk.dbc.dataio.bfs.ejb.BinaryFileStoreBean;
import dk.dbc.dataio.common.utils.flowstore.ejb.FlowStoreServiceConnectorBean;
import dk.dbc.dataio.commons.faust.factory.FaustFactory;
import dk.dbc.dataio.commons.utils.jobstore.ejb.JobStoreServiceConnectorBean;
import dk.dbc.dataio.filestore.service.connector.ejb.FileStoreServiceConnectorBean;
import dk.dbc.dataio.harvester.AbstractHarvesterBean;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.PromatHarvesterConfig;
import dk.dbc.opennumberroll.OpennumberRollConnector;
import dk.dbc.promat.service.connector.PromatServiceConnector;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.inject.Inject;

@Singleton
public class HarvesterBean extends AbstractHarvesterBean<HarvesterBean, PromatHarvesterConfig> {
    private static final Logger LOGGER = LoggerFactory.getLogger(HarvesterBean.class);

    @EJB BinaryFileStoreBean binaryFileStoreBean;
    @EJB FileStoreServiceConnectorBean fileStoreServiceConnectorBean;
    @EJB FlowStoreServiceConnectorBean flowStoreServiceConnectorBean;
    @EJB JobStoreServiceConnectorBean jobStoreServiceConnectorBean;
    @Inject OpennumberRollConnector openNumberRollConnector;
    @Inject PromatServiceConnector promatServiceConnector;

    @Inject
    @ConfigProperty(name = "OPEN_NUMBER_ROLL_NAME")
    private String openNumberRollName;

    @Override
    public int executeFor(PromatHarvesterConfig config) throws HarvesterException {
        final HarvestOperation harvestOperation = new HarvestOperation(config,
                binaryFileStoreBean,
                fileStoreServiceConnectorBean.getConnector(),
                flowStoreServiceConnectorBean.getConnector(),
                jobStoreServiceConnectorBean.getConnector(),
                promatServiceConnector,
                new FaustFactory(openNumberRollConnector, openNumberRollName));
        return harvestOperation.execute();
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
