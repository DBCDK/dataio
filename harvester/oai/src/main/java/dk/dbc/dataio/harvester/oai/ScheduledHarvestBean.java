package dk.dbc.dataio.harvester.oai;

import dk.dbc.dataio.harvester.AbstractScheduledHarvestBean;
import dk.dbc.dataio.harvester.types.OaiHarvesterConfig;
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
 * This singleton Enterprise Java Bean (EJB) class schedules harvests
 * every thirty seconds
 */
@Singleton
@Startup
public class ScheduledHarvestBean extends AbstractScheduledHarvestBean<HarvesterBean, OaiHarvesterConfig, HarvesterConfigurationBean> {
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
    public boolean canRun(OaiHarvesterConfig config) {
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
