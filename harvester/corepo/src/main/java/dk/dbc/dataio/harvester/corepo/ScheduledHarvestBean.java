package dk.dbc.dataio.harvester.corepo;

import dk.dbc.dataio.harvester.AbstractScheduledHarvestBean;
import dk.dbc.dataio.harvester.types.CoRepoHarvesterConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.EJB;
import javax.ejb.ScheduleExpression;
import javax.ejb.Singleton;
import javax.ejb.Startup;

/**
 * This singleton Enterprise Java Bean (EJB) class executes scheduled harvest
 * operations
 */
@Singleton
@Startup
public class ScheduledHarvestBean extends AbstractScheduledHarvestBean<HarvesterBean, CoRepoHarvesterConfig, HarvesterConfigurationBean> {
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
}
