package dk.dbc.dataio.jobstore.types;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.invariant.InvariantUtil;

public class JobInputStream {

    private final JobSpecification jobSpecification;
    private final boolean isEndOfJob;
    private final int partNumber;

    /**
     * Class constructor
     *
     * @param jobSpecification the jobSpecification
     * @param isEndOfJob       boolean indicating if this is last input resulting in end of job
     * @param partNumber       the partNumber
     * @throws NullPointerException     if given null-valued argument
     * @throws IllegalArgumentException if value of partNumber is less than 0
     */
    @JsonCreator
    public JobInputStream(@JsonProperty("jobSpecification") JobSpecification jobSpecification,
                          @JsonProperty("isEndOfJob") boolean isEndOfJob,
                          @JsonProperty("partNumber") int partNumber) throws NullPointerException, IllegalArgumentException {

        this.jobSpecification = InvariantUtil.checkNotNullOrThrow(jobSpecification, "jobSpecification");
        this.isEndOfJob = isEndOfJob;
        if (partNumber < 0) {
            throw new IllegalArgumentException("partNumber must be greater than 0");
        }
        this.partNumber = partNumber;
    }

    public JobInputStream(JobSpecification jobSpecification) {
        this(jobSpecification, false, 0);
    }

    public JobSpecification getJobSpecification() {
        return jobSpecification;
    }

    public boolean getIsEndOfJob() {
        return isEndOfJob;
    }

    public int getPartNumber() {
        return partNumber;
    }
}
