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

package dk.dbc.dataio.gui.server;

import dk.dbc.dataio.commons.time.StopWatch;
import dk.dbc.dataio.commons.utils.service.ServiceUtil;
import dk.dbc.dataio.gui.client.proxies.JndiProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.NamingException;

public class JndiProxyImpl implements JndiProxy {
    private static final Logger log = LoggerFactory.getLogger(JndiProxyImpl.class);

    public JndiProxyImpl() {}


    @Override
    public String getJndiResource(String jndiName) {
        log.trace("JndiProxy: getJndiResource({});", jndiName);
        final StopWatch stopWatch = new StopWatch();
        try {
            return ServiceUtil.getStringValueFromSystemPropertyOrJndi(jndiName);
        } catch (NamingException e) {  // We must catch this exception, because it is not present in JRE Emulation Library
            throw new IllegalArgumentException("Naming Exception");
        } finally {
            log.debug("JndiProxy: getJndiResource took {} milliseconds", stopWatch.getElapsedTime());
        }
    }

}
