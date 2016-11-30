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

package dk.dbc.dataio.sink.batchexchange;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.EJB;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Startup;

/**
 * This enterprise Java bean represents periodic attempts at completing chunks as a result of finished batches
 * in the batch exchange system.
 */
@Singleton
@Startup
public class ScheduledBatchFinalizerBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScheduledBatchFinalizerBean.class);

    @EJB
    BatchFinalizerBean batchFinalizerBean;

    @Schedule(second = "*/5", minute = "*", hour = "*", persistent = false)
    public void run() {
        try {
            // Keep finalizing until we run out of completed batches.
            int numberOfBatchesCompleted = 0;
            while (batchFinalizerBean.finalizeNextCompletedBatch()) {
                numberOfBatchesCompleted++;
            }
            LOGGER.info("Finalized {} batches", numberOfBatchesCompleted);
        } catch (Exception e) {
            LOGGER.error("Exception caught during scheduled batch finalization", e);
        }
    }
}
