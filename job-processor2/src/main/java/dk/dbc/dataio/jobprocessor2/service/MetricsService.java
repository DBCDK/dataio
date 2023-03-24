package dk.dbc.dataio.jobprocessor2.service;

import com.sun.management.OperatingSystemMXBean;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.microprofile.metrics.Counting;
import org.eclipse.microprofile.metrics.Gauge;
import org.eclipse.microprofile.metrics.Metric;
import org.eclipse.microprofile.metrics.MetricID;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.SimpleTimer;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.Arrays;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class MetricsService {
    private final MetricRegistry metricRegistry;

    public MetricsService(HttpService httpService, MetricRegistry metricRegistry) {
        httpService.addServlet(this::makePrometheusPage, "/metrics/*");
        this.metricRegistry = metricRegistry;
        registerVmMetrics();
    }

    private void registerVmMetrics() {
        metricRegistry.gauge("base_jvm_uptime_seconds", () -> ManagementFactory.getRuntimeMXBean().getUptime() / 1000);
        OperatingSystemMXBean osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        metricRegistry.gauge("base_cpu_system_loadAverage", osBean::getSystemCpuLoad);
        metricRegistry.gauge("base_cpu_process_loadAverage", osBean::getProcessCpuLoad);
        metricRegistry.gauge("base_memory_freePhysicalMemorySize", osBean::getFreePhysicalMemorySize);
        metricRegistry.gauge("base_memory_freeSwapSpaceSize", osBean::getFreeSwapSpaceSize);
        MemoryMXBean memBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heap = memBean.getHeapMemoryUsage();
        metricRegistry.gauge("base_memory_initHeap_bytes", heap::getInit);
        metricRegistry.gauge("base_memory_usedHeap_bytes", heap::getUsed);
        metricRegistry.gauge("base_memory_committedHeap_bytes", heap::getCommitted);
        metricRegistry.gauge("base_memory_maxHeap_bytes", heap::getMax);
        MemoryUsage nonHeap = memBean.getNonHeapMemoryUsage();
        metricRegistry.gauge("base_memory_initNonHeap_bytes", nonHeap::getInit);
        metricRegistry.gauge("base_memory_usedNonHeap_bytes", nonHeap::getUsed);
        metricRegistry.gauge("base_memory_committedNonHeap_bytes", nonHeap::getCommitted);
        metricRegistry.gauge("base_memory_maxNonHeap_bytes", nonHeap::getMax);
    }

    @SuppressWarnings("PMD")
    private void makePrometheusPage(HttpServletRequest request, HttpServletResponse response) throws IOException {
        PrintWriter writer = response.getWriter();
        metricRegistry.getMetrics().entrySet().stream()
                .flatMap(e -> mapMetric(e.getKey(), e.getValue()))
                .forEach(writer::println);
    }

    private Stream<String> mapMetric(MetricID id, Metric metric) {
        String name = idToName(id);
        MetricType metricType = MetricType.from(metric);
        if(metricType == null) return Stream.empty();
        String type = "# TYPE " + id.getName() + " " + metricType.name;
        String val = name + " " + metricType.value(metric);
        return Stream.of(type, val);
    }

    private String idToName(MetricID id) {
        String tags = id.getTagsAsString();
        return id.getName() + (tags.isEmpty() ? "" : "{" + tags + "}");
    }

    public enum MetricType {
        COUNTER("counter", m -> m instanceof Counting, MetricType::counter),
        SIMPLE_TIMER("counter", m -> m instanceof SimpleTimer, MetricType::counter),
        GAUGE("gauge", m -> m instanceof Gauge, MetricType::gauge);

        private final String name;
        private final Predicate<Metric> typePredicate;
        private final Function<Metric, Number> valueExtractor;

        MetricType(String name, Predicate<Metric> typePredicate, Function<Metric, Number> valueExtractor) {
            this.name = name;
            this.typePredicate = typePredicate;
            this.valueExtractor = valueExtractor;
        }

        public Number value(Metric metric) {
            return valueExtractor.apply(metric);
        }

        public static MetricType from(Metric metric) {
            return Arrays.stream(values()).filter(t -> t.typePredicate.test(metric)).findFirst().orElse(null);
        }

        private static Number counter(Metric metric) {
            return ((Counting)metric).getCount();
        }

        private static Number gauge(Metric metric) {
            //noinspection unchecked
            return ((Gauge<Number>)metric).getValue();
        }
    }
}
