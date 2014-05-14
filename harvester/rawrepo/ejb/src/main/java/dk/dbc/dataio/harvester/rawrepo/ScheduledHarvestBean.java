package dk.dbc.dataio.harvester.rawrepo;

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
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;

/**
 * This singleton Enterprise Java Bean (EJB) class executes scheduled harvest
 * operations
 */
@Singleton
@Startup
public class ScheduledHarvestBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScheduledHarvestBean.class);

    private Timer timer = null;

    @Resource
    TimerService timerService;

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
        scheduleExpression.second("*/30");
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
     * Executes harvest operation on each scheduled point in time
     * @param timer current timer
     */
    @Timeout
    public void runScheduledHarvest(Timer timer) {
        try {
            harvester.harvest();
        } catch (Exception e) {
            LOGGER.warn("Exception caught from harvester", e);
        }
    }
}
