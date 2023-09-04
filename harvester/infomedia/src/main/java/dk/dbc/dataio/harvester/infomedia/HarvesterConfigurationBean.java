package dk.dbc.dataio.harvester.infomedia;

import dk.dbc.dataio.harvester.AbstractHarvesterConfigurationBean;
import dk.dbc.dataio.harvester.types.InfomediaHarvesterConfig;
import jakarta.ejb.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class HarvesterConfigurationBean extends AbstractHarvesterConfigurationBean<InfomediaHarvesterConfig> {
    private static final Logger LOGGER = LoggerFactory.getLogger(HarvesterConfigurationBean.class);

    @Override
    public Logger getLogger() {
        return LOGGER;
    }

    @Override
    public Class<InfomediaHarvesterConfig> getConfigClass() {
        return InfomediaHarvesterConfig.class;
    }
}
