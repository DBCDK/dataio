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

package dk.dbc.dataio.harvester;

import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.common.utils.flowstore.ejb.FlowStoreServiceConnectorBean;
import dk.dbc.dataio.harvester.types.HarvesterConfig;
import dk.dbc.dataio.harvester.types.HarvesterException;
import java.util.Optional;
import org.slf4j.Logger;

import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.Collections;
import java.util.List;

/**
 * Abstract base class for harvester configurations
 *
 * @param <T> type parameter for harvester configuration type
 */
public abstract class AbstractHarvesterConfigurationBean<T extends HarvesterConfig<?>>  {
    protected List<T> configs;

    @EJB
    protected FlowStoreServiceConnectorBean flowStoreServiceConnectorBean;

    /**
     * Reloads configuration
     * @throws HarvesterException on failure to lookup configuration resource
     */
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void reload() throws HarvesterException {
        getLogger().debug("Retrieving configuration");
        try {
            configs = flowStoreServiceConnectorBean.getConnector().findEnabledHarvesterConfigsByType(getConfigClass());
            getLogger().info("Applying configuration: {}", configs);
        } catch (FlowStoreServiceConnectorException e) {
            throw new HarvesterException("Exception caught while refreshing configuration", e);
        }
    }

    /**
     * @return list of currently enabled harvester configs
     */
    public List<T> getConfigs() {
        return configs == null ? Collections.emptyList() : configs;
    }

    /**
     *
     * @param id
     * @return harvesterconfig including disabled configs
     */
    public Optional<T> getConfig(long id) throws FlowStoreServiceConnectorException {
        List<T> allConfigs = flowStoreServiceConnectorBean.getConnector().findHarvesterConfigsByType(getConfigClass());
        return allConfigs.stream().filter(config -> config.getId() == id).findFirst();
    }

    /**
     * @return Logger
     */
    public abstract Logger getLogger();

    /**
     * @return Class of harvester config
     */
    public abstract Class<T> getConfigClass();
}
