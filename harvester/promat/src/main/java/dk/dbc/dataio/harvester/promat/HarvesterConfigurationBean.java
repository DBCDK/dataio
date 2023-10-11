package dk.dbc.dataio.harvester.promat;

import dk.dbc.dataio.harvester.AbstractHarvesterConfigurationBean;
import dk.dbc.dataio.harvester.types.PromatHarvesterConfig;
import jakarta.ejb.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
