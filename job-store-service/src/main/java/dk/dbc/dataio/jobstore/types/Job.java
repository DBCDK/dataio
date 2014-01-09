package dk.dbc.dataio.jobstore.types;

import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.JobInfo;
import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;

import java.io.Serializable;

/**
 * Job DTO class.
 */
public class Job implements Serializable {
    private static final long serialVersionUID = 592111006810833332L;

    private final JobInfo jobInfo;
    private final JobState jobState;
    private final Flow flow;

    public Job(JobInfo jobInfo, JobState jobState, Flow flow) {
        this.jobInfo = InvariantUtil.checkNotNullOrThrow(jobInfo, "jobInfo");
        this.jobState = InvariantUtil.checkNotNullOrThrow(jobState, "jobState");
        this.flow = InvariantUtil.checkNotNullOrThrow(flow, "flow");
    }

    public long getId() {
        return jobInfo.getJobId();
    }

    public JobInfo getJobInfo() {
        return jobInfo;
    }

    public JobState getJobState() {
        return jobState;
    }

    public Flow getFlow() {
        return flow;
    }

}