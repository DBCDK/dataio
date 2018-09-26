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
import dk.dbc.dataio.gui.client.proxies.ConfigProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigProxyImpl implements ConfigProxy {
    private static final Logger log = LoggerFactory.getLogger(ConfigProxyImpl.class);

    public ConfigProxyImpl() {}


    @Override
    public String getConfigResource(String configName) {
        log.trace("ConfigProxy: getConfigResource({});", configName);
        final StopWatch stopWatch = new StopWatch();
        String systemProperty = ServiceUtil.getStringValueFromSystemEnvironmentOrProperty(configName);
        log.debug("ConfigProxy: getConfigResource took {} milliseconds", stopWatch.getElapsedTime());
        return systemProperty;
    }

}
