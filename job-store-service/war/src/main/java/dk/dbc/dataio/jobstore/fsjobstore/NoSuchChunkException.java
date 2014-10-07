package dk.dbc.dataio.jobstore.fsjobstore;

import dk.dbc.dataio.jobstore.types.JobStoreException;

class NoSuchChunkException extends JobStoreException {

    public NoSuchChunkException(String message) {
        super(message);
    }
    
    public NoSuchChunkException(Exception cause) {
        super(cause.getMessage(), cause);
    }
    
    public NoSuchChunkException(String message, Exception cause) {
        super(message, cause);
    }
}
