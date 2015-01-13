package dk.dbc.dataio.jobstore.types;

public class InvalidInputException extends JobStoreException{

    private JobError jobError;

    public InvalidInputException(String message, JobError jobError) {
        super(message);
        this.jobError = jobError;
    }

    public JobError getJobError() {
        return jobError;
    }
}
