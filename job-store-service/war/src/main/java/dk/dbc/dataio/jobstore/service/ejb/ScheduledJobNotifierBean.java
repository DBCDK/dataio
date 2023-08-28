package dk.dbc.dataio.jobstore.service.ejb;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.ejb.EJB;
import jakarta.ejb.ScheduleExpression;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.ejb.Timeout;
import jakarta.ejb.TimerConfig;
import jakarta.ejb.TimerService;

/**
 * This singleton Enterprise Java Bean (EJB) class handles scheduled job notifications
 */
@Singleton
@Startup
public class ScheduledJobNotifierBean {
    @Resource
    TimerService timerService;

    @EJB
    JobNotificationRepository jobNotificationRepository;

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
     *
     * @param scheduleExpression schedule expression
     */
    public void start(ScheduleExpression scheduleExpression) {
        /* stop current timer (if any) and create new timer
           with given schedule */
        final TimerConfig timerConfig = new TimerConfig();
        timerConfig.setPersistent(false);
        timerService.createCalendarTimer(scheduleExpression, timerConfig);
    }

    @Timeout
    public void scheduleNotifications() {
        jobNotificationRepository.flushNotifications();
    }
}
