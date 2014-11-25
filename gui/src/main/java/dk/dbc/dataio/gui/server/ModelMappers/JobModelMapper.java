package dk.dbc.dataio.gui.server.ModelMappers;

import dk.dbc.dataio.commons.types.JobInfo;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.gui.client.model.JobModel;
import dk.dbc.dataio.gui.client.util.Format;

public final class JobModelMapper {

    /**
     * Private Constructor prevents instantiation of this static class
     */
    private JobModelMapper() {
    }

    /**
     * Maps a Job to a Model
     *
     * @param jobInfo The Job as a JobInfo class
     * @return model The Job as a JobModel class
     */
    public static JobModel toModel(JobInfo jobInfo) {
        boolean jobNotDone = jobInfo.getChunkifyingChunkCounter() == null ||
                jobInfo.getProcessingChunkCounter() == null ||
                jobInfo.getDeliveringChunkCounter() == null;
        return new JobModel(
                Format.getLongDateTimeFormat(jobInfo.getJobCreationTime()),
                String.valueOf(jobInfo.getJobId()),
                jobInfo.getJobSpecification().getDataFile().replaceFirst("^/tmp/", ""),
                Long.toString(jobInfo.getJobSpecification().getSubmitterId()),
                !jobNotDone,
                jobInfo.getJobErrorCode(),
                jobInfo.getChunkifyingChunkCounter().getItemResultCounter().getFailure(),
                jobInfo.getProcessingChunkCounter().getItemResultCounter().getFailure(),
                jobInfo.getDeliveringChunkCounter().getItemResultCounter().getFailure()
        );
    }

    /**
     * Maps a Model to a Job
     *
     * @param model The model as a JobModel class
     * @return The Job as a JobInfo class
     * @throws IllegalArgumentException
     */
    public static JobInfo toJobInfo(JobModel model) {
        return new JobInfo(
                Long.valueOf(model.getJobId()),
                new JobSpecification("", "", "", "", 0L, "", "", "", ""),
                Format.parseLongDate("model.getJobCreationTime()")
        );
    }

}
