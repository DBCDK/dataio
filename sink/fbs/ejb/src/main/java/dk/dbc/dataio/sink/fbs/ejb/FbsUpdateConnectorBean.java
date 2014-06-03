package dk.dbc.dataio.sink.fbs.ejb;

import dk.dbc.dataio.commons.utils.service.ServiceUtil;
import dk.dbc.dataio.sink.fbs.connector.FbsUpdateConnector;
import dk.dbc.dataio.sink.fbs.types.FbsUpdateConnectorException;
import dk.dbc.oss.ns.updatemarcxchange.UpdateMarcXchangeResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.ejb.EJBException;
import javax.ejb.Singleton;
import javax.naming.NamingException;

@Singleton
public class FbsUpdateConnectorBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(FbsUpdateConnectorBean.class);
    private FbsUpdateConnector fbsUpdateConnector;

    @PostConstruct
    public void initializeConnector() {
        LOGGER.debug("Initializing connector");
        try {
            final String endpoint = ServiceUtil.getStringValueFromResource("url/fbs/update-marc-exchange");
            fbsUpdateConnector = new FbsUpdateConnector(endpoint);
        } catch (NamingException | FbsUpdateConnectorException e) {
            throw new EJBException(e);
        }
    }

    public UpdateMarcXchangeResult updateMarcExchange(String agencyId, String collection, String trackingId)
            throws FbsUpdateConnectorException {
        return fbsUpdateConnector.updateMarcExchange(agencyId, collection, trackingId);
    }
}
