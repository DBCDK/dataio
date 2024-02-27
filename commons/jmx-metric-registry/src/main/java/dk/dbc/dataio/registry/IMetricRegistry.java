package dk.dbc.dataio.registry;

import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Gauge;
import org.eclipse.microprofile.metrics.Histogram;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.Metric;
import org.eclipse.microprofile.metrics.MetricFilter;
import org.eclipse.microprofile.metrics.MetricID;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.Tag;
import org.eclipse.microprofile.metrics.Timer;

import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public interface IMetricRegistry extends MetricRegistry {


    @Override
    default Counter counter(String s) {
        return counter(new MetricID(s));
    }

    @Override
    default Counter counter(String s, Tag... tags) {
        return counter(new MetricID(s, tags));
    }

    @Override
    default Counter counter(Metadata metadata) {
        return counter(metadata.getName());
    }

    @Override
    default Counter counter(Metadata metadata, Tag... tags) {
        return counter(new MetricID(metadata.getName(), tags));
    }

    @Override
    default <T, R extends Number> Gauge<R> gauge(String s, T t, Function<T, R> function, Tag... tags) {
        return gauge(new MetricID(s, tags), t, function);
    }

    @Override
    default <T, R extends Number> Gauge<R> gauge(Metadata metadata, T t, Function<T, R> function, Tag... tags) {
        return gauge(new MetricID(metadata.getName(), tags), t, function);
    }

    @Override
    default <T extends Number> Gauge<T> gauge(String s, Supplier<T> supplier, Tag... tags) {
        return gauge(new MetricID(s, tags), supplier);
    }

    @Override
    default <T extends Number> Gauge<T> gauge(Metadata metadata, Supplier<T> supplier, Tag... tags) {
        return gauge(new MetricID(metadata.getName(), tags), supplier);
    }

    @Override
    default Histogram histogram(String s) {
        throw new NotImplementedException("Please add");
    }

    @Override
    default Histogram histogram(String s, Tag... tags) {
        throw new NotImplementedException("Please add");
    }

    @Override
    default Histogram histogram(MetricID metricID) {
        throw new NotImplementedException("Please add");
    }

    @Override
    default Histogram histogram(Metadata metadata) {
        throw new NotImplementedException("Please add");
    }

    @Override
    default Histogram histogram(Metadata metadata, Tag... tags) {
        throw new NotImplementedException("Please add");
    }

    @Override
    default Timer timer(String s) {
        return timer(new MetricID(s));
    }

    @Override
    default Timer timer(String s, Tag... tags) {
        return timer(new MetricID(s, tags));
    }

    @Override
    default Timer timer(Metadata metadata) {
        return timer(new MetricID(metadata.getName()));
    }

    @Override
    default Timer timer(Metadata metadata, Tag... tags) {
        return timer(new MetricID(metadata.getName(), tags));
    }

    @Override
    default <T extends Metric> T getMetric(MetricID metricID, Class<T> aClass) {
        //noinspection unchecked
        return (T)getMetric(metricID);
    }

    @Override
    default Counter getCounter(MetricID metricID) {
        return getMetric(metricID, Counter.class);
    }

    @Override
    default Gauge<?> getGauge(MetricID metricID) {
        return getMetric(metricID, Gauge.class);
    }

    @Override
    default Histogram getHistogram(MetricID metricID) {
        return getMetric(metricID, Histogram.class);
    }

    @Override
    default Timer getTimer(MetricID metricID) {
        return getMetric(metricID, Timer.class);
    }

    @Override
    default Metadata getMetadata(String s) {
        throw new NotImplementedException("Please add");
    }

    @Override
    default boolean remove(String s) {
        return remove(new MetricID(s));
    }

    @Override
    default boolean remove(MetricID metricID) {
        throw new NotImplementedException("Please add");
    }

    @Override
    default void removeMatching(MetricFilter metricFilter) {
        throw new NotImplementedException("Please add");
    }

    @Override
    default SortedSet<String> getNames() {
        return getMetricIDs().stream().map(MetricID::getName).collect(Collectors.toCollection(TreeSet::new));
    }

    @Override
    default SortedMap<MetricID, Gauge> getGauges() {
        return getMetrics(Gauge.class, (metricID, metric) -> metric instanceof Gauge);
    }

    @Override
    default SortedMap<MetricID, Gauge> getGauges(MetricFilter metricFilter) {
        return getMetrics(Gauge.class, metricFilter);
    }

    @Override
    default SortedMap<MetricID, Counter> getCounters() {
        return getMetrics(Counter.class, (metricID, metric) -> metric instanceof Counter);
    }

    @Override
    default SortedMap<MetricID, Counter> getCounters(MetricFilter metricFilter) {
        return getMetrics(Counter.class, metricFilter);
    }

    @Override
    default SortedMap<MetricID, Histogram> getHistograms() {
        throw new NotImplementedException("Please add");
    }

    @Override
    default SortedMap<MetricID, Histogram> getHistograms(MetricFilter metricFilter) {
        throw new NotImplementedException("Please add");
    }

    @Override
    default SortedMap<MetricID, Metric> getMetrics(MetricFilter metricFilter) {
        return getMetrics(Metric.class, metricFilter);
    }

    @Override
    default SortedMap<MetricID, Timer> getTimers() {
        return getMetrics(Timer.class, (metricID, metric) -> metric instanceof Timer);
    }

    @Override
    default SortedMap<MetricID, Timer> getTimers(MetricFilter metricFilter) {
        return getMetrics(Timer.class, metricFilter);
    }

    default  <T extends Metric> SortedMap<MetricID, T> getMetrics(Class<T> clazz, MetricFilter metricFilter) {
        return getMetrics().entrySet().stream().filter(e -> metricFilter.matches(e.getKey(), e.getValue())).collect(Collectors.toMap(Map.Entry::getKey, e -> (T)e.getValue(), (old, repl) -> repl, TreeMap::new));
    }

    @Override
    default SortedSet<MetricID> getMetricIDs() {
        return new TreeSet<>(getMetrics().keySet());
    }

    @Override
    default Metric getMetric(MetricID metricID) {
        return getMetrics().get(metricID);
    }

    @Override
    default Map<String, Metadata> getMetadata() {
        throw new NotImplementedException("Please add");
    }
}
