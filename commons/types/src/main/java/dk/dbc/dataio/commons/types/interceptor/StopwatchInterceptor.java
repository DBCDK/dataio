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

package dk.dbc.dataio.commons.types.interceptor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

@Stopwatch
@Interceptor
public class StopwatchInterceptor {
    private static final Logger LOGGER = LoggerFactory.getLogger(StopwatchInterceptor.class);

    @AroundInvoke
    public Object time(InvocationContext invocationContext) throws Exception {
        final String systemClassName = invocationContext.getMethod().getDeclaringClass().getCanonicalName();
        final String systemMethodName = invocationContext.getMethod().getName();

        final long startTime = System.currentTimeMillis();
        final Object businessCall;
        try {
            businessCall = invocationContext.proceed();
        } finally {
            LOGGER.info("calling method {}.{} took {} milliseconds",
                    systemClassName, systemMethodName, System.currentTimeMillis() - startTime);
        }
        return businessCall;
    }
}