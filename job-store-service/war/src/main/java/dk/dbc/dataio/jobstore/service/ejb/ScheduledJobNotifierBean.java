package dk.dbc.dataio.jobstore.service.ejb;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.ScheduleExpression;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;

/**
 * This singleton Enterprise Java Bean (EJB) class handles scheduled job notifications
 */
@Singleton
@Startup
public class ScheduledJobNotifierBean {
    private Timer timer = null;

    @Resource
    TimerService timerService;

    @EJB
    PgJobNotify jobNotify;

    /**
     * Starts default (every 30s) notifications schedule.
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
     * (Re)starts with given schedule.
     */
    public void start(ScheduleExpression scheduleExpression) {
        /* stop current timer (if any) and create new timer
           with given schedule */
        stop();
        final TimerConfig timerConfig = new TimerConfig();
        timerConfig.setPersistent(false);
        timer = timerService.createCalendarTimer(scheduleExpression, timerConfig);
    }

    /**
     * Stops scheduled notifications
     */
    public void stop() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    @Timeout
    public void scheduleNotifications(Timer timer) {
        jobNotify.flushNotifications();
    }
}
