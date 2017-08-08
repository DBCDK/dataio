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

package dk.dbc.dataio.harvester.ush.solr;

import dk.dbc.dataio.harvester.types.UshHarvesterProperties;
import dk.dbc.dataio.harvester.types.UshSolrHarvesterConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.DependsOn;
import javax.ejb.EJB;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.ScheduleExpression;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Future;

/**
 * This singleton Enterprise Java Bean (EJB) class executes scheduled harvest operations
 */
@Singleton
@Startup
@DependsOn("BootstrapBean")
public class ScheduledHarvestBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScheduledHarvestBean.class);
    private static final long TWENTY_FOUR_HOURS_IN_MS = 24 * 60 * 60 * 1000;

    final Map<Long, Future<Integer>> runningHarvests = new HashMap<>();

    @Resource
    TimerService timerService;

    @EJB
    HarvesterConfigurationBean configs;

    @EJB
    HarvesterBean harvester;

    /**
     * Starts hourly harvest.
     */
    @PostConstruct
    public void bootstrap() {
        final ScheduleExpression scheduleExpression = new ScheduleExpression();
        scheduleExpression.second(0);
        scheduleExpression.minute(0);
        scheduleExpression.hour("*");
        start(scheduleExpression);
    }

    /**
     * Starts harvesting schedule.
     * @param scheduleExpression harvest schedule
     */
    @Lock(LockType.WRITE)
    public void start(ScheduleExpression scheduleExpression) {
        final TimerConfig timerConfig = new TimerConfig();
        timerConfig.setPersistent(false);
        timerService.createCalendarTimer(scheduleExpression, timerConfig);
    }

    /**
     * Executes harvest operations not already running on each scheduled point in time
     * @param timer current timer
     */
    @Timeout
    public void scheduleHarvests() {
        try {
            configs.reload();
            final Iterator<Map.Entry<Long, Future<Integer>>> iterator = runningHarvests.entrySet().iterator();
            while (iterator.hasNext()) {
                final Map.Entry<Long, Future<Integer>> harvest = iterator.next();
                if (harvest.getValue().isDone()) {
                    iterator.remove();
                    try {
                        final Integer recordsHarvested = harvest.getValue().get();
                        LOGGER.info("Scheduled harvest for '{}' harvested {} records", harvest.getKey(), recordsHarvested);
                    } catch (Exception e) {
                        LOGGER.warn("Exception caught from scheduled harvest for '{}'", harvest.getKey(), e);
                    }
                }
            }

            for (UshSolrHarvesterConfig config : configs.get()) {
                try {
                    if (!runningHarvests.containsKey(config.getId()) && isEligibleForExecution(config)) {
                        runningHarvests.put(config.getId(), harvester.harvest(config));
                        LOGGER.debug("Scheduling harvest for '{}'", config.getId());
                    }
                } catch (IllegalStateException e) {
                    LOGGER.warn("Exception caught while scheduling harvest", e);
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Exception caught while scheduling harvests", e);
        }
    }

    private boolean isEligibleForExecution(UshSolrHarvesterConfig config) throws IllegalStateException {
        final UshHarvesterProperties ushHarvesterProperties = config.getContent().getUshHarvesterProperties();
        if (ushHarvesterProperties == null) {
            throw new IllegalStateException(String.format("Config with ID %d for USH harvester '%d' contained no properties",
                    config.getId(), config.getContent().getUshHarvesterJobId()));
        }
        if (!"OK".equals(ushHarvesterProperties.getCurrentStatus())) {
            LOGGER.warn("Current status of '{}' USH harvest is {}",
                    ushHarvesterProperties.getId(), ushHarvesterProperties.getCurrentStatus());
            return false;
        }
        final Date now = new Date();
        final Date epoch = new Date(0);
        // Currently we only wish to execute a harvest operation once in a 24 hours period,
        // and only if the USH has been updated since last operation.
        // (.orElse() parts are set so as to ensure a false return value from this method when not present)
        final Date ushSolrTimeOfLastHarvest = getUshSolrTimeOfLastHarvest(config).orElse(epoch);
        final Date ushTimeOfLastHarvest = getUshTimeOfLastHarvest(ushHarvesterProperties).orElse(ushSolrTimeOfLastHarvest);
        return now.getTime() - ushSolrTimeOfLastHarvest.getTime() > TWENTY_FOUR_HOURS_IN_MS
                && ushTimeOfLastHarvest.getTime() > ushSolrTimeOfLastHarvest.getTime();
    }

    private Optional<Date> getUshTimeOfLastHarvest(UshHarvesterProperties ushHarvesterProperties) {
        return Optional.ofNullable(ushHarvesterProperties.getLastHarvestFinished());
    }

    private Optional<Date> getUshSolrTimeOfLastHarvest(UshSolrHarvesterConfig config) {
        return Optional.ofNullable(config.getContent().getTimeOfLastHarvest());
    }
}
