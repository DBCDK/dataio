package dk.dbc.dataio.harvester.dmat;

import dk.dbc.dataio.harvester.AbstractHarvesterConfigurationBean;
import dk.dbc.dataio.harvester.types.DMatHarvesterConfig;
import jakarta.ejb.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class HarvesterConfigurationBean extends AbstractHarvesterConfigurationBean<DMatHarvesterConfig> {
    private static final Logger LOGGER = LoggerFactory.getLogger(HarvesterConfigurationBean.class);

    @Override
    public Logger getLogger() {
        return LOGGER;
    }

    @Override
    public Class<DMatHarvesterConfig> getConfigClass() {
        return DMatHarvesterConfig.class;
    }
}
