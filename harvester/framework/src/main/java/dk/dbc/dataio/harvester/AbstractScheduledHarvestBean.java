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
import org.slf4j.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.ScheduleExpression;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Future;

/**
 * Abstract base class for time scheduled harvests
 *
 * @param <T> type parameter for harvester bean
 * @param <U> type parameter for harvester configuration type
 * @param <V> type parameter for harvester configuration bean
 */
public abstract class AbstractScheduledHarvestBean<T extends AbstractHarvesterBean<T, U>, U extends HarvesterConfig<?>, V extends AbstractHarvesterConfigurationBean<U>> {
    private Timer timer = null;

    final protected Map<String, Future<Integer>> runningHarvests = new HashMap<>();

    @Resource
    TimerService timerService;

    protected T harvesterBeanImpl;
    protected V harvesterConfigurationBeanImpl;

    /**
     * Starts default harvest schedule.
     */
    @PostConstruct
    public void bootstrap() {
        harvesterBeanImpl = getHarvesterBeanImpl();
        harvesterConfigurationBeanImpl = getHarvesterConfigurationBeanImpl();
        start(getTimerSchedule());
    }

    /**
     * (Re)starts harvester with given schedule.
     * @param scheduleExpression harvest schedule
     */
    @Lock(LockType.WRITE)
    public void start(ScheduleExpression scheduleExpression) {
        /* stop current timer (if any) and create new timer
           with given schedule */
        stop();
        final TimerConfig timerConfig = new TimerConfig();
        timerConfig.setPersistent(false);
        timer = timerService.createCalendarTimer(scheduleExpression, timerConfig);
    }

    /**
     * Stops harvester
     */
    @Lock(LockType.WRITE)
    public void stop() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

   /**
     * Executes harvest operations not already running on each scheduled point in time
     * @param timer current timer
     */
    @Timeout
    public void scheduleHarvests(Timer timer) {
        try {
            final Iterator<Map.Entry<String, Future<Integer>>> iterator = runningHarvests.entrySet().iterator();
            while (iterator.hasNext()) {
                final Map.Entry<String, Future<Integer>> harvest = iterator.next();
                if (harvest.getValue().isDone()) {
                    iterator.remove();
                    try {
                        final Integer recordsHarvested = harvest.getValue().get();
                        getLogger().info("Scheduled harvest for '{}' harvested {} records",
                                harvest.getKey(), recordsHarvested);
                    } catch (Exception e) {
                        getLogger().warn("Exception caught from scheduled harvest for '{}'", harvest.getKey(), e);
                    }
                }
            }

            harvesterConfigurationBeanImpl.reload();

            for (U config : harvesterConfigurationBeanImpl.getConfigs()) {
                final String harvesterId = config.getLogId();
                if (!runningHarvests.containsKey(harvesterId)) {
                    runningHarvests.put(harvesterId, harvesterBeanImpl.harvest(config));
                    getLogger().debug("Scheduling harvest for '{}'", harvesterId);
                }
            }
        } catch (Exception e) {
            System.out.println(e);
            getLogger().warn("Exception caught while scheduling harvests", e);
        }
    }

    /**
     * @return AbstractHarvesterBean implementation
     */
    public abstract T getHarvesterBeanImpl();

    /**
     * @return AbstractHarvesterConfigurationBean implementation
     */
    public abstract V getHarvesterConfigurationBeanImpl();

    /**
     * @return timer schedule
     */
    public abstract ScheduleExpression getTimerSchedule();

    /**
     * @return Logger
     */
    public abstract Logger getLogger();
}
