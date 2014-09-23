package dk.dbc.dataio.logstore.logback;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.AbstractMatcherFilter;
import ch.qos.logback.core.spi.FilterReply;
import org.slf4j.MDC;

/**
 * This filter allows output for a given MDC key
 *
 * <p>
 * To allow output for the key, set the OnMatch option
 * to ACCEPT. To disable output for the given value, set
 * the OnMatch option to DENY.
 *
 * <p>
 * By default, values of the OnMatch and OnMisMatch
 * options are set to NEUTRAL.
 */
public class MdcKeyExistsFilter extends AbstractMatcherFilter<ILoggingEvent> {
    String MDCKey;

    public void setMDCKey(String MDCKey) {
        this.MDCKey = MDCKey;
    }

    @Override
    public FilterReply decide(ILoggingEvent event) {
        if (MDCKey == null || !isStarted()) {
            return FilterReply.NEUTRAL;
        }

        final String value = MDC.get(MDCKey);
        if (value != null) {
            return onMatch;
        }
        return onMismatch;
    }
}
