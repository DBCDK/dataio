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
     * @param scheduleExpression schedule expression
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
