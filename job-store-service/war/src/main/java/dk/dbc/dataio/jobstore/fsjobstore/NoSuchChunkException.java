package dk.dbc.dataio.jobstore.fsjobstore;

class NoSuchChunkException extends Exception {

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
