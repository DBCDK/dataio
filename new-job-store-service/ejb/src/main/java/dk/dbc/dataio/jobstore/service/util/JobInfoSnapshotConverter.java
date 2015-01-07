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
        return new JobInfoSnapshot(
                jobEntity.getId(),
                jobEntity.isEoj(),
                jobEntity.getPartNumber(),
                jobEntity.getNumberOfChunks(),
                jobEntity.getNumberOfItems(),
                toDate(jobEntity.getTimeOfCreation()),
                toDate(jobEntity.getTimeOfLastModification()),
                toDate(jobEntity.getTimeOfCompletion()),
                jobEntity.getSpecification(),
                jobEntity.getState(),
                jobEntity.getFlowName(),
                jobEntity.getSinkName());
    }

    /**
     * Converts a java.sql.Timestamp to a java.util.Date
     *
     * @param timestamp to convert
     * @return new Date representation of the timestamp
     */
    private static Date toDate(Timestamp timestamp) {
        return new Date(timestamp.getTime() + timestamp.getNanos() / 1000000);
    }
}
