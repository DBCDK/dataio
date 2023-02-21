package dk.dbc.dataio.registry;

import dk.dbc.dataio.registry.metrics.CounterMetric;
import dk.dbc.dataio.registry.metrics.GaugeMetric;
import dk.dbc.dataio.registry.metrics.SimpleTimerMetric;
import dk.dbc.invariant.InvariantUtil;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Gauge;
import org.eclipse.microprofile.metrics.Metric;
import org.eclipse.microprofile.metrics.MetricID;
import org.eclipse.microprofile.metrics.SimpleTimer;
import org.eclipse.microprofile.metrics.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class JMXMetricRegistry implements IMetricRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger(JMXMetricRegistry.class);
    private static final MBeanServer beanServer = ManagementFactory.getPlatformMBeanServer();
    private static final Map<MetricID, Metric> map = new ConcurrentHashMap<>();
    private static final JMXMetricRegistry INSTANCE = new JMXMetricRegistry();

    private JMXMetricRegistry() {
    }

    public static JMXMetricRegistry create() {
        return INSTANCE;
    }

    public ObjectName makeObjectName(MetricID metricID) {
        String name = null;
        try {
            String tags = metricID.getTags().entrySet().stream()
                    .filter(e -> !"name".equalsIgnoreCase(e.getKey()))
                    .map(e -> e.getKey().toLowerCase() + "=" + e.getValue().toLowerCase())
                    .collect(Collectors.joining(","));
            name = "dbc:type=service,name=" + metricID.getName().toLowerCase() + (tags.isEmpty() ? "" : "," + tags);
            return new ObjectName(name);
        } catch (MalformedObjectNameException e) {
            throw new RuntimeException("Invalid object name: " + name, e);
        }
    }

    @Override
    public Counter counter(MetricID metricID) {
        return findOrRegister(metricID, CounterMetric::new);
    }

    @Override
    public <T, R extends Number> Gauge<R> gauge(MetricID metricID, T t, Function<T, R> function) {
        return findOrRegister(metricID, () -> new GaugeMetric<>(() -> function.apply(t)));
    }

    @Override
    public <T extends Number> Gauge<T> gauge(MetricID metricID, Supplier<T> supplier) {
        return findOrRegister(metricID, () -> new GaugeMetric<>(supplier));
    }

    @Override
    public Timer timer(MetricID metricID) {
        return null;
    }

    @Override
    public SimpleTimer simpleTimer(MetricID metricID) {
        return findOrRegister(metricID, SimpleTimerMetric::new);
    }

    private <T extends Metric> T findOrRegister(MetricID metricID, Supplier<T> s) {
        InvariantUtil.checkNotNullOrThrow(metricID, "metricID");
        //noinspection unchecked
        return (T)map.computeIfAbsent(metricID, id -> registerBean(id, s.get()));
    }

    private <T extends Metric> T registerBean(MetricID metricID, T metric) {
        try {
            beanServer.registerMBean(metric, makeObjectName(metricID));
            return metric;
        } catch (NotCompliantMBeanException | InstanceAlreadyExistsException | MBeanRegistrationException e) {
            throw new RuntimeException(e);
        }
    }

    public Map<MetricID, Metric> getMetrics() {
        return Collections.unmodifiableMap(map);
    }

    public void resetAll() {
        map.values().stream().filter(m -> m instanceof Resettable).map(Resettable.class::cast).forEach(Resettable::reset);
    }

    public void removeAll() {
        map.keySet().forEach(this::unregister);
        map.clear();
    }

    private void unregister(MetricID metricID) {
        try {
            beanServer.unregisterMBean(makeObjectName(metricID));
        } catch (InstanceNotFoundException | MBeanRegistrationException e) {
            LOGGER.info("Failed to unregister the MBean for " + metricID);
        }
    }
}
