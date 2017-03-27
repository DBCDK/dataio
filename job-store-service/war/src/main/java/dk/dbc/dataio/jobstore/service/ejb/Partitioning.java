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

import dk.dbc.dataio.jobstore.service.entity.JobEntity;
import dk.dbc.dataio.jobstore.service.util.JobInfoSnapshotConverter;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.jobstore.types.PrematureEndOfDataException;

/**
 * This class represents the result of a job partitioning
 */
public class Partitioning {
    private JobEntity jobEntity;
    private Throwable failure;
    private boolean failureIsCauseOfPrematureEndOfDataException;

    public Throwable getFailure() {
        return failure;
    }

    public Partitioning withFailure(Exception failure) {
        this.failure = walkStackTrace(failure);
        return this;
    }

    public JobEntity getJobEntity() {
        return jobEntity;
    }

    public JobInfoSnapshot getJobInfoSnapshot() {
        if (jobEntity != null) {
            return JobInfoSnapshotConverter.toJobInfoSnapshot(jobEntity);
        }
        return null;
    }

    public Partitioning withJobEntity(JobEntity jobEntity) {
        this.jobEntity = jobEntity;
        return this;
    }

    public boolean hasFailedUnexpectedly() {
        return failure != null;
    }

    public boolean hasFailedPossiblyDueToLostFileStoreConnection() {
        return hasFailedUnexpectedly() && failureIsCauseOfPrematureEndOfDataException;
    }

    private Throwable walkStackTrace(Exception e) {
        failureIsCauseOfPrematureEndOfDataException = false;
        Throwable result = e;
        if (result != null) {
            Throwable cause;
            while (null != (cause = result.getCause()) && result != cause) {
                if (cause instanceof PrematureEndOfDataException) {
                    result = cause.getCause();
                    if (result != null) {
                        failureIsCauseOfPrematureEndOfDataException = true;
                        break;
                    }
                }
                result = cause;
            }
        }
        return result;
    }
}
