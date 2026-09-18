package dk.dbc.dataio.jse.artemis.common;

/**
 * Thrown when a step of the per-item delivery protocol could not be carried out and the
 * item must be tried again: the watermark could not be read, the delivery itself threw,
 * or the result could not be reported (see docs/chunk-scheduling-redesign.md).
 * <p>
 * Unchecked, so that it travels out of the sink framework to
 * {@code MessageConsumer.onMessage}, which rolls the JMS session back and leaves the
 * broker to redeliver the message.
 * <p>
 * This is deliberately not the outcome of a delivery a target system rejected. A rejection
 * the target will give again on the next attempt is reported as
 * {@code ItemDeliveryResult.Status.FAILED} instead, so the item is recorded as failed and
 * its job can complete. Throwing means "ask again later", not "this item failed".
 */
public class ItemDeliveryException extends RuntimeException {
    public ItemDeliveryException(String message, Exception cause) {
        super(message, cause);
    }
}
