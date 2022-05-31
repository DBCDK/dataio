package dk.dbc.dataio.harvester.periodicjobs;

import dk.dbc.dataio.harvester.AbstractHarvesterConfigurationBean;
import dk.dbc.dataio.harvester.types.PeriodicJobsHarvesterConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Singleton;

@Singleton
public class HarvesterConfigurationBean extends AbstractHarvesterConfigurationBean<PeriodicJobsHarvesterConfig> {
    private static final Logger LOGGER = LoggerFactory.getLogger(HarvesterConfigurationBean.class);

    @Override
    public Logger getLogger() {
        return LOGGER;
    }

    @Override
    public Class<PeriodicJobsHarvesterConfig> getConfigClass() {
        return PeriodicJobsHarvesterConfig.class;
    }
}
