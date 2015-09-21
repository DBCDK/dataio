/*
 * DataIO - Data IO
 * Copyright (C) 2015 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
 * Denmark. CVR: 15149043
 *
 * This file is part of DataIO.
 *
 * DataIO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DataIO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DataIO.  If not, see <http://www.gnu.org/licenses/>.
 */

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
