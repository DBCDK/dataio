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

import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.RRHarvesterConfig;
import dk.dbc.rawrepo.queue.ConfigurationException;
import dk.dbc.rawrepo.queue.QueueException;
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
import java.sql.SQLException;
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

    @EJB
    HarvestOperationFactoryBean harvestOperationFactory;

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
    public Future<Integer> harvest(RRHarvesterConfig config) throws HarvesterException, SQLException, QueueException, ConfigurationException {
        LOGGER.debug("Called with config {}", config);
        try {
            MDC.put(HARVESTER_MDC_KEY, config.getContent().getId());
            final HarvesterBean businessObject = sessionContext.getBusinessObject(HarvesterBean.class);
            int itemsHarvested = businessObject.executeFor(config);
            return new AsyncResult<>(itemsHarvested);
        } finally {
            MDC.remove(HARVESTER_MDC_KEY);
        }
    }

    /**
     * Executes harvest operation
     * @param config harvest configuration
     * @return number of items harvested in batch
     * @throws IllegalStateException on low-level binary file operation failure
     * @throws HarvesterException on failure to complete harvest operation
     */
    @Lock(LockType.READ)
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public int executeFor(RRHarvesterConfig config) throws HarvesterException, SQLException, QueueException, ConfigurationException {
        return harvestOperationFactory.createFor(config).execute();
    }
}
