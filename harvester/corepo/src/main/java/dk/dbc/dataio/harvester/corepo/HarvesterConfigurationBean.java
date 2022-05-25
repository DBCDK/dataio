package dk.dbc.dataio.harvester.corepo;

import dk.dbc.dataio.harvester.AbstractHarvesterConfigurationBean;
import dk.dbc.dataio.harvester.types.CoRepoHarvesterConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Singleton;

@Singleton
public class HarvesterConfigurationBean extends AbstractHarvesterConfigurationBean<CoRepoHarvesterConfig> {
    private static final Logger LOGGER = LoggerFactory.getLogger(HarvesterConfigurationBean.class);

    @Override
    public Logger getLogger() {
        return LOGGER;
    }

    @Override
    public Class<CoRepoHarvesterConfig> getConfigClass() {
        return CoRepoHarvesterConfig.class;
    }
}
