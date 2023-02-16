package dk.dbc.dataio.gatekeeper.operation;

import org.eclipse.microprofile.metrics.ConcurrentGauge;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Gauge;
import org.eclipse.microprofile.metrics.Histogram;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.Meter;
import org.eclipse.microprofile.metrics.Metric;
import org.eclipse.microprofile.metrics.MetricFilter;
import org.eclipse.microprofile.metrics.MetricID;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.SimpleTimer;
import org.eclipse.microprofile.metrics.Tag;
import org.eclipse.microprofile.metrics.Timer;

import javax.management.DynamicMBean;
import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.util.Hashtable;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.function.Supplier;

public class JMXMetricRegistry implements MetricRegistry {
    private static final Map<MetricID, Metric> map = new ConcurrentHashMap<>();
    private static final MBeanServer beanServer = ManagementFactory.getPlatformMBeanServer();

    public ObjectName makeObjectName(MetricID metricID) {
        try {
            Hashtable<String, String> hashtable = new Hashtable<>();
            hashtable.put("name", metricID.getName());
            hashtable.putAll(metricID.getTags());
            return new ObjectName("dbc", hashtable);
        } catch (MalformedObjectNameException e) {
            throw new RuntimeException(e);
        }
    }

    public static class CounterMetric implements Metric, Counter {
        private AtomicLong value;


        @Override
        public void inc() {
            value.incrementAndGet();
        }

        @Override
        public void inc(long l) {
            value.addAndGet(l);
        }

        @Override
        public long getCount() {
            return value.get();
        }
    }

    @Override
    public <T extends Metric> T register(String s, T t) throws IllegalArgumentException {
        return null;
    }

    @Override
    public <T extends Metric> T register(Metadata metadata, T t) throws IllegalArgumentException {
        return null;
    }

    @Override
    public <T extends Metric> T register(Metadata metadata, T t, Tag... tags) throws IllegalArgumentException {
        return null;
    }

    @Override
    public Counter counter(String s) {
        return null;
    }

    @Override
    public Counter counter(String s, Tag... tags) {
        return null;
    }

    @Override
    public Counter counter(MetricID metricID) {
        return (Counter) map.computeIfAbsent(metricID, id -> registerBean(id, new CounterMetric()));
    }

    private <T extends Metric> T registerBean(MetricID metricID, T metric) {
        try {
            beanServer.registerMBean(metric, makeObjectName(metricID));
            return metric;
        } catch (NotCompliantMBeanException | InstanceAlreadyExistsException | MBeanRegistrationException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Counter counter(Metadata metadata) {
        return null;
    }

    @Override
    public Counter counter(Metadata metadata, Tag... tags) {
        return null;
    }

    @Override
    public ConcurrentGauge concurrentGauge(String s) {
        return null;
    }

    @Override
    public ConcurrentGauge concurrentGauge(String s, Tag... tags) {
        return null;
    }

    @Override
    public ConcurrentGauge concurrentGauge(MetricID metricID) {
        return null;
    }

    @Override
    public ConcurrentGauge concurrentGauge(Metadata metadata) {
        return null;
    }

    @Override
    public ConcurrentGauge concurrentGauge(Metadata metadata, Tag... tags) {
        return null;
    }

    @Override
    public <T, R extends Number> Gauge<R> gauge(String s, T t, Function<T, R> function, Tag... tags) {
        return null;
    }

    @Override
    public <T, R extends Number> Gauge<R> gauge(MetricID metricID, T t, Function<T, R> function) {
        return null;
    }

    @Override
    public <T, R extends Number> Gauge<R> gauge(Metadata metadata, T t, Function<T, R> function, Tag... tags) {
        return null;
    }

    @Override
    public <T extends Number> Gauge<T> gauge(String s, Supplier<T> supplier, Tag... tags) {
        return null;
    }

    @Override
    public <T extends Number> Gauge<T> gauge(MetricID metricID, Supplier<T> supplier) {
        return null;
    }

    @Override
    public <T extends Number> Gauge<T> gauge(Metadata metadata, Supplier<T> supplier, Tag... tags) {
        return null;
    }

    @Override
    public Histogram histogram(String s) {
        return null;
    }

    @Override
    public Histogram histogram(String s, Tag... tags) {
        return null;
    }

    @Override
    public Histogram histogram(MetricID metricID) {
        return null;
    }

    @Override
    public Histogram histogram(Metadata metadata) {
        return null;
    }

    @Override
    public Histogram histogram(Metadata metadata, Tag... tags) {
        return null;
    }

    @Override
    public Meter meter(String s) {
        return null;
    }

    @Override
    public Meter meter(String s, Tag... tags) {
        return null;
    }

    @Override
    public Meter meter(MetricID metricID) {
        return null;
    }

    @Override
    public Meter meter(Metadata metadata) {
        return null;
    }

    @Override
    public Meter meter(Metadata metadata, Tag... tags) {
        return null;
    }

    @Override
    public Timer timer(String s) {
        return null;
    }

    @Override
    public Timer timer(String s, Tag... tags) {
        return null;
    }

    @Override
    public Timer timer(MetricID metricID) {
        return null;
    }

    @Override
    public Timer timer(Metadata metadata) {
        return null;
    }

    @Override
    public Timer timer(Metadata metadata, Tag... tags) {
        return null;
    }

    @Override
    public SimpleTimer simpleTimer(String s) {
        return null;
    }

    @Override
    public SimpleTimer simpleTimer(String s, Tag... tags) {
        return null;
    }

    @Override
    public SimpleTimer simpleTimer(MetricID metricID) {
        return null;
    }

    @Override
    public SimpleTimer simpleTimer(Metadata metadata) {
        return null;
    }

    @Override
    public SimpleTimer simpleTimer(Metadata metadata, Tag... tags) {
        return null;
    }

    @Override
    public Metric getMetric(MetricID metricID) {
        return null;
    }

    @Override
    public <T extends Metric> T getMetric(MetricID metricID, Class<T> aClass) {
        return null;
    }

    @Override
    public Counter getCounter(MetricID metricID) {
        return null;
    }

    @Override
    public ConcurrentGauge getConcurrentGauge(MetricID metricID) {
        return null;
    }

    @Override
    public Gauge<?> getGauge(MetricID metricID) {
        return null;
    }

    @Override
    public Histogram getHistogram(MetricID metricID) {
        return null;
    }

    @Override
    public Meter getMeter(MetricID metricID) {
        return null;
    }

    @Override
    public Timer getTimer(MetricID metricID) {
        return null;
    }

    @Override
    public SimpleTimer getSimpleTimer(MetricID metricID) {
        return null;
    }

    @Override
    public Metadata getMetadata(String s) {
        return null;
    }

    @Override
    public boolean remove(String s) {
        return false;
    }

    @Override
    public boolean remove(MetricID metricID) {
        return false;
    }

    @Override
    public void removeMatching(MetricFilter metricFilter) {

    }

    @Override
    public SortedSet<String> getNames() {
        return null;
    }

    @Override
    public SortedSet<MetricID> getMetricIDs() {
        return null;
    }

    @Override
    public SortedMap<MetricID, Gauge> getGauges() {
        return null;
    }

    @Override
    public SortedMap<MetricID, Gauge> getGauges(MetricFilter metricFilter) {
        return null;
    }

    @Override
    public SortedMap<MetricID, Counter> getCounters() {
        return null;
    }

    @Override
    public SortedMap<MetricID, Counter> getCounters(MetricFilter metricFilter) {
        return null;
    }

    @Override
    public SortedMap<MetricID, ConcurrentGauge> getConcurrentGauges() {
        return null;
    }

    @Override
    public SortedMap<MetricID, ConcurrentGauge> getConcurrentGauges(MetricFilter metricFilter) {
        return null;
    }

    @Override
    public SortedMap<MetricID, Histogram> getHistograms() {
        return null;
    }

    @Override
    public SortedMap<MetricID, Histogram> getHistograms(MetricFilter metricFilter) {
        return null;
    }

    @Override
    public SortedMap<MetricID, Meter> getMeters() {
        return null;
    }

    @Override
    public SortedMap<MetricID, Meter> getMeters(MetricFilter metricFilter) {
        return null;
    }

    @Override
    public SortedMap<MetricID, Timer> getTimers() {
        return null;
    }

    @Override
    public SortedMap<MetricID, Timer> getTimers(MetricFilter metricFilter) {
        return null;
    }

    @Override
    public SortedMap<MetricID, SimpleTimer> getSimpleTimers() {
        return null;
    }

    @Override
    public SortedMap<MetricID, SimpleTimer> getSimpleTimers(MetricFilter metricFilter) {
        return null;
    }

    @Override
    public SortedMap<MetricID, Metric> getMetrics(MetricFilter metricFilter) {
        return null;
    }

    @Override
    public <T extends Metric> SortedMap<MetricID, T> getMetrics(Class<T> aClass, MetricFilter metricFilter) {
        return null;
    }

    @Override
    public Map<MetricID, Metric> getMetrics() {
        return null;
    }

    @Override
    public Map<String, Metadata> getMetadata() {
        return null;
    }

    @Override
    public Type getType() {
        return null;
    }

    public static class DataBean implements DynamicMBean {

    }
}
