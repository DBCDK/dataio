package dk.dbc.dataio.jobstore.types;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;

public class JobInputStream {

    private static JobSpecification jobSpecification;
    private static boolean isEndOfJob;
    private static long partNumber;

    @JsonCreator
    public JobInputStream (@JsonProperty ("jobSpecification") JobSpecification jobSpecification,
                           @JsonProperty("isEndOfJob") boolean isEndOfJob,
                           @JsonProperty("partNumber") long partNumber) throws NullPointerException, IllegalArgumentException {

        this.jobSpecification = InvariantUtil.checkNotNullOrThrow(jobSpecification, "jobSpecification");
        this.isEndOfJob = isEndOfJob;
        this.partNumber = partNumber;
    }
    public JobSpecification getJobSpecification() {
        return jobSpecification;
    }

    public boolean getIsEndOfJob() {
        return isEndOfJob;
    }

    public long getPartNumber() {
        return partNumber;
    }


}
