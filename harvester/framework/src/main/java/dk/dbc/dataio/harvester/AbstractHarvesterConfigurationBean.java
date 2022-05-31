package dk.dbc.dataio.harvester;

import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.common.utils.flowstore.ejb.FlowStoreServiceConnectorBean;
import dk.dbc.dataio.harvester.types.HarvesterConfig;
import dk.dbc.dataio.harvester.types.HarvesterException;
import org.slf4j.Logger;

import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Abstract base class for harvester configurations
 *
 * @param <T> type parameter for harvester configuration type
 */
public abstract class AbstractHarvesterConfigurationBean<T extends HarvesterConfig<?>> {
    protected List<T> configs;

    @EJB
    protected FlowStoreServiceConnectorBean flowStoreServiceConnectorBean;

    /**
     * Reloads configuration
     *
     * @throws HarvesterException on failure to lookup configuration resource
     */
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void reload() throws HarvesterException {
        getLogger().debug("Retrieving configuration");
        try {
            configs = flowStoreServiceConnectorBean.getConnector().findEnabledHarvesterConfigsByType(getConfigClass());
            getLogger().info("Applying configuration: {}", configs);
        } catch (FlowStoreServiceConnectorException e) {
            throw new HarvesterException("Exception caught while refreshing configuration", e);
        }
    }

    /**
     * @return list of currently enabled harvester configs
     */
    public List<T> getConfigs() {
        return configs == null ? Collections.emptyList() : configs;
    }

    /**
     * @param id harvester config id
     * @return harvesterconfig including disabled configs
     * @throws HarvesterException All flowstore exceptions are transformed to harvester exceptions
     */
    public Optional<T> getConfig(long id) throws HarvesterException {
        try {
            List<T> allConfigs = flowStoreServiceConnectorBean.getConnector().findHarvesterConfigsByType(getConfigClass());
            return allConfigs.stream().filter(config -> config.getId() == id).findFirst();
        } catch (FlowStoreServiceConnectorException e) {
            throw new HarvesterException(String.format("Exception caught while fetching harvester config with id %d", id), e);
        }
    }

    /**
     * @return Logger
     */
    public abstract Logger getLogger();

    /**
     * @return Class of harvester config
     */
    public abstract Class<T> getConfigClass();
}
