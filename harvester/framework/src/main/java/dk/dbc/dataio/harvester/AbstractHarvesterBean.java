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

package dk.dbc.dataio.harvester;

import dk.dbc.dataio.harvester.types.HarvesterConfig;
import dk.dbc.dataio.harvester.types.HarvesterException;
import org.slf4j.Logger;
import org.slf4j.MDC;

import javax.annotation.Resource;
import javax.ejb.AsyncResult;
import javax.ejb.Asynchronous;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.SessionContext;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.concurrent.Future;

/**
 * Abstract base class for harvesters
 *
 * @param <T> type parameter (recursive) for AbstractHarvesterBean implementation
 * @param <U> type parameter for harvester configuration type
 */
public abstract class AbstractHarvesterBean<T extends AbstractHarvesterBean<T, U>, U extends HarvesterConfig<?>> {
    private static final String HARVESTER_MDC_KEY = "HARVESTER_ID";

    @Resource
    public SessionContext sessionContext;

    /**
     * Executes harvest operation in batches (each batch in its own transactional
     * scope to avoid tearing down any controlling timers in case of an exception)
     * @param config harvest configuration
     * @return number of items harvested
     * @throws HarvesterException on failure to complete harvest operation
     */
    @Asynchronous
    @Lock(LockType.READ)
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public Future<Integer> harvest(U config) throws HarvesterException {
        getLogger().debug("Called with config {}", config);
        try {
            MDC.put(HARVESTER_MDC_KEY, config.getLogId());
            int itemsHarvested = self().executeFor(config);
            return new AsyncResult<>(itemsHarvested);
        } finally {
            MDC.remove(HARVESTER_MDC_KEY);
        }
    }

    /**
     * Executes harvest operation
     * @param config harvest configuration
     * @return number of items harvested in batch
     * @throws HarvesterException on failure to complete harvest operation
     */
    @Lock(LockType.READ)
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public abstract int executeFor(U config) throws HarvesterException;

    /**
     * @return reference to business interface of this AbstractHarvesterBean implementation
     */
    public abstract T self();

    /**
     * @return Logger
     */
    public abstract Logger getLogger();
}
