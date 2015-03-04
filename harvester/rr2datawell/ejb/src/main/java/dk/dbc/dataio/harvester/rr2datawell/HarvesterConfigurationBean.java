package dk.dbc.dataio.harvester.rr2datawell;

import dk.dbc.dataio.commons.types.jndi.JndiConstants;
import dk.dbc.dataio.commons.utils.service.ServiceUtil;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.RawRepoHarvesterConfig;
import dk.dbc.dataio.jsonb.JSONBException;
import dk.dbc.dataio.jsonb.ejb.JSONBBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.naming.NamingException;

/**
 * This Enterprise Java Bean is responsible for retrieval of harvester configuration via JNDI lookup
 * of {@value dk.dbc.dataio.commons.types.jndi.JndiConstants#CONFIG_RESOURCE_HARVESTER_RR} resource.
 */
@Singleton
@Startup
public class HarvesterConfigurationBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(HarvesterConfigurationBean.class);

    @EJB
    JSONBBean jsonbBean;

    RawRepoHarvesterConfig config;

    /**
     * Initializes configuration
     * @throws HarvesterException on failure to lookup configuration resource or failure to
     * unmarshall returned value returned by lookup to configuration POJO.
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
     * @throws HarvesterException on failure to lookup configuration resource or failure to
     * unmarshall returned value returned by lookup to configuration POJO.
     */
    public void reload() throws HarvesterException {
        LOGGER.debug("Retrieving configuration");
        try {
            final String jsonConfig = ServiceUtil.getStringValueFromResource(JndiConstants.CONFIG_RESOURCE_HARVESTER_RR);
            config = jsonbBean.getContext().unmarshall(jsonConfig, RawRepoHarvesterConfig.class);
            LOGGER.info("Applying configuration: {}", config);
        } catch (NamingException | JSONBException e) {
            throw new HarvesterException("Exception caught while refreshing configuration", e);
        }
    }

    public RawRepoHarvesterConfig get() {
        return config;
    }
}
