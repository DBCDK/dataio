package dk.dbc.dataio.sink.fbs.ejb;

import dk.dbc.dataio.commons.types.jndi.JndiConstants;
import dk.dbc.dataio.commons.utils.service.ServiceUtil;
import dk.dbc.dataio.sink.fbs.connector.FbsUpdateConnector;
import dk.dbc.dataio.sink.fbs.types.FbsUpdateConnectorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.ejb.EJBException;
import javax.ejb.Singleton;
import javax.naming.NamingException;

/**
 * This Enterprise Java Bean (EJB) singleton is used as a connector
 * to the FBS UpdateMarcXchange web-service.
 */
@Singleton
public class FbsUpdateConnectorBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(FbsUpdateConnectorBean.class);
    FbsUpdateConnector fbsUpdateConnector;

    @PostConstruct
    public void initializeConnector() {
        LOGGER.debug("Initializing connector");
        try {
            final String endpoint = ServiceUtil.getStringValueFromResource(JndiConstants.URL_RESOURCE_FBS_WS);
            fbsUpdateConnector = new FbsUpdateConnector(endpoint);
        } catch (NamingException | FbsUpdateConnectorException e) {
            throw new EJBException(e);
        }
    }

    public FbsUpdateConnector getConnector() {
        System.err.println("TOTEM: Requesting FbsUpdateConnector.");
        return fbsUpdateConnector;
    }
}
