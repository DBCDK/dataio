package dk.dbc.dataio.rrharvester.service.connector.ejb;

import jakarta.ejb.EJBException;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;


public class RRHarvesterConnectorBeanTest {

    @Test
    public void initializeConnector_environmentNotSet_throws() {
        assertThrows(EJBException.class, () -> new RRHarvesterServiceConnectorBean().initializeConnector());
    }

    @Test
    public void initializeConnector() {
        RRHarvesterServiceConnectorBean rrHarvesterServiceConnectorBean = new RRHarvesterServiceConnectorBean("http://test");
        assertThat(rrHarvesterServiceConnectorBean.getConnector(), not(nullValue()));
    }
}
