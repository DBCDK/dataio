package dk.dbc.dataio.sinkservice.rest;

import dk.dbc.dataio.sinkservice.ejb.PingBean;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.junit.Assert.assertThat;

public class SinkServiceApplicationTest {
    @Test
    public void application_hasRegistered_PingBean() {
        final SinkServiceApplication sinkServiceApplication = new SinkServiceApplication();
        assertThat(sinkServiceApplication.getClasses(), hasItem(PingBean.class));
    }

    @Test
    public void application_hasRegistered_JsonExceptionMapper() {
        final SinkServiceApplication sinkServiceApplication = new SinkServiceApplication();
        assertThat(sinkServiceApplication.getClasses(), hasItem(JsonExceptionMapper.class));
    }
}
