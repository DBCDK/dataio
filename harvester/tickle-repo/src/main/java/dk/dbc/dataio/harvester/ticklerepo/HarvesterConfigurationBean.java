package dk.dbc.dataio.harvester.ticklerepo;

import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.common.utils.flowstore.ejb.FlowStoreServiceConnectorBean;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.TickleRepoHarvesterConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.Collections;
import java.util.List;

/**
 * This Enterprise Java Bean is responsible for retrieval of harvester configurations via flow-store lookup
 */
@Singleton
public class HarvesterConfigurationBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(HarvesterConfigurationBean.class);

    @EJB
    FlowStoreServiceConnectorBean flowStoreServiceConnectorBean;

    List<TickleRepoHarvesterConfig> configs;

    /**
     * Reloads configuration
     *
     * @throws dk.dbc.dataio.harvester.types.HarvesterException on failure to lookup configuration resource
     */
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void reload() throws HarvesterException {
        LOGGER.debug("Retrieving configuration");
        try {
            configs = flowStoreServiceConnectorBean.getConnector().findEnabledHarvesterConfigsByType(TickleRepoHarvesterConfig.class);
            LOGGER.info("Applying configuration: {}", configs);
        } catch (FlowStoreServiceConnectorException e) {
            throw new HarvesterException("Exception caught while refreshing configuration", e);
        }
    }

    public List<TickleRepoHarvesterConfig> get() {
        return configs == null ? Collections.emptyList() : configs;
    }
}
