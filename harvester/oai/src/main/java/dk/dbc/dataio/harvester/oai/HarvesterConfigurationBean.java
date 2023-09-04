package dk.dbc.dataio.harvester.oai;

import dk.dbc.dataio.harvester.AbstractHarvesterConfigurationBean;
import dk.dbc.dataio.harvester.types.OaiHarvesterConfig;
import jakarta.ejb.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class HarvesterConfigurationBean extends AbstractHarvesterConfigurationBean<OaiHarvesterConfig> {
    private static final Logger LOGGER = LoggerFactory.getLogger(HarvesterConfigurationBean.class);

    @Override
    public Logger getLogger() {
        return LOGGER;
    }

    @Override
    public Class<OaiHarvesterConfig> getConfigClass() {
        return OaiHarvesterConfig.class;
    }
}
