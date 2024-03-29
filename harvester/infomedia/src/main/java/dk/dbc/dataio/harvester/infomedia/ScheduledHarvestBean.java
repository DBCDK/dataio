package dk.dbc.dataio.harvester.infomedia;

import dk.dbc.dataio.harvester.AbstractScheduledHarvestBean;
import dk.dbc.dataio.harvester.types.InfomediaHarvesterConfig;
import dk.dbc.util.RunSchedule;
import jakarta.ejb.EJB;
import jakarta.ejb.ScheduleExpression;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZoneId;
import java.util.Date;

/**
 * This singleton Enterprise Java Bean (EJB) class schedules harvests
 * every thirty seconds
 */
@Singleton
@Startup
public class ScheduledHarvestBean extends AbstractScheduledHarvestBean<HarvesterBean, InfomediaHarvesterConfig, HarvesterConfigurationBean> {
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
        scheduleExpression.second("*/30");
        scheduleExpression.minute("*");
        scheduleExpression.hour("*");
        return scheduleExpression;
    }

    @Override
    public Logger getLogger() {
        return LOGGER;
    }

    @Override
    public boolean canRun(InfomediaHarvesterConfig config) {
        try {
            return new RunSchedule(config.getContent().getSchedule())
                    .withTimezone(ZoneId.of("Europe/Copenhagen"))
                    .isSatisfiedBy(new Date(), config.getContent().getTimeOfLastHarvest());
        } catch (RuntimeException e) {
            LOGGER.error("Unable to check schedule for {}", config.getLogId(), e);
        }
        return false;
    }
}
