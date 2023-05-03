package dk.dbc.dataio.jse.artemis.common.service;

import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.jse.artemis.common.Config;
import dk.dbc.dataio.registry.PrometheusMetricRegistry;
import org.eclipse.microprofile.metrics.MetricRegistry;

import javax.ws.rs.client.ClientBuilder;


public class ServiceHub implements AutoCloseable {
    public final MetricRegistry metricRegistry;
    public final HttpService httpService;
    public final HealthService healthService;
    public final MetricsService metricsService;
    public final ZombieWatch zombieWatch;
    public final JobStoreServiceConnector jobStoreServiceConnector;

    private ServiceHub(MetricRegistry metricRegistry, HttpService httpService, HealthService healthService, MetricsService metricsService, ZombieWatch zombieWatch, JobStoreServiceConnector jobStoreServiceConnector) {
        this.metricRegistry = metricRegistry;
        this.httpService = httpService;
        this.healthService = healthService;
        this.metricsService = metricsService;
        this.zombieWatch = zombieWatch;
        this.jobStoreServiceConnector = jobStoreServiceConnector;
    }

    public static ServiceHub defaultHub() {
        return new ServiceHub.Builder().build();
    }

    @Override
    public void close() throws Exception {
        httpService.close();
    }

    @SuppressWarnings("unused")
    public static class Builder {
        private MetricRegistry metricRegistry = PrometheusMetricRegistry.create();
        private HttpService httpService = new HttpService(Config.WEB_PORT.asInteger());
        private HealthService healthService = new HealthService(httpService);
        private MetricsService metricsService = new MetricsService(httpService, metricRegistry);
        private ZombieWatch zombieWatch = new ZombieWatch(healthService);
        private JobStoreServiceConnector jobStoreServiceConnector = Config.JOBSTORE_URL.asOptionalString().map(js -> new JobStoreServiceConnector(ClientBuilder.newClient(), js)).orElse(null);

        public ServiceHub build() {
            return new ServiceHub(metricRegistry, httpService, healthService, metricsService, zombieWatch, jobStoreServiceConnector);
        }

        public Builder withMetricRegistry(MetricRegistry metricRegistry) {
            this.metricRegistry = metricRegistry;
            return this;
        }

        public Builder withHttpService(HttpService httpService) {
            this.httpService = httpService;
            return this;
        }

        public Builder withHealthService(HealthService healthService) {
            this.healthService = healthService;
            return this;
        }

        public Builder withMetricsService(MetricsService metricsService) {
            this.metricsService = metricsService;
            return this;
        }

        public Builder withZombieWatch(ZombieWatch zombieWatch) {
            this.zombieWatch = zombieWatch;
            return this;
        }

        public Builder withJobStoreServiceConnector(JobStoreServiceConnector jobStoreServiceConnector) {
            this.jobStoreServiceConnector = jobStoreServiceConnector;
            return this;
        }
    }
}
