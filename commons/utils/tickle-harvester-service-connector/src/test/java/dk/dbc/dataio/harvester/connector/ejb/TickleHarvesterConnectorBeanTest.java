package dk.dbc.dataio.harvester.connector.ejb;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;


public class TickleHarvesterConnectorBeanTest {
    @Test
    public void initializeConnector_environmentNotSet_throws() {
        assertThrows(NullPointerException.class, TickleHarvesterServiceConnectorBean::new);
    }

    @Test
    public void initializeConnector() {
        TickleHarvesterServiceConnectorBean bean = new TickleHarvesterServiceConnectorBean("http://test");
        assertThat(bean.getConnector(), not(nullValue()));
    }
}
