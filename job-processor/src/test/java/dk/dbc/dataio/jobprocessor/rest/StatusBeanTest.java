package dk.dbc.dataio.jobprocessor.rest;

import dk.dbc.dataio.jobprocessor.ejb.CapacityBean;
import dk.dbc.dataio.jobprocessor.ejb.HealthBean;
import dk.dbc.dataio.jobprocessor.exception.JobProcessorCapacityExceededException;
import dk.dbc.dataio.jobprocessor.exception.JobProcessorTerminallyIllException;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

public class StatusBeanTest {
    @Test
    public void statusBeanReturnsResponseWhenCapacityIsNotExceeded()
            throws JobProcessorTerminallyIllException {
        assertThat(createStatusBean().getStatus(), is(notNullValue()));
    }

    @Test
    public void statusBeanThrowsWhenCapacityIsExceeded()
            throws JobProcessorTerminallyIllException {
        final StatusBean statusBean = createStatusBean();
        statusBean.capacityBean.signalCapacityExceeded();
        try {
            statusBean.getStatus();
            fail("no JobProcessorCapacityExceededException thrown");
        } catch (JobProcessorCapacityExceededException ignored) {}
    }

    @Test
    public void statusBeanThrowsOnTerminallyIll() {
        final StatusBean statusBean = createStatusBean();
        statusBean.healthBean.signalTerminallyIll();
        try {
            statusBean.getStatus();
            fail("no JobProcessorTerminallyIllException thrown");
        } catch (JobProcessorTerminallyIllException ignored) {}
    }

    private StatusBean createStatusBean() {
        final StatusBean statusBean = new StatusBean();
        statusBean.capacityBean = new CapacityBean();
        statusBean.healthBean = new HealthBean();
        return statusBean;
    }
}
