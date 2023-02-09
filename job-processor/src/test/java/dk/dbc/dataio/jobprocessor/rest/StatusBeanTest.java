package dk.dbc.dataio.jobprocessor.rest;

import dk.dbc.dataio.jobprocessor.ejb.CapacityBean;
import dk.dbc.dataio.jobprocessor.ejb.HealthBean;
import dk.dbc.dataio.jobprocessor.exception.JobProcessorTerminallyIllException;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;

import javax.ws.rs.core.Response;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class StatusBeanTest {
    @Test
    public void statusBeanReturnsResponseWhenCapacityIsNotExceeded()
            throws JobProcessorTerminallyIllException {
        assertThat(createStatusBean().getStatus(), is(notNullValue()));
    }

    @Test
    public void statusBeanDownsWhenCapacityIsExceeded() {
        final StatusBean statusBean = createStatusBean();
        statusBean.capacityBean.signalTimeout();
        Assertions.assertNotEquals(Response.Status.OK.toEnum(), statusBean.getStatus().getStatusInfo().toEnum(), "Server should be marked down");
    }

    @Test
    public void statusBeanDownOnTerminallyIll() {
        final StatusBean statusBean = createStatusBean();
        statusBean.healthBean.signalTerminallyIll();
        Assertions.assertNotEquals(Response.Status.OK.toEnum(), statusBean.getStatus().getStatusInfo().toEnum(), "Server should be marked down");
    }

    private StatusBean createStatusBean() {
        final StatusBean statusBean = new StatusBean();
        statusBean.capacityBean = new CapacityBean();
        statusBean.healthBean = new HealthBean();
        return statusBean;
    }
}
