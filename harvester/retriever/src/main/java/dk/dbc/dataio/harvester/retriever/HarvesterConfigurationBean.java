package dk.dbc.dataio.harvester.retriever;

import dk.dbc.dataio.harvester.AbstractHarvesterConfigurationBean;
import dk.dbc.dataio.harvester.types.RetrieverHarvesterConfig;
import jakarta.ejb.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class HarvesterConfigurationBean extends AbstractHarvesterConfigurationBean<RetrieverHarvesterConfig> {
    private static final Logger LOGGER = LoggerFactory.getLogger(HarvesterConfigurationBean.class);

    @Override
    public Logger getLogger() {
        return LOGGER;
    }

    @Override
    public Class<RetrieverHarvesterConfig> getConfigClass() {
        return RetrieverHarvesterConfig.class;
    }
}
