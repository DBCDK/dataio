package dk.dbc.dataio.harvester.ticklerepo;

import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.common.utils.flowstore.ejb.FlowStoreServiceConnectorBean;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.TickleRepoHarvesterConfig;
import dk.dbc.ticklerepo.TickleRepo;
import dk.dbc.ticklerepo.dto.DataSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * This Enterprise Java Bean is responsible for retrieval of harvester configurations via flow-store lookup
 */
@Singleton
@Startup
public class HarvesterConfigurationBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(HarvesterConfigurationBean.class);

    @EJB
    FlowStoreServiceConnectorBean flowStoreServiceConnectorBean;

    @EJB
    TickleRepo tickleRepo;

    List<ExtendedTickleRepoHarvesterConfig> configs;

    /**
     * Initializes configuration
     * @throws javax.ejb.EJBException on failure to lookup configuration resource
     */
    @PostConstruct
    public void initialize() {
        try {
            reload();
        } catch (HarvesterException e) {
            throw new EJBException(e);
        }
    }

    /**
     * Reloads configuration
     * @throws dk.dbc.dataio.harvester.types.HarvesterException on failure to lookup configuration resource
     */
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void reload() throws HarvesterException {
        LOGGER.debug("Retrieving configuration");
        try {
            configs = new ArrayList<>();
            List<TickleRepoHarvesterConfig> tickleRepoHarvesterConfigs = flowStoreServiceConnectorBean.getConnector().findEnabledHarvesterConfigsByType(TickleRepoHarvesterConfig.class);
            for(TickleRepoHarvesterConfig config : tickleRepoHarvesterConfigs) {
                Optional<DataSet> dataSet = tickleRepo.lookupDataSet(new DataSet().withName(config.getContent().getDatasetName()));
                configs.add(new ExtendedTickleRepoHarvesterConfig().withTickleRepoHarvesterConfig(config).withDataSet(dataSet.orElse(null)));
            }
            LOGGER.info("Applying configuration: {}", configs);
        } catch (FlowStoreServiceConnectorException e) {
            throw new HarvesterException("Exception caught while refreshing configuration", e);
        }
    }

    public List<ExtendedTickleRepoHarvesterConfig> get() {
        return configs;
    }
}