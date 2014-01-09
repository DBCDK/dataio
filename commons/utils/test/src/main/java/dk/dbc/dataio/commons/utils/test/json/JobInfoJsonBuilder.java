package dk.dbc.dataio.commons.utils.test.json;

import dk.dbc.dataio.commons.types.JobErrorCode;

import java.util.Date;

public class JobInfoJsonBuilder extends JsonBuilder {
    private long jobId = 42L;
    private long jobCreationTime = new Date().getTime();
    private long jobRecordCount = 0;
    private String jobSpecification = new JobSpecificationJsonBuilder().build();
    private JobErrorCode jobErrorCode = JobErrorCode.NO_ERROR;
    private String jobResultDataFile = "file";

    public JobInfoJsonBuilder setJobId(long jobId) {
        this.jobId = jobId;
        return this;
    }

    public JobInfoJsonBuilder setJobCreationTime(long jobCreationTime) {
        this.jobCreationTime = jobCreationTime;
        return this;
    }

    public JobInfoJsonBuilder setJobRecordCount(long jobRecordCount) {
        this.jobRecordCount = jobRecordCount;
        return this;
    }

    public JobInfoJsonBuilder setJobSpecification(String jobSpecification) {
        this.jobSpecification = jobSpecification;
        return this;
    }

    public JobInfoJsonBuilder setJobResultDataFile(String jobResultDataFile) {
        this.jobResultDataFile = jobResultDataFile;
        return this;
    }

    public JobInfoJsonBuilder setJobErrorCode(JobErrorCode jobErrorCode) {
        this.jobErrorCode = jobErrorCode;
        return this;
    }

    public String build() {
        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(START_OBJECT);
        stringBuilder.append(asLongMember("jobId", jobId)); stringBuilder.append(MEMBER_DELIMITER);
        stringBuilder.append(asLongMember("jobCreationTime", jobCreationTime)); stringBuilder.append(MEMBER_DELIMITER);
        stringBuilder.append(asLongMember("jobRecordCount", jobRecordCount)); stringBuilder.append(MEMBER_DELIMITER);
        stringBuilder.append(asObjectMember("jobSpecification", jobSpecification)); stringBuilder.append(MEMBER_DELIMITER);
        stringBuilder.append(asTextMember("jobErrorCode", jobErrorCode.name())); stringBuilder.append(MEMBER_DELIMITER);
        stringBuilder.append(asTextMember("jobResultDataFile", jobResultDataFile));
        stringBuilder.append(END_OBJECT);
        return stringBuilder.toString();
    }
}
