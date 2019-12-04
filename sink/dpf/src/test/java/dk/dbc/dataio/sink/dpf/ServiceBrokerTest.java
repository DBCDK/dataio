package dk.dbc.dataio.sink.dpf;

import dk.dbc.weekresolver.WeekResolverResult;
import dk.dbc.weekresolver.WeekResolverConnector;
import dk.dbc.weekresolver.WeekResolverConnectorException;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ServiceBrokerTest {
    private ServiceBroker serviceBroker;
    private static final String DPF = "DPF";

    @Before
    public void init() {
        serviceBroker = new ServiceBroker();
        serviceBroker.weekResolverConnector = mock(WeekResolverConnector.class);
    }

    @Test
    public void getWeekCodeTest_ok() throws Exception {
        final WeekResolverResult weekResolverResult = WeekResolverResult.create(null, 42, 2019, "DPF201942", DPF);

        when(serviceBroker.weekResolverConnector.getWeekCode(DPF)).thenReturn(weekResolverResult);

        assertThat("ok result", serviceBroker.getCatalogueCode(DPF), is("DPF201942"));
    }

    @Test(expected = WeekResolverConnectorException.class)
    public void getWeekCodeTest_ResultEmpty() throws Exception {
        final WeekResolverResult weekResolverResult = WeekResolverResult.create(null, 0, 0, null, null);

        when(serviceBroker.weekResolverConnector.getWeekCode(DPF)).thenReturn(weekResolverResult);

        serviceBroker.getCatalogueCode(DPF);
    }

    @Test(expected = WeekResolverConnectorException.class)
    public void getWeekCodeTest_ResultMismatch() throws Exception {
        final WeekResolverResult weekResolverResult = WeekResolverResult.create(null, 42, 2019, "AAA201942", DPF);

        when(serviceBroker.weekResolverConnector.getWeekCode(DPF)).thenReturn(weekResolverResult);

        serviceBroker.getCatalogueCode(DPF);
    }

}
