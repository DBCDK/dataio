package dk.dbc.dataio.gui.server.ModelMappers;

import dk.dbc.dataio.commons.types.JobInfo;
import dk.dbc.dataio.gui.client.model.JobModelOld;
import dk.dbc.dataio.gui.client.util.Format;

import java.text.SimpleDateFormat;
import java.util.Date;

public final class JobModelMapperOld {

    /**
     * Private Constructor prevents instantiation of this static class
     */
    private JobModelMapperOld() {
    }

    /**
     * Maps a Job to a Model
     *
     * @param jobInfo The Job as a JobInfo class
     * @return model The Job as a JobModel class
     */
    public static JobModelOld toModel(JobInfo jobInfo) {
        boolean jobNotDone = jobInfo.getChunkifyingChunkCounter() == null ||
                jobInfo.getProcessingChunkCounter() == null ||
                jobInfo.getDeliveringChunkCounter() == null;
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(Format.LONG_DATE_TIME_FORMAT);
        return new JobModelOld(
                simpleDateFormat.format(new Date(jobInfo.getJobCreationTime())),
                String.valueOf(jobInfo.getJobId()),
                jobInfo.getJobSpecification().getDataFile().replaceFirst("^/tmp/", ""),
                Long.toString(jobInfo.getJobSpecification().getSubmitterNumber()),
                !jobNotDone,
                jobInfo.getJobErrorCode(),
                jobNotDone ? 0 : jobInfo.getChunkifyingChunkCounter().getItemResultCounter().getTotal(),
                jobNotDone ? 0 : jobInfo.getChunkifyingChunkCounter().getItemResultCounter().getSuccess(),
                jobNotDone ? 0 : jobInfo.getChunkifyingChunkCounter().getItemResultCounter().getFailure(),
                jobNotDone ? 0 : jobInfo.getChunkifyingChunkCounter().getItemResultCounter().getIgnore(),
                jobNotDone ? 0 : jobInfo.getProcessingChunkCounter().getItemResultCounter().getTotal(),
                jobNotDone ? 0 : jobInfo.getProcessingChunkCounter().getItemResultCounter().getSuccess(),
                jobNotDone ? 0 : jobInfo.getProcessingChunkCounter().getItemResultCounter().getFailure(),
                jobNotDone ? 0 : jobInfo.getProcessingChunkCounter().getItemResultCounter().getIgnore(),
                jobNotDone ? 0 : jobInfo.getDeliveringChunkCounter().getItemResultCounter().getTotal(),
                jobNotDone ? 0 : jobInfo.getDeliveringChunkCounter().getItemResultCounter().getSuccess(),
                jobNotDone ? 0 : jobInfo.getDeliveringChunkCounter().getItemResultCounter().getFailure(),
                jobNotDone ? 0 : jobInfo.getDeliveringChunkCounter().getItemResultCounter().getIgnore()
        );
    }

}
