package dk.dbc.dataio.commons.utils.test.model;

import dk.dbc.dataio.commons.types.JobErrorCode;
import dk.dbc.dataio.commons.types.JobInfo;
import dk.dbc.dataio.commons.types.JobSpecification;
import java.util.Calendar;
import java.util.Date;

public class JobInfoBuilder {
    private long jobId = 1;
    private JobSpecification jobSpecification = new JobSpecificationBuilder().build();
    private Date jobCreationTime;
    private JobErrorCode jobErrorCode = JobErrorCode.NO_ERROR;
    private long jobRecordCount = 1;
    private String jobResultDataFile = "-jobResultDataFile-";

    public JobInfoBuilder() {
        Calendar cal = Calendar.getInstance();
        cal.set(2014, Calendar.JANUARY, 27);
        this.jobCreationTime = cal.getTime();
    }

    public JobInfoBuilder setJobId(long jobId) {
        this.jobId = jobId;
        return this;
    }

    public JobInfoBuilder setJobSpecification(JobSpecification jobSpecification) {
        this.jobSpecification = jobSpecification;
        return this;
    }

    public JobInfoBuilder setJobCreationTime(Date jobCreationTime) {
        this.jobCreationTime = jobCreationTime;
        return this;
    }

    public JobInfoBuilder setJobErrorCode(JobErrorCode jobErrorCode) {
        this.jobErrorCode = jobErrorCode;
        return this;
    }

    public JobInfoBuilder setJobRecordCount(long jobRecordCount) {
        this.jobRecordCount = jobRecordCount;
        return this;
    }

    public JobInfoBuilder setJobResultDataFile(String jobResultDataFile) {
        this.jobResultDataFile = jobResultDataFile;
        return this;
    }

    public JobInfo build() {
        return new JobInfo(jobId, jobSpecification, jobCreationTime, jobErrorCode, jobRecordCount, jobResultDataFile);

    }
}
