package dk.dbc.dataio.jobstore.service.ejb.monitoring;

import org.junit.Test;

import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SequenceAnalyserMonitorBeanTest {
    private MBeanServer mBeanServer = mock(MBeanServer.class);

    @Test(expected = NullPointerException.class)
    public void registerInJmx_localNameArgIsNull_throws() {
        getSequenceAnalyserMonitorBean().registerInJmx(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void registerInJmx_localNameArgIsEmpty_throws() {
        getSequenceAnalyserMonitorBean().registerInJmx("");
    }

    @Test(expected = IllegalStateException.class)
    public void registerInJmx_mBeanObjectNameIsInvalid_throws() {
        getSequenceAnalyserMonitorBean().registerInJmx(",=:");
    }

    @Test(expected = IllegalStateException.class)
    public void registerInJmx_mBeanServerThrowsNotCompliantMBeanException_throws()
            throws NotCompliantMBeanException, InstanceAlreadyExistsException, MBeanRegistrationException {
        when(mBeanServer.registerMBean(any(SequenceAnalyserMonitorMXBean.class), any(ObjectName.class)))
                .thenThrow(new NotCompliantMBeanException());
        getSequenceAnalyserMonitorBean().registerInJmx("localName");
    }

    @Test(expected = IllegalStateException.class)
    public void registerInJmx_mBeanServerThrowsInstanceAlreadyExistsException_throws()
            throws NotCompliantMBeanException, InstanceAlreadyExistsException, MBeanRegistrationException {
        when(mBeanServer.registerMBean(any(SequenceAnalyserMonitorMXBean.class), any(ObjectName.class)))
                .thenThrow(new InstanceAlreadyExistsException());
        getSequenceAnalyserMonitorBean().registerInJmx("localName");
    }

    @Test(expected = IllegalStateException.class)
    public void registerInJmx_mBeanServerThrowsMBeanRegistrationException_throws()
            throws NotCompliantMBeanException, InstanceAlreadyExistsException, MBeanRegistrationException {
        when(mBeanServer.registerMBean(any(SequenceAnalyserMonitorMXBean.class), any(ObjectName.class)))
                .thenThrow(new MBeanRegistrationException(new NullPointerException()));
        getSequenceAnalyserMonitorBean().registerInJmx("localName");
    }

    @Test
    public void registerInJmx_localNameArgIsValid_registersNewMBean() {
        final SequenceAnalyserMonitorBean sequenceAnalyserMonitorBean = getSequenceAnalyserMonitorBean();
        sequenceAnalyserMonitorBean.registerInJmx("localName");
        assertThat(sequenceAnalyserMonitorBean.getMBeans().size(), is(1));
        assertThat(sequenceAnalyserMonitorBean.getMBeans().containsKey("localName"), is(true));
        assertThat(sequenceAnalyserMonitorBean.getMBeans().get("localName") != null, is(true));
    }

    @Test
    public void unregisterInJmx_mBeanServerThrowsMBeanRegistrationException_throws()
            throws MBeanRegistrationException, InstanceNotFoundException {
        doThrow(new MBeanRegistrationException(new NullPointerException()))
                .when(mBeanServer).unregisterMBean(any(ObjectName.class));

        final SequenceAnalyserMonitorBean sequenceAnalyserMonitorBean = getSequenceAnalyserMonitorBean();
        sequenceAnalyserMonitorBean.registerInJmx("localName");
        try {
            sequenceAnalyserMonitorBean.unregisterInJmx();
            fail("No exception thrown");
        } catch (IllegalStateException e) {
        }
    }

    @Test
    public void unregisterInJmx_mBeanServerThrowsInstanceNotFoundException_throws()
            throws MBeanRegistrationException, InstanceNotFoundException {
        doThrow(new InstanceNotFoundException())
                .when(mBeanServer).unregisterMBean(any(ObjectName.class));

        final SequenceAnalyserMonitorBean sequenceAnalyserMonitorBean = getSequenceAnalyserMonitorBean();
        sequenceAnalyserMonitorBean.registerInJmx("localName");
        try {
            sequenceAnalyserMonitorBean.unregisterInJmx();
            fail("No exception thrown");
        } catch (IllegalStateException e) {
        }
    }

    @Test
    public void unregisterInJmx_mBeansAreRegistered_unregistersMBeans() {
        final SequenceAnalyserMonitorBean sequenceAnalyserMonitorBean = getSequenceAnalyserMonitorBean();
        sequenceAnalyserMonitorBean.registerInJmx("localName");
        assertThat(sequenceAnalyserMonitorBean.getMBeans().size(), is(1));
        sequenceAnalyserMonitorBean.unregisterInJmx();
        assertThat(sequenceAnalyserMonitorBean.getMBeans().size(), is(0));
    }

    private SequenceAnalyserMonitorBean getSequenceAnalyserMonitorBean() {
        final SequenceAnalyserMonitorBean sequenceAnalyserMonitorBean = new SequenceAnalyserMonitorBean();
        sequenceAnalyserMonitorBean.mBeanServer = mBeanServer;
        return sequenceAnalyserMonitorBean;
    }
}