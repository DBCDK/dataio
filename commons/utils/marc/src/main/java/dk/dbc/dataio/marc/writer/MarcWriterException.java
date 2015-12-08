package dk.dbc.dataio.marc.writer;

public class MarcWriterException extends Exception {
    private static final long serialVersionUID = -2953151817802594255L;

    public MarcWriterException(String message, Exception cause) {
        super(message, cause);
    }
}
