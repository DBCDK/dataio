package dk.dbc.dataio.sink.dpf.transform;

public class DpfRecordProcessorException extends Exception {
    public DpfRecordProcessorException(String message) {
        super(message);
    }
    public DpfRecordProcessorException(String message, Exception cause) {
        super(message, cause);
    }
}
