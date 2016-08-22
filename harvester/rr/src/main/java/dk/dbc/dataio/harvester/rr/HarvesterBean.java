/*
 * DataIO - Data IO
 * Copyright (C) 2015 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
 * Denmark. CVR: 15149043
 *
 * This file is part of DataIO.
 *
 * DataIO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DataIO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DataIO.  If not, see <http://www.gnu.org/licenses/>.
 */

package dk.dbc.dataio.harvester.rr;

import dk.dbc.dataio.bfs.ejb.BinaryFileStoreBean;
import dk.dbc.dataio.commons.utils.jobstore.ejb.JobStoreServiceConnectorBean;
import dk.dbc.dataio.filestore.service.connector.ejb.FileStoreServiceConnectorBean;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.RRHarvesterConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import javax.annotation.Resource;
import javax.ejb.AsyncResult;
import javax.ejb.Asynchronous;
import javax.ejb.EJB;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.SessionContext;
import javax.ejb.Singleton;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;
import java.util.concurrent.Future;

/**
 * This Enterprise Java Bean (EJB) handles an actual RawRepo harvest
 */
@Singleton
public class HarvesterBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(HarvesterBean.class);
    private static final String HARVESTER_MDC_KEY = "HARVESTER_ID";

    @Resource
    SessionContext sessionContext;

    @PersistenceUnit(unitName = "harvesterRR_PU")
    EntityManagerFactory entityManagerFactory;

    @EJB
    public BinaryFileStoreBean binaryFileStoreBean;

    @EJB
    public FileStoreServiceConnectorBean fileStoreServiceConnectorBean;

    @EJB
    public JobStoreServiceConnectorBean jobStoreServiceConnectorBean;

    /**
     * Executes harvest operation in batches (each batch in its own transactional
     * scope to avoid tearing down any controlling timers in case of an exception)
     * creating the corresponding jobs in the job-store if data is retrieved.
     * @param config harvest configuration
     * @return number of items harvested
     * @throws IllegalStateException on low-level binary file operation failure
     * @throws HarvesterException on failure to complete harvest operation
     */
    @Asynchronous
    @Lock(LockType.READ)
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public Future<Integer> harvest(RRHarvesterConfig config) throws HarvesterException {
        LOGGER.debug("Called with config {}", config);
        try {
            MDC.put(HARVESTER_MDC_KEY, config.getContent().getId());
            final HarvesterBean businessObject = sessionContext.getBusinessObject(HarvesterBean.class);
            final HarvestOperation harvestOperation = getHarvestOperation(config);
            int itemsHarvested = businessObject.execute(harvestOperation);
            return new AsyncResult<>(itemsHarvested);
        } finally {
            MDC.remove(HARVESTER_MDC_KEY);
        }
    }

    /**
     * Executes harvest operation
     * @param harvestOperation harvest operation
     * @return number of items harvested in batch
     * @throws IllegalStateException on low-level binary file operation failure
     * @throws HarvesterException on failure to complete harvest operation
     */
    @Lock(LockType.READ)
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public int execute(HarvestOperation harvestOperation) throws HarvesterException {
        EntityManager entityManager = null;
        try {
            entityManager = entityManagerFactory.createEntityManager();
            entityManager.joinTransaction();
            return harvestOperation.execute(entityManager);
        } finally {
            if (entityManager != null && entityManager.isOpen()) {
               entityManager.close();
            }
        }
    }

    public HarvestOperation getHarvestOperation(RRHarvesterConfig config) {
        final HarvesterJobBuilderFactory harvesterJobBuilderFactory = new HarvesterJobBuilderFactory(
                binaryFileStoreBean, fileStoreServiceConnectorBean.getConnector(), jobStoreServiceConnectorBean.getConnector());

        if (config.getContent().isImsHarvester()) {
            return new ImsHarvestOperation(config, harvesterJobBuilderFactory);
        }
        return new HarvestOperation(config, harvesterJobBuilderFactory);
    }
}
