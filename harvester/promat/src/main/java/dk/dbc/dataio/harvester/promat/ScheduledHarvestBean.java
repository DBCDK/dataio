/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.dataio.harvester.promat;

import dk.dbc.dataio.harvester.AbstractScheduledHarvestBean;
import dk.dbc.dataio.harvester.types.PeriodicJobsHarvesterConfig;
import dk.dbc.dataio.harvester.types.PromatHarvesterConfig;
import dk.dbc.util.RunSchedule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.EJB;
import javax.ejb.ScheduleExpression;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import java.time.ZoneId;
import java.util.Date;

/**
 * This singleton Enterprise Java Bean (EJB) class schedules harvests every 60 seconds
 */
@Singleton
@Startup
public class ScheduledHarvestBean extends AbstractScheduledHarvestBean<HarvesterBean,
        PromatHarvesterConfig, HarvesterConfigurationBean> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScheduledHarvestBean.class);

    @EJB HarvesterBean harvesterBean;
    @EJB HarvesterConfigurationBean harvesterConfigurationBean;

    @Override
    public HarvesterBean getHarvesterBeanImpl() {
        return harvesterBean;
    }

    @Override
    public HarvesterConfigurationBean getHarvesterConfigurationBeanImpl() {
        return harvesterConfigurationBean;
    }

    @Override
    public ScheduleExpression getTimerSchedule() {
        final ScheduleExpression scheduleExpression = new ScheduleExpression();
        scheduleExpression.second("0");
        scheduleExpression.minute("*");
        scheduleExpression.hour("*");
        return scheduleExpression;
    }

    @Override
    public Logger getLogger() {
        return LOGGER;
    }

    @Override
    public boolean canRun(PromatHarvesterConfig config) {
        try {
            final RunSchedule runSchedule = new RunSchedule(config.getContent().getSchedule())
                    .withTimezone(ZoneId.of(System.getenv("TZ")));
            final Date now = new Date();
            return runSchedule.isSatisfiedBy(now, config.getContent().getTimeOfLastHarvest())
                    || runSchedule.isOverdue(now, config.getContent().getTimeOfLastHarvest());
        } catch (RuntimeException e) {
            LOGGER.error("Unable to check schedule for {}", config.getLogId(), e);
        }
        return false;
    }
}
