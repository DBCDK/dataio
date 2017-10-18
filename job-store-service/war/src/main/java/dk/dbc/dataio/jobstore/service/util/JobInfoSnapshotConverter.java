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

package dk.dbc.dataio.jobstore.service.util;

import dk.dbc.dataio.jobstore.service.entity.JobEntity;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;

import java.sql.Timestamp;
import java.util.Date;

public final class JobInfoSnapshotConverter {

    /**
     * Private Constructor prevents instantiation of this static class
     */
    private JobInfoSnapshotConverter() {}

    /**
     *
     * Maps information from a job entity to a JobInfoSnapshot
     *
     * @param jobEntity the job entity
     * @return jobInfoSnapshot containing information about the job from one exact moment in time (now)
     */
    public static JobInfoSnapshot toJobInfoSnapshot(JobEntity jobEntity) {
        return new JobInfoSnapshot()
                .withJobId(jobEntity.getId())
                .withFatalError(jobEntity.hasFatalError())
                .withPartNumber(jobEntity.getPartNumber())
                .withNumberOfChunks(jobEntity.getNumberOfChunks())
                .withNumberOfItems(jobEntity.getNumberOfItems())
                .withTimeOfCreation(toDate(jobEntity.getTimeOfCreation()))
                .withTimeOfLastModification(toDate(jobEntity.getTimeOfLastModification()))
                .withTimeOfCompletion(toDate(jobEntity.getTimeOfCompletion()))
                .withSpecification(jobEntity.getSpecification())
                .withState(jobEntity.getState())
                .withFlowStoreReferences(jobEntity.getFlowStoreReferences())
                .withWorkflowNote(jobEntity.getWorkflowNote());
    }

    /**
     * Converts a java.sql.Timestamp to a java.util.Date
     *
     * @param timestamp to convert
     * @return new Date representation of the timestamp, null if the timestamp is null
     */
    private static Date toDate(Timestamp timestamp) {
        if (timestamp != null) {
            return new Date(timestamp.getTime());

        } else {
            return null;
        }
    }
}
