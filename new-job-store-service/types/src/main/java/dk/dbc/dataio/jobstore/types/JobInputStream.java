package dk.dbc.dataio.jobstore.types;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;

public class JobInputStream {

    private final JobSpecification jobSpecification;
    private final boolean isEndOfJob;
    private final int partNumber;
    private final long PART_NUMBER_LOWER_BOUND = 0L;

    /**
     * Class constructor
     * @param jobSpecification
     * @param isEndOfJob
     * @param partNumber
     * @throws NullPointerException if given null-valued argument
     * @throws IllegalArgumentException if value of partNumber is <= 0
     */
    @JsonCreator
    public JobInputStream (@JsonProperty ("jobSpecification") JobSpecification jobSpecification,
                           @JsonProperty("isEndOfJob") boolean isEndOfJob,
                           @JsonProperty("partNumber") int partNumber) throws NullPointerException, IllegalArgumentException {

        this.jobSpecification = InvariantUtil.checkNotNullOrThrow(jobSpecification, "jobSpecification");
        this.isEndOfJob = isEndOfJob;
        this.partNumber = Long.valueOf(InvariantUtil.checkLowerBoundOrThrow(partNumber, "partNumber", PART_NUMBER_LOWER_BOUND)).intValue();
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
