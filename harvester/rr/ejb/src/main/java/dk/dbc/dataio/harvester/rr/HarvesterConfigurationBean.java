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

package dk.dbc.dataio.harvester.rr;

import dk.dbc.dataio.commons.types.jndi.JndiConstants;
import dk.dbc.dataio.commons.utils.service.ServiceUtil;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.RawRepoHarvesterConfig;
import dk.dbc.dataio.jsonb.JSONBContext;
import dk.dbc.dataio.jsonb.JSONBException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.ejb.EJBException;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.naming.NamingException;

/**
 * This Enterprise Java Bean is responsible for retrieval of harvester configuration via JNDI lookup
 * of {@value dk.dbc.dataio.commons.types.jndi.JndiConstants#CONFIG_RESOURCE_HARVESTER_RR} resource.
 */
@Singleton
@Startup
public class HarvesterConfigurationBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(HarvesterConfigurationBean.class);

    JSONBContext jsonbContext = new JSONBContext();

    RawRepoHarvesterConfig config;

    /**
     * Initializes configuration
     * @throws javax.ejb.EJBException on failure to lookup configuration resource or failure to
     * unmarshall returned value returned by lookup to configuration POJO.
     */
    @PostConstruct
    public void initialize() {
        try {
            reload();
        } catch (HarvesterException e) {
            throw new EJBException(e);
        }
    }

    /**
     * Reloads configuration
     * @throws dk.dbc.dataio.harvester.types.HarvesterException on failure to lookup configuration resource or failure to
     * unmarshall returned value returned by lookup to configuration POJO.
     */
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void reload() throws HarvesterException {
        LOGGER.debug("Retrieving configuration");
        try {
            final String jsonConfig = ServiceUtil.getStringValueFromResource(JndiConstants.CONFIG_RESOURCE_HARVESTER_RR);
            config = jsonbContext.unmarshall(jsonConfig, RawRepoHarvesterConfig.class);
            LOGGER.info("Applying configuration: {}", config);
        } catch (NamingException | JSONBException e) {
            throw new HarvesterException("Exception caught while refreshing configuration", e);
        }
    }

    public RawRepoHarvesterConfig get() {
        return config;
    }
}
