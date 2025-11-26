package dk.dbc.dataio.harvester.rr_dm3;

import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.common.utils.flowstore.ejb.FlowStoreServiceConnectorBean;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.RRV3HarvesterConfig;
import jakarta.ejb.EJB;
import jakarta.ejb.Singleton;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    List<RRV3HarvesterConfig> configs;

    /**
     * Reloads configuration
     *
     * @throws dk.dbc.dataio.harvester.types.HarvesterException on failure to lookup configuration resource
     */
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void reload() throws HarvesterException {
        LOGGER.debug("Retrieving configuration");
        try {
            configs = flowStoreServiceConnectorBean.getConnector().findEnabledHarvesterConfigsByType(RRV3HarvesterConfig.class);
            LOGGER.debug("Applying configuration: {}", configs);
        } catch (FlowStoreServiceConnectorException e) {
            throw new HarvesterException("Exception caught while refreshing configuration", e);
        }
    }

    public List<RRV3HarvesterConfig> get() {
        return configs == null ? Collections.emptyList() : configs;
    }
}
