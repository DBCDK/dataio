/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.dataio.harvester.infomedia;

import dk.dbc.dataio.bfs.ejb.BinaryFileStoreBean;
import dk.dbc.dataio.common.utils.flowstore.ejb.FlowStoreServiceConnectorBean;
import dk.dbc.dataio.commons.utils.jobstore.ejb.JobStoreServiceConnectorBean;
import dk.dbc.dataio.filestore.service.connector.ejb.FileStoreServiceConnectorBean;
import dk.dbc.dataio.harvester.AbstractHarvesterBean;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.InfomediaHarvesterConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.EJB;
import javax.ejb.Singleton;

@Singleton
public class HarvesterBean extends AbstractHarvesterBean<HarvesterBean, InfomediaHarvesterConfig> {
    private static final Logger LOGGER = LoggerFactory.getLogger(HarvesterBean.class);

    @EJB BinaryFileStoreBean binaryFileStoreBean;
    @EJB FileStoreServiceConnectorBean fileStoreServiceConnectorBean;
    @EJB FlowStoreServiceConnectorBean flowStoreServiceConnectorBean;
    @EJB JobStoreServiceConnectorBean jobStoreServiceConnectorBean;

    // TODO: 15-01-19 inject Infomedia connector;

    @Override
    public int executeFor(InfomediaHarvesterConfig config) throws HarvesterException {
        return new HarvestOperation(config,
                binaryFileStoreBean,
                flowStoreServiceConnectorBean.getConnector(),
                fileStoreServiceConnectorBean.getConnector(),
                jobStoreServiceConnectorBean.getConnector())
                .execute();
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
