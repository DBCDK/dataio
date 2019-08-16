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

import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.jobstore.service.entity.JobQueueEntity;
import dk.dbc.dataio.jobstore.service.entity.RerunEntity;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.DependsOn;
import javax.ejb.EJB;
import javax.ejb.ScheduleExpression;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import java.util.HashMap;
import java.util.Map;

@Singleton
@Startup
@DependsOn("DatabaseMigrator")
public class BootstrapBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(BootstrapBean.class);

    @EJB JobQueueRepository jobQueueRepository;
    @EJB JobSchedulerBean jobSchedulerBean;
    @EJB PgJobStore jobStore;
    @EJB RerunsRepository rerunsRepository;
    @EJB JobRerunnerBean jobRerunnerBean;
    @Resource TimerService timerService;

    private Timer jumpStartTimer;

    @PostConstruct
    public void initialize() {
        resetJobsInterruptedDuringPartitioning();
        resetInterruptedRerunTasks();
        jobSchedulerBean.loadSinkStatusOnBootstrap();
        jumpStartTimer = createJumpStartTimer();
    }

    /*
     * Locates and resets any job interrupted in its partitioning phase during a previous shutdown,
     * restoring the corresponding job queue entry to its waiting state
     */
    private void resetJobsInterruptedDuringPartitioning() {
        for (JobQueueEntity inProgress : jobQueueRepository.getInProgress()) {
            jobSchedulerBean.ensureLastChunkIsScheduled(inProgress.getJob().getId());
            inProgress.withState(JobQueueEntity.State.WAITING);
        }
    }

    private void resetInterruptedRerunTasks() {
        for (RerunEntity interrupted : rerunsRepository.getInProgress()) {
            interrupted.withState(RerunEntity.State.WAITING);
        }
    }

    /**
     * Jump-starts partitioning for each sink found in the job queue
     * @param timer timer
     */
    @Timeout
    public void jumpStart(Timer timer) {
        final Map<Long, Sink> sinks = new HashMap<>();
        for (JobQueueEntity jobQueueEntity : jobQueueRepository.getWaiting()) {
            final Sink sink = jobQueueEntity.getJob().getCachedSink().getSink();
            if (!sinks.containsKey(sink.getId()) || sinks.get(sink.getId()).getVersion() < sink.getVersion()) {
                sinks.put(sink.getId(), sink);
            }
        }
        LOGGER.info("jumpStart(): found {} sinks to jump-start", sinks.size());
        sinks.forEach((id, sink) -> {
            LOGGER.info("jumpStart(): jump-starting partitioning for sink {}({})",
                    id, sink.getContent().getName());
            jobStore.partitionNextJobForSinkIfAvailable(sink);
        });

        try {
            jobRerunnerBean.rerunNextIfAvailable();
        } catch (JobStoreException e) {
            LOGGER.error("Error jump-starting rerun tasks handling", e);
        }

        timer.cancel();  // single event timer
    }

    private Timer createJumpStartTimer() {
        final ScheduleExpression scheduleExpression = new ScheduleExpression();
        scheduleExpression.second("*/1");
        scheduleExpression.minute("*");
        scheduleExpression.hour("*");
        final TimerConfig timerConfig = new TimerConfig();
        timerConfig.setPersistent(false);
        return timerService.createCalendarTimer(scheduleExpression, timerConfig);
    }
}
