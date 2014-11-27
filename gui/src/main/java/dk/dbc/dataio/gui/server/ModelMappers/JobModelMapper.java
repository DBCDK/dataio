package dk.dbc.dataio.gui.server.ModelMappers;

import dk.dbc.dataio.commons.types.JobInfo;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.gui.client.model.JobModel;
import dk.dbc.dataio.gui.client.util.Format;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

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
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(Format.LONG_DATE_TIME_FORMAT);
        return new JobModel(
                simpleDateFormat.format(new Date(jobInfo.getJobCreationTime())),
                String.valueOf(jobInfo.getJobId()),
                jobInfo.getJobSpecification().getDataFile().replaceFirst("^/tmp/", ""),
                Long.toString(jobInfo.getJobSpecification().getSubmitterId()),
                !jobNotDone,
                jobInfo.getJobErrorCode(),
                jobNotDone ? 0 : jobInfo.getChunkifyingChunkCounter().getItemResultCounter().getFailure(),
                jobNotDone ? 0 : jobInfo.getProcessingChunkCounter().getItemResultCounter().getFailure(),
                jobNotDone ? 0 : jobInfo.getDeliveringChunkCounter().getItemResultCounter().getFailure()
        );
    }

    /**
     * Maps a Model to a Job
     *
     * @param model The model as a JobModel class
     * @return The Job as a JobInfo class
     * @throws IllegalArgumentException
     * @throws ParseException
     */
    public static JobInfo toJobInfo(JobModel model) throws ParseException {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(Format.LONG_DATE_TIME_FORMAT);
        return new JobInfo(
                Long.valueOf(model.getJobId()),
                new JobSpecification("", "", "", "", 0L, "", "", "", ""),
                simpleDateFormat.parse(model.getJobCreationTime()).getTime()
        );
    }

}
