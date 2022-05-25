package dk.dbc.dataio.commons.types.exceptions;

public class DataIoException extends Exception {
    private static final long serialVersionUID = 6206662806478137024L;

    private final String type;

    /* Below fields are hacks since jackson serialization/deserialization of Throwable
       does not retain type information. Perhaps this can be remedied by configuring
       the ObjectMapper as shown, but this will have profound consequences on the rest
       of the project.

       objectMapper.configure( DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
       objectMapper.setVisibility( PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY );
       objectMapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);
     */
    private String causedBy;
    private String causedByDetail;

    public DataIoException() {
        super();
        type = this.getClass().getName();
    }

    /**
     * Constructs a new exception with the specified detail message
     * <p>
     * The cause is not initialized, and may subsequently be initialized by
     * a call to {@link #initCause}.
     *
     * @param message detail message saved for later retrieval by the
     *                {@link #getMessage()} method. May be null.
     */
    public DataIoException(String message) {
        super(message);
        type = this.getClass().getName();
    }

    /**
     * Constructs a new exception with the specified detail message and
     * cause
     * <p>
     * Note that the detail message associated with cause is not
     * automatically incorporated in this exception's detail message.
     *
     * @param message detail message saved for later retrieval
     *                by the {@link #getMessage()} method). May be null.
     * @param cause   cause saved for later retrieval by the
     *                {@link #getCause()} method). (A null value is
     *                permitted, and indicates that the cause is nonexistent or
     *                unknown).
     */
    public DataIoException(String message, Exception cause) {
        super(message, cause);
        type = this.getClass().getName();
        causedBy = cause.getClass().getName();
        causedByDetail = cause.getMessage();
    }

    public String getType() {
        return type;
    }

    public String getCausedBy() {
        return causedBy;
    }

    public String getCausedByDetail() {
        return causedByDetail;
    }
}
