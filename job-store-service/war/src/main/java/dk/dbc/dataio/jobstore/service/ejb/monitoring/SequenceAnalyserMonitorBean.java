/*
 * DataIO - Data IO
 * Copyright (C) 2015 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
 * Denmark. CVR: 15149043
 *
 * This file is part of DataIO.
 *
 * DataIO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DataIO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DataIO.  If not, see <http://www.gnu.org/licenses/>.
 */

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
import javax.management.JMX;
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
            SequenceAnalyserMonitorMXBean sequenceAnalyserMonitorMXBean;
            final ObjectName objectName = getObjectName(localName);
            if (mBeanServer.isRegistered(objectName)) {
                // Under GF 4.1 isRegistered() returns true even though
                // unregisterMBean() was called during previous undeploy
                // - seems like a bug
                LOGGER.info("Already registered in JMX: {}", objectName);
                sequenceAnalyserMonitorMXBean = JMX.newMXBeanProxy(
                        mBeanServer, objectName, SequenceAnalyserMonitorMXBean.class);
                sequenceAnalyserMonitorMXBean.setSample(new SequenceAnalyserMonitorSample(0,0));
            } else {
                LOGGER.info("Registering {} in JMX", objectName);
                mBeanServer.registerMBean(new SequenceAnalyserMonitorMXBean() {
                    private SequenceAnalyserMonitorSample sample;
                    @Override
                    public SequenceAnalyserMonitorSample getSample() {
                        return sample;
                    }
                    @Override
                    public void setSample(SequenceAnalyserMonitorSample sample) {
                        this.sample = sample;
                    }
                }, objectName);
                sequenceAnalyserMonitorMXBean = JMX.newMXBeanProxy(
                        mBeanServer, objectName, SequenceAnalyserMonitorMXBean.class);
            }
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
