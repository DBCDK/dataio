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

import dk.dbc.dataio.commons.types.interceptor.Stopwatch;
import dk.dbc.dataio.jobstore.service.entity.JobQueueEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.ejb.DependsOn;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.Startup;

@Singleton
@Startup
@DependsOn("StartupDBMigrator")
public class BootstrapBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(BootstrapBean.class);
    private static final int CACHE_MAX_ENTRIES = 100;

    @EJB
    PgJobStoreRepository jobStoreRepository;

    @EJB
    JobQueueRepository jobQueueRepository;

    @EJB
    JobSchedulerBean jobSchedulerBean;

    @PostConstruct
    @Stopwatch
    public void initialize() {
        jobStoreRepository.purgeReorderedItems();
        resetJobsInterruptedDuringPartitioning();
        jobSchedulerBean.loadSinkStatusOnBootstrap();
    }

    /**
     * Locates and resets any job interrupted in its partitioning phase during a previous shutdown,
     * restoring the corresponding job queue entry to its waiting state
     */
    private void resetJobsInterruptedDuringPartitioning() {
        for (JobQueueEntity inProgress : jobQueueRepository.getInProgress()) {
            jobStoreRepository.resetJob(inProgress.getJob().getId());
            inProgress.setState(JobQueueEntity.State.WAITING);
        }
    }

}
