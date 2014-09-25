package dk.dbc.dataio.logstore.service.connector.ejb;

import org.junit.Test;

import javax.ejb.EJBException;

public class LogStoreServiceConnectorBeanTest {
    @Test(expected = EJBException.class)
    public void initializeConnector_urlResourceLookupThrowNamingException_throws() {
        final LogStoreServiceConnectorBean logStoreServiceConnectorBean = new LogStoreServiceConnectorBean();
        logStoreServiceConnectorBean.initializeConnector();
    }
}