/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.dataio.harvester.promat;

import dk.dbc.dataio.harvester.AbstractHarvesterConfigurationBean;
import dk.dbc.dataio.harvester.types.PromatHarvesterConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Singleton;

@Singleton
public class HarvesterConfigurationBean extends AbstractHarvesterConfigurationBean<PromatHarvesterConfig> {
    private static final Logger LOGGER = LoggerFactory.getLogger(HarvesterConfigurationBean.class);

    @Override
    public Logger getLogger() {
        return LOGGER;
    }

    @Override
    public Class<PromatHarvesterConfig> getConfigClass() {
        return PromatHarvesterConfig.class;
    }
}
