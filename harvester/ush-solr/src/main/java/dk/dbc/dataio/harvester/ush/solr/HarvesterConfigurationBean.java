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

package dk.dbc.dataio.harvester.ush.solr;

import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.common.utils.flowstore.ejb.FlowStoreServiceConnectorBean;
import dk.dbc.dataio.commons.utils.ush.UshHarvesterConnectorException;
import dk.dbc.dataio.commons.utils.ush.ejb.UshHarvesterConnectorBean;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.UshHarvesterProperties;
import dk.dbc.dataio.harvester.types.UshSolrHarvesterConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * This Enterprise Java Bean is responsible for retrieval of harvester configuration
 */
@Singleton
public class HarvesterConfigurationBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(HarvesterConfigurationBean.class);

    List<UshSolrHarvesterConfig> configs;

    @EJB
    FlowStoreServiceConnectorBean flowStoreServiceConnectorBean;

    @EJB
    UshHarvesterConnectorBean ushHarvesterConnectorBean;

    /**
     * Reloads configuration
     * @throws HarvesterException on failure to lookup configuration resource
     */
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void reload() throws HarvesterException {
        LOGGER.debug("Retrieving configuration");
        Map<Integer, UshHarvesterProperties> ushHarvesterPropertiesMap;
        try {
            configs = flowStoreServiceConnectorBean.getConnector().findEnabledHarvesterConfigsByType(UshSolrHarvesterConfig.class);
            ushHarvesterPropertiesMap = ushHarvesterConnectorBean.getConnector().listIndexedUshHarvesterJobs();
            for(UshSolrHarvesterConfig config : configs) {
                final int ushHarvesterJobId = config.getContent().getUshHarvesterJobId();
                if(ushHarvesterPropertiesMap.containsKey(ushHarvesterJobId)) {
                    config.getContent().withUshHarvesterProperties(ushHarvesterPropertiesMap.get(ushHarvesterJobId));
                }
            }
            LOGGER.info("Applying configuration: {}", configs);
        } catch (FlowStoreServiceConnectorException | UshHarvesterConnectorException e) {
            throw new HarvesterException("Exception caught while refreshing configuration", e);
        }
    }

    public List<UshSolrHarvesterConfig> get() {
        return configs == null ? Collections.emptyList() : configs;
    }
}
