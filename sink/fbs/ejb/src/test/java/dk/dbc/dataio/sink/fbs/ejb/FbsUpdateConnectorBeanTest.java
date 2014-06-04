package dk.dbc.dataio.sink.fbs.ejb;

import org.junit.Test;

import javax.ejb.EJBException;

public class FbsUpdateConnectorBeanTest {
    @Test(expected = EJBException.class)
    public void initializeConnector_urlResourceLookupThrowNamingException_throws() {
        final FbsUpdateConnectorBean fbsUpdateConnectorBean = new FbsUpdateConnectorBean();
        fbsUpdateConnectorBean.initializeConnector();
    }
}