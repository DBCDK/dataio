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
import dk.dbc.dataio.filestore.service.connector.ejb.FileStoreServiceConnectorBean;
import dk.dbc.dataio.jobstore.service.entity.JobEntity;
import dk.dbc.dataio.jobstore.service.entity.JobQueueEntity;
import dk.dbc.dataio.jobstore.service.param.PartitioningParam;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.DependsOn;
import javax.ejb.EJB;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import java.util.List;

@Singleton
@Startup
@DependsOn("BootstrapBean")
public class JobQueueWatcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobQueueWatcher.class);

    @EJB
    FileStoreServiceConnectorBean fileStoreServiceConnectorBean;

    @EJB
    JobQueueRepository jobQueueRepository;

    @EJB
    PgJobStoreRepository jobStoreRepository;

    @EJB
    PgJobStore jobStore;

    @Stopwatch
    @Schedule(second = "*/5", minute = "*", hour = "*", persistent = false)
    public void doWatch() {
        final List<Long> uniqueSinkIds = jobQueueRepository.getUniqueSinkIds();

        if(uniqueSinkIds != null && !uniqueSinkIds.isEmpty()) {
            for (Long uniqueSinkId : uniqueSinkIds) {

                final JobQueueEntity firstWaitingJobToStart = jobQueueRepository.getFirstWaitingJobQueueEntityBySink(uniqueSinkId);
                if (!jobQueueRepository.isSinkOccupied(uniqueSinkId) && firstWaitingJobToStart != null) {

                    this.startJob(firstWaitingJobToStart.getJob());
                }
            }
        }
    }


    private void startJob(JobEntity jobToStart) {
        final JobQueueEntity jobQueueEntity = this.jobQueueRepository.getJobQueueEntityByJob(jobToStart);
        try{
            jobStore.handlePartitioningAsynchronously(
                    new PartitioningParam(
                            jobToStart,
                            fileStoreServiceConnectorBean.getConnector(),
                            jobQueueEntity.isSequenceAnalysis(),
                            jobQueueEntity.getRecordSplitterType()));
        } catch (JobStoreException jse) {
            LOGGER.info(
                    "this job received an error and is rolled back hence stays in the queue!. Queue ID: {}, Job ID: {}, Sink ID: {}",
                    jobQueueEntity.getId(),
                    jobQueueEntity.getJob().getId(),
                    jobQueueEntity.getSinkId());
        }
    }
}