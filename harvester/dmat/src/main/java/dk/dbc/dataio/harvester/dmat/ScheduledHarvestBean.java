package dk.dbc.dataio.harvester.dmat;

import dk.dbc.dataio.commons.types.Constants;
import dk.dbc.dataio.harvester.AbstractScheduledHarvestBean;
import dk.dbc.dataio.harvester.types.DMatHarvesterConfig;
import dk.dbc.util.RunSchedule;
import jakarta.ejb.EJB;
import jakarta.ejb.ScheduleExpression;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

/**
 * This singleton Enterprise Java Bean (EJB) class schedules harvests every 60 seconds
 */
@Singleton
@Startup
public class ScheduledHarvestBean extends AbstractScheduledHarvestBean<HarvesterBean, DMatHarvesterConfig, HarvesterConfigurationBean> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScheduledHarvestBean.class);

    @EJB
    HarvesterBean harvesterBean;
    @EJB
    HarvesterConfigurationBean harvesterConfigurationBean;

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
    public boolean canRun(DMatHarvesterConfig config) {
        try {
            final RunSchedule runSchedule = new RunSchedule(config.getContent().getSchedule()).withTimezone(Constants.ZONE_CPH);
            final Date now = new Date();
            return runSchedule.isSatisfiedBy(now, config.getContent().getTimeOfLastHarvest())
                    || runSchedule.isOverdue(now, config.getContent().getTimeOfLastHarvest());
        } catch (RuntimeException e) {
            LOGGER.error("Unable to check schedule for {}", config.getLogId(), e);
        }
        return false;
    }
}
