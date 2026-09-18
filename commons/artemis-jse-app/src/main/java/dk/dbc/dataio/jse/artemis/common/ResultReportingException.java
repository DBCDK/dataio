package dk.dbc.dataio.jse.artemis.common;

/**
 * Thrown when a chunk result could not be handed to job-store, either because the call
 * itself failed or because job-store rejected it.
 * <p>
 * Unchecked, so that it travels out of the framework to {@code MessageConsumer.onMessage},
 * which rolls the JMS session back and leaves the broker to redeliver the message, giving
 * the chunk another attempt at being reported.
 * <p>
 * Concerns whole-chunk reporting only. The per-item protocol reports through
 * {@link ItemDeliveryException} instead.
 */
public class ResultReportingException extends RuntimeException {
    public ResultReportingException(String message, Exception cause) {
        super(message, cause);
    }
}
