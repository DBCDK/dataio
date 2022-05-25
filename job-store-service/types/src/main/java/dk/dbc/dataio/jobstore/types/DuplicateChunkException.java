package dk.dbc.dataio.jobstore.types;

import javax.ejb.ApplicationException;

@ApplicationException(rollback = false)
public class DuplicateChunkException extends JobStoreException {

    private JobError jobError;

    public DuplicateChunkException(String message, JobError jobError) {
        super(message);
        this.jobError = jobError;
    }

    public JobError getJobError() {
        return jobError;
    }
}
