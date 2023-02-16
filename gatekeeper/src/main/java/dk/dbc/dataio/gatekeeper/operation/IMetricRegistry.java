package dk.dbc.dataio.gatekeeper.operation;

import org.apache.commons.lang3.NotImplementedException;
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

import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.function.Function;
import java.util.function.Supplier;

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
    default <T extends Metric> T register(String s, T t) throws IllegalArgumentException {
        throw new NotImplementedException("");
    }

    @Override
    default <T extends Metric> T register(Metadata metadata, T t) throws IllegalArgumentException {
        throw new NotImplementedException("Please add");
    }

    @Override
    default <T extends Metric> T register(Metadata metadata, T t, Tag... tags) throws IllegalArgumentException {
        throw new NotImplementedException("Please add");
    }

    @Override
    default Counter counter(MetricID metricID) {
        throw new NotImplementedException("Please add");
    }

    @Override
    default ConcurrentGauge concurrentGauge(String s) {
        throw new NotImplementedException("Please add");
    }

    @Override
    default ConcurrentGauge concurrentGauge(String s, Tag... tags) {
        throw new NotImplementedException("Please add");
    }

    @Override
    default ConcurrentGauge concurrentGauge(MetricID metricID) {
        throw new NotImplementedException("Please add");
    }

    @Override
    default ConcurrentGauge concurrentGauge(Metadata metadata) {
        throw new NotImplementedException("Please add");
    }

    @Override
    default ConcurrentGauge concurrentGauge(Metadata metadata, Tag... tags) {
        throw new NotImplementedException("Please add");
    }

    @Override
    default <T, R extends Number> Gauge<R> gauge(String s, T t, Function<T, R> function, Tag... tags) {
        throw new NotImplementedException("Please add");
    }

    @Override
    default <T, R extends Number> Gauge<R> gauge(MetricID metricID, T t, Function<T, R> function) {
        throw new NotImplementedException("Please add");
    }

    @Override
    default <T, R extends Number> Gauge<R> gauge(Metadata metadata, T t, Function<T, R> function, Tag... tags) {
        throw new NotImplementedException("Please add");
    }

    @Override
    default <T extends Number> Gauge<T> gauge(String s, Supplier<T> supplier, Tag... tags) {
        throw new NotImplementedException("Please add");
    }

    @Override
    default <T extends Number> Gauge<T> gauge(MetricID metricID, Supplier<T> supplier) {
        throw new NotImplementedException("Please add");
    }

    @Override
    default <T extends Number> Gauge<T> gauge(Metadata metadata, Supplier<T> supplier, Tag... tags) {
        throw new NotImplementedException("Please add");
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
    default Meter meter(String s) {
        throw new NotImplementedException("Please add");
    }

    @Override
    default Meter meter(String s, Tag... tags) {
        throw new NotImplementedException("Please add");
    }

    @Override
    default Meter meter(MetricID metricID) {
        throw new NotImplementedException("Please add");
    }

    @Override
    default Meter meter(Metadata metadata) {
        throw new NotImplementedException("Please add");
    }

    @Override
    default Meter meter(Metadata metadata, Tag... tags) {
        throw new NotImplementedException("Please add");
    }

    @Override
    default Timer timer(String s) {
        throw new NotImplementedException("Please add");
    }

    @Override
    default Timer timer(String s, Tag... tags) {
        throw new NotImplementedException("Please add");
    }

    @Override
    default Timer timer(MetricID metricID) {
        throw new NotImplementedException("Please add");
    }

    @Override
    default Timer timer(Metadata metadata) {
        throw new NotImplementedException("Please add");
    }

    @Override
    default Timer timer(Metadata metadata, Tag... tags) {
        throw new NotImplementedException("Please add");
    }

    @Override
    default SimpleTimer simpleTimer(String s) {
        throw new NotImplementedException("Please add");
    }

    @Override
    default SimpleTimer simpleTimer(String s, Tag... tags) {
        throw new NotImplementedException("Please add");
    }

    @Override
    default SimpleTimer simpleTimer(MetricID metricID) {
        throw new NotImplementedException("Please add");
    }

    @Override
    default SimpleTimer simpleTimer(Metadata metadata) {
        throw new NotImplementedException("Please add");
    }

    @Override
    default SimpleTimer simpleTimer(Metadata metadata, Tag... tags) {
        throw new NotImplementedException("Please add");
    }

    @Override
    default Metric getMetric(MetricID metricID) {
        throw new NotImplementedException("Please add");
    }

    @Override
    default <T extends Metric> T getMetric(MetricID metricID, Class<T> aClass) {
        throw new NotImplementedException("Please add");
    }

    @Override
    default Counter getCounter(MetricID metricID) {
        throw new NotImplementedException("Please add");
    }

    @Override
    default ConcurrentGauge getConcurrentGauge(MetricID metricID) {
        throw new NotImplementedException("Please add");
    }

    @Override
    default Gauge<?> getGauge(MetricID metricID) {
        throw new NotImplementedException("Please add");
    }

    @Override
    default Histogram getHistogram(MetricID metricID) {
        throw new NotImplementedException("Please add");
    }

    @Override
    default Meter getMeter(MetricID metricID) {
        throw new NotImplementedException("Please add");
    }

    @Override
    default Timer getTimer(MetricID metricID) {
        throw new NotImplementedException("Please add");
    }

    @Override
    default SimpleTimer getSimpleTimer(MetricID metricID) {
        throw new NotImplementedException("Please add");
    }

    @Override
    default Metadata getMetadata(String s) {
        throw new NotImplementedException("Please add");
    }

    @Override
    default boolean remove(String s) {
        throw new NotImplementedException("Please add");
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
        throw new NotImplementedException("Please add");
    }

    @Override
    default SortedSet<MetricID> getMetricIDs() {
        throw new NotImplementedException("Please add");
    }

    @Override
    default SortedMap<MetricID, Gauge> getGauges() {
        throw new NotImplementedException("Please add");
    }

    @Override
    default SortedMap<MetricID, Gauge> getGauges(MetricFilter metricFilter) {
        throw new NotImplementedException("Please add");
    }

    @Override
    default SortedMap<MetricID, Counter> getCounters() {
        throw new NotImplementedException("Please add");
    }

    @Override
    default SortedMap<MetricID, Counter> getCounters(MetricFilter metricFilter) {
        throw new NotImplementedException("Please add");
    }

    @Override
    default SortedMap<MetricID, ConcurrentGauge> getConcurrentGauges() {
        throw new NotImplementedException("Please add");
    }

    @Override
    default SortedMap<MetricID, ConcurrentGauge> getConcurrentGauges(MetricFilter metricFilter) {
        throw new NotImplementedException("Please add");
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
    default SortedMap<MetricID, Meter> getMeters() {
        throw new NotImplementedException("Please add");
    }

    @Override
    default SortedMap<MetricID, Meter> getMeters(MetricFilter metricFilter) {
        throw new NotImplementedException("Please add");
    }

    @Override
    default SortedMap<MetricID, Timer> getTimers() {
        throw new NotImplementedException("Please add");
    }

    @Override
    default SortedMap<MetricID, Timer> getTimers(MetricFilter metricFilter) {
        throw new NotImplementedException("Please add");
    }

    @Override
    default SortedMap<MetricID, SimpleTimer> getSimpleTimers() {
        throw new NotImplementedException("Please add");
    }

    @Override
    default SortedMap<MetricID, SimpleTimer> getSimpleTimers(MetricFilter metricFilter) {
        throw new NotImplementedException("Please add");
    }

    @Override
    default SortedMap<MetricID, Metric> getMetrics(MetricFilter metricFilter) {
        throw new NotImplementedException("Please add");
    }

    @Override
    default <T extends Metric> SortedMap<MetricID, T> getMetrics(Class<T> aClass, MetricFilter metricFilter) {
        throw new NotImplementedException("Please add");
    }

    @Override
    default Map<MetricID, Metric> getMetrics() {
        throw new NotImplementedException("Please add");
    }

    @Override
    default Map<String, Metadata> getMetadata() {
        throw new NotImplementedException("Please add");
    }

    @Override
    default Type getType() {
        throw new NotImplementedException("Please add");
    }
}
