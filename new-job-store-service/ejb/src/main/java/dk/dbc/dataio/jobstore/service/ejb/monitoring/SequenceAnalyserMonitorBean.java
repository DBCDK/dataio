package dk.dbc.dataio.jobstore.service.ejb.monitoring;

import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This Enterprise Java Bean (EJB) is responsible for the setting up, tearing
 * down and exposing of SequenceAnalyser JMX MXBean monitors
 */
@Singleton
@Startup
public class SequenceAnalyserMonitorBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(SequenceAnalyserMonitorBean.class);

    private final ConcurrentHashMap<String, SequenceAnalyserMonitorMXBean> mBeans = new ConcurrentHashMap<>(16, 0.9F, 1);

    MBeanServer mBeanServer;

    /**
     * Caches the platform MBeanServer
     */
    @PostConstruct
    public void getMBeanServer() {
        mBeanServer = ManagementFactory.getPlatformMBeanServer();
    }

    /**
     * Creates and registers SequenceAnalyserMonitorMXBean instance with
     * object name derived from given {@code localName} in MBean server.
     * The created MBean object is exposed through the map returned
     * by the {@code getMBeans()} method - keyed by {@code localName}
     * @param localName user defined part of MBean object name
     * @throws NullPointerException if given null-valued localName argument
     * @throws IllegalArgumentException if given empty-valued localName argument
     * @throws IllegalStateException if unable to register MBean object
     */
    public void registerInJmx(String localName)
            throws NullPointerException, IllegalArgumentException, IllegalStateException {
        InvariantUtil.checkNotNullNotEmptyOrThrow(localName, "localName");
        try {
            final SequenceAnalyserMonitorMXBean sequenceAnalyserMonitorMXBean = new SequenceAnalyserMonitorMXBean() {
                private SequenceAnalyserMonitorSample sample;
                @Override
                public SequenceAnalyserMonitorSample getSample() {
                    return sample;
                }
                @Override
                public void setSample(SequenceAnalyserMonitorSample sample) {
                    this.sample = sample;
                }
            };
            final ObjectName objectName = getObjectName(localName);
            LOGGER.info("Registering {} in JMX", objectName);
            mBeanServer.registerMBean(sequenceAnalyserMonitorMXBean, objectName);
            mBeans.put(localName, sequenceAnalyserMonitorMXBean);
        } catch (MalformedObjectNameException | NotCompliantMBeanException | InstanceAlreadyExistsException | MBeanRegistrationException e) {
            throw new IllegalStateException("Unable to register monitor for " + localName, e);
        }
    }

    /**
     * @return created MBean object instances
     */
    public ConcurrentHashMap<String, SequenceAnalyserMonitorMXBean> getMBeans() {
        return mBeans;
    }

    /**
     * Unregisters created MBean objects from MBean server
     * @throws IllegalStateException if unable to unregister MBean object
     */
    @PreDestroy
    public void unregisterInJmx() throws IllegalStateException {
        try {
            for (String localName : mBeans.keySet()) {
                final ObjectName objectName = getObjectName(localName);
                LOGGER.info("Unregistering {} in JMX", objectName);
                mBeanServer.unregisterMBean(objectName);
                mBeans.remove(localName);
            }
        } catch (InstanceNotFoundException | MBeanRegistrationException | MalformedObjectNameException e) {
            throw new IllegalStateException("Unable to unregister monitor", e);
        }
    }

    private ObjectName getObjectName(String localName) throws MalformedObjectNameException {
        return new ObjectName("dk.dbc.dataio.jobstore.monitoring:type=" + this.getClass().getSimpleName() + ",name=" + localName);
    }
}
