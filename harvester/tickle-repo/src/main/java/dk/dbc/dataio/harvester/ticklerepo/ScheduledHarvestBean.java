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

package dk.dbc.dataio.harvester.ticklerepo;

import dk.dbc.dataio.harvester.types.TickleRepoHarvesterConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.ScheduleExpression;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.Timeout;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Future;

/**
 * This singleton Enterprise Java Bean (EJB) class executes scheduled harvest
 * operations
 */
@Singleton
@Startup
public class ScheduledHarvestBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScheduledHarvestBean.class);

    final Map<String, Future<Integer>> runningHarvests = new HashMap<>();

    @Resource
    TimerService timerService;

    @EJB
    HarvesterConfigurationBean config;

    @EJB
    HarvesterBean harvester;

    /**
     * Starts default (every 30s) harvest schedule.
     * Note: in the near future this method will probably be removed and
     * scheduled harvesting will not be started by default.
     */
    @PostConstruct
    public void bootstrap() {
        final ScheduleExpression scheduleExpression = new ScheduleExpression();
        scheduleExpression.second("*/15");
        scheduleExpression.minute("*");
        scheduleExpression.hour("*");
        start(scheduleExpression);
    }

    /**
     * (Re)starts harvester with given schedule.
     * @param scheduleExpression harvest schedule
     */
    @Lock(LockType.WRITE)
    public void start(ScheduleExpression scheduleExpression) {
        /* stop current timer (if any) and create new timer
           with given schedule */
        final TimerConfig timerConfig = new TimerConfig();
        timerConfig.setPersistent(false);
        timerService.createCalendarTimer(scheduleExpression, timerConfig);
    }

    /**
     * Executes harvest operations not already running on each scheduled point in time
     */
    @Timeout
    public void scheduleHarvests() {
        try {
            final Iterator<Map.Entry<String, Future<Integer>>> iterator = runningHarvests.entrySet().iterator();
            while (iterator.hasNext()) {
                final Map.Entry<String, Future<Integer>> harvest = iterator.next();
                if (harvest.getValue().isDone()) {
                    iterator.remove();
                    try {
                        final Integer recordsHarvested = harvest.getValue().get();
                        LOGGER.info("Scheduled harvest for '{}' harvested {} records",
                                harvest.getKey(), recordsHarvested);
                    } catch (Exception e) {
                        LOGGER.error("Exception caught from scheduled harvest for '{}'", harvest.getKey(), e);
                    }
                }
            }

            config.reload();
            for (TickleRepoHarvesterConfig config : config.get()) {
                final String harvesterId = config.getContent().getId();
                if (!runningHarvests.containsKey(harvesterId)) {
                    runningHarvests.put(harvesterId, harvester.harvest(config));
                    LOGGER.debug("Scheduling harvest for '{}'", harvesterId);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Exception caught while scheduling harvests", e);
        }
    }
}
